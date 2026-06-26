package xyz.xiao6.myboard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 全局设置 ViewModel。
 * 为 SettingsScreen 提供设置数据和更新操作。
 */
class SettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    data class UiState(
        val settings: Map<String, String> = emptyMap(),
        val isLoading: Boolean = true
    )

    val uiState: StateFlow<UiState> = repo.settings
        .map { UiState(settings = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch { repo.updateSetting(key, value) }
    }

    fun getString(key: String, default: String = ""): String {
        return uiState.value.settings[key] ?: default
    }

    fun getInt(key: String, default: Int = 0): Int {
        return uiState.value.settings[key]?.toIntOrNull() ?: default
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean {
        return uiState.value.settings[key]?.toBooleanStrictOrNull() ?: default
    }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(repo) as T
        }
    }
}
