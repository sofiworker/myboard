package xyz.xiao6.myboard.core.contract

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

/**
 * 语言能力（Locale 级别）。
 */
data class LocaleCapability(
    val locale: LocaleTag,
    val displayName: Map<String, String>,
    val scripts: Map<Script, ScriptCapability>,
    val defaults: LocaleDefaults
)

/**
 * Locale 默认值。
 */
data class LocaleDefaults(
    val script: Script,
    val schema: Schema,
    val layoutId: String
)

/**
 * 文字系统能力（Script 级别）。
 */
data class ScriptCapability(
    val script: Script,
    val defaultSchema: Schema,
    val schemas: Map<Schema, SchemaCapability>
)

/**
 * 输入方案能力（Schema 级别）。
 * 状态层、引擎层、布局层共同依赖的能力契约。
 */
data class SchemaCapability(
    val engineId: String,
    val layoutId: String,
    val supportsShift: Boolean,
    val encoderId: String? = null,
    val encoderConfig: String? = null,
    val dictionary: String? = null,
    val dictionaryOptional: Boolean = false,
    val mapping: String? = null,
    val fsm: String? = null,
    val conversionDictionary: String? = null,
    val outputScript: Script? = null,
    val candidatePolicy: String,
    val displayPolicy: String? = null,
    val subtype: SubtypeInfo? = null
)

/**
 * Android Subtype 导出信息。
 */
data class SubtypeInfo(
    val labelKey: String
)

/**
 * 语言包 Manifest 数据模型。
 */
data class LanguageManifest(
    val manifestVersion: Int,
    val packageId: String,
    val packageVersion: String,
    val locale: LocaleTag,
    val displayName: Map<String, String>,
    val defaults: LocaleDefaults,
    val scripts: Map<Script, ScriptManifest>
)

/**
 * Manifest 中 Script 级别的声明。
 */
data class ScriptManifest(
    val defaultSchema: Schema,
    val schemas: Map<Schema, SchemaCapability>
)