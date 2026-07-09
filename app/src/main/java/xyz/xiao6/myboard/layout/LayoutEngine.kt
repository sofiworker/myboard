package xyz.xiao6.myboard.layout

import android.graphics.Path
import android.graphics.RectF
import xyz.xiao6.myboard.contract.layout.AbsoluteLayout
import xyz.xiao6.myboard.contract.layout.CompositeLayout
import xyz.xiao6.myboard.contract.layout.Dimension
import xyz.xiao6.myboard.contract.layout.GridLayout
import xyz.xiao6.myboard.contract.layout.HintPosition
import xyz.xiao6.myboard.contract.layout.HitShape
import xyz.xiao6.myboard.contract.layout.KeyDef
import xyz.xiao6.myboard.contract.layout.LayoutContainer
import xyz.xiao6.myboard.contract.layout.LayoutDoc
import xyz.xiao6.myboard.contract.layout.LayoutNode
import xyz.xiao6.myboard.contract.layout.LinearLayout
import xyz.xiao6.myboard.contract.layout.MeasuredKey
import xyz.xiao6.myboard.contract.layout.MeasuredLayout
import xyz.xiao6.myboard.contract.layout.MeasuredRegion
import xyz.xiao6.myboard.contract.layout.Orientation
import xyz.xiao6.myboard.contract.layout.Region
import xyz.xiao6.myboard.contract.layout.RowLayout
import xyz.xiao6.myboard.contract.state.LayoutLayer

/**
 * Measures a layout document into a flat key list plus optional region metadata.
 *
 * Inline toolbar/candidate chrome is owned by the page frame. Composite regions
 * are for the current layout's own internal areas, such as rails and content.
 */
object LayoutEngine {

    private const val DEFAULT_KEY_HEIGHT_DP = 46f

    fun measure(
        doc: LayoutDoc,
        layer: LayoutLayer,
        viewWidthPx: Int,
        viewHeightPx: Int,
        density: Float = 2.0f
    ): MeasuredLayout {
        if (doc.root is CompositeLayout) {
            val regions = measureComposite(
                composite = doc.root,
                area = RectF(0f, 0f, viewWidthPx.toFloat(), viewHeightPx.toFloat()),
                density = density
            )
            return MeasuredLayout(
                doc = doc,
                keys = regions.flatMap { it.keys },
                viewWidth = viewWidthPx,
                viewHeight = viewHeightPx,
                layer = layer,
                regions = regions
            )
        }

        return MeasuredLayout(
            doc = doc,
            keys = measureContainer(doc.root, viewWidthPx, viewHeightPx, density),
            viewWidth = viewWidthPx,
            viewHeight = viewHeightPx,
            layer = layer
        )
    }

    private fun measureContainer(
        container: LayoutContainer,
        viewWidthPx: Int,
        viewHeightPx: Int,
        density: Float
    ): List<MeasuredKey> {
        return measureContainer(container, viewWidthPx, viewHeightPx, density, originX = 0f, originY = 0f)
    }

    private fun measureContainer(
        container: LayoutContainer,
        viewWidthPx: Int,
        viewHeightPx: Int,
        density: Float,
        originX: Float,
        originY: Float
    ): List<MeasuredKey> {
        val padding = container.padding
        val containerW = resolveDimension(container.width, viewWidthPx.toFloat(), density, 1f)
        val containerH = resolveDimension(container.height, viewHeightPx.toFloat(), density, 1f)

        val innerW = containerW - (padding.start + padding.end) * density
        val innerH = containerH - (padding.top + padding.bottom) * density
        val offsetX = originX + padding.start * density
        val offsetY = originY + padding.top * density

        return when (container) {
            is RowLayout -> measureRow(container, offsetX, offsetY, innerW, innerH, density)
            is GridLayout -> measureGrid(container, offsetX, offsetY, innerW, innerH, density)
            is LinearLayout -> measureLinear(container, offsetX, offsetY, innerW, innerH, density)
            is AbsoluteLayout -> measureAbsolute(container, offsetX, offsetY, innerW, innerH, density)
            is CompositeLayout -> measureComposite(
                composite = container,
                area = RectF(originX, originY, originX + containerW, originY + containerH),
                density = density
            ).flatMap { it.keys }
        }
    }

    private fun measureComposite(
        composite: CompositeLayout,
        area: RectF,
        density: Float
    ): List<MeasuredRegion> {
        if (composite.regions.isEmpty()) return emptyList()

        val padding = composite.padding
        val gapPx = composite.gap * density
        val contentLeft = area.left + padding.start * density
        val contentTop = area.top + padding.top * density
        val contentRight = area.right - padding.end * density
        val contentBottom = area.bottom - padding.bottom * density
        val contentWidth = (contentRight - contentLeft).coerceAtLeast(0f)
        val contentHeight = (contentBottom - contentTop).coerceAtLeast(0f)
        val horizontal = composite.orientation == Orientation.HORIZONTAL
        val mainSize = if (horizontal) contentWidth else contentHeight

        var fixedSize = 0f
        var totalWeight = 0f
        val mainDimensions = composite.regions.map { region ->
            if (horizontal) region.container.width else region.container.height
        }

        mainDimensions.forEach { dimension ->
            when (dimension) {
                is Dimension.Weight -> totalWeight += dimension.value
                is Dimension.Match -> totalWeight += 1f
                is Dimension.Dp -> fixedSize += dimension.value * density
                is Dimension.Percent -> fixedSize += dimension.value / 100f * mainSize
                is Dimension.RatioW -> fixedSize += dimension.value * mainSize
                is Dimension.Wrap -> fixedSize += DEFAULT_KEY_HEIGHT_DP * density
            }
        }

        val totalGap = gapPx * (composite.regions.size - 1).coerceAtLeast(0)
        val remaining = (mainSize - fixedSize - totalGap).coerceAtLeast(0f)
        val weightUnit = if (totalWeight > 0f) remaining / totalWeight else 0f
        var cursor = if (horizontal) contentLeft else contentTop

        return composite.regions.mapIndexed { index, region ->
            val dimension = mainDimensions[index]
            val regionMainSize = when (dimension) {
                is Dimension.Weight -> dimension.value * weightUnit
                is Dimension.Match -> weightUnit
                is Dimension.Dp -> dimension.value * density
                is Dimension.Percent -> dimension.value / 100f * mainSize
                is Dimension.RatioW -> dimension.value * mainSize
                is Dimension.Wrap -> DEFAULT_KEY_HEIGHT_DP * density
            }.coerceAtLeast(0f)

            val rect = if (horizontal) {
                RectF(cursor, contentTop, cursor + regionMainSize, contentBottom)
            } else {
                RectF(contentLeft, cursor, contentRight, cursor + regionMainSize)
            }
            cursor += regionMainSize + gapPx

            MeasuredRegion(
                region = region,
                rect = rect,
                keys = measureContainerInBounds(region.container, rect, density)
            )
        }
    }

    private fun measureContainerInBounds(
        container: LayoutContainer,
        bounds: RectF,
        density: Float
    ): List<MeasuredKey> {
        val padding = container.padding
        val innerW = bounds.width() - (padding.start + padding.end) * density
        val innerH = bounds.height() - (padding.top + padding.bottom) * density
        val offsetX = bounds.left + padding.start * density
        val offsetY = bounds.top + padding.top * density

        return when (container) {
            is RowLayout -> measureRow(container, offsetX, offsetY, innerW, innerH, density)
            is GridLayout -> measureGrid(container, offsetX, offsetY, innerW, innerH, density)
            is LinearLayout -> measureLinear(container, offsetX, offsetY, innerW, innerH, density)
            is AbsoluteLayout -> measureAbsolute(container, offsetX, offsetY, innerW, innerH, density)
            is CompositeLayout -> measureComposite(container, bounds, density).flatMap { it.keys }
        }
    }

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

        var totalWeight = 0f
        var fixedWidth = 0f
        for (key in keys) {
            when (val width = key.width) {
                is Dimension.Weight -> totalWeight += width.value
                is Dimension.Dp -> fixedWidth += width.value * density
                is Dimension.Wrap -> fixedWidth += DEFAULT_KEY_HEIGHT_DP * density
                is Dimension.Match -> totalWeight += 1f
                is Dimension.Percent -> fixedWidth += width.value / 100f * innerW
                is Dimension.RatioW -> fixedWidth += width.value * innerH
            }
        }

        val remainingW = innerW - fixedWidth - totalGap
        val weightUnit = if (totalWeight > 0) remainingW / totalWeight else 0f
        var x = offsetX

        return keys.map { key ->
            val keyW = resolveDimension(key.width, innerW, density, weightUnit)
            val keyH = resolveDimension(key.height, innerH, density, weightUnit)
            val rect = RectF(x, offsetY, x + keyW, offsetY + keyH)
            x += keyW + gap

            MeasuredKey(
                key = key,
                resolvedContent = key.content,
                rect = rect,
                hitPath = createHitPath(key, rect),
                zIndex = 0
            )
        }
    }

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

        val columns = grid.columns.toFloat()
        val rows = if (grid.rows > 0) {
            grid.rows
        } else {
            cells.maxOfOrNull { it.row + it.rowSpan } ?: 1f
        }

        val colGap = grid.colGap * density
        val rowGap = grid.rowGap * density
        val cellW = (innerW - colGap * (columns - 1f)) / columns
        val cellH = (innerH - rowGap * (rows - 1f)) / rows

        return cells.map { cell ->
            val x = offsetX + cell.col * (cellW + colGap)
            val y = offsetY + cell.row * (cellH + rowGap)
            val w = cell.colSpan * cellW + (cell.colSpan - 1f) * colGap
            val h = cell.rowSpan * cellH + (cell.rowSpan - 1f) * rowGap
            val rect = RectF(x, y, x + w, y + h)

            MeasuredKey(
                key = cell.key,
                resolvedContent = cell.key.content,
                rect = rect,
                hitPath = createHitPath(cell.key, rect),
                zIndex = 0
            )
        }
    }

    private fun measureLinear(
        linear: LinearLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        if (linear.children.isEmpty()) return emptyList()

        val gap = linear.gap * density
        val isHorizontal = linear.orientation == Orientation.HORIZONTAL
        var pos = if (isHorizontal) offsetX else offsetY
        val measuredKeys = mutableListOf<MeasuredKey>()

        for (child in linear.children) {
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

                    measuredKeys.add(
                        MeasuredKey(
                            key = key,
                            resolvedContent = key.content,
                            rect = rect,
                            hitPath = createHitPath(key, rect),
                            zIndex = 0
                        )
                    )
                    pos += (if (isHorizontal) keyW else keyH) + gap
                }

                is LayoutNode.SpacerNode -> {
                    val spacerSize = if (isHorizontal) {
                        resolveDimension(child.width, innerW, density, 1f)
                    } else {
                        resolveDimension(child.height, innerH, density, 1f)
                    }
                    pos += spacerSize + gap
                }

                is LayoutNode.DividerNode -> {
                    val dividerSize = if (isHorizontal) {
                        resolveDimension(child.width, innerW, density, 1f)
                    } else {
                        resolveDimension(child.height, innerH, density, 1f)
                    }
                    pos += dividerSize + gap
                }
            }
        }

        return measuredKeys
    }

    private fun measureAbsolute(
        absolute: AbsoluteLayout,
        offsetX: Float,
        offsetY: Float,
        innerW: Float,
        innerH: Float,
        density: Float
    ): List<MeasuredKey> {
        if (absolute.items.isEmpty()) return emptyList()

        return absolute.items.map { item ->
            val x = resolveDimension(item.x, innerW, density, 1f)
            val y = resolveDimension(item.y, innerH, density, 1f)
            val w = resolveDimension(item.width, innerW, density, 1f)
            val h = resolveDimension(item.height, innerH, density, 1f)

            val adjustedX = when (item.anchor) {
                HintPosition.TOP_LEFT,
                HintPosition.CENTER_LEFT,
                HintPosition.BOTTOM_LEFT -> offsetX + x
                HintPosition.TOP_CENTER,
                HintPosition.CENTER,
                HintPosition.BOTTOM_CENTER -> offsetX + x - w / 2
                HintPosition.TOP_RIGHT,
                HintPosition.CENTER_RIGHT,
                HintPosition.BOTTOM_RIGHT -> offsetX + x - w
            }
            val adjustedY = when (item.anchor) {
                HintPosition.TOP_LEFT,
                HintPosition.TOP_CENTER,
                HintPosition.TOP_RIGHT -> offsetY + y
                HintPosition.CENTER_LEFT,
                HintPosition.CENTER,
                HintPosition.CENTER_RIGHT -> offsetY + y - h / 2
                HintPosition.BOTTOM_LEFT,
                HintPosition.BOTTOM_CENTER,
                HintPosition.BOTTOM_RIGHT -> offsetY + y - h
            }

            val rect = RectF(adjustedX, adjustedY, adjustedX + w, adjustedY + h)
            MeasuredKey(
                key = item.key,
                resolvedContent = item.key.content,
                rect = rect,
                hitPath = createHitPath(item.key, rect),
                zIndex = item.zIndex
            )
        }.sortedBy { it.zIndex }
    }

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

    private fun createHitPath(key: KeyDef, rect: RectF): Path {
        return when (val shape = key.hitShape) {
            is HitShape.Rect -> Path().apply {
                addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.CW)
            }

            is HitShape.Circle -> Path().apply {
                addCircle(shape.cx, shape.cy, shape.r, Path.Direction.CW)
            }

            is HitShape.Rounded -> Path().apply {
                addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.CW)
            }

            null -> Path().apply {
                addRect(rect, Path.Direction.CW)
            }
        }
    }
}
