package xyz.xiao6.myboard.core.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理器。
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("myboard_settings", Context.MODE_PRIVATE)

    // 语言设置
    var currentLanguage: String
        get() = prefs.getString("current_language", "en_us") ?: "en_us"
        set(value) = prefs.edit().putString("current_language", value).apply()

    var enabledLanguages: Set<String>
        get() = prefs.getStringSet("enabled_languages", setOf("en_us", "zh_cn")) ?: setOf("en_us", "zh_cn")
        set(value) = prefs.edit().putStringSet("enabled_languages", value).apply()

    // 主题设置
    var currentTheme: String
        get() = prefs.getString("current_theme", "default") ?: "default"
        set(value) = prefs.edit().putString("current_theme", value).apply()

    var darkMode: String
        get() = prefs.getString("dark_mode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("dark_mode", value).apply()

    // 键盘设置
    var keyboardHeight: Int
        get() = prefs.getInt("keyboard_height", 260)
        set(value) = prefs.edit().putInt("keyboard_height", value).apply()

    var keyFontSize: Float
        get() = prefs.getFloat("key_font_size", 18f)
        set(value) = prefs.edit().putFloat("key_font_size", value).apply()

    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    var soundFeedback: Boolean
        get() = prefs.getBoolean("sound_feedback", false)
        set(value) = prefs.edit().putBoolean("sound_feedback", value).apply()

    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean("double_space_period", true)
        set(value) = prefs.edit().putBoolean("double_space_period", value).apply()

    var autoCapitalize: Boolean
        get() = prefs.getBoolean("auto_capitalize", true)
        set(value) = prefs.edit().putBoolean("auto_capitalize", value).apply()

    // LLM 设置
    var llmProvider: String
        get() = prefs.getString("llm_provider", "disabled") ?: "disabled"
        set(value) = prefs.edit().putString("llm_provider", value).apply()

    var llmApiKey: String
        get() = prefs.getString("llm_api_key", "") ?: ""
        set(value) = prefs.edit().putString("llm_api_key", value).apply()

    var llmEndpoint: String
        get() = prefs.getString("llm_endpoint", "") ?: ""
        set(value) = prefs.edit().putString("llm_endpoint", value).apply()

    // STT 设置
    var sttProvider: String
        get() = prefs.getString("stt_provider", "system") ?: "system"
        set(value) = prefs.edit().putString("stt_provider", value).apply()

    // 首次使用
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "current_language" to currentLanguage,
            "enabled_languages" to enabledLanguages,
            "current_theme" to currentTheme,
            "dark_mode" to darkMode,
            "keyboard_height" to keyboardHeight,
            "key_font_size" to keyFontSize,
            "haptic_feedback" to hapticFeedback,
            "sound_feedback" to soundFeedback,
            "double_space_period" to doubleSpacePeriod,
            "auto_capitalize" to autoCapitalize,
            "llm_provider" to llmProvider,
            "stt_provider" to sttProvider,
            "onboarding_completed" to onboardingCompleted
        )
    }
}
