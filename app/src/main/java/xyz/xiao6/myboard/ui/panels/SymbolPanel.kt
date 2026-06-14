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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 符号面板。
 */
@Composable
fun SymbolPanel(
    categories: List<Pair<String, List<String>>>,
    onSymbolClick: (String) -> Unit,
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

        // 符号网格
        val symbols = categories.find { it.first == selectedCategory }?.second ?: emptyList()
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(symbols) { symbol ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onSymbolClick(symbol) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(symbol, fontSize = 20.sp)
                }
            }
        }
    }
}
