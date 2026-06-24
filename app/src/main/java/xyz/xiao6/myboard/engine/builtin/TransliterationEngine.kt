package xyz.xiao6.myboard.engine.builtin
import xyz.xiao6.myboard.contract.manifest.CapabilityId

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * Transliteration 引擎（转写输入）。
 * 适用于日文罗马字→假名等基于 FSM 的输入方式。
 */
class TransliterationEngine : InputEngine {
    
    override val engineId: String = "transliteration"
    
    override fun createSession(context: EngineContext): InputSession {
        return TransliterationSession(context)
    }
    
    private class TransliterationSession(
        private val context: EngineContext
    ) : InputSession {
        
        override val capabilityId: CapabilityId = CapabilityId(
            packageId = "builtin",
            locale = context.keyboardContext.orthogonal.locale,
            script = context.keyboardContext.orthogonal.script,
            schema = context.keyboardContext.orthogonal.schema
        )
        
        private var fsmState: String = context.resources.fsm?.startState ?: ""
        
        private val _state = MutableStateFlow(InputSessionState())
        override val state: StateFlow<InputSessionState> = _state.asStateFlow()
        
        override suspend fun handle(event: InputEvent): EngineResult {
            val currentState = _state.value
            val resources = context.resources
            
            return when (event) {
                is InputEvent.PushToken -> {
                    val fsm = resources.fsm
                    if (fsm != null) {
                        val transition = fsm.states[fsmState]?.get(event.token)
                        val emit = transition?.emit
                        val nextState = transition?.next ?: fsm.startState
                        fsmState = nextState
                        
                        val newBuffer = currentState.rawBuffer + event.token
                        val displayText = resources.displayPolicy.display(
                            currentState.copy(rawBuffer = newBuffer)
                        )
                        
                        if (emit != null) {
                            val candidates = resources.dictionary?.lookup(emit, 50) ?: emptyList()
                            val sortedCandidates = resources.candidatePolicy.sort(candidates)
                            
                            _state.value = currentState.copy(
                                rawBuffer = newBuffer,
                                composingText = displayText,
                                candidates = sortedCandidates
                            )
                            
                            EngineResult.CommitAndUpdate(
                                commit = emit,
                                composing = displayText,
                                candidates = sortedCandidates
                            )
                        } else {
                            _state.value = currentState.copy(
                                rawBuffer = newBuffer,
                                composingText = displayText
                            )
                            EngineResult.UpdateComposing(text = displayText, candidates = emptyList())
                        }
                    } else {
                        EngineResult.CommitText(event.token)
                    }
                }
                
                is InputEvent.Backspace -> {
                    if (currentState.rawBuffer.isNotEmpty()) {
                        val newBuffer = currentState.rawBuffer.dropLast(1)
                        fsmState = context.resources.fsm?.startState ?: ""
                        
                        val displayText = resources.displayPolicy.display(
                            currentState.copy(rawBuffer = newBuffer)
                        )
                        
                        _state.value = currentState.copy(
                            rawBuffer = newBuffer,
                            composingText = displayText
                        )
                        EngineResult.UpdateComposing(text = displayText, candidates = emptyList())
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
                
                is InputEvent.SelectCandidateByLabel -> EngineResult.Nothing
                
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
            fsmState = context.resources.fsm?.startState ?: ""
        }
        
        override suspend fun reset(reason: ResetReason) {
            clearBuffer()
        }
        
        override suspend fun close() {}
    }
}
