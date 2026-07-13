package xyz.xiao6.myboard.theme.foundation

object ThemeColorUtils {
    fun normalizeHex(input: String, fallback: String = "#1A73E8"): String {
        val body = input.trim().removePrefix("#")
        val normalized = when (body.length) {
            6 -> body
            8 -> body.takeLast(6)
            else -> return fallback
        }
        return if (normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "#${normalized.uppercase()}"
        } else {
            fallback
        }
    }

    fun withAlpha(hex: String, alpha: Int): String {
        val rgb = normalizeHex(hex).removePrefix("#")
        val alphaHex = alpha.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()
        return "#$alphaHex$rgb"
    }

    fun mix(fromHex: String, toHex: String, amount: Float): String {
        val from = parseRgb(normalizeHex(fromHex))
        val to = parseRgb(normalizeHex(toHex))
        val t = amount.coerceIn(0f, 1f)
        val r = (from[0] + (to[0] - from[0]) * t).toInt().coerceIn(0, 255)
        val g = (from[1] + (to[1] - from[1]) * t).toInt().coerceIn(0, 255)
        val b = (from[2] + (to[2] - from[2]) * t).toInt().coerceIn(0, 255)
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
    }

    private fun parseRgb(hex: String): IntArray {
        val body = normalizeHex(hex).removePrefix("#")
        return intArrayOf(
            body.substring(0, 2).toInt(16),
            body.substring(2, 4).toInt(16),
            body.substring(4, 6).toInt(16)
        )
    }
}
