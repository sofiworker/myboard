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
 * 布局测量器。
 * 阶段 01 使用 StubLayoutMeasurer，阶段 04 替换真实实现。
 */
interface LayoutMeasurer {
    fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int, density: Float = 2.0f): MeasuredLayout
    fun invalidate(layoutId: String? = null)
}