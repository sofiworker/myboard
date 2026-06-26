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
 * STT 设置页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun STTSettingsScreen(
    repo: SettingsRepository,
    onBack: () -> Unit
) {
    val settings by repo.settings.collectAsState(initial = emptyMap())
    val scope = rememberCoroutineScope()
    var provider by remember(settings) { mutableStateOf(settings["stt_provider"] ?: "system") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音输入设置") },
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
                Text("STT Provider", style = MaterialTheme.typography.titleMedium)
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
                            Text(if (option == "system") "系统" else "端侧")
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (provider == "system") {
                        "使用 Android 系统语音识别"
                    } else {
                        "使用端侧模型（待接入）"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
