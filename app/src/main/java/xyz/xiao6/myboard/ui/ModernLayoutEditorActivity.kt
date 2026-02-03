package xyz.xiao6.myboard.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.EditorKeyCategory
import xyz.xiao6.myboard.model.EditorKeyType
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.KeyboardRow
import xyz.xiao6.myboard.model.LayoutDefaults
import xyz.xiao6.myboard.model.LayoutPadding
import xyz.xiao6.myboard.model.RowAlignment
import xyz.xiao6.myboard.model.RowType
import xyz.xiao6.myboard.model.LayoutMetadata
import xyz.xiao6.myboard.model.LayoutFeatures
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme
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
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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
    val haptic = LocalHapticFeedback.current
    var layoutName by remember { mutableStateOf(existingLayoutId ?: "") }
    var systemLayouts by remember { mutableStateOf<List<KeyboardLayout>>(emptyList()) }
    var showImportDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        systemLayouts = try {
            layoutManager.loadAll()
            layoutManager.listLayoutIds().mapNotNull { layoutManager.getLayout(it) }
        } catch (e: Exception) { emptyList() }
    }

    var selectedKeys by remember { mutableStateOf(setOf<GridPosition>()) }
    var selectedRows by remember { mutableStateOf(setOf<Int>()) }
    var selectionMode by remember { mutableStateOf<SelectionMode?>(null) }

    // Default QWERTY layout
    var rows by remember { mutableStateOf(createDefaultQwertyRows()) }
    var showKeyPalette by remember { mutableStateOf(true) }
    var selectedCategory by remember { mutableStateOf(EditorKeyCategory.CHARACTER) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var showKeyEditDialog by remember { mutableStateOf<Pair<GridPosition, LayoutEditorKey>?>(null) }
    var showCharPicker by remember { mutableStateOf<((String) -> Unit)?>(null) }
    val maxColumns = rows.maxOfOrNull { it.keys.size } ?: 10
    val hasSelection = selectedKeys.isNotEmpty() || selectedRows.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(if (existingLayoutId.isNullOrBlank()) "Create Layout" else "Edit Layout", 
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        Text(localeTag, style = MaterialTheme.typography.bodySmall, 
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Outlined.Create, contentDescription = "Import")
                    }
                    if (layoutName.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { showSaveDialog = true },
                            modifier = Modifier.padding(end = 16.dp)
                        ) {
                            Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (hasSelection) {
                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedKeys = setOf()
                        selectedRows = setOf()
                        selectionMode = null
                    },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Clear Selection")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = layoutName,
                    onValueChange = { layoutName = it },
                    label = { Text("Layout Name") },
                    placeholder = { Text("e.g., My Custom QWERTY or T9") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            item {
                ModernKeyboardGrid(
                    rows = rows,
                    selectedKeys = selectedKeys,
                    selectedRows = selectedRows,
                    onKeyClick = { position, key ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (key == null) {
                            showCharPicker = { char ->
                                rows[position.row].keys.add(position.col, 
                                    LayoutEditorKey(EditorKeyType.TEXT, char, char, ""))
                                showCharPicker = null
                            }
                        } else {
                            if (selectedKeys.contains(position)) {
                                showKeyEditDialog = position to key
                            } else {
                                selectedKeys = selectedKeys + position
                                selectionMode = SelectionMode.CELL
                                selectedRows = setOf()
                            }
                        }
                    },
                    onKeyLongClick = { position, key ->
                        if (key != null) showKeyEditDialog = position to key
                    },
                    onRowHeaderClick = { rowIndex ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        selectedRows = if (selectedRows.contains(rowIndex)) selectedRows - rowIndex else selectedRows + rowIndex
                        selectionMode = SelectionMode.ROW
                        selectedKeys = setOf()
                    },
                    onAddRow = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        rows = rows + LayoutEditorRow(rows.size, mutableStateListOf())
                    },
                    onDeleteSelected = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        if (selectedRows.isNotEmpty() && rows.size > 1) {
                            val newRows = rows.toMutableList()
                            selectedRows.sortedDescending().forEach { idx ->
                                if (newRows.size > 1) newRows.removeAt(idx)
                            }
                            rows = newRows
                            selectedRows = setOf()
                        }
                        if (selectedKeys.isNotEmpty()) {
                            selectedKeys.sortedByDescending { it.row * 100 + it.col }.forEach { pos ->
                                rows.getOrNull(pos.row)?.keys?.let { 
                                    if (pos.col < it.size) it.removeAt(pos.col) 
                                }
                            }
                            selectedKeys = setOf()
                        }
                        selectionMode = null
                    },
                    canDelete = rows.size > 1 || selectedKeys.isNotEmpty()
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                ModernKeyPalette(
                    selectedCategory = selectedCategory,
                    onCategorySelect = { selectedCategory = it },
                    onKeyTypeSelect = { keyType ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (keyType == EditorKeyType.TEXT) {
                            showCharPicker = { char ->
                                addKeyToFirstEmptySlot(rows, maxColumns, keyType, char)
                                showCharPicker = null
                            }
                        } else {
                            val label = when {
                                keyType.defaultLabel.isNotEmpty() -> keyType.defaultLabel
                                keyType == EditorKeyType.SPACE -> "Space"
                                else -> keyType.name
                            }
                            val token = when (keyType) {
                                EditorKeyType.SPACE -> " "
                                EditorKeyType.ENTER -> "\n"
                                else -> ""
                            }
                            addKeyToFirstEmptySlot(rows, maxColumns, keyType, label, token)
                        }
                    },
                    expanded = showKeyPalette,
                    onToggleExpand = { showKeyPalette = !showKeyPalette }
                )
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showImportDialog) {
        LayoutImportDialog(
            layouts = systemLayouts,
            onDismiss = { showImportDialog = false },
            onImport = { layout ->
                layoutName = layout.name ?: layout.layoutId
                rows = layout.rows.mapIndexed { idx, row ->
                    LayoutEditorRow(idx, mutableStateListOf<LayoutEditorKey>().apply {
                        addAll(row.keys.map { key ->
                            LayoutEditorKey(
                                type = inferKeyType(key),
                                label = key.ui.label ?: key.label ?: "",
                                token = key.primaryCode.toChar().takeIf { it != '\u0000' }?.toString() ?: "",
                                hint = key.hints.values.firstOrNull() ?: "",
                                widthWeight = key.ui.widthWeight
                            )
                        })
                    })
                }
                showImportDialog = false
            }
        )
    }

    if (showSaveDialog) {
        ModernSaveDialog(
            layoutName = layoutName,
            onDismiss = { showSaveDialog = false },
            onConfirm = {
                try {
                    val keyboardLayout = buildKeyboardLayoutFromEditor(layoutName, localeTag, rows)
                    writeUserLayoutSpec(context, keyboardLayout)
                    prefs.setCustomLayoutIds(localeTag, (prefs.getCustomLayoutIds(localeTag) + keyboardLayout.layoutId).distinct())
                    prefs.setEnabledLayoutIds(localeTag, (prefs.getEnabledLayoutIds(localeTag) + keyboardLayout.layoutId).distinct())
                    showSaveDialog = false
                    onBack()
                } catch (e: Exception) {
                    saveError = e.message
                }
            },
            error = saveError
        )
    }

    showKeyEditDialog?.let { (position, key) ->
        AdvancedKeyEditDialog(
            key = key,
            onDismiss = { showKeyEditDialog = null },
            onConfirm = { updatedKey ->
                rows.getOrNull(position.row)?.keys?.set(position.col, updatedKey)
                showKeyEditDialog = null
            },
            onShowCharPicker = { callback -> showCharPicker = callback }
        )
    }

    showCharPicker?.let { callback ->
        CharacterPickerDialog(onDismiss = { showCharPicker = null }, onSelect = callback)
    }
}

private fun addKeyToFirstEmptySlot(
    rows: List<LayoutEditorRow>, 
    maxColumns: Int, 
    keyType: EditorKeyType, 
    label: String,
    token: String = label
) {
    for (rowIndex in rows.indices) {
        val row = rows[rowIndex]
        for (colIndex in 0 until maxOf(maxColumns, row.keys.size + 1)) {
            if (row.keys.getOrNull(colIndex) == null) {
                row.keys.add(colIndex, LayoutEditorKey(keyType, label, token, "", widthWeight = keyType.widthWeight))
                return
            }
        }
    }
}

private fun createDefaultQwertyRows(): List<LayoutEditorRow> {
    return listOf(
        // Row 0: Alpha with number hints (q->1, w->2, etc.)
        LayoutEditorRow(
            rowIndex = 0,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                val hints = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
                addAll("qwertyuiop".mapIndexed { index, char ->
                    LayoutEditorKey(
                        type = EditorKeyType.TEXT,
                        label = char.toString(),
                        token = char.toString(),
                        hint = hints.getOrNull(index) ?: ""
                    )
                })
            },
            rowType = RowType.ALPHA
        ),
        // Row 1: Normal alpha
        LayoutEditorRow(
            rowIndex = 1,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll("asdfghjkl".map { LayoutEditorKey(EditorKeyType.TEXT, it.toString(), it.toString(), "") })
            },
            rowType = RowType.ALPHA
        ),
        // Row 2: Alpha with function keys
        LayoutEditorRow(
            rowIndex = 2,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.SHIFT, "⇧", "", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "z", "z", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "x", "x", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "c", "c", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "v", "v", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "b", "b", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "n", "n", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "m", "m", ""),
                    LayoutEditorKey(EditorKeyType.BACKSPACE, "⌫", "", ""),
                ))
            },
            rowType = RowType.ALPHA
        ),
        // Row 3: Function row
        LayoutEditorRow(
            rowIndex = 3,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.LAYER_SWITCH, "?123", "", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, ",", ",", ""),
                    LayoutEditorKey(EditorKeyType.SPACE, "", " ", "", widthWeight = 4f),
                    LayoutEditorKey(EditorKeyType.TEXT, ".", ".", ""),
                    LayoutEditorKey(EditorKeyType.ENTER, "⏎", "\n", ""),
                ))
            },
            rowType = RowType.FUNCTION
        )
    )
}

private fun createDefaultT9Rows(): List<LayoutEditorRow> {
    return listOf(
        // Row 0: T9 first row
        LayoutEditorRow(
            rowIndex = 0,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.TEXT, ",", ",", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "分词", "1", "1"),
                    LayoutEditorKey(EditorKeyType.TEXT, "ABC", "abc", "2"),
                    LayoutEditorKey(EditorKeyType.TEXT, "DEF", "def", "3"),
                    LayoutEditorKey(EditorKeyType.BACKSPACE, "⌫", "", ""),
                ))
            },
            rowType = RowType.ALPHA
        ),
        // Row 1: T9 second row
        LayoutEditorRow(
            rowIndex = 1,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.TEXT, "。", "。", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "GHI", "ghi", "4"),
                    LayoutEditorKey(EditorKeyType.TEXT, "JKL", "jkl", "5"),
                    LayoutEditorKey(EditorKeyType.TEXT, "MNO", "mno", "6"),
                    LayoutEditorKey(EditorKeyType.TEXT, "重输", "", ""),
                ))
            },
            rowType = RowType.ALPHA
        ),
        // Row 2: T9 third row
        LayoutEditorRow(
            rowIndex = 2,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.TEXT, "?", "?", ""),
                    LayoutEditorKey(EditorKeyType.TEXT, "PQRS", "pqrs", "7"),
                    LayoutEditorKey(EditorKeyType.TEXT, "TUV", "tuv", "8"),
                    LayoutEditorKey(EditorKeyType.TEXT, "WXYZ", "wxyz", "9"),
                    LayoutEditorKey(EditorKeyType.TEXT, "0", "0", ""),
                ))
            },
            rowType = RowType.ALPHA
        ),
        // Row 3: Function row with symbol access
        LayoutEditorRow(
            rowIndex = 3,
            keys = mutableStateListOf<LayoutEditorKey>().apply {
                addAll(listOf(
                    LayoutEditorKey(EditorKeyType.LAYER_SWITCH, "符", "", ""),
                    LayoutEditorKey(EditorKeyType.LAYER_SWITCH, "123", "", ""),
                    LayoutEditorKey(EditorKeyType.SPACE, "", " ", "", widthWeight = 2f),
                    LayoutEditorKey(EditorKeyType.TEXT, "中/英", "", ""),
                    LayoutEditorKey(EditorKeyType.ENTER, "搜索", "\n", ""),
                ))
            },
            rowType = RowType.FUNCTION
        )
    )
}

@Composable
private fun LayoutImportDialog(
    layouts: List<KeyboardLayout>,
    onDismiss: () -> Unit,
    onImport: (KeyboardLayout) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Import from System Layout", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Select a layout to use as a starting point", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                if (layouts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No system layouts available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 300.dp)) {
                        items(layouts) { layout ->
                            Card(
                                modifier = Modifier.fillMaxWidth().clickable { onImport(layout) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Outlined.Create, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(layout.name ?: layout.layoutId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                        Text(layout.layoutId, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("${layout.rows.size} rows · ${layout.rows.sumOf { it.keys.size }} keys", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun CharacterPickerDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    
    val categories = listOf(
        "Lowercase" to ('a'..'z').map { it.toString() },
        "Uppercase" to ('A'..'Z').map { it.toString() },
        "Digits" to ('0'..'9').map { it.toString() },
        "Symbols" to listOf("!", "@", "#", "$", "%", "^", "&", "*", "(", ")", "-", "+", "=", "[", "]", "{", "}", "|", ";", ":", "'", "\"", ",", ".", "<", ">", "/", "?", "`", "~", "\\"),
        "Currency" to listOf("$", "€", "£", "¥", "₩", "₹", "¢", "¤"),
        "Math" to listOf("+", "-", "×", "÷", "=", "≠", "≈", "<", ">", "≤", "≥", "±", "∞", "√", "π", "°"),
        "Arrows" to listOf("←", "↑", "→", "↓", "↔", "↕", "⇐", "⇑", "⇒", "⇓", "⇔", "↖", "↗", "↘", "↙"),
    )
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 600.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Select Character", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        Text("Tap to select for your key", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick = onDismiss) { Icon(Icons.Filled.Done, contentDescription = "Done") }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search characters...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                if (searchQuery.isNotBlank()) {
                    val results = categories.flatMap { it.second }.filter { it.contains(searchQuery, ignoreCase = true) }.distinct()
                    if (results.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text("No characters found") }
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            results.forEach { CharButton(char = it, onClick = { onSelect(it) }) }
                        }
                    }
                } else {
                    ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                        categories.forEachIndexed { index, (name, _) ->
                            Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(name) })
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories[selectedTab].second.forEach { CharButton(char = it, onClick = { onSelect(it) }) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CharButton(char: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    
    Box(
        modifier = Modifier.size(48.dp).scale(scale).clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = char, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AdvancedKeyEditDialog(
    key: LayoutEditorKey,
    onDismiss: () -> Unit,
    onConfirm: (LayoutEditorKey) -> Unit,
    onShowCharPicker: ((String) -> Unit) -> Unit
) {
    var label by remember { mutableStateOf(key.label) }
    var token by remember { mutableStateOf(key.token) }
    var hint by remember { mutableStateOf(key.hint) }
    var longPress by remember { mutableStateOf(key.longPress) }
    var widthWeight by remember { mutableStateOf(key.widthWeight) }
    
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.fillMaxWidth(0.95f).heightIn(max = 700.dp), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Edit Key", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                Text("Configure display label, output token, hints and more", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Display Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { IconButton(onClick = { onShowCharPicker { label = it } }) { Icon(Icons.Filled.Search, contentDescription = "Pick") } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Output Token") },
                    placeholder = { Text("For T9: enter multiple letters like \"abc\"") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { IconButton(onClick = { onShowCharPicker { token = it } }) { Icon(Icons.Filled.Search, contentDescription = "Pick") } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("For T9: enter multiple letters like \"abc\" for multi-tap cycling", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = hint,
                    onValueChange = { hint = it },
                    label = { Text("Hint / Secondary Label (e.g., number for T9)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { IconButton(onClick = { onShowCharPicker { hint = it } }) { Icon(Icons.Filled.Search, contentDescription = "Pick") } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = longPress,
                    onValueChange = { longPress = it },
                    label = { Text("Long Press Output") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = { IconButton(onClick = { onShowCharPicker { longPress = it } }) { Icon(Icons.Filled.Search, contentDescription = "Pick") } }
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Key Width: ${String.format("%.1f", widthWeight)}x", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Slider(value = widthWeight, onValueChange = { widthWeight = it }, valueRange = 0.5f..5f, steps = 8)
                Spacer(modifier = Modifier.height(8.dp))
                
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Preview", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.width((64 * widthWeight).dp).height(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(label.take(3), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (hint.isNotBlank()) Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = { onConfirm(key.copy(label = label, token = token, hint = hint, longPress = longPress, widthWeight = widthWeight)) }) { Text("Save Key") }
                }
            }
        }
    }
}

// Include remaining composables (ModernKeyboardGrid, AnimatedKeyCell, ModernKeyPalette, ModernSaveDialog, FlowRow) from previous implementation
// Due to length, these are kept the same as before

@Composable
private fun ModernKeyboardGrid(
    rows: List<LayoutEditorRow>,
    selectedKeys: Set<GridPosition>,
    selectedRows: Set<Int>,
    onKeyClick: (GridPosition, LayoutEditorKey?) -> Unit,
    onKeyLongClick: (GridPosition, LayoutEditorKey?) -> Unit,
    onRowHeaderClick: (Int) -> Unit,
    onAddRow: () -> Unit,
    onDeleteSelected: () -> Unit,
    canDelete: Boolean
) {
    val maxColumns = rows.maxOfOrNull { it.keys.size } ?: 10
    val haptic = LocalHapticFeedback.current

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Keyboard Layout", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = onAddRow, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Row")
                    }
                    if (canDelete && selectedKeys.isNotEmpty()) {
                        FilledTonalIconButton(onClick = onDeleteSelected, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete Selected")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEachIndexed { rowIndex, row ->
                    val isRowSelected = selectedRows.contains(rowIndex)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val interactionSource = remember { MutableInteractionSource() }
                        val rowScale by animateFloatAsState(targetValue = if (isRowSelected) 1.1f else 1f, animationSpec = spring(stiffness = Spring.StiffnessMedium))
                        Box(modifier = Modifier.size(36.dp).scale(rowScale).clip(CircleShape).background(if (isRowSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable { onRowHeaderClick(rowIndex) }, contentAlignment = Alignment.Center) {
                            Text((rowIndex + 1).toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = if (isRowSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(max(maxColumns, row.keys.size + 1)) { colIndex ->
                                val position = GridPosition(rowIndex, colIndex)
                                val isSelected = selectedKeys.contains(position)
                                val key = row.keys.getOrNull(colIndex)
                                AnimatedKeyCell(position = position, key = key, isSelected = isSelected, isRowSelected = isRowSelected, onClick = { onKeyClick(position, key) }, onLongClick = { onKeyLongClick(position, key) })
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedKeys.isNotEmpty() || selectedRows.isNotEmpty(), enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                                text = when {
                                    selectedKeys.isNotEmpty() -> "${selectedKeys.size} key(s) selected"
                                    selectedRows.isNotEmpty() -> "${selectedRows.size} row(s) selected"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        TextButton(onClick = onDeleteSelected) { Text("Delete") }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedKeyCell(position: GridPosition, key: LayoutEditorKey?, isSelected: Boolean, isRowSelected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.9f else if (isSelected) 1.05f else 1f, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            isRowSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            key != null -> MaterialTheme.colorScheme.surface
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isRowSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            key != null -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(200)
    )

    Box(
        modifier = Modifier.size(44.dp).scale(scale).clip(RoundedCornerShape(12.dp)).background(backgroundColor).border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp)).clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        key?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = it.label.take(3), style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium), color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
                if (it.hint.isNotBlank()) Text(text = it.hint, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernKeyPalette(selectedCategory: EditorKeyCategory, onCategorySelect: (EditorKeyCategory) -> Unit, onKeyTypeSelect: (EditorKeyType) -> Unit, expanded: Boolean, onToggleExpand: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(text = "Key Palette", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Text(text = "Tap to add keys to layout", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                IconButton(onClick = onToggleExpand) { Icon(if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = if (expanded) "Collapse" else "Expand") }
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(), exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(EditorKeyCategory.entries, key = { it.name }) { category ->
                            val isSelected = category == selectedCategory
                            FilterChip(onClick = { onCategorySelect(category) }, label = { Text(category.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) }, selected = isSelected, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MaterialTheme.colorScheme.primary, selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    val keysInCategory = EditorKeyType.groupedByCategory()[selectedCategory] ?: emptyList()
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        keysInCategory.forEach { keyType ->
                            val displayText = when {
                                keyType.defaultLabel.isNotEmpty() -> keyType.defaultLabel
                                keyType == EditorKeyType.SPACE -> "Space"
                                else -> keyType.name.replace("_", " ")
                            }
                            AssistChip(onClick = { onKeyTypeSelect(keyType) }, label = { Text(displayText) }, leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }, colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernSaveDialog(layoutName: String, onDismiss: () -> Unit, onConfirm: () -> Unit, error: String?) {
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Done, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(28.dp))
                }
                Column {
                    Text(text = "Save Layout", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "\"${layoutName}\" will be saved to your custom layouts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (error != null) {
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.errorContainer) {
                        Text(text = error, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = onConfirm) { Text("Save Layout") }
                }
            }
        }
    }
}

@Composable
private fun FlowRow(modifier: Modifier = Modifier, horizontalArrangement: Arrangement.Horizontal = Arrangement.Start, verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val hGapPx = 8.dp.roundToPx()
        val vGapPx = 8.dp.roundToPx()
        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        val rowWidths = mutableListOf<Int>()
        val rowHeights = mutableListOf<Int>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0
        var currentRowHeight = 0
        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints)
            if (currentRow.isNotEmpty() && currentRowWidth + hGapPx + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                rowWidths.add(currentRowWidth)
                rowHeights.add(currentRowHeight)
                currentRow = mutableListOf()
                currentRowWidth = 0
                currentRowHeight = 0
            }
            currentRow.add(placeable)
            currentRowWidth += if (currentRow.size == 1) placeable.width else hGapPx + placeable.width
            currentRowHeight = maxOf(currentRowHeight, placeable.height)
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
            rowWidths.add(currentRowWidth)
            rowHeights.add(currentRowHeight)
        }
        val width = constraints.maxWidth
        val height = rowHeights.sum() + (rows.size - 1).coerceAtLeast(0) * vGapPx
        layout(width, height) {
            var y = 0
            rows.forEachIndexed { index, row ->
                var x = when (horizontalArrangement) {
                    Arrangement.End -> width - rowWidths[index]
                    Arrangement.Center -> (width - rowWidths[index]) / 2
                    else -> 0
                }
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + hGapPx
                }
                y += rowHeights[index] + vGapPx
            }
        }
    }
}

private fun inferKeyType(key: Key): EditorKeyType = when {
    key.primaryCode == 32 -> EditorKeyType.SPACE
    key.primaryCode == 10 -> EditorKeyType.ENTER
    key.primaryCode == -5 -> EditorKeyType.BACKSPACE
    key.primaryCode == -1 -> EditorKeyType.SHIFT
    key.primaryCode == -2 -> EditorKeyType.LAYER_SWITCH
    key.ui.styleId.contains("function") -> EditorKeyType.LAYER_SWITCH
    else -> EditorKeyType.TEXT
}

data class LayoutEditorRow(
    val rowIndex: Int,
    val keys: MutableList<LayoutEditorKey>,
    val rowType: RowType = RowType.ALPHA,
    val rowStyleId: String? = null,
)

data class LayoutEditorKey(
    val type: EditorKeyType,
    val label: String = "",
    val token: String = "",
    val hint: String = "",
    val longPress: String = "",
    val widthWeight: Float = 1f,
)
data class GridPosition(val row: Int, val col: Int)
enum class SelectionMode { CELL, ROW, COLUMN, ALL }

@OptIn(ExperimentalSerializationApi::class)
private val layoutJson = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

private fun sanitizeLayoutFileName(layoutId: String): String {
    val normalized = layoutId.trim().lowercase(Locale.ROOT)
    return normalized.replace(Regex("[^a-z0-9._-]"), "_").ifBlank { "layout_${System.currentTimeMillis()}" }
}

private fun writeUserLayoutSpec(context: android.content.Context, layout: KeyboardLayout) {
    val dir = LayoutManager(context).getUserLayoutDir()
    val file = File(dir, "${sanitizeLayoutFileName(layout.layoutId)}.json")
    file.writeText(layoutJson.encodeToString(KeyboardLayout.serializer(), layout), Charsets.UTF_8)
}

private fun buildKeyboardLayoutFromEditor(layoutName: String, localeTag: String, rows: List<LayoutEditorRow>): KeyboardLayout {
    val layoutId = layoutName.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "_")
    
    // 推断布局类型
    val isT9 = rows.any { row -> 
        row.keys.any { it.token.length > 1 && it.token.all { c -> c.isLetter() } }
    }
    val hasNumberRow = rows.any { it.rowType == RowType.NUMBER }
    
    val keyboardRows = rows.mapIndexed { rowIndex, editorRow ->
        val keys = editorRow.keys.mapIndexed { colIndex, editorKey ->
            xyz.xiao6.myboard.model.Key(
                keyId = "key_${rowIndex}_${colIndex}",
                primaryCode = editorKey.token.firstOrNull()?.code ?: if (editorKey.type == EditorKeyType.SPACE) 32 else if (editorKey.type == EditorKeyType.ENTER) 10 else 0,
                label = editorKey.label,
                hints = if (editorKey.hint.isNotBlank()) mapOf("BOTTOM_CENTER" to editorKey.hint) else emptyMap(),
                ui = xyz.xiao6.myboard.model.KeyUI(
                    label = null, 
                    styleId = editorKey.type.styleId, 
                    gridPosition = xyz.xiao6.myboard.model.GridPosition(startCol = colIndex, startRow = rowIndex, spanCols = 1), 
                    widthWeight = editorKey.widthWeight
                ),
                actions = emptyMap()
            )
        }
        KeyboardRow(
            rowId = "row_$rowIndex", 
            heightRatio = 1f, 
            alignment = RowAlignment.JUSTIFY,
            rowType = editorRow.rowType,
            rowStyleId = editorRow.rowStyleId,

            keys = keys
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
            padding = LayoutPadding(topDp = 6f, bottomDp = 6f, leftDp = 6f, rightDp = 6f)
        ),
        metadata = LayoutMetadata(
            layoutType = if (isT9) "t9" else if (hasNumberRow) "qwerty_with_numbers" else "qwerty",
            defaultEngine = if (isT9) "ZH_PINYIN" else "EN_DIRECT"
        ),
        features = LayoutFeatures(
            showHints = true,
            showLabels = true,
            enableLongPress = true
        ),
        rows = keyboardRows
    )
}
