package xyz.xiao6.myboard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 工具栏设置 ViewModel。
 * 管理工具栏按钮的顺序、显隐和布局模式。
 */
class ToolbarSettingsViewModel(
    private val repo: SettingsRepository
) : ViewModel() {

    data class ToolbarItemDisplay(
        val type: ToolbarItemType,
        val enabled: Boolean,
        val sortOrder: Int,
        val isFixed: Boolean = false
    )

    data class UiState(
        val items: List<ToolbarItemDisplay> = emptyList(),
        val layoutMode: ToolbarLayoutMode = ToolbarLayoutMode.SCROLLABLE,
        val availableToAdd: List<ToolbarItemType> = emptyList()
    )

    val uiState: StateFlow<UiState> = combine(
        repo.toolbarItems,
        repo.toolbarLayoutMode
    ) { items, layoutMode ->
        val enabledTypeNames = items.filter { it.enabled }.map { it.type }.toSet()
        val available = ToolbarItemType.entries.filter { it.name !in enabledTypeNames }
        UiState(
            items = items.sortedBy { it.sortOrder }.map {
                ToolbarItemDisplay(
                    type = ToolbarItemType.valueOf(it.type),
                    enabled = it.enabled,
                    sortOrder = it.sortOrder
                )
            },
            layoutMode = layoutMode,
            availableToAdd = available
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun setLayoutMode(mode: ToolbarLayoutMode) {
        viewModelScope.launch { repo.updateToolbarLayoutMode(mode) }
    }

    fun toggleItem(type: ToolbarItemType, enabled: Boolean) {
        viewModelScope.launch {
            val current = repo.toolbarItems.first()
            if (enabled) {
                val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
                val newEntity = ToolbarItemEntity(
                    type = type.name,
                    enabled = true,
                    sortOrder = maxOrder + 1
                )
                repo.updateToolbarItems(current + newEntity)
            } else {
                repo.updateToolbarItems(current.filter { it.type != type.name })
            }
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = repo.toolbarItems.first().sortedBy { it.sortOrder }.toMutableList()
            if (fromIndex < 0 || fromIndex >= current.size) return@launch
            if (toIndex < 0 || toIndex >= current.size) return@launch
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val reordered = current.mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            }
            repo.updateToolbarItems(reordered)
        }
    }

    class Factory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolbarSettingsViewModel(repo) as T
        }
    }
}
