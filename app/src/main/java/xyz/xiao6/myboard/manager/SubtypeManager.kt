package xyz.xiao6.myboard.manager

import android.content.Context
import xyz.xiao6.myboard.model.LocaleLayoutProfile
import xyz.xiao6.myboard.model.SubtypeParser
import java.io.File
import java.util.zip.ZipInputStream
import java.util.Locale

/**
 * SubtypeManager（方案2最终）：管理 “locale -> layoutIds[]” 映射。
 * SubtypeManager (final scheme2): manages the "locale -> layoutIds[]" mapping.
 *
 * 内置：assets/subtypes/ *.json（支持单个 LocaleLayoutProfile 或 SubtypePack）
 * 用户自定义：filesDir/subtypes/ *.json（同 schema，可覆盖同 localeTag 的内置项）
 */
class SubtypeManager(
    private val context: Context,
    private val builtInAssetsDir: String = "subtypes",
    private val userDir: File = File(context.filesDir, "subtypes"),
) {
    private var loaded = false
    private val profilesByLocaleTag = LinkedHashMap<String, LocaleLayoutProfile>()

    fun loadAll(): SubtypeManager {
        if (loaded) return this
        loaded = true

        loadBuiltInFromAssets()
        loadUserDefinedFromFiles()

        validate()
        return this
    }

    fun listAll(): List<LocaleLayoutProfile> {
        ensureLoaded()
        return profilesByLocaleTag.values.toList()
    }

    fun get(localeTag: String): LocaleLayoutProfile? {
        ensureLoaded()
        return profilesByLocaleTag[normalizeLocaleTag(localeTag)]
    }

    /**
     * 按 Locale 匹配可用 profile 列表（按优先级排序）。
     * Returns matching locale profiles for a locale (sorted by score/priority).
     */
    fun findByLocale(locale: Locale): List<LocaleLayoutProfile> {
        ensureLoaded()
        val tags = candidateTagsFor(locale)
        val languageOnly = normalizeLocaleTag(locale.language)

        val scored = mutableListOf<Pair<Int, LocaleLayoutProfile>>()
        for (profile in profilesByLocaleTag.values) {
            if (!profile.enabled) continue
            val profileTag = normalizeLocaleTag(profile.localeTag)

            val score = when {
                profileTag.isBlank() -> 0
                tags.any { it == profileTag } -> 100
                languageOnly.isNotBlank() && profileTag == languageOnly -> 50
                else -> 0
            }
            if (score > 0) scored += (score to profile)
        }

        return scored
            .sortedWith(compareByDescending<Pair<Int, LocaleLayoutProfile>> { it.first }
                .thenByDescending { it.second.priority }
                .thenBy { it.second.localeTag })
            .map { it.second }
    }

    /**
     * 解析默认 profile：可传 preferredLocaleTag（例如用户在设置中选择）。
     * Resolve default locale profile with optional preferred locale tag.
     */
    fun resolve(locale: Locale, preferredLocaleTag: String? = null): LocaleLayoutProfile? {
        ensureLoaded()
        if (!preferredLocaleTag.isNullOrBlank()) {
            val preferred = get(preferredLocaleTag)
            if (preferred != null && preferred.enabled) return preferred
        }
        return findByLocale(locale).firstOrNull()
    }

    /**
     * 供用户自定义：写入一个 locale profile JSON 文件（会覆盖同 localeTag）。
     * User-defined profile: saves JSON under filesDir.
     */
    fun saveUserProfile(profile: LocaleLayoutProfile) {
        userDir.mkdirs()
        val normalizedTag = normalizeLocaleTag(profile.localeTag)
        val safe = normalizedTag.replace("-", "_").ifBlank { "unknown" }
        val file = File(userDir, "locale_$safe.json")
        file.writeText(
            """
            {
              "localeTag": "${profile.localeTag}",
              "layoutIds": ${profile.layoutIds.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }},
              "defaultLayoutId": ${profile.defaultLayoutId?.let { "\"$it\"" } ?: "null"},
              "enabled": ${profile.enabled},
              "priority": ${profile.priority}
            }
            """.trimIndent(),
        )
        profilesByLocaleTag[normalizedTag] = profile.copy(localeTag = normalizedTag)
    }

    private fun loadBuiltInFromAssets() {
        val files = context.assets.list(builtInAssetsDir).orEmpty()
            .filter {
                it.endsWith(".json", ignoreCase = true) ||
                    it.endsWith(".json.jar", ignoreCase = true)
            }
            .sorted()

        for (file in files) {
            val text = readAssetText("$builtInAssetsDir/$file")
            val pack = runCatching { SubtypeParser.parsePack(text) }.getOrNull()
            val profiles = if (pack != null && pack.locales.isNotEmpty()) {
                pack.locales
            } else {
                // Same rationale as DictionaryManager: a single profile JSON may decode as an "empty pack".
                listOf(SubtypeParser.parseProfile(text))
            }
            for (p in profiles) {
                val tag = normalizeLocaleTag(p.localeTag)
                profilesByLocaleTag[tag] = p.copy(localeTag = tag)
            }
        }
    }

    private fun loadUserDefinedFromFiles() {
        userDir.mkdirs()
        val files = userDir.listFiles()?.filter { it.isFile && it.name.endsWith(".json", ignoreCase = true) }.orEmpty()
            .sortedBy { it.name }
        for (file in files) {
            val text = file.readText()
            val pack = runCatching { SubtypeParser.parsePack(text) }.getOrNull()
            val profiles = if (pack != null && pack.locales.isNotEmpty()) {
                pack.locales
            } else {
                listOf(SubtypeParser.parseProfile(text))
            }
            for (p in profiles) {
                val tag = normalizeLocaleTag(p.localeTag)
                profilesByLocaleTag[tag] = p.copy(localeTag = tag) // user overrides built-in
            }
        }
    }

    private fun validate() {
        val errors = mutableListOf<String>()
        for (p in profilesByLocaleTag.values) {
            val tag = normalizeLocaleTag(p.localeTag)
            if (tag.isBlank()) errors += "localeTag must not be blank"
            if (p.layoutIds.any { it.isBlank() }) errors += "layoutIds must not contain blank items (localeTag=$tag)"
            if (p.layoutIds.distinct().size != p.layoutIds.size) errors += "layoutIds must not contain duplicates (localeTag=$tag)"
        }
        require(errors.isEmpty()) { errors.joinToString("; ") }
    }

    private fun ensureLoaded() {
        check(loaded) { "SubtypeManager not loaded; call loadAll() first." }
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
