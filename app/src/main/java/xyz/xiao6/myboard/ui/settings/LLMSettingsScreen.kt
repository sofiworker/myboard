package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.settings.SettingsManager

/**
 * LLM 设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMSettingsScreen(
    settings: SettingsManager,
    onBack: () -> Unit
) {
    var provider by remember { mutableStateOf(settings.llmProvider) }
    var apiKey by remember { mutableStateOf(settings.llmApiKey) }
    var endpoint by remember { mutableStateOf(settings.llmEndpoint) }

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
                                settings.llmProvider = option
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
                            settings.llmApiKey = it
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
                            settings.llmEndpoint = it
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
