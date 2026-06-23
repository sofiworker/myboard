package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 布局测量器真实实现。
 * 阶段 04：替换 stub。
 */
class LayoutMeasurerImpl : LayoutMeasurer {
    
    private val cache = mutableMapOf<String, MeasuredLayout>()
    
    override fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int): MeasuredLayout {
        val cacheKey = "${doc.id}:${layer}:${w}:${h}"
        
        return cache.getOrPut(cacheKey) {
            LayoutEngine.measure(doc, layer, w, h)
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