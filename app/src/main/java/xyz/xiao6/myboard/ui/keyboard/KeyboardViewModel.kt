package xyz.xiao6.myboard.ui.keyboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.model.ShiftState
import xyz.xiao6.myboard.data.model.theme.ThemeData
import xyz.xiao6.myboard.data.repository.DictionaryRepository
import xyz.xiao6.myboard.data.repository.KeyboardRepository
import xyz.xiao6.myboard.data.repository.ThemeRepository

class KeyboardViewModel(application: Application) : AndroidViewModel(application) {

    private val keyboardRepository = KeyboardRepository(application)
    private val dictionaryRepository = DictionaryRepository(application)
    private val themeRepository = ThemeRepository(application)

    private val _keyAction = MutableSharedFlow<KeyAction>()
    val keyAction = _keyAction.asSharedFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions = _suggestions.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText = _composingText.asStateFlow()

    private val _shiftState = MutableStateFlow(ShiftState.OFF)
    val shiftState = _shiftState.asStateFlow()

    private val _keyboardLayoutName = MutableStateFlow("qwerty")
    val keyboardLayout = _keyboardLayoutName.map { keyboardRepository.getKeyboardLayout(it) }

    private val _themeData = MutableStateFlow<ThemeData?>(null)
    val themeData = _themeData.asStateFlow()

    private var dictionary: List<String> = emptyList()

    init {
        viewModelScope.launch {
            dictionary = dictionaryRepository.getWords("en_words.txt")
        }
        viewModelScope.launch {
            _themeData.value = themeRepository.getThemeData("default")
        }
        viewModelScope.launch {
            composingText.debounce(100).collect { text ->
                if (text.isBlank()) {
                    _suggestions.value = emptyList()
                } else {
                    _suggestions.value = dictionary.filter { it.startsWith(text, ignoreCase = true) }
                }
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
            is KeyAction.Delete -> _composingText.value = _composingText.value.dropLast(1)
            is KeyAction.Space -> {
                _keyAction.emit(KeyAction.InsertText(_composingText.value + " "))
                _composingText.value = ""
                return
            }
            is KeyAction.CommitSuggestion -> {
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
                _keyboardLayoutName.value = action.layout
                return // Do not send a key action
            }
            is KeyAction.ShowSettings -> {
                _keyAction.emit(action)
                return
            }
        }
        _keyAction.emit(KeyAction.InsertText(_composingText.value))
    }
}
