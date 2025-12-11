package xyz.xiao6.myboard

import android.annotation.SuppressLint
import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.data.voice.VoiceInputManager
import xyz.xiao6.myboard.ui.keyboard.KeyboardScreen
import xyz.xiao6.myboard.ui.keyboard.KeyboardViewModel

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
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        viewModel = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
            .create(KeyboardViewModel::class.java)
        voiceInputManager = VoiceInputManager(this)
        lifecycleScope.launch {
            voiceInputManager.state.collect {
                viewModel.setVoiceState(it)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this)
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            KeyboardScreen(viewModel)
        }
        return view
    }

    private fun handleKeyAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.SystemVoice -> {
                voiceInputManager.startListening { text ->
                    ic.commitText(text, 1)
                }
            }
            is KeyAction.CommitSuggestion -> ic.commitText(action.suggestion, 1)
            KeyAction.Delete -> ic.deleteSurroundingText(1, 0)
            is KeyAction.InsertText -> ic.commitText(action.text, 1)
            is KeyAction.MoveCursor -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT))
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
            is KeyAction.SwitchToLayout -> { /* Handled by ViewModel */
            }
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