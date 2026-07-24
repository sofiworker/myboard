package xyz.xiao6.myboard.engine

import xyz.xiao6.myboard.contract.engine.EngineResources
import xyz.xiao6.myboard.contract.manifest.LanguageCapability
import xyz.xiao6.myboard.contract.manifest.ResolvedResourceKey
import xyz.xiao6.myboard.contract.registry.ResourceRef

interface EngineResourceResolver {
    fun resolve(capability: LanguageCapability): CapabilityResourceResolution
    fun resolveResource(reference: ResourceRef, availableResources: Collection<ResolvedResourceKey>): ResourceResolution
}

class ResolvedResourceCatalog(
    resources: Collection<ResolvedResourceKey>,
    private val readBytes: (ResolvedResourceKey) -> ByteArray? = { null }
) {
    private val snapshot = resources.distinct().toList()

    fun snapshot(): List<ResolvedResourceKey> = snapshot

    fun read(key: ResolvedResourceKey): ByteArray? =
        key.takeIf(snapshot::contains)?.let(readBytes)

    companion object {
        fun combine(vararg catalogs: ResolvedResourceCatalog): ResolvedResourceCatalog {
            val resources: Map<ResolvedResourceKey, ByteArray?> = catalogs
                .flatMap { catalog -> catalog.snapshot() }
                .distinct()
                .associateWith { key ->
                    catalogs.firstNotNullOfOrNull { catalog -> catalog.read(key) }
                }
            return ResolvedResourceCatalog(resources.keys) { key -> resources[key]?.copyOf() }
        }
    }
}

sealed interface ResourceResolution {
    data class Resolved(val key: ResolvedResourceKey) : ResourceResolution
    data class RejectedPackage(val reason: String) : ResourceResolution
    data class CapabilityDisabled(val reason: String) : ResourceResolution
    data class CapabilityFallbackRequired(val reason: String) : ResourceResolution
}

sealed interface CapabilityResourceResolution {
    data class Resolved(val resources: EngineResources) : CapabilityResourceResolution
    data class RejectedPackage(val reason: String) : CapabilityResourceResolution
    data class CapabilityDisabled(val reason: String) : CapabilityResourceResolution
    data class CapabilityFallbackRequired(val reason: String) : CapabilityResourceResolution
}
