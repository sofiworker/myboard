package xyz.xiao6.myboard.contract.manifest

import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.engine.DictionaryBinding
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.language.LocalizedText
import xyz.xiao6.myboard.contract.language.PackageDependency
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.registry.ManifestValidationError
import xyz.xiao6.myboard.contract.registry.ManifestValidationResult
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.requireSha256

/**
 * Immutable physical identity obtained after resolving a logical [ResourceRef].
 * The path is constrained to a normalized, package-relative POSIX path so it
 * can never identify a file outside the installed package.
 */
data class ResolvedResourceKey(
    val packageId: String,
    val packageVersion: SemVer,
    val normalizedPath: String,
    val kind: ResourceKind,
    val sha256: String
) {
    init {
        require(packageId.isNotBlank()) { "Resolved resource package ID must not be blank" }
        requireSha256(sha256)
        require(normalizedPath == normalizeResourcePath(normalizedPath)) {
            "Resolved resource path must be normalized and package-relative"
        }
    }
}

fun normalizeResourcePath(path: String): String {
    require(path.isNotBlank() && !path.startsWith('/') && '\\' !in path) {
        "Resource path must be a non-blank package-relative POSIX path"
    }
    val segments = path.split('/')
    require(segments.all { it.isNotBlank() && it != "." && it != ".." }) {
        "Resource path must not contain empty, current-directory, or parent-directory segments"
    }
    return segments.joinToString("/")
}

/**
 * Script 的展示与排版元数据。
 *
 * Language Pack 声明是运行时权威来源；[ScriptCatalog] 仅提供内置 Script 的默认展示信息。
 */
data class ScriptDescriptor(
    val script: Script,
    val displayNames: Map<String, String>,
    val direction: TextDirection,
    val layoutMirror: LayoutMirrorPolicy,
    val preferredFont: String? = null
)

enum class TextDirection { LTR, RTL }

enum class LayoutMirrorPolicy { NONE, MIRROR_HORIZONTAL }

/**
 * 常用 Script 的本地目录，不负责决定 Script 是否可由某个 Language Pack 使用。
 */
object ScriptCatalog {
    private val descriptors = listOf(
        descriptor(Script.LATN, "Latin", "拉丁字母", "ラテン文字"),
        descriptor(Script.HANI, "Han", "汉字", "漢字"),
        descriptor(Script.HIRA, "Hiragana", "平假名", "ひらがな"),
        descriptor(Script.KANA, "Kana", "假名", "仮名"),
        descriptor(Script.HANG, "Hangul", "谚文", "ハングル"),
        descriptor(
            script = Script.ARAB,
            englishName = "Arabic",
            chineseName = "阿拉伯字母",
            japaneseName = "アラビア文字",
            direction = TextDirection.RTL,
            layoutMirror = LayoutMirrorPolicy.MIRROR_HORIZONTAL
        ),
        descriptor(Script.THAI, "Thai", "泰文", "タイ文字"),
        descriptor(Script.DEVA, "Devanagari", "天城文", "デーヴァナーガリー文字")
    ).associateBy(ScriptDescriptor::script)

    operator fun get(script: Script): ScriptDescriptor? = descriptors[script]

    private fun descriptor(
        script: Script,
        englishName: String,
        chineseName: String,
        japaneseName: String,
        direction: TextDirection = TextDirection.LTR,
        layoutMirror: LayoutMirrorPolicy = LayoutMirrorPolicy.NONE
    ) = ScriptDescriptor(
        script = script,
        displayNames = mapOf(
            "en-US" to englishName,
            "zh-CN" to chineseName,
            "ja-JP" to japaneseName
        ),
        direction = direction,
        layoutMirror = layoutMirror
    )
}

/**
 * 能力唯一标识。
 * CapabilityId = packageId + locale + script + schema
 */
data class CapabilityId(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
)

data class LanguageCapability(
    val id: CapabilityId,
    val engine: EngineBinding,
    val layout: ResourceRef,
    val dictionaries: List<DictionaryBinding>,
    val mapping: ResourceRef? = null,
    val fsm: ResourceRef? = null,
    val candidatePolicyId: String,
    val displayPolicyId: String? = null,
    val supportsShift: Boolean,
    val outputScript: Script? = null,
    val subtype: SubtypeInfo? = null,
    val fallbackCapabilityIds: List<CapabilityId> = emptyList()
)

data class LanguagePackManifest(
    val manifestVersion: Int,
    val identity: PackageIdentity,
    val minAppVersion: SemVer,
    val locale: LocaleTag,
    val displayName: LocalizedText,
    val defaults: LocaleDefaults,
    val scripts: List<ScriptManifest>,
    val dependencies: List<PackageDependency> = emptyList(),
    val capabilities: List<LanguageCapability>
)

/**
 * 语言能力（Locale 级别）。
 */
/**
 * Locale 默认值。
 */
data class LocaleDefaults(
    val script: Script,
    val schema: Schema,
    val layout: ResourceRef
)

/**
 * 文字系统能力（Script 级别）。
 */
/**
 * 输入方案能力（Schema 级别）。
 * 状态层、引擎层、布局层共同依赖的能力契约。
 */
/**
 * Android Subtype 导出信息。
 */
data class SubtypeInfo(
    val labelKey: String
)

/**
 * 语言包 Manifest 数据模型。
 */
/**
 * Manifest 中 Script 级别的声明。
 */
data class ScriptManifest(
    val id: Script,
    val descriptor: ScriptDescriptor,
    val defaultSchema: Schema
)

fun LanguagePackManifest.validate(): ManifestValidationResult {
    val errors = buildList {
        scripts.groupBy(ScriptManifest::id)
            .filterValues { it.size > 1 }
            .keys
            .forEach { add(ManifestValidationError.DuplicateScript(it)) }

        scripts.filter { it.id != it.descriptor.script }
            .forEach { add(ManifestValidationError.ScriptDescriptorMismatch(it.id, it.descriptor.script)) }

        capabilities.groupBy(LanguageCapability::id)
            .filterValues { it.size > 1 }
            .keys
            .forEach { add(ManifestValidationError.DuplicateCapabilityId(it)) }

        val scriptIds = scripts.map(ScriptManifest::id).toSet()
        capabilities.filter {
            it.id.packageId != identity.packageId || it.id.locale != locale || it.id.script !in scriptIds
        }.forEach { add(ManifestValidationError.CapabilityOutsideManifest(it.id)) }

        val capabilityIds = capabilities.map(LanguageCapability::id).toSet()
        buildList {
            add(defaults.script to defaults.schema)
            scripts.forEach { add(it.id to it.defaultSchema) }
        }.distinct().filter { (script, schema) ->
            CapabilityId(identity.packageId, locale, script, schema) !in capabilityIds
        }.forEach { (script, schema) ->
            add(ManifestValidationError.MissingDefaultCapability(script, schema))
        }

        checkResource("defaults.layout", defaults.layout)
        checkResourceKind("defaults.layout", defaults.layout, setOf(ResourceKind.LAYOUT))
        capabilities.forEach { capability ->
            val resources = buildList {
                add(capability.layout)
                capability.engine.encoderConfig?.let(::add)
                capability.mapping?.let(::add)
                capability.fsm?.let(::add)
                addAll(capability.dictionaries.map(DictionaryBinding::resource))
            }
            resources.forEachIndexed { index, resource ->
                checkResource("capabilities[${capability.id}].resources[$index]", resource)
            }
            checkResourceKind("capabilities[${capability.id}].layout", capability.layout, setOf(ResourceKind.LAYOUT))
            capability.engine.encoderConfig?.let { checkResourceKind("encoderConfig", it, setOf(ResourceKind.ENCODER_CONFIG)) }
            capability.mapping?.let { checkResourceKind("mapping", it, setOf(ResourceKind.MAPPING)) }
            capability.fsm?.let { checkResourceKind("fsm", it, setOf(ResourceKind.FSM)) }
            if (resources.any { it.onMissing == xyz.xiao6.myboard.contract.registry.MissingResourcePolicy.USE_CAPABILITY_FALLBACK } &&
                capability.fallbackCapabilityIds.none(::isValidFallbackId)
            ) {
                add(ManifestValidationError.MissingFallbackCapability(capability.id))
            }
            capability.dictionaries.forEachIndexed { index, binding ->
                checkResourceKind("dictionaries[$index]", binding.resource, binding.resourceKinds())
                if (!binding.isCompatible()) {
                    add(
                        ManifestValidationError.IncompatibleDictionaryBinding(
                            capabilityId = capability.id,
                            kind = binding.kind,
                            role = binding.role
                        )
                    )
                }
            }
            capability.fallbackCapabilityIds.filter { !isValidFallbackId(it) || it == capability.id || hasFallbackCycle(capability.id, it, capabilities) }
                .forEach { add(ManifestValidationError.InvalidFallbackCapability(capability.id, it)) }
        }
    }
    return ManifestValidationResult(errors)
}

private fun isValidFallbackId(id: CapabilityId): Boolean =
    id.packageId.isNotBlank() &&
        id.locale.value.isNotBlank() &&
        id.script.value.isNotBlank() &&
        id.schema.value.isNotBlank()

private fun DictionaryBinding.resourceKinds(): Set<ResourceKind> = when (kind) {
    xyz.xiao6.myboard.contract.engine.DictionaryKind.WORD,
    xyz.xiao6.myboard.contract.engine.DictionaryKind.PHRASE,
    xyz.xiao6.myboard.contract.engine.DictionaryKind.CONVERSION,
    xyz.xiao6.myboard.contract.engine.DictionaryKind.FREQUENCY,
    xyz.xiao6.myboard.contract.engine.DictionaryKind.SPELLING,
    xyz.xiao6.myboard.contract.engine.DictionaryKind.EMOJI -> setOf(ResourceKind.DICTIONARY)
}

private fun hasFallbackCycle(source: CapabilityId, target: CapabilityId, capabilities: List<LanguageCapability>): Boolean {
    val byId = capabilities.associateBy(LanguageCapability::id)
    fun reachesSource(current: CapabilityId, visited: Set<CapabilityId>): Boolean {
        if (current == source) return true
        return current !in visited && byId[current]?.fallbackCapabilityIds.orEmpty().any { reachesSource(it, visited + current) }
    }
    return reachesSource(target, emptySet())
}

private fun MutableList<ManifestValidationError>.checkResourceKind(
    location: String,
    resource: ResourceRef,
    expected: Set<ResourceKind>
) {
    if (resource.kind !in expected) add(ManifestValidationError.UnexpectedResourceKind(location, expected, resource.kind))
}

private fun MutableList<ManifestValidationError>.checkResource(location: String, resource: ResourceRef) {
    if (resource.packageId.isBlank() || resource.path.isBlank()) {
        add(ManifestValidationError.InvalidResourceRef(location, resource))
    }
}
