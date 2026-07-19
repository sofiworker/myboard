package xyz.xiao6.myboard.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.contract.engine.CandidatePolicy
import xyz.xiao6.myboard.contract.engine.DisplayPolicy
import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.engine.PolicyAction
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.InputSessionState
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.registry.toLayoutCanonicalId
import xyz.xiao6.myboard.contract.layout.LayoutKey
import xyz.xiao6.myboard.contract.registry.LayoutSource
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.engine.CapabilityResourceResolution
import xyz.xiao6.myboard.engine.DictionaryRegistryImpl
import xyz.xiao6.myboard.engine.EngineRegistryImpl
import xyz.xiao6.myboard.engine.EngineResourceResolver
import xyz.xiao6.myboard.engine.ResourceResolution
import xyz.xiao6.myboard.layout.LayoutRegistryImpl
import xyz.xiao6.myboard.layout.BuiltInLayouts
import xyz.xiao6.myboard.pack.BuiltInLanguagePacks
import xyz.xiao6.myboard.engine.builtin.DirectEngine
import xyz.xiao6.myboard.contract.registry.ResourceKind

class KeyboardContextManagerTest {

    @Test(expected = IllegalStateException::class)
    fun `empty registry cannot create a context manager`() {
        val registry = testRegistry()

        KeyboardContextManagerImpl(
            transitionEngine = TransitionEngineImpl(registry),
            registry = registry,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
    }

    @Test
    fun `initial context is resolved from the registered manifest default`() {
        val registry = testRegistry()
        registry.register(BuiltInLanguagePacks.enUS)

        val manager = KeyboardContextManagerImpl(
            transitionEngine = TransitionEngineImpl(registry),
            registry = registry,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        assertEquals(LocaleTag("en-US"), manager.context.value.orthogonal.locale)
        assertEquals(BuiltInLanguagePacks.enUS.defaults.script, manager.context.value.orthogonal.script)
        assertEquals(BuiltInLanguagePacks.enUS.defaults.schema, manager.context.value.orthogonal.schema)
        assertEquals(BuiltInLanguagePacks.enUS.defaults.layout.toLayoutCanonicalId().value, manager.context.value.layoutId)
    }

    private fun testRegistry(): OrthogonalRegistryImpl {
        val engines = EngineRegistryImpl().apply { register(DirectEngine()) }
        val layouts = LayoutRegistryImpl().apply {
            register(LayoutKey("builtin", "qwerty", BuiltInLanguagePacks.packageVersion), BuiltInLayouts.qwerty, LayoutSource.BUILT_IN)
        }
        return OrthogonalRegistryImpl(
        engineRegistry = engines,
        layoutRegistry = layouts,
        dictionaryRegistry = DictionaryRegistryImpl(),
        engineResourceResolver = resolvedResourceResolver()
        )
    }

    private fun resolvedResourceResolver() = object : EngineResourceResolver {
        override fun resolve(capability: LanguageCapability) = CapabilityResourceResolution.Resolved(
            EngineResources(
                candidatePolicy = object : CandidatePolicy {
                    override val policyId = "test"
                    override fun sort(candidates: List<Candidate>) = candidates
                    override fun onSpace(state: InputSessionState) = PolicyAction.Noop
                    override fun onEnter(state: InputSessionState) = PolicyAction.Noop
                    override fun onCandidateSelected(state: InputSessionState, index: Int) = PolicyAction.Noop
                },
                displayPolicy = object : DisplayPolicy {
                    override val policyId = "test"
                    override fun display(state: InputSessionState) = ""
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
}
