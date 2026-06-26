package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.repository.SettingsRepository

/**
 * LLM 设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMSettingsScreen(
    repo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by repo.settings.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var provider by remember(settings) { mutableStateOf(settings["llm_provider"] ?: "disabled") }
    var apiKey by remember(settings) { mutableStateOf(settings["llm_api_key"] ?: "") }
    var endpoint by remember(settings) { mutableStateOf(settings["llm_endpoint"] ?: "") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LLM 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Provider", style = MaterialTheme.typography.titleMedium)
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
                            Text(option)
                        }
                    }
                }
            }

            if (provider == "cloud") {
                item {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            scope.launch { repo.updateSetting("llm_api_key", it) }
                        },
                        label = { Text("API Key") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = {
                            endpoint = it
                            scope.launch { repo.updateSetting("llm_endpoint", it) }
                        },
                        label = { Text("API Endpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                Text("功能", style = MaterialTheme.typography.titleMedium)
                SwitchRow("自动补全", true)
                SwitchRow("翻译", true)
                SwitchRow("语句美化", true)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    text: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
