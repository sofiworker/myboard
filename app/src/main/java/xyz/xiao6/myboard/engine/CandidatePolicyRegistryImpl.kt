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
 * 候选策略注册表真实实现。
 */
class CandidatePolicyRegistryImpl : CandidatePolicyRegistry {
    
    private val policies = mutableMapOf<String, CandidatePolicy>()
    
    override fun register(policy: CandidatePolicy) {
        policies[policy.policyId] = policy
    }
    
    override fun get(policyId: String): CandidatePolicy? {
        return policies[policyId]
    }
}
