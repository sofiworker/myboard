package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThemeSeedInputTest {
    @Test
    fun `normalizes valid custom seed input`() {
        assertEquals("#00A86B", ThemeSeedInput.normalizeOrNull("00a86b"))
        assertEquals("#00A86B", ThemeSeedInput.normalizeOrNull("#00A86B"))
    }

    @Test
    fun `rejects partial or invalid custom seed input`() {
        assertNull(ThemeSeedInput.normalizeOrNull("#00A8"))
        assertNull(ThemeSeedInput.normalizeOrNull("#GGGGGG"))
    }
}
