package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.theme.skin.SkinThemeId

class ThemeRuntimeProviderTest {
    private val provider = ThemeRuntimeProvider()

    @Test
    fun `follow system selects dark when system is dark`() {
        val runtime = provider.resolve(AppearanceSettings.default(), systemDark = true)

        assertEquals(ThemeVariant.DARK, runtime.variant)
        assertTrue(runtime.doc.dark)
        assertNull(runtime.skinThemeId)
    }

    @Test
    fun `explicit light ignores dark system`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(appearanceMode = AppearanceMode.LIGHT)
            ),
            systemDark = true
        )

        assertEquals(ThemeVariant.LIGHT, runtime.variant)
        assertFalse(runtime.doc.dark)
    }

    @Test
    fun `system dynamic falls back to preset when dynamic seed is absent`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(paletteSource = PaletteSource.SYSTEM_DYNAMIC)
            ),
            systemDark = false,
            dynamicSeedColor = null
        )

        assertEquals("#1A73E8", runtime.doc.colors.candidateHighlight)
    }

    @Test
    fun `system dynamic uses provided dynamic seed`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(paletteSource = PaletteSource.SYSTEM_DYNAMIC)
            ),
            systemDark = false,
            dynamicSeedColor = "#FF5722"
        )

        assertEquals("#FF5722", runtime.doc.colors.candidateHighlight)
    }

    @Test
    fun `pure flat locked skin overrides foundation palette`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(
                    paletteSource = PaletteSource.PRESET,
                    paletteId = FoundationPaletteId.GBOARD_BLUE,
                    appearanceMode = AppearanceMode.LIGHT
                ),
                skinThemeId = SkinThemeId.PURE_FLAT.id
            ),
            systemDark = false
        )

        assertEquals(SkinThemeId.PURE_FLAT.id, runtime.skinThemeId)
        assertEquals(SkinThemeId.PURE_FLAT.id, runtime.doc.id)
        assertEquals("#FF3B30", runtime.doc.colors.keyAction)
        assertEquals("#FFFFFF", runtime.doc.colors.background)
        assertEquals("#F0F0F0", runtime.doc.colors.keyDefault)
        assertNotNull(runtime.foundationDoc)
        assertNotEquals(runtime.doc.colors.keyAction, runtime.foundationDoc!!.colors.keyAction)
    }

    @Test
    fun `pure flat dark variant keeps locked red action`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(appearanceMode = AppearanceMode.DARK),
                skinThemeId = SkinThemeId.PURE_FLAT.id
            ),
            systemDark = false
        )

        assertEquals(ThemeVariant.DARK, runtime.variant)
        assertTrue(runtime.doc.dark)
        assertEquals("#FF3B30", runtime.doc.colors.keyAction)
        assertEquals("#121212", runtime.doc.colors.background)
    }

    @Test
    fun `unknown skin id falls back to foundation`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(appearanceMode = AppearanceMode.LIGHT),
                skinThemeId = "missing_skin"
            ),
            systemDark = false
        )

        assertNull(runtime.skinThemeId)
        assertNotEquals("missing_skin", runtime.doc.id)
        assertEquals("#1A73E8", runtime.doc.colors.candidateHighlight)
    }
}
