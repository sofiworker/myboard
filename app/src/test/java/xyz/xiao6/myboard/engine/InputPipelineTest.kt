package xyz.xiao6.myboard.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.xiao6.myboard.androidbridge.InputConnectionGateway
import xyz.xiao6.myboard.contract.engine.EngineContext
import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.engine.InputEngine
import xyz.xiao6.myboard.contract.engine.InputSession
import xyz.xiao6.myboard.contract.input.EngineResult
import xyz.xiao6.myboard.contract.input.InputAction
import xyz.xiao6.myboard.contract.input.InputEvent
import xyz.xiao6.myboard.contract.input.InputSessionState
import xyz.xiao6.myboard.contract.input.ResetReason
import xyz.xiao6.myboard.contract.state.KeyboardContext
import xyz.xiao6.myboard.contract.state.LayoutLayer
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.state.KeyboardContextManager
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.bridge.EditorProfile
import xyz.xiao6.myboard.contract.state.PanelType
import xyz.xiao6.myboard.contract.state.TransitionResult
import xyz.xiao6.myboard.contract.state.TransitionRejectReason
import android.view.KeyEvent
import android.view.inputmethod.ExtractedText
import xyz.xiao6.myboard.contract.engine.CandidatePolicy
import xyz.xiao6.myboard.contract.engine.DisplayPolicy
import xyz.xiao6.myboard.contract.engine.PolicyAction
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.state.CapabilityRegistry
import xyz.xiao6.myboard.state.ResolvedLanguageCapability
import xyz.xiao6.myboard.engine.builtin.DirectEngine
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog

class InputPipelineTest {

    @Test
    fun `push token is handled by the recreated input session`() = runBlocking {
        val context = testContext()
        val engine = RecordingEngine(engineId = context.orthogonal.schema.value)
        val gateway = RecordingGateway()
        val pipeline = InputPipelineImpl(
            capabilityRegistry = TestCapabilityRegistry(context.orthogonal, engine.engineId),
            engineRegistry = EngineRegistryImpl().apply { register(engine) },
            keyboardContextManager = TestKeyboardContextManager(context),
            gateway = gateway,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        pipeline.onContextChanged(context)
        pipeline.handle(InputAction.PushToken("a"))

        assertEquals(listOf(InputEvent.PushToken("a")), engine.events)
        assertEquals(emptyList<String>(), gateway.commits)
    }

    @Test
    fun `rejected schema switch keeps the current input session`() = runBlocking {
        val context = testContext()
        val engine = RecordingEngine(engineId = context.orthogonal.schema.value)
        val pipeline = InputPipelineImpl(
            capabilityRegistry = TestCapabilityRegistry(context.orthogonal, engine.engineId),
            engineRegistry = EngineRegistryImpl().apply { register(engine) },
            keyboardContextManager = TestKeyboardContextManager(context),
            gateway = RecordingGateway(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )

        pipeline.onContextChanged(context)
        pipeline.handle(InputAction.SwitchSchema(Schema("UNSUPPORTED")))

        assertEquals(1, engine.createdSessionCount)
        assertEquals(0, engine.closedSessionCount)
    }

    @Test
    fun `direct session preserves the resolved capability package identity`() {
        val context = testContext()
        val capability = LanguageCapability(
            id = CapabilityId("external.provider", context.orthogonal.locale, context.orthogonal.script, context.orthogonal.schema),
            engine = EngineBinding("direct"),
            layout = ResourceRef("external.provider", "layouts/qwerty.jsonc", ResourceKind.LAYOUT),
            dictionaries = emptyList(),
            candidatePolicyId = "test",
            displayPolicyId = "test",
            supportsShift = false
        )
        val resources = TestCapabilityRegistry(context.orthogonal, "direct").resources

        val session = DirectEngine().createSession(
            EngineContext(context, capability, resources, CoroutineScope(Dispatchers.Unconfined))
        )

        assertEquals(capability.id, session.capabilityId)
        assertEquals(capability.id, session.capabilityKey.capabilityId)
    }

    @Test
    fun `context replacement invalidates a late result from the old session`() = runBlocking {
        val context = testContext()
        val replacement = context.copy(layoutId = "builtin:replacement")
        val engine = DelayedEngine(context.orthogonal.schema.value)
        val gateway = RecordingGateway()
        val pipeline = InputPipelineImpl(
            capabilityRegistry = TestCapabilityRegistry(context.orthogonal, engine.engineId),
            engineRegistry = EngineRegistryImpl().apply { register(engine) },
            keyboardContextManager = TestKeyboardContextManager(context),
            gateway = gateway,
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
        pipeline.onContextChanged(context)

        val input = async { pipeline.handle(InputAction.PushToken("old")) }
        engine.started.await()
        val replace = async { pipeline.onContextChanged(replacement) }
        yield()

        assertTrue("Context replacement must not wait for the old lookup", replace.isCompleted)
        engine.release.complete(Unit)
        input.await()
        replace.await()
        assertEquals(emptyList<String>(), gateway.commits)
        assertEquals(1, engine.closedSessionCount)
        assertEquals(2, engine.createdSessionCount)
    }

    @Test
    fun `resource generation change recreates the session for the same keyboard context`() = runBlocking {
        val context = testContext()
        val engine = RecordingEngine(context.orthogonal.schema.value)
        val registry = TestCapabilityRegistry(context.orthogonal, engine.engineId)
        val pipeline = InputPipelineImpl(
            capabilityRegistry = registry,
            engineRegistry = EngineRegistryImpl().apply { register(engine) },
            keyboardContextManager = TestKeyboardContextManager(context),
            gateway = RecordingGateway(),
            scope = CoroutineScope(Dispatchers.Unconfined)
        )
        pipeline.onContextChanged(context)
        registry.updateLayoutGeneration(
            ResolvedResourceKey(
                packageId = "builtin",
                packageVersion = SemVer(2, 0, 0),
                normalizedPath = "layouts/qwerty.jsonc",
                kind = ResourceKind.LAYOUT,
                sha256 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
            )
        )

        pipeline.onContextChanged(context)

        assertEquals(2, engine.createdSessionCount)
        assertEquals(1, engine.closedSessionCount)
    }

    private fun testContext() = KeyboardContext(
        orthogonal = OrthogonalState(LocaleTag("en-US"), Script.LATN, Schema("DIRECT")),
        layoutId = "builtin:qwerty",
        layer = LayoutLayer.NORMAL
    )

    private class RecordingEngine(override val engineId: String) : InputEngine {
        val events = mutableListOf<InputEvent>()
        var createdSessionCount = 0
        var closedSessionCount = 0

        override fun createSession(context: EngineContext): InputSession = object : InputSession {
            init {
                createdSessionCount++
            }
            override val capabilityId = context.capability.id
            override val capabilityKey = xyz.xiao6.myboard.contract.engine.ResolvedCapabilityKey(capabilityId, emptyMap())
            override val state: StateFlow<InputSessionState> = MutableStateFlow(InputSessionState())

            override suspend fun handle(event: InputEvent): EngineResult {
                events += event
                return EngineResult.Nothing
            }

            override suspend fun reset(reason: ResetReason) = Unit
            override suspend fun close() {
                closedSessionCount++
            }
        }
    }

    private class DelayedEngine(override val engineId: String) : InputEngine {
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var createdSessionCount = 0
        var closedSessionCount = 0

        override fun createSession(context: EngineContext): InputSession {
            val sessionNumber = ++createdSessionCount
            return object : InputSession {
                override val capabilityId = context.capability.id
                override val capabilityKey = xyz.xiao6.myboard.contract.engine.ResolvedCapabilityKey(capabilityId, emptyMap())
                override val state: StateFlow<InputSessionState> = MutableStateFlow(InputSessionState())

                override suspend fun handle(event: InputEvent): EngineResult {
                    if (sessionNumber == 1) {
                        started.complete(Unit)
                        release.await()
                    }
                    return EngineResult.CommitText("late")
                }

                override suspend fun reset(reason: ResetReason) = Unit
                override suspend fun close() {
                    closedSessionCount++
                }
            }
        }
    }

    private class TestCapabilityRegistry(state: OrthogonalState, engineId: String) : CapabilityRegistry {
        private val capability = LanguageCapability(
            id = CapabilityId("test", state.locale, state.script, state.schema),
            engine = EngineBinding(engineId),
            layout = ResourceRef("builtin", "layouts/qwerty.jsonc", ResourceKind.LAYOUT),
            dictionaries = emptyList(),
            candidatePolicyId = "test",
            displayPolicyId = "test",
            supportsShift = false
        )
        val resources = EngineResources(
            candidatePolicy = object : CandidatePolicy {
                override val policyId = "test"
                override fun sort(candidates: List<Candidate>) = candidates
                override fun onSpace(state: InputSessionState) = PolicyAction.Noop
                override fun onEnter(state: InputSessionState) = PolicyAction.Noop
                override fun onCandidateSelected(state: InputSessionState, index: Int) = PolicyAction.Noop
            },
            displayPolicy = object : DisplayPolicy {
                override val policyId = "test"
                override fun display(state: InputSessionState) = state.rawBuffer
            }
        )

        private var resolvedResourceKeys = emptyMap<ResourceRef, ResolvedResourceKey>()

        fun updateLayoutGeneration(key: ResolvedResourceKey) {
            resolvedResourceKeys = mapOf(capability.layout to key)
        }

        override fun resolve(state: OrthogonalState): ResolvedLanguageCapability? =
            ResolvedLanguageCapability(
                capability,
                resources.copy(resolvedResources = resolvedResourceKeys),
                checkNotNull(ScriptCatalog[capability.id.script])
            ).takeIf { it.capability.id.locale == state.locale && it.capability.id.script == state.script && it.capability.id.schema == state.schema }
    }

    private class TestKeyboardContextManager(initial: KeyboardContext) : KeyboardContextManager {
        private val mutableContext = MutableStateFlow(initial)
        override val context: StateFlow<KeyboardContext> = mutableContext
        override fun switchLocale(locale: LocaleTag): TransitionResult = rejected()
        override fun switchScript(script: Script): TransitionResult = rejected()
        override fun switchSchema(schema: Schema): TransitionResult = rejected()
        override fun switchLayer(layer: LayoutLayer): TransitionResult = rejected()
        override fun openPanel(panel: PanelType): TransitionResult = rejected()
        override fun closePanel(): TransitionResult = rejected()
        override fun setComposing(text: String, candidates: List<Candidate>): TransitionResult = rejected()
        override fun clearComposing(): TransitionResult = rejected()
        override fun applyEditorProfile(profile: EditorProfile): TransitionResult = rejected()

        private fun rejected() = TransitionResult.Rejected(TransitionRejectReason.ILLEGAL_COMBINATION)
    }

    private class RecordingGateway : InputConnectionGateway {
        val commits = mutableListOf<String>()
        override fun commitText(text: String): Boolean = true.also { commits += text }
        override fun setComposingText(text: String): Boolean = true
        override fun finishComposingText(): Boolean = true
        override fun deleteSurroundingText(before: Int, after: Int): Boolean = true
        override fun setSelection(start: Int, end: Int): Boolean = true
        override fun performEditorAction(action: Int): Boolean = true
        override fun getExtractedText(): ExtractedText? = null
        override fun sendKeyEvent(keyEvent: KeyEvent): Boolean = true
        override fun finishAndCommit(commit: String): Boolean = commitText(commit)
    }
}
