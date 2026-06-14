# 整体架构设计

> 本文件定义项目的整体架构、模块划分、核心接口和数据流。

## 1. 项目结构

```
app/
├── core/                          # 核心模块
│   ├── keyboard/                  # 键盘状态机、Action 分发
│   ├── layout/                    # 布局解析、几何计算
│   ├── keybinding/                # Code 映射、Remap
│   └── common/                    # 公共工具类
│
├── input/                         # 输入模块
│   ├── direct/                    # 直接输入（英文等）
│   ├── composing/                 # 组合输入（拼音等）
│   ├── engine/                    # 输入引擎接口
│   └── hybrid/                    # 混合输入
│
├── dictionary/                    # 词典模块
│   ├── trie/                      # Trie 前缀词典
│   ├── user/                      # 用户词典
│   ├── frequency/                 # 词频词典
│   ├── pinyin/                    # 拼音词典
│   ├── japanese/                  # 日文词典
│   ├── korean/                    # 韩文词典
│   ├── import/                    # 词典导入导出
│   └── suggestion/                # 联想引擎
│
├── ai/                            # AI 模块
│   ├── llm/                       # LLM Bridge
│   └── stt/                       # STT Bridge
│
├── ui/                            # UI 模块
│   ├── keyboard/                  # Canvas 按键渲染
│   ├── candidate/                 # Compose 候选栏
│   ├── toolbar/                   # Compose 工具栏
│   ├── popup/                     # 弹出层
│   ├── panel/                     # LLM/STT 面板
│   ├── theme/                     # 主题系统
│   └── settings/                  # 设置界面
│
├── service/                       # 服务模块
│   └── MyBoardImeService.kt       # IME 服务入口
│
└── assets/                        # 资源
    ├── layouts/                   # JSON 布局
    ├── dictionaries/              # 词典文件
    ├── themes/                    # 主题文件
    └── prompts/                   # LLM Prompt 模板
```

## 2. 核心接口定义

### 2.1 InputEngine 接口

```kotlin
interface InputEngine {
    val id: String
    val type: EngineType
    val config: InputMethodConfig

    fun activate()
    fun deactivate()
    fun reset()

    suspend fun onKeyInput(char: String): EngineResult
    suspend fun onBackspace(): EngineResult
    suspend fun onSpace(): EngineResult
    suspend fun onEnter(): EngineResult
    suspend fun onCandidateSelected(index: Int): EngineResult
    suspend fun onShift(): EngineResult
    suspend fun onDoubleShift(): EngineResult

    fun getComposingText(): String
    fun getCandidates(): List<Candidate>
    fun getShiftState(): ShiftState
}

enum class EngineType { DIRECT, COMPOSITION, COMPLEX }

sealed interface EngineResult {
    data class UpdateComposing(val text: String) : EngineResult
    data class CommitText(val text: String) : EngineResult
    data class UpdateCandidates(val candidates: List<Candidate>) : EngineResult
    data class Combined(
        val composing: String?,
        val candidates: List<Candidate>?,
        val commit: String?
    ) : EngineResult
    data object Nothing : EngineResult
}
```

### 2.2 KeyboardState 数据模型

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
    val activePanel: PanelType = PanelType.NONE
)

enum class ShiftState { OFF, ON, CAPS_LOCK }
enum class PanelType { NONE, EMOJI, SYMBOL, CLIPBOARD, LLM, STT }
```

### 2.3 Action 分发

```kotlin
sealed interface InputAction {
    data class CommitText(val text: String) : InputAction
    data class Delete(val count: Int = 1) : InputAction
    data object ToggleShift : InputAction
    data object ToggleDoubleShift : InputAction
    data class SwitchArrangement(val id: String) : InputAction
    data class SwitchLanguage(val id: String) : InputAction
    data class CommitKeyOutput(val key: KeyData) : InputAction
    data class SelectCandidate(val index: Int) : InputAction
    data class OpenPanel(val id: String) : InputAction
    data object ClosePanel : InputAction
    data class LLMComplete(val prompt: String) : InputAction
    data object StartSTT : InputAction
    data object StopSTT : InputAction
    data class MoveCursor(val direction: Direction) : InputAction
    data class PerformEditorAction(val action: ImeAction) : InputAction
}
```

## 3. 核心数据流

```
用户触摸
    ↓
Canvas TouchEvent
    ↓
GestureRecognizer (手势识别)
    ↓
ActionDispatcher (动作分发)
    ├─→ InputConnection (文本输入)
    ├─→ KeyboardState (状态更新)
    ├─→ SuggestionEngine (联想查询)
    ├─→ LLMBridge (AI 调用)
    └─→ STTBridge (语音输入)
          ↓
    Flow<UiState> (响应式更新)
          ↓
    UI Recomposition (界面刷新)
```

## 4. 模块依赖关系

```
service/MyBoardImeService
    ├─→ ui/* (所有 UI 组件)
    ├─→ core/* (核心逻辑)
    ├─→ input/* (输入引擎)
    ├─→ dictionary/* (词典)
    └─→ ai/* (LLM/STT)

core/keyboard
    ├─→ core/layout
    ├─→ input/engine
    └─→ core/keybinding

input/engine
    └─→ dictionary/suggestion

ai/llm
    └─→ core/keyboard (回调)
```

## 5. 技术选型

| 类别 | 技术 | 版本 |
|------|------|------|
| **语言** | Kotlin | 2.0+ |
| **UI** | Jetpack Compose + Canvas | 1.6+ |
| **架构** | MVVM + Clean Architecture | - |
| **DI** | Hilt | 2.51+ |
| **异步** | Coroutines + Flow | 1.8+ |
| **序列化** | kotlinx.serialization | 1.6+ |
| **存储** | DataStore + Room | 1.1+ / 2.6+ |
| **最低 SDK** | API 24 | - |
