package xyz.xiao6.myboard.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一的设计令牌系统
 * Unified Design Token System for MyBoard
 *
 * 遵循 Material 3 设计规范，提供统一的间距、圆角、阴影、动画等设计值
 * Follows Material 3 design specifications for consistent spacing, corners, shadows, and animations
 */
object DesignTokens {

    object Spacing {
        val xs: Dp = 4.dp
        val sm: Dp = 8.dp
        val md: Dp = 16.dp
        val lg: Dp = 24.dp
        val xl: Dp = 32.dp
        val xxl: Dp = 48.dp
    }

    object CornerRadius {
        val sm: Dp = 8.dp
        val md: Dp = 12.dp
        val lg: Dp = 16.dp
        val xl: Dp = 24.dp
        val full: Dp = 28.dp
    }

    object Elevation {
        val level0: Dp = 0.dp
        val level1: Dp = 1.dp
        val level2: Dp = 3.dp
        val level3: Dp = 6.dp
        val level4: Dp = 8.dp
        val level5: Dp = 12.dp
    }

    object IconSize {
        val small: Dp = 18.dp
        val medium: Dp = 24.dp
        val large: Dp = 32.dp
        val xlarge: Dp = 48.dp
    }

    object ButtonHeight {
        val small: Dp = 32.dp
        val medium: Dp = 40.dp
        val large: Dp = 48.dp
    }

    object Keyboard {
        val keyCornerRadiusNormal: Dp = CornerRadius.md
        val keyCornerRadiusFunction: Dp = CornerRadius.sm
        val keyGapHorizontal: Dp = Spacing.xs
        val keyGapVertical: Dp = Spacing.xs
        val keyPaddingHorizontal: Dp = Spacing.sm
        val keyPaddingVertical: Dp = Spacing.xs
        val keyShadowRadius: Dp = 2.dp
        val keyShadowOffsetY: Dp = 1.dp
        val keyShadowAlpha: Float = 0.1f
        val keyTextSizeNormal: Float = 16f
        val keyTextSizeSmall: Float = 14f
        val keyHintSize: Float = 10f
    }

    object AnimationDuration {
        val instant: Int = 0
        val short: Int = 150
        val medium: Int = 250
        val long: Int = 350
        val extraLong: Int = 500
    }

    data class ShadowSpec(
        val radius: Dp = Elevation.level2,
        val dx: Float = 0f,
        val dy: Dp = 1.dp,
        val alpha: Float = 0.1f
    )

    object Shadows {
        val subtle: ShadowSpec = ShadowSpec(
            radius = 2.dp,
            dx = 0f,
            dy = 1.dp,
            alpha = 0.08f
        )

        val standard: ShadowSpec = ShadowSpec(
            radius = 3.dp,
            dx = 0f,
            dy = 2.dp,
            alpha = 0.12f
        )

        val prominent: ShadowSpec = ShadowSpec(
            radius = 6.dp,
            dx = 0f,
            dy = 3.dp,
            alpha = 0.15f
        )
    }

    object FontWeight {
        const val Light = 300
        const val Regular = 400
        const val Medium = 500
        const val SemiBold = 600
        const val Bold = 700
    }

    object Opacity {
        val fullyOpaque: Float = 1.0f
        val high: Float = 0.87f
        val medium: Float = 0.60f
        val low: Float = 0.38f
        val veryLow: Float = 0.12f
    }
}

@Composable
fun spacing() = DesignTokens.Spacing
@Composable
fun cornerRadius() = DesignTokens.CornerRadius
@Composable
fun elevation() = DesignTokens.Elevation
@Composable
fun iconSize() = DesignTokens.IconSize
