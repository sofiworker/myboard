package xyz.xiao6.myboard.controller

/**
 * 布局状态（不改变几何）：仅影响 Key 的内容/样式/可见性。
 * Layout state (no geometry change): only affects key content/style/visibility.
 */
data class LayoutState(
    val shift: ShiftState = ShiftState.OFF,
    val layer: Layer = Layer.BASE,
    val hiddenKeyIds: Set<String> = emptySet(),
    val highlightedKeyIds: Set<String> = emptySet(),
)

/**
 * Shift 状态。
 * Shift state.
 */
enum class ShiftState {
    OFF,
    ON,
    CAPS_LOCK,
}

/**
 * 同一布局内的“页/层”切换：要求几何不变（仅内容变化）。
 * In-layout layer/page switching: geometry must remain unchanged (content changes only).
 */
enum class Layer {
    BASE,
    SYMBOLS_1,
    SYMBOLS_2,
}

