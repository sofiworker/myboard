package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import xyz.xiao6.myboard.data.db.AppDatabase
import xyz.xiao6.myboard.data.db.UserWord

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserDictionaryScreen() {
    val context = LocalContext.current
    val dao = AppDatabase.getDatabase(context).userWordDao()
    // In a real app, this would be in a ViewModel
    val words = remember { runBlocking { dao.getAll() } }

    Scaffold(
        topBar = { TopAppBar(title = { Text("User Dictionary") }) }
    ) {
        LazyColumn(modifier = Modifier.padding(it)) {
            items(words) { word ->
                UserWordItem(word = word, onDelete = { 
                    runBlocking { dao.delete(word.id) }
                })
            }
        }
    }
}

@Composable
fun UserWordItem(word: UserWord, onDelete: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = word.text, modifier = Modifier.weight(1f))
            Text(text = "Freq: ${word.frequency}")
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete word")
            }
        }
    }
}
