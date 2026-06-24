package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Emoji 面板。
 */
@Composable
fun EmojiPanel(
    categories: List<Triple<String, String, List<String>>>,
    onEmojiClick: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()?.first ?: "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
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
            TextButton(onClick = onClose) {
                Text("返回", fontSize = 12.sp)
            }
            Text("Emoji", fontSize = 14.sp, modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(48.dp))
        }
        // 分类标签
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { (id, icon, _) ->
                FilterChip(
                    selected = id == selectedCategory,
                    onClick = { selectedCategory = id },
                    label = { Text(icon, fontSize = 16.sp) }
                )
            }
        }

        // Emoji 网格
        val emojis = categories.find { it.first == selectedCategory }?.third ?: emptyList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(emojis) { emoji ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onEmojiClick(emoji) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 24.sp)
                }
            }
        }
    }
}
