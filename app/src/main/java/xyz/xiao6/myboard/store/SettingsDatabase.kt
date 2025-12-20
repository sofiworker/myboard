package xyz.xiao6.myboard.store

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "settings")
data class SettingsEntry(
    @PrimaryKey val key: String,
    val value: String,
)

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun getAll(): List<SettingsEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(entry: SettingsEntry)

    @Query("DELETE FROM settings WHERE `key` = :key")
    fun deleteByKey(key: String)

    @Query("DELETE FROM settings")
    fun clearAll()
}

@Database(entities = [SettingsEntry::class], version = 1)
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
                    "myboard_settings.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
