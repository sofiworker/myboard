package xyz.xiao6.myboard.dictionary

import androidx.room.*

/**
 * 用户词典 DAO 接口。
 */
@Dao
interface UserDictionaryDao {
    @Query("SELECT * FROM user_phrases WHERE pinyin = :pinyin ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPinyin(pinyin: String, limit: Int = 50): List<UserPhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: UserPhraseEntity)

    @Update
    suspend fun update(phrase: UserPhraseEntity)

    @Query("UPDATE user_phrases SET frequency = frequency + :delta, last_used_at = :now WHERE phrase = :phrase")
    suspend fun incrementFrequency(phrase: String, delta: Int = 1, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(phrase: UserPhraseEntity)

    @Query("SELECT * FROM user_phrases ORDER BY frequency DESC")
    suspend fun getAll(): List<UserPhraseEntity>

    @Query("SELECT COUNT(*) FROM user_phrases")
    suspend fun count(): Int
}
