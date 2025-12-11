package xyz.xiao6.myboard.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserWordDao {

    @Query("SELECT * FROM user_words WHERE text LIKE :term || '%' ORDER BY frequency DESC")
    suspend fun search(term: String): List<UserWord>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(word: UserWord): Long

    @Query("UPDATE user_words SET frequency = frequency + 1 WHERE text = :text")
    suspend fun incrementFrequency(text: String)

    @Query("SELECT * FROM user_words ORDER BY frequency DESC")
    fun getAll(): List<UserWord>

    @Query("DELETE FROM user_words WHERE id = :id")
    suspend fun delete(id: Int)
}
