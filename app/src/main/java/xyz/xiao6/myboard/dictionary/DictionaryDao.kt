package xyz.xiao6.myboard.dictionary

import androidx.room.*

/**
 * 系统词典 DAO 接口。
 */
@Dao
interface DictionaryDao {
    @Query("SELECT * FROM phrases WHERE pinyin = :pinyin ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPinyin(pinyin: String, limit: Int = 50): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE pinyin LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: PhraseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<PhraseEntity>)

    @Update
    suspend fun update(phrase: PhraseEntity)

    @Query("UPDATE phrases SET frequency = frequency + :delta WHERE phrase = :phrase")
    suspend fun incrementFrequency(phrase: String, delta: Int = 1)

    @Query("SELECT * FROM phrases ORDER BY frequency DESC LIMIT :limit")
    suspend fun getHotWords(limit: Int = 20): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE last_used_at > :since ORDER BY frequency DESC LIMIT :limit")
    suspend fun getRecentWords(since: Long, limit: Int = 20): List<PhraseEntity>

    @Query("DELETE FROM phrases WHERE last_used_at < :before AND type = 0")
    suspend fun cleanupOldSystemWords(before: Long)

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun count(): Int
}
