package xyz.xiao6.myboard.state

import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.validate
import xyz.xiao6.myboard.contract.registry.toLayoutCanonicalId
import xyz.xiao6.myboard.contract.layout.LayoutKey
import xyz.xiao6.myboard.contract.registry.RegisterResult
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.engine.DictionaryRegistry
import xyz.xiao6.myboard.engine.EngineRegistry
import xyz.xiao6.myboard.engine.EngineResourceResolver
import xyz.xiao6.myboard.engine.CapabilityResourceResolution
import xyz.xiao6.myboard.layout.LayoutRegistry

class OrthogonalRegistryImpl(
    private val engineRegistry: EngineRegistry,
    private val layoutRegistry: LayoutRegistry,
    @Suppress("UNUSED_PARAMETER") private val dictionaryRegistry: DictionaryRegistry,
    private val engineResourceResolver: EngineResourceResolver
) : OrthogonalRegistry {

    @Volatile private var current = RegistrySnapshot()

    @Synchronized
    override fun register(manifest: LanguagePackManifest): RegisterResult {
        val validation = manifest.validate()
        if (!validation.isValid) {
            return RegisterResult.Failed(validation.errors.map(Any::toString))
        }

        val resolvedCapabilities = mutableListOf<ResolvedLanguageCapability>()
        for (capability in manifest.capabilities) {
            if (engineRegistry.get(capability.engine.engineId) == null) {
                return RegisterResult.Failed(listOf("Engine '${capability.engine.engineId}' is not registered"))
            }
            when (val result = engineResourceResolver.resolve(capability)) {
                is CapabilityResourceResolution.Resolved -> {
                    val layoutKey = result.resources.resolvedResources[capability.layout]
                        ?: return RegisterResult.Failed(listOf("Layout '${capability.layout.path}' was not resolved"))
                    val canonicalId = capability.layout.toLayoutCanonicalId()
                    val layoutRegistered = runCatching {
                        layoutRegistry.resolve(
                            LayoutKey(canonicalId.components().first, canonicalId.components().second, layoutKey.packageVersion).packageId,
                            canonicalId.components().second,
                            layoutKey.packageVersion
                        )
                    }.isSuccess
                    if (!layoutRegistered) {
                        return RegisterResult.Failed(listOf("Layout '${canonicalId.value}' is not registered"))
                    }
                    resolvedCapabilities += ResolvedLanguageCapability(capability, result.resources)
                }
                is CapabilityResourceResolution.RejectedPackage -> return RegisterResult.Failed(listOf(result.reason))
                is CapabilityResourceResolution.CapabilityDisabled,
                is CapabilityResourceResolution.CapabilityFallbackRequired -> Unit
            }
        }

        val providers = current.providersByLocale.values.flatten()
            .associateBy { it.identity.packageId }
            .toMutableMap()
        providers[manifest.identity.packageId] = manifest
        val capabilitiesByPackage = current.capabilitiesByPackage.toMutableMap()
        capabilitiesByPackage[manifest.identity.packageId] = resolvedCapabilities.map(ResolvedLanguageCapability::capability)
        val resolvedByPackage = current.resolvedCapabilitiesByState.values.flatten()
            .groupBy { it.capability.id.packageId }
            .toMutableMap()
        resolvedByPackage[manifest.identity.packageId] = resolvedCapabilities
        current = snapshotOf(providers.values, capabilitiesByPackage, resolvedByPackage)
        return RegisterResult.Success(manifest.identity.packageId)
    }

    @Synchronized
    override fun unregister(packageId: String): RegisterResult {
        val installedProviders = current.providersByLocale.values.flatten()
        val providers = installedProviders.filterNot { it.identity.packageId == packageId }
        val removed = providers.size != installedProviders.size
        if (removed) {
            current = snapshotOf(
                providers = providers,
                capabilitiesByPackage = current.capabilitiesByPackage - packageId,
                resolvedByPackage = current.resolvedCapabilitiesByState.values.flatten()
                    .filterNot { it.capability.id.packageId == packageId }
                    .groupBy { it.capability.id.packageId }
            )
        }
        return if (removed) RegisterResult.Success(packageId) else RegisterResult.Failed(listOf("Package '$packageId' is not registered"))
    }

    override fun getLocale(locale: LocaleTag): LanguagePackManifest? = current.provider(locale)

    override fun isSupported(state: OrthogonalState): Boolean = state in current.capabilitiesByState

    override fun defaultState(locale: LocaleTag): OrthogonalState? = current.provider(locale)?.defaults?.let {
        OrthogonalState(locale, it.script, it.schema)
    }

    override fun defaultSchema(locale: LocaleTag, script: Script): Schema? =
        current.provider(locale)?.scripts?.firstOrNull { it.id == script }?.defaultSchema

    override fun schemaCapability(state: OrthogonalState): LanguageCapability? = current.capability(state)

    override fun resolve(state: OrthogonalState): ResolvedLanguageCapability? = current.resolvedCapability(state)

    override fun snapshot(): RegistrySnapshot = current

    private fun snapshotOf(
        providers: Collection<LanguagePackManifest>,
        capabilitiesByPackage: Map<String, List<LanguageCapability>>,
        resolvedByPackage: Map<String, List<ResolvedLanguageCapability>>
    ): RegistrySnapshot {
        val providersByLocale = providers
            .groupBy(LanguagePackManifest::locale)
            .mapValues { (_, manifests) -> manifests.sortedBy { it.identity.packageId } }
        val capabilitiesByState = providersByLocale.values
            .flatten()
            .flatMap { provider -> capabilitiesByPackage[provider.identity.packageId].orEmpty() }
            .groupBy { capability ->
                OrthogonalState(capability.id.locale, capability.id.script, capability.id.schema)
            }
            .mapValues { (_, capabilities) -> capabilities.sortedBy { it.id.packageId } }
        val resolvedByState = providersByLocale.values
            .flatten()
            .flatMap { provider -> resolvedByPackage[provider.identity.packageId].orEmpty() }
            .groupBy { resolved ->
                OrthogonalState(
                    resolved.capability.id.locale,
                    resolved.capability.id.script,
                    resolved.capability.id.schema
                )
            }
            .mapValues { (_, capabilities) -> capabilities.sortedBy { it.capability.id.packageId } }
        return RegistrySnapshot(providersByLocale, capabilitiesByState, capabilitiesByPackage.toMap(), resolvedByState)
    }
}
