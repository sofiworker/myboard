package xyz.xiao6.myboard.core.dictionary

import xyz.xiao6.myboard.core.keyboard.Candidate
import xyz.xiao6.myboard.core.keyboard.CandidateSource
import xyz.xiao6.myboard.core.keyboard.CandidateType

/**
 * 联想引擎：多源聚合联想。
 */
class SuggestionEngine {
    private val primaryDict = TrieDict()
    private val userWords = mutableMapOf<String, Long>()

    fun loadDictionary(words: List<Pair<String, Long>>) {
        for ((word, freq) in words) {
            primaryDict.insert(word, freq)
        }
    }

    fun suggest(prefix: String, maxResults: Int = 10): List<Candidate> {
        if (prefix.isBlank()) return emptyList()

        val results = mutableListOf<Candidate>()

        // 前缀匹配
        val prefixMatches = primaryDict.prefixSearch(prefix)
            .map { Candidate(it.word, CandidateType.WORD, it.frequency.toFloat(), CandidateSource.SYSTEM) }
        results.addAll(prefixMatches)

        // 用户词典
        val userMatches = userWords.entries
            .filter { it.key.startsWith(prefix, ignoreCase = true) }
            .map { Candidate(it.key, CandidateType.WORD, it.value.toFloat() + 1000, CandidateSource.USER) }
        results.addAll(userMatches)

        return results.distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    fun recordWord(word: String) {
        userWords[word] = (userWords[word] ?: 0) + 1
    }

    fun fuzzySearch(word: String, maxDistance: Int = 2): List<Candidate> {
        return primaryDict.fuzzySearch(word, maxDistance)
            .map { Candidate(it.word, CandidateType.CORRECTION, it.frequency.toFloat(), CandidateSource.SYSTEM) }
    }
}
