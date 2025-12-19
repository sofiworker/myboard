package xyz.xiao6.myboard.decoder

import org.junit.Assert.assertEquals
import org.junit.Test

class PinyinKeyNormalizerTest {
    @Test
    fun normalize_removesSpacesAndApostrophes() {
        assertEquals("nihao", normalizePinyinKey("ni'hao"))
        assertEquals("nihao", normalizePinyinKey("ni hao"))
        assertEquals("nihao", normalizePinyinKey("  N i  H a o  "))
    }

    @Test
    fun normalize_filtersNonLetters() {
        assertEquals("a", normalizePinyinKey("异维A酸"))
        assertEquals("asuan", normalizePinyinKey("异维A酸suan"))
        assertEquals("nihao", normalizePinyinKey("你ni好hao"))
    }
}
