package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 工具栏设置页面。
 * 支持按钮拖拽排序、显隐开关、布局模式切换。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolbarSettingsScreen(
    onBack: () -> Unit,
    viewModel: ToolbarSettingsViewModel = viewModel(
        factory = ToolbarSettingsViewModel.Factory(SettingsRepository(
            xyz.xiao6.myboard.data.db.SettingsDatabase.getInstance(
                androidx.compose.ui.platform.LocalContext.current
            ).settingsDao()
        ))
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.toolbar_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 布局模式
            item {
                Text(
                    stringResource(R.string.toolbar_layout_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                LayoutModeSelector(
                    currentMode = uiState.layoutMode,
                    onModeSelected = { viewModel.setLayoutMode(it) }
                )
            }

            // 当前按钮
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.toolbar_manage_buttons),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            itemsIndexed(uiState.items) { index, item ->
                ToolbarItemRow(
                    type = item.type,
                    enabled = item.enabled,
                    canMoveUp = index > 0,
                    canMoveDown = index < uiState.items.size - 1,
                    onToggle = { viewModel.toggleItem(item.type, it) },
                    onMoveUp = { viewModel.reorderItems(index, index - 1) },
                    onMoveDown = { viewModel.reorderItems(index, index + 1) }
                )
            }

            // 可添加的按钮
            if (uiState.availableToAdd.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.toolbar_available_buttons),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                itemsIndexed(uiState.availableToAdd) { _, type ->
                    AvailableToolbarItemRow(
                        type = type,
                        onAdd = { viewModel.toggleItem(type, true) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun LayoutModeSelector(
    currentMode: ToolbarLayoutMode,
    onModeSelected: (ToolbarLayoutMode) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = currentMode == ToolbarLayoutMode.SCROLLABLE,
                    onClick = { onModeSelected(ToolbarLayoutMode.SCROLLABLE) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.toolbar_layout_scrollable), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.toolbar_layout_scrollable_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = currentMode == ToolbarLayoutMode.FIXED,
                    onClick = { onModeSelected(ToolbarLayoutMode.FIXED) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.toolbar_layout_fixed), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.toolbar_layout_fixed_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ToolbarItemRow(
    type: ToolbarItemType,
    enabled: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onToggle: (Boolean) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getIconForType(type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(getLabelForType(type)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.toolbar_move_up), modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.toolbar_move_down), modifier = Modifier.size(16.dp))
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun AvailableToolbarItemRow(
    type: ToolbarItemType,
    onAdd: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getIconForType(type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                stringResource(getLabelForType(type)),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.toolbar_add))
            }
        }
    }
}

private fun getIconForType(type: ToolbarItemType): ImageVector = when (type) {
    ToolbarItemType.LOCALE_SWITCH -> Icons.Default.Language
    ToolbarItemType.THEME_TOGGLE -> Icons.Default.DarkMode
    ToolbarItemType.EMOJI -> Icons.Default.EmojiEmotions
    ToolbarItemType.SYMBOL -> Icons.Default.Star
    ToolbarItemType.CLIPBOARD -> Icons.Default.ContentPaste
    ToolbarItemType.LAYOUT_SWITCH -> Icons.Default.Keyboard
    ToolbarItemType.VOICE_INPUT -> Icons.Default.Mic
}

private fun getLabelForType(type: ToolbarItemType): Int = when (type) {
    ToolbarItemType.LOCALE_SWITCH -> R.string.toolbar_locale_switch
    ToolbarItemType.THEME_TOGGLE -> R.string.toolbar_theme_toggle
    ToolbarItemType.EMOJI -> R.string.toolbar_emoji
    ToolbarItemType.SYMBOL -> R.string.toolbar_symbol
    ToolbarItemType.CLIPBOARD -> R.string.toolbar_clipboard
    ToolbarItemType.LAYOUT_SWITCH -> R.string.toolbar_layout_switch
    ToolbarItemType.VOICE_INPUT -> R.string.toolbar_voice_input
}
