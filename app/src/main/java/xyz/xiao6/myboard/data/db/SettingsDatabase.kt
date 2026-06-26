package xyz.xiao6.myboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity

/**
 * 设置数据库。独立于词典数据库，仅存储应用设置和工具栏配置。
 */
@Database(
    entities = [SettingsEntity::class, ToolbarItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: SettingsDatabase? = null

        fun getInstance(context: Context): SettingsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SettingsDatabase::class.java,
                    "myboard_settings.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
