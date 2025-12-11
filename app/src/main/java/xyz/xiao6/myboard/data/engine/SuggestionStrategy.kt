package xyz.xiao6.myboard.data.engine

/**
 * 定义了建议处理策略的标准接口。
 * 任何排序、过滤或预测算法都应实现此接口。
 */
interface SuggestionStrategy {
    /**
     * 处理一个输入词条，并返回一个经过处理的候选词列表。
     *
     * @param term 用户的原始输入词条。
     * @param candidates 从 DictionarySource 获取的原始候选词列表。
     * @return 返回一个经过排序、过滤或以其他方式处理的候选词列表。
     */
    fun process(term: String, candidates: List<Candidate>): List<Candidate>
}
