package xyz.xiao6.myboard.theme.foundation

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.theme.ThemeResolverImpl

class FoundationThemeMainFlowTest {
    @Test
    fun `resolver consumes generated foundation theme`() {
        val runtime = ThemeRuntimeProvider().resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(
                    paletteId = FoundationPaletteId.MINT,
                    appearanceMode = AppearanceMode.LIGHT,
                    keyTreatment = KeyTreatment.FILLED
                )
            ),
            systemDark = false
        )
        val resolver = ThemeResolverImpl(runtime.doc)

        val chrome = resolver.resolveChromeColors()
        val action = resolver.resolveKeyStyle(KeyStyleRole.ACTION.ref)

        assertEquals(Color(0xFF00875A.toInt()), chrome.candidateHighlight)
        assertEquals(Color.White, action.textColor)
    }

    @Test
    fun `unknown key style falls back to generated key default`() {
        val runtime = ThemeRuntimeProvider().resolve(AppearanceSettings.default(), systemDark = false)
        val resolver = ThemeResolverImpl(runtime.doc)

        val unknown = resolver.resolveKeyStyle("${KeyStyleRole.DEFAULT.ref}_missing")
        val fallback = resolver.resolveKeyStyle(KeyStyleRole.DEFAULT.ref)

        assertEquals(fallback.background, unknown.background)
        assertEquals(fallback.textColor, unknown.textColor)
    }
}
