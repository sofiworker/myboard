package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.model.Token
import xyz.xiao6.myboard.util.PinyinSyllableSegmenter

/**
 * Unified pinyin decoder that accepts structured [Token]s:
 * - [Token.Literal] for normal qwerty letters
 * - [Token.SymbolSet]/[Token.WeightedSet] for predictive T9-like input
 *
 * This replaces the need for per-layout decoders (e.g. "FULL" vs "T9").
 */
class TokenPinyinDecoder(
    private val dictionary: DictionaryLookup,
    private val candidateLimit: Int = 50,
) : Decoder, TokenDecoder {
    private val slots: MutableList<List<String>> = ArrayList()

    private var bestGuessLetters: String = ""
    private var lastCandidates: List<String> = emptyList()
    private var lastOptions: List<String> = emptyList()

    override fun onToken(token: Token): DecodeUpdate {
        return when (token) {
            is Token.Literal -> onLiteralToken(token.text)
            is Token.Marker -> onText(token.marker)
            is Token.Sequence -> applySequence(token.tokens)
            is Token.SymbolSet -> onSymbolSet(token.symbols)
            is Token.WeightedSet -> onSymbolSet(token.symbols.sortedByDescending { it.weight }.map { it.ch })
        }
    }

    override fun onText(text: String): DecodeUpdate {
        if (text.isEmpty()) return currentUpdate()

        if (text == "\b") {
            if (slots.isNotEmpty()) {
                slots.removeLast()
                recompute()
                return currentUpdate()
            }
            return DecodeUpdate(commitTexts = listOf("\b"), composingText = "")
        }

        if (text == " ") {
            return if (slots.isNotEmpty()) {
                val commit = lastCandidates.firstOrNull() ?: bestGuessLetters
                clear()
                DecodeUpdate(commitTexts = listOf(commit), composingText = "")
            } else {
                DecodeUpdate(commitTexts = listOf(" "), composingText = "")
            }
        }

        // Treat single-letter text as token input; everything else flushes composing first.
        if (text.length == 1 && text[0].isLetter()) {
            return onToken(Token.Literal(text))
        }

        if (slots.isNotEmpty()) {
            val commit = lastCandidates.firstOrNull() ?: bestGuessLetters
            clear()
            return DecodeUpdate(commitTexts = listOf(commit, text), composingText = "")
        }

        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun onCandidateSelected(text: String): DecodeUpdate {
        clear()
        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun reset(): DecodeUpdate {
        clear()
        return DecodeUpdate(composingText = "")
    }

    private fun applySequence(tokens: List<Token>): DecodeUpdate {
        var last = DecodeUpdate()
        val commits = ArrayList<String>()
        for (t in tokens) {
            last = onToken(t)
            if (last.commitTexts.isNotEmpty()) commits.addAll(last.commitTexts)
        }
        return last.copy(commitTexts = commits)
    }

    private fun onLiteralToken(raw: String): DecodeUpdate {
        if (raw.isBlank()) return currentUpdate()
        if (raw == "\b" || raw == " ") return onText(raw)

        // Break into code points and treat letters as composing; others flush.
        val iter = raw.codePoints().iterator()
        var pendingCommit: MutableList<String>? = null

        while (iter.hasNext()) {
            val cp = iter.nextInt()
            val ch = String(Character.toChars(cp))
            val c = ch.singleOrNull()
            if (c != null && c.isLetter()) {
                slots.add(listOf(c.lowercaseChar().toString()))
                continue
            }

            if (slots.isNotEmpty()) {
                val commit = lastCandidates.firstOrNull() ?: bestGuessLetters
                clear()
                if (commit.isNotBlank()) {
                    if (pendingCommit == null) pendingCommit = ArrayList()
                    pendingCommit!!.add(commit)
                }
            }
            if (pendingCommit == null) pendingCommit = ArrayList()
            pendingCommit!!.add(ch)
        }

        recompute()
        val update = currentUpdate()
        return if (pendingCommit.isNullOrEmpty()) update else update.copy(commitTexts = pendingCommit!!)
    }

    private fun onSymbolSet(symbols: List<String>): DecodeUpdate {
        val options =
            symbols.mapNotNull { it.trim().takeIf(String::isNotBlank) }
                .map { it.lowercase() }
                .distinct()
        if (options.isEmpty()) return currentUpdate()

        slots.add(options)
        recompute()
        return currentUpdate()
    }

    private fun recompute() {
        if (slots.isEmpty()) {
            bestGuessLetters = ""
            lastCandidates = emptyList()
            lastOptions = emptyList()
            return
        }

        val isDeterministic = slots.all { it.size == 1 && it[0].length == 1 }
        if (isDeterministic) {
            val key = slots.joinToString(separator = "") { it[0] }
            bestGuessLetters = key
            lastOptions = emptyList()
            lastCandidates = dictionary.candidates(key, candidateLimit)
            return
        }

        data class PrefixState(val prefix: String, val score: Int)

        var states = listOf(PrefixState(prefix = "", score = 1))
        for (slot in slots) {
            val next = ArrayList<PrefixState>(states.size * slot.size)
            for (s in states) {
                for (opt in slot) {
                    val prefix = s.prefix + opt
                    val score = dictionary.candidates(prefix, 6).size
                    if (score > 0) next.add(PrefixState(prefix, score))
                }
            }
            if (next.isEmpty()) {
                bestGuessLetters = (states.firstOrNull()?.prefix.orEmpty() + slot.firstOrNull().orEmpty())
                lastCandidates = emptyList()
                lastOptions =
                    listOf(PinyinSyllableSegmenter.segmentForDisplay(bestGuessLetters)).filter { it.isNotBlank() }
                return
            }
            next.sortWith(compareByDescending<PrefixState> { it.score }.thenBy { it.prefix })
            states = next.take(128)
        }

        bestGuessLetters = states.firstOrNull()?.prefix.orEmpty()
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
    }

    private fun currentUpdate(): DecodeUpdate {
        return DecodeUpdate(
            candidates = lastCandidates,
            composingText = bestGuessLetters,
            composingOptions = lastOptions,
        )
    }

    private fun clear() {
        slots.clear()
        bestGuessLetters = ""
        lastCandidates = emptyList()
        lastOptions = emptyList()
    }
}

