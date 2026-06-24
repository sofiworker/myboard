package xyz.xiao6.myboard.dictionary

import xyz.xiao6.myboard.contract.input.Candidate

/**
 * 自适应词频词典。
 * 根据用户使用习惯自动调整词频排序。
 */
class AdaptiveDictionary(
    private val pinyinDictionary: PinyinDictionary,
    private val hotWordCalculator: HotWordCalculator
) {
    /**
     * 用户选择候选后调用，自动调整词频。
     */
    suspend fun onCandidateSelected(candidate: Candidate) {
        pinyinDictionary.updateFrequency(candidate.text, 1)
    }

    /**
     * 获取热词列表（带时间衰减）。
     */
    suspend fun getHotWords(limit: Int = 20): List<Candidate> {
        return hotWordCalculator.calculateHotWords(limit)
    }

    /**
     * 查找候选词。
     */
    suspend fun lookup(query: String, limit: Int = 50): List<Candidate> {
        // 先查热词，再查词典
        val hotWords = hotWordCalculator.calculateHotWords(limit / 4)
        val dictWords = pinyinDictionary.lookup(query, limit)

        // 合并去重
        return (hotWords + dictWords)
            .distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
