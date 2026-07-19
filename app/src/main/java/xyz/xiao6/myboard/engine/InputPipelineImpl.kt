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
import xyz.xiao6.myboard.contract.input.EngineResult
import xyz.xiao6.myboard.contract.input.InputAction
import xyz.xiao6.myboard.contract.input.InputEvent
import xyz.xiao6.myboard.contract.input.ResetReason
import xyz.xiao6.myboard.contract.state.KeyboardContext
import xyz.xiao6.myboard.state.CapabilityRegistry
import xyz.xiao6.myboard.state.KeyboardContextManager

class InputPipelineImpl(
    private val capabilityRegistry: CapabilityRegistry,
    private val engineRegistry: EngineRegistry,
    private val keyboardContextManager: KeyboardContextManager,
    private val gateway: InputConnectionGateway,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : InputPipeline {

    private val mutex = Mutex()
    private var currentSession: xyz.xiao6.myboard.contract.engine.InputSession? = null
    private var currentContext: KeyboardContext? = null
    private var generation = 0L
    private var querySequence = 0L
    private val _engineResult = MutableStateFlow<EngineResult>(EngineResult.Nothing)
    val engineResult: StateFlow<EngineResult> = _engineResult.asStateFlow()

    override suspend fun handle(action: InputAction) {
        mutex.withLock {
        when (action) {
            is InputAction.PushToken -> dispatch(InputEvent.PushToken(action.token)) { gateway.commitText(action.token) }
            InputAction.Delete -> dispatch(InputEvent.Backspace) { gateway.deleteSurroundingText(1, 0) }
            InputAction.Space -> dispatch(InputEvent.Space) { gateway.commitText(" ") }
            InputAction.Enter -> dispatch(InputEvent.Enter) { gateway.performEditorAction(EditorInfo.IME_ACTION_UNSPECIFIED) }
            is InputAction.CommitCandidate -> if (action.index >= 0) dispatch(InputEvent.SelectCandidate(action.index)) { true }
            is InputAction.SwitchLocale -> {
                keyboardContextManager.switchLocale(action.locale)
                recreateSession(keyboardContextManager.context.value)
            }
            is InputAction.SwitchScript -> {
                keyboardContextManager.switchScript(action.script)
                recreateSession(keyboardContextManager.context.value)
            }
            is InputAction.SwitchSchema -> {
                keyboardContextManager.switchSchema(action.schema)
                recreateSession(keyboardContextManager.context.value)
            }
            is InputAction.SwitchLayer -> keyboardContextManager.switchLayer(action.layer)
            is InputAction.OpenPanel -> keyboardContextManager.openPanel(action.panelType)
            InputAction.ClosePanel -> keyboardContextManager.closePanel()
            is InputAction.PageCandidate, InputAction.RestorePreviousSchema, InputAction.Noop -> Unit
        }
        }
    }

    override suspend fun onContextChanged(context: KeyboardContext) = mutex.withLock {
        if (currentContext != context) recreateSession(context)
    }

    override suspend fun reset(reason: ResetReason) {
        mutex.withLock {
            generation++
            querySequence++
            currentSession?.reset(reason)
            clearComposing()
        }
    }

    private suspend fun dispatch(event: InputEvent, noSession: () -> Boolean) {
        val session = currentSession
        if (session == null) {
            if (!noSession()) clearComposing()
            return
        }
        val resultGeneration = generation
        val resultSequence = ++querySequence
        val result = session.handle(event)
        if (resultGeneration == generation && resultSequence == querySequence) {
            handleEngineResult(result)
        }
    }

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
