package xyz.xiao6.myboard.theme.skin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.theme.foundation.FeedbackTokenId
import xyz.xiao6.myboard.theme.foundation.KeyStyleRole
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

class PureFlatSkinTest {

    @Test
    fun `meta declares locked pure token skin`() {
        assertEquals(SkinThemeId.PURE_FLAT, PureFlatSkin.meta.id)
        assertEquals(SkinColorPolicy.LOCKED, PureFlatSkin.meta.colorPolicy)
        assertFalse(PureFlatSkin.meta.usesImages)
        assertFalse(PureFlatSkin.meta.usesDecorations)
        assertFalse(PureFlatSkin.meta.usesLayoutBindings)
    }

    @Test
    fun `light variant matches design tokens`() {
        val doc = PureFlatSkin.themeDoc(ThemeVariant.LIGHT)

        assertEquals(SkinThemeId.PURE_FLAT.id, doc.id)
        assertFalse(doc.dark)
        assertEquals("#FFFFFF", doc.colors.background)
        assertEquals("#F0F0F0", doc.colors.keyDefault)
        assertEquals("#F0F0F0", doc.colors.keyFunction)
        assertEquals("#FF3B30", doc.colors.keyAction)
        assertEquals("#FFFFFF", doc.colors.keyActionText)
        assertEquals("#FF3B30", doc.colors.candidateHighlight)

        val action = doc.keyStyles.getValue(KeyStyleRole.ACTION.ref)
        assertEquals(16f, action.cornerRadius)
        assertEquals("#FF3B30", action.background)

        val space = doc.keyStyles.getValue(KeyStyleRole.SPACE.ref)
        assertEquals(18f, space.cornerRadius)
    }

    @Test
    fun `dark variant keeps red action accent`() {
        val doc = PureFlatSkin.themeDoc(ThemeVariant.DARK)

        assertTrue(doc.dark)
        assertEquals("#121212", doc.colors.background)
        assertEquals("#2A2A2A", doc.colors.keyDefault)
        assertEquals("#FF3B30", doc.colors.keyAction)
        assertEquals("#FFFFFF", doc.colors.keyActionText)
        assertEquals("#FF3B30", doc.colors.candidateHighlight)
    }

    @Test
    fun `feedback tokens use protocol ids`() {
        val doc = PureFlatSkin.themeDoc(ThemeVariant.LIGHT)
        assertTrue(doc.feedback.haptic.containsKey(FeedbackTokenId.KEY_TAP.ref))
        assertTrue(doc.feedback.sound.containsKey(FeedbackTokenId.KEY_ACTION.ref))
        assertEquals(
            FeedbackTokenId.KEY_SPACE.soundResName,
            doc.feedback.sound.getValue(FeedbackTokenId.KEY_SPACE.ref).soundResName
        )
    }
}
