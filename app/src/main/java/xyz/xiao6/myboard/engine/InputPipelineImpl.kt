package xyz.xiao6.myboard.engine

import android.view.inputmethod.EditorInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.xiao6.myboard.androidbridge.InputConnectionGateway
import xyz.xiao6.myboard.contract.engine.EngineContext
import xyz.xiao6.myboard.contract.engine.ResolvedCapabilityKey
import xyz.xiao6.myboard.contract.input.EngineResult
import xyz.xiao6.myboard.contract.input.InputAction
import xyz.xiao6.myboard.contract.input.InputEvent
import xyz.xiao6.myboard.contract.input.ResetReason
import xyz.xiao6.myboard.contract.state.KeyboardContext
import xyz.xiao6.myboard.contract.state.TransitionResult
import xyz.xiao6.myboard.state.CapabilityRegistry
import xyz.xiao6.myboard.state.KeyboardContextManager

class InputPipelineImpl(
    private val capabilityRegistry: CapabilityRegistry,
    private val engineRegistry: EngineRegistry,
    private val keyboardContextManager: KeyboardContextManager,
    private val gateway: InputConnectionGateway,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : InputPipeline {

    private val lifecycleMutex = Mutex()
    private val inputMutex = Mutex()
    private var currentSession: xyz.xiao6.myboard.contract.engine.InputSession? = null
    private var currentContext: KeyboardContext? = null
    private var generation = 0L
    private var querySequence = 0L
    private val _engineResult = MutableStateFlow<EngineResult>(EngineResult.Nothing)
    val engineResult: StateFlow<EngineResult> = _engineResult.asStateFlow()

    override suspend fun handle(action: InputAction) {
        when (action) {
            is InputAction.SwitchLocale -> lifecycleMutex.withLock {
                recreateSessionIfApplied(keyboardContextManager.switchLocale(action.locale))
            }
            is InputAction.SwitchScript -> lifecycleMutex.withLock {
                recreateSessionIfApplied(keyboardContextManager.switchScript(action.script))
            }
            is InputAction.SwitchSchema -> lifecycleMutex.withLock {
                recreateSessionIfApplied(keyboardContextManager.switchSchema(action.schema))
            }
            else -> inputMutex.withLock { handleSerialized(action) }
        }
    }

    private suspend fun handleSerialized(action: InputAction) {
        when (action) {
            is InputAction.PushToken -> dispatch(InputEvent.PushToken(action.token)) { gateway.commitText(action.token) }
            InputAction.Delete -> dispatch(InputEvent.Backspace) { gateway.deleteSurroundingText(1, 0) }
            InputAction.Space -> dispatch(InputEvent.Space) { gateway.commitText(" ") }
            InputAction.Enter -> dispatch(InputEvent.Enter) { gateway.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED) }
            is InputAction.CommitCandidate -> if (action.index >= 0) dispatch(InputEvent.SelectCandidate(action.index)) { true }
            is InputAction.SwitchLayer -> keyboardContextManager.switchLayer(action.layer)
            is InputAction.OpenPanel -> keyboardContextManager.openPanel(action.panelType)
            InputAction.ClosePanel -> keyboardContextManager.closePanel()
            is InputAction.PageCandidate, InputAction.RestorePreviousSchema, InputAction.Noop -> Unit
            is InputAction.SwitchLocale, is InputAction.SwitchScript, is InputAction.SwitchSchema -> error("Session switches are handled outside the input queue")
        }
    }

    override suspend fun onContextChanged(context: KeyboardContext) = lifecycleMutex.withLock {
        val desiredKey = capabilityRegistry.resolve(context.orthogonal)?.let { resolved ->
            ResolvedCapabilityKey(resolved.capability.id, resolved.resources.resolvedResources)
        }
        if (currentContext != context || currentSession?.capabilityKey != desiredKey) {
            recreateSession(context)
        }
    }

    override suspend fun reset(reason: ResetReason) {
        lifecycleMutex.withLock {
            generation++
            querySequence++
            currentSession?.reset(reason)
            clearComposing()
        }
    }

    private suspend fun dispatch(event: InputEvent, noSession: () -> Boolean) {
        val snapshot = lifecycleMutex.withLock {
            val session = currentSession ?: return@withLock null
            DispatchSnapshot(session, generation, ++querySequence)
        }
        val session = snapshot?.session
        if (session == null) {
            if (!noSession()) clearComposing()
            return
        }
        val result = session.handle(event)
        lifecycleMutex.withLock {
            if (snapshot.generation == generation && snapshot.sequence == querySequence && snapshot.session === currentSession) {
                handleEngineResult(result)
            }
        }
    }

    private data class DispatchSnapshot(
        val session: xyz.xiao6.myboard.contract.engine.InputSession,
        val generation: Long,
        val sequence: Long
    )

    private suspend fun recreateSession(context: KeyboardContext) {
        generation++
        querySequence++
        currentSession?.close()
        currentSession = null
        clearComposing()
        currentContext = context

        val resolved = capabilityRegistry.resolve(context.orthogonal) ?: return
        val engine = engineRegistry.get(resolved.capability.engine.engineId) ?: return
        currentSession = engine.createSession(
            EngineContext(
                keyboardContext = context,
                capability = resolved.capability,
                resources = resolved.resources,
                coroutineScope = scope
            )
        )
    }

    private suspend fun recreateSessionIfApplied(result: TransitionResult) {
        if (result is TransitionResult.Applied && result.context != currentContext) {
            recreateSession(result.context)
        }
    }

    private fun handleEngineResult(result: EngineResult) {
        _engineResult.value = result
        val succeeded = when (result) {
            is EngineResult.CommitText -> gateway.finishAndCommit(result.text).also { keyboardContextManager.clearComposing() }
            is EngineResult.UpdateComposing -> gateway.setComposingText(result.text).also {
                if (it) keyboardContextManager.setComposing(result.text, result.candidates)
            }
            is EngineResult.CommitAndUpdate -> gateway.commitText(result.commit) && gateway.setComposingText(result.composing).also {
                if (it) keyboardContextManager.setComposing(result.composing, result.candidates)
            }
            is EngineResult.DeleteText -> gateway.deleteSurroundingText(result.beforeCursor, 0)
            EngineResult.PerformEditorAction -> gateway.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED)
            EngineResult.ClearComposing -> clearComposing()
            EngineResult.Nothing -> true
        }
        if (!succeeded) clearComposing()
    }

    private fun clearComposing(): Boolean {
        val finished = gateway.finishComposingText()
        keyboardContextManager.clearComposing()
        return finished
    }
}
