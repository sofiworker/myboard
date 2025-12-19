package xyz.xiao6.myboard.util

import kotlin.math.roundToInt

object KeyboardSizeConstraints {
    const val MIN_KEYBOARD_WIDTH_DP: Float = 240f
    const val MIN_KEYBOARD_HEIGHT_DP: Float = 160f
    const val TOP_BAR_HEIGHT_DP: Float = 48f

    /**
     * Max keyboard (content area) height ratio relative to the full screen height.
     * Keep below 1.0 to avoid a full-screen IME experience.
     */
    const val MAX_KEYBOARD_HEIGHT_RATIO: Float = 0.55f

    fun minKeyboardWidthPx(density: Float): Int = (density * MIN_KEYBOARD_WIDTH_DP).roundToInt()
    fun minKeyboardHeightPx(density: Float): Int = (density * MIN_KEYBOARD_HEIGHT_DP).roundToInt()
    fun topBarHeightPx(density: Float): Int = (density * TOP_BAR_HEIGHT_DP).roundToInt()

    fun maxKeyboardHeightPx(screenHeightPx: Int, density: Float): Int {
        val maxByRatio = (screenHeightPx * MAX_KEYBOARD_HEIGHT_RATIO).roundToInt()
        val maxByScreen = (screenHeightPx - topBarHeightPx(density)).coerceAtLeast(0)
        return minOf(maxByRatio, maxByScreen)
    }
}

