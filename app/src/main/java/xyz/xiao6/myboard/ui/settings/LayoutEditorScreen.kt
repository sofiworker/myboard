package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.data.KeyData as LayoutKeyData
import xyz.xiao6.myboard.data.KeyboardData as LayoutKeyboardData
import xyz.xiao6.myboard.data.KeyArrangement
import xyz.xiao6.myboard.data.KeyArrangement as LayoutKeyArrangement
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.model.KeyType
import xyz.xiao6.myboard.data.repository.KeyboardRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutEditorScreen(layoutName: String) {
    val context = LocalContext.current
    val keyboardRepository = remember { KeyboardRepository(context) }
    var keyboardData by remember { mutableStateOf(keyboardRepository.getKeyboardLayout(layoutName) as LayoutKeyboardData?) }
    var selectedKeyForEdit by remember { mutableStateOf<LayoutKeyData?>(null) }

    fun updateKey(keyboard: LayoutKeyboardData, oldKey: LayoutKeyData, newKey: LayoutKeyData): LayoutKeyboardData {
        val newArrangement = keyboard.arrangement.map { rowData ->
            val newRow = rowData.row.map { keyArrangement ->
                keyArrangement.copy(keys = keyArrangement.keys.map { k -> if (k == oldKey) newKey else k })
            }
            rowData.copy(row = newRow)
        }
        return keyboard.copy(arrangement = newArrangement)
    }

    Scaffold(
        topBar = { /* ... */ },
    ) { paddingValues ->
        if (keyboardData == null) {
            // ...
        } else {
            Column(
                modifier = Modifier.padding(paddingValues).fillMaxSize()
            ) {
                // Toolbar Preview (New)
                ToolbarPreview(keyboardData!!.toolbar ?: emptyList()) { key ->
                    selectedKeyForEdit = key
                }

                // Keyboard Preview
                Box(modifier = Modifier.weight(1f)) { /* ... */ }
                // Property Editor
                Box(modifier = Modifier.weight(1f)) {
                    selectedKeyForEdit?.let { selectedKey ->
                        KeyEditor(
                            keyData = selectedKey,
                            onKeyUpdated = { updatedKey ->
                                keyboardData = if (keyboardData!!.toolbar?.contains(selectedKey) == true) {
                                    val newToolbar = keyboardData!!.toolbar?.map { if (it == selectedKey) updatedKey else it }
                                    keyboardData!!.copy(toolbar = newToolbar)
                                } else {
                                    updateKey(keyboardData!!, selectedKey, updatedKey)
                                }
                                selectedKeyForEdit = updatedKey
                            },
                            onAddToolbarKey = { 
                                val newKey = LayoutKeyData(type = "character", value = "new", label = "new")
                                val newToolbar = (keyboardData!!.toolbar ?: emptyList()) + newKey
                                keyboardData = keyboardData!!.copy(toolbar = newToolbar)
                            },
                            onDeleteToolbarKey = { keyToDelete ->
                                val newToolbar = keyboardData!!.toolbar?.filterNot { it == keyToDelete }
                                keyboardData = keyboardData!!.copy(toolbar = newToolbar)
                                if (selectedKeyForEdit == keyToDelete) selectedKeyForEdit = null
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolbarPreview(toolbarKeys: List<LayoutKeyData>, onKeyClick: (LayoutKeyData) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().background(Color.DarkGray).padding(4.dp)) {
        toolbarKeys.forEach { key ->
            Box(
                modifier = Modifier.weight(1f).height(48.dp).padding(2.dp)
                    .background(Color.White).clickable { onKeyClick(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(key.label ?: "")
            }
        }
    }
}

@Composable
fun KeyEditor(
    keyData: LayoutKeyData,
    onKeyUpdated: (LayoutKeyData) -> Unit,
    onAddToolbarKey: () -> Unit,
    onDeleteToolbarKey: (LayoutKeyData) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        // ... (Key Type, Value, Width editor)

        // Toolbar Editor (New)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Toolbar Actions", style = MaterialTheme.typography.titleMedium)
        Row {
            Button(onClick = onAddToolbarKey) { Text("Add Toolbar Key") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { onDeleteToolbarKey(keyData) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Delete This Key") }
        }
    }
}

// ... (rest of the file)
