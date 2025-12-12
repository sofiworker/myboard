package xyz.xiao6.myboard

import android.annotation.SuppressLint
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.voice.VoiceInputManager
import xyz.xiao6.myboard.ui.keyboard.KeyboardScreen
import xyz.xiao6.myboard.ui.keyboard.KeyboardViewModel
import xyz.xiao6.myboard.ui.theme.MyboardTheme

@SuppressLint("RestrictedApi")
class MyboardImeService : InputMethodService(), ViewModelStoreOwner, LifecycleOwner,
    SavedStateRegistryOwner, OnBackPressedDispatcherOwner {

    override val viewModelStore by lazy { ViewModelStore() }
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val onBackPressedDispatcher = OnBackPressedDispatcher()
    private lateinit var viewModel: KeyboardViewModel
    private lateinit var voiceInputManager: VoiceInputManager
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController = SavedStateRegistryController.create(this)
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        // Install lifecycle/saved-state owners on the IME window root so Compose can find them.
        window?.window?.decorView?.let { decorView ->
            decorView.setViewTreeLifecycleOwner(this)
            decorView.setViewTreeViewModelStoreOwner(this)
            decorView.setViewTreeSavedStateRegistryOwner(this)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(KeyboardViewModel::class.java)
        voiceInputManager = VoiceInputManager(this)
        lifecycleScope.launch {
            viewModel.keyAction.collect { handleKeyAction(it) }
        }
        lifecycleScope.launch {
            viewModel.composingText.collect { text ->
                val ic = currentInputConnection
                if (ic != null) {
                    if (text.isEmpty()) {
                        ic.finishComposingText()
                    } else {
                        ic.setComposingText(text, 1)
                    }
                }
            }
        }
        lifecycleScope.launch {
            voiceInputManager.state.collect {
                viewModel.setVoiceState(it)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        // The IME window can be recreated; make sure the root keeps the owners Compose expects.
        view.doOnAttach {
            it.rootView?.let { root ->
                root.setViewTreeLifecycleOwner(this)
                root.setViewTreeViewModelStoreOwner(this)
                root.setViewTreeSavedStateRegistryOwner(this)
            }
        }
        view.setContent {
            val themeData by viewModel.themeData.collectAsState()
            MyboardTheme(themeData = themeData) {
                KeyboardScreen(viewModel)
            }
        }
        return view
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // Avoid resetting state on input view restarts, which can happen frequently in some apps
        // and would clear composing text/modes causing flicker and "auto-delete" feel.
        if (!restarting) {
            viewModel.resetToMainLayout()
        } else {
            viewModel.resetSession()
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    private fun handleKeyAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.SystemVoice -> {
                voiceInputManager.startListening { text ->
                    ic.commitText(text, 1)
                }
            }
            is KeyAction.CommitSuggestion -> {
                ic.finishComposingText()
                ic.commitText(action.suggestion, 1)
            }
            KeyAction.Delete -> ic.deleteSurroundingText(1, 0)
            is KeyAction.InsertText -> {
                ic.finishComposingText()
                ic.commitText(action.text, 1)
            }
            is KeyAction.MoveCursor -> {
                val offset = action.offset
                val keyCode = if (offset < 0) KeyEvent.KEYCODE_DPAD_LEFT else KeyEvent.KEYCODE_DPAD_RIGHT
                repeat(kotlin.math.abs(offset)) {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
            }
            is KeyAction.MoveFloatingWindow -> { /* Handled by UI */
            }
            is KeyAction.Shift -> { /* Handled by ViewModel */
            }
            KeyAction.ShowSettings -> startActivity(
                Intent(
                    this,
                    SettingsActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            KeyAction.Space -> ic.commitText(" ", 1)
            is KeyAction.SwitchToLayout -> { ic.finishComposingText() /* Handled by ViewModel state */ }
            KeyAction.SystemClipboard -> { /* TODO */
            }
            KeyAction.SystemEmoji -> { /* TODO */
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        voiceInputManager.cancel()
    }

    // Implement LifecycleOwner's lifecycle property (newer API uses a property, not getLifecycle())
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

}
