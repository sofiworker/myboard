package xyz.xiao6.myboard.pack

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.manifest.validate
import xyz.xiao6.myboard.engine.CapabilityResourceResolution
import xyz.xiao6.myboard.engine.EngineResourceResolver
import xyz.xiao6.myboard.engine.ResourceResolution
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.layout.LayoutKey
import xyz.xiao6.myboard.contract.registry.LayoutSource
import xyz.xiao6.myboard.engine.builtin.DirectEngine
import xyz.xiao6.myboard.layout.BuiltInLayouts
import xyz.xiao6.myboard.layout.LayoutRegistryImpl
import xyz.xiao6.myboard.engine.EngineRegistryImpl
import xyz.xiao6.myboard.state.OrthogonalRegistryImpl

class LanguagePackRegistrationTest {

    @Test
    fun `built in manifests register Chinese English and Japanese capabilities`() {
        val manifests = BuiltInLanguagePacks.all

        assertEquals(setOf("zh-CN", "en-US", "ja-JP"), manifests.map { it.locale.value }.toSet())
        assertTrue(manifests.all { it.validate().isValid })
        assertTrue(manifests.flatMap { it.capabilities }.isNotEmpty())
    }

    @Test
    fun `resource catalog hashes actual source bytes instead of manifest hashes`() {
        val source = mapOf(
            "layouts/qwerty.jsonc" to "layout".encodeToByteArray(),
            "layouts/shuangpin_ziran.jsonc" to "shuangpin-layout".encodeToByteArray(),
            "layouts/t9_chinese.jsonc" to "t9-layout".encodeToByteArray(),
            "dicts/pinyin_main.dict" to "dictionary".encodeToByteArray(),
            "maps/latin_qwerty.json" to "mapping".encodeToByteArray(),
            "rules/romaji_hira.fsm.json" to "fsm".encodeToByteArray(),
            "rules/romaji_kata.fsm.json" to "fsm-kata".encodeToByteArray(),
            "engines/ziran_map.json" to "ziran".encodeToByteArray(),
            "engines/t9_keymap.json" to "t9".encodeToByteArray()
        )

        val catalog = BuiltInLanguagePacks.resourceCatalog(source::get).snapshot()

        assertTrue(catalog.isNotEmpty())
        catalog.forEach { key ->
            val expected = MessageDigest.getInstance("SHA-256")
                .digest(requireNotNull(source[key.normalizedPath]))
                .joinToString("") { "%02x".format(it) }
            assertEquals(expected, key.sha256)
        }
    }

    @Test
    fun `same locale providers retain backup and restore it after active provider uninstall`() {
        val registry = OrthogonalRegistryImpl(
            engineRegistry = testEngineRegistry(),
            layoutRegistry = testLayoutRegistry(),
            dictionaryRegistry = unusedDictionaryRegistry(),
            engineResourceResolver = resolvedResourceResolver()
        )
        val alpha = BuiltInLanguagePacks.enUS.copy(
            identity = PackageIdentity("language.alpha", SemVer(1, 0, 0))
        ).withCapabilityPackage("language.alpha")
        val zulu = BuiltInLanguagePacks.enUS.copy(
            identity = PackageIdentity("language.zulu", SemVer(1, 0, 0))
        ).withCapabilityPackage("language.zulu")

        registry.register(alpha)
        registry.register(zulu)

        assertEquals("language.alpha", registry.getLocale(LocaleTag("en-US"))?.identity?.packageId)
        val state = OrthogonalState(LocaleTag("en-US"), alpha.defaults.script, alpha.defaults.schema)
        assertTrue(registry.isSupported(state))
        assertTrue(registry.unregister("language.alpha").isSuccess())
        assertEquals("language.zulu", registry.getLocale(LocaleTag("en-US"))?.identity?.packageId)
        assertTrue(registry.isSupported(state))
    }

    private fun LanguagePackManifest.withCapabilityPackage(packageId: String) = copy(
        capabilities = capabilities.map { capability ->
            capability.copy(id = capability.id.copy(packageId = packageId))
        }
    )

    private fun resolvedResourceResolver() = object : EngineResourceResolver {
        override fun resolve(capability: xyz.xiao6.myboard.contract.manifest.LanguageCapability) =
            CapabilityResourceResolution.Resolved(
                xyz.xiao6.myboard.contract.engine.EngineResources(
                    candidatePolicy = object : xyz.xiao6.myboard.contract.engine.CandidatePolicy {
                        override val policyId = "test"
                        override fun sort(candidates: List<xyz.xiao6.myboard.contract.input.Candidate>) = candidates
                        override fun onSpace(state: xyz.xiao6.myboard.contract.input.InputSessionState) = xyz.xiao6.myboard.contract.engine.PolicyAction.Noop
                        override fun onEnter(state: xyz.xiao6.myboard.contract.input.InputSessionState) = xyz.xiao6.myboard.contract.engine.PolicyAction.Noop
                        override fun onCandidateSelected(state: xyz.xiao6.myboard.contract.input.InputSessionState, index: Int) = xyz.xiao6.myboard.contract.engine.PolicyAction.Noop
                    },
                    displayPolicy = object : xyz.xiao6.myboard.contract.engine.DisplayPolicy {
                        override val policyId = "test"
                        override fun display(state: xyz.xiao6.myboard.contract.input.InputSessionState) = ""
                    },
                    resolvedResources = mapOf(
                        capability.layout to ResolvedResourceKey(
                            "builtin", BuiltInLanguagePacks.packageVersion, "layouts/qwerty.jsonc",
                            ResourceKind.LAYOUT, "a".repeat(64)
                        )
                    )
                )
            )

        override fun resolveResource(reference: ResourceRef, availableResources: Collection<ResolvedResourceKey>) =
            ResourceResolution.RejectedPackage("not used")
    }

    private fun testEngineRegistry() = EngineRegistryImpl().apply { register(DirectEngine()) }
    private fun testLayoutRegistry() = LayoutRegistryImpl().apply {
        register(LayoutKey("builtin", "qwerty", BuiltInLanguagePacks.packageVersion), BuiltInLayouts.qwerty, LayoutSource.BUILT_IN)
    }
    private fun unusedDictionaryRegistry() = xyz.xiao6.myboard.engine.DictionaryRegistryImpl()

    private fun xyz.xiao6.myboard.contract.registry.RegisterResult.isSuccess() =
        this is xyz.xiao6.myboard.contract.registry.RegisterResult.Success
}
