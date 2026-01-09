package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * Input mode for alternative input methods
 * 输入模式：替代输入方法
 */
@Serializable
enum class InputMode {
    /** Normal tap input on keyboard */
    NORMAL,
    /** Swype/gesture typing on QWERTY keyboard */
    SWYPE,
    /** Handwriting input on overlay panel */
    HANDWRITING,
}

/**
 * Configuration for swype input
 * 滑行输入配置
 */
@Serializable
data class SwypeConfig(
    /** Minimum distance to trigger swype (dp) */
    val minDistanceDp: Float = 30f,
    /** Maximum time for a swype gesture (ms) */
    val maxDurationMs: Long = 800L,
    /** Show trail line during swype */
    val showTrail: Boolean = true,
    /** Trail line color (ARGB) */
    val trailColor: Int = 0x80000000.toInt(),
    /** Trail line width (dp) */
    val trailWidthDp: Float = 4f,
)

/**
 * Configuration for handwriting input
 * 手写输入配置
 */
@Serializable
data class HandwritingConfig(
    /** Stroke width (dp) */
    val strokeWidthDp: Float = 6f,
    /** Stroke color (ARGB) */
    val strokeColor: Int = 0xFF000000.toInt(),
    /** Show stroke preview */
    val showStrokePreview: Boolean = true,
    /** Auto-recognize timeout (ms), 0 for manual trigger only */
    val autoRecognizeTimeoutMs: Long = 500L,
    /** Maximum strokes per recognition */
    val maxStrokesPerRecognition: Int = 20,
)
