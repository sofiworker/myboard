package xyz.xiao6.myboard.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.xiao6.myboard.data.KeyData as LayoutKeyData
import xyz.xiao6.myboard.data.voice.VoiceInputManager
import xyz.xiao6.myboard.ui.theme.LocalTheme

@Composable
fun KeyboardScreen(viewModel: KeyboardViewModel = viewModel()) {
    val theme = LocalTheme.current ?: return
    val keyboardLayout by viewModel.keyboardLayout.collectAsState(initial = null)
    val suggestions by viewModel.suggestions.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val isEmojiMode by viewModel.isEmojiMode.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column {
            keyboardLayout?.let { layout ->
                layout.toolbar?.let {
                    Toolbar(it, viewModel)
                }
            }
            if (isEmojiMode) {
//                 EmojiScreen(emojiData = emojiData, onEmojiClick = { viewModel.commitSuggestion(it) })
            } else {
                CandidateView(suggestions = suggestions, onCandidateClick = { viewModel.commitSuggestion(it) })
            }
            Box(modifier = Modifier.weight(1f)) {
                // Docked or Floating Keyboard
                val isFloatingMode by viewModel.isFloatingMode.collectAsState()
                if (isFloatingMode) {
                    FloatingKeyboard(viewModel)
                } else {
                    DockedKeyboard(viewModel)
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
private fun Toolbar(toolbar: List<LayoutKeyData>, viewModel: KeyboardViewModel) {
    // TODO: Implement Toolbar
}

@Composable
private fun FloatingKeyboard(viewModel: KeyboardViewModel) {
    // TODO: Implement FloatingKeyboard
}

@Composable
private fun DockedKeyboard(viewModel: KeyboardViewModel) {
    // TODO: Implement DockedKeyboard
}
