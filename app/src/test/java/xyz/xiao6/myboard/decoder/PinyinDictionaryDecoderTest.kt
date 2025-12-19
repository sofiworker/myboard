package xyz.xiao6.myboard.decoder

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinDictionaryDecoderTest {
    @Test
    fun full_flow() {
        val lookup = DictionaryLookup { key, limit ->
            val all = when (key) {
                "n" -> listOf("嗯")
                "ni" -> listOf("你", "尼")
                else -> emptyList()
            }
            all.take(limit)
        }
        val decoder = TokenPinyinDecoder(lookup, candidateLimit = 10)

        val u1 = decoder.onText("n")
        assertEquals(emptyList<String>(), u1.commitTexts)
        assertEquals(listOf("嗯"), u1.candidates)

        val u2 = decoder.onText("i")
        assertEquals(emptyList<String>(), u2.commitTexts)
        assertEquals(listOf("你", "尼"), u2.candidates)

        val update = decoder.onText(" ")
        assertEquals(listOf("你"), update.commitTexts)
        assertEquals(emptyList<String>(), update.candidates)
    }

    @Test
    fun backspace_deletes_composing_then_emits_raw_backspace() {
        val lookup = DictionaryLookup { _, _ -> emptyList() }
        val decoder = TokenPinyinDecoder(lookup)

        decoder.onText("a")
        assertEquals(emptyList<String>(), decoder.onText("\b").commitTexts)
        assertEquals(listOf("\b"), decoder.onText("\b").commitTexts)
    }
}
