package xyz.xiao6.myboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import xyz.xiao6.myboard.contract.state.PanelType

class PanelLayoutResolverTest {

    @Test
    fun `symbol and emoji panels resolve to full surface layout ids`() {
        assertEquals("symbols_full_surface", PanelLayoutResolver.layoutIdFor(PanelType.SYMBOL))
        assertEquals("emoji_full_surface", PanelLayoutResolver.layoutIdFor(PanelType.EMOJI))
    }

    @Test
    fun `non layout backed panels keep their existing compose panels`() {
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.NONE))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.CLIPBOARD))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.KAOMOJI))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.LOCALE_SWITCH))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.LAYOUT_SWITCH))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.LLM))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.STT))
        assertNull(PanelLayoutResolver.layoutIdFor(PanelType.TEXT_EXPANSION))
    }
}
