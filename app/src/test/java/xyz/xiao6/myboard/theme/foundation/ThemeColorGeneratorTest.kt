package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorGeneratorTest {
    private val generator = ThemeColorGenerator()

    @Test
    fun `default light palette generates readable filled keys`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(),
            variant = ThemeVariant.LIGHT
        )

        assertFalse(doc.dark)
        assertEquals("foundation_gboard_blue_light", doc.id)
        assertEquals("#F1F3F4", doc.colors.background)
        assertEquals("#FFFFFF", doc.keyStyles.getValue(KeyStyleRole.DEFAULT.ref).background)
        assertEquals("#1A73E8", doc.colors.candidateHighlight)
    }

    @Test
    fun `dark variant uses dark surfaces and light text`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(),
            variant = ThemeVariant.DARK
        )

        assertTrue(doc.dark)
        assertEquals("#1E1E1E", doc.colors.background)
        assertEquals("#E8EAED", doc.colors.keyText)
    }

    @Test
    fun `outlined treatment keeps transparent default key and visible function colors`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(keyTreatment = KeyTreatment.OUTLINED),
            variant = ThemeVariant.LIGHT
        )

        assertEquals("#00FFFFFF", doc.keyStyles.getValue(KeyStyleRole.DEFAULT.ref).background)
        assertEquals("#E8EAED", doc.colors.keyFunction)
    }

    @Test
    fun `custom seed changes accent without changing layout related fields`() {
        val blue = generator.generate(FoundationThemeSelection(), ThemeVariant.LIGHT)
        val green = generator.generate(
            FoundationThemeSelection(
                paletteSource = PaletteSource.CUSTOM_SEED,
                customSeedColor = "#00A86B"
            ),
            ThemeVariant.LIGHT
        )

        assertNotEquals(blue.colors.candidateHighlight, green.colors.candidateHighlight)
        assertEquals(blue.keyStyles.keys, green.keyStyles.keys)
    }
}
