package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.manager.ToolbarManager
import xyz.xiao6.myboard.store.SettingsStore
import kotlin.math.abs

@Composable
fun ToolbarSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
    toolbarManager: ToolbarManager,
) {
    val toolbarSpec = remember(toolbarManager) { toolbarManager.getDefaultToolbar() }
    val defaultItems = remember(toolbarSpec) { buildToolbarSettingItems(toolbarSpec) }
    val orderedDefault = remember(defaultItems, prefs.toolbarItemOrder) {
        applyToolbarOrderForSettings(defaultItems, prefs.toolbarItemOrder)
    }
    var items by remember { mutableStateOf(orderedDefault) }
    var maxCount by remember { mutableStateOf(prefs.toolbarMaxVisibleCount.toFloat()) }
    var draggingItemId by remember { mutableStateOf<String?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }
    var itemHeightPx by remember { mutableStateOf(0) }
    val latestItems by rememberUpdatedState(items)

    LazyColumn(modifier = modifier, contentPadding = WindowInsets.systemBars.asPaddingValues()) {
        item { SectionHeader(textRes = R.string.settings_toolbar) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_toolbar_limit)) },
                supportingContent = {
                    val label =
                        if (maxCount.toInt() <= 0) {
                            stringResource(R.string.settings_toolbar_limit_unlimited)
                        } else {
                            stringResource(R.string.settings_toolbar_limit_fixed, maxCount.toInt())
                        }
                    Text(stringResource(R.string.settings_toolbar_limit_desc, label))
                },
            )
        }
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Slider(
                    value = maxCount,
                    onValueChange = {
                        maxCount = it
                        prefs.toolbarMaxVisibleCount = it.toInt()
                    },
                    valueRange = 0f..12f,
                )
            }
        }
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp)) }
        item { SectionHeader(textRes = R.string.settings_toolbar_sort) }
        items(items, key = { it.itemId }) { item ->
            val isDragging = draggingItemId == item.itemId
            ListItem(
                headlineContent = { Text(item.name) },
                supportingContent = { Text(item.itemId) },
                modifier = Modifier
                    .onSizeChanged { size ->
                        if (size.height > 0) itemHeightPx = size.height
                    }
                    .offset { IntOffset(0, if (isDragging) dragOffsetPx.toInt() else 0) }
                    .pointerInput(item.itemId, itemHeightPx) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingItemId = item.itemId
                                dragOffsetPx = 0f
                            },
                            onDragCancel = {
                                draggingItemId = null
                                dragOffsetPx = 0f
                            },
                            onDragEnd = {
                                draggingItemId = null
                                dragOffsetPx = 0f
                                prefs.toolbarItemOrder = items.map { it.itemId }
                            },
                            onDrag = { change: androidx.compose.ui.input.pointer.PointerInputChange, dragAmount: Offset ->
                                if (draggingItemId != item.itemId || itemHeightPx <= 0) return@detectDragGesturesAfterLongPress
                                dragOffsetPx += dragAmount.y
                                val move = (dragOffsetPx / itemHeightPx).toInt()
                                if (move != 0) {
                                    val from = latestItems.indexOfFirst { it.itemId == item.itemId }
                                    val to = (from + move).coerceIn(0, latestItems.lastIndex)
                                    if (from >= 0 && to != from) {
                                        val next = latestItems.toMutableList()
                                        val moving = next.removeAt(from)
                                        next.add(to, moving)
                                        items = next
                                        dragOffsetPx -= move * itemHeightPx
                                        prefs.toolbarItemOrder = next.map { it.itemId }
                                    }
                                }
                                change.consume()
                            },
                        )
                    },
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun SectionHeader(textRes: Int) {
    Text(
        text = stringResource(textRes),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    )
}

private data class ToolbarSettingItem(
    val itemId: String,
    val name: String,
)

private fun buildToolbarSettingItems(toolbarSpec: xyz.xiao6.myboard.model.ToolbarSpec?): List<ToolbarSettingItem> {
    val items = toolbarSpec?.items.orEmpty().filter { it.enabled }
    if (items.isNotEmpty()) {
        return items
            .sortedWith(compareByDescending<xyz.xiao6.myboard.model.ToolbarItemSpec> { it.priority }.thenBy { it.itemId })
            .map { ToolbarSettingItem(it.itemId, it.name) }
    }
    return listOf(
        ToolbarSettingItem("layout", "Layout"),
        ToolbarSettingItem("voice", "Voice"),
        ToolbarSettingItem("emoji", "Emoji"),
        ToolbarSettingItem("clipboard", "Clipboard"),
        ToolbarSettingItem("kb_resize", "Resize"),
        ToolbarSettingItem("settings", "Settings"),
    )
}

private fun applyToolbarOrderForSettings(
    items: List<ToolbarSettingItem>,
    order: List<String>,
): List<ToolbarSettingItem> {
    if (order.isEmpty()) return items
    val byId = items.associateBy { it.itemId }
    val ordered = order.mapNotNull { byId[it] }
    val orderSet = order.toSet()
    val remaining = items.filterNot { it.itemId in orderSet }
    return ordered + remaining
}
