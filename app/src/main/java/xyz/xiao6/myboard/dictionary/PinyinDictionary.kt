package xyz.xiao6.myboard.dictionary

import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.CandidateSource
import xyz.xiao6.myboard.contract.input.CandidateType

/**
 * 拼音词典实现。
 * 结合系统词典和用户词典，提供统一的查找接口。
 */
class PinyinDictionary(
    private val dictionaryDao: DictionaryDao,
    private val userDao: UserDictionaryDao
) {

    /**
     * 根据拼音查找候选词。
     * 用户词典结果权重高于系统词典。
     */
    suspend fun lookup(query: String, limit: Int = 50): List<Candidate> {
        // 1. 先查用户词典（权重 1.5x）
        val userResults = userDao.lookupByPinyin(query, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.WORD,
                score = entity.frequency * 1.5f,
                source = CandidateSource.USER
            )
        }

        // 2. 再查系统词典
        val systemResults = dictionaryDao.lookupByPinyin(query, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.WORD,
                score = entity.frequency.toFloat(),
                source = CandidateSource.SYSTEM
            )
        }

        // 3. 合并去重，按分数排序
        return (userResults + systemResults)
            .distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 按前缀查找（用于联想输入）。
     */
    suspend fun lookupByPrefix(prefix: String, limit: Int = 20): List<Candidate> {
        return dictionaryDao.lookupByPrefix(prefix, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.PREDICTION,
                score = entity.frequency.toFloat(),
                source = CandidateSource.SYSTEM
            )
        }
    }

    /**
     * 添加用户自定义词条。
     */
    suspend fun addPhrase(pinyin: String, phrase: String, frequency: Int = 1) {
        userDao.insert(
            UserPhraseEntity(
                pinyin = pinyin,
                phrase = phrase,
                frequency = frequency
            )
        )
    }

    /**
     * 更新词条词频。
     */
    suspend fun updateFrequency(phrase: String, delta: Int = 1) {
        val now = System.currentTimeMillis()
        userDao.incrementFrequency(phrase, delta, now)
        dictionaryDao.incrementFrequency(phrase, delta)
    }

    /**
     * 删除用户词条。
     */
    suspend fun removeUserPhrase(phrase: String) {
        val all = userDao.getAll()
        val target = all.find { it.phrase == phrase }
        if (target != null) {
            userDao.delete(target)
        }
    }
}
