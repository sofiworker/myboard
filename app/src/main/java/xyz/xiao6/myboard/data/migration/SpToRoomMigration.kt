package xyz.xiao6.myboard.data.migration

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.settings.KeyboardHeightPolicy

/**
 * 从 SharedPreferences 迁移数据到 Room。
 * 迁移完成后标记已完成，后续启动不再重复迁移。
 */
class SpToRoomMigration(
    private val context: Context,
    private val dao: SettingsDao
) {
    /**
     * 执行一次性迁移。如果已迁移过则直接返回。
     */
    suspend fun migrateIfNeeded() = withContext(Dispatchers.IO) {
        val migrationKey = "sp_migration_completed"
        if (dao.getSetting(migrationKey) != null) return@withContext

        val sp: SharedPreferences = context.getSharedPreferences(
            "myboard_settings", Context.MODE_PRIVATE
        )

        // 迁移通用设置
        MIGRATION_KEYS.forEach { key ->
            val value = sp.getString(key, null)
            if (value != null) {
                dao.upsertSetting(SettingsEntity(key = key, stringValue = value))
            }
        }

        // 迁移布尔值（SharedPreferences 存储为 Boolean，需转为 String）
        BOOLEAN_KEYS.forEach { key ->
            if (sp.contains(key)) {
                dao.upsertSetting(SettingsEntity(key = key, stringValue = sp.getBoolean(key, false).toString()))
            }
        }

        // 迁移 Int
        INT_KEYS.forEach { key ->
            if (sp.contains(key)) {
                dao.upsertSetting(SettingsEntity(key = key, stringValue = sp.getInt(key, 0).toString()))
            }
        }

        // 迁移 Float
        FLOAT_KEYS.forEach { key ->
            if (sp.contains(key)) {
                dao.upsertSetting(SettingsEntity(key = key, stringValue = sp.getFloat(key, 0f).toString()))
            }
        }

        // 迁移 Set<String>
        SET_KEYS.forEach { key ->
            if (sp.contains(key)) {
                val set = sp.getStringSet(key, emptySet()) ?: emptySet()
                dao.upsertSetting(SettingsEntity(key = key, stringValue = set.joinToString(",")))
            }
        }

        // 初始化 toolbar 默认配置
        if (dao.getToolbarItemCount() == 0) {
            DEFAULT_TOOLBAR.forEach { entity ->
                dao.upsertToolbarItem(entity)
            }
        }

        // 初始化 toolbar_layout_mode（旧 SP 中不存在此 key）
        if (dao.getSetting("toolbar_layout_mode") == null) {
            dao.upsertSetting(SettingsEntity(key = "toolbar_layout_mode", stringValue = "SCROLLABLE"))
        }

        // 标记迁移完成
        dao.upsertSetting(SettingsEntity(key = migrationKey, stringValue = "true"))
    }

    companion object {
        private val MIGRATION_KEYS = listOf(
            "current_locale", "theme_mode", "current_theme",
            "llm_provider", "llm_api_key", "llm_endpoint",
            "stt_provider", "default_script_per_locale",
            "default_schema_per_locale_script"
        )

        private val BOOLEAN_KEYS = listOf(
            "double_pinyin_enabled", "voice_input_enabled", "handwriting_enabled",
            "haptic_feedback", "sound_feedback", "double_space_period",
            "auto_capitalize", "onboarding_completed"
        )

        private val INT_KEYS = listOf(
            KeyboardHeightPolicy.KEY_HEIGHT,
            KeyboardHeightPolicy.KEY_HORIZONTAL_INSET
        )

        private val FLOAT_KEYS = listOf("key_font_size")

        private val SET_KEYS = listOf("enabled_locales")

        private val DEFAULT_TOOLBAR = listOf(
            ToolbarItemEntity(type = ToolbarItemType.LOCALE_SWITCH.name, enabled = true, sortOrder = 0),
            ToolbarItemEntity(type = ToolbarItemType.THEME_TOGGLE.name, enabled = true, sortOrder = 1),
            ToolbarItemEntity(type = ToolbarItemType.EMOJI.name, enabled = true, sortOrder = 2),
            ToolbarItemEntity(type = ToolbarItemType.SYMBOL.name, enabled = true, sortOrder = 3),
            ToolbarItemEntity(type = ToolbarItemType.CLIPBOARD.name, enabled = true, sortOrder = 4),
            ToolbarItemEntity(type = ToolbarItemType.LAYOUT_SWITCH.name, enabled = true, sortOrder = 5)
        )
    }
}
