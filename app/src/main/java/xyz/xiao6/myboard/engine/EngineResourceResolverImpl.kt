package xyz.xiao6.myboard.engine

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
 * 引擎资源解析器真实实现。
 */
class EngineResourceResolverImpl(
    private val encoderRegistry: EncoderRegistry,
    private val dictionaryRegistry: DictionaryRegistry,
    private val candidatePolicyRegistry: CandidatePolicyRegistry,
    private val displayPolicyRegistry: DisplayPolicyRegistry
) : EngineResourceResolver {
    
    override fun resolve(capability: SchemaCapability, packageId: String): EngineResources {
        val encoder = capability.encoderId?.let { encoderRegistry.get(it) }
        
        // DictionaryKey 需要 locale/script/schema/path
        // 由于 resolve 只接收 SchemaCapability，暂时使用占位值
        val dictionary = capability.dictionary?.let { dictPath ->
            dictionaryRegistry.load(DictionaryKey(
                packageId = packageId,
                locale = LocaleTag(""),
                script = Script.LATN,
                schema = Schema(""),
                path = dictPath
            ))
        }
        
        val candidatePolicy = candidatePolicyRegistry.get(capability.candidatePolicy)
            ?: object : CandidatePolicy {
                override val policyId: String = "fallback"
                override fun sort(candidates: List<Candidate>): List<Candidate> = candidates
                override fun onSpace(state: InputSessionState): PolicyAction = PolicyAction.Noop
                override fun onEnter(state: InputSessionState): PolicyAction = PolicyAction.Noop
                override fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction = PolicyAction.Noop
            }
        
        val displayPolicy = capability.displayPolicy?.let { displayPolicyRegistry.get(it) }
            ?: object : DisplayPolicy {
                override val policyId: String = "fallback"
                override fun display(state: InputSessionState): String = state.rawBuffer
            }
        
        val mapping = capability.mapping?.let { KeyMapping(id = it, layers = emptyMap()) }
        val fsm = capability.fsm?.let { TransliterationFsm(id = it, startState = "", states = emptyMap()) }
        
        return EngineResources(
            mapping = mapping,
            encoder = encoder,
            fsm = fsm,
            dictionary = dictionary,
            candidatePolicy = candidatePolicy,
            displayPolicy = displayPolicy
        )
    }
}
