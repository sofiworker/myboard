package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
fun LLMSettingsScreen(
    repo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by repo.settings.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var provider by remember(settings) {
        mutableStateOf(settings["llm_provider"] ?: "disabled")
    }
    var apiKey by remember(settings) { mutableStateOf(settings["llm_api_key"] ?: "") }
    var endpoint by remember(settings) { mutableStateOf(settings["llm_endpoint"] ?: "") }

    SettingsScaffold(
        title = stringResource(R.string.settings_llm_title),
        onBack = onBack
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            item { SettingsSectionHeader(stringResource(R.string.settings_llm_provider)) }
            item {
                SettingsGroup {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf("disabled", "local", "cloud").forEachIndexed { index, option ->
                                SegmentedButton(
                                    selected = provider == option,
                                    onClick = {
                                        provider = option
                                        scope.launch { repo.updateSetting("llm_provider", option) }
                                    },
                                    shape = SegmentedButtonDefaults.itemShape(index, 3)
                                ) {
                                    Text(
                                        when (option) {
                                            "local" -> stringResource(R.string.settings_llm_provider_local)
                                            "cloud" -> stringResource(R.string.settings_llm_provider_cloud)
                                            else -> stringResource(R.string.settings_llm_provider_disabled)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (provider == "cloud") {
                item { SettingsSectionHeader(stringResource(R.string.settings_llm_cloud)) }
                item {
                    SettingsGroup {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = {
                                    apiKey = it
                                    scope.launch { repo.updateSetting("llm_api_key", it) }
                                },
                                label = { Text(stringResource(R.string.settings_llm_api_key)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = endpoint,
                                onValueChange = {
                                    endpoint = it
                                    scope.launch { repo.updateSetting("llm_endpoint", it) }
                                },
                                label = { Text(stringResource(R.string.settings_llm_endpoint)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(40.dp)) }
        }
    }
}
