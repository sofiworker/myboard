package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.settings.SettingsManager

/**
 * 设置页面 - 完整版。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings = remember { SettingsManager(context) }

    var currentLanguage by remember { mutableStateOf(settings.currentLanguage) }
    var currentTheme by remember { mutableStateOf(settings.currentTheme) }
    var keyboardHeight by remember { mutableFloatStateOf(settings.keyboardHeight.toFloat()) }
    var hapticFeedback by remember { mutableStateOf(settings.hapticFeedback) }
    var soundFeedback by remember { mutableStateOf(settings.soundFeedback) }
    var doubleSpacePeriod by remember { mutableStateOf(settings.doubleSpacePeriod) }
    var autoCapitalize by remember { mutableStateOf(settings.autoCapitalize) }
    var llmProvider by remember { mutableStateOf(settings.llmProvider) }
    var sttProvider by remember { mutableStateOf(settings.sttProvider) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 语言
            item { SectionHeader("语言") }
            item {
                NavigationItem(
                    title = "当前语言",
                    subtitle = currentLanguage,
                    icon = Icons.Default.Language,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = "管理语言",
                    icon = Icons.Default.Translate,
                    onClick = { }
                )
            }
            item {
                SwitchItem(
                    title = "自动切换语言",
                    icon = Icons.Default.Autorenew,
                    checked = true,
                    onCheckedChange = { }
                )
            }

            // 布局
            item { SectionHeader("布局") }
            item {
                NavigationItem(
                    title = "布局选择",
                    subtitle = "QWERTY",
                    icon = Icons.Default.Keyboard,
                    onClick = { }
                )
            }
            item {
                SliderItem(
                    title = "键盘高度",
                    value = keyboardHeight,
                    onValueChange = {
                        keyboardHeight = it
                        settings.keyboardHeight = it.toInt()
                    },
                    valueRange = 180f..400f,
                    suffix = "dp"
                )
            }

            // 外观
            item { SectionHeader("外观") }
            item {
                NavigationItem(
                    title = "主题",
                    subtitle = currentTheme,
                    icon = Icons.Default.Palette,
                    onClick = { }
                )
            }
            item {
                SwitchItem(
                    title = "按键动画",
                    icon = Icons.Default.Animation,
                    checked = true,
                    onCheckedChange = { }
                )
            }

            // 功能
            item { SectionHeader("功能") }
            item {
                NavigationItem(
                    title = "词典管理",
                    icon = Icons.Default.MenuBook,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = "文本填充",
                    icon = Icons.Default.TextSnippet,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = "剪贴板",
                    icon = Icons.Default.ContentPaste,
                    onClick = { }
                )
            }

            // AI
            item { SectionHeader("AI") }
            item {
                NavigationItem(
                    title = "LLM 设置",
                    subtitle = llmProvider,
                    icon = Icons.Default.SmartToy,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = "语音输入",
                    subtitle = sttProvider,
                    icon = Icons.Default.Mic,
                    onClick = { }
                )
            }
            item {
                SwitchItem(
                    title = "智能联想",
                    icon = Icons.Default.AutoAwesome,
                    checked = true,
                    onCheckedChange = { }
                )
            }

            // 反馈
            item { SectionHeader("反馈") }
            item {
                SwitchItem(
                    title = "触觉反馈",
                    subtitle = "按键时振动",
                    icon = Icons.Default.Vibration,
                    checked = hapticFeedback,
                    onCheckedChange = {
                        hapticFeedback = it
                        settings.hapticFeedback = it
                    }
                )
            }
            item {
                SwitchItem(
                    title = "声音反馈",
                    subtitle = "按键时播放声音",
                    icon = Icons.Default.VolumeUp,
                    checked = soundFeedback,
                    onCheckedChange = {
                        soundFeedback = it
                        settings.soundFeedback = it
                    }
                )
            }
            item {
                SwitchItem(
                    title = "双击空格输入句号",
                    icon = Icons.Default.SpaceBar,
                    checked = doubleSpacePeriod,
                    onCheckedChange = {
                        doubleSpacePeriod = it
                        settings.doubleSpacePeriod = it
                    }
                )
            }
            item {
                SwitchItem(
                    title = "自动大写",
                    icon = Icons.Default.TextFormat,
                    checked = autoCapitalize,
                    onCheckedChange = {
                        autoCapitalize = it
                        settings.autoCapitalize = it
                    }
                )
            }

            // 关于
            item { SectionHeader("关于") }
            item {
                NavigationItem(
                    title = "版本",
                    subtitle = "1.0.0",
                    icon = Icons.Default.Info,
                    onClick = { }
                )
            }
            item {
                NavigationItem(
                    title = "开源许可",
                    icon = Icons.Default.Code,
                    onClick = { }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun NavigationItem(
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
private fun SwitchItem(
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
private fun SliderItem(
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
