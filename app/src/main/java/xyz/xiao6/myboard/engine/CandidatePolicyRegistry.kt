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
 * 候选策略注册表。
 * 阶段 01 使用 StubCandidatePolicyRegistry，阶段 05 替换真实实现。
 */
interface CandidatePolicyRegistry {
    fun register(policy: CandidatePolicy)
    fun get(policyId: String): CandidatePolicy?
}