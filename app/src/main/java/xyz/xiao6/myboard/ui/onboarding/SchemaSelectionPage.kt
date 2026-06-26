package xyz.xiao6.myboard.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema
import xyz.xiao6.myboard.contract.state.BuiltInSchemas
import xyz.xiao6.myboard.state.BuiltInManifests

/**
 * 第 4 页：输入方案（布局）选择。
 * 根据选中的语言，显示可用的输入方案（拼音、双拼、QWERTY 等）。
 */
@Composable
fun SchemaSelectionPage(
    locale: LocaleTag?,
    selectedSchemas: List<Schema>,
    onToggleSchema: (Schema) -> Unit,
    onNext: () -> Unit,
    onSkip: () -> Unit
) {
    val manifest = remember(locale) {
        locale?.let { BuiltInManifests.all.find { m -> m.locale == it } }
    }
    val displayName = manifest?.displayName?.get("zh-CN")
        ?: manifest?.displayName?.get("en-US")
        ?: locale?.value ?: ""

    val availableSchemas = remember(manifest) {
        manifest?.scripts?.values?.flatMap { it.schemas.keys }?.distinct() ?: emptyList()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "选择输入方案",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "$displayName 的输入方案",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "选择一个或多个输入方式",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(availableSchemas, key = { it.value }) { schema ->
                val isSelected = schema in selectedSchemas
                val name = schemaDisplayName(schema)
                val description = schemaDescription(schema)

                SchemaItem(
                    name = name,
                    description = description,
                    isSelected = isSelected,
                    onClick = { onToggleSchema(schema) }
                )
            }
        }

        if (selectedSchemas.isEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "请至少选择一种方案",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("确认", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(
            onClick = onSkip,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("使用默认方案")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SchemaItem(
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = onClick,
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

            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
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
