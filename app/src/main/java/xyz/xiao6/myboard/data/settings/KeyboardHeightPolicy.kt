package xyz.xiao6.myboard.data.settings

import kotlin.math.roundToInt

object KeyboardHeightPolicy {
    const val KEY_HEIGHT = "keyboard_height"
    const val KEY_HORIZONTAL_INSET = "keyboard_horizontal_inset"

    const val MIN_HEIGHT_DP = 180
    const val MAX_HEIGHT_DP = 400
    const val MIN_CONTENT_HEIGHT_DP = 120
    const val MIN_HORIZONTAL_INSET_DP = 0
    const val MAX_HORIZONTAL_INSET_DP = 24

    private const val DEFAULT_HEIGHT_SCREEN_RATIO = 0.32f
    private const val DEFAULT_HORIZONTAL_INSET_SCREEN_RATIO = 0.02f

    data class HeightResolution(
        val heightDp: Int,
        val shouldPersist: Boolean
    )

    data class HorizontalInsetResolution(
        val insetDp: Int,
        val shouldPersist: Boolean
    )

    fun defaultHeightDp(screenHeightDp: Int): Int =
        (screenHeightDp * DEFAULT_HEIGHT_SCREEN_RATIO)
            .roundToInt()
            .coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)

    fun resolve(storedHeight: String?, screenHeightDp: Int): HeightResolution {
        val parsed = storedHeight?.toIntOrNull()
        val height = parsed?.coerceIn(MIN_HEIGHT_DP, MAX_HEIGHT_DP)
            ?: defaultHeightDp(screenHeightDp)
        return HeightResolution(
            heightDp = height,
            shouldPersist = parsed == null || parsed != height
        )
    }

    fun contentHeightDp(pageHeightDp: Int, chromeHeightDp: Int): Int =
        (pageHeightDp - chromeHeightDp).coerceAtLeast(MIN_CONTENT_HEIGHT_DP)

    fun defaultHorizontalInsetDp(screenWidthDp: Int): Int =
        (screenWidthDp * DEFAULT_HORIZONTAL_INSET_SCREEN_RATIO)
            .roundToInt()
            .coerceIn(MIN_HORIZONTAL_INSET_DP, MAX_HORIZONTAL_INSET_DP)

    fun resolveHorizontalInset(storedInset: String?, screenWidthDp: Int): HorizontalInsetResolution {
        val parsed = storedInset?.toIntOrNull()
        val inset = parsed?.coerceIn(MIN_HORIZONTAL_INSET_DP, MAX_HORIZONTAL_INSET_DP)
            ?: defaultHorizontalInsetDp(screenWidthDp)
        return HorizontalInsetResolution(
            insetDp = inset,
            shouldPersist = parsed == null || parsed != inset
        )
    }
}
