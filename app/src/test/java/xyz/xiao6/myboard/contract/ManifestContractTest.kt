package xyz.xiao6.myboard.contract

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.contract.engine.DictionaryBinding
import xyz.xiao6.myboard.contract.engine.DictionaryKind
import xyz.xiao6.myboard.contract.engine.DictionaryRole
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.LocaleDefaults
import xyz.xiao6.myboard.contract.manifest.ScriptDescriptor
import xyz.xiao6.myboard.contract.manifest.ScriptManifest
import xyz.xiao6.myboard.contract.manifest.validate
import xyz.xiao6.myboard.contract.registry.ManifestValidationError
import xyz.xiao6.myboard.contract.registry.MissingResourcePolicy
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.registry.toLayoutCanonicalId
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class ManifestContractTest {

    @Test
    fun `accepts flat capabilities for multiple schemas of one script`() {
        val manifest = manifest(
            capabilities = listOf(
                capability(schema = Schema("DIRECT")),
                capability(schema = Schema("ALT"))
            )
        )

        assertTrue(manifest.validate().isValid)
    }

    @Test
    fun `rejects duplicate script declarations`() {
        val manifest = manifest(
            scripts = listOf(scriptManifest(), scriptManifest())
        )

        assertHasError<ManifestValidationError.DuplicateScript>(manifest)
    }

    @Test
    fun `rejects duplicate capability identities`() {
        val direct = capability(schema = Schema("DIRECT"))
        val manifest = manifest(capabilities = listOf(direct, direct))

        assertHasError<ManifestValidationError.DuplicateCapabilityId>(manifest)
    }

    @Test
    fun `rejects script descriptor whose identity differs from its declaration`() {
        val manifest = manifest(
            scripts = listOf(scriptManifest(descriptorScript = Script.HANI))
        )

        assertHasError<ManifestValidationError.ScriptDescriptorMismatch>(manifest)
    }

    @Test
    fun `rejects manifest whose locale default capability is absent`() {
        val manifest = manifest(capabilities = listOf(capability(schema = Schema("ALT"))))

        assertHasError<ManifestValidationError.MissingDefaultCapability>(manifest)
    }

    @Test
    fun `accepts only dictionary kind and role combinations from the compatibility matrix`() {
        val accepted = manifest(
            capabilities = listOf(
                capability(
                    dictionaries = listOf(
                        dictionary(DictionaryKind.WORD, DictionaryRole.PRIMARY),
                        dictionary(DictionaryKind.PHRASE, DictionaryRole.PRIMARY),
                        dictionary(DictionaryKind.CONVERSION, DictionaryRole.CONVERSION),
                        dictionary(DictionaryKind.FREQUENCY, DictionaryRole.FREQUENCY),
                        dictionary(DictionaryKind.SPELLING, DictionaryRole.SPELLING),
                        dictionary(DictionaryKind.EMOJI, DictionaryRole.EMOJI)
                    )
                )
            )
        )

        assertTrue(accepted.validate().isValid)
    }

    @Test
    fun `rejects a dictionary role that is incompatible with its intrinsic kind`() {
        val manifest = manifest(
            capabilities = listOf(
                capability(dictionaries = listOf(dictionary(DictionaryKind.CONVERSION, DictionaryRole.PRIMARY)))
            )
        )

        assertFalse(manifest.validate().isValid)
        assertHasError<ManifestValidationError.IncompatibleDictionaryBinding>(manifest)
    }

    @Test
    fun `rejects resources with missing required package or path`() {
        val manifest = manifest(
            capabilities = listOf(
                capability(
                    dictionaries = listOf(
                        DictionaryBinding(
                            kind = DictionaryKind.WORD,
                            role = DictionaryRole.PRIMARY,
                            resource = ResourceRef("", "", ResourceKind.DICTIONARY),
                            required = true
                        )
                    )
                )
            )
        )

        assertHasError<ManifestValidationError.InvalidResourceRef>(manifest)
    }

    @Test
    fun `rejects capability fallback resources without a fallback capability`() {
        val manifest = manifest(
            capabilities = listOf(
                capability(
                    dictionaries = listOf(
                        dictionary(DictionaryKind.WORD, DictionaryRole.PRIMARY).copy(
                            resource = ResourceRef(
                                packageId = "language.en-US",
                                path = "dicts/word.dict",
                                kind = ResourceKind.DICTIONARY,
                                onMissing = MissingResourcePolicy.USE_CAPABILITY_FALLBACK
                            )
                        )
                    )
                )
            )
        )

        assertHasError<ManifestValidationError.MissingFallbackCapability>(manifest)
    }

    @Test
    fun `accepts capability fallback resources with a non-empty fallback identity`() {
        val fallback = CapabilityId("language.en-US", LocaleTag("en-US"), Script.LATN, Schema("FALLBACK"))
        val manifest = manifest(
            capabilities = listOf(
                capability(
                    dictionaries = listOf(
                        dictionary(DictionaryKind.WORD, DictionaryRole.PRIMARY).copy(
                            resource = ResourceRef(
                                packageId = "language.en-US",
                                path = "dicts/word.dict",
                                kind = ResourceKind.DICTIONARY,
                                onMissing = MissingResourcePolicy.USE_CAPABILITY_FALLBACK
                            )
                        )
                    ),
                    fallbackCapabilityIds = listOf(fallback)
                )
            )
        )

        assertTrue(manifest.validate().isValid)
    }

    @Test
    fun `maps the built in qwerty resource to its canonical layout id`() {
        assertEquals("builtin:qwerty", layoutRef().toLayoutCanonicalId().value)
    }

    @Test
    fun `rejects a mapping resource with the wrong resource kind`() {
        val invalid = capability().copy(
            mapping = ResourceRef("language.en-US", "maps/latin.json", ResourceKind.FSM)
        )
        assertHasError<ManifestValidationError.UnexpectedResourceKind>(manifest(capabilities = listOf(invalid)))
    }

    @Test
    fun `rejects a fallback capability that refers to itself`() {
        val self = capability()
        val invalid = self.copy(fallbackCapabilityIds = listOf(self.id))
        assertHasError<ManifestValidationError.InvalidFallbackCapability>(manifest(capabilities = listOf(invalid)))
    }

    private inline fun <reified T : ManifestValidationError> assertHasError(manifest: LanguagePackManifest) {
        assertTrue(manifest.validate().errors.any { it is T })
    }

    private fun manifest(
        scripts: List<ScriptManifest> = listOf(scriptManifest()),
        capabilities: List<LanguageCapability> = listOf(capability())
    ) = LanguagePackManifest(
        manifestVersion = 1,
        identity = PackageIdentity("language.en-US", SemVer(1, 0, 0)),
        minAppVersion = SemVer(1, 0, 0),
        locale = LocaleTag("en-US"),
        displayName = mapOf("en-US" to "English"),
        defaults = LocaleDefaults(
            script = Script.LATN,
            schema = Schema("DIRECT"),
            layout = layoutRef()
        ),
        scripts = scripts,
        capabilities = capabilities
    )

    private fun scriptManifest(descriptorScript: Script = Script.LATN) = ScriptManifest(
        id = Script.LATN,
        descriptor = ScriptDescriptor(
            script = descriptorScript,
            displayNames = mapOf("en-US" to "Latin"),
            direction = xyz.xiao6.myboard.contract.manifest.TextDirection.LTR,
            layoutMirror = xyz.xiao6.myboard.contract.manifest.LayoutMirrorPolicy.NONE
        ),
        defaultSchema = Schema("DIRECT")
    )

    private fun capability(
        schema: Schema = Schema("DIRECT"),
        dictionaries: List<DictionaryBinding> = emptyList(),
        fallbackCapabilityIds: List<CapabilityId> = emptyList()
    ) = LanguageCapability(
        id = CapabilityId(
            packageId = "language.en-US",
            locale = LocaleTag("en-US"),
            script = Script.LATN,
            schema = schema
        ),
        engine = EngineBinding(engineId = "direct"),
        layout = layoutRef(),
        dictionaries = dictionaries,
        candidatePolicyId = "direct_default",
        displayPolicyId = "hidden",
        supportsShift = true,
        fallbackCapabilityIds = fallbackCapabilityIds
    )

    private fun dictionary(kind: DictionaryKind, role: DictionaryRole) = DictionaryBinding(
        kind = kind,
        role = role,
        resource = ResourceRef(
            packageId = "language.en-US",
            path = "dicts/${kind.name.lowercase()}.dict",
            kind = ResourceKind.DICTIONARY
        ),
        required = true
    )

    private fun layoutRef() = ResourceRef(
        packageId = "builtin",
        path = "layouts/qwerty.json",
        kind = ResourceKind.LAYOUT
    )
}
