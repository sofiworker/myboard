package xyz.xiao6.myboard.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity

/**
 * 设置数据库 DAO。
 * 提供通用设置和工具栏配置的 CRUD 操作。
 */
@Dao
interface SettingsDao {

    // ===== 通用设置 =====

    @Query("SELECT * FROM settings")
    fun getAllSettings(): Flow<List<SettingsEntity>>

    @Query("SELECT stringValue FROM settings WHERE `key` = :key")
    suspend fun getSetting(key: String): String?

    @Query("SELECT stringValue FROM settings WHERE `key` = :key")
    fun observeSetting(key: String): Flow<String?>

    @Upsert
    suspend fun upsertSetting(entity: SettingsEntity)

    @Transaction
    suspend fun upsertSettings(entities: List<SettingsEntity>) {
        entities.forEach { upsertSetting(it) }
    }

    @Query("SELECT COUNT(*) FROM settings")
    suspend fun getSettingCount(): Int

    // ===== Toolbar 配置 =====

    @Query("SELECT * FROM toolbar_items ORDER BY sortOrder ASC")
    fun getToolbarItems(): Flow<List<ToolbarItemEntity>>

    @Upsert
    suspend fun upsertToolbarItem(entity: ToolbarItemEntity)

    @Query("DELETE FROM toolbar_items WHERE type = :type")
    suspend fun deleteToolbarItem(type: String)

    @Query("DELETE FROM toolbar_items")
    suspend fun deleteAllToolbarItems()

    @Query("SELECT COUNT(*) FROM toolbar_items")
    suspend fun getToolbarItemCount(): Int

    @Transaction
    suspend fun replaceAllToolbarItems(items: List<ToolbarItemEntity>) {
        deleteAllToolbarItems()
        items.forEach { upsertToolbarItem(it) }
    }
}
