package xyz.xiao6.myboard.pack

import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.state.RegistrySnapshot

data class EffectiveRegistryResult(
    val snapshot: RegistrySnapshot,
    val errors: List<String>
)

fun buildEffectiveRegistrySnapshot(
    manifests: Collection<LanguagePackManifest>,
    builtInPackageIds: Set<String>,
    enabledExternalPackageIds: Set<String>
): EffectiveRegistryResult {
    val installedById = manifests.associateBy { it.identity.packageId }
    val requestedIds = builtInPackageIds + enabledExternalPackageIds
    val effective = requestedIds.mapNotNull(installedById::get)
    val effectiveVersions = effective.associate { it.identity.packageId to it.identity.version }
    val errors = buildList {
        enabledExternalPackageIds.filterNot(installedById::containsKey).forEach {
            add("Enabled package '$it' is not installed")
        }
        effective.forEach { manifest ->
            manifest.dependencies.filterNot { it.optional }.forEach { dependency ->
                val version = effectiveVersions[dependency.packageId]
                if (version == null || version !in dependency.versionRange) {
                    add("Enabled package '${manifest.identity.packageId}' requires enabled dependency '${dependency.packageId}'")
                }
            }
        }
    }
    val capabilities = effective.flatMap(LanguagePackManifest::capabilities)
    return EffectiveRegistryResult(
        snapshot = RegistrySnapshot(
            providersByLocale = effective
                .groupBy(LanguagePackManifest::locale)
                .mapValues { (_, providers) -> providers.sortedBy { it.identity.packageId } },
            capabilitiesByState = capabilities
                .groupBy { OrthogonalState(it.id.locale, it.id.script, it.id.schema) }
                .mapValues { (_, providers) -> providers.sortedBy { it.id.packageId } },
            capabilitiesByPackage = capabilities
                .groupBy { it.id.packageId }
                .mapValues { (_, values) -> values.toList() }
        ),
        errors = errors.distinct()
    )
}
