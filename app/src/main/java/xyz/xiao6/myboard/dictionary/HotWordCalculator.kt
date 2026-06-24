package xyz.xiao6.myboard.dictionary

import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.CandidateSource
import xyz.xiao6.myboard.contract.input.CandidateType
import kotlin.math.exp

/**
 * 热词计算器。
 * 基于时间衰减算法，最近使用过的词权重更高。
 */
class HotWordCalculator(private val dictionaryDao: DictionaryDao) {

    companion object {
        private const val ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L
        private const val DECAY_HALF_LIFE_HOURS = 168.0 // 一周
    }

    /**
     * 计算热词列表。
     * 使用指数时间衰减：score = frequency * exp(-hoursSinceUse / halfLife)
     */
    suspend fun calculateHotWords(limit: Int = 20): List<Candidate> {
        val now = System.currentTimeMillis()
        val recentWords = dictionaryDao.getRecentWords(now - ONE_WEEK_MS, limit * 2)

        return recentWords
            .map { entity ->
                val hoursSinceUse = (now - entity.lastUsedAt) / 3_600_000.0
                val timeDecay = exp(-hoursSinceUse / DECAY_HALF_LIFE_HOURS)
                val hotScore = entity.frequency * timeDecay

                Candidate(
                    text = entity.phrase,
                    type = CandidateType.WORD,
                    score = hotScore.toFloat(),
                    source = CandidateSource.HISTORY
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
