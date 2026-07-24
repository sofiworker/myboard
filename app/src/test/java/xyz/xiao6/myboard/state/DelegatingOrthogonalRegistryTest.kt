package xyz.xiao6.myboard.state

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.contract.engine.CandidatePolicy
import xyz.xiao6.myboard.contract.engine.DisplayPolicy
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.engine.PolicyAction
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.InputSessionState
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.LocaleDefaults
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog
import xyz.xiao6.myboard.contract.manifest.ScriptManifest
import xyz.xiao6.myboard.contract.registry.RegisterResult
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

class DelegatingOrthogonalRegistryTest {

    @Test
    fun `replacement applies preferred provider atomically`() {
        val state = OrthogonalState(LocaleTag("en-US"), Script.LATN, Schema("DIRECT"))
        val first = resolved("provider.a", state)
        val second = resolved("provider.b", state)
        val delegate = DelegatingOrthogonalRegistry(FakeRegistry(state, listOf(first)))

        assertEquals("provider.a", delegate.resolve(state)?.capability?.id?.packageId)

        delegate.replace(FakeRegistry(state, listOf(first, second)), mapOf(state to "provider.b"))

        assertEquals("provider.b", delegate.resolve(state)?.capability?.id?.packageId)
        assertEquals("provider.b", delegate.schemaCapability(state)?.id?.packageId)
    }

    private fun resolved(packageId: String, state: OrthogonalState): ResolvedLanguageCapability {
        val layout = ResourceRef("builtin", "layouts/qwerty.json", ResourceKind.LAYOUT)
        val capability = LanguageCapability(
            CapabilityId(packageId, state.locale, state.script, state.schema),
            EngineBinding("direct"), layout, emptyList(), candidatePolicyId = "direct",
            supportsShift = true
        )
        return ResolvedLanguageCapability(
            capability,
            EngineResources(candidatePolicy = policy, displayPolicy = display),
            checkNotNull(ScriptCatalog[state.script])
        )
    }

    private class FakeRegistry(
        private val state: OrthogonalState,
        private val resolved: List<ResolvedLanguageCapability>
    ) : OrthogonalRegistry {
        override fun resolve(state: OrthogonalState) = snapshot().resolvedCapability(state)
        override fun register(manifest: LanguagePackManifest): RegisterResult = RegisterResult.Success(manifest.identity.packageId)
        override fun unregister(packageId: String): RegisterResult = RegisterResult.Success(packageId)
        override fun getLocale(locale: LocaleTag): LanguagePackManifest? = null
        override fun isSupported(state: OrthogonalState): Boolean = state == this.state
        override fun defaultState(locale: LocaleTag): OrthogonalState? = state.takeIf { it.locale == locale }
        override fun defaultSchema(locale: LocaleTag, script: Script): Schema? = state.schema
        override fun schemaCapability(state: OrthogonalState) = snapshot().capability(state)
        override fun snapshot() = RegistrySnapshot(
            capabilitiesByState = mapOf(state to resolved.map { it.capability }),
            capabilitiesByPackage = resolved.groupBy { it.capability.id.packageId }.mapValues { it.value.map(ResolvedLanguageCapability::capability) },
            resolvedCapabilitiesByState = mapOf(state to resolved)
        )
    }

    private companion object {
        val policy = object : CandidatePolicy {
            override val policyId = "test"
            override fun sort(candidates: List<Candidate>) = candidates
            override fun onSpace(state: InputSessionState) = PolicyAction.Noop
            override fun onEnter(state: InputSessionState) = PolicyAction.Noop
            override fun onCandidateSelected(state: InputSessionState, index: Int) = PolicyAction.Noop
        }
        val display = object : DisplayPolicy {
            override val policyId = "test"
            override fun display(state: InputSessionState) = state.rawBuffer
        }
    }
}
