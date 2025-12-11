package xyz.xiao6.myboard.data.engine

/**
 * 管理一个或多个词库源 (DictionarySource)。
 * 它的职责是向所有已注册的词库源查询候选词，并汇总结果。
 */
class DictionaryManager(private vararg val sources: DictionarySource) {

    /**
     * 从所有词库源中搜索与给定词条匹配的候选词。
     *
     * @param term 要搜索的词条。
     * @return 返回一个包含所有词库源结果的、合并后的候选词列表。
     */
    fun search(term: String): List<Candidate> {
        if (term.isBlank()) return emptyList()
        
        // 遍历所有词库源，收集结果，然后用 flatMap 将它们合并成一个单一的列表
        return sources.flatMap { it.search(term) }
    }
}
