package xyz.xiao6.myboard.data.engine

/**
 * 定义了词库来源的标准接口。
 * 任何词库（内置、用户、在线）都应实现此接口。
 */
interface DictionarySource {
    /**
     * 搜索与给定词条匹配的候选词。
     *
     * @param term 要搜索的词条 (例如, 用户的输入 "nihao")。
     * @return 返回一个匹配的候选词列表。
     */
    fun search(term: String): List<Candidate>
}
