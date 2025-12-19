package xyz.xiao6.myboard.controller
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import xyz.xiao6.myboard.decoder.DecodeUpdate
import xyz.xiao6.myboard.decoder.Decoder
import xyz.xiao6.myboard.decoder.PassthroughDecoder
import xyz.xiao6.myboard.decoder.TokenDecoder
import xyz.xiao6.myboard.decoder.TokenDecoderAdapter
import xyz.xiao6.myboard.manager.LayoutManager
import xyz.xiao6.myboard.model.KeyboardLayout
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.Token as KeyToken
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.KeyboardLayer
import xyz.xiao6.myboard.model.ModifierKey
import xyz.xiao6.myboard.model.Action
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.ui.keyboard.KeyboardSurfaceView
import xyz.xiao6.myboard.util.MLog

/**
 * 输入法控制器：负责执行状态机 effect，并严格遵守 requestLayout vs invalidate 的约束。
 * Input method controller: executes state machine effects and enforces requestLayout vs invalidate constraints.
 */
class InputMethodController(
    private val layoutManager: LayoutManager,
    private val scope: CoroutineScope,
    initialDecoder: Decoder = PassthroughDecoder,
) {
    private val logTag = "Controller"
    private var keyboardView: KeyboardSurfaceView? = null
    private val layoutHistory = ArrayDeque<String>()
    private var primaryLayoutId: String = ""
    private val symbolsLayoutId = "symbols"

    private val _layoutState = MutableStateFlow(LayoutState())
    val layoutState: StateFlow<LayoutState> = _layoutState.asStateFlow()

    private val _currentLayout = MutableStateFlow<KeyboardLayout?>(null)
    val currentLayout: StateFlow<KeyboardLayout?> = _currentLayout.asStateFlow()

    private val _candidates = MutableStateFlow<List<String>>(emptyList())
    val candidates: StateFlow<List<String>> = _candidates.asStateFlow()

    private val _composingText = MutableStateFlow("")
    val composingText: StateFlow<String> = _composingText.asStateFlow()

    private val _composingOptions = MutableStateFlow<List<String>>(emptyList())
    val composingOptions: StateFlow<List<String>> = _composingOptions.asStateFlow()

    private var lastLoggedComposing: String? = null
    private var lastLoggedCandidatesHash: Int? = null
    private val decodeRequests = Channel<DecodeRequest>(Channel.UNLIMITED)

    private var model = KeyboardStateMachine.Model(
        currentLayoutId = "",
        state = LayoutState(),
        keys = emptyList(),
    )

    var onCommitText: ((String) -> Unit)? = null
    var onSwitchLocale: ((String) -> Unit)? = null
    var onToggleLocale: (() -> Unit)? = null
    var onShowSymbols: (() -> Unit)? = null

    private sealed interface DecodeRequest {
        data class SetDecoder(val decoder: Decoder) : DecodeRequest
        data object Reset : DecodeRequest
        data class Text(val text: String) : DecodeRequest
        data class Token(val token: KeyToken) : DecodeRequest
        data class CandidateSelected(val text: String) : DecodeRequest
    }

    init {
        scope.launch(Dispatchers.Default) {
            var decoder: Decoder = initialDecoder
            for (req in decodeRequests) {
                val update =
                    when (req) {
                        is DecodeRequest.SetDecoder -> {
                            decoder = req.decoder
                            decoder.reset()
                        }
                        DecodeRequest.Reset -> decoder.reset()
                        is DecodeRequest.Text -> decoder.onText(req.text)
                        is DecodeRequest.Token -> onToken(decoder, req.token)
                        is DecodeRequest.CandidateSelected -> decoder.onCandidateSelected(req.text)
                    }
                withContext(Dispatchers.Main.immediate) {
                    applyDecodeUpdate(update)
                }
            }
        }
    }

    fun attach(view: KeyboardSurfaceView) {
        keyboardView = view
        _currentLayout.value?.let { view.setLayout(it) }
        view.setLayoutState(_layoutState.value, invalidateKeyIds = emptySet())
    }

    fun setDecoder(newDecoder: Decoder) {
        MLog.d(logTag, "setDecoder=${newDecoder::class.simpleName}")
        clearDecodeUiState()
        updateState({ it.copy(engine = inferEngineTag(newDecoder)) }, invalidateKeyIds = emptySet())
        decodeRequests.trySend(DecodeRequest.SetDecoder(newDecoder))
    }

    private fun inferEngineTag(decoder: Decoder): String {
        return when (decoder) {
            is xyz.xiao6.myboard.decoder.TokenPinyinDecoder -> "ZH_PINYIN"
            xyz.xiao6.myboard.decoder.PassthroughDecoder -> "DIRECT"
            else -> decoder::class.simpleName ?: "UNKNOWN"
        }
    }

    /**
     * Layout Switch：加载新的 Layout JSON，并强制几何重算（requestLayout）。
     * Layout switch: load a new layout JSON and force geometry recomputation (requestLayout).
     */
    fun loadLayout(layoutId: String) {
        layoutHistory.clear()
        primaryLayoutId = layoutId
        switchToLayout(layoutId, pushToHistory = false)
    }

    private fun switchToLayout(layoutId: String, pushToHistory: Boolean) {
        if (layoutId == symbolsLayoutId) {
            MLog.d(logTag, "switchToLayout(symbols) -> showSymbols panel")
            onShowSymbols?.invoke()
            return
        }
        layoutManager.loadAllFromAssets()
        val layout = layoutManager.getLayout(layoutId)
        applyNewLayout(layout, pushToHistory = pushToHistory)
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
    fun onKeyTriggered(keyId: String, trigger: GestureType) {
        val key = model.keys.firstOrNull { it.keyId == keyId } ?: return
        MLog.d(
            logTag,
            "onKeyTriggered layout=${model.currentLayoutId} keyId=$keyId trigger=$trigger label=${key.label}",
        )
        dispatch(KeyboardStateMachine.Event.Triggered(key, trigger))
    }

    /**
     * 处理功能动作（例如 Shift 或布局切换）。
     * Handle an action (e.g. Shift or layout switch).
     */
    fun onAction(action: KeyAction) {
        dispatch(KeyboardStateMachine.Event.ActionEvent(action))
    }

    fun onCandidateSelected(text: String) {
        decodeRequests.trySend(DecodeRequest.CandidateSelected(text))
    }

    fun resetComposing() {
        clearDecodeUiState()
        decodeRequests.trySend(DecodeRequest.Reset)
    }

    fun resetLayoutStateToDefault(
        keepEngineTag: Boolean = true,
        keepLocaleTag: Boolean = true,
    ) {
        val prev = _layoutState.value
        val next =
            LayoutState(
                shift = ShiftState.OFF,
                layer = xyz.xiao6.myboard.model.KeyboardLayer.ALPHA,
                localeTag = if (keepLocaleTag) prev.localeTag else null,
                engine = if (keepEngineTag) prev.engine else null,
                hiddenKeyIds = emptySet(),
                highlightedKeyIds = emptySet(),
                labelOverrides = emptyMap(),
                hintOverrides = emptyMap(),
            )

        val layout = _currentLayout.value
        val invalidateIds =
            layout?.rows?.flatMapTo(LinkedHashSet()) { row -> row.keys.map { it.keyId } }.orEmpty()
        updateState(reducer = { next }, invalidateKeyIds = invalidateIds)
    }

    private fun dispatch(event: KeyboardStateMachine.Event) {
        val (newModel, effects) = KeyboardStateMachine.reduce(model, event)
        model = newModel

        for (effect in effects) {
            when (effect) {
                is KeyboardStateMachine.Effect.CommitText -> {
                    if (effect.text == "\n") {
                        val candidates = _candidates.value
                        val composing = _composingText.value
                        // ENTER with candidates: commit the top candidate.
                        if (candidates.isNotEmpty()) {
                            val top = candidates.first()
                            MLog.d(logTag, "Effect.CommitText(ENTER) -> commitTopCandidate len=${top.length}")
                            onCommitText?.invoke(top)
                            clearDecodeUiState()
                            decodeRequests.trySend(DecodeRequest.Reset)
                            continue
                        }
                        // ENTER with composing but no candidates: commit composing raw.
                        if (composing.isNotEmpty()) {
                            MLog.d(logTag, "Effect.CommitText(ENTER) -> commitComposingRaw len=${composing.length}")
                            onCommitText?.invoke(composing)
                            clearDecodeUiState()
                            decodeRequests.trySend(DecodeRequest.Reset)
                            continue
                        }
                    }

                    MLog.d(logTag, "Effect.CommitText text=${effect.text.take(16)}")
                    decodeRequests.trySend(DecodeRequest.Text(effect.text))
                }

                is KeyboardStateMachine.Effect.PushToken -> {
                    val token = applyShiftToToken(effect.token)
                    MLog.d(logTag, "Effect.PushToken type=${token.type}")
                    decodeRequests.trySend(DecodeRequest.Token(token))

                    if (shouldReleaseShiftAfterToken(token)) {
                        val layout = _currentLayout.value
                        val invalidateIds =
                            layout?.rows?.flatMapTo(LinkedHashSet()) { row -> row.keys.map { it.keyId } }.orEmpty()
                        updateState(
                            reducer = { it.copy(shift = ShiftState.OFF) },
                            invalidateKeyIds = invalidateIds,
                        )
                    }
                }

                KeyboardStateMachine.Effect.CommitComposing -> {
                    val composing = _composingText.value
                    if (composing.isNotEmpty()) {
                        MLog.d(logTag, "Effect.CommitComposing len=${composing.length}")
                        onCommitText?.invoke(composing)
                    } else {
                        MLog.d(logTag, "Effect.CommitComposing (empty)")
                    }
                    clearDecodeUiState()
                    decodeRequests.trySend(DecodeRequest.Reset)
                }

                is KeyboardStateMachine.Effect.SwitchLayout -> {
                    MLog.d(logTag, "Effect.SwitchLayout layoutId=${effect.layoutId}")
                    switchToLayout(effect.layoutId, pushToHistory = true)
                }

                KeyboardStateMachine.Effect.Back -> {
                    val target =
                        layoutHistory.removeLastOrNull()
                            ?: primaryLayoutId.takeIf { it.isNotBlank() && it != model.currentLayoutId }
                            ?: "qwerty"
                    MLog.d(logTag, "Effect.Back -> $target")
                    switchToLayout(target, pushToHistory = false)
                }

                KeyboardStateMachine.Effect.ToggleLocale -> {
                    MLog.d(logTag, "Effect.ToggleLocale")
                    onToggleLocale?.invoke()
                }

                is KeyboardStateMachine.Effect.SwitchLayer -> {
                    MLog.d(logTag, "Effect.SwitchLayer layer=${effect.layer}")
                    clearDecodeUiState()
                    decodeRequests.trySend(DecodeRequest.Reset)

                    val layout = _currentLayout.value
                    val overrideLabels = layout?.let { computeLabelOverrides(it, effect.layer) }.orEmpty()
                    val invalidateIds =
                        if (layout == null) emptySet()
                        else layout.rows.flatMapTo(LinkedHashSet()) { row -> row.keys.map { it.keyId } }

                    updateState(
                        reducer = { s -> s.copy(layer = effect.layer, labelOverrides = overrideLabels) },
                        invalidateKeyIds = invalidateIds,
                    )
                }

                is KeyboardStateMachine.Effect.SwitchEngine -> {
                    MLog.d(logTag, "Effect.SwitchEngine engine=${effect.engine} (no-op for now)")
                }

                is KeyboardStateMachine.Effect.ToggleModifier -> {
                    MLog.d(logTag, "Effect.ToggleModifier modifier=${effect.modifier} (partial)")
                    when (effect.modifier) {
                        ModifierKey.SHIFT -> {
                            val layout = _currentLayout.value
                            val invalidateIds =
                                layout?.rows?.flatMapTo(LinkedHashSet()) { row -> row.keys.map { it.keyId } }.orEmpty()
                            updateState(
                                reducer = { it.copy(shift = if (it.shift == ShiftState.OFF) ShiftState.ON else ShiftState.OFF) },
                                invalidateKeyIds = invalidateIds,
                            )
                        }
                        ModifierKey.CAPS_LOCK -> {
                            val layout = _currentLayout.value
                            val invalidateIds =
                                layout?.rows?.flatMapTo(LinkedHashSet()) { row -> row.keys.map { it.keyId } }.orEmpty()
                            updateState(
                                reducer = { it.copy(shift = if (it.shift == ShiftState.CAPS_LOCK) ShiftState.OFF else ShiftState.CAPS_LOCK) },
                                invalidateKeyIds = invalidateIds,
                            )
                        }
                        ModifierKey.ALT -> Unit
                    }
                }

                KeyboardStateMachine.Effect.ClearComposition -> {
                    clearDecodeUiState()
                    decodeRequests.trySend(DecodeRequest.Reset)
                }

                KeyboardStateMachine.Effect.NoOp -> Unit
            }
        }
    }

    private fun applyDecode(update: DecodeUpdate) {
        applyDecodeUpdate(update)
    }

    private fun applyDecodeUpdate(update: DecodeUpdate) {
        val nextComposing = update.composingText
        if (nextComposing != null && nextComposing != lastLoggedComposing) {
            lastLoggedComposing = nextComposing
            MLog.d(logTag, "composing=\"${nextComposing.take(32)}\"")
        }

        val candidatesHash = update.candidates.hashCode()
        if (candidatesHash != lastLoggedCandidatesHash) {
            lastLoggedCandidatesHash = candidatesHash
            val preview = update.candidates.take(5).joinToString(prefix = "[", postfix = "]")
            MLog.d(logTag, "candidates size=${update.candidates.size} top5=$preview")
        }

        if (update.commitTexts.isNotEmpty()) {
            val preview = update.commitTexts.take(5).joinToString(prefix = "[", postfix = "]") { it.take(8) }
            MLog.d(logTag, "commitTexts size=${update.commitTexts.size} $preview")
        }
        update.composingText?.let { _composingText.value = it }
        _composingOptions.value = update.composingOptions
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

        clearDecodeUiState()
        decodeRequests.trySend(DecodeRequest.Reset)
        _currentLayout.value = layout
        val keys = layout.rows.flatMap { it.keys }
        model = model.copy(
            currentLayoutId = layout.layoutId,
            keys = keys,
        )

        keyboardView?.setLayout(layout)
        keyboardView?.setLayoutState(_layoutState.value, invalidateKeyIds = emptySet())
    }

    private fun clearDecodeUiState() {
        lastLoggedComposing = null
        lastLoggedCandidatesHash = null
        _composingText.value = ""
        _composingOptions.value = emptyList()
        _candidates.value = emptyList()
    }

    private fun onToken(decoder: Decoder, token: KeyToken): DecodeUpdate {
        val tokenDecoder: TokenDecoder =
            (decoder as? TokenDecoder) ?: TokenDecoderAdapter(decoder)
        return tokenDecoder.onToken(token)
    }

    private fun applyShiftToToken(token: KeyToken): KeyToken {
        val shift = _layoutState.value.shift
        if (shift == ShiftState.OFF) return token
        if (token !is KeyToken.Literal) return token
        if (token.text.length != 1) return token
        val c = token.text[0]
        if (!c.isLetter()) return token
        val out = if (shift == ShiftState.OFF) c.lowercaseChar() else c.uppercaseChar()
        return if (out == c) token else KeyToken.Literal(out.toString())
    }

    private fun shouldReleaseShiftAfterToken(token: KeyToken): Boolean {
        val state = _layoutState.value
        if (state.shift != ShiftState.ON) return false
        if (token !is KeyToken.Literal) return false
        if (token.text.length != 1) return false
        return token.text[0].isLetter()
    }

    private fun computeLabelOverrides(layout: KeyboardLayout, layer: KeyboardLayer): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (row in layout.rows) {
            for (key in row.keys) {
                val action = key.actions[GestureType.TAP] ?: continue
                val case = action.cases.firstOrNull { it.whenCondition?.layer == layer } ?: continue
                val first = case.doActions.firstOrNull() ?: continue
                val label =
                    when (first) {
                        is Action.CommitText -> first.text
                        is Action.PushToken -> (first.token as? xyz.xiao6.myboard.model.Token.Literal)?.text
                        else -> null
                    } ?: continue
                out[key.keyId] = label
            }
        }
        return out
    }
}
