package xyz.xiao6.myboard.theme.skin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.theme.foundation.ThemeVariant

class BuiltInSkinCatalogTest {

    @Test
    fun `catalog includes pure flat`() {
        assertTrue(BuiltInSkinCatalog.contains(SkinThemeId.PURE_FLAT.id))
        assertNotNull(BuiltInSkinCatalog.metaOf(SkinThemeId.PURE_FLAT.id))
        assertEquals(
            SkinThemeId.PURE_FLAT,
            BuiltInSkinCatalog.all.single { it.id == SkinThemeId.PURE_FLAT }.id
        )
    }

    @Test
    fun `unknown skin id is rejected`() {
        assertFalse(BuiltInSkinCatalog.contains("not_a_skin"))
        assertNull(BuiltInSkinCatalog.metaOf("not_a_skin"))
        assertNull(BuiltInSkinCatalog.resolve("not_a_skin", ThemeVariant.LIGHT))
        assertNull(BuiltInSkinCatalog.resolve(null, ThemeVariant.LIGHT))
    }

    @Test
    fun `resolve pure flat returns theme doc`() {
        val doc = BuiltInSkinCatalog.resolve(SkinThemeId.PURE_FLAT.id, ThemeVariant.LIGHT)
        assertNotNull(doc)
        assertEquals(SkinThemeId.PURE_FLAT.id, doc!!.id)
    }
}
