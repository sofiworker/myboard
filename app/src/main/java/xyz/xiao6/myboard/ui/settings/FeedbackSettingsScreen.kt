package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R

@Composable
fun FeedbackSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val hapticEnabled = uiState.settings["haptic_feedback"]?.toBooleanStrictOrNull() ?: true
    val soundEnabled = uiState.settings["sound_feedback"]?.toBooleanStrictOrNull() ?: false

    SettingsScaffold(
        title = stringResource(R.string.settings_feedback),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_section_vibration)) }
            item {
                SettingsGroup {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_vibration_follow_system),
                        subtitle = stringResource(R.string.settings_vibration_follow_system_desc),
                        icon = Icons.Default.Vibration,
                        checked = hapticEnabled,
                        onCheckedChange = {
                            viewModel.updateSetting("haptic_feedback", it.toString())
                        },
                        accent = SettingsAccent.Orange
                    )
                }
            }

            item { SettingsSectionHeader(stringResource(R.string.settings_section_sound)) }
            item {
                SettingsGroup {
                    SettingsSwitchItem(
                        title = stringResource(R.string.settings_sound_feedback),
                        subtitle = stringResource(R.string.settings_sound_feedback_desc),
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        checked = soundEnabled,
                        onCheckedChange = {
                            viewModel.updateSetting("sound_feedback", it.toString())
                        },
                        accent = SettingsAccent.Pink
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
