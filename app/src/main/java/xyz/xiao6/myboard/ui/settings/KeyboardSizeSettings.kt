package xyz.xiao6.myboard.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.R
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.util.KeyboardSizeConstraints
import java.util.Locale

private enum class KeyboardSizeHandle {
    NONE,
    LEFT,
    RIGHT,
    TOP,
    BOTTOM,
}

@Composable
fun KeyboardSizeSettings(
    modifier: Modifier = Modifier,
    prefs: SettingsStore,
    layoutManager: LayoutManager,
) {
    val defaultLayout = remember { layoutManager.getDefaultLayout(Locale.getDefault()) }
    val defaultWidthRatio = defaultLayout.totalWidthRatio.coerceIn(0.5f, 1.0f)
    val defaultHeightRatio = defaultLayout.totalHeightRatio.coerceIn(0.15f, 0.8f)
    val defaultWidthOffset = defaultLayout.totalWidthDpOffset
    val defaultHeightOffset = defaultLayout.totalHeightDpOffset

    // Keep ratio as-is (IME resize currently adjusts dp offset only).
    var widthRatio by remember { mutableStateOf(prefs.globalKeyboardWidthRatio ?: defaultWidthRatio) }
    var heightRatio by remember { mutableStateOf(prefs.globalKeyboardHeightRatio ?: defaultHeightRatio) }
    var widthOffset by remember { mutableStateOf(prefs.globalKeyboardWidthDpOffset ?: defaultWidthOffset) }
    var heightOffset by remember { mutableStateOf(prefs.globalKeyboardHeightDpOffset ?: defaultHeightOffset) }

    fun persist() {
        prefs.globalKeyboardWidthRatio = widthRatio
        prefs.globalKeyboardHeightRatio = heightRatio
        prefs.globalKeyboardWidthDpOffset = widthOffset
        prefs.globalKeyboardHeightDpOffset = heightOffset
    }

    // Keep Settings UI in sync with IME toolbar resizing (shared prefs are the single source of truth).
    DisposableEffect(prefs) {
        fun syncFromPrefs() {
            widthRatio = prefs.globalKeyboardWidthRatio ?: defaultWidthRatio
            heightRatio = prefs.globalKeyboardHeightRatio ?: defaultHeightRatio
            widthOffset = prefs.globalKeyboardWidthDpOffset ?: defaultWidthOffset
            heightOffset = prefs.globalKeyboardHeightDpOffset ?: defaultHeightOffset
        }

        // Ensure ratio exists once user starts customizing.
        if (prefs.globalKeyboardWidthRatio == null) prefs.globalKeyboardWidthRatio = widthRatio
        if (prefs.globalKeyboardHeightRatio == null) prefs.globalKeyboardHeightRatio = heightRatio

        val listener = prefs.addOnChangeListener { syncFromPrefs() }
        onDispose { prefs.removeOnChangeListener(listener) }
    }

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = stringResource(R.string.settings_keyboard_size_desc))
        Spacer(modifier = Modifier.height(12.dp))

        KeyboardSizePreview(
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            widthOffsetDp = widthOffset,
            heightOffsetDp = heightOffset,
            onChangeOffsets = { newW, newH ->
                widthOffset = newW
                heightOffset = newH
                persist()
            },
        )

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "ratio: w=${String.format(Locale.ROOT, "%.2f", widthRatio)} h=${String.format(Locale.ROOT, "%.2f", heightRatio)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "offset: w=${String.format(Locale.ROOT, "%.0f", widthOffset)}dp h=${String.format(Locale.ROOT, "%.0f", heightOffset)}dp",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = {
                    widthOffset = defaultWidthOffset
                    heightOffset = defaultHeightOffset
                    prefs.globalKeyboardWidthRatio = defaultWidthRatio
                    prefs.globalKeyboardHeightRatio = defaultHeightRatio
                    prefs.globalKeyboardWidthDpOffset = defaultWidthOffset
                    prefs.globalKeyboardHeightDpOffset = defaultHeightOffset
                },
            ) {
                Text(stringResource(R.string.settings_keyboard_size_reset_default))
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun KeyboardSizePreview(
    widthRatio: Float,
    heightRatio: Float,
    widthOffsetDp: Float,
    heightOffsetDp: Float,
    onChangeOffsets: (newWidthOffsetDp: Float, newHeightOffsetDp: Float) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Screen metrics in px, matching IME calculations (real pixels).
    val metrics = context.resources.displayMetrics
    val screenWidthPx = metrics.widthPixels.toFloat().coerceAtLeast(1f)
    val screenHeightPx = metrics.heightPixels.toFloat().coerceAtLeast(1f)
    val dpPx = with(density) { 1.dp.toPx() }.coerceAtLeast(0.5f)
    val topBarHeightPx = with(density) { 48.dp.toPx() }
    val minWidthPx = KeyboardSizeConstraints.minKeyboardWidthPx(metrics.density).toFloat()
    val minHeightPx = KeyboardSizeConstraints.minKeyboardHeightPx(metrics.density).toFloat()
    val maxKeyboardHeightPx =
        KeyboardSizeConstraints.maxKeyboardHeightPx(metrics.heightPixels, metrics.density).toFloat()
            .coerceAtLeast(minHeightPx)

    fun clampWidthOffsetDp(deltaWidthPx: Float, currentOffsetDp: Float): Float {
        val curPx = screenWidthPx * widthRatio + currentOffsetDp * dpPx
        val targetPx = (curPx + deltaWidthPx).coerceIn(minWidthPx, screenWidthPx)
        return (targetPx - screenWidthPx * widthRatio) / dpPx
    }

    fun clampHeightOffsetDp(deltaHeightPx: Float, currentOffsetDp: Float): Float {
        val curPx = screenHeightPx * heightRatio + currentOffsetDp * dpPx
        val targetPx = (curPx + deltaHeightPx).coerceIn(minHeightPx, maxKeyboardHeightPx)
        return (targetPx - screenHeightPx * heightRatio) / dpPx
    }

    val onChangeOffsetsLatest by rememberUpdatedState(onChangeOffsets)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).height(280.dp)) {
        val previewW = with(density) { maxWidth.toPx() }
        val previewH = with(density) { maxHeight.toPx() }
        val scale = kotlin.math.min(previewW / screenWidthPx, previewH / screenHeightPx).coerceIn(0.1f, 1f)

        val keyboardWidthPx = (screenWidthPx * widthRatio + widthOffsetDp * dpPx).coerceIn(minWidthPx, screenWidthPx)
        val keyboardHeightPx = (screenHeightPx * heightRatio + heightOffsetDp * dpPx).coerceIn(minHeightPx, maxKeyboardHeightPx)
        val panelWidthPx = keyboardWidthPx
        val panelHeightPx = keyboardHeightPx + topBarHeightPx

        val screenW = screenWidthPx * scale
        val screenH = screenHeightPx * scale
        val screenLeft = (previewW - screenW) / 2f
        val screenTop = (previewH - screenH) / 2f
        val screenRight = screenLeft + screenW
        val screenBottom = screenTop + screenH

        val panelW = panelWidthPx * scale
        val panelH = panelHeightPx * scale
        val panelLeft = screenLeft + (screenW - panelW) / 2f
        val panelTop = screenBottom - panelH
        val panelRight = panelLeft + panelW
        val panelBottom = panelTop + panelH

        val borderColor = Color(0xFF007AFF)
        val handleRadius = with(density) { 10.dp.toPx() }
        val handleHit = with(density) { 28.dp.toPx() }

        val left = panelLeft
        val top = panelTop
        val right = panelRight
        val bottom = panelBottom
        val cx = (left + right) / 2f
        val cy = (top + bottom) / 2f

        var activeHandle by remember { mutableStateOf(KeyboardSizeHandle.NONE) }

        fun pickHandleAt(p: Offset): KeyboardSizeHandle {
            fun near(a: Offset, b: Offset): Boolean {
                val dx = a.x - b.x
                val dy = a.y - b.y
                return (dx * dx + dy * dy) <= (handleHit * handleHit)
            }
            val leftP = Offset(left, cy)
            val rightP = Offset(right, cy)
            val topP = Offset(cx, top)
            val bottomP = Offset(cx, bottom)
            return when {
                near(p, leftP) -> KeyboardSizeHandle.LEFT
                near(p, rightP) -> KeyboardSizeHandle.RIGHT
                near(p, topP) -> KeyboardSizeHandle.TOP
                near(p, bottomP) -> KeyboardSizeHandle.BOTTOM
                else -> KeyboardSizeHandle.NONE
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(widthRatio, heightRatio, scale, screenWidthPx, screenHeightPx) {
                    var wDp = widthOffsetDp
                    var hDp = heightOffsetDp
                    detectDragGestures(
                        onDragStart = { start ->
                            activeHandle = pickHandleAt(start)
                            wDp = widthOffsetDp
                            hDp = heightOffsetDp
                        },
                        onDragEnd = { activeHandle = KeyboardSizeHandle.NONE },
                        onDragCancel = { activeHandle = KeyboardSizeHandle.NONE },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle == KeyboardSizeHandle.NONE) return@detectDragGestures
                            val dx = dragAmount.x / scale
                            val dy = dragAmount.y / scale
                            when (activeHandle) {
                                KeyboardSizeHandle.LEFT -> {
                                    wDp = clampWidthOffsetDp(-dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.RIGHT -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.TOP -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(-dy, hDp)
                                }
                                KeyboardSizeHandle.BOTTOM -> {
                                    wDp = clampWidthOffsetDp(dx, wDp)
                                    hDp = clampHeightOffsetDp(dy, hDp)
                                }
                                KeyboardSizeHandle.NONE -> Unit
                            }
                            onChangeOffsetsLatest(wDp, hDp)
                        },
                    )
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background.
                drawRoundRect(
                    color = Color(0xFFF2F2F7),
                    size = size,
                    cornerRadius = CornerRadius(with(density) { 12.dp.toPx() }),
                )

                // Phone screen frame (full device screen).
                val frameCorner = with(density) { 18.dp.toPx() }
                drawRoundRect(
                    color = Color(0xFF1C1C1E),
                    topLeft = Offset(screenLeft, screenTop),
                    size = Size(screenW, screenH),
                    cornerRadius = CornerRadius(frameCorner),
                )
                val inset = with(density) { 3.dp.toPx() }
                drawRoundRect(
                    color = Color(0xFFF9F9FB),
                    topLeft = Offset(screenLeft + inset, screenTop + inset),
                    size = Size((screenW - inset * 2).coerceAtLeast(1f), (screenH - inset * 2).coerceAtLeast(1f)),
                    cornerRadius = CornerRadius((frameCorner - inset).coerceAtLeast(0f)),
                )

                // Panel border.
                drawRoundRect(
                    color = borderColor,
                    topLeft = Offset(left, top),
                    size = Size(panelW, panelH),
                    cornerRadius = CornerRadius(with(density) { 14.dp.toPx() }),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = with(density) { 2.dp.toPx() }),
                )
                // Toolbar separator line.
                drawLine(
                    color = borderColor.copy(alpha = 0.35f),
                    start = Offset(left, top + topBarHeightPx * scale),
                    end = Offset(right, top + topBarHeightPx * scale),
                    strokeWidth = with(density) { 1.dp.toPx() },
                )
                // Handles as circles.
                drawCircle(borderColor, radius = handleRadius, center = Offset(left, cy))
                drawCircle(borderColor, radius = handleRadius, center = Offset(right, cy))
                drawCircle(borderColor, radius = handleRadius, center = Offset(cx, top))
                drawCircle(borderColor, radius = handleRadius, center = Offset(cx, bottom))
            }

            // Center hint text.
            Text(
                text = "拖动手柄调整",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
