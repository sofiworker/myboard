package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.theme.BuiltInThemes
import xyz.xiao6.myboard.theme.ThemeDoc

@Composable
fun ThemeSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val themeMode = uiState.settings["theme_mode"] ?: "auto"
    val currentThemeId = uiState.settings["current_theme"] ?: "default_light"
    val themes = BuiltInThemes.all

    SettingsScaffold(
        title = stringResource(R.string.settings_theme),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_theme_mode)) }
            item {
                SettingsGroup {
                    ThemeModeOption(
                        label = stringResource(R.string.settings_theme_auto),
                        description = stringResource(R.string.settings_theme_auto_desc),
                        selected = themeMode == "auto",
                        onClick = { viewModel.updateSetting("theme_mode", "auto") },
                        showDivider = true
                    )
                    ThemeModeOption(
                        label = stringResource(R.string.settings_theme_light),
                        description = stringResource(R.string.settings_theme_light_desc),
                        selected = themeMode == "light",
                        onClick = { viewModel.updateSetting("theme_mode", "light") },
                        showDivider = true
                    )
                    ThemeModeOption(
                        label = stringResource(R.string.settings_theme_dark),
                        description = stringResource(R.string.settings_theme_dark_desc),
                        selected = themeMode == "dark",
                        onClick = { viewModel.updateSetting("theme_mode", "dark") }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SettingsSectionHeader(stringResource(R.string.settings_keyboard_theme))
            }
            item {
                SettingsGroup {
                    themes.forEachIndexed { index, theme ->
                        ThemeItem(
                            theme = theme,
                            isSelected = theme.id == currentThemeId ||
                                (currentThemeId == "default" && theme.id == "default_light"),
                            onClick = { viewModel.updateSetting("current_theme", theme.id) },
                            showDivider = index < themes.lastIndex
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun ThemeModeOption(
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
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 10.dp),
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
private fun ThemeItem(
    theme: ThemeDoc,
    isSelected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ThemeSwatch(theme = theme)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = if (theme.dark) {
                        stringResource(R.string.settings_theme_dark)
                    } else {
                        stringResource(R.string.settings_theme_light)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (showDivider) SettingsGroupDivider()
    }
}

@Composable
private fun ThemeSwatch(theme: ThemeDoc) {
    val bg = parseHex(theme.colors.background)
    val key = parseHex(theme.colors.keyDefault)
    val accent = parseHex(theme.colors.candidateHighlight)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(key)
            )
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    }
}

private fun parseHex(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (_: Exception) {
        Color.Gray
    }
}
