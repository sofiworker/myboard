package xyz.xiao6.myboard.data.engine

/**
 * 一个基于词频对候选词进行排序的建议策略。
 */
class FrequencySuggestionStrategy : SuggestionStrategy {

    /**
     * 对输入的候选词列表按词频进行降序排序。
     *
     * @param term 用户的原始输入 (在此策略中未使用)。
     * @param candidates 未经排序的候选词列表。
     * @return 返回按 `frequency` 字段降序排序后的列表。
     */
    override fun process(term: String, candidates: List<Candidate>): List<Candidate> {
        return candidates.sortedByDescending { it.frequency }
    }
}
