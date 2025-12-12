package xyz.xiao6.myboard.ui.keyboard

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.google.mlkit.vision.digitalink.recognition.Ink
import xyz.xiao6.myboard.data.KeyboardData as LayoutKeyboardData
import xyz.xiao6.myboard.data.KeyData as LayoutKeyData
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.model.ShiftState
import xyz.xiao6.myboard.data.voice.VoiceInputManager
import xyz.xiao6.myboard.ui.theme.LocalTheme
import xyz.xiao6.myboard.ui.theme.DefaultThemeData

@Composable
fun KeyboardScreen(viewModel: KeyboardViewModel = viewModel()) {
    val theme = LocalTheme.current ?: DefaultThemeData
    val keyboardLayout by viewModel.keyboardLayout.collectAsState(initial = null)
    val suggestions by viewModel.suggestions.collectAsState()
    val composingText by viewModel.composingText.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isEmojiMode by viewModel.isEmojiMode.collectAsState()
    val isClipboardMode by viewModel.isClipboardMode.collectAsState()
    val shiftState by viewModel.shiftState.collectAsState()
    val emojiData by viewModel.emojiData.collectAsState()
    val isFloatingMode by viewModel.isFloatingMode.collectAsState()
    val toolbarKeys by viewModel.toolbarKeys.collectAsState()
    val topBarState by viewModel.topBarState.collectAsState()
    val isChineseMode by viewModel.isChineseMode.collectAsState()
    val isDefaultCharactersLayout by viewModel.isDefaultCharactersLayout.collectAsState()
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenHeightDp = configuration.screenHeightDp.dp
    val screenWidthDp = configuration.screenWidthDp.dp
    val resolvedKeyboardHeight = viewModel.keyboardHeight.takeIf { it > 0 }?.dp ?: run {
        val fraction = if (isLandscape) 0.45f else 0.4f
        val minimum = if (isLandscape) 160.dp else 220.dp
        (screenHeightDp * fraction).coerceAtLeast(minimum)
    }.coerceAtMost(screenHeightDp * 0.55f)
    val resolvedKeyboardWidth = if (isLandscape && isFloatingMode) {
        screenWidthDp * 0.9f
    } else {
        screenWidthDp
    }
    val toolbarHeight = 48.dp
    val candidateCollapsedHeight = 52.dp
    val candidateExpandedHeight = resolvedKeyboardHeight * 0.35f
    // Top segment is always present (except emoji/clipboard), showing either toolbar or candidates.
    val topSegmentHeight = when {
        isEmojiMode -> 0.dp
        isClipboardMode -> candidateExpandedHeight
        topBarState == KeyboardViewModel.TopBarState.CANDIDATES_EXPANDED -> candidateExpandedHeight
        else -> candidateCollapsedHeight
    }
    val bodyHeight = (resolvedKeyboardHeight - topSegmentHeight).coerceAtLeast(160.dp)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = resolvedKeyboardWidth)
                .fillMaxWidth()
                .height(resolvedKeyboardHeight)
                .background(theme.keyboardBackground.color)
        ) {
            Column {
                if (isEmojiMode) {
                    EmojiScreen(
                        emojiData = emojiData,
                        height = resolvedKeyboardHeight,
                        onEmojiClick = { viewModel.commitSuggestion(it) },
                        onClose = { viewModel.exitEmojiMode() }
                    )
                } else {
                    TopSegment(
                        viewModel = viewModel,
                        isClipboardMode = isClipboardMode,
                        topBarState = topBarState,
                        toolbarKeys = toolbarKeys,
                        isDefaultCharactersLayout = isDefaultCharactersLayout,
                        suggestions = suggestions,
                        composingText = composingText,
                        isChineseMode = isChineseMode,
                        shiftState = shiftState,
                        height = topSegmentHeight,
                        scrollState = scrollState,
                        onKeyAction = { action -> scope.launch { viewModel.onKeyPress(action) } },
                        onCandidateClick = { viewModel.commitSuggestion(it) },
                        onExpandToggle = { viewModel.expandCandidates() },
                    )
                    keyboardLayout?.let { layout ->
                        val body: @Composable () -> Unit = {
                            KeyboardBody(
                                layout = layout,
                                shiftState = shiftState,
                                onKeyAction = { action -> scope.launch { viewModel.onKeyPress(action) } },
                                onStrokeFinished = viewModel::onStrokeFinished,
                                keyShape = RoundedCornerShape(12.dp),
                                height = bodyHeight
                            )
                        }
                        if (isFloatingMode) {
                            FloatingKeyboard(resolvedKeyboardWidth) { body() }
                        } else {
                            DockedKeyboard(resolvedKeyboardWidth) { body() }
                        }
                    } ?: Spacer(modifier = Modifier.height(200.dp))
                }
            }
        }

        // Voice Input Overlay
        if (voiceState == VoiceInputManager.State.LISTENING || voiceState == VoiceInputManager.State.RECOGNIZING) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable { /* Consume clicks */ },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Listening...", color = Color.White)
            }
        }
    }
}

@Composable
private fun TopSegment(
    viewModel: KeyboardViewModel,
    isClipboardMode: Boolean,
    topBarState: KeyboardViewModel.TopBarState,
    toolbarKeys: List<LayoutKeyData>,
    isDefaultCharactersLayout: Boolean,
    suggestions: List<String>,
    composingText: String,
    isChineseMode: Boolean,
    shiftState: ShiftState,
    height: Dp,
    scrollState: ScrollState,
    onKeyAction: (KeyAction) -> Unit,
    onCandidateClick: (String) -> Unit,
    onExpandToggle: () -> Unit,
) {
    if (height <= 0.dp) return
    val isModeOverlayActive by viewModel.isModeOverlayActive.collectAsState()
    when {
        isClipboardMode -> ClipboardScreen(viewModel, height = height)
        topBarState == KeyboardViewModel.TopBarState.TOOLBAR -> {
            if (isModeOverlayActive) {
                ModeBackBar(height = height) {
                    onKeyAction(KeyAction.SwitchToLayout("_main_"))
                }
            } else if (isDefaultCharactersLayout && toolbarKeys.isNotEmpty()) {
                Toolbar(
                    keys = toolbarKeys,
                    shiftState = shiftState,
                    onKeyAction = onKeyAction,
                    height = height,
                    scrollState = scrollState
                )
            } else {
                Spacer(modifier = Modifier.height(height))
            }
        }
        topBarState == KeyboardViewModel.TopBarState.CANDIDATES_EXPANDED -> {
            CandidateGrid(
                suggestions = suggestions,
                composingText = composingText,
                height = height,
                onCandidateClick = onCandidateClick,
                showComposing = isChineseMode,
                onExpandToggle = onExpandToggle
            )
        }
        else -> {
            CandidateCollapsedBar(
                suggestions = suggestions,
                composingText = composingText,
                isChineseMode = isChineseMode,
                height = height,
                onCandidateClick = onCandidateClick,
                onExpandToggle = onExpandToggle
            )
        }
    }
}

@Composable
private fun ModeBackBar(
    height: Dp,
    onBack: () -> Unit,
) {
    val theme = LocalTheme.current ?: DefaultThemeData
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(theme.keyboardBackground.color)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clickable(onClick = onBack)
                .background(theme.keyBackground.color, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "ABC",
                color = theme.keyForeground.color,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CandidateCollapsedBar(
    suggestions: List<String>,
    composingText: String,
    isChineseMode: Boolean,
    height: Dp,
    onCandidateClick: (String) -> Unit,
    onExpandToggle: () -> Unit,
) {
    val theme = LocalTheme.current ?: DefaultThemeData
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(theme.suggestionsBackground.color)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (isChineseMode && composingText.isNotBlank()) {
            Text(
                text = composingText,
                color = theme.suggestionsForeground.color,
                fontWeight = FontWeight.SemiBold
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                suggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        color = theme.suggestionsForeground.color,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onCandidateClick(suggestion) }
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "More candidates",
                tint = theme.suggestionsForeground.color,
                modifier = Modifier
                    .clickable { onExpandToggle() }
                    .padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun Toolbar(
    keys: List<LayoutKeyData>,
    shiftState: ShiftState,
    onKeyAction: (KeyAction) -> Unit,
    height: Dp,
    scrollState: ScrollState
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .horizontalScroll(scrollState),
    ) {
        keys.forEach { key ->
            val action = key.toKeyAction() ?: return@forEach
            KeyButton(
                label = key.displayLabel(shiftState),
                onClick = { onKeyAction(action) },
                weight = 1f,
                height = 44.dp,
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

@Composable
private fun FloatingKeyboard(maxWidth: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .padding(8.dp)
            .widthIn(max = maxWidth)
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun DockedKeyboard(maxWidth: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun KeyboardBody(
    layout: LayoutKeyboardData,
    shiftState: ShiftState,
    onKeyAction: (KeyAction) -> Unit,
    onStrokeFinished: (Ink) -> Unit,
    keyShape: Shape,
    height: Dp
) {
    if (layout.type.startsWith("handwriting")) {
        HandwritingPad(
            modifier = Modifier
                .fillMaxWidth()
                .height(height),
            mode = layout.mode ?: "full",
            onStrokeFinished = onStrokeFinished
        )
        return
    }
    val rowCount = layout.arrangement.size.coerceAtLeast(1)
    val columnVerticalPadding = 8.dp
    val rowSpacing = 8.dp
    val availableHeight = (height - columnVerticalPadding - rowSpacing * (rowCount - 1))
        .coerceAtLeast(0.dp)
    val keyHeight = (availableHeight / rowCount).coerceIn(44.dp, 72.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        layout.arrangement.forEach { rowData ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                rowData.row.forEach { keyArrangement ->
                    keyArrangement.keys.forEach { key ->
                        val action = key.toKeyAction() ?: return@forEach
                        val weight = keyArrangement.width
                        KeyButton(
                            label = key.displayLabel(shiftState),
                            onClick = { onKeyAction(action) },
                            weight = weight,
                            height = keyHeight,
                            shape = keyShape,
                            onLongPress = if (action == KeyAction.Delete) {
                                { onKeyAction(KeyAction.Delete) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyButton(
    label: String,
    onClick: () -> Unit,
    weight: Float,
    shape: Shape,
    height: androidx.compose.ui.unit.Dp = 56.dp,
    onLongPress: (() -> Unit)? = null
) {
    val theme = LocalTheme.current ?: DefaultThemeData
    val scope = rememberCoroutineScope()
    Box(
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 3.dp)
            .height(height)
            .background(theme.keyBackground.color, shape)
            .pointerInput(onLongPress) {
                awaitEachGesture {
                    var longPressFired = false
                    val down = awaitFirstDown()
                    val repeatJob = onLongPress?.let {
                        scope.launch {
                            delay(320)
                            longPressFired = true
                            while (isActive) {
                                it()
                                delay(60)
                            }
                        }
                    }
                    val up = waitForUpOrCancellation()
                    repeatJob?.cancel()
                    if (!longPressFired && up != null) {
                        onClick()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = theme.keyForeground.color,
            fontWeight = FontWeight.Medium
        )
    }
}

private object SpecialKeyResolver {
    fun resolve(key: LayoutKeyData): KeyAction? {
        return when (key.type.lowercase()) {
            "char" -> KeyAction.InsertText(key.value)
            "delete" -> KeyAction.Delete
            "space" -> KeyAction.Space
            "enter" -> KeyAction.InsertText("\n")
            "shift" -> KeyAction.Shift(layout = null)
            "switch_to_layout" -> KeyAction.SwitchToLayout(key.value)
            "symbol" -> KeyAction.SwitchToLayout("symbols")
            "numeric" -> KeyAction.SwitchToLayout("numeric")
            "show_settings" -> KeyAction.ShowSettings
            "system_voice" -> KeyAction.SystemVoice
            "system_emoji" -> KeyAction.SystemEmoji
            "system_clipboard" -> KeyAction.SystemClipboard
            else -> null
        }
    }
}

private fun LayoutKeyData.toKeyAction(): KeyAction? = SpecialKeyResolver.resolve(this)

private fun LayoutKeyData.displayLabel(shiftState: ShiftState): String {
    val base = label ?: value
    return if (type.equals("char", true) && shiftState != ShiftState.OFF) {
        base.uppercase()
    } else {
        when {
            type.equals("delete", true) -> "⌫"
            type.equals("shift", true) -> "⇧"
            type.equals("enter", true) -> "⏎"
            else -> base
        }
    }
}
