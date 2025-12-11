package xyz.xiao6.myboard.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ClipboardItem::class, UserWord::class], version = 2) // Bump version to 2
abstract class AppDatabase : RoomDatabase() {

    abstract fun clipboardDao(): ClipboardDao
    abstract fun userWordDao(): UserWordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myboard_database"
                )
                .fallbackToDestructiveMigration() // For simplicity, we'll use this for now
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
