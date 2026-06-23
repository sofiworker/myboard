package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 输入管线。
 * 输入引擎层和状态层之间的唯一协调器。
 * 阶段 05 实现真实逻辑，阶段 01-04 使用 stub。
 */
interface InputPipeline {
    suspend fun handle(action: InputAction)
    suspend fun onContextChanged(context: KeyboardContext)
    suspend fun reset(reason: ResetReason)
}