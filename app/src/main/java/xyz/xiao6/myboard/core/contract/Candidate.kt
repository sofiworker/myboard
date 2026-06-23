package xyz.xiao6.myboard.core.contract

/**
 * 候选词数据。
 */
data class Candidate(
    val text: String,
    val type: CandidateType = CandidateType.WORD,
    val score: Float = 0f,
    val source: CandidateSource = CandidateSource.SYSTEM
)

enum class CandidateType { WORD, PREFIX, PREDICTION, CORRECTION, LLM }
enum class CandidateSource { SYSTEM, USER, HISTORY, LLM }