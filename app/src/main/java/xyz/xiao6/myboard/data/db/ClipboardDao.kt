package xyz.xiao6.myboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ClipboardDao {

    @Query("SELECT * FROM clipboard_history ORDER BY timestamp DESC")
    fun getHistory(): Flow<List<ClipboardItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ClipboardItem)

    @Query("DELETE FROM clipboard_history WHERE id = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM clipboard_history")
    suspend fun clear()
}
