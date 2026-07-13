package xyz.xiao6.myboard.theme.foundation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoundationThemeModelsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `default appearance settings encode and decode`() {
        val encoded = json.encodeToString(AppearanceSettings.default())
        val decoded = json.decodeFromString<AppearanceSettings>(encoded)

        assertEquals(PaletteSource.PRESET, decoded.foundation.paletteSource)
        assertEquals(FoundationPaletteId.GBOARD_BLUE, decoded.foundation.paletteId)
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, decoded.foundation.appearanceMode)
        assertEquals(KeyTreatment.FILLED, decoded.foundation.keyTreatment)
        assertNull(decoded.skinThemeId)
    }

    @Test
    fun `custom seed source keeps selected color`() {
        val settings = AppearanceSettings(
            foundation = FoundationThemeSelection(
                paletteSource = PaletteSource.CUSTOM_SEED,
                customSeedColor = "#00A86B",
                keyTreatment = KeyTreatment.OUTLINED,
                cornerStyle = CornerStyle.PILL
            )
        )

        val decoded = json.decodeFromString<AppearanceSettings>(json.encodeToString(settings))

        assertEquals(PaletteSource.CUSTOM_SEED, decoded.foundation.paletteSource)
        assertEquals("#00A86B", decoded.foundation.customSeedColor)
        assertEquals(KeyTreatment.OUTLINED, decoded.foundation.keyTreatment)
        assertEquals(CornerStyle.PILL, decoded.foundation.cornerStyle)
    }

    @Test
    fun `style and feedback ids are referenced through protocol enums`() {
        assertEquals(KeyStyleRole.DEFAULT, KeyStyleRole.fromRef(KeyStyleRole.DEFAULT.ref))
        assertEquals(KeyStyleRole.ACTION, KeyStyleRole.fromRef(KeyStyleRole.ACTION.ref))
        assertEquals(FeedbackTokenId.KEY_TAP.ref, FeedbackTokenId.KEY_TAP.soundResName)
    }
}
