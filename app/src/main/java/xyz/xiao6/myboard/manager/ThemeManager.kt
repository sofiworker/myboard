package xyz.xiao6.myboard.manager

import android.content.Context
import java.io.File
import xyz.xiao6.myboard.model.ThemeParser
import xyz.xiao6.myboard.model.ThemeSpec
import xyz.xiao6.myboard.util.MLog

class ThemeManager(
    private val context: Context,
    private val builtInAssetsDir: String = "themes",
    private val userDir: File = File(context.filesDir, DEFAULT_USER_THEME_DIR),
) {
    private val logTag = "ThemeManager"
    private var loaded = false
    private val themes = LinkedHashMap<String, ThemeSpec>()

    fun loadAll(): ThemeManager {
        if (loaded) return this
        loaded = true
        loadBuiltInFromAssets()
        loadUserDefinedFromFiles()
        return this
    }

    fun reload(): ThemeManager {
        loaded = false
        themes.clear()
        return loadAll()
    }

    fun loadAllFromAssets(dir: String = builtInAssetsDir): ThemeManager {
        themes.clear()
        loadBuiltInFromAssets(dir)
        return this
    }

    fun listAll(): List<ThemeSpec> = themes.values.toList()

    fun getTheme(themeId: String): ThemeSpec? = themes[themeId]

    fun getDefaultTheme(): ThemeSpec? = themes["default"] ?: themes.values.firstOrNull()

    fun getUserThemeDir(): File = userDir.apply { mkdirs() }

    private fun loadBuiltInFromAssets(dir: String = builtInAssetsDir) {
        val assetManager = context.assets
        val files = runCatching { assetManager.list(dir)?.toList().orEmpty() }.getOrDefault(emptyList())
            .filter { it.endsWith(".json", ignoreCase = true) }
            .sorted()
        for (file in files) {
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
    }

    private fun loadUserDefinedFromFiles() {
        userDir.mkdirs()
        val files = userDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }.orEmpty()
            .sortedBy { it.name }
        for (file in files) {
            val spec =
                runCatching {
                    val text = file.readText(Charsets.UTF_8)
                    ThemeParser.parseThemeSpec(text, useDark = ThemeParser.isSystemDark(context))
                }.onFailure { t ->
                    MLog.e(logTag, "Failed to load custom theme: ${file.absolutePath}", t)
                }.getOrNull() ?: continue
            themes[spec.themeId] = spec
            MLog.d(logTag, "loaded custom themeId=${spec.themeId} name=${spec.name ?: ""} path=${file.name}")
        }
    }

    companion object {
        const val DEFAULT_USER_THEME_DIR = "themes"
    }
}

