package xyz.xiao6.myboard.controller

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.decoder.DecodeUpdate
import xyz.xiao6.myboard.decoder.Decoder
import xyz.xiao6.myboard.decoder.PassthroughDecoder
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyTrigger
import xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView
import xyz.xiao6.myboard.util.MLog

/**
 * 输入法控制器：负责执行状态机 effect，并严格遵守 requestLayout vs invalidate 的约束。
 * Input method controller: executes state machine effects and enforces requestLayout vs invalidate constraints.
 */
class InputMethodController(
    private val layoutManager: LayoutManager,
    initialDecoder: Decoder = PassthroughDecoder,
) {
    private val logTag = "Controller"
    private var keyboardView: KeyboardSurfaceView? = null
    private val layoutHistory = ArrayDeque<String>()
    private var primaryLayoutId: String = ""

    private val _layoutState = MutableStateFlow(LayoutState())
    val layoutState: StateFlow<LayoutState> = _layoutState.asStateFlow()

    private val _currentLayout = MutableStateFlow<KeyboardLayout?>(null)
    val currentLayout: StateFlow<KeyboardLayout?> = _currentLayout.asStateFlow()

    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val candidates: StateFlow<List<String>> = _candidates.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText: StateFlow<String> = _composingText.asStateFlow()

    private var decoder: Decoder = initialDecoder

    private var model = KeyboardStateMachine.Model(
        currentLayoutId = "",
        state = LayoutState(),
        keys = emptyList(),
    )

    var onCommitText: ((String) -> Unit)? = null
    var onSwitchLocale: ((String) -> Unit)? = null

    fun attach(view: KeyboardSurfaceView) {
        keyboardView = view
        _currentLayout.value?.let { view.setLayout(it) }
        view.setLayoutState(_layoutState.value, invalidateKeyIds = emptySet())
    }

    fun setDecoder(newDecoder: Decoder) {
        MLog.d(logTag, "setDecoder=${newDecoder::class.simpleName}")
        decoder = newDecoder
        applyDecode(decoder.reset())
    }

    /**
     * Layout Switch：加载新的 Layout JSON，并强制几何重算（requestLayout）。
     * Layout switch: load a new layout JSON and force geometry recomputation (requestLayout).
     */
    fun loadLayout(layoutId: String) {
        layoutManager.loadAllFromAssets()
        val layout = layoutManager.getLayout(layoutId)
        layoutHistory.clear()
        primaryLayoutId = layoutId
        applyNewLayout(layout, pushToHistory = false)
    }

    /**
     * State Change：仅更新 LayoutState，并局部刷新（invalidate(Rect)），禁止 requestLayout。
     * State change: only updates LayoutState and performs partial invalidation; requestLayout is forbidden.
     */
    fun updateState(reducer: (LayoutState) -> LayoutState, invalidateKeyIds: Set<String>) {
        val newState = reducer(_layoutState.value)
        _layoutState.value = newState
        model = model.copy(state = newState)
        keyboardView?.setLayoutState(newState, invalidateKeyIds)
    }

    /**
     * 处理按键手势触发。
     * Handle key trigger (gesture).
     */
    fun onKeyTriggered(keyId: String, trigger: KeyTrigger) {
        val key = model.keys.firstOrNull { it.keyId == keyId } ?: return
        dispatch(KeyboardStateMachine.Event.Triggered(key, trigger))
    }

    /**
     * 处理功能动作（例如 Shift 或布局切换）。
     * Handle an action (e.g. Shift or layout switch).
     */
    fun onAction(action: KeyAction) {
        dispatch(KeyboardStateMachine.Event.Action(action))
    }

    fun onCandidateSelected(text: String) {
        applyDecode(decoder.onCandidateSelected(text))
    }

    private fun dispatch(event: KeyboardStateMachine.Event) {
        val (newModel, effects) = KeyboardStateMachine.reduce(model, event)
        model = newModel

        for (effect in effects) {
            when (effect) {
                is KeyboardStateMachine.Effect.CommitText -> {
                    MLog.d(logTag, "Effect.CommitText text=${effect.text.take(16)}")
                    applyDecode(decoder.onText(effect.text))
                }

                is KeyboardStateMachine.Effect.SwitchLayout -> {
                    MLog.d(logTag, "Effect.SwitchLayout layoutId=${effect.layoutId}")
                    layoutManager.loadAllFromAssets()
                    val layout = layoutManager.getLayout(effect.layoutId)
                    applyNewLayout(layout, pushToHistory = true)
                }

                is KeyboardStateMachine.Effect.SwitchLocale -> {
                    MLog.d(logTag, "Effect.SwitchLocale localeTag=${effect.localeTag}")
                    // Locale change usually implies dictionary/mode change; clear current candidates.
                    applyDecode(decoder.reset())
                    onSwitchLocale?.invoke(effect.localeTag)
                }

                KeyboardStateMachine.Effect.BackLayout -> {
                    val targetId = layoutHistory.removeLastOrNull()
                        ?: primaryLayoutId.takeIf { it.isNotBlank() && it != model.currentLayoutId }
                        ?: continue

                    layoutManager.loadAllFromAssets()
                    val layout = layoutManager.getLayout(targetId)
                    applyNewLayout(layout, pushToHistory = false)
                }

                is KeyboardStateMachine.Effect.UpdateState -> {
                    _layoutState.value = effect.newState
                    keyboardView?.setLayoutState(effect.newState, effect.invalidateKeyIds)
                }

                KeyboardStateMachine.Effect.NoOp -> Unit
            }
        }
    }

    private fun applyDecode(update: DecodeUpdate) {
        if (update.commitTexts.isNotEmpty() || update.candidates.isNotEmpty()) {
            MLog.d(
                logTag,
                "applyDecode commits=${update.commitTexts.size} candidates=${update.candidates.size}",
            )
        }
        update.composingText?.let { _composingText.value = it }
        for (t in update.commitTexts) {
            if (t.isEmpty()) continue
            onCommitText?.invoke(t)
        }
        _candidates.value = update.candidates
    }

    private fun applyNewLayout(layout: KeyboardLayout, pushToHistory: Boolean) {
        val previousId = model.currentLayoutId.takeIf { it.isNotBlank() }
        if (pushToHistory && previousId != null && previousId != layout.layoutId) {
            layoutHistory.addLast(previousId)
        }

        applyDecode(decoder.reset())
        _currentLayout.value = layout
        val keys = layout.rows.flatMap { it.keys }
        model = model.copy(
            currentLayoutId = layout.layoutId,
            keys = keys,
        )

        keyboardView?.setLayout(layout)
        keyboardView?.setLayoutState(_layoutState.value, invalidateKeyIds = emptySet())
    }
}
