package xyz.xiao6.myboard.manager

import android.content.Context
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryFileV1
import xyz.xiao6.myboard.model.DictionaryParser
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.util.MLog
import java.io.File
import java.util.zip.ZipInputStream
import java.util.Locale

/**
 * DictionaryManager：管理字典元数据。
 * DictionaryManager: manages dictionary metadata.
 *
 * 设计要点 / Design:
 * - 内置字典：assets/dictionary/meta 下的 *.json（元数据），数据文件可由 DictionarySpec.assetPath 指向
 * - 用户自定义：filesDir/dictionary/ *.json（覆盖同 dictionaryId）
 */
class DictionaryManager(
    private val context: Context,
    private val builtInAssetsDir: String = "dictionary/meta",
    private val userDir: File = File(context.filesDir, "dictionary"),
) {
    private val logTag = "DictionaryManager"
    private var loaded = false
    private val specsById = LinkedHashMap<String, DictionarySpec>()

    fun loadAll(): DictionaryManager {
        if (loaded) return this
        loaded = true

        loadBuiltInFromAssets()
        loadUserDefinedFromFiles()
        loadUserDefinedFromMybdf()
        validate()

        MLog.d(logTag, "loaded dictionaries=${specsById.size} ids=${specsById.keys}")
        return this
    }

    fun get(dictionaryId: String): DictionarySpec {
        ensureLoaded()
        return specsById[dictionaryId]
            ?: error("Dictionary not found: $dictionaryId (available=${specsById.keys})")
    }

    fun listAll(): List<DictionarySpec> {
        ensureLoaded()
        return specsById.values.toList()
    }

    /**
     * 供 UI 设置页：按 locale 筛选可用字典（不决定 layout）。
     * For settings UI: find dictionaries matching a locale (does not choose layout).
     */
    fun findByLocale(locale: Locale): List<DictionarySpec> {
        ensureLoaded()
        val tags = candidateTagsFor(locale)
        val scored = mutableListOf<Pair<Int, DictionarySpec>>()
        for (spec in specsById.values) {
            if (!spec.enabled) continue
            val normalizedTags = spec.localeTags.map { normalizeLocaleTag(it) }.filter { it.isNotBlank() }.distinct()
            val score = when {
                normalizedTags.isEmpty() -> 10
                tags.any { it in normalizedTags } -> 100
                else -> 0
            }
            if (score > 0) scored += (score to spec)
        }
        return scored
            .sortedWith(compareByDescending<Pair<Int, DictionarySpec>> { it.first }
                .thenByDescending { it.second.priority }
                .thenBy { it.second.dictionaryId })
            .map { it.second }
    }

    private fun loadBuiltInFromAssets() {
        val files = context.assets.list(builtInAssetsDir).orEmpty()
            .filter {
                it.endsWith(".json", ignoreCase = true) ||
                    it.endsWith(".json.jar", ignoreCase = true)
            }
            .sorted()

        MLog.d(logTag, "assets/$builtInAssetsDir files=$files")

        for (file in files) {
            val text = readAssetText("$builtInAssetsDir/$file")
            val pack = runCatching { DictionaryParser.parsePack(text) }.getOrNull()
            val specs = if (pack != null && pack.dictionaries.isNotEmpty()) {
                pack.dictionaries
            } else {
                // When `ignoreUnknownKeys=true`, decoding a single spec JSON into DictionaryPack may "succeed" with empty list.
                listOf(DictionaryParser.parseSpec(text))
            }
            for (spec in specs) {
                specsById[spec.dictionaryId] = spec
                MLog.d(
                    logTag,
                    "loaded spec id=${spec.dictionaryId} localeTags=${spec.localeTags} layoutIds=${spec.layoutIds} assetPath=${spec.assetPath}",
                )
            }
        }
    }

    private fun loadUserDefinedFromFiles() {
        userDir.mkdirs()
        val files = userDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }.orEmpty()
            .sortedBy { it.name }

        for (file in files) {
            val text = file.readText()
            val pack = runCatching { DictionaryParser.parsePack(text) }.getOrNull()
            val specs = if (pack != null && pack.dictionaries.isNotEmpty()) {
                pack.dictionaries
            } else {
                listOf(DictionaryParser.parseSpec(text))
            }
            for (spec in specs) {
                specsById[spec.dictionaryId] = spec // override built-in
            }
        }
    }

    private fun loadUserDefinedFromMybdf() {
        userDir.mkdirs()
        val files = userDir.listFiles()?.filter { it.isFile && it.name.endsWith(".mybdict", ignoreCase = true) }.orEmpty()
            .sortedBy { it.name }

        for (file in files) {
            val parsed = runCatching { MyBoardDictionaryFileV1.readHeaderAndMeta(file) }.getOrNull() ?: continue
            val (header, meta) = parsed
            val id = meta.dictionaryId.trim()
            if (id.isBlank()) continue
            if (specsById.containsKey(id)) continue // explicit json wins; also avoids overriding built-in unintentionally

            specsById[id] = DictionarySpec(
                dictionaryId = id,
                name = meta.name,
                localeTags = meta.languages,
                layoutIds = emptyList(),
                assetPath = null,
                filePath = file.absolutePath,
                dictionaryVersion = header.dictVersion.toString(),
                enabled = true,
                priority = 0,
            )
        }
    }

    private fun validate() {
        val errors = mutableListOf<String>()
        for (spec in specsById.values) {
            if (spec.dictionaryId.isBlank()) errors += "dictionaryId must not be blank"
            if (spec.localeTags.any { it.isBlank() }) errors += "localeTags must not contain blank items (dictionaryId=${spec.dictionaryId})"
            if (spec.localeTags.distinct().size != spec.localeTags.size) errors += "localeTags must not contain duplicates (dictionaryId=${spec.dictionaryId})"
            if (spec.layoutIds.any { it.isBlank() }) errors += "layoutIds must not contain blank items (dictionaryId=${spec.dictionaryId})"
            if (spec.layoutIds.distinct().size != spec.layoutIds.size) errors += "layoutIds must not contain duplicates (dictionaryId=${spec.dictionaryId})"
            if (spec.codeScheme != null && spec.codeScheme.isBlank()) errors += "codeScheme must not be blank (dictionaryId=${spec.dictionaryId})"
            if (spec.kind != null && spec.kind.isBlank()) errors += "kind must not be blank (dictionaryId=${spec.dictionaryId})"
            if (spec.core != null && spec.core.isBlank()) errors += "core must not be blank (dictionaryId=${spec.dictionaryId})"
            if (spec.variant != null && spec.variant.isBlank()) errors += "variant must not be blank (dictionaryId=${spec.dictionaryId})"
        }
        require(errors.isEmpty()) { errors.joinToString("; ") }
    }

    private fun ensureLoaded() {
        check(loaded) { "DictionaryManager not loaded; call loadAll() first." }
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
        // Some build pipelines may package assets into a *.jar container.
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
