package xyz.xiao6.myboard.state

import java.util.concurrent.atomic.AtomicReference
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.registry.RegisterResult
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class DelegatingOrthogonalRegistry(initial: OrthogonalRegistry) : OrthogonalRegistry {
    private val delegate = AtomicReference(initial)
    private val providerPreferences = AtomicReference<Map<OrthogonalState, String>>(emptyMap())

    fun replace(registry: OrthogonalRegistry, preferences: Map<OrthogonalState, String> = emptyMap()) {
        providerPreferences.set(preferences.toMap())
        delegate.set(registry)
    }

    override fun resolve(state: OrthogonalState): ResolvedLanguageCapability? {
        val candidates = delegate.get().snapshot().resolvedCapabilitiesByState[state].orEmpty()
        val preferred = providerPreferences.get()[state]
        return candidates.firstOrNull { it.capability.id.packageId == preferred } ?: candidates.firstOrNull()
    }

    override fun schemaCapability(state: OrthogonalState): LanguageCapability? = resolve(state)?.capability
    override fun register(manifest: LanguagePackManifest): RegisterResult = delegate.get().register(manifest)
    override fun unregister(packageId: String): RegisterResult = delegate.get().unregister(packageId)
    override fun getLocale(locale: LocaleTag): LanguagePackManifest? = delegate.get().getLocale(locale)
    override fun isSupported(state: OrthogonalState): Boolean = delegate.get().isSupported(state)
    override fun defaultState(locale: LocaleTag): OrthogonalState? = delegate.get().defaultState(locale)
    override fun defaultSchema(locale: LocaleTag, script: Script): Schema? = delegate.get().defaultSchema(locale, script)
    override fun snapshot(): RegistrySnapshot = delegate.get().snapshot()
}
