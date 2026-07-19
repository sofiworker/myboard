package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.contract.manifest.LanguagePackManifest
import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Schema

/**
 * 语言与布局统一选择面板。
 *
 * 上方：语言切换标签（FilterChip 横向滚动）
 * 下方：当前语言可用的输入方案列表
 *
 * @param locales 所有已注册的语言能力列表
 * @param currentLocale 当前语言标签
 * @param currentSchema 当前选中的 Schema
 * @param schemasForLocale 获取指定语言可用 Schema 列表的函数
 * @param getSchemaDisplayName 获取 Schema 显示名称的函数
 * @param onLocaleSelected 用户选择语言后的回调
 * @param onSchemaSelected 用户选择 Schema 后的回调
 * @param onBack 返回键盘主界面（关闭面板）
 * @param onHideKeyboard 收起整个键盘
 */
@Composable
fun LocaleLayoutSwitchPanel(
    locales: List<LanguagePackManifest>,
    currentLocale: LocaleTag,
    currentSchema: Schema,
    schemasForLocale: (LocaleTag) -> List<Schema>,
    getSchemaDisplayName: (Schema) -> String,
    onLocaleSelected: (LocaleTag) -> Unit,
    onSchemaSelected: (Schema) -> Unit,
    onBack: () -> Unit,
    onHideKeyboard: () -> Unit = {}
) {
    // 面板内部维护选中语言状态，初始值为当前语言
    var selectedLocale by remember { mutableStateOf(currentLocale) }

    // 当选中语言变化时，获取该语言的可用 Schema
    val availableSchemas = remember(selectedLocale) {
        schemasForLocale(selectedLocale)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF1F3F4))
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("返回", fontSize = 12.sp)
            }
            Text("语言与方案", fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onHideKeyboard, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardHide, "收起键盘", modifier = Modifier.size(18.dp))
            }
        }

        // 语言标签（横向滚动，类似 EmojiPanel 的分类标签）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            locales.forEach { localeCap ->
                val isSelected = localeCap.locale == selectedLocale
                val displayName = localeCap.displayName["zh-CN"]
                    ?: localeCap.displayName["en-US"]
                    ?: localeCap.locale.value

                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedLocale = localeCap.locale
                    },
                    label = { Text(displayName, fontSize = 13.sp) },
                    leadingIcon = if (isSelected) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // 分隔线
        Divider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = Color(0xFFDADCE0),
            thickness = 0.5.dp
        )

        // 输入方案列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(availableSchemas) { schema ->
                val isSelected = schema == currentSchema && selectedLocale == currentLocale
                val displayName = getSchemaDisplayName(schema)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable {
                            if (selectedLocale != currentLocale) {
                                // 先切换语言
                                onLocaleSelected(selectedLocale)
                            }
                            // 再切换方案
                            onSchemaSelected(schema)
                        }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayName,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "已选",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
