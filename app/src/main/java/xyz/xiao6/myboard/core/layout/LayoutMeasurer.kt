package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 布局测量器。
 * 阶段 01 使用 StubLayoutMeasurer，阶段 04 替换真实实现。
 */
interface LayoutMeasurer {
    fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int): MeasuredLayout
    fun invalidate(layoutId: String? = null)
}