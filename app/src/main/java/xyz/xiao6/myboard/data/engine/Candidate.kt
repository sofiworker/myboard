package xyz.xiao6.myboard.data.engine

/**
 * 表示一个候选词及其元数据。
 *
 * @property text 候选词的文本。
 * @property source 候选词的来源 (例如, "内置词库", "用户词典")。
 * @property frequency 词频或其他相关性分数。
 */
data class Candidate(
    val text: String,
    val source: String,
    val frequency: Double = 0.0
)
