package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
 * 文本填充面板。
 */
@Composable
fun TextExpansionPanel(
    expansions: List<Pair<String, String>>,
    onExpansionClick: (String) -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onClose: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newShortcut by remember { mutableStateOf("") }
    var newExpansion by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F3F4))
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("文本填充", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Row {
                TextButton(onClick = { showAddDialog = true }) {
                    Text("添加")
                }
                IconButton(onClick = onClose) {
                    Text("×", fontSize = 20.sp)
                }
            }
        }

        // 扩展列表
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(expansions) { (shortcut, expansion) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                        .clickable { onExpansionClick(shortcut) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(shortcut, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(expansion, fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(onClick = { onDelete(shortcut) }) {
                        Text("×", color = Color.Gray)
                    }
                }
            }
        }
    }

    // 添加对话框
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加文本填充") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newShortcut,
                        onValueChange = { newShortcut = it },
                        label = { Text("缩写") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newExpansion,
                        onValueChange = { newExpansion = it },
                        label = { Text("展开文本") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onAdd(newShortcut, newExpansion)
                    newShortcut = ""
                    newExpansion = ""
                    showAddDialog = false
                }) {
                    Text("添加")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}
