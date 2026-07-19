package xyz.xiao6.myboard.contract.engine

import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.KeyboardContext
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.input.InputSessionState
import xyz.xiao6.myboard.contract.input.InputEvent
import xyz.xiao6.myboard.contract.input.EngineResult
import xyz.xiao6.myboard.contract.input.ResetReason

data class EngineBinding(
    val engineId: String,
    val encoderId: String? = null,
    val encoderConfig: ResourceRef? = null
) {
    init {
        require(engineId.isNotBlank()) { "Engine id must not be blank" }
    }
}

enum class DictionaryKind {
    WORD,
    PHRASE,
    CONVERSION,
    FREQUENCY,
    SPELLING,
    EMOJI
}

enum class DictionaryRole {
    PRIMARY,
    CONVERSION,
    FREQUENCY,
    SPELLING,
    EMOJI
}

data class DictionaryBinding(
    val kind: DictionaryKind,
    val role: DictionaryRole,
    val resource: ResourceRef,
    val required: Boolean
) {
    fun isCompatible(): Boolean = role.isCompatibleWith(kind)
}

fun DictionaryRole.isCompatibleWith(kind: DictionaryKind): Boolean = when (kind) {
    DictionaryKind.WORD, DictionaryKind.PHRASE -> this == DictionaryRole.PRIMARY
    DictionaryKind.CONVERSION -> this == DictionaryRole.CONVERSION
    DictionaryKind.FREQUENCY -> this == DictionaryRole.FREQUENCY
    DictionaryKind.SPELLING -> this == DictionaryRole.SPELLING
    DictionaryKind.EMOJI -> this == DictionaryRole.EMOJI
}

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
    val capability: LanguageCapability,
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
    val displayPolicy: DisplayPolicy,
    val resolvedResources: Map<xyz.xiao6.myboard.contract.registry.ResourceRef, ResolvedResourceKey> = emptyMap()
)

data class ResolvedCapabilityKey(
    val capabilityId: CapabilityId,
    val resources: Map<xyz.xiao6.myboard.contract.registry.ResourceRef, ResolvedResourceKey>
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
    val capabilityKey: ResolvedCapabilityKey
    val state: kotlinx.coroutines.flow.StateFlow<InputSessionState>
    
    suspend fun handle(event: InputEvent): EngineResult
    suspend fun reset(reason: ResetReason)
    suspend fun close()
}
