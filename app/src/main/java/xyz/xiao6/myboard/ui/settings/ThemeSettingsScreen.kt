package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.BuiltInThemes
import xyz.xiao6.myboard.theme.ThemeDoc

/**
 * 主题设置页面。
 * 包含主题模式（自动/亮色/暗色）和键盘主题选择。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            SettingsRepository(
                xyz.xiao6.myboard.data.db.SettingsDatabase.getInstance(
                    androidx.compose.ui.platform.LocalContext.current
                ).settingsDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode = uiState.settings["theme_mode"] ?: "auto"
    val currentThemeId = uiState.settings["current_theme"] ?: "default"
    val themes = BuiltInThemes.all

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_theme)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 主题模式
            item {
                Text(
                    stringResource(R.string.settings_theme_mode),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ThemeModeOption(
                            label = stringResource(R.string.settings_theme_auto),
                            description = stringResource(R.string.settings_theme_auto_desc),
                            selected = themeMode == "auto",
                            onClick = { viewModel.updateSetting("theme_mode", "auto") }
                        )
                        ThemeModeOption(
                            label = stringResource(R.string.settings_theme_light),
                            description = stringResource(R.string.settings_theme_light_desc),
                            selected = themeMode == "light",
                            onClick = { viewModel.updateSetting("theme_mode", "light") }
                        )
                        ThemeModeOption(
                            label = stringResource(R.string.settings_theme_dark),
                            description = stringResource(R.string.settings_theme_dark_desc),
                            selected = themeMode == "dark",
                            onClick = { viewModel.updateSetting("theme_mode", "dark") }
                        )
                    }
                }
            }

            // 键盘主题
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_keyboard_theme),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            items(themes.size) { index ->
                val theme = themes[index]
                ThemeItem(
                    theme = theme,
                    isSelected = theme.id == currentThemeId,
                    onClick = { viewModel.updateSetting("current_theme", theme.id) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun ThemeModeOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThemeItem(
    theme: ThemeDoc,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(theme.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    theme.id,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Text("✓", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
