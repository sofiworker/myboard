package xyz.xiao6.myboard.suggest

class SuggestionPipeline(
    private val providers: List<SuggestionProvider>,
) {
    fun build(context: SuggestionContext, limit: Int): List<SuggestionCandidate> {
        val max = limit.coerceAtLeast(1)
        val merged = LinkedHashMap<String, SuggestionCandidate>(max * 2)
        for (provider in providers) {
            val list = provider.suggest(context, max)
            for (cand in list) {
                val key = cand.text
                val existing = merged[key]
                if (existing == null || cand.score > existing.score) {
                    merged[key] = cand
                }
            }
            if (merged.size >= max) break
        }
        return merged.values
            .sortedWith(
                compareByDescending<SuggestionCandidate> { it.score }
                    .thenBy { it.text },
            )
            .take(max)
    }
}
