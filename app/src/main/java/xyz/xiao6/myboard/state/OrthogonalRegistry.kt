package xyz.xiao6.myboard.state

import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.engine.ResolvedCapabilityKey
import xyz.xiao6.myboard.contract.registry.RegisterResult
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

interface CapabilityRegistry {
    fun resolve(state: OrthogonalState): ResolvedLanguageCapability?
}

data class ResolvedLanguageCapability(
    val capability: LanguageCapability,
    val resources: EngineResources
) {
    val key = ResolvedCapabilityKey(capability.id, resources.resolvedResources)
}

interface OrthogonalRegistry : CapabilityRegistry {
    fun register(manifest: LanguagePackManifest): RegisterResult
    fun unregister(packageId: String): RegisterResult
    fun getLocale(locale: LocaleTag): LanguagePackManifest?
    fun isSupported(state: OrthogonalState): Boolean
    fun defaultState(locale: LocaleTag): OrthogonalState?
    fun defaultSchema(locale: LocaleTag, script: Script): Schema?
    fun schemaCapability(state: OrthogonalState): LanguageCapability?
    fun snapshot(): RegistrySnapshot
}

data class RegistrySnapshot(
    val providersByLocale: Map<LocaleTag, List<LanguagePackManifest>> = emptyMap(),
    val capabilitiesByState: Map<OrthogonalState, List<LanguageCapability>> = emptyMap(),
    val capabilitiesByPackage: Map<String, List<LanguageCapability>> = emptyMap(),
    val resolvedCapabilitiesByState: Map<OrthogonalState, List<ResolvedLanguageCapability>> = emptyMap()
) {
    fun capabilities(state: OrthogonalState): List<LanguageCapability> =
        capabilitiesByState[state].orEmpty()

    fun provider(locale: LocaleTag): LanguagePackManifest? = providersByLocale[locale]?.firstOrNull()

    fun capability(state: OrthogonalState): LanguageCapability? = capabilities(state).firstOrNull()

    fun resolvedCapability(state: OrthogonalState): ResolvedLanguageCapability? =
        resolvedCapabilitiesByState[state].orEmpty().firstOrNull()
}
