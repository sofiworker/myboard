package xyz.xiao6.myboard.engine

import xyz.xiao6.myboard.contract.engine.CandidatePolicy
import xyz.xiao6.myboard.contract.engine.DisplayPolicy
import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.engine.FsmTransition
import xyz.xiao6.myboard.contract.engine.KeyMapping
import xyz.xiao6.myboard.contract.engine.PolicyAction
import xyz.xiao6.myboard.contract.engine.TransliterationFsm
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.InputSessionState
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.manifest.normalizeResourcePath
import xyz.xiao6.myboard.contract.registry.DictionaryKey
import xyz.xiao6.myboard.contract.registry.MissingResourcePolicy
import xyz.xiao6.myboard.contract.registry.ResourceRef

class EngineResourceResolverImpl(
    private val encoderRegistry: EncoderRegistry,
    private val dictionaryRegistry: DictionaryRegistry,
    private val candidatePolicyRegistry: CandidatePolicyRegistry,
    private val displayPolicyRegistry: DisplayPolicyRegistry,
    private val resourceCatalog: ResolvedResourceCatalog
) : EngineResourceResolver {

    override fun resolveResource(
        reference: ResourceRef,
        availableResources: Collection<ResolvedResourceKey>
    ): ResourceResolution {
        val normalizedPath = runCatching { normalizeResourcePath(reference.path) }
            .getOrElse { return ResourceResolution.RejectedPackage(it.message ?: "Invalid resource path") }

        val matchingPath = availableResources.filter {
            it.packageId == reference.packageId && it.normalizedPath == normalizedPath
        }
        val matchingVersion = matchingPath.filter { reference.versionRange == null || it.packageVersion in reference.versionRange }
        val matchingKind = matchingVersion.filter { it.kind == reference.kind }
        val resolved = matchingKind.singleOrNull { reference.sha256 == null || it.sha256 == reference.sha256 }

        return resolved?.let(ResourceResolution::Resolved)
            ?: missingResource(reference, mismatchReason(reference, matchingPath, matchingVersion, matchingKind))
    }

    override fun resolve(capability: LanguageCapability): CapabilityResourceResolution {
        val references = buildList {
            add(capability.layout)
            capability.engine.encoderConfig?.let(::add)
            capability.mapping?.let(::add)
            capability.fsm?.let(::add)
            addAll(capability.dictionaries.map { it.resource })
        }
        val resolvedResources = linkedMapOf<ResourceRef, ResolvedResourceKey>()
        references.forEach { reference ->
            when (val resolution = resolveResource(reference, resourceCatalog.snapshot())) {
                is ResourceResolution.Resolved -> resolvedResources[reference] = resolution.key
                is ResourceResolution.RejectedPackage -> return CapabilityResourceResolution.RejectedPackage(resolution.reason)
                is ResourceResolution.CapabilityDisabled -> return CapabilityResourceResolution.CapabilityDisabled(resolution.reason)
                is ResourceResolution.CapabilityFallbackRequired -> return CapabilityResourceResolution.CapabilityFallbackRequired(resolution.reason)
            }
        }

        val resolvedEncoderConfig = capability.engine.encoderConfig?.let(resolvedResources::get)
        val encoder = capability.engine.encoderId
            ?.takeIf { capability.engine.encoderConfig == null || resolvedEncoderConfig != null }
            ?.let(encoderRegistry::get)
        val primaryDictionary = capability.dictionaries.firstOrNull()
        val dictionary = primaryDictionary?.let { binding ->
            val resource = checkNotNull(resolvedResources[binding.resource])
            val loaded = dictionaryRegistry.load(
                DictionaryKey(
                    packageId = resource.packageId,
                    locale = capability.id.locale,
                    script = capability.id.script,
                    schema = capability.id.schema,
                    path = resource.normalizedPath
                )
            )
            if (loaded == null && binding.required) {
                return CapabilityResourceResolution.RejectedPackage(
                    "Required dictionary ${resource.packageId}:${resource.normalizedPath} is unavailable"
                )
            }
            loaded
        }
        val candidatePolicy = candidatePolicyRegistry.get(capability.candidatePolicyId) ?: fallbackCandidatePolicy
        val displayPolicy = capability.displayPolicyId?.let(displayPolicyRegistry::get) ?: fallbackDisplayPolicy

        return CapabilityResourceResolution.Resolved(EngineResources(
            mapping = capability.mapping
                ?.let(resolvedResources::get)
                ?.let { KeyMapping(it.normalizedPath, emptyMap()) },
            encoder = encoder,
            fsm = capability.fsm
                ?.let(resolvedResources::get)
                ?.let { TransliterationFsm(it.normalizedPath, "", emptyMap<String, Map<String, FsmTransition>>()) },
            dictionary = dictionary,
            candidatePolicy = candidatePolicy,
            displayPolicy = displayPolicy
        ))
    }

    private val fallbackCandidatePolicy = object : CandidatePolicy {
        override val policyId = "fallback"
        override fun sort(candidates: List<Candidate>): List<Candidate> = candidates
        override fun onSpace(state: InputSessionState): PolicyAction = PolicyAction.Noop
        override fun onEnter(state: InputSessionState): PolicyAction = PolicyAction.Noop
        override fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction = PolicyAction.Noop
    }

    private val fallbackDisplayPolicy = object : DisplayPolicy {
        override val policyId = "fallback"
        override fun display(state: InputSessionState): String = state.rawBuffer
    }

    private fun missingResource(reference: ResourceRef, reason: String): ResourceResolution = when (reference.onMissing) {
        MissingResourcePolicy.REJECT_PACKAGE -> ResourceResolution.RejectedPackage(reason)
        MissingResourcePolicy.DISABLE_CAPABILITY -> ResourceResolution.CapabilityDisabled(reason)
        MissingResourcePolicy.USE_CAPABILITY_FALLBACK -> ResourceResolution.CapabilityFallbackRequired(reason)
    }

    private fun mismatchReason(
        reference: ResourceRef,
        matchingPath: List<ResolvedResourceKey>,
        matchingVersion: List<ResolvedResourceKey>,
        matchingKind: List<ResolvedResourceKey>
    ): String = when {
        matchingPath.isEmpty() -> "Resource ${reference.packageId}:${reference.path} is unavailable"
        matchingVersion.isEmpty() -> "Resource ${reference.packageId}:${reference.path} does not satisfy the requested version"
        matchingKind.isEmpty() -> "Resource ${reference.packageId}:${reference.path} has an unexpected kind"
        else -> "Resource ${reference.packageId}:${reference.path} has an unexpected SHA-256"
    }
}
