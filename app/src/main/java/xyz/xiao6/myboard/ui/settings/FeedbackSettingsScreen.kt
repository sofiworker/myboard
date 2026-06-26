package xyz.xiao6.myboard.ui.settings

import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * 反馈设置页面。
 * 管理触觉反馈和按键音量。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackSettingsScreen(
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
    val hapticEnabled = uiState.settings["haptic_feedback"]?.toBooleanStrictOrNull() ?: true
    val soundEnabled = uiState.settings["sound_feedback"]?.toBooleanStrictOrNull() ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_feedback)) },
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
            // 触觉反馈
            item { SectionHeader(stringResource(R.string.settings_section_vibration)) }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_vibration_follow_system),
                    subtitle = stringResource(R.string.settings_vibration_follow_system_desc),
                    icon = Icons.Default.Vibration,
                    checked = hapticEnabled,
                    onCheckedChange = { viewModel.updateSetting("haptic_feedback", it.toString()) }
                )
            }

            // 声音反馈
            item { SectionHeader(stringResource(R.string.settings_section_sound)) }
            item {
                SwitchItem(
                    title = stringResource(R.string.settings_sound_feedback),
                    subtitle = stringResource(R.string.settings_sound_feedback_desc),
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    checked = soundEnabled,
                    onCheckedChange = { viewModel.updateSetting("sound_feedback", it.toString()) }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}
