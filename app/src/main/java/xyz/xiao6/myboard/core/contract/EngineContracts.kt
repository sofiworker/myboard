package xyz.xiao6.myboard.core.contract

/**
 * 编码器状态。
 */
data class EncodingState(
    val rawBuffer: String,
    val queryBuffer: String,
    val displayText: String = queryBuffer
)

/**
 * 编码器接口。
 */
interface Encoder {
    val encoderId: String
    
    fun append(state: EncodingState, token: String): EncodingState
    fun backspace(state: EncodingState): EncodingState
}

/**
 * 字典接口。
 */
interface Dictionary {
    val dictionaryId: String
    
    suspend fun lookup(query: String, limit: Int): List<Candidate>
}

/**
 * 候选策略动作。
 */
sealed interface PolicyAction {
    data class Commit(val text: String) : PolicyAction
    data class Update(val state: InputSessionState) : PolicyAction
    data class Delete(val beforeCursor: Int) : PolicyAction
    data object PerformEditorAction : PolicyAction
    data object Noop : PolicyAction
}

/**
 * 候选策略接口。
 */
interface CandidatePolicy {
    val policyId: String
    
    fun sort(candidates: List<Candidate>): List<Candidate>
    fun onSpace(state: InputSessionState): PolicyAction
    fun onEnter(state: InputSessionState): PolicyAction
    fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction
}

/**
 * 显示策略接口。
 */
interface DisplayPolicy {
    val policyId: String
    
    fun display(state: InputSessionState): String
}

/**
 * 引擎上下文。
 */
data class EngineContext(
    val keyboardContext: KeyboardContext,
    val capability: SchemaCapability,
    val resources: EngineResources,
    val coroutineScope: kotlinx.coroutines.CoroutineScope
)

/**
 * 引擎资源。
 */
data class EngineResources(
    val mapping: KeyMapping? = null,
    val encoder: Encoder? = null,
    val fsm: TransliterationFsm? = null,
    val dictionary: Dictionary? = null,
    val candidatePolicy: CandidatePolicy,
    val displayPolicy: DisplayPolicy
)

/**
 * 按键映射表。
 */
data class KeyMapping(
    val id: String,
    val layers: Map<String, Map<String, String>>,
    val fallback: KeyMappingFallback = KeyMappingFallback()
)

/**
 * 按键映射回退策略。
 */
data class KeyMappingFallback(
    val unknownToken: String = "commitToken"
)

/**
 * 转写 FSM。
 */
data class TransliterationFsm(
    val id: String,
    val startState: String,
    val states: Map<String, Map<String, FsmTransition>>
)

/**
 * FSM 状态转移。
 */
data class FsmTransition(
    val next: String? = null,
    val emit: String? = null
)

/**
 * 输入引擎接口（工厂，无状态）。
 */
interface InputEngine {
    val engineId: String
    
    fun createSession(context: EngineContext): InputSession
}

/**
 * 输入会话接口（有状态运行时对象）。
 * 每次 Schema 变化，都必须关闭旧 session 并创建新 session。
 */
interface InputSession {
    val capabilityId: CapabilityId
    val state: kotlinx.coroutines.flow.StateFlow<InputSessionState>
    
    suspend fun handle(event: InputEvent): EngineResult
    suspend fun reset(reason: ResetReason)
    suspend fun close()
}