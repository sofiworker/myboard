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
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.theme.BuiltInThemes
import xyz.xiao6.myboard.theme.ThemeDoc

/**
 * 主题设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen(
    currentThemeId: String,
    onThemeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val themes = BuiltInThemes.all

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("主题设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
            items(themes.size) { index ->
                val theme = themes[index]
                ThemeItem(
                    theme = theme,
                    isSelected = theme.id == currentThemeId,
                    onClick = { onThemeSelected(theme.id) }
                )
            }
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
