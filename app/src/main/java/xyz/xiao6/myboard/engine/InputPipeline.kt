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
 * 输入管线。
 * 输入引擎层和状态层之间的唯一协调器。
 * 阶段 05 实现真实逻辑，阶段 01-04 使用 stub。
 */
interface InputPipeline {
    suspend fun handle(action: InputAction)
    suspend fun onContextChanged(context: KeyboardContext)
    suspend fun reset(reason: ResetReason)
}