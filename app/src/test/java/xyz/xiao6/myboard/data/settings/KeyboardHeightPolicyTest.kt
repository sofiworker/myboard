package xyz.xiao6.myboard.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardHeightPolicyTest {

    @Test
    fun `default height is calculated from screen height and clamped`() {
        assertEquals(256, KeyboardHeightPolicy.defaultHeightDp(screenHeightDp = 800))
        assertEquals(180, KeyboardHeightPolicy.defaultHeightDp(screenHeightDp = 400))
        assertEquals(400, KeyboardHeightPolicy.defaultHeightDp(screenHeightDp = 1600))
    }

    @Test
    fun `missing invalid and out of range stored heights are normalized for persistence`() {
        val missing = KeyboardHeightPolicy.resolve(storedHeight = null, screenHeightDp = 900)
        assertEquals(288, missing.heightDp)
        assertTrue(missing.shouldPersist)

        val invalid = KeyboardHeightPolicy.resolve(storedHeight = "bad", screenHeightDp = 900)
        assertEquals(288, invalid.heightDp)
        assertTrue(invalid.shouldPersist)

        val tooLarge = KeyboardHeightPolicy.resolve(storedHeight = "999", screenHeightDp = 900)
        assertEquals(400, tooLarge.heightDp)
        assertTrue(tooLarge.shouldPersist)

        val valid = KeyboardHeightPolicy.resolve(storedHeight = "300", screenHeightDp = 900)
        assertEquals(300, valid.heightDp)
        assertFalse(valid.shouldPersist)
    }

    @Test
    fun `keyboard content height is derived from the fixed page height`() {
        assertEquals(224, KeyboardHeightPolicy.contentHeightDp(pageHeightDp = 260, chromeHeightDp = 36))
        assertEquals(120, KeyboardHeightPolicy.contentHeightDp(pageHeightDp = 140, chromeHeightDp = 36))
    }

    @Test
    fun `default horizontal inset is calculated from screen width and clamped`() {
        assertEquals(7, KeyboardHeightPolicy.defaultHorizontalInsetDp(screenWidthDp = 360))
        assertEquals(4, KeyboardHeightPolicy.defaultHorizontalInsetDp(screenWidthDp = 200))
        assertEquals(24, KeyboardHeightPolicy.defaultHorizontalInsetDp(screenWidthDp = 1400))
    }

    @Test
    fun `missing invalid and out of range horizontal insets are normalized for persistence`() {
        val missing = KeyboardHeightPolicy.resolveHorizontalInset(storedInset = null, screenWidthDp = 360)
        assertEquals(7, missing.insetDp)
        assertTrue(missing.shouldPersist)

        val invalid = KeyboardHeightPolicy.resolveHorizontalInset(storedInset = "wide", screenWidthDp = 360)
        assertEquals(7, invalid.insetDp)
        assertTrue(invalid.shouldPersist)

        val tooLarge = KeyboardHeightPolicy.resolveHorizontalInset(storedInset = "80", screenWidthDp = 360)
        assertEquals(24, tooLarge.insetDp)
        assertTrue(tooLarge.shouldPersist)

        val valid = KeyboardHeightPolicy.resolveHorizontalInset(storedInset = "12", screenWidthDp = 360)
        assertEquals(12, valid.insetDp)
        assertFalse(valid.shouldPersist)
    }
}
