package xyz.xiao6.myboard.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.EditorKeyCategory
import xyz.xiao6.myboard.model.EditorKeyType
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.KeyboardRow
import xyz.xiao6.myboard.model.LayoutDefaults
import xyz.xiao6.myboard.model.LayoutPadding
import xyz.xiao6.myboard.model.RowAlignment
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
import xyz.xiao6.myboard.R
import java.io.File
import java.util.Locale
import kotlin.math.max
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ModernLayoutEditorActivity : ComponentActivity() {
    private lateinit var layoutManager: LayoutManager
    private lateinit var prefs: SettingsStore
    private var localeTag: String = "en_US"
    private var existingLayoutId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localeTag = intent.getStringExtra("locale_tag") ?: "en_US"
        existingLayoutId = intent.getStringExtra("layout_id")

        layoutManager = LayoutManager(this)
        prefs = SettingsStore(this)

        setContent {
            MyBoardTheme {
                ModernLayoutEditorScreen(
                    onBack = { finish() },
                    localeTag = localeTag,
                    layoutManager = layoutManager,
                    prefs = prefs,
                    existingLayoutId = existingLayoutId,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernLayoutEditorScreen(
    onBack: () -> Unit,
    localeTag: String,
    layoutManager: LayoutManager,
    prefs: SettingsStore,
    existingLayoutId: String?,
) {
    val context = LocalContext.current
    var layoutName by remember { mutableStateOf(existingLayoutId ?: "") }
    var columns by remember { mutableStateOf(10) }

    var selectedKeys by remember { mutableStateOf(setOf<GridPosition>()) }
    var selectedRows by remember { mutableStateOf(setOf<Int>()) }
    var selectedColumns by remember { mutableStateOf(setOf<Int>()) }
    var selectionMode by remember { mutableStateOf<SelectionMode?>(null) }

    var rows by remember {
        mutableStateOf(
            List(4) { rowIndex ->
                LayoutEditorRow(
                    rowIndex = rowIndex,
                    keys = mutableStateListOf<LayoutEditorKey>().apply {
                        when (rowIndex) {
                            0 -> addAll("qwertyuiop".map { LayoutEditorKey(EditorKeyType.TEXT, it.toString()) })
                            1 -> addAll("asdfghjkl".map { LayoutEditorKey(EditorKeyType.TEXT, it.toString()) })
                            2 -> addAll("zxcvbnm".map { LayoutEditorKey(EditorKeyType.TEXT, it.toString()) })
                            3 -> listOf(
                                LayoutEditorKey(EditorKeyType.SHIFT, "⇧"),
                                LayoutEditorKey(EditorKeyType.TEXT, ","),
                                LayoutEditorKey(EditorKeyType.TEXT, " "),
                                LayoutEditorKey(EditorKeyType.TEXT, "."),
                                LayoutEditorKey(EditorKeyType.BACKSPACE, "⌫"),
                            )
                        }
                    }
                )
            }
        )
    }

    var showKeyPalette by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(EditorKeyCategory.CHARACTER) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    val totalRows = rows.size
    val maxColumns = rows.maxOfOrNull { it.keys.size } ?: columns

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existingLayoutId.isNullOrBlank()) "New Layout" else "Edit Layout") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = { showSaveDialog = true },
                        enabled = layoutName.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("Layout Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Layout Grid",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        rows.forEachIndexed { rowIndex, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val isRowSelected = selectedRows.contains(rowIndex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .clickable {
                                            selectedRows = if (isRowSelected) {
                                                selectedRows - rowIndex
                                            } else {
                                                selectedRows + rowIndex
                                            }
                                            selectionMode = SelectionMode.ROW
                                            selectedKeys = setOf()
                                            selectedColumns = setOf()
                                        }
                                        .background(
                                            if (isRowSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                Color.Transparent
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (rowIndex + 1).toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isRowSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(max(maxColumns, columns)) { colIndex ->
                                        val cellPos = GridPosition(rowIndex, colIndex)
                                        val isSelected = selectedKeys.contains(cellPos)
                                        val key = row.keys.getOrNull(colIndex)

                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when {
                                                        key == null -> Color.Transparent
                                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                                    }
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = when {
                                                        key == null -> Color.Transparent
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        else -> MaterialTheme.colorScheme.outlineVariant
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    if (key == null) {
                                                        val newKey = LayoutEditorKey(EditorKeyType.TEXT, "a")
                                                        row.keys.add(colIndex, newKey)
                                                    } else {
                                                        selectedKeys = if (isSelected) {
                                                            selectedKeys - cellPos
                                                        } else {
                                                            selectedKeys + cellPos
                                                        }
                                                    }
                                                    selectionMode = SelectionMode.CELL
                                                    selectedRows = setOf()
                                                    selectedColumns = setOf()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            key?.let {
                                                Text(
                                                    text = it.displayText,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontSize = 14.sp
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val newRow = LayoutEditorRow(rowIndex = rows.size, keys = mutableStateListOf())
                                    val newRows = rows.toMutableList()
                                    newRows.add(newRow)
                                    rows = newRows
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add Row")
                            }

                            if (selectedRows.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val sortedRows = selectedRows.sortedDescending()
                                        val newRows = rows.toMutableList()
                                        sortedRows.forEach { idx ->
                                            if (rows.size > 1) {
                                                newRows.removeAt(idx)
                                            }
                                        }
                                        rows = newRows
                                        selectedRows = setOf()
                                        selectionMode = null
                                    },
                                    enabled = rows.size > 1
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Row")
                                }
                            }

                            if (selectedKeys.isNotEmpty()) {
                                OutlinedButton(
                                    onClick = {
                                        val sortedKeys = selectedKeys.sortedBy { it.row }.toList()
                                        sortedKeys.forEach { pos ->
                                            val row = rows.getOrNull(pos.row)
                                            row?.keys?.let { keyList ->
                                                if (pos.col < keyList.size) {
                                                    keyList.removeAt(pos.col)
                                                }
                                            }
                                        }
                                        selectedKeys = setOf()
                                        selectionMode = null
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Key")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    selectedKeys = setOf()
                                    selectedRows = setOf()
                                    selectedColumns = setOf()
                                    selectionMode = null
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }
                }
            }

            item {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Key Palette",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showKeyPalette = !showKeyPalette }) {
                                Icon(
                                    imageVector = if (showKeyPalette) Icons.Filled.Delete else Icons.Filled.Add,
                                    contentDescription = "Toggle"
                                )
                            }
                        }

                        if (showKeyPalette) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(EditorKeyCategory.entries, key = { it.name }) { category ->
                                    val isSelected = category == selectedCategory
                                    FilterChip(
                                        onClick = { selectedCategory = category },
                                        label = { Text(category.name) },
                                        selected = isSelected
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val keysInCategory = EditorKeyType.groupedByCategory()[selectedCategory] ?: emptyList()
                                items(keysInCategory, key = { it.name }) { keyType ->
                                    val displayText = when {
                                        keyType.defaultLabel.isNotEmpty() -> keyType.defaultLabel
                                        keyType == EditorKeyType.SPACE -> "Space"
                                        else -> keyType.name.replace("_", " ")
                                    }

                                    AssistChip(
                                        onClick = {
                                            outer@ for (rowIndex in rows.indices) {
                                                val row = rows[rowIndex]
                                                for (colIndex in 0 until max(columns, row.keys.size + 1)) {
                                                    val pos = GridPosition(rowIndex, colIndex)
                                                    if (!selectedKeys.contains(pos) &&
                                                        row.keys.getOrNull(colIndex) == null) {
                                                        val newKey = LayoutEditorKey(keyType, displayText)
                                                        row.keys.add(colIndex, newKey)
                                                        break@outer
                                                    }
                                                }
                                            }
                                        },
                                        label = { Text(displayText) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Layout") },
            text = {
                Column {
                    Text("Are you sure you want to save this layout?")
                    if (saveError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = saveError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            val keyboardLayout = buildKeyboardLayoutFromEditor(
                                layoutName = layoutName,
                                localeTag = localeTag,
                                rows = rows,
                                columns = columns,
                            )

                            writeUserLayoutSpec(context, keyboardLayout)

                            val tag = localeTag
                            val nextCustom = (prefs.getCustomLayoutIds(tag) + keyboardLayout.layoutId).distinct()
                            prefs.setCustomLayoutIds(tag, nextCustom)
                            val nextEnabled = (prefs.getEnabledLayoutIds(tag) + keyboardLayout.layoutId).distinct()
                            prefs.setEnabledLayoutIds(tag, nextEnabled)
                            prefs.setPreferredLayoutId(tag, prefs.getPreferredLayoutId(tag) ?: keyboardLayout.layoutId)

                            showSaveDialog = false
                            onBack()
                        } catch (e: Exception) {
                            saveError = e.message
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class LayoutEditorRow(
    val rowIndex: Int,
    val keys: MutableList<LayoutEditorKey>
)

data class LayoutEditorKey(
    val type: EditorKeyType,
    val label: String = type.defaultLabel,
    val longPress: String = "",
    val widthWeight: Float = type.widthWeight
) {
    val displayText: String
        get() = when {
            label.isNotEmpty() -> label
            else -> when (type) {
                EditorKeyType.SPACE -> "Space"
                EditorKeyType.ENTER -> "Enter"
                EditorKeyType.BACKSPACE -> "⌫"
                EditorKeyType.SHIFT -> "⇧"
                else -> type.name.replace("_", " ")
            }
        }
}

data class GridPosition(
    val row: Int,
    val col: Int
)

enum class SelectionMode {
    CELL,
    ROW,
    COLUMN,
    ALL
}

@OptIn(ExperimentalSerializationApi::class)
private val layoutJson =
    Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

private fun sanitizeLayoutFileName(layoutId: String): String {
    val normalized = layoutId.trim().lowercase(Locale.ROOT)
    val sanitized = normalized.replace(Regex("[^a-z0-9._-]"), "_")
    return sanitized.ifBlank { "layout_${System.currentTimeMillis()}" }
}

private fun writeUserLayoutSpec(context: android.content.Context, layout: KeyboardLayout) {
    val dir = LayoutManager(context).getUserLayoutDir()
    val safeName = sanitizeLayoutFileName(layout.layoutId)
    val file = File(dir, "$safeName.json")
    val text = layoutJson.encodeToString(KeyboardLayout.serializer(), layout)
    file.writeText(text, Charsets.UTF_8)
}

private fun buildKeyboardLayoutFromEditor(
    layoutName: String,
    localeTag: String,
    rows: List<LayoutEditorRow>,
    columns: Int,
): KeyboardLayout {
    val layoutId = layoutName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")

    val keyboardRows = rows.mapIndexed { rowIndex, editorRow ->
        val keys = editorRow.keys.mapIndexed { colIndex, editorKey ->
            xyz.xiao6.myboard.model.Key(
                keyId = "key_${rowIndex}_${colIndex}",
                primaryCode = editorKey.type.primaryCode,
                label = editorKey.label,
                ui = xyz.xiao6.myboard.model.KeyUI(
                    label = null,
                    styleId = editorKey.type.styleId,
                    gridPosition = xyz.xiao6.myboard.model.GridPosition(
                        startCol = colIndex,
                        startRow = rowIndex,
                        spanCols = 1
                    ),
                    widthWeight = editorKey.widthWeight,
                ),
                actions = emptyMap(),
            )
        }

        xyz.xiao6.myboard.model.KeyboardRow(
            rowId = "row_$rowIndex",
            heightRatio = 1f,
            alignment = RowAlignment.JUSTIFY,
            keys = keys,
        )
    }

    return KeyboardLayout(
        layoutId = layoutId,
        name = layoutName,
        locale = listOf(localeTag),
        totalWidthRatio = 1.0f,
        totalHeightRatio = 0.25f,
        defaults = LayoutDefaults(
            horizontalGapDp = 4f,
            verticalGapDp = 5f,
            padding = LayoutPadding(topDp = 6f, bottomDp = 6f, leftDp = 6f, rightDp = 6f),
        ),
        rows = keyboardRows,
    )
}
