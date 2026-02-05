package xyz.xiao6.myboard.ui.handwriting

import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.xiao6.myboard.manager.HandwritingRecognitionManager
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.model.HandwritingPosition
import xyz.xiao6.myboard.ui.theme.DesignTokens

/**
 * Full-screen or half-screen handwriting input panel
 * 全屏或半屏手写输入面板 - Modern Design matching design screenshots
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandwritingPanel(
    mode: HandwritingLayoutMode = HandwritingLayoutMode.HALF_SCREEN,
    position: HandwritingPosition = HandwritingPosition.BOTTOM,
    layoutSpec: HandwritingLayoutSpec? = null,
    recognitionManager: HandwritingRecognitionManager?,
    onBack: () -> Unit,
    onCandidateSelected: (String) -> Unit,
    onClear: () -> Unit,
    clearSignal: Int = 0,
    onLayoutPickerRequest: () -> Unit = {},
    onVoiceRequest: () -> Unit = {},
    onEmojiRequest: () -> Unit = {},
    onResizeRequest: () -> Unit = {},
    onBackspaceRequest: () -> Unit = {},
    onSwitchLayoutRequest: (String) -> Unit = {},
    onToggleLocaleRequest: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val candidates = remember { mutableStateListOf<String>() }
    var showCandidates by remember { mutableStateOf(false) }
    var isRecognizing by remember { mutableStateOf(false) }
    var strokeCount by remember { mutableIntStateOf(0) }
    
    // Track clear signal to trigger clears
    var lastClearSignal by remember { mutableIntStateOf(clearSignal) }
    
    val recognitionConfig = layoutSpec?.recognition
    val maxCandidates = recognitionConfig?.maxCandidates ?: 10
    val TRIGGER_RECOGNITION_SIGNAL = -1

    LaunchedEffect(strokeCount) {
        if (strokeCount == TRIGGER_RECOGNITION_SIGNAL) {
            isRecognizing = true
            val result = withContext(Dispatchers.Default) {
                recognitionManager?.recognize() ?: emptyList()
            }
            candidates.clear()
            candidates.addAll(result.take(maxCandidates))
            showCandidates = result.isNotEmpty()
            isRecognizing = false
        }
    }

    // Determine panel positioning and sizing
    val panelAlignment = when (mode) {
        HandwritingLayoutMode.FULL_SCREEN -> Alignment.Center
        HandwritingLayoutMode.HALF_SCREEN -> if (position == HandwritingPosition.TOP) Alignment.TopCenter else Alignment.BottomCenter
        HandwritingLayoutMode.OVERLAY -> Alignment.BottomCenter
    }

    val panelHeight = if (mode == HandwritingLayoutMode.FULL_SCREEN) 1f else 0.45f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (mode == HandwritingLayoutMode.FULL_SCREEN) Color.Transparent else Color.Black.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(panelHeight)
                .align(panelAlignment)
                .background(MaterialTheme.colorScheme.surface)
                .padding(4.dp)
        ) {
            // 1. Candidate Bar (Top)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp
            ) {
                if (showCandidates && candidates.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(candidates) { candidate ->
                            Text(
                                text = candidate,
                                modifier = Modifier
                                    .clickable {
                                        onCandidateSelected(candidate)
                                        candidates.clear()
                                        showCandidates = false
                                        onClear()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 20.sp
                            )
                        }
                    }
                } else {
                    // Toolbar icons when no candidates
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.layout_line, onClick = onLayoutPickerRequest)
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.keyboard_box_line, onClick = onBack)
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.mic_line, onClick = onVoiceRequest)
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.aspect_ratio_line, onClick = onResizeRequest)
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.emotion_line, onClick = onEmojiRequest)
                        ToolbarIcon(xyz.xiao6.myboard.R.drawable.arrow_down_wide_line, onClick = onBack)
                    }
                }
            }

            // 2. Main Content Area (Canvas + Sidebar)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .border(1.dp, Color.LightGray.copy(alpha = 0.2f))
                ) {
                    AndroidView(
                        factory = { context ->
                            HandwritingOverlayView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                recognitionManager?.let { setRecognitionManager(it) }
                                setBackgroundColor(android.graphics.Color.WHITE)
                                onStrokeCountChanged = { count ->
                                    strokeCount = count
                                }
                                onRecognitionResult = { result ->
                                    candidates.clear()
                                    candidates.addAll(result.take(maxCandidates))
                                    showCandidates = result.isNotEmpty()
                                }
                                layoutSpec?.let { applyConfig(it) }
                            }
                        },
                        update = { view ->
                            recognitionManager?.let { view.setRecognitionManager(it) }
                            layoutSpec?.let { view.applyConfig(it) }
                            view.onStrokeCountChanged = { count -> strokeCount = count }
                            if (clearSignal != lastClearSignal) {
                                view.clearCanvas()
                                lastClearSignal = clearSignal
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Sidebar
                Column(
                    modifier = Modifier
                        .width(70.dp)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SidebarButton(
                        modifier = Modifier.weight(1f),
                        icon = xyz.xiao6.myboard.R.drawable.delete_back_2_line,
                        onClick = {
                            if (strokeCount > 0 || candidates.isNotEmpty()) {
                                onClear()
                                candidates.clear()
                                showCandidates = false
                            } else {
                                onBackspaceRequest()
                            }
                        }
                    )
                    SidebarButton(
                        modifier = Modifier.weight(1f),
                        text = "ab",
                        onClick = { onToggleLocaleRequest() }
                    )
                    SidebarButton(
                        modifier = Modifier.weight(1f),
                        text = "^_^",
                        onClick = { onEmojiRequest() }
                    )
                }
            }

            // 3. Bottom Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BottomKey(text = "123", modifier = Modifier.weight(1.2f), onClick = { onSwitchLayoutRequest("numeric") })
                BottomKey(text = "符", modifier = Modifier.weight(1f), onClick = { onSwitchLayoutRequest("symbols") })
                BottomKey(text = ",", modifier = Modifier.weight(0.8f), onClick = { onCandidateSelected(",") })
                BottomKey(
                    icon = xyz.xiao6.myboard.R.drawable.ic_space,
                    modifier = Modifier.weight(1.5f),
                    onClick = { onCandidateSelected(" ") }
                )
                BottomKey(text = "。", modifier = Modifier.weight(0.8f), onClick = { onCandidateSelected("。") })
                BottomKey(text = "中/英", modifier = Modifier.weight(1.2f), onClick = { onToggleLocaleRequest() })
                BottomKey(
                    text = "完成",
                    modifier = Modifier.weight(1.2f),
                    isPrimary = true,
                    onClick = { onBack() }
                )
            }
        }
    }
}

@Composable
private fun ToolbarIcon(@DrawableRes id: Int, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) {
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = id),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SidebarButton(
    modifier: Modifier = Modifier,
    text: String? = null,
    @DrawableRes icon: Int? = null,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BottomKey(
    modifier: Modifier = Modifier,
    text: String? = null,
    @DrawableRes icon: Int? = null,
    isPrimary: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        shape = RoundedCornerShape(8.dp),
        color = if (isPrimary) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        onClick = onClick,
        border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            } else if (text != null) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun HandwritingCanvasView(
    layoutSpec: HandwritingLayoutSpec? = null,
    recognitionManager: HandwritingRecognitionManager?,
    modifier: Modifier = Modifier,
    onStrokeCountChanged: ((Int) -> Unit)? = null,
    onRecognitionResult: ((List<String>) -> Unit)? = null,
) {
    AndroidView(
        factory = { context ->
            HandwritingOverlayView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                recognitionManager?.let { setRecognitionManager(it) }
                layoutSpec?.let { applyConfig(it) }
                onStrokeCountChanged?.let { this.onStrokeCountChanged = it }
                onRecognitionResult?.let { this.onRecognitionResult = it }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}
