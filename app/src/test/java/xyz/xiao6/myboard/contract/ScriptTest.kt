package xyz.xiao6.myboard.contract

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.xiao6.myboard.contract.manifest.LayoutMirrorPolicy
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog
import xyz.xiao6.myboard.contract.manifest.TextDirection
import xyz.xiao6.myboard.contract.state.Script

class ScriptTest {

    @Test
    fun `parses valid four letter script identifiers`() {
        assertEquals(Script.LATN, Script.parse("LATN"))
        assertEquals(Script("QAAA"), Script.parse("QAAA"))
    }

    @Test
    fun `normalizes script identifiers to uppercase`() {
        assertEquals(Script.LATN, Script.parse("latn"))
        assertEquals(Script("QAAA"), Script.parse("qAaA"))
    }

    @Test
    fun `rejects script identifiers with invalid length or characters`() {
        assertNull(Script.parse("LAT"))
        assertNull(Script.parse("LATNN"))
        assertNull(Script.parse("LA7N"))
        assertNull(Script.parse("LAT-"))
    }

    @Test
    fun `exposes standardized kana and hang constants`() {
        assertEquals(Script("KANA"), Script.KANA)
        assertEquals(Script("HANG"), Script.HANG)
    }

    @Test
    fun `keeps unknown valid script identifiers extensible`() {
        assertEquals(Script("QAAA"), Script.parse("QAAA"))
    }

    @Test
    fun `catalog provides explicit rtl metadata only for known scripts`() {
        val arabic = ScriptCatalog[Script.ARAB]

        assertEquals(TextDirection.RTL, arabic?.direction)
        assertEquals(LayoutMirrorPolicy.MIRROR_HORIZONTAL, arabic?.layoutMirror)
        assertEquals("アラビア文字", arabic?.displayNames?.get("ja-JP"))
        assertNull(ScriptCatalog[Script("QAAA")])
    }
}
