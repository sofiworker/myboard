package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 第 3 页：语言选择。
 * 点击语言项弹出方案选择 Dialog（Gboard 风格）。
 */
@Composable
fun LanguageSelectionPage(
    selectedLanguages: Map<LocaleTag, List<Schema>>,
    showSchemaDialog: Boolean,
    schemaDialogLocale: LocaleTag?,
    schemaDialogSchemas: List<Schema>,
    onToggleLanguage: (LocaleTag, Schema) -> Unit,
    onOpenSchemaDialog: (LocaleTag) -> Unit,
    onToggleDialogSchema: (Schema) -> Unit,
    onConfirmSchemaDialog: () -> Unit,
    onDismissSchemaDialog: () -> Unit,
    onFinish: () -> Unit
) {
    val manifests = remember { BuiltInManifests.all }

    // 方案选择 Dialog
    if (showSchemaDialog && schemaDialogLocale != null) {
        val manifest = remember(schemaDialogLocale) {
            BuiltInManifests.all.find { m -> m.locale == schemaDialogLocale }
        }
        val displayName = manifest?.displayName?.get("zh-CN")
            ?: manifest?.displayName?.get("en-US")
            ?: schemaDialogLocale.value

        val availableSchemas = remember(manifest) {
            manifest?.scripts?.values?.flatMap { it.schemas.keys }?.distinct() ?: emptyList()
        }

        AlertDialog(
            onDismissRequest = onDismissSchemaDialog,
            title = {
                Text("输入方案 — $displayName")
            },
            text = {
                Column {
                    Text(
                        text = "选择一个或多个输入方式",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        availableSchemas.forEach { schema ->
                            val isSelected = schema in schemaDialogSchemas
                            SchemaDialogItem(
                                name = schemaDisplayName(schema),
                                description = schemaDescription(schema),
                                isSelected = isSelected,
                                onClick = { onToggleDialogSchema(schema) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onConfirmSchemaDialog) {
                    Text("确认")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissSchemaDialog) {
                    Text("取消")
                }
            }
        )
    }

    // 主界面
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "选择输入语言",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择您要使用的输入语言，点击可配置输入方案",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(manifests, key = { it.locale.value }) { manifest ->
                val isSelected = manifest.locale in selectedLanguages
                val schemas = selectedLanguages[manifest.locale] ?: emptyList()
                val displayName = manifest.displayName["zh-CN"]
                    ?: manifest.displayName["en-US"]
                    ?: manifest.locale.value

                LanguageItem(
                    displayName = displayName,
                    schemas = schemas,
                    isSelected = isSelected,
                    onClick = { onToggleLanguage(manifest.locale, manifest.defaults.schema) },
                    onSchemaClick = { onOpenSchemaDialog(manifest.locale) }
                )
            }
        }

        if (selectedLanguages.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请至少选择一种语言",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedLanguages.isNotEmpty()
        ) {
            Text("完成", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LanguageItem(
    displayName: String,
    schemas: List<Schema>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSchemaClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 2.dp else 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语言首字母标识
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = displayName.take(1),
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                if (isSelected && schemas.isNotEmpty()) {
                    Text(
                        text = schemas.joinToString("、") { schemaDisplayName(it) },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isSelected) {
                IconButton(onClick = onSchemaClick) {
                    Icon(
                        imageVector = Icons.Outlined.ChevronRight,
                        contentDescription = "配置方案",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun SchemaDialogItem(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun schemaDisplayName(schema: Schema): String = when (schema) {
    BuiltInSchemas.PINYIN -> "拼音"
    BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼（自然码）"
    BuiltInSchemas.T9_PINYIN -> "T9 拼音"
    BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
    BuiltInSchemas.LATIN_DIRECT -> "QWERTY"
    BuiltInSchemas.ROMAJI -> "假名（Romaji）"
    else -> schema.value
}

private fun schemaDescription(schema: Schema): String = when (schema) {
    BuiltInSchemas.PINYIN -> "标准拼音输入，支持全拼"
    BuiltInSchemas.SHUANGPIN_ZIRAN -> "自然码双拼方案，按键更少"
    BuiltInSchemas.T9_PINYIN -> "九宫格拼音输入"
    BuiltInSchemas.DOUBLE_PINYIN -> "双拼方案"
    BuiltInSchemas.LATIN_DIRECT -> "直接输入英文"
    BuiltInSchemas.ROMAJI -> "罗马音输入日文假名"
    else -> ""
}
