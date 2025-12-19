package xyz.xiao6.myboard.decoder

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.model.Token

class TokenPinyinDecoderTest {

    private val dict =
        DictionaryLookup { key, limit ->
            val all = when (key) {
                "n" -> listOf("嗯")
                else -> emptyList()
            }
            all.take(limit)
        }

    @Test
    fun uppercaseLetter_commitsDirectlyInPinyinMode() {
        val decoder = TokenPinyinDecoder(dictionary = dict)

        val update = decoder.onToken(Token.Literal("A"))

        assertEquals(listOf("A"), update.commitTexts)
        assertEquals("", update.composingText)
    }

    @Test
    fun lowercaseLetter_goesToComposingInPinyinMode() {
        val decoder = TokenPinyinDecoder(dictionary = dict)

        val update = decoder.onToken(Token.Literal("a"))

        assertEquals(emptyList<String>(), update.commitTexts)
        assertEquals("a", update.composingText)
    }

    @Test
    fun uppercaseLetter_freezesCurrentSegmentIntoPrefix() {
        val decoder = TokenPinyinDecoder(dictionary = dict)

        decoder.onToken(Token.Literal("n"))
        val update = decoder.onToken(Token.Literal("A"))

        assertEquals(emptyList<String>(), update.commitTexts)
        assertEquals("嗯A", update.composingText)
    }
}
