package xyz.xiao6.myboard.suggest

import android.content.Context
import xyz.xiao6.myboard.store.SettingsStore
import kotlin.math.min
import kotlin.math.ln

class SuggestionManager(
    context: Context,
    private val prefs: SettingsStore,
) {
    private val userLexicon = UserLexiconStore(context)
    private val nextWordStore = NextWordStore(context)

    private val decoderProvider = SuggestionProvider { ctx, limit ->
        ctx.decoderCandidates
            .take(limit)
            .mapIndexed { idx, text ->
                SuggestionCandidate(
                    text = text,
                    source = SuggestionSource.DECODER,
                    score = 1.0 - idx * 0.001 - lengthPenalty(text),
                )
            }
    }

    private val userProvider = SuggestionProvider { ctx, limit ->
        if (!ctx.learningEnabled || ctx.composingText.isBlank()) return@SuggestionProvider emptyList()
        val matches = userLexicon.getPrefixMatches(ctx.localeTag, ctx.composingText, limit)
        matches.map { (text, freq) ->
            SuggestionCandidate(
                text = text,
                source = SuggestionSource.USER_LEXICON,
                score = userScore(freq, ctx.composingText, text),
            )
        }
    }

    private val historyProvider = SuggestionProvider { ctx, limit ->
        if (!ctx.learningEnabled || ctx.composingText.isNotBlank()) return@SuggestionProvider emptyList()
        val prev = ctx.lastCommittedWord ?: return@SuggestionProvider emptyList()
        nextWordStore.suggest(ctx.localeTag, prev, limit).map { (text, weight) ->
            SuggestionCandidate(
                text = text,
                source = SuggestionSource.HISTORY,
                score = historyScore(weight, text),
                commitText = "$text ",
            )
        }
    }

    private val remoteProvider = SuggestionProvider { _, _ ->
        emptyList()
    }

    private val pipeline = SuggestionPipeline(
        providers = listOf(
            decoderProvider,
            userProvider,
            historyProvider,
            remoteProvider,
        ),
    )

    fun build(context: SuggestionContext, limit: Int): List<SuggestionCandidate> {
        val base =
            if (!context.suggestionEnabled) {
                decoderProvider.suggest(context, limit)
            } else {
                pipeline.build(context, limit)
            }
        return base.filterNot { userLexicon.isBlocked(context.localeTag, it.text) }
    }

    fun onCommittedText(localeTag: String, committedText: String, previousWord: String?) {
        if (!prefs.suggestionLearningEnabled) return
        val words = extractTokens(committedText, useNgram = prefs.suggestionNgramEnabled)
        if (words.isEmpty()) return
        words.forEach { userLexicon.record(localeTag, it) }
        if (!previousWord.isNullOrBlank()) {
            nextWordStore.record(localeTag, previousWord, words.first())
        }
        for (i in 0 until (words.size - 1)) {
            nextWordStore.record(localeTag, words[i], words[i + 1])
        }
    }

    fun blockWord(localeTag: String, word: String) {
        userLexicon.block(localeTag, word)
    }

    fun demoteWord(localeTag: String, word: String) {
        userLexicon.adjust(localeTag, word, delta = -3)
        nextWordStore.demoteWord(localeTag, word, delta = -2)
    }

    fun clearLearning(localeTag: String) {
        userLexicon.clear(localeTag)
        nextWordStore.clear(localeTag)
    }

    fun clearBlocked(localeTag: String) {
        userLexicon.clearBlocked(localeTag)
    }

    private fun extractTokens(text: String, useNgram: Boolean): List<String> {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return emptyList()
        val regex = Regex("[\\p{IsHan}]+|[\\p{L}\\p{N}]+")
        val base = regex.findAll(trimmed).map { it.value }.toList()
        if (!useNgram) return base
        val out = ArrayList<String>(base.size * 2)
        for (token in base) {
            out.add(token)
            if (token.any { it in '\u4E00'..'\u9FFF' }) {
                out.addAll(expandHanNgrams(token, minGram = 2, maxGram = 3))
            }
        }
        return out
    }

    private fun expandHanNgrams(text: String, minGram: Int, maxGram: Int): List<String> {
        val chars = text.codePoints().toArray().map { cp -> String(Character.toChars(cp)) }
        if (chars.size < minGram) return emptyList()
        val out = ArrayList<String>()
        for (n in minGram..maxGram) {
            if (chars.size < n) continue
            for (i in 0..(chars.size - n)) {
                val seg = StringBuilder()
                for (j in i until (i + n)) seg.append(chars[j])
                out.add(seg.toString())
            }
        }
        return out
    }

    private fun userScore(freq: Int, composing: String, text: String): Double {
        val freqBoost = ln((freq.coerceAtLeast(1) + 1).toDouble()) / 6.0
        val prefixBoost = if (composing.isNotBlank() && text.startsWith(composing)) 0.1 else 0.0
        return 0.8 + freqBoost + prefixBoost - lengthPenalty(text)
    }

    private fun historyScore(weight: Double, text: String): Double {
        val freqBoost = ln((weight.coerceAtLeast(0.1) + 1.0)) / 7.0
        return 0.6 + freqBoost - lengthPenalty(text)
    }

    private fun lengthPenalty(text: String): Double {
        return text.length * 0.0005
    }
}
