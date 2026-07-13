package xyz.xiao6.myboard.theme.foundation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeRuntimeMaterialColorsTest {
    @Test
    fun `material color scheme follows theme runtime chrome colors`() {
        val runtime = ThemeRuntimeProvider().resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(
                    paletteId = FoundationPaletteId.MINT,
                    appearanceMode = AppearanceMode.LIGHT
                )
            ),
            systemDark = false
        )

        val scheme = ThemeRuntimeMaterialColors.colorSchemeFor(runtime)

        assertEquals(Color(0xFF00875A.toInt()), scheme.primary)
        assertEquals(Color(0xFFF1F3F4.toInt()), scheme.background)
        assertEquals(Color(0xFFFFFFFF.toInt()), scheme.surface)
    }
}
