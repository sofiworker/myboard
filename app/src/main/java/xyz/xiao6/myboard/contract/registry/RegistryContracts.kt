package xyz.xiao6.myboard.contract.registry

import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Script
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.layout.LayoutDoc
import xyz.xiao6.myboard.contract.layout.LayoutCanonicalId

data class ResourceRef(
    val packageId: String,
    val path: String,
    val kind: ResourceKind,
    val versionRange: xyz.xiao6.myboard.contract.language.VersionRange? = null,
    val sha256: String? = null,
    val onMissing: MissingResourcePolicy = MissingResourcePolicy.REJECT_PACKAGE
) {
    init {
        sha256?.let(::requireSha256)
    }
}

fun requireSha256(value: String) {
    require(value.matches(Regex("[0-9a-fA-F]{64}"))) { "SHA-256 must be exactly 64 hexadecimal characters" }
}

enum class ResourceKind {
    DICTIONARY,
    CONVERSION,
    FREQUENCY,
    SPELLING,
    EMOJI,
    MAPPING,
    FSM,
    ENCODER_CONFIG,
    LAYOUT,
    I18N,
    MODEL
}

fun ResourceRef.toLayoutCanonicalId(): LayoutCanonicalId {
    require(kind == ResourceKind.LAYOUT) { "Only layout resources have canonical layout IDs" }
    require(!path.startsWith('/') && '\\' !in path && path.split('/').all { it.isNotBlank() && it != "." && it != ".." }) {
        "Layout resource path must be package-relative and normalized"
    }
    val layoutId = when {
        path.endsWith(".jsonc") -> path.removeSuffix(".jsonc").substringAfterLast('/')
        path.endsWith(".json") -> path.removeSuffix(".json").substringAfterLast('/')
        else -> throw IllegalArgumentException("Layout resource path must end in .json or .jsonc")
    }
    return LayoutCanonicalId.of(packageId, layoutId)
}

enum class MissingResourcePolicy {
    REJECT_PACKAGE,
    DISABLE_CAPABILITY,
    USE_CAPABILITY_FALLBACK
}

data class ManifestValidationResult(
    val errors: List<ManifestValidationError>
) {
    val isValid: Boolean get() = errors.isEmpty()
}

sealed interface ManifestValidationError {
    data class DuplicateScript(val script: Script) : ManifestValidationError
    data class DuplicateCapabilityId(val id: xyz.xiao6.myboard.contract.manifest.CapabilityId) : ManifestValidationError
    data class ScriptDescriptorMismatch(
        val script: Script,
        val descriptorScript: Script
    ) : ManifestValidationError

    data class MissingDefaultCapability(
        val script: Script,
        val schema: Schema
    ) : ManifestValidationError

    data class CapabilityOutsideManifest(
        val id: xyz.xiao6.myboard.contract.manifest.CapabilityId
    ) : ManifestValidationError

    data class IncompatibleDictionaryBinding(
        val capabilityId: xyz.xiao6.myboard.contract.manifest.CapabilityId,
        val kind: xyz.xiao6.myboard.contract.engine.DictionaryKind,
        val role: xyz.xiao6.myboard.contract.engine.DictionaryRole
    ) : ManifestValidationError

    data class InvalidResourceRef(
        val location: String,
        val resource: ResourceRef
    ) : ManifestValidationError

    data class MissingFallbackCapability(
        val capabilityId: xyz.xiao6.myboard.contract.manifest.CapabilityId
    ) : ManifestValidationError

    data class UnexpectedResourceKind(
        val location: String,
        val expected: Set<ResourceKind>,
        val actual: ResourceKind
    ) : ManifestValidationError

    data class InvalidFallbackCapability(
        val capabilityId: xyz.xiao6.myboard.contract.manifest.CapabilityId,
        val target: xyz.xiao6.myboard.contract.manifest.CapabilityId
    ) : ManifestValidationError
}

/**
 * 注册结果。
 */
sealed interface RegisterResult {
    data class Success(val id: String) : RegisterResult
    data class Failed(val errors: List<String>) : RegisterResult
}

/**
 * 布局校验问题。
 */
data class LayoutIssue(
    val severity: IssueSeverity,
    val message: String,
    val path: String? = null
)

enum class IssueSeverity { ERROR, WARNING }

/**
 * 布局来源。
 */
enum class LayoutSource { BUILT_IN, LANGUAGE_PACK, USER }

/**
 * 字典加载键。
 */
data class DictionaryKey(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema,
    val path: String
)

/**
 * 导入结果。
 */
sealed interface ImportResult {
    data class Success(val packageId: String) : ImportResult
    data class PartialSuccess(val packageId: String, val warnings: List<String>) : ImportResult
    data class Failed(val errors: List<String>) : ImportResult
}
