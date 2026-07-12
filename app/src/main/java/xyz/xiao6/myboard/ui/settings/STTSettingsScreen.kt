package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.data.repository.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun STTSettingsScreen(
    repo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by repo.settings.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var provider by remember(settings) {
        mutableStateOf(settings["stt_provider"] ?: "system")
    }

    SettingsScaffold(
        title = stringResource(R.string.settings_stt_title),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_stt_provider)) }
            item {
                SettingsGroup {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("system", "on_device").forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = provider == option,
                                    onClick = {
                                        provider = option
                                        scope.launch { repo.updateSetting("stt_provider", option) }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, 2)
                                ) {
                                    Text(
                                        if (option == "system") {
                                            stringResource(R.string.settings_stt_provider_system)
                                        } else {
                                            stringResource(R.string.settings_stt_provider_on_device)
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (provider == "system") {
                                stringResource(R.string.settings_stt_provider_system_desc)
                            } else {
                                stringResource(R.string.settings_stt_provider_on_device_desc)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
