package xyz.xiao6.myboard.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.layout.LayoutCanonicalId
import xyz.xiao6.myboard.contract.layout.LayoutDoc
import xyz.xiao6.myboard.contract.layout.LayoutKey
import xyz.xiao6.myboard.contract.layout.RowLayout
import xyz.xiao6.myboard.contract.layout.resolve
import xyz.xiao6.myboard.contract.layout.toCanonicalId
import xyz.xiao6.myboard.contract.registry.LayoutSource

class LayoutCanonicalIdTest {

    @Test
    fun `canonical ID excludes package version`() {
        val key = LayoutKey("builtin", "qwerty", SemVer(3, 2, 1))

        assertEquals("builtin:qwerty", key.toCanonicalId().value)
    }

    @Test
    fun `canonical ID rejects colons in component IDs`() {
        assertInvalid { LayoutKey("builtin:override", "qwerty", SemVer(1, 0, 0)) }
        assertInvalid { LayoutKey("builtin", "qwerty:override", SemVer(1, 0, 0)) }
        assertInvalid { LayoutCanonicalId("builtin:qwerty:override") }
    }

    @Test
    fun `same layout name from separate packages does not collide`() {
        val registry = LayoutRegistryImpl()
        val builtin = LayoutKey("builtin", "qwerty", SemVer(1, 0, 0))
        val thirdParty = LayoutKey("thirdparty.keyboard", "qwerty", SemVer(1, 0, 0))

        registry.register(builtin, layout("builtin qwerty"), LayoutSource.BUILT_IN)
        registry.register(thirdParty, layout("third party qwerty"), LayoutSource.LANGUAGE_PACK)

        assertEquals("builtin qwerty", registry.get(builtin)?.meta?.name)
        assertEquals("third party qwerty", registry.get(thirdParty)?.meta?.name)
    }

    @Test
    fun `canonical ID restores only through an explicitly versioned registry lookup`() {
        val registry = LayoutRegistryImpl()
        val key = LayoutKey("builtin", "qwerty", SemVer(1, 2, 3))
        registry.register(key, layout("qwerty"), LayoutSource.BUILT_IN)

        assertEquals(key, key.toCanonicalId().resolve(SemVer(1, 2, 3), registry))
        assertNotNull(registry.get(key))
        assertNull(registry.get(LayoutKey("builtin", "qwerty", SemVer(1, 2, 4))))
        assertInvalid { key.toCanonicalId().resolve(SemVer(1, 2, 4), registry) }
    }

    @Test
    fun `registry rejects a key whose layout ID differs from the document ID`() {
        val result = LayoutRegistryImpl().register(
            LayoutKey("builtin", "qwerty", SemVer(1, 0, 0)),
            LayoutDoc(id = "dvorak", root = RowLayout(id = "row", keys = emptyList())),
            LayoutSource.BUILT_IN
        )

        assertTrue(result is xyz.xiao6.myboard.contract.registry.RegisterResult.Failed)
    }

    private fun layout(name: String) = LayoutDoc(
        id = "qwerty",
        meta = xyz.xiao6.myboard.contract.layout.LayoutMeta(name = name),
        root = RowLayout(id = "row", keys = emptyList())
    )

    private fun assertInvalid(block: () -> Unit) {
        check(runCatching(block).exceptionOrNull() is IllegalArgumentException)
    }
}
