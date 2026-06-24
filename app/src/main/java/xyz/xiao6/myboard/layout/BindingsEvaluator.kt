package xyz.xiao6.myboard.layout

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
 * Bindings 评估器。
 * 评估 region 的 visible 和 enabled。
 * 阶段 01 使用 StubBindingsEvaluator，阶段 04 替换真实实现。
 */
interface BindingsEvaluator {
    fun evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean>
}