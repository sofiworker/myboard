package xyz.xiao6.myboard

import android.content.Intent
import android.inputmethodservice.InputMethodService
import android.view.View
import androidx.activity.OnBackPressedDispatcher
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.activity.setViewTreeOnBackPressedDispatcherOwner
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.xiao6.myboard.data.model.KeyAction
import xyz.xiao6.myboard.ui.theme.MyboardTheme
import xyz.xiao6.myboard.ui.keyboard.KeyboardScreen
import xyz.xiao6.myboard.ui.keyboard.KeyboardViewModel

class MyboardImeService : InputMethodService(), ViewModelStoreOwner, LifecycleOwner,
    SavedStateRegistryOwner, OnBackPressedDispatcherOwner {

    override val viewModelStore by lazy { ViewModelStore() }

    private val savedStateRegistryController by lazy { SavedStateRegistryController.create(this) }
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val lifecycleRegistry by lazy { LifecycleRegistry(this) }
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override val onBackPressedDispatcher by lazy { OnBackPressedDispatcher(null) }

    private val viewModel by lazy {
        ViewModelProvider(this)[KeyboardViewModel::class.java]
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        lifecycleScope.launch {
            viewModel.keyAction.collectLatest { action ->
                handleKeyAction(action)
            }
        }
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setViewTreeLifecycleOwner(this@MyboardImeService)
            setViewTreeSavedStateRegistryOwner(this@MyboardImeService)
            setViewTreeOnBackPressedDispatcherOwner(this@MyboardImeService)
            setContent {
                val themeData by viewModel.themeData.collectAsState()
                MyboardTheme(themeData = themeData) {
                    KeyboardScreen()
                }
            }
        }
        return view
    }

    private fun handleKeyAction(action: KeyAction) {
        val ic = currentInputConnection ?: return
        when (action) {
            is KeyAction.InsertText -> ic.setComposingText(action.text, 1)
            is KeyAction.Delete -> ic.deleteSurroundingText(1, 0)
            is KeyAction.Space -> ic.commitText(" ", 1)
            is KeyAction.CommitSuggestion -> {
                ic.commitText(action.suggestion + " ", 1)
            }
            is KeyAction.ShowSettings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            else -> { /* Other actions are handled in the ViewModel */ }
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}
