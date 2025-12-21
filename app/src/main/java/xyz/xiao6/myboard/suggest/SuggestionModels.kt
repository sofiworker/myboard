package xyz.xiao6.myboard.suggest

enum class SuggestionSource {
    DECODER,
    USER_LEXICON,
    HISTORY,
    REMOTE,
}

data class SuggestionCandidate(
    val text: String,
    val source: SuggestionSource,
    val score: Double,
    val commitText: String? = null,
)

data class SuggestionContext(
    val composingText: String,
    val decoderCandidates: List<String>,
    val lastCommittedWord: String?,
    val localeTag: String,
    val suggestionEnabled: Boolean,
    val learningEnabled: Boolean,
)
