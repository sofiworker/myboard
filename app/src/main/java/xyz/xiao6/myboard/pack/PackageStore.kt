package xyz.xiao6.myboard.pack

import java.security.MessageDigest
import java.text.Normalizer
import xyz.xiao6.myboard.contract.language.PackageDependency
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.manifest.validate
import xyz.xiao6.myboard.contract.registry.ResourceRef
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.state.RegistrySnapshot

/**
 * Transactional in-memory package store. Persisting its [Snapshot] is deliberately
 * outside this class so Android storage can use the same validated transaction.
 */
class PackageStore {

    private val versions = linkedMapOf<PackageIdentity, StoredPackage>()
    private val activeIds = linkedMapOf<String, PackageIdentity>()

    @Volatile
    private var current = Snapshot(emptyMap(), RegistrySnapshot())

    @Synchronized
    fun install(payload: PackagePayload): PackageOperationResult {
        val staged = runCatching { payload.copy(resources = payload.resources.copyNormalized()) }
            .getOrElse { error ->
                return PackageOperationResult.Failure(
                    listOf(error.message ?: "Package resources could not be normalized")
                )
            }
        val validationErrors = staged.validateForActivation()
        if (validationErrors.isNotEmpty()) return PackageOperationResult.Failure(validationErrors)

        val active = activeIds[staged.manifest.identity.packageId]
        if (active != null && staged.manifest.identity.version <= active.version) {
            return PackageOperationResult.Failure(
                listOf("Package '${active.packageId}' version ${active.version} is already active")
            )
        }

        val dependencyResult = validateDependencies(staged.manifest.dependencies, staged.manifest.identity)
        if (dependencyResult.errors.isNotEmpty()) return PackageOperationResult.Failure(dependencyResult.errors)
        if (introducesRequiredDependencyCycle(staged.manifest)) {
            return PackageOperationResult.Failure(
                listOf("Package '${staged.manifest.identity.packageId}' introduces a required dependency cycle")
            )
        }

        val identity = staged.manifest.identity
        // Staging never mutates the published pointer. Only this point can activate it.
        versions[identity] = StoredPackage(staged, PackageVersionState.ACTIVE)
        active?.let { previous -> versions[previous]?.state = PackageVersionState.DEACTIVATING }
        activeIds[identity.packageId] = identity
        publish()
        active?.let(::removeWhenUnleased)
        return PackageOperationResult.Success(identity, dependencyResult.warnings)
    }

    @Synchronized
    fun uninstall(packageId: String): PackageOperationResult {
        val active = activeIds[packageId]
            ?: return PackageOperationResult.Failure(listOf("Package '$packageId' is not active"))
        val dependents = activeIds.values
            .asSequence()
            .filter { it.packageId != packageId }
            .mapNotNull { versions[it]?.payload?.manifest }
            .filter { manifest -> manifest.dependencies.any { !it.optional && it.packageId == packageId && active.version in it.versionRange } }
            .map { it.identity.packageId }
            .toList()
        if (dependents.isNotEmpty()) {
            return PackageOperationResult.Failure(listOf("Package '$packageId' is required by ${dependents.joinToString()}"))
        }

        activeIds.remove(packageId)
        versions[active]?.state = PackageVersionState.DEACTIVATING
        publish()
        removeWhenUnleased(active)
        return PackageOperationResult.Success(active)
    }

    /**
     * Leases are available only from the active version. A deactivating version
     * remains physically retained for existing handles, but cannot create new ones.
     */
    @Synchronized
    fun <T> acquireResource(
        packageId: String,
        path: String,
        decode: (ByteArray) -> T
    ): ResourceHandle<T>? {
        val identity = activeIds[packageId] ?: return null
        val stored = versions[identity] ?: return null
        if (stored.state != PackageVersionState.ACTIVE) return null
        val normalizedPath = normalizePath(path) ?: return null
        val resource = stored.payload.resources[normalizedPath] ?: return null
        val value = decode(resource.copyOf())
        stored.leases += 1
        return ResourceHandle(value) {
            release(identity)
        }
    }

    fun snapshot(): Snapshot = current

    @Synchronized
    fun hasRetainedVersion(packageId: String, version: SemVer): Boolean =
        PackageIdentity(packageId, version) in versions

    @Synchronized
    private fun release(identity: PackageIdentity) {
        val stored = versions[identity] ?: return
        check(stored.leases > 0) { "Package lease underflow for $identity" }
        stored.leases -= 1
        removeWhenUnleased(identity)
    }

    private fun removeWhenUnleased(identity: PackageIdentity) {
        val stored = versions[identity] ?: return
        if (stored.state == PackageVersionState.DEACTIVATING && stored.leases == 0) {
            versions.remove(identity)
        }
    }

    private fun validateDependencies(
        dependencies: List<PackageDependency>,
        candidate: PackageIdentity
    ): DependencyValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        dependencies.forEach { dependency ->
            val resolved = activeIds[dependency.packageId]
            val available = resolved != null && resolved.version in dependency.versionRange
            if (!available) {
                val message = "Dependency '${dependency.packageId}' does not satisfy ${dependency.versionRange}"
                if (dependency.optional) warnings += message else errors += message
            }
            if (dependency.packageId == candidate.packageId && !dependency.optional) {
                errors += "Package '${candidate.packageId}' cannot require itself"
            }
        }
        return DependencyValidation(errors, warnings)
    }

    private fun introducesRequiredDependencyCycle(candidate: LanguagePackManifest): Boolean {
        val manifests = activeIds.values
            .filterNot { it.packageId == candidate.identity.packageId }
            .mapNotNull { versions[it]?.payload?.manifest }
            .plus(candidate)
            .associateBy { it.identity.packageId }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()

        fun hasCycle(packageId: String): Boolean {
            if (packageId in visiting) return true
            if (!visited.add(packageId)) return false
            visiting += packageId
            val cycle = manifests[packageId]?.dependencies.orEmpty()
                .asSequence()
                .filterNot(PackageDependency::optional)
                .map(PackageDependency::packageId)
                .filter(manifests::containsKey)
                .any(::hasCycle)
            visiting -= packageId
            return cycle
        }

        return manifests.keys.any(::hasCycle)
    }

    private fun PackagePayload.validateForActivation(): List<String> = buildList {
        manifest.validate().errors.forEach { add(it.toString()) }
        resources.keys.forEach { path ->
            if (normalizePath(path) == null) add("Invalid resource path '$path'")
        }
        referencedResources(manifest).forEach { reference ->
            val path = normalizePath(reference.path)
            val resource = path?.let(resources::get)
            if (resource == null) {
                add("Resource '${reference.path}' is missing")
            } else if (reference.sha256 != null && sha256(resource) != reference.sha256.lowercase()) {
                add("Resource '${reference.path}' SHA-256 does not match manifest")
            }
        }
    }

    private fun publish() {
        val manifests = activeIds.values.mapNotNull { versions[it]?.payload?.manifest }
        val capabilities = manifests.flatMap(LanguagePackManifest::capabilities)
        val registry = RegistrySnapshot(
            providersByLocale = manifests
                .groupBy(LanguagePackManifest::locale)
                .mapValues { (_, providers) -> providers.sortedBy { it.identity.packageId }.toList() },
            capabilitiesByState = capabilities
                .groupBy { OrthogonalState(it.id.locale, it.id.script, it.id.schema) }
                .mapValues { (_, values) -> values.sortedBy { it.id.packageId }.toList() },
            capabilitiesByPackage = capabilities
                .groupBy { it.id.packageId }
                .mapValues { (_, values) -> values.toList() }
        )
        current = Snapshot(
            active = activeIds.mapValues { (_, identity) -> PackageVersion(identity.packageId, identity.version) }.toMap(),
            registrySnapshot = registry
        )
    }

    private data class StoredPackage(
        val payload: PackagePayload,
        var state: PackageVersionState,
        var leases: Int = 0
    )

    private data class DependencyValidation(val errors: List<String>, val warnings: List<String>)
}

data class PackagePayload(
    val manifest: LanguagePackManifest,
    val resources: Map<String, ByteArray>
)

data class PackageVersion(val packageId: String, val version: SemVer)

data class Snapshot(
    val active: Map<String, PackageVersion>,
    val registrySnapshot: RegistrySnapshot
)

sealed interface PackageOperationResult {
    val isSuccess: Boolean
    val warnings: List<String>

    data class Success(
        val identity: PackageIdentity,
        override val warnings: List<String> = emptyList()
    ) : PackageOperationResult {
        override val isSuccess: Boolean = true
    }

    data class Failure(val errors: List<String>) : PackageOperationResult {
        override val isSuccess: Boolean = false
        override val warnings: List<String> = emptyList()
    }
}

class ResourceHandle<T> internal constructor(
    val value: T,
    private val onClose: () -> Unit
) : AutoCloseable {
    private var closed = false

    override fun close() {
        if (!closed) {
            closed = true
            onClose()
        }
    }
}

enum class PackageVersionState { ACTIVE, DEACTIVATING }

private fun Map<String, ByteArray>.copyNormalized(): Map<String, ByteArray> = buildMap {
    this@copyNormalized.forEach { (path, bytes) ->
        val normalized = normalizePath(path) ?: path
        require(put(normalized, bytes.copyOf()) == null) { "Duplicate resource path '$normalized'" }
    }
}

private fun referencedResources(manifest: LanguagePackManifest): List<ResourceRef> = buildList {
    add(manifest.defaults.layout)
    manifest.capabilities.forEach { capability ->
        add(capability.layout)
        capability.engine.encoderConfig?.let(::add)
        capability.mapping?.let(::add)
        capability.fsm?.let(::add)
        addAll(capability.dictionaries.map { it.resource })
    }
}.distinct()

private fun normalizePath(path: String): String? {
    val normalized = Normalizer.normalize(path, Normalizer.Form.NFC)
    if (normalized.isBlank() || normalized.startsWith('/') || '\\' in normalized) return null
    val segments = normalized.split('/')
    return normalized.takeIf { segments.all { it.isNotBlank() && it != "." && it != ".." } }
}

private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
    .digest(bytes)
    .joinToString("") { "%02x".format(it) }
