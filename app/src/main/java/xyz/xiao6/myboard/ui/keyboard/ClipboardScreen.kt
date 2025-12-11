package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.data.db.ClipboardItem

@Composable
fun ClipboardScreen(
    viewModel: KeyboardViewModel
) {
    val history by viewModel.clipboardHistory.collectAsState(initial = emptyList())

    LazyColumn(modifier = Modifier.padding(8.dp)) {
        items(history) { item ->
            ClipboardHistoryItem(item = item, onPaste = { viewModel.commitSuggestion(it) }, onDelete = { viewModel.deleteClipboardItem(it) })
        }
    }
}

@Composable
fun ClipboardHistoryItem(
    item: ClipboardItem,
    onPaste: (String) -> Unit,
    onDelete: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onPaste(item.text) }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.text, modifier = Modifier.weight(1f), maxLines = 1)
            IconButton(onClick = { onDelete(item.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}
