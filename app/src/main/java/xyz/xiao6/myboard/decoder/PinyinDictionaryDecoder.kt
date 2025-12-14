package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.util.MLog

/**
 * Minimal pinyin decoder:
 * - Letters build a composing search key (lowercased).
 * - Backspace deletes composing first; otherwise emits "\b".
 * - Space selects the top candidate when composing is non-empty; otherwise emits " ".
 * - Non-letter flushes composing first, then emits the original text.
 */
class PinyinDictionaryDecoder(
    private val dictionary: DictionaryLookup,
    private val candidateLimit: Int = 50,
) : Decoder {
    private val logTag = "PinyinDecoder"
    private val composing = StringBuilder()

    override fun onText(text: String): DecodeUpdate {
        if (text.isEmpty()) return DecodeUpdate(composingText = composing.toString())

        if (text == "\b") {
            if (composing.isNotEmpty()) {
                composing.deleteCharAt(composing.length - 1)
                return DecodeUpdate(candidates = query(), composingText = composing.toString())
            }
            return DecodeUpdate(commitTexts = listOf("\b"), composingText = "")
        }

        if (text == " ") {
            return if (composing.isNotEmpty()) {
                val commit = selectTopOrRaw()
                composing.clear()
                DecodeUpdate(commitTexts = listOf(commit), composingText = "")
            } else {
                DecodeUpdate(commitTexts = listOf(" "), composingText = "")
            }
        }

        if (text.length == 1 && text[0].isLetter()) {
            composing.append(text.lowercase())
            val key = composing.toString()
            val candidates = query()
            MLog.d(logTag, "onText='$text' composing='$key' -> ${candidates.size} candidates")
            return DecodeUpdate(candidates = candidates, composingText = key)
        }

        if (composing.isNotEmpty()) {
            val commit = selectTopOrRaw()
            composing.clear()
            return DecodeUpdate(commitTexts = listOf(commit, text), composingText = "")
        }

        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun onCandidateSelected(text: String): DecodeUpdate {
        composing.clear()
        return DecodeUpdate(commitTexts = listOf(text), composingText = "")
    }

    override fun reset(): DecodeUpdate {
        composing.clear()
        return DecodeUpdate(composingText = "")
    }

    private fun query(): List<String> {
        if (composing.isEmpty()) return emptyList()
        return dictionary.candidates(composing.toString(), candidateLimit)
    }

    private fun selectTopOrRaw(): String {
        val key = composing.toString()
        val cands = dictionary.candidates(key, 1)
        return cands.firstOrNull() ?: key
    }
}
