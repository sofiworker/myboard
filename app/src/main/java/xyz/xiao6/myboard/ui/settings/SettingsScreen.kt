package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SpaceBar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.db.SettingsDatabase
import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.data.settings.KeyboardHeightPolicy

/**
 * 设置主页面 — 所有设置项均从 SettingsRepository 单一来源读取。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit = {},
    viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            SettingsRepository(
                SettingsDatabase.getInstance(LocalContext.current).settingsDao()
            )
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val configuration = LocalConfiguration.current
    val keyboardHeight = KeyboardHeightPolicy.resolve(
        storedHeight = uiState.settings[KeyboardHeightPolicy.KEY_HEIGHT],
        screenHeightDp = configuration.screenHeightDp
    ).heightDp.toFloat()
    val keyboardHorizontalInset = KeyboardHeightPolicy.resolveHorizontalInset(
        storedInset = uiState.settings[KeyboardHeightPolicy.KEY_HORIZONTAL_INSET],
        screenWidthDp = configuration.screenWidthDp
    ).insetDp.toFloat()

    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_section_input_method)) }
            item {
                SettingsGroup {
                    SettingsNavItem(
                        title = stringResource(R.string.settings_language_layout),
                        subtitle = uiState.settings["current_locale"] ?: "en-US",
                        icon = Icons.Default.Language,
                        onClick = { onNavigate("language") },
                        showDivider = true,
                        accent = SettingsAccent.Blue
                    )
                    SettingsNavItem(
                        title = stringResource(R.string.settings_toolbar),
                        subtitle = stringResource(R.string.settings_toolbar_manage),
                        icon = Icons.Default.Widgets,
                        onClick = { onNavigate("toolbar") },
                        accent = SettingsAccent.Green
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_keyboard)) }
            item {
                SettingsGroup {
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_keyboard_height),
                        value = keyboardHeight,
                        onValueChange = {
                            viewModel.updateSetting(
                                KeyboardHeightPolicy.KEY_HEIGHT,
                                it.toInt().toString()
                            )
                        },
                        valueRange = KeyboardHeightPolicy.MIN_HEIGHT_DP.toFloat()..
                            KeyboardHeightPolicy.MAX_HEIGHT_DP.toFloat(),
                        valueLabel = "${keyboardHeight.toInt()}dp",
                        showDivider = true,
                        icon = Icons.Default.Height,
                        accent = SettingsAccent.Indigo
                    )
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_keyboard_horizontal_inset),
                        value = keyboardHorizontalInset,
                        onValueChange = {
                            viewModel.updateSetting(
                                KeyboardHeightPolicy.KEY_HORIZONTAL_INSET,
                                it.toInt().toString()
                            )
                        },
                        valueRange = KeyboardHeightPolicy.MIN_HORIZONTAL_INSET_DP.toFloat()..
                            KeyboardHeightPolicy.MAX_HORIZONTAL_INSET_DP.toFloat(),
                        valueLabel = "${keyboardHorizontalInset.toInt()}dp",
                        showDivider = true,
                        icon = Icons.Default.SwapHoriz,
                        accent = SettingsAccent.Indigo
                    )
                    SettingsSliderItem(
                        title = stringResource(R.string.settings_key_font_size),
                        value = (uiState.settings["key_font_size"] ?: "18").toFloatOrNull() ?: 18f,
                        onValueChange = {
                            viewModel.updateSetting("key_font_size", it.toInt().toString())
                        },
                        valueRange = 12f..24f,
                        valueLabel = "${((uiState.settings["key_font_size"] ?: "18").toFloatOrNull() ?: 18f).toInt()}sp",
                        icon = Icons.Default.FormatSize,
                        accent = SettingsAccent.Indigo
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                SettingsGroup {
                    SettingsNavItem(
                        title = stringResource(R.string.settings_theme),
                        subtitle = when (uiState.settings["theme_mode"] ?: "auto") {
                            "dark" -> stringResource(R.string.settings_theme_dark)
                            "light" -> stringResource(R.string.settings_theme_light)
                            else -> stringResource(R.string.settings_theme_auto)
                        },
                        icon = Icons.Default.Palette,
                        onClick = { onNavigate("theme") },
                        accent = SettingsAccent.Purple
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_feedback)) }
            item {
                SettingsGroup {
                    SettingsNavItem(
                        title = stringResource(R.string.settings_feedback),
                        subtitle = stringResource(R.string.settings_feedback_desc),
                        icon = Icons.Default.Vibration,
                        onClick = { onNavigate("feedback") },
                        accent = SettingsAccent.Orange
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_input)) }
            item {
                SettingsGroup {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_double_space_period),
                        subtitle = stringResource(R.string.settings_double_space_period_desc),
                        icon = Icons.Default.SpaceBar,
                        checked = uiState.settings["double_space_period"]?.toBooleanStrictOrNull() ?: true,
                        onCheckedChange = {
                            viewModel.updateSetting("double_space_period", it.toString())
                        },
                        showDivider = true,
                        accent = SettingsAccent.Teal
                    )
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_auto_capitalize),
                        subtitle = stringResource(R.string.settings_auto_capitalize_desc),
                        icon = Icons.Default.TextFormat,
                        checked = uiState.settings["auto_capitalize"]?.toBooleanStrictOrNull() ?: true,
                        onCheckedChange = {
                            viewModel.updateSetting("auto_capitalize", it.toString())
                        },
                        accent = SettingsAccent.Teal
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_ai)) }
            item {
                SettingsGroup {
                    SettingsNavItem(
                        title = stringResource(R.string.settings_llm),
                        subtitle = when (uiState.settings["llm_provider"] ?: "disabled") {
                            "local" -> stringResource(R.string.settings_llm_provider_local)
                            "cloud" -> stringResource(R.string.settings_llm_provider_cloud)
                            else -> stringResource(R.string.settings_llm_provider_disabled)
                        },
                        icon = Icons.Default.SmartToy,
                        onClick = { onNavigate("llm") },
                        showDivider = true,
                        accent = SettingsAccent.Blue
                    )
                    SettingsNavItem(
                        title = stringResource(R.string.settings_voice_input),
                        subtitle = when (uiState.settings["stt_provider"] ?: "system") {
                            "on_device" -> stringResource(R.string.settings_stt_provider_on_device)
                            else -> stringResource(R.string.settings_stt_provider_system)
                        },
                        icon = Icons.Default.Mic,
                        onClick = { onNavigate("stt") },
                        accent = SettingsAccent.Pink
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_about)) }
            item {
                SettingsGroup {
                    SettingsNavItem(
                        title = stringResource(R.string.settings_version),
                        subtitle = versionName,
                        icon = Icons.Default.Info,
                        onClick = null,
                        showDivider = true,
                        accent = SettingsAccent.Gray
                    )
                    SettingsNavItem(
                        title = stringResource(R.string.settings_open_source),
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        onClick = { onNavigate("about") },
                        accent = SettingsAccent.Amber
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
