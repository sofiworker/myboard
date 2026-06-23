package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 显示策略注册表。
 * 阶段 01 使用 StubDisplayPolicyRegistry，阶段 05 替换真实实现。
 */
interface DisplayPolicyRegistry {
    fun register(policy: DisplayPolicy)
    fun get(policyId: String): DisplayPolicy?
}