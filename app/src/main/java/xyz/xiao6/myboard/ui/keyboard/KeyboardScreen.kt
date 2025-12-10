package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.model.KeyData
import xyz.xiao6.myboard.data.model.KeyType
import xyz.xiao6.myboard.data.model.ShiftState
import xyz.xiao6.myboard.ui.theme.LocalTheme

@Composable
fun KeyboardScreen(viewModel: KeyboardViewModel = viewModel()) {
    val theme = LocalTheme.current ?: return
    val keyboardLayout by viewModel.keyboardLayout.collectAsState(initial = null)
    val suggestions by viewModel.suggestions.collectAsState()
    val shiftState by viewModel.shiftState.collectAsState()

    Column(modifier = Modifier.background(theme.keyboardBackground)) {
        keyboardLayout?.let { layout ->
            Toolbar(toolbar = layout.toolbar, viewModel = viewModel, shiftState = shiftState)
            SuggestionsBar(suggestions = suggestions, viewModel = viewModel)
            for (row in layout.rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (key in row) {
                        Key(
                            key = key,
                            modifier = Modifier.weight(key.weight),
                            viewModel = viewModel,
                            shiftState = shiftState
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Toolbar(
    toolbar: List<KeyData>,
    viewModel: KeyboardViewModel,
    shiftState: ShiftState
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (key in toolbar) {
            Key(
                key = key,
                modifier = Modifier.weight(key.weight),
                viewModel = viewModel,
                shiftState = shiftState
            )
        }
    }
}

@Composable
fun SuggestionsBar(suggestions: List<String>, viewModel: KeyboardViewModel) {
    val theme = LocalTheme.current ?: return
    val scope = rememberCoroutineScope()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(theme.suggestionsBackground)
    ) {
        for (suggestion in suggestions) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(2.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = { 
                                scope.launch {
                                    viewModel.onKeyPress(KeyAction.CommitSuggestion(suggestion))
                                }
                             }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = suggestion, color = theme.suggestionsForeground)
            }
        }
    }
}

@Composable
fun Key(
    key: KeyData,
    modifier: Modifier = Modifier,
    viewModel: KeyboardViewModel,
    shiftState: ShiftState
) {
    val theme = LocalTheme.current ?: return
    val scope = rememberCoroutineScope()
    val isShifted = shiftState != ShiftState.OFF
    var showPopup by remember { mutableStateOf(false) }

    val label = if (key.type == KeyType.CHARACTER) {
        if (isShifted) key.label.uppercase() else key.label.lowercase()
    } else {
        key.label
    }

    val backgroundColor = if (key.action is KeyAction.Shift && isShifted) {
        theme.keyBackground.copy(alpha = 0.7f)
    } else {
        theme.keyBackground
    }

    Box(
        modifier = modifier
            .height(64.dp)
            .padding(2.dp)
            .background(backgroundColor)
            .pointerInput(key.action) {
                detectTapGestures(
                    onLongPress = {
                        if (key.more.isNotEmpty()) {
                            showPopup = true
                        }
                    },
                    onTap = { 
                        scope.launch {
                            viewModel.onKeyPress(key.action)
                        }
                     }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = theme.keyForeground)
        if (showPopup) {
            Popup(onDismissRequest = { showPopup = false }) {
                Row(modifier = Modifier.background(theme.suggestionsBackground)) {
                    for (popupKey in key.more) {
                        Key(key = popupKey, viewModel = viewModel, shiftState = shiftState)
                    }
                }
            }
        }
    }
}
