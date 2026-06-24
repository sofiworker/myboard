package xyz.xiao6.myboard.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.androidbridge.InputConnectionGateway
import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*
import xyz.xiao6.myboard.state.KeyboardContextManager

/**
 * InputPipeline 真实实现。
 */
class InputPipelineImpl(
    private val engineRegistry: EngineRegistry,
    private val keyboardContextManager: KeyboardContextManager,
    private val gateway: InputConnectionGateway,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) : InputPipeline {
    
    private var currentSession: InputSession? = null
    private var currentContext: KeyboardContext? = null
    private val _engineResult = MutableStateFlow<EngineResult>(EngineResult.Nothing)
    val engineResult: StateFlow<EngineResult> = _engineResult.asStateFlow()
    
    override suspend fun handle(action: InputAction) {
        val session = currentSession
        
        when (action) {
            is InputAction.PushToken -> {
                if (session != null) {
                    val result = session.handle(InputEvent.PushToken(action.token))
                    handleEngineResult(result)
                } else {
                    gateway.commitText(action.token)
                }
            }
            
            is InputAction.Delete -> {
                if (session != null) {
                    val result = session.handle(InputEvent.Backspace)
                    handleEngineResult(result)
                } else {
                    gateway.deleteSurroundingText(1, 0)
                }
            }
            
            is InputAction.Space -> {
                if (session != null) {
                    val result = session.handle(InputEvent.Space)
                    handleEngineResult(result)
                } else {
                    gateway.commitText(" ")
                }
            }
            
            is InputAction.Enter -> {
                if (session != null) {
                    val result = session.handle(InputEvent.Enter)
                    handleEngineResult(result)
                } else {
                    gateway.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED)
                }
            }
            
            is InputAction.CommitCandidate -> {
                if (session != null && action.index >= 0) {
                    val result = session.handle(InputEvent.SelectCandidate(action.index))
                    handleEngineResult(result)
                }
            }
            
            is InputAction.SwitchLocale -> {
                keyboardContextManager.switchLocale(action.locale)
                recreateSessionIfNeeded()
            }
            
            is InputAction.SwitchScript -> {
                keyboardContextManager.switchScript(action.script)
                recreateSessionIfNeeded()
            }
            
            is InputAction.SwitchSchema -> {
                keyboardContextManager.switchSchema(action.schema)
                recreateSessionIfNeeded()
            }
            
            is InputAction.SwitchLayer -> {
                keyboardContextManager.switchLayer(action.layer)
            }
            
            is InputAction.OpenPanel -> {
                keyboardContextManager.openPanel(action.panelType)
            }
            
            is InputAction.ClosePanel -> {
                keyboardContextManager.closePanel()
            }
            
            is InputAction.PageCandidate -> {
                // TODO: 候选分页
            }
            
            is InputAction.RestorePreviousSchema -> {
                // TODO: 恢复上一个 schema
            }
            
            is InputAction.Noop -> {
                // No operation
            }
        }
    }
    
    override suspend fun onContextChanged(context: KeyboardContext) {
        if (currentContext != context) {
            currentContext = context
            recreateSession(context)
        }
    }
    
    override suspend fun reset(reason: ResetReason) {
        currentSession?.reset(reason)
        gateway.finishComposingText()
        keyboardContextManager.clearComposing()
    }
    
    private fun recreateSessionIfNeeded() {
        val newContext = keyboardContextManager.context.value
        if (currentContext != newContext) {
            currentContext = newContext
            scope.launch {
                recreateSession(newContext)
            }
        }
    }
    
    private suspend fun recreateSession(context: KeyboardContext) {
        currentSession?.close()
        val engineId = context.orthogonal.schema.value
        val engine = engineRegistry.get(engineId)
        if (engine != null) {
            // TODO: 创建真实 EngineContext
            // currentSession = engine.createSession(engineContext)
            currentSession = null
        } else {
            currentSession = null
        }
    }
    
    private fun handleEngineResult(result: EngineResult) {
        _engineResult.value = result
        
        when (result) {
            is EngineResult.CommitText -> {
                gateway.finishComposingText()
                gateway.commitText(result.text)
                keyboardContextManager.clearComposing()
            }
            is EngineResult.UpdateComposing -> {
                gateway.setComposingText(result.text)
                keyboardContextManager.setComposing(result.text, result.candidates)
            }
            is EngineResult.CommitAndUpdate -> {
                gateway.commitText(result.commit)
                gateway.setComposingText(result.composing)
                keyboardContextManager.setComposing(result.composing, result.candidates)
            }
            is EngineResult.DeleteText -> {
                gateway.deleteSurroundingText(result.beforeCursor, 0)
            }
            is EngineResult.PerformEditorAction -> {
                gateway.performEditorAction(android.view.inputmethod.EditorInfo.IME_ACTION_UNSPECIFIED)
            }
            is EngineResult.ClearComposing -> {
                gateway.finishComposingText()
                keyboardContextManager.clearComposing()
            }
            is EngineResult.Nothing -> {
                // No operation
            }
        }
    }
}
