package xyz.xiao6.myboard.model

fun KeyboardLayout.validate(): List<String> {
    val errors = mutableListOf<String>()

    if (layoutId.isBlank()) errors += "layoutId must not be blank"
    if (totalWidthRatio <= 0f || totalWidthRatio > 1f) {
        errors += "totalWidthRatio must be in (0, 1]"
    }
    if (totalHeightRatio <= 0f || totalHeightRatio > 1f) {
        errors += "totalHeightRatio must be in (0, 1]"
    }
    if (locale.any { it.isBlank() }) errors += "locale must not contain blank items"
    if (locale.distinct().size != locale.size) errors += "locale must not contain duplicates"
    if (defaults.horizontalGapDp < 0f) errors += "defaults.horizontalGapDp must be >= 0"
    if (defaults.verticalGapDp < 0f) errors += "defaults.verticalGapDp must be >= 0"
    if (defaults.padding.topDp < 0f) errors += "defaults.padding.topDp must be >= 0"
    if (defaults.padding.bottomDp < 0f) errors += "defaults.padding.bottomDp must be >= 0"
    if (defaults.padding.leftDp < 0f) errors += "defaults.padding.leftDp must be >= 0"
    if (defaults.padding.rightDp < 0f) errors += "defaults.padding.rightDp must be >= 0"
    if (rows.isEmpty()) errors += "rows must not be empty"

    rows.forEachIndexed { rowIndex, row ->
        if (row.rowId.isBlank()) errors += "rows[$rowIndex].rowId must not be blank"
        if (row.widthRatio <= 0f || row.widthRatio > 1f) {
            errors += "rows[$rowIndex].widthRatio must be in (0, 1]"
        }
        if (row.heightRatio <= 0f || row.heightRatio > 1f) {
            errors += "rows[$rowIndex].heightRatio must be in (0, 1]"
        }
        if (row.horizontalGapDp != null && row.horizontalGapDp < 0f) {
            errors += "rows[$rowIndex].horizontalGapDp must be >= 0"
        }
        if (row.startPaddingDp != null && row.startPaddingDp < 0f) {
            errors += "rows[$rowIndex].startPaddingDp must be >= 0"
        }
        if (row.endPaddingDp != null && row.endPaddingDp < 0f) {
            errors += "rows[$rowIndex].endPaddingDp must be >= 0"
        }
        if (row.keys.isEmpty()) errors += "rows[$rowIndex].keys must not be empty"

        row.keys.forEachIndexed { keyIndex, key ->
            if (key.keyId.isBlank()) errors += "rows[$rowIndex].keys[$keyIndex].keyId must not be blank"
            if (key.styleId.isBlank()) errors += "rows[$rowIndex].keys[$keyIndex].styleId must not be blank"
            if (key.label.isBlank()) errors += "rows[$rowIndex].keys[$keyIndex].label must not be blank"
            if (key.widthWeight <= 0f) errors += "rows[$rowIndex].keys[$keyIndex].widthWeight must be > 0"
            if (key.widthDp != null && key.widthDp <= 0f) {
                errors += "rows[$rowIndex].keys[$keyIndex].widthDp must be > 0"
            }

            val pos = key.gridPosition
            if (pos.startCol < 0) errors += "rows[$rowIndex].keys[$keyIndex].gridPosition.startCol must be >= 0"
            if (pos.startRow < 0) errors += "rows[$rowIndex].keys[$keyIndex].gridPosition.startRow must be >= 0"
            if (pos.spanCols <= 0) errors += "rows[$rowIndex].keys[$keyIndex].gridPosition.spanCols must be >= 1"
            if (pos.spanRows <= 0) errors += "rows[$rowIndex].keys[$keyIndex].gridPosition.spanRows must be >= 1"
        }
    }

    val allKeys = rows.flatMap { it.keys }
    val duplicateKeyIds = allKeys.groupBy { it.keyId }.filterValues { it.size > 1 }.keys
    if (duplicateKeyIds.isNotEmpty()) errors += "keyId must be unique; duplicates=$duplicateKeyIds"

    val backLayoutKeys = allKeys.filter { key ->
        key.behaviors.values.any { it.actionType == ActionType.BACK_LAYOUT }
    }
    if (backLayoutKeys.size > 1) errors += "Only one BACK_LAYOUT key is allowed per layout"
    backLayoutKeys.firstOrNull()?.let { key ->
        if (key.keyId != KeyIds.BACK_LAYOUT) {
            errors += "BACK_LAYOUT action must use keyId='${KeyIds.BACK_LAYOUT}'"
        }
        val action = key.behaviors[KeyTrigger.TAP]
        if (action?.actionType != ActionType.BACK_LAYOUT) {
            errors += "BACK_LAYOUT key must bind TAP -> BACK_LAYOUT"
        } else {
            if (action.value != null) errors += "BACK_LAYOUT action must not set value"
            if (!action.values.isNullOrEmpty()) errors += "BACK_LAYOUT action must not set values"
        }
    }

    allKeys.firstOrNull { it.keyId == KeyIds.BACK_LAYOUT }?.let { backKey ->
        if (!backKey.behaviors.containsKey(KeyTrigger.TAP)) {
            errors += "keyId='${KeyIds.BACK_LAYOUT}' must define behaviors.TAP"
        }

        val nonBackTriggers = backKey.behaviors.filterValues { it.actionType != ActionType.BACK_LAYOUT }.keys
        if (nonBackTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.BACK_LAYOUT}' behaviors must only use BACK_LAYOUT; invalidTriggers=$nonBackTriggers"
        }

        val invalidParamTriggers = backKey.behaviors.filterValues { it.value != null || !it.values.isNullOrEmpty() }.keys
        if (invalidParamTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.BACK_LAYOUT}' BACK_LAYOUT must not set value/values; invalidTriggers=$invalidParamTriggers"
        }
    }

    val backspaceKeys = allKeys.filter { key ->
        key.behaviors.values.any { it.actionType == ActionType.BACKSPACE }
    }
    if (backspaceKeys.size > 1) errors += "Only one BACKSPACE key is allowed per layout"
    backspaceKeys.firstOrNull()?.let { key ->
        if (key.keyId != KeyIds.BACKSPACE) {
            errors += "BACKSPACE action must use keyId='${KeyIds.BACKSPACE}'"
        }
        val action = key.behaviors[KeyTrigger.TAP]
        if (action?.actionType != ActionType.BACKSPACE) {
            errors += "BACKSPACE key must bind TAP -> BACKSPACE"
        } else {
            if (action.value != null) errors += "BACKSPACE action must not set value"
            if (!action.values.isNullOrEmpty()) errors += "BACKSPACE action must not set values"
        }
    }

    allKeys.firstOrNull { it.keyId == KeyIds.BACKSPACE }?.let { backspaceKey ->
        if (!backspaceKey.behaviors.containsKey(KeyTrigger.TAP)) {
            errors += "keyId='${KeyIds.BACKSPACE}' must define behaviors.TAP"
        }

        val nonBackspaceTriggers = backspaceKey.behaviors.filterValues { it.actionType != ActionType.BACKSPACE }.keys
        if (nonBackspaceTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.BACKSPACE}' behaviors must only use BACKSPACE; invalidTriggers=$nonBackspaceTriggers"
        }

        val invalidParamTriggers =
            backspaceKey.behaviors.filterValues { it.value != null || !it.values.isNullOrEmpty() }.keys
        if (invalidParamTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.BACKSPACE}' BACKSPACE must not set value/values; invalidTriggers=$invalidParamTriggers"
        }
    }

    val spaceKeys = allKeys.filter { key ->
        key.behaviors.values.any { it.actionType == ActionType.SPACE }
    }
    if (spaceKeys.size > 1) errors += "Only one SPACE key is allowed per layout"
    spaceKeys.firstOrNull()?.let { key ->
        if (key.keyId != KeyIds.SPACE) {
            errors += "SPACE action must use keyId='${KeyIds.SPACE}'"
        }
        val action = key.behaviors[KeyTrigger.TAP]
        if (action?.actionType != ActionType.SPACE) {
            errors += "SPACE key must bind TAP -> SPACE"
        } else {
            if (action.value != null) errors += "SPACE action must not set value"
            if (!action.values.isNullOrEmpty()) errors += "SPACE action must not set values"
        }
    }

    allKeys.firstOrNull { it.keyId == KeyIds.SPACE }?.let { spaceKey ->
        if (!spaceKey.behaviors.containsKey(KeyTrigger.TAP)) {
            errors += "keyId='${KeyIds.SPACE}' must define behaviors.TAP"
        }

        val nonSpaceTriggers = spaceKey.behaviors.filterValues { it.actionType != ActionType.SPACE }.keys
        if (nonSpaceTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.SPACE}' behaviors must only use SPACE; invalidTriggers=$nonSpaceTriggers"
        }

        val invalidParamTriggers =
            spaceKey.behaviors.filterValues { it.value != null || !it.values.isNullOrEmpty() }.keys
        if (invalidParamTriggers.isNotEmpty()) {
            errors += "keyId='${KeyIds.SPACE}' SPACE must not set value/values; invalidTriggers=$invalidParamTriggers"
        }
    }

    return errors
}
