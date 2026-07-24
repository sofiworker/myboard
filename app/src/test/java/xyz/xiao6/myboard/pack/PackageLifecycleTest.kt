package xyz.xiao6.myboard.pack

import java.security.MessageDigest
import java.nio.file.Files
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
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

    @Test
    fun `active packages recover from persisted state after process restart`() {
        val persistence = InMemoryPackagePersistence()
        val firstProcess = PackageStore(persistence)
        assertTrue(firstProcess.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)

        val recoveredProcess = PackageStore(persistence)

        assertEquals(SemVer(1, 0, 0), recoveredProcess.snapshot().active["language.demo"]?.version)
        recoveredProcess.acquireResource<String>("language.demo", "layouts/main.json") { it.decodeToString() }.use { handle ->
            assertEquals("1.0.0", handle?.value)
        }
    }

    @Test
    fun `persistence failure leaves the previous snapshot active`() {
        val persistence = FailingPackagePersistence()
        val store = PackageStore(persistence)
        assertTrue(store.install(packagePayload("language.demo", SemVer(1, 0, 0))).isSuccess)
        persistence.failWrites = true

        val result = store.install(packagePayload("language.demo", SemVer(2, 0, 0)))

        assertFalse(result.isSuccess)
        assertEquals(SemVer(1, 0, 0), store.snapshot().active["language.demo"]?.version)
    }

    @Test
    fun `atomic file persistence restores the active package`() {
        val directory = Files.createTempDirectory("myboard-package-store").toFile()
        try {
            val firstProcess = PackageStore(FilePackagePersistence(directory))
            val installResult = firstProcess.install(packagePayload("language.demo", SemVer(1, 0, 0)))
            assertTrue(installResult.toString(), installResult.isSuccess)

            val recoveredProcess = PackageStore(FilePackagePersistence(directory))

            assertEquals(SemVer(1, 0, 0), recoveredProcess.snapshot().active["language.demo"]?.version)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `archive staging rejects traversal unicode duplicates and executable content`() {
        val stager = PackageArchiveStager()
        val manifest = packagePayload("language.demo", SemVer(1, 0, 0)).manifest

        assertTrue(runCatching {
            stager.stage(ByteArrayInputStream(zipOf(mapOf("manifest.json" to byteArrayOf(), "../escape" to byteArrayOf())))) { manifest }
        }.isFailure)
        assertTrue(runCatching {
            stager.stage(
                ByteArrayInputStream(
                    zipOf(
                        mapOf(
                            "manifest.json" to byteArrayOf(),
                            "layouts/caf\u00e9.json" to byteArrayOf(1),
                            "layouts/cafe\u0301.json" to byteArrayOf(2)
                        )
                    )
                )
            ) { manifest }
        }.isFailure)
        assertTrue(runCatching {
            stager.stage(ByteArrayInputStream(zipOf(mapOf("manifest.json" to byteArrayOf(), "code/plugin.dex" to byteArrayOf(1))))) { manifest }
        }.isFailure)
    }

    @Test
    fun `archive staging enforces entry and total byte limits`() {
        val manifest = packagePayload("language.demo", SemVer(1, 0, 0)).manifest
        val stager = PackageArchiveStager(
            PackageArchiveLimits(maxEntries = 2, maxEntryBytes = 4, maxTotalBytes = 6)
        )

        assertTrue(runCatching {
            stager.stage(
                ByteArrayInputStream(zipOf(mapOf("manifest.json" to byteArrayOf(), "a" to ByteArray(5))))
            ) { manifest }
        }.isFailure)
        assertTrue(runCatching {
            stager.stage(
                ByteArrayInputStream(zipOf(mapOf("manifest.json" to byteArrayOf(), "a" to ByteArray(4), "b" to ByteArray(3))))
            ) { manifest }
        }.isFailure)
    }

    @Test
    fun `archive staging creates a payload from validated entries`() {
        val expected = packagePayload("language.demo", SemVer(1, 0, 0))
        val staged = PackageArchiveStager().stage(
            ByteArrayInputStream(
                zipOf(mapOf("manifest.json" to "manifest".encodeToByteArray()) + expected.resources)
            )
        ) { expected.manifest }

        assertEquals(expected.manifest, staged.manifest)
        assertEquals(expected.resources.keys, staged.resources.keys)
    }

    @Test
    fun `transactional importer installs only the staged validated payload`() = runBlocking {
        val expected = packagePayload("language.demo", SemVer(1, 0, 0))
        val store = PackageStore()
        val importer: LanguagePackImporter = TransactionalLanguagePackImporter(store)

        val result = importer.import(
            ByteArrayInputStream(
                zipOf(mapOf("manifest.json" to "manifest".encodeToByteArray()) + expected.resources)
            )
        ) { expected.manifest }

        assertTrue(result.isSuccess)
        assertEquals(SemVer(1, 0, 0), store.snapshot().active["language.demo"]?.version)
    }

    @Test
    fun `active package resources publish a versioned resolver catalog`() {
        val store = PackageStore()
        val payload = packagePayload("language.demo", SemVer(1, 0, 0))
        assertTrue(store.install(payload).isSuccess)

        val catalog = store.resourceCatalog()
        val key = catalog.snapshot().single()

        assertEquals("language.demo", key.packageId)
        assertEquals(SemVer(1, 0, 0), key.packageVersion)
        assertTrue(payload.resources["layouts/main.json"]!!.contentEquals(catalog.read(key)))
    }

    private class FailingPackagePersistence : PackagePersistence {
        private var state = PersistedPackageState()
        var failWrites = false

        override fun load(): PersistedPackageState = state.copyActivePayloads()

        override fun save(state: PersistedPackageState) {
            check(!failWrites) { "disk full" }
            this.state = state.copyActivePayloads()
        }
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

    private fun zipOf(entries: Map<String, ByteArray>): ByteArray = ByteArrayOutputStream().use { bytes ->
        ZipOutputStream(bytes).use { zip ->
            entries.forEach { (path, content) ->
                zip.putNextEntry(ZipEntry(path))
                zip.write(content)
                zip.closeEntry()
            }
        }
        bytes.toByteArray()
    }
}
