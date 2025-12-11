package xyz.xiao6.myboard.data.engine

/**
 * 建议引擎的核心。
 * 它组合了词库管理器和一系列建议策略，以生成最终的候选词列表。
 */
class SuggestionEngine(
    private val dictionaryManager: DictionaryManager,
    private val strategies: List<SuggestionStrategy>
) {

    /**
     * 获取给定词条的建议。
     *
     * @param term 要为其获取建议的词条。
     * @return 返回一个经过处理和排序的候选词列表。
     */
    fun getSuggestions(term: String): List<Candidate> {
        // 1. 从词库管理器获取原始候选词
        var candidates = dictionaryManager.search(term)

        // 2. 将候选词列表依次传递给每一个策略进行处理
        for (strategy in strategies) {
            candidates = strategy.process(term, candidates)
        }

        return candidates
    }
}
