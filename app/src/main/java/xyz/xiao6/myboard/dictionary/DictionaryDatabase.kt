package xyz.xiao6.myboard.dictionary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 词典数据库。
 * 系统词典和用户词典分别存储在不同的表中。
 */
@Database(
    entities = [PhraseEntity::class, UserPhraseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun userDictionaryDao(): UserDictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null

        fun getInstance(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DictionaryDatabase::class.java,
                    "myboard_dictionary.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
