package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
 */
@Composable
fun KaomojiPanel(
    categories: List<Pair<String, List<String>>>,
    onKaomojiClick: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.first ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F3F4))
    ) {
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
                .height(200.dp)
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
