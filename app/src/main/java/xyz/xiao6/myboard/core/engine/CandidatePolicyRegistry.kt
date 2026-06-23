package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 候选策略注册表。
 * 阶段 01 使用 StubCandidatePolicyRegistry，阶段 05 替换真实实现。
 */
interface CandidatePolicyRegistry {
    fun register(policy: CandidatePolicy)
    fun get(policyId: String): CandidatePolicy?
}