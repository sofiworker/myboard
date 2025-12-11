package xyz.xiao6.myboard.data.engine

import android.content.Context
import xyz.xiao6.myboard.data.repository.SettingsRepository

class FuzzyPinyinStrategy(context: Context) : SuggestionStrategy {

    private val settingsRepository = SettingsRepository(context)
    private val fuzzyPinyinPairs = listOf(
        "s" to "sh", "c" to "ch", "z" to "zh",
        "an" to "ang", "en" to "eng", "in" to "ing"
    )

    override fun process(term: String, candidates: List<Candidate>): List<Candidate> {
        val enabledFuzzyPairs = fuzzyPinyinPairs.filter { settingsRepository.isFuzzyPinyinEnabled(it.first, it.second) }
        if (enabledFuzzyPairs.isEmpty()) {
            return candidates
        }

        val fuzzyTerms = generateFuzzyTerms(term, enabledFuzzyPairs)
        // In a real implementation, you would use these fuzzyTerms to query the dictionary again.
        // For simplicity, we'll just return the original candidates for now.
        return candidates
    }

    private fun generateFuzzyTerms(term: String, enabledPairs: List<Pair<String, String>>): Set<String> {
        val terms = mutableSetOf(term)
        for (pair in enabledPairs) {
            val newTerms = mutableSetOf<String>()
            for (t in terms) {
                newTerms.add(t.replace(pair.first, pair.second))
                newTerms.add(t.replace(pair.second, pair.first))
            }
            terms.addAll(newTerms)
        }
        return terms
    }
}
