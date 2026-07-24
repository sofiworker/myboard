package xyz.xiao6.myboard.engine.builtin
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
 * Direct 引擎（直接输入）。
 * PushToken 直接 commit，不进入组合态。
 */
class DirectEngine : InputEngine {
    
    override val engineId: String = "direct"
    
    override fun createSession(context: EngineContext): InputSession {
        return DirectSession(context)
    }
    
    private class DirectSession(
        private val context: EngineContext
    ) : InputSession {
        
        override val capabilityId: CapabilityId = context.capability.id
        override val capabilityKey = ResolvedCapabilityKey(capabilityId, context.resources.resolvedResources)
        
        private val _state = MutableStateFlow(InputSessionState())
        override val state: StateFlow<InputSessionState> = _state.asStateFlow()
        
        override suspend fun handle(event: InputEvent): EngineResult {
            return when (event) {
                is InputEvent.PushToken -> EngineResult.CommitText(event.token)
                is InputEvent.Backspace -> EngineResult.DeleteText(1)
                is InputEvent.Space -> EngineResult.CommitText(" ")
                is InputEvent.Enter -> EngineResult.PerformEditorAction
                is InputEvent.SelectCandidate -> EngineResult.Nothing
                is InputEvent.SelectCandidateByLabel -> EngineResult.Nothing
                is InputEvent.Reset -> { reset(ResetReason.UserCleared); EngineResult.ClearComposing }
            }
        }
        
        override suspend fun reset(reason: ResetReason) {
            _state.value = InputSessionState()
        }
        
        override suspend fun close() {}
    }
}
