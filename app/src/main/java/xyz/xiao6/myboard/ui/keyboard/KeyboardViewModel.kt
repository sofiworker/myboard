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
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.HandwritingRecognizer
import xyz.xiao6.myboard.data.db.ClipboardItem
import xyz.xiao6.myboard.data.engine.AssetDictionarySource
import xyz.xiao6.myboard.data.engine.DictionaryManager
import xyz.xiao6.myboard.data.engine.FileDictionarySource
import xyz.xiao6.myboard.data.engine.FrequencySuggestionStrategy
import xyz.xiao6.myboard.data.engine.FuzzyPinyinStrategy
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

    private val keyboardRepository = KeyboardRepository(application)
    private val themeRepository = ThemeRepository(application)
    private val settingsRepository = SettingsRepository(application)
    private val clipboardRepository = ClipboardRepository(application)
    private val emojiRepository = EmojiRepository(application)
    private val handwritingRecognizer = HandwritingRecognizer()

    private val suggestionEngine: SuggestionEngine
    private val userDictionarySource: UserDictionarySource

    val clipboardHistory: Flow<List<ClipboardItem>> = clipboardRepository.getHistory()
    private val _voiceState = MutableStateFlow(VoiceInputManager.State.IDLE)
    val voiceState: StateFlow<VoiceInputManager.State> = _voiceState

    private val _mainKeyboardLayoutName = settingsRepository.getSelectedLayout()
    private val _keyboardLayoutName = MutableStateFlow(_mainKeyboardLayoutName)
    val keyboardLayout = _keyboardLayoutName.map { keyboardRepository.getKeyboardLayout(it) }

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

    init {
        userDictionarySource = UserDictionarySource(application)
        suggestionEngine = SuggestionEngine(
            DictionaryManager(
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
            composingText.debounce(100).collect { text ->
                if (text.isBlank()) {
                    _suggestions.value = emptyList()
                } else {
                    viewModelScope.launch {
                        _suggestions.value = suggestionEngine.getSuggestions(text).map { it.text }
                    }
                }
            }
        }
        handwritingRecognizer.downloadModel(Locale.getDefault().toLanguageTag()) { success ->
            if (!success) {
                Log.w("KeyboardViewModel", "Failed to prepare handwriting model; handwriting suggestions disabled.")
            }
        }
    }

    fun commitSuggestion(text: String) {
        viewModelScope.launch {
            _keyAction.emit(KeyAction.CommitSuggestion(text))
        }
    }

    fun deleteClipboardItem(id: Int) {
        viewModelScope.launch {
            clipboardRepository.delete(id)
        }
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
                val text = if (_shiftState.value != ShiftState.OFF) action.text.uppercase() else action.text
                _composingText.value += text
                if (_shiftState.value == ShiftState.ON) {
                    _shiftState.value = ShiftState.OFF
                }
            }
            KeyAction.Delete -> _composingText.value = _composingText.value.dropLast(1)
            KeyAction.Space -> {
                val composing = _composingText.value
                learnWord(composing)
                _keyAction.emit(KeyAction.InsertText("$composing "))
                _composingText.value = ""
                return
            }
            is KeyAction.CommitSuggestion -> {
                learnWord(action.suggestion) // Learn the selected suggestion
                _keyAction.emit(action)
                _composingText.value = ""
                return
            }
            is KeyAction.Shift -> {
                _shiftState.value = when (_shiftState.value) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.CAPS_LOCK
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                return // Do not send a key action
            }
            is KeyAction.SwitchToLayout -> {
                val targetLayout = if (action.layout == "_main_") _mainKeyboardLayoutName else action.layout
                _keyboardLayoutName.value = targetLayout
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
}
