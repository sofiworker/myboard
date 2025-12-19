package xyz.xiao6.myboard.model

fun KeyboardLayout.validate(): List<String> {
    val errors = mutableListOf<String>()

    if (layoutId.isBlank()) errors += "layoutId must not be blank"
    if (totalWidthRatio <= 0f || totalWidthRatio > 1f) errors += "totalWidthRatio must be in (0, 1]"
    if (totalHeightRatio <= 0f || totalHeightRatio > 1f) errors += "totalHeightRatio must be in (0, 1]"
    if (rows.isEmpty()) errors += "rows must not be empty"

    val allKeys = rows.flatMap { it.keys }
    val dup = allKeys.groupBy { it.keyId }.filterValues { it.size > 1 }.keys
    if (dup.isNotEmpty()) errors += "keyId must be unique; duplicates=$dup"

    rows.forEachIndexed { rowIndex, row ->
        if (row.rowId.isBlank()) errors += "rows[$rowIndex].rowId must not be blank"
        if (row.keys.isEmpty()) errors += "rows[$rowIndex].keys must not be empty"
        row.keys.forEachIndexed { keyIndex, key ->
            if (key.keyId.isBlank()) errors += "rows[$rowIndex].keys[$keyIndex].keyId must not be blank"
            if (key.ui.styleId.isBlank()) errors += "rows[$rowIndex].keys[$keyIndex].ui.styleId must not be blank"
            if (key.ui.gridPosition.startCol < 0) errors += "rows[$rowIndex].keys[$keyIndex].ui.gridPosition.startCol must be >= 0"
            if (key.ui.gridPosition.startRow < 0) errors += "rows[$rowIndex].keys[$keyIndex].ui.gridPosition.startRow must be >= 0"
            if (key.ui.gridPosition.spanCols <= 0) errors += "rows[$rowIndex].keys[$keyIndex].ui.gridPosition.spanCols must be >= 1"
            if (key.ui.widthWeight <= 0f) errors += "rows[$rowIndex].keys[$keyIndex].ui.widthWeight must be > 0"
        }
    }

    return errors
}
