package xyz.xiao6.myboard.manager

import android.content.Context
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.LayoutParser
import xyz.xiao6.myboard.model.validate
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * LayoutManager：负责按 Locale 查找并加载 KeyboardLayout（JSON -> Model）。
 * LayoutManager: loads and resolves KeyboardLayout (JSON -> Model) by Locale.
 *
 * 多对多关系示例 / Many-to-many example:
 * - 同一个布局可以被多个语言复用：en_US 与 zh_CN 都指向 layoutId="qwerty"
 *   The same layout can serve multiple locales: en_US and zh_CN both use "qwerty"
 * - 输入模式/字典是另一条维度：layout 决定几何与键位，decoder/dictionary/recognizer 决定候选词/联想
 *   Input mode/dictionary is another dimension: layout decides geometry/keys, decoder/dictionary/recognizer decides candidates
 */
class LayoutManager(
    private val context: Context,
    private val assetsDir: String = "layouts",
) {
    private val layoutsById = LinkedHashMap<String, KeyboardLayout>()
    private val layoutIdsByLocaleTag = LinkedHashMap<String, LinkedHashSet<String>>()
    private var loaded = false

    /**
     * 一次性加载 assets/layouts 目录下所有 .json 作为候选布局。
     * Loads all *.json under assets/layouts as available layouts.
     */
    fun loadAllFromAssets(): LayoutManager {
        if (loaded) return this
        loaded = true

        val files = context.assets.list(assetsDir).orEmpty()
            .filter {
                it.endsWith(".json", ignoreCase = true) ||
                    it.endsWith(".json.jar", ignoreCase = true)
            }
            .sorted()

        for (file in files) {
            val text = readAssetText("$assetsDir/$file")
            val layout = LayoutParser.parse(text)

            val errors = layout.validate()
            require(errors.isEmpty()) {
                "Invalid layout '$file' (layoutId=${layout.layoutId}): ${errors.joinToString("; ")}"
            }

            layoutsById[layout.layoutId] = layout
            indexLocales(layout)
        }

        return this
    }

    /**
     * 通过 layoutId 获取布局。
     * Get a layout by layoutId.
     */
    fun getLayout(layoutId: String): KeyboardLayout {
        ensureLoaded()
        return layoutsById[layoutId]
            ?: error("Layout not found: $layoutId (available=${layoutsById.keys})")
    }

    /**
     * 查找某个 Locale 支持的所有布局（一个 locale 可以对应多个 layout）。
     * Find all layouts supported by a locale (one locale can map to multiple layouts).
     */
    fun findLayouts(locale: Locale): List<KeyboardLayout> {
        ensureLoaded()
        val tags = candidateTagsFor(locale)
        val ids = LinkedHashSet<String>()
        for (tag in tags) {
            layoutIdsByLocaleTag[tag]?.let { ids.addAll(it) }
        }
        return ids.mapNotNull { layoutsById[it] }
    }

    /**
     * 查找某个 Locale 的默认布局（取匹配到的第一个）。
     * Get the default layout for a locale (first match).
     */
    fun getDefaultLayout(locale: Locale): KeyboardLayout {
        return findLayouts(locale).firstOrNull()
            ?: error("No layout found for locale=${locale.toLanguageTag()}")
    }

    private fun indexLocales(layout: KeyboardLayout) {
        for (raw in layout.locale) {
            val tag = normalizeLocaleTag(raw)
            layoutIdsByLocaleTag.getOrPut(tag) { LinkedHashSet() }.add(layout.layoutId)
        }
    }

    private fun ensureLoaded() {
        check(loaded) { "LayoutManager not loaded; call loadAllFromAssets() first." }
    }

    private fun candidateTagsFor(locale: Locale): List<String> {
        val language = locale.language.lowercase(Locale.ROOT)
        val region = locale.country.uppercase(Locale.ROOT)
        val languageRegion = if (region.isNotBlank()) "$language-$region" else language
        return listOf(
            normalizeLocaleTag(languageRegion),
            normalizeLocaleTag(language),
        ).distinct()
    }

    private fun normalizeLocaleTag(tag: String): String {
        val t = tag.trim().replace('_', '-')
        val parts = t.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val language = parts[0].lowercase(Locale.ROOT)
        val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
        return if (region.isNullOrBlank()) language else "$language-$region"
    }

    private fun readAssetText(path: String): String {
        if (path.endsWith(".jar", ignoreCase = true)) {
            context.assets.open(path).use { raw ->
                ZipInputStream(raw).use { zis ->
                    val entry = zis.nextEntry ?: error("Empty asset jar: $path")
                    val bytes = zis.readBytes()
                    zis.closeEntry()
                    return bytes.toString(Charsets.UTF_8)
                }
            }
        }
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
