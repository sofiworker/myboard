package xyz.xiao6.myboard.manager

import android.content.Context
import xyz.xiao6.myboard.model.ThemeParser
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.util.MLog

class ThemeManager(
    private val context: Context,
) {
    private val logTag = "ThemeManager"
    private val themes = LinkedHashMap<String, ThemeSpec>()

    fun loadAllFromAssets(dir: String = "themes"): ThemeManager {
        themes.clear()
        val assetManager = context.assets
        val files = runCatching { assetManager.list(dir)?.toList().orEmpty() }.getOrDefault(emptyList())
        for (file in files) {
            if (!file.endsWith(".json", ignoreCase = true)) continue
            val path = "$dir/$file"
            val spec =
                runCatching {
                    val text = assetManager.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }
                    ThemeParser.parseThemeSpec(text, useDark = ThemeParser.isSystemDark(context))
                }.onFailure { t ->
                    MLog.e(logTag, "Failed to load theme: $path", t)
                }.getOrNull() ?: continue
            themes[spec.themeId] = spec
            MLog.d(logTag, "loaded themeId=${spec.themeId} name=${spec.name ?: ""} path=$path")
        }
        return this
    }

    fun listAll(): List<ThemeSpec> = themes.values.toList()

    fun getTheme(themeId: String): ThemeSpec? = themes[themeId]

    fun getDefaultTheme(): ThemeSpec? = themes.values.firstOrNull()
}

