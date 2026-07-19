package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.entity.ToolbarItemType
import xyz.xiao6.myboard.data.entity.ToolbarLayoutMode
import xyz.xiao6.myboard.data.repository.SettingsRepository

@Composable
fun ToolbarSettingsScreen(
    onBack: () -> Unit,
    viewModel: ToolbarSettingsViewModel = viewModel(
        factory = ToolbarSettingsViewModel.Factory(
            SettingsRepository(
                SettingsDatabase.getInstance(LocalContext.current).settingsDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    SettingsScaffold(
        title = stringResource(R.string.toolbar_settings_title),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.toolbar_layout_mode)) }
            item {
                SettingsGroup {
                    LayoutModeOption(
                        label = stringResource(R.string.toolbar_layout_scrollable),
                        description = stringResource(R.string.toolbar_layout_scrollable_desc),
                        selected = uiState.layoutMode == ToolbarLayoutMode.SCROLLABLE,
                        onClick = { viewModel.setLayoutMode(ToolbarLayoutMode.SCROLLABLE) },
                        showDivider = true
                    )
                    LayoutModeOption(
                        label = stringResource(R.string.toolbar_layout_fixed),
                        description = stringResource(R.string.toolbar_layout_fixed_desc),
                        selected = uiState.layoutMode == ToolbarLayoutMode.FIXED,
                        onClick = { viewModel.setLayoutMode(ToolbarLayoutMode.FIXED) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(stringResource(R.string.toolbar_manage_buttons))
            }
            item {
                SettingsGroup {
                    uiState.items.forEachIndexed { index, item ->
                        ToolbarItemRow(
                            type = item.type,
                            enabled = item.enabled,
                            canMoveUp = index > 0,
                            canMoveDown = index < uiState.items.size - 1,
                            onToggle = { viewModel.toggleItem(item.type, it) },
                            onMoveUp = { viewModel.reorderItems(index, index - 1) },
                            onMoveDown = { viewModel.reorderItems(index, index + 1) },
                            showDivider = index < uiState.items.lastIndex
                        )
                    }
                }
            }

            if (uiState.availableToAdd.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    SettingsSectionHeader(stringResource(R.string.toolbar_available_buttons))
                }
                item {
                    SettingsGroup {
                        uiState.availableToAdd.forEachIndexed { index, type ->
                            AvailableToolbarItemRow(
                                type = type,
                                onAdd = { viewModel.toggleItem(type, true) },
                                showDivider = index < uiState.availableToAdd.lastIndex
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun LayoutModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = onClick)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) SettingsGroupDivider()
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
    onMoveDown: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBubble(icon = getIconForType(type), accent = getAccentForType(type))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(getLabelForType(type)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ArrowUpward,
                    contentDescription = stringResource(R.string.toolbar_move_up),
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = stringResource(R.string.toolbar_move_down),
                    modifier = Modifier.size(16.dp)
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
        if (showDivider) SettingsGroupDivider()
    }
}

@Composable
private fun AvailableToolbarItemRow(
    type: ToolbarItemType,
    onAdd: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconBubble(icon = getIconForType(type), accent = getAccentForType(type))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(getLabelForType(type)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.toolbar_add))
            }
        }
        if (showDivider) SettingsGroupDivider()
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

private fun getAccentForType(type: ToolbarItemType): SettingsAccent = when (type) {
    ToolbarItemType.LOCALE_SWITCH -> SettingsAccent.Blue
    ToolbarItemType.THEME_TOGGLE -> SettingsAccent.Indigo
    ToolbarItemType.EMOJI -> SettingsAccent.Orange
    ToolbarItemType.SYMBOL -> SettingsAccent.Amber
    ToolbarItemType.CLIPBOARD -> SettingsAccent.Teal
    ToolbarItemType.LAYOUT_SWITCH -> SettingsAccent.Purple
    ToolbarItemType.VOICE_INPUT -> SettingsAccent.Pink
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
