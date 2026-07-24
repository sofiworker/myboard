package xyz.xiao6.myboard.pack

import java.security.MessageDigest
import xyz.xiao6.myboard.contract.engine.DictionaryBinding
import xyz.xiao6.myboard.contract.engine.DictionaryKind
import xyz.xiao6.myboard.contract.engine.DictionaryRole
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.LocaleDefaults
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.manifest.ScriptCatalog
import xyz.xiao6.myboard.contract.manifest.ScriptManifest
import xyz.xiao6.myboard.contract.manifest.SubtypeInfo
import xyz.xiao6.myboard.contract.manifest.normalizeResourcePath
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.engine.ResolvedResourceCatalog
import xyz.xiao6.myboard.engine.DictionaryRegistryImpl
import xyz.xiao6.myboard.contract.engine.Dictionary
import xyz.xiao6.myboard.contract.input.Candidate
import xyz.xiao6.myboard.contract.registry.DictionaryKey

/** Built-in packages are manifests first; asset bytes remain the source of resource identity. */
object BuiltInLanguagePacks {
    val packageVersion = SemVer(1, 0, 0)
    private const val BUILTIN = "builtin"

    val zhCN = pack("language.zh-CN", "zh-CN", mapOf("en-US" to "Chinese", "zh-CN" to "Chinese"), Script.HANI, BuiltInSchemas.PINYIN,
        listOf(Script.HANI to BuiltInSchemas.PINYIN, Script.LATN to BuiltInSchemas.LATIN_DIRECT), listOf(
            capability("language.zh-CN", "zh-CN", Script.HANI, BuiltInSchemas.PINYIN, "table_composing", "qwerty", encoder = "identity", dictionary = true, candidate = "chinese_default", display = "show_query", subtype = "subtype_zh_pinyin"),
            capability("language.zh-CN", "zh-CN", Script.HANI, BuiltInSchemas.SHUANGPIN_ZIRAN, "table_composing", "shuangpin_ziran", encoder = "shuangpin_ziran", encoderConfig = "engines/ziran_map.json", dictionary = true, candidate = "chinese_default", display = "show_query", subtype = "subtype_zh_shuangpin"),
            capability("language.zh-CN", "zh-CN", Script.HANI, BuiltInSchemas.T9_PINYIN, "table_composing", "t9_chinese", encoder = "t9", encoderConfig = "engines/t9_keymap.json", dictionary = true, candidate = "chinese_default", display = "show_query", subtype = "subtype_zh_t9"),
            capability("language.zh-CN", "zh-CN", Script.HANI, BuiltInSchemas.DOUBLE_PINYIN, "table_composing", "shuangpin_ziran", encoder = "shuangpin_ziran", encoderConfig = "engines/ziran_map.json", dictionary = true, candidate = "chinese_default", display = "show_query", subtype = "subtype_zh_double"),
            capability("language.zh-CN", "zh-CN", Script.LATN, BuiltInSchemas.LATIN_DIRECT, "direct", "qwerty", mapping = "maps/latin_qwerty.json", candidate = "direct_default", display = "hidden", subtype = "subtype_zh_latin", shift = true)
        ))

    val enUS = pack("language.en-US", "en-US", mapOf("en-US" to "English", "zh-CN" to "English"), Script.LATN, BuiltInSchemas.LATIN_DIRECT,
        listOf(Script.LATN to BuiltInSchemas.LATIN_DIRECT), listOf(
            capability("language.en-US", "en-US", Script.LATN, BuiltInSchemas.LATIN_DIRECT, "direct", "qwerty", mapping = "maps/latin_qwerty.json", candidate = "direct_default", display = "hidden", subtype = "subtype_en_direct", shift = true)
        ))

    val jaJP = pack("language.ja-JP", "ja-JP", mapOf("en-US" to "Japanese", "zh-CN" to "Japanese", "ja-JP" to "Japanese"), Script.HIRA, BuiltInSchemas.ROMAJI,
        listOf(Script.HIRA to BuiltInSchemas.ROMAJI, Script.KANA to BuiltInSchemas.ROMAJI, Script.LATN to BuiltInSchemas.LATIN_DIRECT), listOf(
            capability("language.ja-JP", "ja-JP", Script.HIRA, BuiltInSchemas.ROMAJI, "transliteration", "qwerty", fsm = "rules/romaji_hira.fsm.json", candidate = "japanese_kana_default", display = "show_composing", subtype = "subtype_ja_hira"),
            capability("language.ja-JP", "ja-JP", Script.KANA, BuiltInSchemas.ROMAJI, "transliteration", "qwerty", fsm = "rules/romaji_kata.fsm.json", candidate = "japanese_kana_default", display = "show_composing", subtype = "subtype_ja_kana"),
            capability("language.ja-JP", "ja-JP", Script.LATN, BuiltInSchemas.LATIN_DIRECT, "direct", "qwerty", mapping = "maps/latin_qwerty.json", candidate = "direct_default", display = "hidden", subtype = "subtype_ja_latin", shift = true)
        ))

    val all: List<LanguagePackManifest> = listOf(zhCN, enUS, jaJP)

    fun resourceCatalog(readAsset: (String) -> ByteArray?): ResolvedResourceCatalog {
        val resources = all.flatMap(::references).distinct().associate { ref ->
            val bytes = requireNotNull(readAsset(ref.path)) { "Built-in resource ${ref.path} is missing" }
            ResolvedResourceKey(ref.packageId, versionFor(ref.packageId), normalizeResourcePath(ref.path), ref.kind, sha256(bytes)) to bytes
        }
        return ResolvedResourceCatalog(resources.keys, resources::get)
    }

    fun registerDictionaries(
        registry: DictionaryRegistryImpl,
        readAsset: (String) -> ByteArray?
    ) {
        all.flatMap { it.capabilities }.forEach { capability ->
            capability.dictionaries.forEach { binding ->
                val path = normalizeResourcePath(binding.resource.path)
                registry.register(
                    DictionaryKey(binding.resource.packageId, capability.id.locale, capability.id.script, capability.id.schema, path),
                    BuiltInDictionary(path, requireNotNull(readAsset(path)) { "Built-in dictionary $path is missing" })
                )
            }
        }
    }

    private fun references(manifest: LanguagePackManifest) = buildList {
        add(manifest.defaults.layout)
        manifest.capabilities.forEach { capability ->
            add(capability.layout); capability.engine.encoderConfig?.let(::add); capability.mapping?.let(::add); capability.fsm?.let(::add)
            addAll(capability.dictionaries.map(DictionaryBinding::resource))
        }
    }

    private fun versionFor(packageId: String) = all.firstOrNull { it.identity.packageId == packageId }?.identity?.version ?: packageVersion
    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun pack(id: String, locale: String, display: Map<String, String>, defaultScript: Script, defaultSchema: Schema, scripts: List<Pair<Script, Schema>>, capabilities: List<LanguageCapability>) = LanguagePackManifest(1, PackageIdentity(id, packageVersion), packageVersion, LocaleTag(locale), display, LocaleDefaults(defaultScript, defaultSchema, layout("qwerty")), scripts.map { ScriptManifest(it.first, requireNotNull(ScriptCatalog[it.first]), it.second) }, capabilities = capabilities)
    private fun capability(packageId: String, locale: String, script: Script, schema: Schema, engine: String, layout: String, encoder: String? = null, encoderConfig: String? = null, dictionary: Boolean = false, mapping: String? = null, fsm: String? = null, candidate: String, display: String, subtype: String, shift: Boolean = false) = LanguageCapability(CapabilityId(packageId, LocaleTag(locale), script, schema), EngineBinding(engine, encoder, encoderConfig?.let { resource(packageId, it, ResourceKind.ENCODER_CONFIG) }), layout(layout), if (dictionary) listOf(DictionaryBinding(DictionaryKind.WORD, DictionaryRole.PRIMARY, resource(packageId, "dicts/pinyin_main.dict", ResourceKind.DICTIONARY), true)) else emptyList(), mapping?.let { resource(packageId, it, ResourceKind.MAPPING) }, fsm?.let { resource(packageId, it, ResourceKind.FSM) }, candidate, display, shift, subtype = SubtypeInfo(subtype))
    private fun layout(id: String) = resource(BUILTIN, "layouts/$id.jsonc", ResourceKind.LAYOUT)
    private fun resource(packageId: String, path: String, kind: ResourceKind) = ResourceRef(packageId, path, kind)

    private class BuiltInDictionary(
        override val dictionaryId: String,
        bytes: ByteArray
    ) : Dictionary {
        private val entries = bytes.decodeToString()
            .lineSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith('#') }
            .mapNotNull { line ->
                line.split(Regex("\\s+"), limit = 2)
                    .takeIf { it.size == 2 }
                    ?.let { (reading, text) -> reading to text }
            }
            .toList()

        override suspend fun lookup(query: String, limit: Int): List<Candidate> = entries
            .asSequence()
            .filter { (reading, _) -> reading.startsWith(query, ignoreCase = true) }
            .take(limit)
            .map { (_, text) -> Candidate(text) }
            .toList()
    }
}
