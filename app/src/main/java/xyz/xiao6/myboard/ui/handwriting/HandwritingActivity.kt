package xyz.xiao6.myboard.ui.handwriting

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.model.HandwritingPosition
import xyz.xiao6.myboard.store.SettingsStore
import xyz.xiao6.myboard.ui.theme.MyBoardTheme

/**
 * Activity for full-screen or half-screen handwriting input
 * 全屏或半屏手写输入Activity
 */
class HandwritingActivity : ComponentActivity() {

    private lateinit var settingsStore: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsStore = SettingsStore(this)

        // Get layout mode from intent or settings
        val layoutModeStr = intent.getStringExtra(EXTRA_LAYOUT_MODE)
        val layoutMode = layoutModeStr?.let { HandwritingLayoutMode.valueOf(it) }
            ?: settingsStore.handwritingLayoutMode.let { HandwritingLayoutMode.valueOf(it) }

        val positionStr = intent.getStringExtra(EXTRA_POSITION)
        val position = positionStr?.let { HandwritingPosition.valueOf(it) }
            ?: settingsStore.handwritingPosition.let { HandwritingPosition.valueOf(it) }

        setContent {
            MyBoardTheme {
                HandwritingScreen(
                    layoutMode = layoutMode,
                    position = position,
                    onClose = { finish() },
                    onCommitText = { text ->
                        commitText(text)
                        finish()
                    },
                )
            }
        }
    }

    private fun commitText(text: String) {
        val result = Intent().apply {
            putExtra(EXTRA_COMMIT_TEXT, text)
        }
        setResult(RESULT_OK, result)
    }

    companion object {
        const val EXTRA_LAYOUT_MODE = "layout_mode"
        const val EXTRA_POSITION = "position"
        const val EXTRA_COMMIT_TEXT = "commit_text"

        fun createIntent(
            activity: Activity,
            layoutMode: HandwritingLayoutMode? = null,
            position: HandwritingPosition? = null,
        ): Intent {
            return Intent(activity, HandwritingActivity::class.java).apply {
                layoutMode?.let { putExtra(EXTRA_LAYOUT_MODE, it.name) }
                position?.let { putExtra(EXTRA_POSITION, it.name) }
            }
        }
    }
}

/**
 * Handwriting screen Composable
 * 手写输入屏幕Compose UI
 */
@Composable
fun HandwritingScreen(
    layoutMode: HandwritingLayoutMode,
    position: HandwritingPosition,
    onClose: () -> Unit,
    onCommitText: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settingsStore = remember { SettingsStore(context) }

    var recognizedText by remember { mutableStateOf("") }

    HandwritingPanel(
        mode = layoutMode,
        position = position,
        onBack = onClose,
        onRecognize = {
            // Simulate recognition - in real implementation, use HandwritingRecognitionManager
            recognizedText = "Recognized text"
        },
        onClear = { recognizedText = "" },
        modifier = modifier,
    )
}
