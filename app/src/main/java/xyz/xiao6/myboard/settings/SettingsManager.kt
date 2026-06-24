package xyz.xiao6.myboard.settings

import android.content.Context
import android.content.SharedPreferences

/**
 * 设置管理器 - 设置持久化唯一事实源。
 * 所有设置项都在此类中管理，不允许 UI 层维护独立设置副本。
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("myboard_settings", Context.MODE_PRIVATE)

    // ===== Locale / Script / Schema 设置 =====
    
    /** 当前默认 Locale */
    var currentLocale: String
        get() = prefs.getString("current_locale", "en-US") ?: "en-US"
        set(value) = prefs.edit().putString("current_locale", value).apply()

    /** 已启用 Locale 列表 */
    var enabledLocales: Set<String>
        get() = prefs.getStringSet("enabled_locales", setOf("en-US", "zh-CN")) ?: setOf("en-US", "zh-CN")
        set(value) = prefs.edit().putStringSet("enabled_locales", value).apply()

    /** 每个 Locale 的默认 Script（JSON: locale -> script） */
    var defaultScriptPerLocale: String
        get() = prefs.getString("default_script_per_locale", """{"zh-CN":"HANI","en-US":"LATN","ja-JP":"HIRA"}""") ?: "{}"
        set(value) = prefs.edit().putString("default_script_per_locale", value).apply()

    /** 每个 Locale+Script 的默认 Schema（JSON: locale_script -> schema） */
    var defaultSchemaPerLocaleScript: String
        get() = prefs.getString("default_schema_per_locale_script", """{"zh-CN_HANI":"PINYIN","en-US_LATN":"LATIN_DIRECT","ja-JP_HIRA":"ROMAJI"}""") ?: "{}"
        set(value) = prefs.edit().putString("default_schema_per_locale_script", value).apply()

    // ===== 能力开关 =====
    
    /** 是否启用双拼 */
    var doublePinyinEnabled: Boolean
        get() = prefs.getBoolean("double_pinyin_enabled", false)
        set(value) = prefs.edit().putBoolean("double_pinyin_enabled", value).apply()

    /** 是否启用语音输入 */
    var voiceInputEnabled: Boolean
        get() = prefs.getBoolean("voice_input_enabled", false)
        set(value) = prefs.edit().putBoolean("voice_input_enabled", value).apply()

    /** 是否启用手写输入 */
    var handwritingEnabled: Boolean
        get() = prefs.getBoolean("handwriting_enabled", false)
        set(value) = prefs.edit().putBoolean("handwriting_enabled", value).apply()

    // ===== 主题设置 =====
    
    /** 主题模式：auto / light / dark */
    var themeMode: String
        get() = prefs.getString("theme_mode", "auto") ?: "auto"
        set(value) = prefs.edit().putString("theme_mode", value).apply()

    /** 切换夜间模式 */
    fun toggleTheme() {
        themeMode = if (themeMode == "dark") "light" else "dark"
    }

    /** 当前主题 ID */
    var currentTheme: String
        get() = prefs.getString("current_theme", "default") ?: "default"
        set(value) = prefs.edit().putString("current_theme", value).apply()

    // ===== 反馈设置 =====
    
    /** 触觉反馈开关 */
    var hapticFeedback: Boolean
        get() = prefs.getBoolean("haptic_feedback", true)
        set(value) = prefs.edit().putBoolean("haptic_feedback", value).apply()

    /** 声音反馈开关 */
    var soundFeedback: Boolean
        get() = prefs.getBoolean("sound_feedback", false)
        set(value) = prefs.edit().putBoolean("sound_feedback", value).apply()

    // ===== 键盘设置 =====
    
    /** 键盘高度 */
    var keyboardHeight: Int
        get() = prefs.getInt("keyboard_height", 260)
        set(value) = prefs.edit().putInt("keyboard_height", value).apply()

    /** 按键字号 */
    var keyFontSize: Float
        get() = prefs.getFloat("key_font_size", 18f)
        set(value) = prefs.edit().putFloat("key_font_size", value).apply()

    /** 双击空格句号 */
    var doubleSpacePeriod: Boolean
        get() = prefs.getBoolean("double_space_period", true)
        set(value) = prefs.edit().putBoolean("double_space_period", value).apply()

    /** 自动大写 */
    var autoCapitalize: Boolean
        get() = prefs.getBoolean("auto_capitalize", true)
        set(value) = prefs.edit().putBoolean("auto_capitalize", value).apply()

    // ===== LLM / STT 设置 =====
    
    /** LLM 供应商 */
    var llmProvider: String
        get() = prefs.getString("llm_provider", "disabled") ?: "disabled"
        set(value) = prefs.edit().putString("llm_provider", value).apply()

    /** LLM API Key */
    var llmApiKey: String
        get() = prefs.getString("llm_api_key", "") ?: ""
        set(value) = prefs.edit().putString("llm_api_key", value).apply()

    /** LLM Endpoint */
    var llmEndpoint: String
        get() = prefs.getString("llm_endpoint", "") ?: ""
        set(value) = prefs.edit().putString("llm_endpoint", value).apply()

    /** STT 供应商 */
    var sttProvider: String
        get() = prefs.getString("stt_provider", "system") ?: "system"
        set(value) = prefs.edit().putString("stt_provider", value).apply()

    // ===== 首次使用 =====
    
    var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) = prefs.edit().putBoolean("onboarding_completed", value).apply()

    /** 保留兼容旧键名（向后兼容） */
    @Deprecated("使用 currentLocale", ReplaceWith("currentLocale"))
    var currentLanguage: String
        get() = currentLocale
        set(value) { currentLocale = value }

    @Deprecated("使用 enabledLocales", ReplaceWith("enabledLocales"))
    var enabledLanguages: Set<String>
        get() = enabledLocales
        set(value) { enabledLocales = value }

    @Deprecated("使用 themeMode", ReplaceWith("themeMode"))
    var darkMode: String
        get() = themeMode
        set(value) { themeMode = value }

    fun getAllSettings(): Map<String, Any> {
        return mapOf(
            "current_locale" to currentLocale,
            "enabled_locales" to enabledLocales,
            "theme_mode" to themeMode,
            "current_theme" to currentTheme,
            "keyboard_height" to keyboardHeight,
            "key_font_size" to keyFontSize,
            "haptic_feedback" to hapticFeedback,
            "sound_feedback" to soundFeedback,
            "double_space_period" to doubleSpacePeriod,
            "auto_capitalize" to autoCapitalize,
            "double_pinyin_enabled" to doublePinyinEnabled,
            "voice_input_enabled" to voiceInputEnabled,
            "handwriting_enabled" to handwritingEnabled,
            "llm_provider" to llmProvider,
            "stt_provider" to sttProvider,
            "onboarding_completed" to onboardingCompleted
        )
    }
}
