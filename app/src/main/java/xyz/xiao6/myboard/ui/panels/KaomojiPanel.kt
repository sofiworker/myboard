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

/**
 * 颜文字面板。
 *
 * @param onBack 返回上一层（回到键盘主界面），**不是**关闭输入法
 * @param onHideKeyboard 收起整个键盘
 */
@Composable
fun KaomojiPanel(
    categories: List<Pair<String, List<String>>>,
    onKaomojiClick: (String) -> Unit,
    /** 返回键盘主界面（关闭面板），不是关闭输入法 */
    onBack: () -> Unit,
    /** 收起整个键盘 */
    onHideKeyboard: () -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.first ?: "") }

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
            Text("颜文字", fontSize = 14.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = onHideKeyboard, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.KeyboardHide, "收起键盘", modifier = Modifier.size(18.dp))
            }
        }
        // 分类标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { (name, _) ->
                FilterChip(
                    selected = name == selectedCategory,
                    onClick = { selectedCategory = name },
                    label = { Text(name, fontSize = 12.sp) }
                )
            }
        }

        // 颜文字列表
        val kaomojis = categories.find { it.first == selectedCategory }?.second ?: emptyList()
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(kaomojis) { kaomoji ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onKaomojiClick(kaomoji) }
                        .padding(12.dp)
                ) {
                    Text(kaomoji, fontSize = 16.sp)
                }
            }
        }
    }
}
