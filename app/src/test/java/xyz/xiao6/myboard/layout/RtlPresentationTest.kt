package xyz.xiao6.myboard.layout

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog
import xyz.xiao6.myboard.contract.state.Script

class RtlPresentationTest {

    @Test
    fun `arabic descriptor drives rtl candidates and horizontal keyboard mirroring`() {
        val presentation = checkNotNull(ScriptCatalog[Script.ARAB]).toLayoutPresentation()

        assertTrue(presentation.isRtl)
        assertTrue(presentation.mirrorHorizontal)
    }

    @Test
    fun `latin descriptor keeps ltr candidates and original keyboard geometry`() {
        val presentation = checkNotNull(ScriptCatalog[Script.LATN]).toLayoutPresentation()

        assertFalse(presentation.isRtl)
        assertFalse(presentation.mirrorHorizontal)
    }
}
