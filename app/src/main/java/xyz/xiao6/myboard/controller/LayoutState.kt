package xyz.xiao6.myboard.controller

/**
 * 布局状态（不改变几何）：仅影响 Key 的内容/样式/可见性。
 * Layout state (no geometry change): only affects key content/style/visibility.
 */
data class LayoutState(
    val shift: ShiftState = ShiftState.OFF,
    val layer: xyz.xiao6.myboard.model.KeyboardLayer = xyz.xiao6.myboard.model.KeyboardLayer.ALPHA,
    /**
     * Current locale tag (normalized, e.g. "zh-CN" or "en-US") used for key action matching.
     */
    val localeTag: String? = null,
    /**
     * Input engine tag used for scripted key behavior matching (e.g. "ZH_PINYIN", "DIRECT").
     * Empty/null means "unknown".
     */
    val engine: String? = null,
    val hiddenKeyIds: Set<String> = emptySet(),
    val highlightedKeyIds: Set<String> = emptySet(),
    /**
     * Per-key label override (no geometry change). Used for locale-dependent symbol/label replacement
     * without reloading layout JSON.
     */
    val labelOverrides: Map<String, String> = emptyMap(),
    /**
     * Per-key hint override (no geometry change). Merged on top of Key.hints (override wins).
     */
    val hintOverrides: Map<String, Map<String, String>> = emptyMap(),
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
// In-layout layer/page is now represented by model.KeyboardLayer.
