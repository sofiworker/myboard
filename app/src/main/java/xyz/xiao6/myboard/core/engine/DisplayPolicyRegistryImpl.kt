package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 显示策略注册表真实实现。
 */
class DisplayPolicyRegistryImpl : DisplayPolicyRegistry {
    
    private val policies = mutableMapOf<String, DisplayPolicy>()
    
    override fun register(policy: DisplayPolicy) {
        policies[policy.policyId] = policy
    }
    
    override fun get(policyId: String): DisplayPolicy? {
        return policies[policyId]
    }
}
