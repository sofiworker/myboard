package xyz.xiao6.myboard.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.language.VersionRange
import xyz.xiao6.myboard.contract.engine.DictionaryBinding
import xyz.xiao6.myboard.contract.engine.DictionaryKind
import xyz.xiao6.myboard.contract.engine.DictionaryRole
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.registry.MissingResourcePolicy
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class ResourceResolverTest {

    private val resolver by lazy {
        EngineResourceResolverImpl(
        encoderRegistry = emptyEncoderRegistry,
        dictionaryRegistry = emptyDictionaryRegistry,
        candidatePolicyRegistry = emptyCandidatePolicyRegistry,
        displayPolicyRegistry = emptyDisplayPolicyRegistry,
        resourceCatalog = ResolvedResourceCatalog(listOf(resource()))
        )
    }

    @Test
    fun `rejects paths that escape their package`() {
        val result = resolver.resolveResource(
            ResourceRef("pack.en", "layouts/../outside.json", ResourceKind.LAYOUT),
            listOf(resource(path = "layouts/qwerty.json"))
        )

        assertTrue(result is ResourceResolution.RejectedPackage)
    }

    @Test
    fun `requires matching hash version and kind`() {
        val resource = resource()
        val hashMismatch = resolver.resolveResource(
            ResourceRef("pack.en", "layouts/qwerty.json", ResourceKind.LAYOUT, sha256 = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"),
            listOf(resource)
        )
        val versionMismatch = resolver.resolveResource(
            ResourceRef(
                "pack.en",
                "layouts/qwerty.json",
                ResourceKind.LAYOUT,
                versionRange = VersionRange(minimum = SemVer(2, 0, 0))
            ),
            listOf(resource)
        )
        val kindMismatch = resolver.resolveResource(
            ResourceRef("pack.en", "layouts/qwerty.json", ResourceKind.DICTIONARY),
            listOf(resource)
        )

        assertTrue(hashMismatch is ResourceResolution.RejectedPackage)
        assertTrue(versionMismatch is ResourceResolution.RejectedPackage)
        assertTrue(kindMismatch is ResourceResolution.RejectedPackage)
    }

    @Test
    fun `applies declared policy when resource is missing`() {
        val resources = emptyList<ResolvedResourceKey>()

        assertTrue(resolveMissing(MissingResourcePolicy.REJECT_PACKAGE, resources) is ResourceResolution.RejectedPackage)
        assertTrue(resolveMissing(MissingResourcePolicy.DISABLE_CAPABILITY, resources) is ResourceResolution.CapabilityDisabled)
        assertTrue(resolveMissing(MissingResourcePolicy.USE_CAPABILITY_FALLBACK, resources) is ResourceResolution.CapabilityFallbackRequired)
    }

    @Test
    fun `returns immutable resolved identity for matching reference`() {
        val resource = resource()

        val result = resolver.resolveResource(
            ResourceRef("pack.en", "layouts/qwerty.json", ResourceKind.LAYOUT, sha256 = resource.sha256),
            listOf(resource)
        )

        assertEquals(resource, (result as ResourceResolution.Resolved).key)
    }

    @Test
    fun `rejects malformed SHA-256 values`() {
        assertTrue(
            runCatching {
                ResourceRef("pack.en", "layouts/qwerty.json", ResourceKind.LAYOUT, sha256 = "not-a-sha")
            }.exceptionOrNull() is IllegalArgumentException
        )
        assertTrue(
            runCatching {
                resource().copy(sha256 = "not-a-sha")
            }.exceptionOrNull() is IllegalArgumentException
        )
    }

    @Test
    fun `required dictionary missing from catalog rejects capability`() {
        val capability = LanguageCapability(
            id = CapabilityId("pack.en", LocaleTag("en-US"), Script.LATN, Schema("DIRECT")),
            engine = EngineBinding(engineId = "table_composing"),
            layout = ResourceRef("pack.en", "layouts/qwerty.json", ResourceKind.LAYOUT),
            dictionaries = listOf(
                DictionaryBinding(
                    kind = DictionaryKind.WORD,
                    role = DictionaryRole.PRIMARY,
                    resource = ResourceRef("pack.en", "dicts/missing.dict", ResourceKind.DICTIONARY),
                    required = true
                )
            ),
            candidatePolicyId = "default",
            supportsShift = false
        )

        assertTrue(resolver.resolve(capability) is CapabilityResourceResolution.RejectedPackage)
    }

    @Test
    fun `loads mapping and fsm content and preserves resolved resource identities`() {
        val layout = resource()
        val mapping = resource(path = "maps/test.json", kind = ResourceKind.MAPPING)
        val fsm = resource(path = "rules/test.json", kind = ResourceKind.FSM)
        val bytes = mapOf(
            mapping to """{"id":"test-map","layers":{"default":{"a":"alpha"}}}""".encodeToByteArray(),
            fsm to """{"id":"test-fsm","startState":"start","states":{"start":{"k":{"next":"done","emit":"か"}}}}""".encodeToByteArray()
        )
        val resolver = EngineResourceResolverImpl(
            encoderRegistry = emptyEncoderRegistry,
            dictionaryRegistry = emptyDictionaryRegistry,
            candidatePolicyRegistry = emptyCandidatePolicyRegistry,
            displayPolicyRegistry = emptyDisplayPolicyRegistry,
            resourceCatalog = ResolvedResourceCatalog(listOf(layout, mapping, fsm), bytes::get)
        )
        val capability = LanguageCapability(
            id = CapabilityId("pack.en", LocaleTag("en-US"), Script.LATN, Schema("DIRECT")),
            engine = EngineBinding(engineId = "direct"),
            layout = ResourceRef("pack.en", layout.normalizedPath, ResourceKind.LAYOUT),
            dictionaries = emptyList(),
            mapping = ResourceRef("pack.en", mapping.normalizedPath, ResourceKind.MAPPING),
            fsm = ResourceRef("pack.en", fsm.normalizedPath, ResourceKind.FSM),
            candidatePolicyId = "default",
            supportsShift = false
        )

        val resources = (resolver.resolve(capability) as CapabilityResourceResolution.Resolved).resources

        assertEquals("alpha", resources.mapping?.layers?.get("default")?.get("a"))
        assertEquals("か", resources.fsm?.states?.get("start")?.get("k")?.emit)
        assertEquals(setOf(layout, mapping, fsm), resources.resolvedResources.values.toSet())
    }

    private fun resolveMissing(
        policy: MissingResourcePolicy,
        resources: List<ResolvedResourceKey>
    ) = resolver.resolveResource(ResourceRef("pack.en", "layouts/missing.json", ResourceKind.LAYOUT, onMissing = policy), resources)

    private fun resource(
        path: String = "layouts/qwerty.json",
        kind: ResourceKind = ResourceKind.LAYOUT
    ) = ResolvedResourceKey(
        packageId = "pack.en",
        packageVersion = SemVer(1, 0, 0),
        normalizedPath = path,
        kind = kind,
        sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
    )

    private val emptyEncoderRegistry = object : EncoderRegistry {
        override fun register(encoder: xyz.xiao6.myboard.contract.engine.Encoder) = Unit
        override fun get(encoderId: String) = null
    }
    private val emptyDictionaryRegistry = object : DictionaryRegistry {
        override fun load(key: xyz.xiao6.myboard.contract.registry.DictionaryKey) = null
        override fun invalidate(key: xyz.xiao6.myboard.contract.registry.DictionaryKey) = Unit
    }
    private val emptyCandidatePolicyRegistry = object : CandidatePolicyRegistry {
        override fun register(policy: xyz.xiao6.myboard.contract.engine.CandidatePolicy) = Unit
        override fun get(policyId: String) = null
    }
    private val emptyDisplayPolicyRegistry = object : DisplayPolicyRegistry {
        override fun register(policy: xyz.xiao6.myboard.contract.engine.DisplayPolicy) = Unit
        override fun get(policyId: String) = null
    }
}
