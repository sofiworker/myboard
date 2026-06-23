package xyz.xiao6.myboard.core.layout

import android.graphics.Path
import android.graphics.RectF
import xyz.xiao6.myboard.core.contract.*

/**
 * 布局测量引擎。
 * 对 5 种容器类型（Row/Grid/Linear/Absolute/Composite）进行测量。
 * 输出 MeasuredLayout。
 */
object LayoutEngine {
    
    /** 默认 key dp 高度 */
    private const val DEFAULT_KEY_HEIGHT_DP = 46f
    
    /** 默认候选栏 dp 高度 */
    private const val DEFAULT_CANDIDATE_HEIGHT_DP = 40f
    
    /**
     * 测量布局文档，返回 MeasuredLayout。
     */
    fun measure(
        doc: LayoutDoc,
        layer: LayoutLayer,
        viewWidthPx: Int,
        viewHeightPx: Int,
        density: Float = 2.0f
    ): MeasuredLayout {
        val regions = mutableListOf<MeasuredRegion>()
        
        when (val root = doc.root) {
            is CompositeLayout -> {
                // 复合布局：测量每个 region
                for (region in root.regions) {
                    val measured = measureRegion(region, viewWidthPx, viewHeightPx, density)
                    regions.add(measured)
                }
            }
            else -> {
                // 单容器：包装为 region
                val region = Region(
                    id = "root",
                    role = RegionRole.KEYBOARD,
                    container = root
                )
                val measured = measureRegion(region, viewWidthPx, viewHeightPx, density)
                regions.add(measured)
            }
        }
        
        return MeasuredLayout(
            doc = doc,
            regions = regions,
            viewWidth = viewWidthPx,
            viewHeight = viewHeightPx,
            layer = layer
        )
    }
    
    private fun measureRegion(
        region: Region,
        viewWidthPx: Int,
        viewHeightPx: Int,
        density: Float
    ): MeasuredRegion {
        val container = region.container
        val padding = container.padding
        
        // 计算容器可用空间
        val containerW = resolveDimension(container.width, viewWidthPx.toFloat(), density, 1f)
        val containerH = resolveDimension(container.height, viewHeightPx.toFloat(), density, 1f)
        
        val innerW = containerW - (padding.start + padding.end) * density
        val innerH = containerH - (padding.top + padding.bottom) * density
        
        val offsetX = padding.start * density
        val offsetY = padding.top * density
        
        val keys = when (container) {
            is RowLayout -> measureRow(container, offsetX, offsetY, innerW, innerH, density)
            is GridLayout -> measureGrid(container, offsetX, offsetY, innerW, innerH, density)
            is LinearLayout -> measureLinear(container, offsetX, offsetY, innerW, innerH, density)
            is AbsoluteLayout -> measureAbsolute(container, offsetX, offsetY, innerW, innerH, density)
            is CompositeLayout -> emptyList()
        }
        
        return MeasuredRegion(
            region = region,
            rect = RectF(0f, 0f, containerW, containerH),
            keys = keys
        )
    }
    
    /**
     * Row 容器测量。
     * 水平排列 keys，使用 weight 分配宽度。
     */
    private fun measureRow(
        row: RowLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        val keys = row.keys
        if (keys.isEmpty()) return emptyList()
        
        val gap = row.gap * density
        val totalGap = gap * (keys.size - 1)
        
        // 计算 weight 总和
        var totalWeight = 0f
        var fixedWidth = 0f
        for (key in keys) {
            when (val w = key.width) {
                is Dimension.Weight -> totalWeight += w.value
                is Dimension.Dp -> fixedWidth += w.value * density
                is Dimension.Wrap -> fixedWidth += DEFAULT_KEY_HEIGHT_DP * density
                is Dimension.Match -> { /* 不处理，视为 weight=1 */ totalWeight += 1f }
                is Dimension.Percent -> fixedWidth += w.value / 100f * innerW
                is Dimension.RatioW -> fixedWidth += w.value * innerH
            }
        }
        
        val remainingW = innerW - fixedWidth - totalGap
        val weightUnit = if (totalWeight > 0) remainingW / totalWeight else 0f
        
        var x = offsetX
        val measuredKeys = mutableListOf<MeasuredKey>()
        
        for (key in keys) {
            val keyW = resolveDimension(key.width, innerW, density, weightUnit)
            val keyH = resolveDimension(key.height, innerH, density, weightUnit)
            
            val rect = RectF(x, offsetY, x + keyW, offsetY + keyH)
            val hitPath = createHitPath(key, rect)
            
            measuredKeys.add(
                MeasuredKey(
                    key = key,
                    resolvedContent = key.content,
                    rect = rect,
                    hitPath = hitPath,
                    zIndex = 0
                )
            )
            
            x += keyW + gap
        }
        
        return measuredKeys
    }
    
    /**
     * Grid 容器测量。
     * 网格布局，使用 cells 指定行列位置。
     */
    private fun measureGrid(
        grid: GridLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        val cells = grid.cells
        if (cells.isEmpty()) return emptyList()
        
        val columns = grid.columns
        val rows = if (grid.rows > 0) grid.rows else {
            cells.maxOfOrNull { it.row + it.rowSpan } ?: 1
        }
        
        val colGap = grid.colGap * density
        val rowGap = grid.rowGap * density
        
        val cellW = (innerW - colGap * (columns - 1)) / columns
        val cellH = (innerH - rowGap * (rows - 1)) / rows
        
        val measuredKeys = mutableListOf<MeasuredKey>()
        
        for (cell in cells) {
            val x = offsetX + cell.col * (cellW + colGap)
            val y = offsetY + cell.row * (cellH + rowGap)
            val w = cell.colSpan * cellW + (cell.colSpan - 1) * colGap
            val h = cell.rowSpan * cellH + (cell.rowSpan - 1) * rowGap
            
            val rect = RectF(x, y, x + w, y + h)
            val hitPath = createHitPath(cell.key, rect)
            
            measuredKeys.add(
                MeasuredKey(
                    key = cell.key,
                    resolvedContent = cell.key.content,
                    rect = rect,
                    hitPath = hitPath,
                    zIndex = 0
                )
            )
        }
        
        return measuredKeys
    }
    
    /**
     * Linear 容器测量（候选栏、工具栏）。
     */
    private fun measureLinear(
        linear: LinearLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        val children = linear.children
        if (children.isEmpty()) return emptyList()
        
        val gap = linear.gap * density
        val isHorizontal = linear.orientation == Orientation.HORIZONTAL
        
        var pos = if (isHorizontal) offsetX else offsetY
        val measuredKeys = mutableListOf<MeasuredKey>()
        
        for (child in children) {
            when (child) {
                is LayoutNode.KeyNode -> {
                    val key = child.key
                    val keyW = resolveDimension(key.width, innerW, density, 1f)
                    val keyH = resolveDimension(key.height, innerH, density, 1f)
                    
                    val rect = if (isHorizontal) {
                        RectF(pos, offsetY, pos + keyW, offsetY + keyH)
                    } else {
                        RectF(offsetX, pos, offsetX + keyW, pos + keyH)
                    }
                    
                    val hitPath = createHitPath(key, rect)
                    
                    measuredKeys.add(
                        MeasuredKey(
                            key = key,
                            resolvedContent = key.content,
                            rect = rect,
                            hitPath = hitPath,
                            zIndex = 0
                        )
                    )
                    
                    pos += (if (isHorizontal) keyW else keyH) + gap
                }
                is LayoutNode.SpacerNode -> {
                    val spacerSize = resolveDimension(child.width, innerW, density, 1f)
                    pos += spacerSize + gap
                }
            }
        }
        
        return measuredKeys
    }
    
    /**
     * Absolute 容器测量（浮层、特殊键）。
     */
    private fun measureAbsolute(
        absolute: AbsoluteLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        val items = absolute.items
        if (items.isEmpty()) return emptyList()
        
        val measuredKeys = mutableListOf<MeasuredKey>()
        
        for (item in items) {
            val x = resolveDimension(item.x, innerW, density, 1f)
            val y = resolveDimension(item.y, innerH, density, 1f)
            val w = resolveDimension(item.width, innerW, density, 1f)
            val h = resolveDimension(item.height, innerH, density, 1f)
            
            // 根据 anchor 调整位置
            val adjustedX = when (item.anchor) {
                HintPosition.TOP_LEFT, HintPosition.CENTER_LEFT, HintPosition.BOTTOM_LEFT -> offsetX + x
                HintPosition.TOP_CENTER, HintPosition.CENTER, HintPosition.BOTTOM_CENTER -> offsetX + x - w / 2
                HintPosition.TOP_RIGHT, HintPosition.CENTER_RIGHT, HintPosition.BOTTOM_RIGHT -> offsetX + x - w
            }
            val adjustedY = when (item.anchor) {
                HintPosition.TOP_LEFT, HintPosition.TOP_CENTER, HintPosition.TOP_RIGHT -> offsetY + y
                HintPosition.CENTER_LEFT, HintPosition.CENTER, HintPosition.CENTER_RIGHT -> offsetY + y - h / 2
                HintPosition.BOTTOM_LEFT, HintPosition.BOTTOM_CENTER, HintPosition.BOTTOM_RIGHT -> offsetY + y - h
            }
            
            val rect = RectF(adjustedX, adjustedY, adjustedX + w, adjustedY + h)
            val hitPath = createHitPath(item.key, rect)
            
            measuredKeys.add(
                MeasuredKey(
                    key = item.key,
                    resolvedContent = item.key.content,
                    rect = rect,
                    hitPath = hitPath,
                    zIndex = item.zIndex
                )
            )
        }
        
        return measuredKeys.sortedBy { it.zIndex }
    }
    
    /**
     * 解析 Dimension 为像素值。
     */
    private fun resolveDimension(
        dim: Dimension,
        parentSize: Float,
        density: Float,
        weightUnit: Float
    ): Float {
        return when (dim) {
            is Dimension.Match -> parentSize
            is Dimension.Wrap -> DEFAULT_KEY_HEIGHT_DP * density
            is Dimension.Weight -> dim.value * weightUnit
            is Dimension.Dp -> dim.value * density
            is Dimension.Percent -> dim.value / 100f * parentSize
            is Dimension.RatioW -> dim.value * parentSize
        }
    }
    
    /**
     * 创建命中区域 Path。
     */
    private fun createHitPath(key: KeyDef, rect: RectF): Path {
        return when (val shape = key.hitShape) {
            is HitShape.Rect -> {
                Path().apply {
                    addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.CW)
                }
            }
            is HitShape.Circle -> {
                Path().apply {
                    addCircle(shape.cx, shape.cy, shape.r, Path.Direction.CW)
                }
            }
            is HitShape.Rounded -> {
                Path().apply {
                    addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.CW)
                }
            }
            null -> {
                Path().apply {
                    addRect(rect, Path.Direction.CW)
                }
            }
        }
    }
}
