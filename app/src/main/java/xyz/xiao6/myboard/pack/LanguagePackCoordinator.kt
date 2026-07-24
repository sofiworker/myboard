package xyz.xiao6.myboard.pack

import java.io.InputStream
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.contract.language.PackageIdentity
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.state.OrthogonalState
import xyz.xiao6.myboard.data.repository.LanguagePackPreferences

interface LanguagePackPreferencesStore {
    suspend fun getLanguagePackPreferences(): LanguagePackPreferences
    suspend fun updateLanguagePackPreferences(preferences: LanguagePackPreferences)
}

data class InstalledLanguagePack(
    val identity: PackageIdentity,
    val displayName: Map<String, String>,
    val enabled: Boolean
)

data class LanguagePackManagementState(
    val installed: List<InstalledLanguagePack> = emptyList(),
    val preferences: LanguagePackPreferences = LanguagePackPreferences(),
    val isWorking: Boolean = false,
    val message: String? = null
)

class LanguagePackCoordinator(
    private val packageStore: PackageStore,
    private val preferencesStore: LanguagePackPreferencesStore,
    private val importer: LanguagePackImporter = TransactionalLanguagePackImporter(packageStore),
    private val manifestDecoder: LanguagePackManifestDecoder = JsonLanguagePackManifestDecoder(),
    private val builtInManifests: List<LanguagePackManifest> = BuiltInLanguagePacks.all,
    private val activeStateSource: ActiveOrthogonalStateSource = ActiveOrthogonalStateSource { null },
    private val documentSource: LanguagePackDocumentSource? = null
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow(LanguagePackManagementState())
    val state: StateFlow<LanguagePackManagementState> = _state.asStateFlow()

    suspend fun refresh() = mutex.withLock { refreshLocked() }

    suspend fun import(input: InputStream): PackageOperationResult = operation {
        withContext(Dispatchers.IO) {
            input.use { importer.import(it, manifestDecoder) }
        }
    }

    suspend fun import(uri: Uri): PackageOperationResult = operation {
        withContext(Dispatchers.IO) {
            val source = requireNotNull(documentSource) { "Language pack document source is unavailable" }
            val input = requireNotNull(source.open(uri)) { "Selected language pack could not be opened" }
            input.use { importer.import(it, manifestDecoder) }
        }
    }

    suspend fun enable(packageId: String): PackageOperationResult = preferenceOperation { current ->
        current.copy(enabledPackageIds = current.enabledPackageIds + packageId)
    }

    suspend fun disable(packageId: String): PackageOperationResult = preferenceOperation { current ->
        current.copy(
            enabledPackageIds = current.enabledPackageIds - packageId,
            providerPreferences = current.providerPreferences.filterValues { it != packageId }
        )
    }

    suspend fun selectProvider(state: OrthogonalState, packageId: String): PackageOperationResult =
        preferenceOperation { current ->
            current.copy(providerPreferences = current.providerPreferences + (state to packageId))
        }

    suspend fun uninstall(packageId: String): PackageOperationResult = mutex.withLock {
        setWorking(true)
        val checkpoint = packageStore.checkpoint()
        val previous = preferencesStore.getLanguagePackPreferences()
        val proposed = previous.copy(
            enabledPackageIds = previous.enabledPackageIds - packageId,
            providerPreferences = previous.providerPreferences.filterValues { it != packageId }
        )
        preflight(proposed)?.let { return@withLock finish(it) }
        val uninstall = packageStore.uninstall(packageId)
        if (!uninstall.isSuccess) return@withLock finish(uninstall)
        val result = runCatching { preferencesStore.updateLanguagePackPreferences(proposed) }
            .fold(
                onSuccess = { uninstall },
                onFailure = { error ->
                    val restored = packageStore.restore(checkpoint)
                    PackageOperationResult.Failure(
                        listOfNotNull(
                            error.message ?: "Language pack preferences could not be saved",
                            (restored as? PackageOperationResult.Failure)?.errors?.joinToString()
                        )
                    )
                }
            )
        finish(result)
    }

    private suspend fun preferenceOperation(
        update: (LanguagePackPreferences) -> LanguagePackPreferences
    ): PackageOperationResult = mutex.withLock {
        setWorking(true)
        val proposed = update(preferencesStore.getLanguagePackPreferences())
        preflight(proposed)?.let { return@withLock finish(it) }
        val result = runCatching { preferencesStore.updateLanguagePackPreferences(proposed) }
            .fold(
                onSuccess = { PackageOperationResult.Success(PackageIdentity("preferences", xyz.xiao6.myboard.contract.language.SemVer(0, 0, 0))) },
                onFailure = { PackageOperationResult.Failure(listOf(it.message ?: "Language pack preferences could not be saved")) }
            )
        finish(result)
    }

    private suspend fun operation(block: suspend () -> PackageOperationResult): PackageOperationResult = mutex.withLock {
        setWorking(true)
        finish(block())
    }

    private fun preflight(preferences: LanguagePackPreferences): PackageOperationResult.Failure? {
        val effective = buildEffectiveRegistrySnapshot(
            manifests = builtInManifests + packageStore.installedManifests(),
            builtInPackageIds = builtInManifests.map { it.identity.packageId }.toSet(),
            enabledExternalPackageIds = preferences.enabledPackageIds
        )
        val errors = effective.errors.toMutableList()
        preferences.providerPreferences.forEach { (state, packageId) ->
            if (effective.snapshot.capabilitiesByState[state].orEmpty().none { it.id.packageId == packageId }) {
                errors += "Preferred provider '$packageId' is unavailable for $state"
            }
        }
        activeStateSource.currentState()?.let { active ->
            if (effective.snapshot.capabilitiesByState[active].isNullOrEmpty()) {
                errors += "No enabled provider remains for active state $active"
            }
        }
        return errors.takeIf(List<String>::isNotEmpty)?.let(PackageOperationResult::Failure)
    }

    private suspend fun refreshLocked() {
        val preferences = preferencesStore.getLanguagePackPreferences()
        _state.value = LanguagePackManagementState(
            installed = packageStore.installedManifests()
                .sortedBy { it.identity.packageId }
                .map { InstalledLanguagePack(it.identity, it.displayName, it.identity.packageId in preferences.enabledPackageIds) },
            preferences = preferences
        )
    }

    private fun setWorking(working: Boolean) {
        _state.value = _state.value.copy(isWorking = working, message = null)
    }

    private suspend fun finish(result: PackageOperationResult): PackageOperationResult {
        refreshLocked()
        _state.value = _state.value.copy(
            isWorking = false,
            message = when (result) {
                is PackageOperationResult.Success -> result.warnings.joinToString().ifBlank { null }
                is PackageOperationResult.Failure -> result.errors.joinToString()
            }
        )
        return result
    }
}
