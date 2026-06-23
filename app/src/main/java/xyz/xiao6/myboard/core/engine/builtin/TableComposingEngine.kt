package xyz.xiao6.myboard.core.engine.builtin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.core.contract.*

/**
 * TableComposing 引擎（表组合输入）。
 * 适用于拼音等需要编码→候选→提交的输入方式。
 */
class TableComposingEngine : InputEngine {
    
    override val engineId: String = "table_composing"
    
    override fun createSession(context: EngineContext): InputSession {
        return TableComposingSession(context)
    }
    
    private class TableComposingSession(
        private val context: EngineContext
    ) : InputSession {
        
        override val capabilityId: CapabilityId = CapabilityId(
            packageId = "builtin",
            locale = context.keyboardContext.orthogonal.locale,
            script = context.keyboardContext.orthogonal.script,
            schema = context.keyboardContext.orthogonal.schema
        )
        
        private val _state = MutableStateFlow(InputSessionState())
        override val state: StateFlow<InputSessionState> = _state.asStateFlow()
        
        override suspend fun handle(event: InputEvent): EngineResult {
            val currentState = _state.value
            val resources = context.resources
            
            return when (event) {
                is InputEvent.PushToken -> {
                    val newBuffer = currentState.rawBuffer + event.token
                    
                    val encodingState = resources.encoder?.append(
                        EncodingState(rawBuffer = currentState.rawBuffer, queryBuffer = currentState.queryBuffer),
                        event.token
                    ) ?: EncodingState(rawBuffer = newBuffer, queryBuffer = newBuffer)
                    
                    val candidates = resources.dictionary?.lookup(encodingState.queryBuffer, 50) ?: emptyList()
                    val sortedCandidates = resources.candidatePolicy.sort(candidates)
                    val displayText = resources.displayPolicy.display(
                        currentState.copy(rawBuffer = encodingState.rawBuffer, queryBuffer = encodingState.queryBuffer)
                    )
                    
                    _state.value = InputSessionState(
                        rawBuffer = encodingState.rawBuffer,
                        queryBuffer = encodingState.queryBuffer,
                        composingText = displayText,
                        candidates = sortedCandidates
                    )
                    
                    EngineResult.UpdateComposing(text = displayText, candidates = sortedCandidates)
                }
                
                is InputEvent.Backspace -> {
                    if (currentState.rawBuffer.isNotEmpty()) {
                        val encodingState = resources.encoder?.backspace(
                            EncodingState(rawBuffer = currentState.rawBuffer, queryBuffer = currentState.queryBuffer)
                        ) ?: EncodingState(
                            rawBuffer = currentState.rawBuffer.dropLast(1),
                            queryBuffer = currentState.queryBuffer.dropLast(1)
                        )
                        
                        val candidates = resources.dictionary?.lookup(encodingState.queryBuffer, 50) ?: emptyList()
                        val sortedCandidates = resources.candidatePolicy.sort(candidates)
                        val displayText = resources.displayPolicy.display(
                            currentState.copy(rawBuffer = encodingState.rawBuffer, queryBuffer = encodingState.queryBuffer)
                        )
                        
                        _state.value = InputSessionState(
                            rawBuffer = encodingState.rawBuffer,
                            queryBuffer = encodingState.queryBuffer,
                            composingText = displayText,
                            candidates = sortedCandidates
                        )
                        
                        EngineResult.UpdateComposing(text = displayText, candidates = sortedCandidates)
                    } else {
                        EngineResult.DeleteText(1)
                    }
                }
                
                is InputEvent.Space -> {
                    val action = resources.candidatePolicy.onSpace(_state.value)
                    handlePolicyAction(action)
                }
                
                is InputEvent.Enter -> {
                    val action = resources.candidatePolicy.onEnter(_state.value)
                    handlePolicyAction(action)
                }
                
                is InputEvent.SelectCandidate -> {
                    val action = resources.candidatePolicy.onCandidateSelected(_state.value, event.index)
                    handlePolicyAction(action)
                }
                
                is InputEvent.SelectCandidateByLabel -> {
                    EngineResult.Nothing
                }
                
                is InputEvent.Reset -> {
                    reset(ResetReason.UserCleared)
                    EngineResult.ClearComposing
                }
            }
        }
        
        private fun handlePolicyAction(action: PolicyAction): EngineResult {
            return when (action) {
                is PolicyAction.Commit -> {
                    clearBuffer()
                    EngineResult.CommitText(action.text)
                }
                is PolicyAction.Update -> {
                    _state.value = action.state
                    EngineResult.UpdateComposing(text = action.state.composingText, candidates = action.state.candidates)
                }
                is PolicyAction.Delete -> EngineResult.DeleteText(action.beforeCursor)
                is PolicyAction.PerformEditorAction -> EngineResult.PerformEditorAction
                is PolicyAction.Noop -> EngineResult.Nothing
            }
        }
        
        private fun clearBuffer() {
            _state.value = InputSessionState()
        }
        
        override suspend fun reset(reason: ResetReason) {
            clearBuffer()
        }
        
        override suspend fun close() {}
    }
}
