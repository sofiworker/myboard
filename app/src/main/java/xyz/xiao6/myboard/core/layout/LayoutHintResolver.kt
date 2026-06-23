package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 布局提示解析器。
 * 根据 EditorInfo.inputType 提示返回布局 ID。
 * 阶段 01 使用 StubLayoutHintResolver，阶段 04 替换真实实现。
 */
interface LayoutHintResolver {
    fun resolve(hint: LayoutHint, currentLayoutId: String): String
}