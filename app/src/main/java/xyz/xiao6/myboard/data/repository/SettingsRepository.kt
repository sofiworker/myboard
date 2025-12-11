package xyz.xiao6.myboard.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun isFuzzyPinyinEnabled(first: String, second: String): Boolean {
        return prefs.getBoolean("fuzzy_pinyin_${first}_$second", false)
    }

    fun setFuzzyPinyinEnabled(first: String, second: String, enabled: Boolean) {
        prefs.edit().putBoolean("fuzzy_pinyin_${first}_$second", enabled).apply()
    }

    fun getSelectedLayout(): String {
        return prefs.getString("selected_layout", "qwerty") ?: "qwerty"
    }

    fun isFloatingMode(): Boolean {
        return prefs.getBoolean("floating_mode", false)
    }

    fun getKeyboardHeight(): Int {
        return prefs.getInt("keyboard_height", 250)
    }

    fun getOneHandedMode(): String {
        return prefs.getString("one_handed_mode", "off") ?: "off"
    }

    fun setLongPressForSymbolsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("long_press_for_symbols", enabled).apply()
    }

    fun setToolbarEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("toolbar_enabled", enabled).apply()
    }

    fun setSelectedLayout(layout: String) {
        prefs.edit().putString("selected_layout", layout).apply()
    }

    fun setKeyboardHeight(height: Int) {
        prefs.edit().putInt("keyboard_height", height).apply()
    }

    fun setVoiceInputEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("voice_input_enabled", enabled).apply()
    }

    fun setOneHandedMode(mode: String) {
        prefs.edit().putString("one_handed_mode", mode).apply()
    }

    fun setFloatingMode(enabled: Boolean) {
        prefs.edit().putBoolean("floating_mode", enabled).apply()
    }

    fun isLongPressForSymbolsEnabled(): Boolean {
        return prefs.getBoolean("long_press_for_symbols", true)
    }

    fun isToolbarEnabled(): Boolean {
        return prefs.getBoolean("toolbar_enabled", true)
    }

    fun isVoiceInputEnabled(): Boolean {
        return prefs.getBoolean("voice_input_enabled", true)
    }
}
