package xyz.xiao6.myboard.ui.handwriting

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import xyz.xiao6.myboard.manager.HandwritingRecognitionManager
import xyz.xiao6.myboard.model.HandwritingLayoutMode
import xyz.xiao6.myboard.ui.theme.MyBoardTheme

import xyz.xiao6.myboard.model.HandwritingPosition

/**
 * Container view for HandwritingPanel (Modern)
 * Bridges View-based architecture with Compose-based handwriting panel
 */
class ModernHandwritingContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr), LifecycleOwner, ViewModelStoreOwner {

    private var composeView: ComposeView? = null
    private var recognitionManagerState by mutableStateOf<HandwritingRecognitionManager?>(null)
    private var layoutModeState by mutableStateOf(HandwritingLayoutMode.FULL_SCREEN)
    private var layoutSpecState by mutableStateOf<HandwritingLayoutSpec?>(null)
    private var clearSignal by mutableIntStateOf(0)

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()

    var onBack: (() -> Unit)? = null
    var onCandidateSelected: ((String) -> Unit)? = null
    var onClear: (() -> Unit)? = null
    var onSwitchToKeyboard: (() -> Unit)? = null
    var onLayoutPickerRequest: (() -> Unit)? = null
    var onVoiceRequest: (() -> Unit)? = null
    var onEmojiRequest: (() -> Unit)? = null
    var onResizeRequest: (() -> Unit)? = null
    var onBackspaceRequest: (() -> Unit)? = null
    var onSwitchLayoutRequest: ((String) -> Unit)? = null
    var onToggleLocaleRequest: (() -> Unit)? = null

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    init {
        setViewTreeLifecycleOwner(this)
        setViewTreeViewModelStoreOwner(this)
        setupComposeView()
    }

    private fun setupComposeView() {
        composeView = ComposeView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                val currentLayoutMode = this@ModernHandwritingContainerView.layoutModeState
                val currentRecognitionManager = this@ModernHandwritingContainerView.recognitionManagerState
                val currentLayoutSpec = this@ModernHandwritingContainerView.layoutSpecState
                val currentClearSignal = this@ModernHandwritingContainerView.clearSignal

                MyBoardTheme {
                    HandwritingPanel(
                        mode = currentLayoutMode,
                        position = currentLayoutSpec?.position ?: HandwritingPosition.BOTTOM,
                        layoutSpec = currentLayoutSpec,
                        recognitionManager = currentRecognitionManager,
                        onBack = { 
                            onBack?.invoke() 
                            onSwitchToKeyboard?.invoke()
                        },
                        onCandidateSelected = { text -> onCandidateSelected?.invoke(text) },
                        onClear = { onClear?.invoke() },
                        clearSignal = currentClearSignal,
                        onLayoutPickerRequest = { onLayoutPickerRequest?.invoke() },
                        onVoiceRequest = { onVoiceRequest?.invoke() },
                        onEmojiRequest = { onEmojiRequest?.invoke() },
                        onResizeRequest = { onResizeRequest?.invoke() },
                        onBackspaceRequest = { onBackspaceRequest?.invoke() },
                        onSwitchLayoutRequest = { layoutId -> onSwitchLayoutRequest?.invoke(layoutId) },
                        onToggleLocaleRequest = { onToggleLocaleRequest?.invoke() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        addView(composeView)
    }

    fun setRecognitionManager(manager: HandwritingRecognitionManager) {
        recognitionManagerState = manager
    }

    fun setLayoutMode(mode: HandwritingLayoutMode) {
        layoutModeState = mode
    }

    fun setLayoutSpec(spec: HandwritingLayoutSpec?) {
        layoutSpecState = spec
    }

    fun clearCanvas() {
        clearSignal += 1
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        super.onDetachedFromWindow()
    }
}
