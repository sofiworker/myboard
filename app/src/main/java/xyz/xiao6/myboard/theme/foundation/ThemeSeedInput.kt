package xyz.xiao6.myboard.theme.foundation

object ThemeSeedInput {
    fun normalizeOrNull(input: String): String? {
        val body = input.trim().removePrefix("#")
        if (body.length != 6 || !body.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            return null
        }
        return "#${body.uppercase()}"
    }
}
