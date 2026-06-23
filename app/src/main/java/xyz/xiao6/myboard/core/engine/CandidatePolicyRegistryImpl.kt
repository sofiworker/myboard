package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

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
