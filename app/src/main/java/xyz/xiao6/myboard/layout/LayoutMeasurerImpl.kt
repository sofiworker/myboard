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
 * 布局测量器真实实现。
 * 阶段 04：替换 stub。
 */
class LayoutMeasurerImpl : LayoutMeasurer {
    
    private val cache = mutableMapOf<String, MeasuredLayout>()
    
    override fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int, density: Float): MeasuredLayout {
        val cacheKey = "${doc.id}:${layer}:${w}:${h}:${density}"
        
        return cache.getOrPut(cacheKey) {
            LayoutEngine.measure(doc, layer, w, h, density)
        }
    }
    
    override fun invalidate(layoutId: String?) {
        if (layoutId == null) {
            cache.clear()
        } else {
            cache.keys.removeAll { it.startsWith("$layoutId:") }
        }
    }
}