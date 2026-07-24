package xyz.xiao6.myboard.pack

import kotlinx.serialization.Serializable
import xyz.xiao6.myboard.contract.engine.DictionaryBinding
import xyz.xiao6.myboard.contract.engine.DictionaryKind
import xyz.xiao6.myboard.contract.engine.DictionaryRole
import xyz.xiao6.myboard.contract.engine.EngineBinding
import xyz.xiao6.myboard.contract.language.PackageDependency
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.language.VersionRange
import xyz.xiao6.myboard.contract.manifest.CapabilityId
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.LayoutMirrorPolicy
import xyz.xiao6.myboard.contract.manifest.LocaleDefaults
import xyz.xiao6.myboard.contract.manifest.ScriptDescriptor
import xyz.xiao6.myboard.contract.manifest.ScriptManifest
import xyz.xiao6.myboard.contract.manifest.SubtypeInfo
import xyz.xiao6.myboard.contract.manifest.TextDirection
import xyz.xiao6.myboard.contract.registry.MissingResourcePolicy
import xyz.xiao6.myboard.contract.registry.ResourceKind
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.Script

@Serializable
internal data class LanguagePackManifestDto(
    val manifestVersion: Int,
    val identity: PackageIdentityDto,
    val minAppVersion: String,
    val locale: String,
    val displayName: Map<String, String>,
    val defaults: LocaleDefaultsDto,
    val scripts: List<ScriptManifestDto>,
    val dependencies: List<PackageDependencyDto> = emptyList(),
    val capabilities: List<LanguageCapabilityDto>
) {
    fun toDomain() = LanguagePackManifest(
        manifestVersion = manifestVersion,
        identity = identity.toDomain(),
        minAppVersion = parseSemVer(minAppVersion),
        locale = LocaleTag(locale),
        displayName = displayName,
        defaults = defaults.toDomain(),
        scripts = scripts.map(ScriptManifestDto::toDomain),
        dependencies = dependencies.map(PackageDependencyDto::toDomain),
        capabilities = capabilities.map(LanguageCapabilityDto::toDomain)
    )
}

@Serializable
internal data class PackageIdentityDto(val packageId: String, val version: String) {
    fun toDomain() = PackageIdentity(packageId, parseSemVer(version))
}

@Serializable
internal data class VersionRangeDto(
    val minimum: String? = null,
    val maximumExclusive: String? = null
) {
    fun toDomain() = VersionRange(minimum?.let(::parseSemVer), maximumExclusive?.let(::parseSemVer))
}

@Serializable
internal data class PackageDependencyDto(
    val packageId: String,
    val versionRange: VersionRangeDto,
    val optional: Boolean = false
) {
    fun toDomain() = PackageDependency(packageId, versionRange.toDomain(), optional)
}

@Serializable
internal data class LocaleDefaultsDto(
    val script: String,
    val schema: String,
    val layout: ResourceRefDto
) {
    fun toDomain() = LocaleDefaults(parseScript(script), Schema(schema), layout.toDomain())
}

@Serializable
internal data class ScriptManifestDto(
    val id: String,
    val descriptor: ScriptDescriptorDto,
    val defaultSchema: String
) {
    fun toDomain() = ScriptManifest(parseScript(id), descriptor.toDomain(), Schema(defaultSchema))
}

@Serializable
internal data class ScriptDescriptorDto(
    val script: String,
    val displayNames: Map<String, String>,
    val direction: TextDirection,
    val layoutMirror: LayoutMirrorPolicy,
    val preferredFont: String? = null
) {
    fun toDomain() = ScriptDescriptor(
        parseScript(script), displayNames, direction, layoutMirror, preferredFont
    )
}

@Serializable
internal data class LanguageCapabilityDto(
    val id: CapabilityIdDto,
    val engine: EngineBindingDto,
    val layout: ResourceRefDto,
    val dictionaries: List<DictionaryBindingDto>,
    val mapping: ResourceRefDto? = null,
    val fsm: ResourceRefDto? = null,
    val candidatePolicyId: String,
    val displayPolicyId: String? = null,
    val supportsShift: Boolean,
    val outputScript: String? = null,
    val subtype: SubtypeInfoDto? = null,
    val fallbackCapabilityIds: List<CapabilityIdDto> = emptyList()
) {
    fun toDomain() = LanguageCapability(
        id = id.toDomain(),
        engine = engine.toDomain(),
        layout = layout.toDomain(),
        dictionaries = dictionaries.map(DictionaryBindingDto::toDomain),
        mapping = mapping?.toDomain(),
        fsm = fsm?.toDomain(),
        candidatePolicyId = candidatePolicyId,
        displayPolicyId = displayPolicyId,
        supportsShift = supportsShift,
        outputScript = outputScript?.let(::parseScript),
        subtype = subtype?.toDomain(),
        fallbackCapabilityIds = fallbackCapabilityIds.map(CapabilityIdDto::toDomain)
    )
}

@Serializable
internal data class CapabilityIdDto(
    val packageId: String,
    val locale: String,
    val script: String,
    val schema: String
) {
    fun toDomain() = CapabilityId(packageId, LocaleTag(locale), parseScript(script), Schema(schema))
}

@Serializable
internal data class EngineBindingDto(
    val engineId: String,
    val encoderId: String? = null,
    val encoderConfig: ResourceRefDto? = null
) {
    fun toDomain() = EngineBinding(engineId, encoderId, encoderConfig?.toDomain())
}

@Serializable
internal data class DictionaryBindingDto(
    val kind: DictionaryKind,
    val role: DictionaryRole,
    val resource: ResourceRefDto,
    val required: Boolean
) {
    fun toDomain() = DictionaryBinding(kind, role, resource.toDomain(), required)
}

@Serializable
internal data class ResourceRefDto(
    val packageId: String,
    val path: String,
    val kind: ResourceKind,
    val versionRange: VersionRangeDto? = null,
    val sha256: String? = null,
    val onMissing: MissingResourcePolicy = MissingResourcePolicy.REJECT_PACKAGE
) {
    fun toDomain() = ResourceRef(packageId, path, kind, versionRange?.toDomain(), sha256, onMissing)
}

@Serializable
internal data class SubtypeInfoDto(val labelKey: String) {
    fun toDomain() = SubtypeInfo(labelKey)
}

internal fun parseSemVer(value: String): SemVer {
    require(SEMVER.matches(value)) { "Invalid semantic version '$value'" }
    val (major, minor, patch) = value.split('.').map(String::toInt)
    return SemVer(major, minor, patch)
}

private fun parseScript(value: String): Script =
    requireNotNull(Script.parse(value)) { "Invalid Script identifier '$value'" }

private val SEMVER = Regex("(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)\\.(?:0|[1-9]\\d*)")
