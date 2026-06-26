package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 设置主页面 — 所有设置项均从 SettingsRepository 单一来源读取。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ===== 输入法 =====
            item { SectionHeader(stringResource(R.string.settings_section_input_method)) }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_language_layout),
                    subtitle = uiState.settings["current_locale"] ?: "en-US",
                    icon = Icons.Default.Language,
                    onClick = { onNavigate("language") }
                )
            }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_toolbar),
                    subtitle = stringResource(R.string.settings_toolbar_manage),
                    icon = Icons.Default.Widgets,
                    onClick = { onNavigate("toolbar") }
                )
            }

            // ===== 键盘 =====
            item { SectionHeader(stringResource(R.string.settings_section_keyboard)) }
            item {
                SliderItem(
                    title = stringResource(R.string.settings_keyboard_height),
                    value = (uiState.settings["keyboard_height"] ?: "260").toFloatOrNull() ?: 260f,
                    onValueChange = { viewModel.updateSetting("keyboard_height", it.toInt().toString()) },
                    valueRange = 180f..400f,
                    suffix = "dp"
                )
            }
            item {
                SliderItem(
                    title = stringResource(R.string.settings_key_font_size),
                    value = (uiState.settings["key_font_size"] ?: "18").toFloatOrNull() ?: 18f,
                    onValueChange = { viewModel.updateSetting("key_font_size", it.toInt().toString()) },
                    valueRange = 12f..24f,
                    suffix = "sp"
                )
            }

            // ===== 外观 =====
            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_theme),
                    subtitle = when (uiState.settings["theme_mode"] ?: "auto") {
                        "dark" -> stringResource(R.string.settings_theme_dark)
                        "light" -> stringResource(R.string.settings_theme_light)
                        else -> stringResource(R.string.settings_theme_auto)
                    },
                    icon = Icons.Default.Palette,
                    onClick = { onNavigate("theme") }
                )
            }

            // ===== 反馈 =====
            item { SectionHeader(stringResource(R.string.settings_feedback)) }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_feedback),
                    icon = Icons.Default.Vibration,
                    onClick = { onNavigate("feedback") }
                )
            }

            // ===== 输入 =====
            item { SectionHeader(stringResource(R.string.settings_section_input)) }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_double_space_period),
                    icon = Icons.Default.SpaceBar,
                    checked = uiState.settings["double_space_period"]?.toBooleanStrictOrNull() ?: true,
                    onCheckedChange = { viewModel.updateSetting("double_space_period", it.toString()) }
                )
            }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_auto_capitalize),
                    icon = Icons.Default.TextFormat,
                    checked = uiState.settings["auto_capitalize"]?.toBooleanStrictOrNull() ?: true,
                    onCheckedChange = { viewModel.updateSetting("auto_capitalize", it.toString()) }
                )
            }

            // ===== AI =====
            item { SectionHeader("AI") }
            item {
                NavigationItem(
                    title = "LLM",
                    subtitle = uiState.settings["llm_provider"] ?: "disabled",
                    icon = Icons.Default.SmartToy,
                    onClick = { onNavigate("llm") }
                )
            }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_voice_input),
                    subtitle = uiState.settings["stt_provider"] ?: "system",
                    icon = Icons.Default.Mic,
                    onClick = { onNavigate("stt") }
                )
            }

            // ===== 关于 =====
            item { SectionHeader(stringResource(R.string.settings_about)) }
            item {
                val context = LocalContext.current
                val versionName = remember {
                    try {
                        val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                        pInfo.versionName ?: "1.0"
                    } catch (_: Exception) { "1.0" }
                }
                NavigationItem(
                    title = stringResource(R.string.settings_version),
                    subtitle = versionName,
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = stringResource(R.string.settings_open_source),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { onNavigate("about") }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun NavigationItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SwitchItem(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
fun SliderItem(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    suffix: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = valueRange,
                    modifier = Modifier.weight(1f)
                )
                Text("${value.toInt()}$suffix", modifier = Modifier.width(60.dp))
            }
        }
    }
}
