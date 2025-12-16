package xyz.xiao6.myboard.ui.theme

import android.graphics.Color
import xyz.xiao6.myboard.model.ShadowStyle
import xyz.xiao6.myboard.model.StrokeStyle
import xyz.xiao6.myboard.model.ThemeSpec

/**
 * Lightweight runtime resolver for ThemeSpec tokens (e.g. "colors.key_text") and hex colors.
 */
class ThemeRuntime(
    private val theme: ThemeSpec,
) {
    fun resolveColor(value: String?, fallback: Int): Int {
        if (value.isNullOrBlank()) return fallback
        val resolved = resolveColorString(value.trim(), depth = 0) ?: return fallback
        return runCatching { Color.parseColor(resolved) }.getOrDefault(fallback)
    }

    fun resolveStrokeColor(stroke: StrokeStyle?, fallback: Int): Int = resolveColor(stroke?.color, fallback)

    fun resolveShadowColor(shadow: ShadowStyle?, fallback: Int): Int = resolveColor(shadow?.color, fallback)

    private fun resolveColorString(value: String, depth: Int): String? {
        if (depth >= 8) return null
        if (!value.startsWith("colors.")) return value
        val key = value.removePrefix("colors.").trim()
        val next = theme.colors[key]?.trim().orEmpty()
        if (next.isBlank()) return null
        return resolveColorString(next, depth + 1)
    }
}

