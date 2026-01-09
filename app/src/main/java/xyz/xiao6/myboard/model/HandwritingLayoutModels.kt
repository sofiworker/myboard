package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * Handwriting panel layout mode
 * 手写面板布局模式
 */
@Serializable
enum class HandwritingLayoutMode {
    /** Overlay on keyboard (small area at top) */
    OVERLAY,
    /** Half screen (top or bottom half) */
    HALF_SCREEN,
    /** Full screen handwriting */
    FULL_SCREEN,
}

/**
 * Handwriting panel position for half-screen mode
 * 半屏模式下的面板位置
 */
@Serializable
enum class HandwritingPosition {
    /** Top half of screen */
    TOP,
    /** Bottom half of screen */
    BOTTOM,
}

/**
 * Configuration for handwriting panel layout
 * 手写面板布局配置
 */
@Serializable
data class HandwritingPanelConfig(
    /** Layout mode */
    val mode: HandwritingLayoutMode = HandwritingLayoutMode.HALF_SCREEN,
    /** Position for half-screen mode */
    val position: HandwritingPosition = HandwritingPosition.BOTTOM,
    /** Show keyboard toggle button */
    val showKeyboardToggle: Boolean = true,
    /** Show clear button */
    val showClearButton: Boolean = true,
    /** Show candidate suggestions */
    val showCandidates: Boolean = true,
    /** Auto-hide panel after recognition */
    val autoHide: Boolean = false,
    /** Auto-hide delay in milliseconds */
    val autoHideDelayMs: Long = 1500L,
)
