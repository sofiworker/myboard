package xyz.xiao6.myboard.core.layout

/**
 * 几何计算器：基于权重的按键布局。
 */
class GridCalculator {
    fun calculate(
        layout: KeyboardLayout,
        containerWidth: Float,
        containerHeight: Float
    ): Map<String, KeyGeometry> {
        val arrangement = layout.arrangements[layout.arrangements.keys.firstOrNull()] ?: return emptyMap()
        val rows = arrangement.rows.mapNotNull { rowId ->
            layout.rows.find { it.id == rowId }
        }

        val hGap = layout.geometry.gap.h
        val vGap = layout.geometry.gap.v
        val padding = layout.geometry.padding.all

        val availableWidth = containerWidth - padding * 2
        val availableHeight = containerHeight - padding * 2
        val rowHeight = availableHeight / rows.size

        val result = mutableMapOf<String, KeyGeometry>()

        for ((rowIndex, row) in rows.withIndex()) {
            val keyIds = row.keys.filter { !it.startsWith("_") }
            val totalWeight = keyIds.sumOf { layout.keys[it]?.weight?.toDouble() ?: 1.0 }.toFloat()

            var xOffset = padding

            for (keyId in keyIds) {
                val key = layout.keys[keyId] ?: continue
                val weight = key.weight
                val keyWidth = (availableWidth - hGap * (keyIds.size - 1)) * (weight / totalWeight)

                val leftPx = xOffset
                val topPx = padding + rowIndex * (rowHeight + vGap)

                result[keyId] = KeyGeometry(
                    keyId = keyId,
                    leftPx = leftPx,
                    topPx = topPx,
                    widthPx = keyWidth,
                    heightPx = rowHeight
                )

                xOffset += keyWidth + hGap
            }
        }

        return result
    }
}

data class KeyGeometry(
    val keyId: String,
    val leftPx: Float,
    val topPx: Float,
    val widthPx: Float,
    val heightPx: Float
)
