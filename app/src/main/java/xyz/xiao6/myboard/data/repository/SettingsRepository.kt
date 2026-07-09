package xyz.xiao6.myboard.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.settings.KeyboardHeightPolicy
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema

/**
 * 设置仓库。单一数据源，协调 DAO 层与上层 ViewModel。
 */
class SettingsRepository(private val dao: SettingsDao) {

    // ===== 通用设置 =====

    val settings: Flow<Map<String, String>> = dao.getAllSettings()
        .map { entities -> entities.associate { it.key to it.stringValue } }

    suspend fun getSetting(key: String): String? = dao.getSetting(key)

    fun observeSetting(key: String): Flow<String?> = dao.observeSetting(key)

    suspend fun updateSetting(key: String, value: String) {
        dao.upsertSetting(SettingsEntity(key = key, stringValue = value))
    }

    suspend fun ensureKeyboardLayoutMetrics(
        screenHeightDp: Int,
        screenWidthDp: Int
    ): KeyboardLayoutMetrics {
        val height = KeyboardHeightPolicy.resolve(
            storedHeight = getSetting(KeyboardHeightPolicy.KEY_HEIGHT),
            screenHeightDp = screenHeightDp
        )
        if (height.shouldPersist) {
            updateSetting(KeyboardHeightPolicy.KEY_HEIGHT, height.heightDp.toString())
        }

        val horizontalInset = KeyboardHeightPolicy.resolveHorizontalInset(
            storedInset = getSetting(KeyboardHeightPolicy.KEY_HORIZONTAL_INSET),
            screenWidthDp = screenWidthDp
        )
        if (horizontalInset.shouldPersist) {
            updateSetting(KeyboardHeightPolicy.KEY_HORIZONTAL_INSET, horizontalInset.insetDp.toString())
        }

        return KeyboardLayoutMetrics(
            heightDp = height.heightDp,
            horizontalInsetDp = horizontalInset.insetDp
        )
    }

    // ===== Toolbar 配置 =====

    val toolbarItems: Flow<List<ToolbarItemEntity>> = dao.getToolbarItems()

    val toolbarLayoutMode: Flow<ToolbarLayoutMode> =
        dao.observeSetting(KEY_TOOLBAR_LAYOUT_MODE).map { raw ->
            try {
                ToolbarLayoutMode.valueOf(raw ?: ToolbarLayoutMode.SCROLLABLE.name)
            } catch (_: IllegalArgumentException) {
                ToolbarLayoutMode.SCROLLABLE
            }
        }

    suspend fun updateToolbarItems(items: List<ToolbarItemEntity>) {
        dao.replaceAllToolbarItems(items)
    }

    suspend fun updateToolbarLayoutMode(mode: ToolbarLayoutMode) {
        updateSetting(KEY_TOOLBAR_LAYOUT_MODE, mode.name)
    }

    // ===== 语言配置（JSON 格式） =====

    /**
     * 获取已启用的语言配置。
     * 格式: { "en-US": ["LATIN_DIRECT"], "zh-CN": ["PINYIN", "SHUANGPIN_ZIRAN"] }
     */
    suspend fun getEnabledLocaleConfigs(): Map<LocaleTag, List<Schema>> {
        val raw = getSetting(KEY_ENABLED_LOCALE_CONFIGS) ?: return defaultLocaleConfigs()
        return parseLocaleConfigs(raw)
    }

    /**
     * 保存语言配置。
     */
    suspend fun setEnabledLocaleConfigs(configs: Map<LocaleTag, List<Schema>>) {
        val json = buildJsonObject {
            configs.forEach { (locale, schemas) ->
                put(locale.value, buildJsonArray {
                    schemas.forEach { add(it.value) }
                })
            }
        }
        updateSetting(KEY_ENABLED_LOCALE_CONFIGS, json.toString())
    }

    /**
     * 添加语言配置。
     */
    suspend fun addLocaleConfig(locale: LocaleTag, schemas: List<Schema>) {
        val configs = getEnabledLocaleConfigs().toMutableMap()
        configs[locale] = schemas
        setEnabledLocaleConfigs(configs)
    }

    /**
     * 移除语言。
     */
    suspend fun removeLocale(locale: LocaleTag) {
        val configs = getEnabledLocaleConfigs().toMutableMap()
        configs.remove(locale)
        setEnabledLocaleConfigs(configs)
    }

    /**
     * 更新某语言的输入方案列表。
     */
    suspend fun updateLocaleSchemas(locale: LocaleTag, schemas: List<Schema>) {
        val configs = getEnabledLocaleConfigs().toMutableMap()
        configs[locale] = schemas
        setEnabledLocaleConfigs(configs)
    }

    // ===== 初始化 =====

    suspend fun initializeDefaults() {
        if (dao.getSettingCount() == 0) {
            DEFAULT_SETTINGS.forEach { (key, value) ->
                dao.upsertSetting(SettingsEntity(key = key, stringValue = value))
            }
        }
        if (dao.getToolbarItemCount() == 0) {
            DEFAULT_TOOLBAR_ITEMS.forEach { dao.upsertToolbarItem(it) }
        }
    }

    companion object {
        const val KEY_TOOLBAR_LAYOUT_MODE = "toolbar_layout_mode"
        const val KEY_ENABLED_LOCALE_CONFIGS = "enabled_locale_configs"

        /** 默认每语言的 schema 配置 */
        private fun defaultLocaleConfigs(): Map<LocaleTag, List<Schema>> = mapOf(
            LocaleTag("en-US") to listOf(Schema("LATIN_DIRECT")),
            LocaleTag("zh-CN") to listOf(Schema("PINYIN"))
        )

        /** 解析 JSON 字符串为 locale → schemas 映射 */
        fun parseLocaleConfigs(raw: String): Map<LocaleTag, List<Schema>> {
            return try {
                val json = Json.parseToJsonElement(raw).jsonObject
                json.mapValues { (_, schemasJson) ->
                    schemasJson.jsonArray.map { Schema(it.jsonPrimitive.content) }
                }.mapKeys { LocaleTag(it.key) }
            } catch (_: Exception) {
                defaultLocaleConfigs()
            }
        }

        private val DEFAULT_SETTINGS = mapOf(
            "current_locale" to "en-US",
            KEY_ENABLED_LOCALE_CONFIGS to """{"en-US":["LATIN_DIRECT"],"zh-CN":["PINYIN"]}""",
            "theme_mode" to "auto",
            "current_theme" to "default",
            "key_font_size" to "18",
            "haptic_feedback" to "true",
            "sound_feedback" to "false",
            "double_space_period" to "true",
            "auto_capitalize" to "true",
            "llm_provider" to "disabled",
            "llm_api_key" to "",
            "llm_endpoint" to "",
            "stt_provider" to "system",
            "onboarding_completed" to "false",
            KEY_TOOLBAR_LAYOUT_MODE to ToolbarLayoutMode.SCROLLABLE.name
        )

        private val DEFAULT_TOOLBAR_ITEMS = listOf(
            ToolbarItemEntity(type = ToolbarItemType.LOCALE_SWITCH.name, enabled = true, sortOrder = 0),
            ToolbarItemEntity(type = ToolbarItemType.THEME_TOGGLE.name, enabled = true, sortOrder = 1),
            ToolbarItemEntity(type = ToolbarItemType.EMOJI.name, enabled = true, sortOrder = 2),
            ToolbarItemEntity(type = ToolbarItemType.SYMBOL.name, enabled = true, sortOrder = 3),
            ToolbarItemEntity(type = ToolbarItemType.CLIPBOARD.name, enabled = true, sortOrder = 4),
            ToolbarItemEntity(type = ToolbarItemType.LAYOUT_SWITCH.name, enabled = true, sortOrder = 5)
        )
    }

    data class KeyboardLayoutMetrics(
        val heightDp: Int,
        val horizontalInsetDp: Int
    )
}
