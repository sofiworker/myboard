package xyz.xiao6.myboard.ui.keyboard

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.HandwritingRecognizer
import xyz.xiao6.myboard.data.db.ClipboardItem
import xyz.xiao6.myboard.data.engine.AssetDictionarySource
import xyz.xiao6.myboard.data.engine.DictionaryManager
import xyz.xiao6.myboard.data.engine.FileDictionarySource
import xyz.xiao6.myboard.data.engine.FrequencySuggestionStrategy
import xyz.xiao6.myboard.data.engine.FuzzyPinyinStrategy
import xyz.xiao6.myboard.data.engine.PinyinDictionarySource
import xyz.xiao6.myboard.data.engine.SuggestionEngine
import xyz.xiao6.myboard.data.engine.UserDictionarySource
import xyz.xiao6.myboard.data.model.EmojiData
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.model.ShiftState
import xyz.xiao6.myboard.data.model.theme.ThemeData
import xyz.xiao6.myboard.data.repository.ClipboardRepository
import xyz.xiao6.myboard.data.repository.EmojiRepository
import xyz.xiao6.myboard.data.repository.KeyboardRepository
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.data.repository.ThemeRepository
import xyz.xiao6.myboard.data.voice.VoiceInputManager
import java.util.Locale

class KeyboardViewModel(application: Application) : AndroidViewModel(application) {

    private enum class KeyboardMode { CHARACTERS, NUMERIC, SYMBOLS }

    private val keyboardRepository = KeyboardRepository(application)
    private val themeRepository = ThemeRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val clipboardRepository = ClipboardRepository(application)
    private val emojiRepository = EmojiRepository(application)
    private val handwritingRecognizer = HandwritingRecognizer()

    private val suggestionEngine: SuggestionEngine
    private val userDictionarySource: UserDictionarySource

    enum class TopBarState { TOOLBAR, CANDIDATES_COLLAPSED, CANDIDATES_EXPANDED }

    /**
     * FlorisBoard-inspired state model:
     * - `mode` controls which fixed mode layout to show (numeric/symbols/etc).
     * - `charactersLayoutName` is the user-selected layout used only in CHARACTERS mode.
     * Mode switches never overwrite the characters layout selection.
     */
    private data class KeyboardState(
        val mode: KeyboardMode = KeyboardMode.CHARACTERS,
        val charactersLayoutName: String,
    )

    private class LayoutManager(private val repo: KeyboardRepository) {
        private val cache = mutableMapOf<String, xyz.xiao6.myboard.data.KeyboardData?>()

        fun getLayout(name: String): xyz.xiao6.myboard.data.KeyboardData? {
            return cache.getOrPut(name) { repo.getKeyboardLayout(name) }
        }

        fun clear() = cache.clear()
    }

    private val layoutManager = LayoutManager(keyboardRepository)

    val clipboardHistory: Flow<List<ClipboardItem>> = clipboardRepository.getHistory()
    private val _voiceState = MutableStateFlow(VoiceInputManager.State.IDLE)
    val voiceState: StateFlow<VoiceInputManager.State> = _voiceState

    private val _mainKeyboardLayoutName = settingsRepository.getSelectedLayout()
    private val _keyboardState =
        MutableStateFlow(KeyboardState(mode = KeyboardMode.CHARACTERS, charactersLayoutName = _mainKeyboardLayoutName))
    private val keyboardState: StateFlow<KeyboardState> = _keyboardState.asStateFlow()

    val keyboardLayout: StateFlow<xyz.xiao6.myboard.data.KeyboardData?> =
        keyboardState.mapLatest { state ->
            val layoutName = when (state.mode) {
                KeyboardMode.CHARACTERS -> state.charactersLayoutName
                KeyboardMode.NUMERIC -> "numeric"
                KeyboardMode.SYMBOLS -> "symbols"
            }
            layoutManager.getLayout(layoutName)
                ?: layoutManager.getLayout(_mainKeyboardLayoutName)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    /**
     * Heuristic language mode for top bar rendering:
     * - Treat non-English layout names (except handwriting) as Chinese/pinyin capable.
     */
    val isChineseMode: StateFlow<Boolean> =
        keyboardState.map { state -> isChineseLayout(state.charactersLayoutName) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /**
     * True when currently showing the user's default characters layout in CHARACTERS mode.
     * Used to decide whether the top segment shows toolbar when idle.
     */
    val isDefaultCharactersLayout: StateFlow<Boolean> =
        keyboardState.map { state ->
            state.mode == KeyboardMode.CHARACTERS && state.charactersLayoutName == _mainKeyboardLayoutName
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val isModeOverlayActive: StateFlow<Boolean> =
        keyboardState.map { it.mode != KeyboardMode.CHARACTERS }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val emojiData = MutableStateFlow<EmojiData?>(null)

    private val _keyAction = MutableSharedFlow<KeyAction>()
    val keyAction = _keyAction.asSharedFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText = _composingText.asStateFlow()

    private val _shiftState = MutableStateFlow(ShiftState.OFF)
    val shiftState = _shiftState.asStateFlow()

    private val _isEmojiMode = MutableStateFlow(false)
    val isEmojiMode = _isEmojiMode.asStateFlow()

    private val _isClipboardMode = MutableStateFlow(false)
    val isClipboardMode = _isClipboardMode.asStateFlow()

    private val _isFloatingMode = MutableStateFlow(settingsRepository.isFloatingMode())
    val isFloatingMode: StateFlow<Boolean> = _isFloatingMode.asStateFlow()

    private val _themeData = MutableStateFlow<ThemeData?>(null)
    val themeData: StateFlow<ThemeData?> = _themeData.asStateFlow()

    val keyboardHeight = settingsRepository.getKeyboardHeight()
    val oneHandedMode = settingsRepository.getOneHandedMode()

    private val _isCandidatesExpanded = MutableStateFlow(false)
    val isCandidatesExpanded: StateFlow<Boolean> = _isCandidatesExpanded.asStateFlow()

    val topBarState: StateFlow<TopBarState> =
        combine(composingText, suggestions, isCandidatesExpanded) { composing, suggs, expanded ->
            if (composing.isNotBlank() || suggs.isNotEmpty()) {
                if (expanded) TopBarState.CANDIDATES_EXPANDED else TopBarState.CANDIDATES_COLLAPSED
            } else {
                TopBarState.TOOLBAR
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, TopBarState.TOOLBAR)

    // Global toolbar keys (decoupled from layouts). Order is user-configurable.
    val toolbarKeys: StateFlow<List<xyz.xiao6.myboard.data.KeyData>> =
        settingsRepository.toolbarOrderFlow.map { order ->
            order.mapNotNull { id ->
                when (id) {
                    "numeric" -> xyz.xiao6.myboard.data.KeyData(type = "switch_to_layout", value = "numeric", label = "123")
                    "symbols" -> xyz.xiao6.myboard.data.KeyData(type = "switch_to_layout", value = "symbols", label = "#+=")
                    "emoji" -> xyz.xiao6.myboard.data.KeyData(type = "system_emoji", value = "😀", label = null)
                    "clipboard" -> xyz.xiao6.myboard.data.KeyData(type = "system_clipboard", value = "📋", label = null)
                    "voice" -> xyz.xiao6.myboard.data.KeyData(type = "system_voice", value = "🎤", label = null)
                    "settings" -> xyz.xiao6.myboard.data.KeyData(type = "show_settings", value = "⚙️", label = null)
                    else -> null
                }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        userDictionarySource = UserDictionarySource(application)
        suggestionEngine = SuggestionEngine(
            DictionaryManager(
                PinyinDictionarySource(application),
                AssetDictionarySource(application, "en_words.txt"),
                FileDictionarySource(application, "updated_en_words.txt"),
                userDictionarySource
            ),
            listOf(
                FuzzyPinyinStrategy(application),
                FrequencySuggestionStrategy()
            )
        )

        viewModelScope.launch {
            _themeData.value = themeRepository.getThemeData()
        }
        viewModelScope.launch {
            emojiData.value = emojiRepository.getEmojiData()
        }
        viewModelScope.launch {
            combine(
                composingText.debounce(100),
                keyboardState,
            ) { text, state -> text to state.mode }
                .mapLatest { (text, mode) ->
                    if (mode != KeyboardMode.CHARACTERS || text.isBlank()) {
                        emptyList()
                    } else {
                        suggestionEngine.getSuggestions(text).map { it.text }
                    }
                }
                .collect { _suggestions.value = it }
        }
        handwritingRecognizer.downloadModel(Locale.getDefault().toLanguageTag()) { success ->
            if (!success) {
                Log.w("KeyboardViewModel", "Failed to prepare handwriting model; handwriting suggestions disabled.")
            }
        }
    }

    fun resetSession() {
        _composingText.value = ""
        _suggestions.value = emptyList()
        _isCandidatesExpanded.value = false
    }

    fun resetToMainLayout() {
        _keyboardState.value = KeyboardState(
            mode = KeyboardMode.CHARACTERS,
            charactersLayoutName = _mainKeyboardLayoutName,
        )
        _isEmojiMode.value = false
        _isClipboardMode.value = false
        _isCandidatesExpanded.value = false
        resetSession()
    }

    fun commitSuggestion(text: String) {
        viewModelScope.launch {
            _isCandidatesExpanded.value = false
            _keyAction.emit(KeyAction.CommitSuggestion(text))
        }
    }

    fun expandCandidates() {
        _isCandidatesExpanded.value = true
    }

    fun collapseCandidates() {
        _isCandidatesExpanded.value = false
    }

    fun deleteClipboardItem(id: Int) {
        viewModelScope.launch {
            clipboardRepository.delete(id)
        }
    }

    fun exitEmojiMode() {
        _isEmojiMode.value = false
    }

    fun setVoiceState(state: VoiceInputManager.State) {
        _voiceState.value = state
    }

    fun onDragFloatingWindow(deltaX: Int, deltaY: Int) {
        viewModelScope.launch {
            _keyAction.emit(KeyAction.MoveFloatingWindow(deltaX, deltaY))
        }
    }

    fun onStrokeFinished(ink: Ink) {
        handwritingRecognizer.recognize(ink) { candidates ->
            _suggestions.value = candidates
        }
    }

    private fun learnWord(word: String) {
        if (word.length > 1) { // A simple heuristic to avoid learning single letters
            viewModelScope.launch {
                userDictionarySource.learn(word)
            }
        }
    }

    suspend fun onKeyPress(action: KeyAction) {
        when (action) {
            is KeyAction.InsertText -> {
                if (action.text == "\n") {
                    val composing = _composingText.value
                    if (composing.isNotEmpty()) {
                        learnWord(composing)
                        _keyAction.emit(KeyAction.InsertText(composing))
                    }
                    _keyAction.emit(action)
                    _composingText.value = ""
                    _isCandidatesExpanded.value = false
                    return
                }
                // Commit immediately for non-letter inputs (digits, punctuation) to avoid trapping them in composing state.
                if (action.text.any { !it.isLetter() }) {
                    val composing = _composingText.value
                    if (composing.isNotEmpty()) {
                        learnWord(composing)
                        _keyAction.emit(KeyAction.InsertText(composing))
                    }
                    _composingText.value = ""
                    _suggestions.value = emptyList()
                    _isCandidatesExpanded.value = false
                    _keyAction.emit(action)
                    return
                }
                val text = if (_shiftState.value != ShiftState.OFF) action.text.uppercase() else action.text
                _composingText.value += text
                if (_shiftState.value == ShiftState.ON) {
                    _shiftState.value = ShiftState.OFF
                }
            }
            KeyAction.Delete -> {
                if (_composingText.value.isNotEmpty()) {
                    _composingText.value = _composingText.value.dropLast(1)
                } else {
                    _keyAction.emit(KeyAction.Delete)
                }
            }
            KeyAction.Space -> {
                val composing = _composingText.value
                learnWord(composing)
                _keyAction.emit(KeyAction.InsertText("$composing "))
                _composingText.value = ""
                _isCandidatesExpanded.value = false
                return
            }
            is KeyAction.CommitSuggestion -> {
                learnWord(action.suggestion) // Learn the selected suggestion
                _keyAction.emit(action)
                _composingText.value = ""
                _isCandidatesExpanded.value = false
                return
            }
            is KeyAction.Shift -> {
                if (_keyboardState.value.mode != KeyboardMode.CHARACTERS) return
                _shiftState.value = when (_shiftState.value) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                return // Do not send a key action
            }
            is KeyAction.SwitchToLayout -> {
                val composing = _composingText.value
                if (composing.isNotBlank()) {
                    learnWord(composing)
                    _keyAction.emit(KeyAction.InsertText(composing))
                }
                _composingText.value = ""
                _suggestions.value = emptyList()
                _shiftState.value = ShiftState.OFF
                _isCandidatesExpanded.value = false
                when (action.layout) {
                    "_main_" -> _keyboardState.update {
                        it.copy(mode = KeyboardMode.CHARACTERS, charactersLayoutName = _mainKeyboardLayoutName)
                    }
                    "numeric" -> _keyboardState.update { it.copy(mode = KeyboardMode.NUMERIC) }
                    "symbols" -> _keyboardState.update { it.copy(mode = KeyboardMode.SYMBOLS) }
                    else -> _keyboardState.update {
                        it.copy(mode = KeyboardMode.CHARACTERS, charactersLayoutName = action.layout)
                    }
                }
                _isEmojiMode.value = false
                _isClipboardMode.value = false
                return
            }
            KeyAction.ShowSettings -> {
                _keyAction.emit(action)
                return
            }
            KeyAction.SystemEmoji -> {
                _isEmojiMode.value = !_isEmojiMode.value
                return
            }
            KeyAction.SystemClipboard -> {
                _isClipboardMode.value = !_isClipboardMode.value
                return
            }
            KeyAction.SystemVoice -> {
                _keyAction.emit(action)
            }
            is KeyAction.MoveCursor -> {
                _keyAction.emit(action)
                return
            }
            is KeyAction.MoveFloatingWindow -> {
                _keyAction.emit(action)
            }
        }
    }

    fun toggleCandidatesExpanded() {
        _isCandidatesExpanded.value = !_isCandidatesExpanded.value
    }

    private fun isChineseLayout(name: String): Boolean {
        val lower = name.lowercase()
        if (lower.startsWith("handwriting")) return false
        if (lower.startsWith("en_")) return false
        return lower.contains("shuangpin") || lower.startsWith("zh") || !lower.startsWith("en")
    }
}
