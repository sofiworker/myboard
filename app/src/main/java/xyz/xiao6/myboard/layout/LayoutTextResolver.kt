package xyz.xiao6.myboard.layout

internal object LayoutTextResolver {
    private const val STRING_PREFIX = "@string/"

    fun resolve(text: String?, lookup: (String) -> String?): String? {
        if (text == null || !text.startsWith(STRING_PREFIX)) return text
        val key = text.removePrefix(STRING_PREFIX)
        return lookup(key) ?: text
    }
}
