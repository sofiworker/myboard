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
 * 布局提示解析器。
 * 根据 EditorInfo.inputType 提示返回布局 ID。
 * 阶段 01 使用 StubLayoutHintResolver，阶段 04 替换真实实现。
 */
interface LayoutHintResolver {
    fun resolve(hint: LayoutHint, currentLayoutId: String): String
}