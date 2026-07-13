package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
