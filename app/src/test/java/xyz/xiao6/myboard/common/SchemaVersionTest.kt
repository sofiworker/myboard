package xyz.xiao6.myboard.common

import org.junit.Assert.*
import org.junit.Test

class SchemaVersionTest {

    // ── 解析 ──────────────────────────────────────────────

    @Test
    fun `parse valid version`() {
        val v = SchemaVersion.parse("1.0.0")
        assertEquals(1, v.major)
        assertEquals(0, v.minor)
        assertEquals(0, v.patch)
    }

    @Test
    fun `parse complex version`() {
        val v = SchemaVersion.parse("2.13.45")
        assertEquals(2, v.major)
        assertEquals(13, v.minor)
        assertEquals(45, v.patch)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse invalid format - too few parts`() {
        SchemaVersion.parse("1.0")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse invalid format - non-numeric`() {
        SchemaVersion.parse("a.b.c")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse invalid format - empty string`() {
        SchemaVersion.parse("")
    }

    // ── toString ──────────────────────────────────────────

    @Test
    fun `toString returns a_b_c format`() {
        assertEquals("1.0.0", SchemaVersion(1, 0, 0).toString())
        assertEquals("2.13.45", SchemaVersion(2, 13, 45).toString())
    }

    // ── 等值比较 ──────────────────────────────────────────

    @Test
    fun `equal versions are equal`() {
        assertEquals(SchemaVersion(1, 0, 0), SchemaVersion(1, 0, 0))
    }

    @Test
    fun `different versions are not equal`() {
        assertNotEquals(SchemaVersion(1, 0, 0), SchemaVersion(1, 0, 1))
    }

    // ── 排序 ──────────────────────────────────────────────

    @Test
    fun `comparison by major`() {
        assertTrue(SchemaVersion(1, 0, 0) < SchemaVersion(2, 0, 0))
    }

    @Test
    fun `comparison by minor`() {
        assertTrue(SchemaVersion(1, 0, 0) < SchemaVersion(1, 1, 0))
    }

    @Test
    fun `comparison by patch`() {
        assertTrue(SchemaVersion(1, 0, 0) < SchemaVersion(1, 0, 1))
    }

    @Test
    fun `equal versions compare to zero`() {
        assertEquals(0, SchemaVersion(1, 0, 0).compareTo(SchemaVersion(1, 0, 0)))
    }

    // ── CURRENT 常量 ──────────────────────────────────────

    @Test
    fun `CURRENT is 1_0_0`() {
        assertEquals("1.0.0", SchemaVersion.CURRENT.toString())
    }

    @Test
    fun `CURRENT_STR matches CURRENT`() {
        assertEquals(SchemaVersion.CURRENT.toString(), SchemaVersion.CURRENT_STR)
    }

    // ── 兼容性检查 ────────────────────────────────────────

    @Test
    fun `same version is compatible`() {
        assertTrue(
            SchemaVersion(1, 0, 0).isCompatibleWith(SchemaVersion(1, 0, 0))
        )
    }

    @Test
    fun `higher minor is compatible`() {
        assertTrue(
            SchemaVersion(1, 2, 0).isCompatibleWith(SchemaVersion(1, 0, 0))
        )
    }

    @Test
    fun `higher patch is compatible`() {
        assertTrue(
            SchemaVersion(1, 0, 5).isCompatibleWith(SchemaVersion(1, 0, 0))
        )
    }

    @Test
    fun `lower minor is NOT compatible`() {
        assertFalse(
            SchemaVersion(1, 0, 0).isCompatibleWith(SchemaVersion(1, 2, 0))
        )
    }

    @Test
    fun `lower patch is NOT compatible`() {
        assertFalse(
            SchemaVersion(1, 0, 0).isCompatibleWith(SchemaVersion(1, 0, 5))
        )
    }

    @Test
    fun `different major is NOT compatible`() {
        assertFalse(
            SchemaVersion(2, 0, 0).isCompatibleWith(SchemaVersion(1, 0, 0))
        )
    }

    @Test
    fun `lower major is NOT compatible`() {
        assertFalse(
            SchemaVersion(1, 0, 0).isCompatibleWith(SchemaVersion(2, 0, 0))
        )
    }

    // ── init 验证 ─────────────────────────────────────────

    @Test(expected = IllegalArgumentException::class)
    fun `negative major throws`() {
        SchemaVersion(-1, 0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative minor throws`() {
        SchemaVersion(0, -1, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative patch throws`() {
        SchemaVersion(0, 0, -1)
    }

    // ── parseOrNull ───────────────────────────────────────

    @Test
    fun `parseOrNull returns version for valid input`() {
        assertEquals(
            SchemaVersion(1, 0, 0),
            SchemaVersion.parseOrNull("1.0.0")
        )
    }

    @Test
    fun `parseOrNull returns null for invalid input`() {
        assertNull(SchemaVersion.parseOrNull("bad"))
    }

    @Test
    fun `parseOrNull handles legacy integer format`() {
        assertEquals(SchemaVersion(2, 0, 0), SchemaVersion.parseOrNull("2"))
    }

    // ── isCompatible 静态方法 ─────────────────────────────

    @Test
    fun `isCompatible returns true for compatible version`() {
        assertTrue(SchemaVersion.isCompatible("1.0.0"))
        assertTrue(SchemaVersion.isCompatible("1.2.3"))
    }

    @Test
    fun `isCompatible returns false for incompatible version`() {
        assertFalse(SchemaVersion.isCompatible("2.0.0"))
        assertFalse(SchemaVersion.isCompatible("0.9.9"))
    }

    @Test
    fun `isCompatible returns false for invalid version`() {
        assertFalse(SchemaVersion.isCompatible("invalid"))
    }

    @Test
    fun `isCompatible handles legacy integer format`() {
        // 旧格式 "2" 会被解析为 SchemaVersion(2, 0, 0)
        assertFalse(SchemaVersion.isCompatible("2"))
        // 旧格式 "1" 会被解析为 SchemaVersion(1, 0, 0)，与 CURRENT 兼容
        assertTrue(SchemaVersion.isCompatible("1"))
    }
}
