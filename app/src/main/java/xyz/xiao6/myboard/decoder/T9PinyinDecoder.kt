package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.util.MLog
import xyz.xiao6.myboard.util.PinyinSyllableSegmenter

/**
 * Predictive T9 pinyin decoder:
 * - Input: digits '2'..'9' from the T9 layout (one press per letter).
 * - Produces candidates by expanding digit->letters combinations with dictionary prefix pruning.
 *
 * Notes:
 * - This implementation is intentionally heuristic (no weights); it prioritizes prefixes that yield more candidates.
 * - Composing text is a "best guess" letter sequence (for display / commit-composing fallback).
 */
class T9PinyinDecoder(
    private val dictionary: DictionaryLookup,
    private val candidateLimit: Int = 50,
) : Decoder {
    private val logTag = "T9PinyinDecoder"

    private val digits = StringBuilder()
    private var bestGuessLetters: String = ""
    private var lastCandidates: List<String> = emptyList()
    private var lastOptions: List<String> = emptyList()

    override fun onText(text: String): DecodeUpdate {
        if (text.isEmpty()) return currentUpdate()

        if (text == "\b") {
            if (digits.isNotEmpty()) {
                digits.deleteCharAt(digits.length - 1)
                recompute()
                return currentUpdate()
            }
            return DecodeUpdate(commitTexts = listOf("\b"), composingText = "")
        }

        if (text == " ") {
            return if (digits.isNotEmpty()) {
                val commit = lastCandidates.firstOrNull() ?: bestGuessLetters.ifBlank { "" }
                digits.clear()
                bestGuessLetters = ""
                lastCandidates = emptyList()
                lastOptions = emptyList()
                DecodeUpdate(commitTexts = listOf(commit), composingText = "")
            } else {
                DecodeUpdate(commitTexts = listOf(" "), composingText = "")
            }
        }

        if (text.length == 1 && text[0] in '2'..'9') {
            digits.append(text[0])
            recompute()
            return currentUpdate()
        }

        if (digits.isNotEmpty()) {
            val commit = lastCandidates.firstOrNull() ?: bestGuessLetters
            digits.clear()
            bestGuessLetters = ""
            lastCandidates = emptyList()
            lastOptions = emptyList()
            return DecodeUpdate(commitTexts = listOf(commit, text), composingText = "")
        }

        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun onCandidateSelected(text: String): DecodeUpdate {
        digits.clear()
        bestGuessLetters = ""
        lastCandidates = emptyList()
        lastOptions = emptyList()
        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun reset(): DecodeUpdate {
        digits.clear()
        bestGuessLetters = ""
        lastCandidates = emptyList()
        lastOptions = emptyList()
        return DecodeUpdate(composingText = "")
    }

    private data class PrefixState(val prefix: String, val score: Int)

    private fun recompute() {
        val d = digits.toString()
        if (d.isBlank()) {
            bestGuessLetters = ""
            lastCandidates = emptyList()
            lastOptions = emptyList()
            return
        }

        var states = listOf(PrefixState(prefix = "", score = 1))
        for (ch in d) {
            val letters = digitLetters(ch)
            if (letters == null) continue
            val next = ArrayList<PrefixState>(states.size * letters.size)
            for (s in states) {
                for (letter in letters) {
                    val p = s.prefix + letter
                    val score = dictionary.candidates(p, 6).size
                    if (score > 0) next.add(PrefixState(p, score))
                }
            }
            if (next.isEmpty()) {
                val fallback = states.firstOrNull()?.prefix.orEmpty() + letters.first()
                bestGuessLetters = fallback
                lastCandidates = emptyList()
                lastOptions = listOf(PinyinSyllableSegmenter.segmentForDisplay(bestGuessLetters)).filter { it.isNotBlank() }
                return
            }
            next.sortWith(compareByDescending<PrefixState> { it.score }.thenBy { it.prefix })
            states = next.take(128)
        }

        val best = states.firstOrNull()
        bestGuessLetters = best?.prefix.orEmpty()

        lastOptions =
            states.take(24)
                .map { it.prefix }
                .distinct()
                .map { PinyinSyllableSegmenter.segmentForDisplay(it) }
                .filter { it.isNotBlank() }

        val out = LinkedHashSet<String>(candidateLimit)
        for (s in states.take(24)) {
            val list = dictionary.candidates(s.prefix, candidateLimit)
            for (w in list) {
                if (out.add(w) && out.size >= candidateLimit) break
            }
            if (out.size >= candidateLimit) break
        }
        lastCandidates = out.toList()
        MLog.d(logTag, "digits='$d' best='$bestGuessLetters' candidates=${lastCandidates.size}")
    }

    private fun currentUpdate(): DecodeUpdate {
        return DecodeUpdate(
            candidates = lastCandidates,
            composingText = bestGuessLetters,
            composingOptions = lastOptions,
        )
    }

    private fun digitLetters(d: Char): CharArray? {
        return when (d) {
            '2' -> charArrayOf('a', 'b', 'c')
            '3' -> charArrayOf('d', 'e', 'f')
            '4' -> charArrayOf('g', 'h', 'i')
            '5' -> charArrayOf('j', 'k', 'l')
            '6' -> charArrayOf('m', 'n', 'o')
            '7' -> charArrayOf('p', 'q', 'r', 's')
            '8' -> charArrayOf('t', 'u', 'v')
            '9' -> charArrayOf('w', 'x', 'y', 'z')
            else -> null
        }
    }
}
