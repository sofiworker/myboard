package xyz.xiao6.myboard.suggest

fun interface SuggestionProvider {
    fun suggest(context: SuggestionContext, limit: Int): List<SuggestionCandidate>
}
