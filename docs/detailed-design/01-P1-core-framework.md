# P1: 核心框架 (2 周)

## 1. 目标

构建可用的基础键盘，能在任意 Android 设备上输入英文。

## 2. 里程碑验收标准

- [x] `MyBoardImeService` 可在设置中启用
- [x] 硬编码 QWERTY 布局可渲染
- [x] 点击按键可输入英文字符
- [x] 支持 Shift、Backspace、Space、Enter
- [x] 基本触摸反馈（按下高亮）

## 3. 详细设计

### 3.1 IME 服务搭建

#### 3.1.1 MyBoardImeService

```kotlin
@AndroidEntryPoint
class MyBoardImeService : InputMethodService() {

    @Inject lateinit var keyboardManager: KeyboardManager
    @Inject lateinit var actionDispatcher: ActionDispatcher
    @Inject lateinit var suggestionEngine: SuggestionEngine

    private lateinit var keyboardView: ComposeView

    override fun onCreateInputView(): View {
        keyboardView = ComposeView(this).apply {
            setContent {
                MyBoardTheme {
                    KeyboardScreen(
                        onAction = { actionDispatcher.dispatch(it) }
                    )
                }
            }
        }
        return keyboardView
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        keyboardManager.updateEditorInfo(attribute)
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        keyboardManager.onStartInputView(info)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        keyboardManager.onFinishInputView()
    }

    override fun onUpdateCursorAnchorInfo(cursorAnchorInfo: CursorAnchorInfo?) {
        // 光标位置更新
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        // 选区变化
    }
}
```

#### 3.1.2 input_method.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<input-method xmlns:android="http://schemas.android.com/apk/res/android"
    android:settingsActivity=".ui.SettingsActivity"
    android:isDefault="false"
    android:supportsInlineSuggestions="true">
    <subtype
        android:imeSubtypeMode="keyboard"
        android:isAsciiCapable="true"
        android:subtypeId="1"
        android:label="@string/subtype_english"
        android:languageTag="en" />
</input-method>
```

### 3.2 键盘状态机

#### 3.2.1 KeyboardState

```kotlin
data class KeyboardState(
    val languageId: String = "en_us",
    val inputMethodId: String = "en_qwerty",
    val arrangement: String = "alpha",
    val shiftState: ShiftState = ShiftState.OFF,
    val capsLock: Boolean = false,
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val activePanel: PanelType = PanelType.NONE,
    val isEmojiOpen: Boolean = false,
    val isSymbolOpen: Boolean = false,
    val isClipboardOpen: Boolean = false
) {
    val isComposing: Boolean get() = composingText.isNotEmpty()
    val hasCandidates: Boolean get() = candidates.isNotEmpty()
}

enum class ShiftState { OFF, ON, CAPS_LOCK }
enum class PanelType { NONE, EMOJI, SYMBOL, CLIPBOARD, LLM, STT }
```

#### 3.2.2 KeyboardStateManager

```kotlin
class KeyboardStateManager @Inject constructor() {
    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    fun update(transform: (KeyboardState) -> KeyboardState) {
        _state.update(transform)
    }

    fun reset() {
        _state.value = KeyboardState()
    }

    fun toggleShift() {
        update { s ->
            when (s.shiftState) {
                ShiftState.OFF -> s.copy(shiftState = ShiftState.ON)
                ShiftState.ON -> s.copy(shiftState = ShiftState.OFF)
                ShiftState.CAPS_LOCK -> s.copy(shiftState = ShiftState.OFF, capsLock = false)
            }
        }
    }

    fun toggleCapsLock() {
        update { s ->
            if (s.shiftState == ShiftState.CAPS_LOCK) {
                s.copy(shiftState = ShiftState.OFF, capsLock = false)
            } else {
                s.copy(shiftState = ShiftState.CAPS_LOCK, capsLock = true)
            }
        }
    }

    fun clearComposing() {
        update { it.copy(composingText = "", candidates = emptyList(), selectedCandidateIndex = -1) }
    }
}
```

### 3.3 Action 分发系统

#### 3.3.1 InputAction 定义

```kotlin
sealed interface InputAction {
    data class CommitText(val text: String) : InputAction
    data class Delete(val count: Int = 1) : InputAction
    data object ToggleShift : InputAction
    data object ToggleCapsLock : InputAction
    data class SwitchArrangement(val id: String) : InputAction
    data class SwitchLanguage(val id: String) : InputAction
    data class CommitKeyOutput(val key: KeyData) : InputAction
    data class SelectCandidate(val index: Int) : InputAction
    data class OpenPanel(val id: String) : InputAction
    data object ClosePanel : InputAction
    data class MoveCursor(val direction: Direction) : InputAction
    data class PerformEditorAction(val action: ImeAction) : InputAction
}
```

#### 3.3.2 ActionDispatcher

```kotlin
class ActionDispatcher @Inject constructor(
    private val stateManager: KeyboardStateManager,
    private val inputConnectionProvider: InputConnectionProvider
) {
    suspend fun dispatch(action: InputAction) {
        when (action) {
            is InputAction.CommitText -> commitText(action.text)
            is InputAction.Delete -> delete(action.count)
            is InputAction.ToggleShift -> stateManager.toggleShift()
            is InputAction.ToggleCapsLock -> stateManager.toggleCapsLock()
            is InputAction.SwitchArrangement -> switchArrangement(action.id)
            is InputAction.SwitchLanguage -> switchLanguage(action.id)
            is InputAction.SelectCandidate -> selectCandidate(action.index)
            is InputAction.OpenPanel -> openPanel(action.id)
            is InputAction.ClosePanel -> closePanel()
            is InputAction.MoveCursor -> moveCursor(action.direction)
            is InputAction.PerformEditorAction -> performEditorAction(action.action)
            else -> {}
        }
    }

    private suspend fun commitText(text: String) {
        val ic = inputConnectionProvider.current ?: return
        ic.commitText(text, 1)
        stateManager.clearComposing()
    }

    private suspend fun delete(count: Int) {
        val ic = inputConnectionProvider.current ?: return
        ic.deleteSurroundingText(count, 0)
    }

    private fun switchArrangement(id: String) {
        stateManager.update { it.copy(arrangement = id) }
    }

    private fun switchLanguage(id: String) {
        stateManager.update { it.copy(languageId = id) }
    }

    private fun selectCandidate(index: Int) {
        val state = stateManager.state.value
        val candidate = state.candidates.getOrNull(index) ?: return
        stateManager.update {
            it.copy(
                composingText = "",
                candidates = emptyList(),
                selectedCandidateIndex = -1
            )
        }
        commitText(candidate.text)
    }

    private fun openPanel(id: String) {
        val panel = when (id) {
            "emoji" -> PanelType.EMOJI
            "symbol" -> PanelType.SYMBOL
            "clipboard" -> PanelType.CLIPBOARD
            "llm" -> PanelType.LLM
            "stt" -> PanelType.STT
            else -> PanelType.NONE
        }
        stateManager.update { it.copy(activePanel = panel) }
    }

    private fun closePanel() {
        stateManager.update { it.copy(activePanel = PanelType.NONE) }
    }

    private fun moveCursor(direction: Direction) {
        val ic = inputConnectionProvider.current ?: return
        val current = ic.getExtractedText(ExtractedTextRequest(), 0)
        val newPos = when (direction) {
            Direction.LEFT -> (current?.start ?: 0) - 1
            Direction.RIGHT -> (current?.start ?: 0) + 1
            else -> current?.start ?: 0
        }
        ic.setSelection(newPos, newPos)
    }

    private fun performEditorAction(action: ImeAction) {
        val ic = inputConnectionProvider.current ?: return
        ic.performEditorAction(action.imeOptions)
    }
}
```

### 3.4 Canvas 渲染器

#### 3.4.1 KeyboardCanvas

```kotlin
class KeyboardCanvas @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var layout: KeyboardLayout? = null
    private var state: KeyboardState = KeyboardState()
    private var keyRects: Map<String, RectF> = emptyMap()
    private var theme: ThemeRuntime? = null

    var onTrigger: ((keyId: String, trigger: GestureType) -> Unit)? = null

    private val keyFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = sp(18f)
    }

    fun setLayout(layout: KeyboardLayout) {
        this.layout = layout
        requestLayout()
        invalidate()
    }

    fun setState(state: KeyboardState) {
        this.state = state
        invalidate()
    }

    fun setTheme(theme: ThemeRuntime?) {
        this.theme = theme
        invalidate()
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        layout?.let { keyRects = GeometryCalculator.computeKeyRects(it, width, height) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        layout?.let { drawKeys(canvas, it, state) }
    }

    private fun drawKeys(canvas: Canvas, layout: KeyboardLayout, state: KeyboardState) {
        for (row in layout.rows) {
            for (key in row.keys) {
                val rect = keyRects[key.keyId] ?: continue
                drawKey(canvas, key, rect, state)
            }
        }
    }

    private fun drawKey(canvas: Canvas, key: Key, rect: RectF, state: KeyboardState) {
        val isPressed = state.highlightedKeyIds.contains(key.keyId)
        val style = theme?.resolveKeyStyle(key.ui.styleId)

        // 背景
        keyFillPaint.color = if (isPressed) {
            style?.backgroundPressed?.color ?: Color.parseColor("#E5E5EA")
        } else {
            style?.background?.color ?: Color.WHITE
        }
        canvas.drawRoundRect(rect, dp(8f), dp(8f), keyFillPaint)

        // 标签
        val label = resolveLabel(key, state)
        if (label.isNotBlank()) {
            labelPaint.color = style?.label?.color ?: Color.BLACK
            val x = rect.centerX()
            val y = rect.centerY() - (labelPaint.ascent() + labelPaint.descent()) / 2f
            canvas.drawText(label, x, y, labelPaint)
        }
    }

    private fun resolveLabel(key: Key, state: KeyboardState): String {
        val base = key.ui.label ?: key.label ?: ""
        if (base.isBlank()) return base
        return when (state.shiftState) {
            ShiftState.OFF -> base.lowercase()
            ShiftState.ON, ShiftState.CAPS_LOCK -> base.uppercase()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = hitTestKey(event.x, event.y) ?: return false
                onTrigger?.invoke(hit.keyId, GestureType.TAP)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun hitTestKey(x: Float, y: Float): Key? {
        for ((id, rect) in keyRects) {
            if (rect.contains(x, y)) {
                return layout?.rows?.flatMap { it.keys }?.firstOrNull { it.keyId == id }
            }
        }
        return null
    }
}
```

## 4. 文件清单

| 文件 | 说明 | 状态 |
|------|------|------|
| `service/MyBoardImeService.kt` | IME 服务入口 | 待实现 |
| `core/keyboard/KeyboardState.kt` | 状态数据模型 | 待实现 |
| `core/keyboard/KeyboardStateManager.kt` | 状态管理 | 待实现 |
| `core/keyboard/ActionDispatcher.kt` | Action 分发 | 待实现 |
| `core/keyboard/InputAction.kt` | Action 定义 | 待实现 |
| `ui/keyboard/KeyboardCanvas.kt` | Canvas 渲染 | 待实现 |
| `ui/keyboard/GeometryCalculator.kt` | 几何计算 | 待实现 |
| `res/xml/input_method.xml` | IME 配置 | 待创建 |
| `res/layout/keyboard_layout.xml` | 键盘布局 | 待创建 |
