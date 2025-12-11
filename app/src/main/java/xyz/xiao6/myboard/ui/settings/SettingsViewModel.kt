package xyz.xiao6.myboard.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.data.repository.KeyboardRepository
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.data.repository.ThemeRepository

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val themeRepository = ThemeRepository(application)
    private val keyboardRepository = KeyboardRepository(application)

    private val _longPressForSymbolsEnabled = MutableStateFlow(settingsRepository.isLongPressForSymbolsEnabled())
    val longPressForSymbolsEnabled = _longPressForSymbolsEnabled.asStateFlow()

    private val _toolbarEnabled = MutableStateFlow(settingsRepository.isToolbarEnabled())
    val toolbarEnabled = _toolbarEnabled.asStateFlow()

    val availableThemes = themeRepository.getAvailableThemes()
    private val _selectedTheme = MutableStateFlow(themeRepository.getThemeData().name)
    val selectedTheme = _selectedTheme.asStateFlow()

    private val _backgroundAlpha = MutableStateFlow(themeRepository.getThemeData().backgroundAlpha)
    val backgroundAlpha = _backgroundAlpha.asStateFlow()

    val availableLayouts = keyboardRepository.getAvailableLayouts()
    private val _selectedLayout = MutableStateFlow(settingsRepository.getSelectedLayout())
    val selectedLayout = _selectedLayout.asStateFlow()

    private val _keyboardHeight = MutableStateFlow(settingsRepository.getKeyboardHeight())
    val keyboardHeight = _keyboardHeight.asStateFlow()

    private val _voiceInputEnabled = MutableStateFlow(settingsRepository.isVoiceInputEnabled())
    val voiceInputEnabled = _voiceInputEnabled.asStateFlow()

    private val _oneHandedMode = MutableStateFlow(settingsRepository.getOneHandedMode())
    val oneHandedMode = _oneHandedMode.asStateFlow()

    private val _isFloatingMode = MutableStateFlow(settingsRepository.isFloatingMode())
    val isFloatingMode = _isFloatingMode.asStateFlow()

    fun setLongPressForSymbolsEnabled(enabled: Boolean) {
        settingsRepository.setLongPressForSymbolsEnabled(enabled)
        _longPressForSymbolsEnabled.value = enabled
    }

    fun setToolbarEnabled(enabled: Boolean) {
        settingsRepository.setToolbarEnabled(enabled)
        _toolbarEnabled.value = enabled
    }

    fun setSelectedTheme(name: String) {
        themeRepository.setSelectedTheme(name)
        _selectedTheme.value = name
    }

    fun setBackgroundImageUri(uri: String) {
        themeRepository.setBackgroundImageUri(uri)
    }

    fun setBackgroundAlpha(alpha: Float) {
        themeRepository.setBackgroundAlpha(alpha)
        _backgroundAlpha.value = alpha
    }

    fun setSelectedLayout(name: String) {
        settingsRepository.setSelectedLayout(name)
        _selectedLayout.value = name
    }

    fun setKeyboardHeight(height: Int) {
        settingsRepository.setKeyboardHeight(height)
        _keyboardHeight.value = height
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        settingsRepository.setVoiceInputEnabled(enabled)
        _voiceInputEnabled.value = enabled
    }

    fun setOneHandedMode(mode: String) {
        settingsRepository.setOneHandedMode(mode)
        _oneHandedMode.value = mode
    }

    fun setFloatingMode(enabled: Boolean) {
        settingsRepository.setFloatingMode(enabled)
        _isFloatingMode.value = enabled
    }
}
