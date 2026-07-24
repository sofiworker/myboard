package xyz.xiao6.myboard.pack

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.PackageDependency
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.language.VersionRange
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.LocaleDefaults
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog
import xyz.xiao6.myboard.contract.manifest.ScriptManifest
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class PackageLifecycleTest {

    @Test
    fun `upgrade publishes new version while existing lease retains old version`() {
        val store = PackageStore()
        assertTrue(store.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)
        val oldLease = requireNotNull(store.acquireResource<String>("language.demo", "layouts/main.json") { it.decodeToString() })

        assertTrue(store.install(packagePayload("language.demo", SemVer(2, 0, 0))).isSuccess)

        assertEquals(SemVer(2, 0, 0), store.snapshot().active["language.demo"]?.version)
        assertEquals("1.0.0", oldLease.value)
        assertTrue(store.hasRetainedVersion("language.demo", SemVer(1, 0, 0)))

        oldLease.close()

        assertFalse(store.hasRetainedVersion("language.demo", SemVer(1, 0, 0)))
    }

    @Test
    fun `uninstall blocks new leases and delays cleanup until open lease closes`() {
        val store = PackageStore()
        assertTrue(store.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)
        val lease = requireNotNull(store.acquireResource<String>("language.demo", "layouts/main.json") { it.decodeToString() })

        assertTrue(store.uninstall("language.demo").isSuccess)

        assertEquals(null, store.acquireResource<String>("language.demo", "layouts/main.json") { it.decodeToString() })
        assertTrue(store.hasRetainedVersion("language.demo", SemVer(1, 0, 0)))
        lease.close()
        assertFalse(store.hasRetainedVersion("language.demo", SemVer(1, 0, 0)))
    }

    @Test
    fun `invalid resource hash leaves previous snapshot active`() {
        val store = PackageStore()
        assertTrue(store.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)

        val result = store.install(packagePayload("language.demo", SemVer(2, 0, 0), declaredHash = "0".repeat(64)))

        assertFalse(result.isSuccess)
        assertEquals(SemVer(1, 0, 0), store.snapshot().active["language.demo"]?.version)
    }

    @Test
    fun `required dependency rejects install while optional dependency degrades`() {
        val store = PackageStore()
        val required = PackageDependency("language.base", VersionRange(SemVer(1, 0, 0)), optional = false)
        val optional = PackageDependency("language.extra", VersionRange(SemVer(1, 0, 0)), optional = true)

        assertFalse(store.install(packagePayload("language.required", SemVer(1, 0, 0), dependencies = listOf(required))).isSuccess)

        val optionalResult = store.install(packagePayload("language.optional", SemVer(1, 0, 0), dependencies = listOf(optional)))
        assertTrue(optionalResult.isSuccess)
        assertTrue(optionalResult.warnings.isNotEmpty())
    }

    @Test
    fun `upgrade that introduces a required dependency cycle is rejected`() {
        val store = PackageStore()
        assertTrue(store.install(packagePayload("language.a", SemVer(1, 0, 0))).isSuccess)
        assertTrue(
            store.install(
                packagePayload(
                    "language.b",
                    SemVer(1, 0, 0),
                    dependencies = listOf(PackageDependency("language.a", VersionRange(SemVer(1, 0, 0))))
                )
            ).isSuccess
        )

        val result = store.install(
            packagePayload(
                "language.a",
                SemVer(2, 0, 0),
                dependencies = listOf(PackageDependency("language.b", VersionRange(SemVer(1, 0, 0))))
            )
        )

        assertFalse(result.isSuccess)
        assertEquals(SemVer(1, 0, 0), store.snapshot().active["language.a"]?.version)
    }

    @Test
    fun `unicode equivalent duplicate resource paths fail without mutating snapshot`() {
        val store = PackageStore()
        assertTrue(store.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)
        val payload = packagePayload("language.other", SemVer(1, 0, 0))
        val duplicateResources = payload.resources + mapOf(
            "layouts/caf\u00e9.json" to "one".encodeToByteArray(),
            "layouts/cafe\u0301.json" to "two".encodeToByteArray()
        )

        val result = store.install(payload.copy(resources = duplicateResources))

        assertFalse(result.isSuccess)
        assertEquals(setOf("language.demo"), store.snapshot().active.keys)
    }

    @Test
    fun `failed resource decoding does not leak a package lease`() {
        val store = PackageStore()
        val version = SemVer(1, 0, 0)
        assertTrue(store.install(packagePayload("language.demo", version)).isSuccess)

        assertTrue(
            runCatching {
                store.acquireResource<String>("language.demo", "layouts/main.json") {
                    error("decoder failed")
                }
            }.isFailure
        )
        assertTrue(store.uninstall("language.demo").isSuccess)

        assertFalse(store.hasRetainedVersion("language.demo", version))
    }

    private fun packagePayload(
        packageId: String,
        version: SemVer,
        declaredHash: String? = null,
        dependencies: List<PackageDependency> = emptyList()
    ): PackagePayload {
        val content = version.toString().encodeToByteArray()
        val hash = declaredHash ?: sha256(content)
        val layout = ResourceRef(packageId, "layouts/main.json", ResourceKind.LAYOUT, sha256 = hash)
        val schema = Schema("default")
        val manifest = LanguagePackManifest(
            manifestVersion = 1,
            identity = PackageIdentity(packageId, version),
            minAppVersion = SemVer(1, 0, 0),
            locale = LocaleTag("en-US"),
            displayName = mapOf("en-US" to packageId),
            defaults = LocaleDefaults(Script.LATN, schema, layout),
            scripts = listOf(ScriptManifest(Script.LATN, checkNotNull(ScriptCatalog[Script.LATN]), schema)),
            dependencies = dependencies,
            capabilities = listOf(
                LanguageCapability(
                    id = CapabilityId(packageId, LocaleTag("en-US"), Script.LATN, schema),
                    engine = EngineBinding("direct"),
                    layout = layout,
                    dictionaries = emptyList(),
                    candidatePolicyId = "direct",
                    supportsShift = true
                )
            )
        )
        return PackagePayload(manifest, mapOf("layouts/main.json" to content))
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString("") { "%02x".format(it) }
}
