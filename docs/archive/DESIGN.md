# 全球化智能软键盘 - 概要设计文档

> 版本: v1.0 | 状态: Draft | 日期: 2026-06-13

---

## 一、项目概述

### 1.1 目标

构建一个**高度可定制的全球化智能软键盘**，具备以下核心能力：

| 能力 | 说明 |
|------|------|
| **多语言输入** | 优先支持中文拼音、英文、日语、韩语；扩展支持藏语、法语、阿拉伯语等全球文字 |
| **高度自定义** | 键盘布局、候选栏、工具栏、符号栏均可自由定制 |
| **智能词典** | 支持联想、中英混合输入、词典导入导出 |
| **主题系统** | 内置基础主题，支持动效、GIF、自定义图片主题 |
| **AI 增强** | 接入 LLM（本地/云端），支持翻译、语句美化、智能联想 |
| **语音输入** | 支持系统级 STT 和端侧模型接入 |

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| **模块化** | 每个功能独立模块，可单独开发测试 |
| **可扩展** | 新语言、新功能通过插件式接入 |
| **高性能** | 核心渲染 60fps，输入延迟 < 16ms |
| **离线优先** | 基础功能完全离线可用 |
| **隐私安全** | 用户数据本地存储，云端调用可选 |

---

## 二、技术选型

### 2.1 核心技术栈

| 类别 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **语言** | Kotlin | 2.0+ | 主开发语言，协程支持 |
| **UI 框架** | Jetpack Compose | 1.6+ | 候选栏、工具栏、设置面板 |
| **Canvas 渲染** | Android Canvas | - | 按键核心渲染（性能最优） |
| **架构模式** | MVVM + Clean Architecture | - | 数据视图分离 |
| **依赖注入** | Hilt | 2.51+ | 编译时安全 DI |
| **异步框架** | Kotlin Coroutines + Flow | 1.8+ | 响应式数据流 |
| **序列化** | kotlinx.serialization | 1.6+ | JSON 解析 |
| **本地存储** | DataStore Preferences | 1.1+ | 轻量级配置存储 |
| **数据库** | Room | 2.6+ | 词典、用户数据 |
| **图片加载** | Coil | 2.6+ | 主题图片/GIF 加载 |
| **构建工具** | Gradle | 8.4+ | Kotlin DSL |
| **最低 SDK** | API 24 (Android 7.0) | - | 覆盖 95%+ 设备 |

### 2.2 AI 技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| **本地 LLM** | ONNX Runtime Mobile | 1.17+ | 端侧模型推理 |
| **云端 LLM** | OkHttp + SSE | 4.12+ | 流式 API 调用 |
| **系统 STT** | Android SpeechRecognizer | 系统 API | 零依赖语音输入 |
| **端侧 STT** | Whisper.cpp (JNI) | - | 离线语音识别 |

### 2.3 测试技术栈

| 类别 | 技术 | 说明 |
|------|------|------|
| **单元测试** | JUnit 5 + MockK | 核心逻辑测试 |
| **协程测试** | kotlinx-coroutines-test | 异步逻辑测试 |
| **Flow 测试** | Turbine | Flow 断言 |
| **Compose 测试** | Compose Testing | UI 组件测试 |
| **集成测试** | Espresso | 端到端测试 |

---

## 三、整体架构设计

### 3.1 分层架构

```
┌─────────────────────────────────────────────────────────────────┐
│                         IME Window                              │
├──────────┬──────────┬──────────┬──────────┬────────────────────┤
│ Toolbar  │ Candidate│ Keyboard │  Popup   │   LLM/STT Panel   │
│ (Compose)│ (Compose)│ (Canvas) │ (Compose)│    (Compose)       │
├──────────┴──────────┴──────────┴──────────┴────────────────────┤
│                        View Layer                               │
├─────────────────────────────────────────────────────────────────┤
│                    Presentation Layer                           │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐              │
│  │  KeyboardVM │ │CandidateVM  │ │  ThemeVM    │              │
│  └─────────────┘ └─────────────┘ └─────────────┘              │
├─────────────────────────────────────────────────────────────────┤
│                       Domain Layer                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Layout   │ │ Dictionary│ │ Suggest  │ │ Input    │          │
│  │ Engine   │ │ Manager  │ │ Engine   │ │ Engine   │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐                       │
│  │ KeyBind  │ │ Theme    │ │ LLM      │ │ STT      │          │
│  │ Manager  │ │ Manager  │ │ Bridge   │ │ Bridge   │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                        Data Layer                               │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ Layout   │ │ Dict     │ │ User     │ │ Theme    │          │
│  │ Repository│ │ Repository│ │ Prefs   │ │ Repository│         │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
├─────────────────────────────────────────────────────────────────┤
│                       Infrastructure                           │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ JSON     │ │ Trie     │ │ DataStore│ │ ONNX     │          │
│  │ Parser   │ │ Engine   │ │          │ │ Runtime  │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 模块划分

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
│   ├── composing/                 # 组合输入（拼音、罗马字等）
│   ├── engine/                    # 输入引擎接口
│   └── hybrid/                    # 混合输入（中英切换）
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
│   │   ├── provider/              # Provider 接口
│   │   ├── local/                 # 本地 ONNX
│   │   ├── cloud/                 # 云端 API
│   │   └── pipeline/             # Prompt/Stream/Parse
│   └── stt/                       # STT Bridge
│       ├── provider/              # Provider 接口
│       ├── android/               # 系统 STT
│       └── whisper/               # Whisper.cpp
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
│   ├── MyBoardImeService.kt       # IME 服务入口
│   └── KeyboardManager.kt        # 键盘生命周期管理
│
└── assets/                        # 资源
    ├── layouts/                   # JSON 布局
    │   ├── latin/                 # 拉丁语系
    │   ├── cjk/                   # 中日韩
    │   ├── indic/                 # 印度系文字
    │   ├── complex/               # 复杂文字（阿拉伯、藏文）
    │   └── symbol/                # 符号
    ├── dictionaries/              # 词典文件
    ├── themes/                    # 主题文件
    └── prompts/                   # LLM Prompt 模板
```

### 3.3 核心数据流

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

---

## 四、功能模块详细设计

### 4.1 多语言输入系统

#### 4.1.1 语言包架构

```kotlin
data class LanguagePack(
    val id: String,                    // "zh_pinyin", "ja_romaji"
    val meta: LanguageMeta,
    val script: ScriptConfig,
    val inputMethod: InputMethodConfig,
    val layouts: List<LayoutRef>,
    val dictionary: DictionaryConfig,
    val rules: TextRules
)

data class LanguageMeta(
    val name: String,                  // "中文 (拼音)"
    val displayName: String,          // "Chinese (Pinyin)"
    val locale: String,               // "zh-CN"
    val tags: List<String>,           // ["cjk", "composition"]
    val scripts: List<String>,        // ["Han", "Latin"]
    val isRtl: Boolean = false,
    val priority: Int = 0             // 优先级，用于排序
)

enum class ScriptType {
    ALPHABETIC,        // 拉丁、西里尔
    SYLLABARY,         // 韩文谚文
    LOGOGRAPHIC,       // 中文汉字
    ABUGIDA,           // 印度系（天城文、藏文）
    ABJAD,             // 阿拉伯系
    SYMBOL             // 符号系统
}
```

#### 4.1.2 支持的语言列表

| 语言 | ID | 类型 | 输入法 | 优先级 |
|------|-----|------|--------|--------|
| 英语 | `en_us` | 直接输入 | QWERTY | P0 |
| 中文 | `zh_cn` | 组合输入 | 拼音/五笔/双拼/笔画 | P0 |
| 日语 | `ja_jp` | 组合输入 | 罗马字/假名 | P0 |
| 韩语 | `ko_kr` | 复合输入 | 谚文 | P0 |
| 法语 | `fr_fr` | 直接输入 | AZERTY | P1 |
| 德语 | `de_de` | 直接输入 | QWERTZ | P1 |
| 西班牙语 | `es_es` | 直接输入 | QWERTY | P1 |
| 葡萄牙语 | `pt_br` | 直接输入 | QWERTY | P1 |
| 俄语 | `ru_ru` | 直接输入 | ЙЦУКЕН | P1 |
| 阿拉伯语 | `ar` | 复杂文字 | Arabic | P2 |
| 希伯来语 | `he` | 复杂文字 | Hebrew | P2 |
| 印地语 | `hi_in` | Abugida | 天城文 | P2 |
| 泰语 | `th_th` | Abugida | Thai | P2 |
| 藏语 | `bo` | Abugida | Tibetan | P2 |


#### 4.1.3 核心设计原则：JSON 驱动，非硬编码

**关键原则**：所有输入法引擎、布局、行为规则均由 JSON 配置定义，Kotlin 代码只提供通用引擎解析器，不包含任何特定语言的硬编码逻辑。

```
┌─────────────────────────────────────────────────────────────┐
│                    InputMethodRegistry                      │
│  (运行时从 JSON 动态注册所有输入法)                           │
├─────────────────────────────────────────────────────────────┤
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │ DirectEngine │  │ Composition  │  │ ComplexEngine│     │
│  │ (通用直接输入) │  │ Engine       │  │ (通用复杂文字) │     │
│  │              │  │ (通用组合输入) │  │              │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                 │                 │              │
│         └────────┬────────┘────────┬────────┘              │
│                  ▼                 ▼                        │
│         ┌─────────────────────────────────┐                │
│         │   JSON Configuration            │                │
│         │   (定义所有输入法行为)            │                │
│         └─────────────────────────────────┘                │
└─────────────────────────────────────────────────────────────┘
```

---

#### 4.1.4 输入法配置 Schema

每个输入法通过 JSON 配置定义其全部行为：

```jsonc
{
  "inputMethod": {
    "id": "pinyin",
    "name": "拼音",
    "engine": "composition",           // direct | composition | complex
    "language": "zh-CN",

    // 引擎参数：由 composition 引擎解析
    "engineParams": {
      "autoCommitOnSpace": true,       // 空格提交组合
      "autoCommitOnPunctuation": true, // 标点提交组合
      "fuzzyPinyin": false,            // 模糊拼音
      "maxComposingLength": 30         // 最大组合长度
    },

    // 按键到引擎的映射规则
    "keyMapping": {
      "tap": "appendToComposition",    // tap 时追加到组合缓冲区
      "longPress": "showPopup"
    },

    // Shift 行为
    "shift": {
      "mode": "autoOff",              // autoOff | autoCapitalize | manual | disabled
      // autoOff: 按键后自动回到小写（英文常用）
      // autoCapitalize: 句首自动大写
      // manual: 用户手动切换，不会自动回退
      // disabled: 不支持 shift（中文不需要）
      "autoOffAfterKeys": true        // 输入一个字符后自动关闭 shift
    },

    // 回车行为
    "enter": {
      "idle": "editorAction",         // 空闲时：执行 EditorAction
      "composing": "commitThenAction", // 组合中：先提交组合，再执行 EditorAction
      "hasCandidates": "selectFirst"   // 有候选时：选中第一个候选
    },

    // Backspace 行为
    "backspace": {
      "idle": "delete",               // 空闲时：删除前一个字符
      "composing": "deleteComposition" // 组合中：删除组合缓冲区最后一个字符
    },

    // 空格行为
    "space": {
      "idle": "commitText",           // 空闲时：输入空格
      "composing": "commitComposition", // 组合中：提交组合
      "hasCandidates": "selectFirst"   // 有候选时：选中第一个候选
    }
  }
}
```

---

#### 4.1.5 通用引擎实现

##### 4.1.5.1 直接输入引擎

```kotlin
/**
 * 通用直接输入引擎：适用于英文、法语、德语等直接输入语言。
 * 所有行为由 JSON 配置驱动。
 */
class DirectInputEngine(
    private val config: InputMethodConfig,
    private val userDict: UserDict
) : InputEngine {
    override val id = config.id
    override val type = EngineType.DIRECT

    private var shiftState: ShiftState = ShiftState.OFF
    private var capsLock: Boolean = false

    override suspend fun onKeyInput(char: String): EngineResult {
        val output = when {
            capsLock -> char.uppercase()
            shiftState == ShiftState.ON -> {
                // 根据 shift.mode 决定是否自动回退
                if (config.shift?.autoOffAfterKeys == true) {
                    shiftState = ShiftState.OFF
                }
                char.uppercase()
            }
            else -> char.lowercase()
        }

        // 记录到用户词典
        userDict.recordWord(output)

        return EngineResult.CommitText(output)
    }

    override suspend fun onShift(): EngineResult {
        return when (config.shift?.mode) {
            "disabled" -> EngineResult.Nothing
            "manual" -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.OFF
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                EngineResult.Nothing
            }
            else -> {
                shiftState = when (shiftState) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.OFF
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                EngineResult.Nothing
            }
        }
    }

    override suspend fun onDoubleShift(): EngineResult {
        if (config.shift?.mode == "disabled") return EngineResult.Nothing
        capsLock = !capsLock
        shiftState = if (capsLock) ShiftState.CAPS_LOCK else ShiftState.OFF
        return EngineResult.Nothing
    }
}
```

##### 4.1.5.2 组合输入引擎

```kotlin
/**
 * 通用组合输入引擎：适用于拼音、五笔、双拼、罗马字等组合输入。
 * 所有行为由 JSON 配置驱动。
 */
class CompositionInputEngine(
    private val config: InputMethodConfig,
    private val dictLookup: DictLookup,
    private val composingResolver: ComposingResolver
) : InputEngine {
    override val id = config.id
    override val type = EngineType.COMPOSITION

    private val composingBuffer = StringBuilder()
    private var candidates: List<Candidate> = emptyList()

    override suspend fun onKeyInput(char: String): EngineResult {
        composingBuffer.append(char)

        // 通过 composingResolver 解析组合缓冲区
        // composingResolver 根据 engineParams 决定如何解析
        val resolved = composingResolver.resolve(
            buffer = composingBuffer.toString(),
            params = config.engineParams
        )

        candidates = dictLookup.lookup(resolved, config.engineParams)

        return EngineResult.Combined(
            composing = resolved.displayText,
            candidates = candidates,
            commit = null
        )
    }

    override suspend fun onSpace(): EngineResult {
        return when {
            composingBuffer.isEmpty() -> {
                // 空闲时：根据 enter.space 配置
                EngineResult.CommitText(" ")
            }
            candidates.isNotEmpty() -> {
                // 有候选时：选中第一个
                val first = candidates.first()
                composingBuffer.clear()
                candidates = emptyList()
                EngineResult.CommitText(first.text)
            }
            else -> {
                // 组合中但无候选：根据 engineParams.autoCommitOnSpace
                if (config.engineParams["autoCommitOnSpace"] == true) {
                    val text = composingBuffer.toString()
                    composingBuffer.clear()
                    EngineResult.CommitText(text)
                } else {
                    EngineResult.Nothing
                }
            }
        }
    }

    override suspend fun onEnter(): EngineResult {
        return when (config.enter?.composing) {
            "commitThenAction" -> {
                // 先提交组合，再执行 EditorAction
                val text = composingBuffer.toString()
                composingBuffer.clear()
                candidates = emptyList()
                EngineResult.Combined(composing = null, candidates = null, commit = text)
            }
            "selectFirst" -> {
                if (candidates.isNotEmpty()) {
                    val first = candidates.first()
                    composingBuffer.clear()
                    candidates = emptyList()
                    EngineResult.CommitText(first.text)
                } else {
                    val text = composingBuffer.toString()
                    composingBuffer.clear()
                    EngineResult.CommitText(text)
                }
            }
            else -> EngineResult.Nothing
        }
    }

    override suspend fun onBackspace(): EngineResult {
        return when {
            composingBuffer.isNotEmpty() -> {
                composingBuffer.deleteCharAt(composingBuffer.length - 1)
                if (composingBuffer.isEmpty()) {
                    candidates = emptyList()
                    EngineResult.UpdateComposing("")
                } else {
                    val resolved = composingResolver.resolve(
                        composingBuffer.toString(), config.engineParams
                    )
                    candidates = dictLookup.lookup(resolved, config.engineParams)
                    EngineResult.Combined(resolved.displayText, candidates, null)
                }
            }
            else -> EngineResult.Delete(1)
        }
    }

    override suspend fun onCandidateSelected(index: Int): EngineResult {
        val candidate = candidates.getOrNull(index) ?: return EngineResult.Nothing
        composingBuffer.clear()
        candidates = emptyList()
        return EngineResult.CommitText(candidate.text)
    }

    override fun reset() {
        composingBuffer.clear()
        candidates = emptyList()
    }

    override fun getComposingText(): String = composingBuffer.toString()
    override fun getCandidates(): List<Candidate> = candidates
}
```

##### 4.1.5.3 复杂文字引擎

```kotlin
/**
 * 通用复杂文字引擎：适用于韩文 Jamo 组合、藏文叠加、阿拉伯连字等。
 * 所有组合规则由 JSON 配置定义。
 */
class ComplexInputEngine(
    private val config: InputMethodConfig,
    private val composingResolver: ComposingResolver
) : InputEngine {
    override val id = config.id
    override val type = EngineType.COMPLEX

    private val buffer = mutableListOf<String>()

    override suspend fun onKeyInput(char: String): EngineResult {
        buffer.add(char)
        val resolved = composingResolver.resolve(
            buffer.joinToString(""), config.engineParams
        )
        return EngineResult.UpdateComposing(resolved.displayText)
    }

    override suspend fun onBackspace(): EngineResult {
        if (buffer.isNotEmpty()) {
            buffer.removeAt(buffer.size - 1)
            if (buffer.isEmpty()) {
                return EngineResult.UpdateComposing("")
            }
            val resolved = composingResolver.resolve(
                buffer.joinToString(""), config.engineParams
            )
            return EngineResult.UpdateComposing(resolved.displayText)
        }
        return EngineResult.Delete(1)
    }

    override suspend fun onSpace(): EngineResult {
        val text = buffer.joinToString("")
        buffer.clear()
        return EngineResult.CommitText(text)
    }

    override fun reset() {
        buffer.clear()
    }
}
```

---

#### 4.1.6 各语言输入法配置示例

##### 4.1.6.1 英文 QWERTY

```jsonc
{
  "inputMethod": {
    "id": "en_qwerty",
    "name": "English (QWERTY)",
    "engine": "direct",
    "language": "en-US",
    "shift": { "mode": "autoOff", "autoOffAfterKeys": true },
    "enter": { "idle": "editorAction", "composing": "editorAction" },
    "space": { "idle": "commitText" },
    "backspace": { "idle": "delete" }
  }
}
```

##### 4.1.6.2 中文拼音

```jsonc
{
  "inputMethod": {
    "id": "zh_pinyin",
    "name": "中文 (拼音)",
    "engine": "composition",
    "language": "zh-CN",
    "engineParams": {
      "autoCommitOnSpace": true,
      "autoCommitOnPunctuation": true,
      "fuzzyPinyin": false,
      "maxComposingLength": 30,
      "composingType": "pinyin"         // composition 引擎根据此参数选择解析逻辑
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.6.3 中文五笔

```jsonc
{
  "inputMethod": {
    "id": "zh_wubi",
    "name": "中文 (五笔)",
    "engine": "composition",
    "language": "zh-CN",
    "engineParams": {
      "autoCommitOnSpace": true,
      "autoCommitOnPunctuation": true,
      "maxComposingLength": 4,
      "composingType": "wubi"
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.6.4 中文双拼

```jsonc
{
  "inputMethod": {
    "id": "zh_shuangpin",
    "name": "中文 (双拼)",
    "engine": "composition",
    "language": "zh-CN",
    "engineParams": {
      "autoCommitOnSpace": true,
      "autoCommitOnPunctuation": true,
      "maxComposingLength": 2,
      "composingType": "shuangpin",
      "shuangpinScheme": "xiaohe"       // 双拼方案：小鹤/自然码/微软
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.6.5 中文笔画

```jsonc
{
  "inputMethod": {
    "id": "zh_stroke",
    "name": "中文 (笔画)",
    "engine": "composition",
    "language": "zh-CN",
    "engineParams": {
      "autoCommitOnSpace": true,
      "maxComposingLength": 10,
      "composingType": "stroke",
      "wildcardKey": "?"                // 通配符按键
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.6.6 日文罗马字

```jsonc
{
  "inputMethod": {
    "id": "ja_romaji",
    "name": "日本語 (ローマ字)",
    "engine": "composition",
    "language": "ja-JP",
    "engineParams": {
      "autoCommitOnSpace": true,
      "autoCommitOnPunctuation": true,
      "maxComposingLength": 20,
      "composingType": "romaji",
      "romajiTable": "hepburn"          // 罗马字方案：黑本/训令
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.6.7 韩文谚文

```jsonc
{
  "inputMethod": {
    "id": "ko_hangul",
    "name": "한국어",
    "engine": "complex",
    "language": "ko-KR",
    "engineParams": {
      "composingType": "hangul",
      "autoCompose": true,              // 自动组合 Jamo → 音节
      "enableDecompose": true           // 支持音节分解
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

---

#### 4.1.7 键盘状态机（统一）

##### 4.1.7.1 状态模型

```kotlin
data class KeyboardState(
    // 当前语言和输入法
    val languageId: String = "en_us",
    val inputMethodId: String = "en_qwerty",

    // 当前 arrangement（布局排列）
    val arrangement: String = "alpha",    // alpha | symbols | numbers | phone | custom

    // 修饰键状态（每个语言独立维护）
    val shiftState: ShiftState = ShiftState.OFF,
    val capsLock: Boolean = false,

    // 组合状态
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,

    // 面板状态
    val activePanel: PanelType = PanelType.NONE,
    val isEmojiOpen: Boolean = false,
    val isSymbolOpen: Boolean = false,
    val isClipboardOpen: Boolean = false
)

enum class ShiftState { OFF, ON, CAPS_LOCK }
enum class PanelType { NONE, EMOJI, SYMBOL, CLIPBOARD, LLM, STT }
```

##### 4.1.7.2 语言切换状态管理

```kotlin
/**
 * 语言切换时的状态管理策略。
 *
 * 核心问题：
 * - 中文切英文时，shift 状态如何处理？
 * - 组合缓冲区是否保留？
 * - arrangement 是否重置？
 * - 各语言的 shift 状态是否独立？
 */
data class LanguageSwitchBehavior(
    // 切换时是否清空组合缓冲区
    val clearComposing: Boolean = true,

    // 切换时是否重置 arrangement 到默认
    val resetArrangement: Boolean = true,

    // shift 状态策略
    val shiftBehavior: ShiftSwitchBehavior = ShiftSwitchBehavior.RESET_TO_OFF,

    // 是否记住每个语言的 arrangement
    val rememberArrangementPerLanguage: Boolean = true,

    // 是否记住每个语言的 shift 状态
    val rememberShiftPerLanguage: Boolean = false
)

enum class ShiftSwitchBehavior {
    RESET_TO_OFF,        // 重置为 OFF（推荐：中文→英文时）
    PRESERVE,            // 保持当前状态
    LANGUAGE_DEFAULT     // 使用目标语言的默认值
}
```

##### 4.1.7.3 状态切换流程

```
用户点击语言切换键
    ↓
┌─────────────────────────────────────────────────────────┐
│              LanguageSwitchHandler                       │
├─────────────────────────────────────────────────────────┤
│ 1. 保存当前语言状态                                       │
│    - shift/capsLock → 保存到 stateMap[currentLang]      │
│    - arrangement → 保存到 arrangementMap[currentLang]    │
│                                                         │
│ 2. 清空当前组合（如果 clearComposing = true）             │
│    - 清空 composingBuffer                               │
│    - 清空 candidates                                    │
│                                                         │
│ 3. 加载目标语言                                           │
│    - 从 stateMap[targetLang] 恢复 shift 状态             │
│    - 从 arrangementMap[targetLang] 恢复 arrangement     │
│    - 加载目标语言的 InputMethodConfig                    │
│    - 切换 InputEngine                                   │
│                                                         │
│ 4. 触发 UI 更新                                          │
│    - 重新渲染键盘布局                                    │
│    - 更新候选栏                                          │
│    - 更新工具栏                                          │
└─────────────────────────────────────────────────────────┘
```

##### 4.1.7.4 各语言状态配置

```jsonc
{
  "languageSwitch": {
    "clearComposing": true,
    "resetArrangement": true,
    "shiftBehavior": "resetToOff",
    "rememberArrangementPerLanguage": true,
    "rememberShiftPerLanguage": false,

    // 快捷切换对（常用组合）
    "quickSwitchPairs": [
      { "from": "en_us", "to": "zh_cn", "gesture": "swipeSpace" },
      { "from": "zh_cn", "to": "en_us", "gesture": "doubleTapSpace" }
    ],

    // 语言循环列表（长按语言键）
    "languageCycle": ["en_us", "zh_cn", "ja_jp", "ko_kr"]
  }
}
```

---

#### 4.1.8 Shift 行为详细设计

##### 4.1.8.1 各语言 Shift 行为

| 语言 | shift.mode | 说明 |
|------|-----------|------|
| **英文** | `autoOff` | 点击后输入一个大写字母，然后自动回到小写 |
| **英文 (Caps Lock)** | `autoOff` | 双击 shift 锁定大写，再双击解锁 |
| **中文拼音** | `disabled` | 中文输入不需要 shift |
| **中文五笔** | `disabled` | 形码输入不需要 shift |
| **日文** | `disabled` | 假名输入不需要 shift |
| **韩文** | `disabled` | 谚文输入不需要 shift |
| **法语** | `autoOff` | 同英文 |
| **德语** | `autoOff` | 同英文 |
| **阿拉伯语** | `disabled` | 阿拉伯文不需要 shift |
| **藏语** | `disabled` | 藏文不需要 shift |

##### 4.1.8.2 Shift 切换流程

```
用户点击 Shift
    ↓
┌─────────────────────────────────────────────────────────┐
│  config.shift.mode == "disabled"?                       │
│  ├─ YES → 忽略，不改变状态                               │
│  └─ NO → 继续                                           │
├─────────────────────────────────────────────────────────┤
│  config.shift.mode == "autoOff"?                        │
│  ├─ YES → ON → 输入一个字符后自动回到 OFF                │
│  └─ NO → 继续                                           │
├─────────────────────────────────────────────────────────┤
│  双击 Shift?                                             │
│  ├─ YES → 切换 CapsLock                                 │
│  └─ NO → 切换 ON/OFF                                    │
└─────────────────────────────────────────────────────────┘
```

---

#### 4.1.9 回车行为详细设计

##### 4.1.9.1 各场景回车行为

| 场景 | enter.idle | enter.composing | enter.hasCandidates |
|------|-----------|-----------------|---------------------|
| **英文** | editorAction | editorAction | editorAction |
| **中文拼音** | editorAction | commitThenAction | selectFirst |
| **中文五笔** | editorAction | commitThenAction | selectFirst |
| **日文** | editorAction | commitThenAction | selectFirst |
| **韩文** | editorAction | commitThenAction | selectFirst |
| **数字键盘** | editorAction | editorAction | editorAction |
| **密码输入** | editorAction | editorAction | editorAction |

##### 4.1.9.2 回车处理流程

```
用户点击 Enter
    ↓
┌─────────────────────────────────────────────────────────┐
│  当前状态判断                                            │
│  ├─ composingBuffer.isEmpty()?                          │
│  │   └─ YES → 使用 enter.idle 配置                      │
│  ├─ candidates.isNotEmpty()?                            │
│  │   └─ YES → 使用 enter.hasCandidates 配置              │
│  └─ 其他 → 使用 enter.composing 配置                     │
├─────────────────────────────────────────────────────────┤
│  执行对应动作                                            │
│  ├─ editorAction: 执行 EditorInfo.imeAction              │
│  ├─ commitThenAction: 先提交 composingText，再执行 Action │
│  └─ selectFirst: 选中第一个候选，清空组合                 │
└─────────────────────────────────────────────────────────┘
```

---

#### 4.1.10 日语输入法详细设计

##### 4.1.10.1 罗马字配置

```jsonc
{
  "inputMethod": {
    "id": "ja_romaji",
    "name": "日本語 (ローマ字)",
    "engine": "composition",
    "language": "ja-JP",
    "engineParams": {
      "composingType": "romaji",
      "romajiTable": "hepburn",
      "autoCommitOnSpace": true,
      "autoCommitOnPunctuation": true,
      "enableKanaDirect": true,         // 支持直接假名输入
      "enableKanjiConversion": true     // 支持假名→汉字转换
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.10.2 假名直接输入配置

```jsonc
{
  "inputMethod": {
    "id": "ja_kana",
    "name": "日本語 (かな)",
    "engine": "complex",
    "language": "ja-JP",
    "engineParams": {
      "composingType": "kana",
      "kanaLayout": "thumb"             // 五十音键盘布局
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

---

#### 4.1.11 韩语输入法详细设计

```jsonc
{
  "inputMethod": {
    "id": "ko_hangul",
    "name": "한국어",
    "engine": "complex",
    "language": "ko-KR",
    "engineParams": {
      "composingType": "hangul",
      "autoCompose": true,
      "enableDecompose": true,
      "enableJamoDirect": false          // 是否允许直接输入 Jamo
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction" },
    "space": { "idle": "commitText", "composing": "commitComposition" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

---

#### 4.1.12 中英文混合输入

##### 4.1.12.1 混合输入模式

```jsonc
{
  "inputMethod": {
    "id": "zh_pinyin_hybrid",
    "name": "中文 (拼音混合)",
    "engine": "composition",
    "language": "zh-CN",
    "engineParams": {
      "composingType": "pinyin",
      "enableHybridInput": true,         // 启用中英混合
      "hybridMode": "autoDetect",        // autoDetect | manualSwitch
      "englishInChinese": {
        "autoSwitchToEnglish": true,      // 检测到英文输入时自动切换
        "switchThreshold": 3              // 连续输入 N 个英文字母时切换
      }
    },
    "shift": { "mode": "autoOff" },      // 混合模式下 shift 可用
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

##### 4.1.12.2 混合输入流程

```
用户在中文拼音模式下输入
    ↓
┌─────────────────────────────────────────────────────────┐
│  输入 "hello"                                            │
│  ├─ h → 组合缓冲区: "h" → 拼音候选                      │
│  ├─ e → 组合缓冲区: "he" → 拼音候选                     │
│  ├─ l → 组合缓冲区: "hel" → 拼音候选                    │
│  ├─ l → 组合缓冲区: "hell" → 无拼音候选                  │
│  └─ o → 组合缓冲区: "hello" → 无拼音候选                 │
│                                                         │
│  检测到连续 5 个英文字母且无拼音候选                       │
│  ├─ 切换到英文直接输入模式                                │
│  ├─ 提交 "hello"                                        │
│  └─ 后续输入作为英文                                     │
└─────────────────────────────────────────────────────────┘
```


#### 4.1.13 任意语言链式切换

##### 4.1.13.1 切换矩阵：从 Pair 到状态机

**问题**：中→英→韩→日 这种多轮切换，pair 对无法覆盖「上一个语言是什么」的上下文。

**解决方案**：用状态机替代 pair 对，切换行为基于「来源语言类型」而非具体语言 ID。

```kotlin
// 语言类型分类（用于切换策略匹配）
enum class LanguageType {
    DIRECT_LTR,     // 直接输入 LTR：英文、法语、德语、西班牙语、俄语等
    DIRECT_RTL,     // 直接输入 RTL：阿拉伯语、希伯来语
    COMPOSITION,    // 组合输入：中文拼音/五笔/双拼、日文罗马字
    COMPLEX,        // 复杂文字：韩文谚文、藏文、天城文
    SYMBOL          // 符号输入
}

// 切换规则：基于「从什么类型」切换到「什么类型」
data class SwitchRule(
    val fromType: LanguageType,
    val toType: LanguageType,
    val clearComposing: Boolean = true,
    val shiftBehavior: ShiftSwitchBehavior = ShiftSwitchBehavior.RESET_TO_OFF,
    val resetArrangement: Boolean = true,
    val switchLayoutDirection: Boolean = false,
    val preservePerLanguageState: Boolean = true
)
```

##### 4.1.13.2 切换规则表

```jsonc
{
  "switchRules": [
    // 组合输入 → 直接输入（中→英、日→英、韩→英）
    { "from": "COMPOSITION", "to": "DIRECT_LTR", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },
    { "from": "COMPOSITION", "to": "DIRECT_RTL", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true, "switchLayoutDirection": true },

    // 直接输入 → 组合输入（英→中、英→日）
    { "from": "DIRECT_LTR", "to": "COMPOSITION", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },

    // 复杂文字 → 直接输入（韩→英）
    { "from": "COMPLEX", "to": "DIRECT_LTR", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },
    { "from": "COMPLEX", "to": "DIRECT_RTL", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true, "switchLayoutDirection": true },

    // 直接输入 → 复杂文字（英→韩）
    { "from": "DIRECT_LTR", "to": "COMPLEX", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },

    // 组合输入之间（中→日、中→韩、日→韩）
    { "from": "COMPOSITION", "to": "COMPLEX", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },
    { "from": "COMPLEX", "to": "COMPOSITION", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true },
    { "from": "COMPOSITION", "to": "COMPOSITION", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": false },
    { "from": "COMPLEX", "to": "COMPLEX", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": false },

    // RTL → RTL（阿拉伯→希伯来）
    { "from": "DIRECT_RTL", "to": "DIRECT_RTL", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": false, "switchLayoutDirection": false },

    // RTL → LTR（阿拉伯→英文）
    { "from": "DIRECT_RTL", "to": "DIRECT_LTR", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true, "switchLayoutDirection": true },

    // 默认：任何未覆盖的切换
    { "from": "*", "to": "*", "clearComposing": true, "shiftBehavior": "resetToOff", "resetArrangement": true }
  ]
}
```

##### 4.1.13.3 多轮切换状态追踪

```kotlin
class LanguageSwitchManager(
    private val rules: List<SwitchRule>,
    private val languageRegistry: LanguageRegistry
) {
    // 每个语言的独立状态
    private val languageStates = mutableMapOf<String, LanguageState>()

    // 切换历史（用于 undo）
    private val history = ArrayDeque<String>(10)

    fun switch(from: String, to: String): SwitchAction {
        val fromLang = languageRegistry.get(from) ?: return SwitchAction.Noop
        val toLang = languageRegistry.get(to) ?: return SwitchAction.Noop

        // 匹配规则
        val rule = rules.firstOrNull {
            (it.fromType == fromLang.type || it.fromType.name == "*") &&
            (it.toType == toLang.type || it.toType.name == "*")
        } ?: rules.last() // 使用默认规则

        // 保存当前语言状态
        saveState(from)

        // 清空组合
        val clearComposing = rule.clearComposing

        // 恢复目标语言状态
        val restoredState = if (rule.preservePerLanguageState) {
            languageStates[to]
        } else null

        // 记录历史
        history.addLast(from)

        return SwitchAction(
            targetLanguage = to,
            clearComposing = clearComposing,
            shiftState = restoredState?.shift ?: ShiftState.OFF,
            arrangement = restoredState?.arrangement ?: toLang.defaultArrangement,
            switchLayoutDirection = rule.switchLayoutDirection,
            fromDirection = fromLang.direction,
            toDirection = toLang.direction
        )
    }

    fun undo(): SwitchAction? {
        val previous = history.removeLastOrNull() ?: return null
        val current = history.lastOrNull() ?: return null
        return switch(current, previous)
    }

    private fun saveState(langId: String) {
        languageStates[langId] = LanguageState(/* ... */)
    }
}

data class LanguageState(
    val shift: ShiftState,
    val arrangement: String,
    val capsLock: Boolean
)

data class SwitchAction(
    val targetLanguage: String,
    val clearComposing: Boolean,
    val shiftState: ShiftState,
    val arrangement: String,
    val switchLayoutDirection: Boolean,
    val fromDirection: TextDirection,
    val toDirection: TextDirection
)
```

##### 4.1.13.4 多轮切换示例

```
用户操作序列：
1. 中文拼音 输入「你好」→ 英文 输入「hello」→ 韩文 输入「안녕」

状态追踪：
┌─────────────────────────────────────────────────────────────┐
│  Step 1: 中文拼音                                          │
│  ├─ language: zh_cn, type: COMPOSITION                      │
│  ├─ shift: disabled                                         │
│  ├─ arrangement: pinyin                                     │
│  └─ composing: "nihao" → 选择「你好」                       │
├─────────────────────────────────────────────────────────────┤
│  Step 2: 切换到英文                                         │
│  ├─ 规则匹配: COMPOSITION → DIRECT_LTR                      │
│  ├─ clearComposing: true (清空)                             │
│  ├─ shiftBehavior: resetToOff                               │
│  ├─ resetArrangement: true → 切换到 QWERTY                  │
│  ├─ 保存中文状态: {shift: disabled, arrangement: pinyin}    │
│  └─ 加载英文状态: {shift: OFF, arrangement: alpha}          │
├─────────────────────────────────────────────────────────────┤
│  Step 3: 英文输入 "hello"                                   │
│  ├─ shift: OFF → 输入小写                                   │
│  └─ autoCapitalize: 句首大写 → "Hello"                      │
├─────────────────────────────────────────────────────────────┤
│  Step 4: 切换到韩文                                         │
│  ├─ 规则匹配: DIRECT_LTR → COMPLEX                          │
│  ├─ clearComposing: true                                    │
│  ├─ resetArrangement: true → 切换到 hangul                  │
│  ├─ 保存英文状态: {shift: OFF, arrangement: alpha}          │
│  └─ 加载韩文状态: {shift: disabled, arrangement: hangul}    │
├─────────────────────────────────────────────────────────────┤
│  Step 5: 韩文输入 "안녕"                                    │
│  ├─ Jamo 组合: ㅇ+ㅏ+ㄴ = 안, ㄴ+ㅕ+ㅇ = 녕                 │
│  └─ 提交 "안녕"                                             │
└─────────────────────────────────────────────────────────────┘

历史栈: [zh_cn, en_us, ko_kr]
当前: ko_kr
```

##### 4.1.13.5 RTL 布局方向切换

```kotlin
// RTL 语言切换时需要同时处理布局方向
data class LayoutDirectionSwitch(
    val fromDirection: TextDirection,  // LTR or RTL
    val toDirection: TextDirection,
    val preserveVisualOrder: Boolean = false
)

// RTL 语言配置
data class RTLConfig(
    val isRTL: Boolean,
    val mirrorShiftIcon: Boolean = true,
    val mirrorBackspaceIcon: Boolean = true,
    val mirrorEnterIcon: Boolean = false,
    val bidirectionalSupport: Boolean = true
)
```

##### 4.1.13.6 混合输入通用配置

```jsonc
{
  "mixedInput": {
    "supportedCombinations": [
      { "primary": "zh_pinyin", "secondary": "en_us", "mode": "autoDetect" },
      { "primary": "ja_romaji", "secondary": "en_us", "mode": "autoDetect" },
      { "primary": "ko_hangul", "secondary": "en_us", "mode": "autoDetect" },
      { "primary": "zh_pinyin", "secondary": "ja_romaji", "mode": "manualSwitch" },
      { "primary": "hi_in", "secondary": "en_us", "mode": "autoDetect" },
      { "primary": "th_th", "secondary": "en_us", "mode": "manualSwitch" }
    ],
    "autoDetect": {
      "latinThreshold": 3,
      "nonLatinThreshold": 1,
      "punctuationReset": true
    }
  }
}
```

---

#### 4.1.14 按应用记忆语言

```jsonc
{
  "perAppLanguage": {
    "enabled": true,

    // 应用专属配置
    "appOverrides": {
      "com.whatsapp": {
        "preferredLanguage": "en_us",
        "rememberLastUsed": true
      },
      "com.tencent.mm": {           // 微信
        "preferredLanguage": "zh_cn",
        "inputMethod": "zh_pinyin",
        "rememberLastUsed": true
      },
      "com.twitter.android": {
        "preferredLanguage": "en_us",
        "rememberLastUsed": true
      },
      "jp.naver.line.android": {    // LINE
        "preferredLanguage": "ja_jp",
        "rememberLastUsed": true
      },
      "com.kakao.talk": {           // KakaoTalk
        "preferredLanguage": "ko_kr",
        "rememberLastUsed": true
      }
    },

    // 默认行为
    "defaultBehavior": {
      "useLastUsedLanguage": true,    // 记住上次使用的语言
      "fallbackToSystem": true        // 无记录时使用系统语言
    }
  }
}
```

---

#### 4.1.15 滑动输入（Swipe Typing）

```jsonc
{
  "swipeInput": {
    "enabled": true,
    "sensitivity": 0.8,              // 灵敏度 0.0-1.0
    "minPathLength": 3,              // 最小路径长度（按键数）
    "showTrail": true,               // 显示滑动轨迹
    "trailColor": "#801A73E8",       // 轨迹颜色（半透明）
    "trailWidth": 8,                 // 轨迹宽度 dp
    "trailDuration": 300,            // 轨迹消失时间 ms

    // 语言支持
    "supportedLanguages": ["en_us", "zh_pinyin", "fr_fr", "de_de", "es_es"],

    // 滑动词典（独立于点击词典）
    "dictionary": {
      "format": "trie",
      "file": "dictionaries/swipe_en.dict"
    }
  }
}
```

##### 4.1.15.1 滑动轨迹渲染

```kotlin
class SwipeTrailRenderer(
    private val canvas: Canvas,
    private val trailColor: Int,
    private val trailWidth: Float
) {
    private val points = mutableListOf<PointF>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    fun addPoint(point: PointF) {
        points.add(point)
    }

    fun clear() {
        points.clear()
    }

    fun draw() {
        if (points.size < 2) return
        paint.color = trailColor
        paint.strokeWidth = trailWidth

        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            path.lineTo(points[i].x, points[i].y)
        }
        canvas.drawPath(path, paint)
    }
}
```

---

#### 4.1.16 自动纠错

```jsonc
{
  "autoCorrection": {
    "enabled": true,
    "level": "moderate",             // off | moderate | aggressive

    // 纠错规则
    "rules": [
      {
        "type": "typo",
        "pattern": "teh",
        "correction": "the",
        "confidence": 0.95
      },
      {
        "type": "typo",
        "pattern": "recieve",
        "correction": "receive",
        "confidence": 0.99
      }
    ],

    // 语言专属纠错
    "perLanguage": {
      "en_us": {
        "enabled": true,
        "level": "moderate"
      },
      "zh_pinyin": {
        "enabled": false               // 拼音不做自动纠错
      },
      "ja_romaji": {
        "enabled": false
      }
    }
  }
}
```

---

#### 4.1.17 键盘行为配置

```jsonc
{
  "keyboardBehavior": {
    // 双击空格输入句号
    "doubleSpacePeriod": {
      "enabled": true,
      "timeout": 300,                 // 双击间隔 ms
      "output": ". ",
      "supportedLanguages": ["en_us", "fr_fr", "de_de", "es_es"],
      "disabledFor": ["zh_cn", "ja_jp", "ko_kr"]  // 组合输入语言禁用
    },

    // 自动大写
    "autoCapitalize": {
      "enabled": true,
      "rules": {
        "sentenceStart": true,        // 句首大写
        "afterNewline": true,         // 换行后大写
        "afterColon": true,           // 冒号后大写
        "i_alone": false              // 单独 "i" 不自动大写（英文）
      },
      "perLanguage": {
        "en_us": { "enabled": true },
        "zh_cn": { "enabled": false },
        "ja_jp": { "enabled": false },
        "ko_kr": { "enabled": false }
      }
    },

    // 退格长按连删
    "backspaceRepeat": {
      "enabled": true,
      "initialDelay": 400,            // 首次延迟 ms
      "repeatInterval": 50,           // 重复间隔 ms
      "accelerate": true,             // 加速
      "maxSpeed": 30                  // 最小间隔 ms
    },

    // 按键反馈
    "keyFeedback": {
      "haptic": {
        "enabled": true,
        "type": "virtualKey",         // virtualKey | keyboard | clock
        "intensity": 0.5              // 0.0-1.0
      },
      "sound": {
        "enabled": false,
        "type": "keyboard",           // keyboard | typewriter | click
        "volume": 0.3                 // 0.0-1.0
      },
      "keyPopupPreview": {
        "enabled": true,
        "showOnTop": true,            // 在按键上方显示预览
        "fontSize": 24                // 预览字体大小 sp
      }
    },

    // 空格滑动控制光标
    "spaceCursorControl": {
      "enabled": true,
      "sensitivity": 1.0,             // 灵敏度
      "showCursorIndicator": true     // 显示光标指示器
    }
  }
}
```

---

#### 4.1.18 键盘布局模式

```jsonc
{
  "layoutModes": {
    // 单手模式
    "oneHanded": {
      "enabled": true,
      "modes": {
        "left": {
          "anchor": "left",
          "widthRatio": 0.85,         // 占屏幕宽度比例
          "offsetDp": 8               // 左边距
        },
        "right": {
          "anchor": "right",
          "widthRatio": 0.85,
          "offsetDp": 8
        },
        "center": {
          "anchor": "center",
          "widthRatio": 1.0
        }
      },
      "switchButton": true            // 显示切换按钮
    },

    // 平板模式
    "tablet": {
      "enabled": true,
      "thresholdWidthDp": 600,        // 触发宽度
      "splitKeyboard": true,          // 分屏键盘
      "splitGapDp": 40                // 分屏间距
    },

    // 横屏模式
    "landscape": {
      "enabled": true,
      "heightReduction": 0.7,         // 高度缩减比例
      "compactLayout": true           // 使用紧凑布局
    },

    // 数字行
    "numberRow": {
      "enabled": false,               // 默认关闭
      "toggleable": true              // 可在工具栏切换
    }
  }
}
```

---

#### 4.1.19 深色模式与主题切换

```jsonc
{
  "themeSwitching": {
    "mode": "auto",                   // auto | manual | battery

    // 自动跟随系统
    "auto": {
      "followSystem": true,
      "transitionDuration": 300       // 切换过渡时间 ms
    },

    // 电量模式
    "battery": {
      "enableAtBatteryBelow": 20,     // 电量低于 20% 时切换到深色
      "restoreAtBatteryAbove": 50     // 电量恢复到 50% 时切回
    },

    // 按时间切换
    "scheduled": {
      "enabled": false,
      "darkStart": "22:00",
      "darkEnd": "07:00"
    }
  }
}
```

---

#### 4.1.20 键盘高度与字体调整

```jsonc
{
  "sizing": {
    // 键盘高度
    "height": {
      "default": 260,                 // 默认高度 dp
      "min": 180,
      "max": 400,
      "userAdjustable": true,         // 用户可调整
      "rememberPerApp": false         // 按应用记忆高度
    },

    // 按键字体大小
    "keyFontSize": {
      "default": 18,                  // sp
      "min": 12,
      "max": 28,
      "userAdjustable": true
    },

    // 候选词字体大小
    "candidateFontSize": {
      "default": 16,
      "min": 12,
      "max": 24
    },

    // 按键尺寸
    "keySize": {
      "heightDp": 46,
      "minHeightDp": 36,
      "maxHeightDp": 56
    }
  }
}
```

---

### 4.2 布局系统

#### 4.2.1 JSON 布局 Schema

```jsonc
{
  "schemaVersion": 1,
  "id": "zh_pinyin_full",
  "meta": {
    "name": "中文 (拼音)",
    "locale": "zh-CN",
    "tags": ["cjk", "composition"]
  },

  "script": {
    "type": "LOGOGRAPHIC",
    "direction": "LTR",
    "composition": {
      "engine": "PINYIN",
      "maxInputLength": 30
    }
  },

  "templates": {
    "char": {
      "role": "character",
      "gestures": {
        "tap": [{ "act": "pushToComposition" }],
        "longPress": [{ "act": "showPopup", "from": "key.popup" }]
      }
    },
    "action": {
      "role": "action",
      "gestures": { "tap": [{ "act": "key.action" }] }
    },
    "repeatable": {
      "extends": "action",
      "repeat": { "delay": 400, "interval": 50 }
    }
  },

  "keys": {
    "q": { "t": "char", "code": 113, "label": "q" },
    "w": { "t": "char", "code": 119, "label": "w" },
    "space": {
      "t": "action",
      "role": "space",
      "action": "commitCompositionOrSpace",
      "dynamicLabel": {
        "composing": "${compositionDisplay}",
        "idle": " "
      }
    }
  },

  "grid": {
    "columns": 10,                    // 基准列数
    "rowHeight": "1fr"                // 行高模式：1fr = 等分
  },

  "rows": [
    { "id": "r1", "keys": ["q","w","e","r","t","y","u","i","o","p"] },
    { "id": "r2", "keys": ["_pad","a","s","d","f","g","h","j","k","l","_pad"], "pad": 0.5 },
    { "id": "r3", "keys": ["shift","z","x","c","v","b","n","m","del"] },
    {
      "id": "r4",
      "keys": [
        { "id": "?123", "colSpan": 1 },
        { "id": ",", "colSpan": 1 },
        { "id": "space", "colSpan": 4 },
        { "id": ".", "colSpan": 1 },
        { "id": "enter", "colSpan": 2 }
      ]
    }
  ],

  // 跨行按键示例（如 Shift 占 2 行）
  "keys": {
    "shift": { "t": "action", "role": "modifier", "icon": "shift", "rowSpan": 2, "colSpan": 1 },
    "space": { "t": "action", "role": "space", "colSpan": 4 },
    "enter": { "t": "action", "role": "enter", "colSpan": 2 }
  },

  "arrangements": {
    "pinyin": { "rows": ["r1","r2","r3","r4"] },
    "symbols": { "rows": ["sym1","sym2","sym3","r4_sym"] }
  },

  "geometry": {
    "height": { "dp": 260, "min": 190, "max": 360 },
    "gap": { "h": 4, "v": 5 },
    "padding": { "all": 6 }
  }
}
```

#### 4.2.2 自定义布局 Patch

```jsonc
{
  "target": "zh_pinyin_full",
  "ops": [
    { "op": "replaceKey", "key": "q", "data": { "label": "Q", "popup": ["á","à","â"] } },
    { "op": "insertKeyAfter", "after": "m", "data": { "id": "emoji_key", "t": "action", "icon": "emoji" } },
    { "op": "removeKey", "key": "?" },
    { "op": "replaceRow", "row": "r4", "keys": ["?123","mic",",","space",".","enter"] }
  ]
}
```

#### 4.2.3 跨行跨列（Span）设计

##### 4.2.3.1 Span 配置

每个按键支持 `colSpan`（跨列）和 `rowSpan`（跨行）：

```jsonc
{
  "keys": {
    "space": {
      "t": "action",
      "role": "space",
      "colSpan": 4,                   // 占 4 列宽度
      "rowSpan": 1                    // 默认 1 行
    },
    "shift": {
      "t": "action",
      "role": "modifier",
      "icon": "shift",
      "colSpan": 1,
      "rowSpan": 2                    // 跨 2 行（如左边 Shift 占两行高度）
    },
    "backspace": {
      "t": "repeatable",
      "role": "delete",
      "colSpan": 2,                   // 占 2 列
      "rowSpan": 1
    },
    "enter": {
      "t": "action",
      "role": "enter",
      "colSpan": 2,
      "rowSpan": 1
    },
    "langSwitch": {
      "t": "action",
      "role": "languageSwitch",
      "icon": "language",
      "colSpan": 1,
      "rowSpan": 2                    // 右侧 Shift 位置跨 2 行
    }
  }
}
```

##### 4.2.3.2 Span 几何计算

```kotlin
data class KeyGeometry(
    val keyId: String,
    val col: Int,                      // 起始列
    val row: Int,                      // 起始行
    val colSpan: Int = 1,              // 跨列数
    val rowSpan: Int = 1,              // 跨行数
    val widthPx: Float,                // 计算后的像素宽度
    val heightPx: Float,               // 计算后的像素高度
    val leftPx: Float,                 // 左边界
    val topPx: Float                   // 上边界
)

class GridCalculator(
    private val gridConfig: GridConfig
) {
    fun calculate(
        rows: List<RowData>,
        keys: Map<String, KeyData>,
        containerWidth: Float,
        containerHeight: Float,
        hGap: Float,
        vGap: Float
    ): Map<String, KeyGeometry> {
        val spanCount = gridConfig.columns
        val cellWidth = (containerWidth - hGap * (spanCount + 1)) / spanCount
        val rowHeight = containerHeight / rows.size

        val result = mutableMapOf<String, KeyGeometry>()
        // 跟踪每列的占用情况（用于 rowSpan）
        val occupied = Array(rows.size) { BooleanArray(spanCount) }

        for ((rowIndex, row) in rows.withIndex()) {
            var col = 0
            for (keyRef in row.keys) {
                val key = keys[keyRef] ?: continue
                val cs = key.colSpan ?: 1
                val rs = key.rowSpan ?: 1

                // 找到下一个未占用的列
                while (col < spanCount && occupied[rowIndex][col]) col++
                if (col >= spanCount) break

                // 计算几何
                val leftPx = hGap + col * (cellWidth + hGap)
                val topPx = vGap + rowIndex * (rowHeight + vGap)
                val widthPx = cs * cellWidth + (cs - 1) * hGap
                val heightPx = rs * rowHeight + (rs - 1) * vGap

                result[keyRef] = KeyGeometry(
                    keyId = keyRef, col = col, row = rowIndex,
                    colSpan = cs, rowSpan = rs,
                    widthPx = widthPx, heightPx = heightPx,
                    leftPx = leftPx, topPx = topPx
                )

                // 标记占用
                for (r in 0 until rs) {
                    for (c in 0 until cs) {
                        if (rowIndex + r < rows.size && col + c < spanCount) {
                            occupied[rowIndex + r][col + c] = true
                        }
                    }
                }

                col += cs
            }
        }

        return result
    }
}
```

##### 4.2.3.3 常见 Span 布局示例

**标准 QWERTY（10列基准）**

```
Row 0: [q][w][e][r][t][y][u][i][o][p]        各 1 列
Row 1: [  ][a][s][d][f][g][h][j][k][l][  ]   spacer 各 0.5 列
Row 2: [⇧ ][z][x][c][v][b][n][m][⌫  ]       ⇧ 跨行, ⌫ 2列
Row 3: [符][,][  空格  ][.][ ↵ ]             空格 4列, ↵ 2列
```

**紧凑布局（8列）**

```
Row 0: [q][w][e][r][t][y][u][i][o][p]
Row 1:  [a][s][d][f][g][h][j][k][l]
Row 2: [⇧][z][x][c][v][b][n][m][⌫]
Row 3: [12][,][   空格   ][.][↵]
```

**大空格单手布局**

```
Row 0: [q][w][e][r][t][y][u][i][o][p]
Row 1:  [a][s][d][f][g][h][j][k][l]
Row 2: [⇧][z][x][c][v][b][n][m][⌫]
Row 3: [123][   空格   ][,][.][↵]
```

---

### 4.3 词典系统

#### 4.3.1 词典架构

```
┌─────────────────────────────────────────────────────────┐
│                  SuggestionEngine                        │
├─────────────────────────────────────────────────────────┤
│  ┌───────────┐ ┌───────────┐ ┌───────────┐            │
│  │ TrieDict  │ │ UserDict  │ │ FreqDict  │            │
│  │ (系统词典) │ │ (用户词典) │ │ (词频词典) │            │
│  └───────────┘ └───────────┘ └───────────┘            │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐            │
│  │ PinyinDict│ │ Japanese  │ │ Korean    │            │
│  │ (拼音词典) │ │  Dict     │ │  Dict     │            │
│  └───────────┘ └───────────┘ └───────────┘            │
│  ┌───────────────────────────────────────┐            │
│  │           LLM Suggestion              │            │
│  │           (AI 联想)                    │            │
│  └───────────────────────────────────────┘            │
└─────────────────────────────────────────────────────────┘
```

#### 4.3.2 词典文件格式

```jsonc
// Trie 格式
{
  "format": "trie",
  "version": 1,
  "language": "en",
  "root": {
    "a": {
      "_": { "f": 1000 },
      "n": { "d": { "_": { "f": 700 } } }
    }
  }
}

// 混合词典格式（中英混合）
{
  "format": "hybrid",
  "version": 1,
  "entries": [
    { "w": "你好", "f": 5000, "py": "ni hao" },
    { "w": "hello", "f": 8000 },
    { "w": "你好世界", "f": 2000, "py": "ni hao shi jie", "en": "hello world" }
  ],
  "bigrams": [
    { "prev": "你好", "next": "世界", "f": 3000 },
    { "prev": "hello", "next": "world", "f": 5000 }
  ]
}
```

#### 4.3.3 联想流程

```
用户输入 "l"
    ↓
┌─────────────────────────────────────────┐
│           SuggestionEngine             │
├─────────────────────────────────────────┤
│  1. Prefix Search (前缀匹配)            │
│  2. Frequency Sort (词频排序)            │
│  3. Context Prediction (上下文预测)      │
│  4. LLM Suggestion (LLM 联想)          │
│  5. Deduplicate & Rank (去重排序)       │
└─────────────────────────────────────────┘
    ↓
候选栏: [language] [large] [last] [late] [lake] ...
```

---

### 4.4 主题系统

#### 4.4.1 主题 JSON

```jsonc
{
  "id": "dracula",
  "name": "Dracula",
  "version": 1,
  "type": "static",                    // static | animated | image

  "colors": {
    "background": "#282A36",
    "surface": "#44475A",
    "key": {
      "normal": "#6272A4",
      "pressed": "#BD93F9",
      "text": "#F8F8F2",
      "hint": "#6272A4"
    },
    "candidate": {
      "background": "#282A36",
      "text": "#F8F8F2",
      "highlight": "#BD93F9"
    }
  },

  "geometry": {
    "key": {
      "cornerRadius": 8,
      "heightDp": 46,
      "gapHDp": 4,
      "gapVDp": 5
    }
  },

  "animations": {
    "keyPress": {
      "type": "scale",
      "from": 1.0,
      "to": 0.95,
      "duration": 100
    }
  }
}
```

#### 4.4.2 图片/GIF 主题

```jsonc
{
  "id": "custom_image",
  "name": "My Theme",
  "type": "image",

  "images": {
    "keyboardBackground": "themes/my_bg.png",
    "keyNormal": "themes/key_normal.9.png",
    "keyPressed": "themes/key_pressed.9.png",
    "toolbarBackground": "themes/toolbar_bg.gif"
  },

  "colors": {
    "key": {
      "text": "#FFFFFF",
      "hint": "#AAAAAA"
    }
  }
}
```

---

### 4.5 LLM 集成

#### 4.5.1 LLM 架构

```
┌─────────────────────────────────────────────────────────┐
│                    LLMBridge                            │
├─────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────┐ │
│  │              Provider Manager                     │ │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐            │ │
│  │  │  Local  │ │  Cloud  │ │  Mock   │            │ │
│  │  │ (ONNX)  │ │ (API)   │ │ (test)  │            │ │
│  │  └─────────┘ └─────────┘ └─────────┘            │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │              Pipeline                             │ │
│  │  Context Builder → Prompt → Stream → Parse        │ │
│  └───────────────────────────────────────────────────┘ │
├─────────────────────────────────────────────────────────┤
│  Features:                                              │
│  ├── Autocomplete (自动补全)                            │
│  ├── Translation (翻译)                                │
│  ├── Text Enhancement (语句美化)                        │
│  └── Smart Suggestion (智能联想)                        │
└─────────────────────────────────────────────────────────┘
```

#### 4.5.2 LLM 功能

| 功能 | 触发方式 | Prompt 模板 |
|------|---------|-------------|
| **自动补全** | 长按空格 | "Complete: ${context}" |
| **中英翻译** | 工具栏按钮 | "Translate to ${target}: ${text}" |
| **语句美化** | 工具栏按钮 | "Enhance: ${text}" |
| **智能联想** | 候选栏 | "Suggest for: ${prefix}" |

---

### 4.6 STT 集成

#### 4.6.1 架构

```
┌─────────────────────────────────────────────────────────┐
│                    STTBridge                            │
├─────────────────────────────────────────────────────────┤
│  ┌───────────────────────────────────────────────────┐ │
│  │              Provider Manager                     │ │
│  │  ┌─────────────┐  ┌─────────────┐               │ │
│  │  │   Android   │  │   端侧模型   │               │ │
│  │  │  SpeechRec  │  │  (待定)      │               │ │
│  │  └─────────────┘  └─────────────┘               │ │
│  └───────────────────────────────────────────────────┘ │
│  ┌───────────────────────────────────────────────────┐ │
│  │              Pipeline                             │ │
│  │  Recorder → VAD → Provider → Formatter            │ │
│  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────┘
```

> **注意**：端侧 STT 模型待用户调研后确定，当前仅实现 Android 系统 API 接口。

#### 4.6.2 Provider 接口

```kotlin
interface STTProvider {
    val id: String
    val name: String

    fun startListening(config: STTConfig): Flow<STTEvent>
    fun stopListening()
    fun cancel()
    fun isAvailable(): Boolean
    fun getSupportedLanguages(): List<String>
}

data class STTConfig(
    val language: String,
    val enablePunctuation: Boolean = true,
    val maxDuration: Duration = 30.seconds,
    val model: String? = null          // 端侧模型名称（待定）
)

sealed interface STTEvent {
    data object Listening : STTEvent
    data class PartialResult(val text: String, val confidence: Float) : STTEvent
    data class FinalResult(val text: String, val segments: List<Segment>) : STTEvent
    data class Error(val cause: Throwable) : STTEvent
}
```

#### 4.6.3 Android 系统 STT

```kotlin
class AndroidSTTProvider(
    private val context: Context
) : STTProvider {
    override val id = "android"
    override val name = "Android SpeechRecognizer"

    private var recognizer: SpeechRecognizer? = null

    override fun startListening(config: STTConfig): Flow<STTEvent> = callbackFlow {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener { /* ... */ })
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
        awaitClose { recognizer?.cancel() }
    }

    override fun stopListening() { recognizer?.stopListening() }
    override fun cancel() { recognizer?.cancel() }
    override fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)
    override fun getSupportedLanguages() = listOf("en", "zh", "ja", "ko", "fr", "de", "es", "ar")
}
```

#### 4.6.4 端侧 STT（待接入）

```kotlin
/**
 * 端侧 STT Provider 接口。
 * 待用户调研后接入具体模型（如 Whisper.cpp、Vosk 等）。
 * 当前保留接口定义，实现返回 isAvailable = false。
 */
class OnDeviceSTTProvider : STTProvider {
    override val id = "on_device"
    override val name = "On-Device STT"

    override fun startListening(config: STTConfig): Flow<STTEvent> = flow {
        emit(STTEvent.Error(UnsupportedOperationException("On-device STT not yet implemented")))
    }

    override fun stopListening() {}
    override fun cancel() {}
    override fun isAvailable() = false   // 待接入后改为 true
    override fun getSupportedLanguages() = emptyList()
}
```

---

### 4.7 符号页系统

#### 4.7.1 符号页架构

符号页有两种模式：

| 模式 | 说明 | 场景 |
|------|------|------|
| **嵌入模式** | 作为 arrangement 切换，键盘区域内 | 轻量级符号输入 |
| **展开模式** | 全屏覆盖，大量符号浏览 | 完整符号库 |

#### 4.7.2 符号分类

```kotlin
data class SymbolCategory(
    val id: String,
    val name: String,
    val icon: String,              // 分类图标
    val symbols: List<SymbolItem>,
    val isDefault: Boolean = false
)

data class SymbolItem(
    val char: String,              // 符号字符
    val label: String? = null,     // 显示标签
    val popup: List<String> = null // 长按弹出变体
)

// 内置分类
enum class BuiltinSymbolCategory(
    val id: String,
    val name: String,
    val icon: String
) {
    COMMON("common", "常用", "ic_symbol_common"),
    PUNCTUATION("punct", "标点", "ic_symbol_punct"),
    MATH("math", "数学", "ic_symbol_math"),
    CURRENCY("currency", "货币", "ic_symbol_currency"),
    ARROW("arrow", "箭头", "ic_symbol_arrow"),
    NUMBER("number", "数字", "ic_symbol_number"),
    GREEK("greek", "希腊", "ic_symbol_greek"),
    UNIT("unit", "单位", "ic_symbol_unit"),
    BRACKET("bracket", "括号", "ic_symbol_bracket")
}
```

#### 4.7.3 符号页 JSON 配置

```jsonc
{
  "symbolPage": {
    "mode": "embedded",            // embedded | expanded | both
    "categories": [
      { "id": "common", "name": "常用", "icon": "ic_symbol_common" },
      { "id": "punct", "name": "标点", "icon": "ic_symbol_punct" },
      { "id": "math", "name": "数学", "icon": "ic_symbol_math" }
    ],
    "layout": {
      "gridColumns": 8,
      "cellHeightDp": 52,
      "cellCornerRadius": 8
    },
    "recentSymbols": {
      "enabled": true,
      "maxRecent": 30
    }
  }
}
```

#### 4.7.4 符号页 UI

```
┌─────────────────────────────────────────────┐
│  符号页 (展开模式)                           │
├─────────────────────────────────────────────┤
│  [返回]  符号分类: 常用 | 标点 | 数学 | ...  │
├─────────────────────────────────────────────┤
│  ┌───┬───┬───┬───┬───┬───┬───┬───┐        │
│  │ ，│ 。│ ？│ ！│ 、│ ；│ ：│ ＠│        │
│  ├───┼───┼───┼───┼───┼───┼───┼───┤        │
│  │ ＃│ ＄│ ％│ ＆│ ＊│ ＋│ ＝│ ／│        │
│  ├───┼───┼───┼───┼───┼───┼───┼───┤        │
│  │ ＜│ ＞│ （│ ）│ 【│ 】│ 《│ 》│        │
│  ├───┼───┼───┼───┼───┼───┼───┼───┤        │
│  │ ～│ ￥│ …│ —│ 「│ 」│ 『│ 』│        │
│  └───┴───┴───┴───┴───┴───┴───┴───┘        │
├─────────────────────────────────────────────┤
│  [上一页]                    [下一页] [锁定] │
└─────────────────────────────────────────────┘
```

---

### 4.8 颜文字 (Kaomoji)

#### 4.8.1 颜文字数据模型

```kotlin
data class KaomojiEntry(
    val text: String,              // "(╯°□°)╯︵ ┻━┻"
    val category: String,          // 分类 ID
    val tags: List<String> = emptyList(),  // 标签，用于搜索
    val frequency: Long = 0        // 使用频率
)

data class KaomojiCategory(
    val id: String,
    val name: String,
    val icon: String,
    val kaomojis: List<KaomojiEntry>
)
```

#### 4.8.2 颜文字分类

| 分类 | ID | 示例 |
|------|-----|------|
| **高兴** | happy | `(＾▽＾)`, `(*≧ω≦*)` |
| **难过** | sad | `(╥﹏╥)`, `(T_T)` |
| **生气** | angry | `(╬▔皿▔)╯`, `(ノ≥∀≤)ノ` |
| **惊讶** | surprise | `(⊙_⊙)`, `(°Д°)` |
| **爱意** | love | `(♥ω♥*)`, `(´▽`ʃ♡ƪ)` |
| **搞笑** | funny | `(╯°□°)╯︵ ┻━┻`, `¯\_(ツ)_/¯` |
| **动物** | animal | `(=^・ω・^=)`, `(ʕ•ᴥ•ʔ)` |
| **食物** | food | `(っ˘ڡ˘ς)`, `(づ｡◕‿‿◕｡)づ` |

#### 4.8.3 颜文字页面 UI

```
┌─────────────────────────────────────────────┐
│  颜文字                                     │
├─────────────────────────────────────────────┤
│  [搜索: _____________________________]      │
├─────────────────────────────────────────────┤
│  分类: 高兴 | 难过 | 生气 | 惊讶 | 爱意     │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐   │
│  │ (＾▽＾)        (*≧ω≦*)            │   │
│  ├─────────────────────────────────────┤   │
│  │ (◕‿◕✿)        (≧▽≦)              │   │
│  ├─────────────────────────────────────┤   │
│  │ (´∀`ʃ♡ƪ)     (♥ω♥*)              │   │
│  └─────────────────────────────────────┘   │
├─────────────────────────────────────────────┤
│  [最近使用]                                 │
│  (╯°□°)╯︵ ┻━┻  ¯\_(ツ)_/¯  (｡◕‿◕｡)    │
└─────────────────────────────────────────────┘
```

---

### 4.9 表情 Emoji

#### 4.9.1 Emoji 数据模型

```kotlin
data class EmojiEntry(
    val emoji: String,             // "😀"
    val name: String,              // "grinning face"
    val category: String,          // 分类 ID
    val skinTones: List<String>? = null,  // 支持肤色变体
    val keywords: List<String> = emptyList()  // 搜索关键词
)

data class EmojiCategory(
    val id: String,
    val name: String,
    val icon: String,
    val emojis: List<EmojiEntry>
)
```

#### 4.9.2 Emoji 分类 (Unicode 标准)

| 分类 | ID | 图标 | 数量 |
|------|-----|------|------|
| **笑脸** | smileys | 😀 | 180+ |
| **人物** | people | 👋 | 250+ |
| **动物** | animals | 🐶 | 150+ |
| **食物** | food | 🍎 | 120+ |
| **旅行** | travel | ✈️ | 100+ |
| **活动** | activities | ⚽ | 80+ |
| **物体** | objects | 💡 | 200+ |
| **符号** | symbols | ❤️ | 100+ |
| **旗帜** | flags | 🏁 | 250+ |

#### 4.9.3 Emoji 页面 UI

```
┌─────────────────────────────────────────────┐
│  Emoji                                     │
├─────────────────────────────────────────────┤
│  [搜索: _____________________________]      │
├─────────────────────────────────────────────┤
│  😀 | 👋 | 🐶 | 🍎 | ✈️ | ⚽ | 💡 | ❤️ | 🏁│
├─────────────────────────────────────────────┤
│  最近使用:                                   │
│  😀 😂 ❤️ 👍 🎉 🔥 ✨ 💯 🙏 😭           │
├─────────────────────────────────────────────┤
│  ┌───┬───┬───┬───┬───┬───┬───┬───┐        │
│  │ 😀│ 😁│ 😂│ 🤣│ 😃│ 😄│ 😅│ 😆│        │
│  ├───┼───┼───┼───┼───┼───┼───┼───┤        │
│  │ 😇│ 😈│ 🤪│ 😋│ 😌│ 😍│ 🥰│ 😘│        │
│  ├───┼───┼───┼───┼───┼───┼───┼───┤        │
│  │ 😗│ 🤩│ 🤔│ 🤨│ 😐│ 🤑│ 🤡│ 😎│        │
│  └───┴───┴───┴───┴───┴───┴───┴───┘        │
└─────────────────────────────────────────────┘
```

#### 4.9.4 Emoji 肤色变体

长按 Emoji 弹出肤色选择：

```
长按 👋 弹出:
┌─────────────────────────────────────┐
│  👋  ✋  🖐  🖖  🤚  🤛  👊  🤜  │
└─────────────────────────────────────┘
```

---

### 4.10 文本填充 (Text Expansion)

#### 4.10.1 功能说明

用户定义快捷短语，输入缩写后自动展开为完整文本。

| 缩写 | 展开 |
|------|------|
| `addr` | `北京市朝阳区xxx街道xxx号` |
| `tel` | `138-xxxx-xxxx` |
| `eml` | `user@example.com` |
| `ts` | 当前时间戳 |

#### 4.10.2 数据模型

```kotlin
data class TextExpansion(
    val id: String,
    val shortcut: String,           // "addr"
    val expansion: String,          // "北京市朝阳区xxx街道xxx号"
    val description: String? = null, // 描述
    val category: String = "default",
    val isEnabled: Boolean = true,
    val createdAt: Long,
    val lastUsedAt: Long? = null,
    val usageCount: Long = 0
)

data class ExpansionCategory(
    val id: String,
    val name: String,
    val expansions: List<TextExpansion>
)
```

#### 4.10.3 存储结构

```jsonc
// user/expansions.json
{
  "version": 1,
  "expansions": [
    {
      "id": "exp_001",
      "shortcut": "addr",
      "expansion": "北京市朝阳区xxx街道xxx号",
      "description": "我的地址",
      "category": "personal",
      "enabled": true
    },
    {
      "id": "exp_002",
      "shortcut": "ts",
      "expansion": "${datetime:yyyy-MM-dd HH:mm}",
      "description": "当前时间",
      "category": "system",
      "enabled": true
    }
  ]
}
```

#### 4.10.4 变量支持

| 变量 | 说明 | 示例输出 |
|------|------|---------|
| `${datetime:format}` | 当前时间 | `2026-06-13 14:30` |
| `${date:format}` | 当前日期 | `2026-06-13` |
| `${clipboard}` | 剪贴板内容 | - |
| `${selection}` | 当前选中文本 | - |

---

### 4.11 剪贴板管理

#### 4.11.1 功能说明

管理剪贴板历史，支持固定常用条目。

#### 4.11.2 数据模型

```kotlin
data class ClipboardEntry(
    val id: String,
    val content: String,
    val type: ClipboardType,        // TEXT | IMAGE | FILE
    val timestamp: Long,
    val isPinned: Boolean = false,  // 固定
    val sourceApp: String? = null,  // 来源应用
    val category: String? = null    // 用户分类
)

enum class ClipboardType { TEXT, IMAGE, FILE }

data class ClipboardConfig(
    val maxHistory: Int = 100,      // 最大历史条数
    val maxPinned: Int = 20,        // 最大固定条数
    val enableSync: Boolean = false // 跨设备同步
)
```

#### 4.11.3 剪贴板页面 UI

```
┌─────────────────────────────────────────────┐
│  剪贴板                         [清空] [设置]│
├─────────────────────────────────────────────┤
│  📌 固定                                    │
│  ┌─────────────────────────────────────┐   │
│  │ 138-xxxx-xxxx            [复制][删除]│   │
│  ├─────────────────────────────────────┤   │
│  │ user@example.com         [复制][删除]│   │
│  └─────────────────────────────────────┘   │
├─────────────────────────────────────────────┤
│  📋 历史                                    │
│  ┌─────────────────────────────────────┐   │
│  │ 这是一段复制的文本...   [复制][删除]  │   │
│  ├─────────────────────────────────────┤   │
│  │ https://example.com     [复制][删除]  │   │
│  ├─────────────────────────────────────┤   │
│  │ 2026-06-13 14:30        [复制][删除]  │   │
│  └─────────────────────────────────────┘   │
└─────────────────────────────────────────────┘
```

#### 4.11.4 剪贴板监听

```kotlin
class ClipboardManager @Inject constructor(
    private val context: Context,
    private val clipboardDao: ClipboardDao
) {
    private val clipboard = context.getSystemService(ClipboardManager::class.java)
    private val _entries = MutableStateFlow<List<ClipboardEntry>>(emptyList())
    val entries: StateFlow<List<ClipboardEntry>> = _entries

    fun startListening() {
        clipboard.addPrimaryClipChangedListener {
            val clip = clipboard.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val text = clip.getItemAt(0).text?.toString()
                if (text.isNullOrBlank()) return@addPrimaryClipChangedListener

                val entry = ClipboardEntry(
                    id = UUID.randomUUID().toString(),
                    content = text,
                    type = ClipboardType.TEXT,
                    timestamp = System.currentTimeMillis()
                )
                addEntry(entry)
            }
        }
    }

    suspend fun addEntry(entry: ClipboardEntry) {
        clipboardDao.insert(entry)
        refreshEntries()
    }
}
```

---

### 4.12 输入框类型适配

#### 4.12.1 功能说明

根据输入框的 `EditorInfo.inputType` 自动切换键盘布局。

#### 4.12.2 输入类型映射

```kotlin
object InputTypeMapper {
    fun mapToArrangement(inputType: Int): String {
        return when (inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER -> "number"
            InputType.TYPE_CLASS_PHONE -> "phone"
            InputType.TYPE_CLASS_TEXT -> {
                when (inputType and InputType.TYPE_MASK_VARIATION) {
                    InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> "email"
                    InputType.TYPE_TEXT_VARIATION_URI -> "url"
                    InputType.TYPE_TEXT_VARIATION_PASSWORD -> "password"
                    InputType.TYPE_TEXT_VARIATION_NORMAL -> "alpha"
                    else -> "alpha"
                }
            }
            InputType.TYPE_CLASS_DATETIME -> "datetime"
            else -> "alpha"
        }
    }
}
```

#### 4.12.3 预定义布局

| 输入类型 | 布局 ID | 说明 |
|---------|---------|------|
| `TYPE_CLASS_NUMBER` | `number` | 数字键盘 |
| `TYPE_CLASS_PHONE` | `phone` | 电话拨号键盘 |
| `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` | `email` | 邮箱键盘（带 @ 和 .） |
| `TYPE_TEXT_VARIATION_URI` | `url` | URL 键盘（带 / 和 .） |
| `TYPE_TEXT_VARIATION_PASSWORD` | `password` | 密码键盘（隐藏输入） |
| `TYPE_CLASS_DATETIME` | `datetime` | 日期时间键盘 |

#### 4.12.4 数字键盘布局

```jsonc
{
  "id": "number",
  "arrangements": {
    "default": {
      "rows": [
        { "keys": ["1","2","3"] },
        { "keys": ["4","5","6"] },
        { "keys": ["7","8","9"] },
        { "keys": [".","0","del"] }
      ]
    }
  }
}
```

#### 4.12.5 电话拨号键盘布局

```jsonc
{
  "id": "phone",
  "arrangements": {
    "default": {
      "rows": [
        { "keys": ["1","2","3"] },
        { "keys": ["4","5","6"] },
        { "keys": ["7","8","9"] },
        { "keys": ["*","0","#"] },
        { "keys": ["call","del"] }
      ]
    }
  }
}
```

---

### 4.13 统一 UI 设计

#### 4.13.1 字体规范

| 场景 | 字体 | 大小 | 字重 |
|------|------|------|------|
| **按键标签** | System Default | 18sp | Normal |
| **按键 Hint** | System Default | 10sp | Normal |
| **候选词** | System Default | 16sp | Normal |
| **候选词高亮** | System Default | 16sp | Bold |
| **工具栏文字** | System Default | 12sp | Normal |
| **符号网格** | Noto Sans Symbols | 20sp | Normal |
| **数学符号** | Noto Sans Math | 20sp | Normal |
| **Emoji** | Noto Color Emoji | 24sp | Normal |

#### 4.13.2 图标规范

| 图标 | 尺寸 | 说明 |
|------|------|------|
| **按键图标** | 24dp × 24dp | SVG 或 VectorDrawable |
| **工具栏图标** | 24dp × 24dp | 单色，支持 tint |
| **导航图标** | 20dp × 20dp | 返回、前进、关闭 |
| **状态图标** | 16dp × 16dp | Shift、锁定状态 |

#### 4.13.3 颜色规范

```jsonc
{
  "colors": {
    "background": {
      "light": "#F1F3F4",
      "dark": "#1F1F1F"
    },
    "surface": {
      "light": "#FFFFFF",
      "dark": "#2D2D2D"
    },
    "key": {
      "normal": {
        "light": "#FFFFFF",
        "dark": "#3C3C3C"
      },
      "pressed": {
        "light": "#E8EAED",
        "dark": "#4A4A4A"
      },
      "text": {
        "light": "#202124",
        "dark": "#E8EAED"
      }
    },
    "accent": {
      "light": "#1A73E8",
      "dark": "#8AB4F8"
    },
    "divider": {
      "light": "#DADCE0",
      "dark": "#3C3C3C"
    }
  }
}
```

#### 4.13.4 间距规范

| 场景 | 间距 | 说明 |
|------|------|------|
| **按键水平间距** | 4dp | 键盘区域 |
| **按键垂直间距** | 5dp | 键盘区域 |
| **键盘内边距** | 6dp | 四周 |
| **工具栏内边距** | 8dp | 左右 |
| **候选词间距** | 8dp | 候选栏 |
| **分类间距** | 4dp | 符号分类 |

#### 4.13.5 圆角规范

| 场景 | 圆角 | 说明 |
|------|------|------|
| **普通按键** | 8dp | 标准按键 |
| **功能按键** | 8dp | Shift、Backspace |
| **候选词** | 4dp | 候选栏项 |
| **工具栏按钮** | 12dp | 工具栏图标背景 |
| **分类标签** | 20dp | 符号分类标签 |
| **弹窗** | 12dp | Popup 容器 |

#### 4.13.6 动效规范

| 动效 | 时长 | 缓动 | 说明 |
|------|------|------|------|
| **按键按下** | 100ms | EaseOut | 缩放 0.95 |
| **按键释放** | 150ms | EaseInOut | 缩放回 1.0 |
| **候选切换** | 200ms | Linear | 水平滑动 |
| **面板切换** | 250ms | EaseInOut | 垂直滑动 |
| **主题切换** | 300ms | EaseInOut | 颜色渐变 |
| **Popup 弹出** | 200ms | EaseOut | 从下向上 |

---

### 4.14 设置页面

#### 4.14.1 设置项结构

```kotlin
sealed interface SettingsItem {
    data class Switch(
        val key: String,
        val title: String,
        val summary: String? = null,
        val defaultValue: Boolean = false
    ) : SettingsItem

    data class SingleChoice(
        val key: String,
        val title: String,
        val summary: String? = null,
        val options: List<ChoiceOption>,
        val defaultIndex: Int = 0
    ) : SettingsItem

    data class Slider(
        val key: String,
        val title: String,
        val summary: String? = null,
        val min: Float,
        val max: Float,
        val step: Float,
        val defaultValue: Float
    ) : SettingsItem

    data class Navigation(
        val title: String,
        val summary: String? = null,
        val route: String
    ) : SettingsItem

    data class Header(val title: String) : SettingsItem
    data class Footer(val text: String) : SettingsItem
}
```

#### 4.14.2 设置页面 UI

```
┌─────────────────────────────────────────────┐
│  ← 设置                                    │
├─────────────────────────────────────────────┤
│  语言                                       │
│  ├─ 当前语言                 [中文 (拼音) →] │
│  ├─ 管理语言                       [管理 →] │
│  └─ 自动切换语言                   [开启 ○] │
├─────────────────────────────────────────────┤
│  布局                                       │
│  ├─ 布局选择                     [QWERTY →] │
│  ├─ 键盘高度            [========●==] 260dp │
│  └─ 按键间距            [====●======] 4dp   │
├─────────────────────────────────────────────┤
│  外观                                       │
│  ├─ 主题                       [Dracula →] │
│  ├─ 字体大小            [=====●=====] 18sp  │
│  └─ 按键动画                       [开启 ○] │
├─────────────────────────────────────────────┤
│  功能                                       │
│  ├─ 词典管理                       [管理 →] │
│  ├─ 文本填充                       [管理 →] │
│  ├─ 剪贴板                         [管理 →] │
│  └─ 快捷手势                       [管理 →] │
├─────────────────────────────────────────────┤
│  AI                                        │
│  ├─ LLM 设置                       [配置 →] │
│  ├─ 语音输入                       [配置 →] │
│  └─ 智能联想                       [开启 ○] │
├─────────────────────────────────────────────┤
│  关于                                       │
│  ├─ 版本                         [1.0.0]    │
│  ├─ 开源许可                         [查看] │
│  └─ 反馈                               [→]  │
└─────────────────────────────────────────────┘
```

#### 4.14.3 设置页面路由

```kotlin
@Serializable
sealed interface SettingsRoute {
    @Serializable data object Main : SettingsRoute
    @Serializable data object Language : SettingsRoute
    @Serializable data object Layout : SettingsRoute
    @Serializable data object Theme : SettingsRoute
    @Serializable data object Dictionary : SettingsRoute
    @Serializable data object TextExpansion : SettingsRoute
    @Serializable data object Clipboard : SettingsRoute
    @Serializable data object LLM : SettingsRoute
    @Serializable data object STT : SettingsRoute
    @Serializable data object About : SettingsRoute
}
```

---

### 4.15 首次使用引导页面

#### 4.15.1 引导流程

```
启动 App
    ↓
┌─────────────────────────────────────────────┐
│  欢迎使用 MyBoard                           │
│  您的智能全球化输入法                         │
│                                             │
│  [开始设置]                                 │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│  1/4 启用键盘                                │
│  请在系统设置中启用 MyBoard                   │
│                                             │
│  [前往设置]                     [跳过]       │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│  2/4 选择语言                                │
│  选择您常用的语言                             │
│                                             │
│  ☑ English (US)                             │
│  ☑ 中文 (拼音)                              │
│  ☐ 日本語                                   │
│  ☐ 한국어                                   │
│  ☐ Français                                 │
│                                             │
│  [下一步]                                   │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│  3/4 选择主题                                │
│  选择您喜欢的键盘主题                         │
│                                             │
│  ┌─────────┐ ┌─────────┐ ┌─────────┐      │
│  │ Default │ │  Dark   │ │ Dracula │      │
│  │  [选中] │ │         │ │         │      │
│  └─────────┘ └─────────┘ └─────────┘      │
│                                             │
│  [下一步]                                   │
└─────────────────────────────────────────────┘
    ↓
┌─────────────────────────────────────────────┐
│  4/4 完成！                                  │
│  您已准备好使用 MyBoard                       │
│                                             │
│  ✓ 键盘已启用                               │
│  ✓ 语言已配置                               │
│  ✓ 主题已设置                               │
│                                             │
│  [开始使用]                                 │
└─────────────────────────────────────────────┘
    ↓
弹出键盘，开始使用
```

#### 4.15.2 引导状态管理

```kotlin
data class OnboardingState(
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val isCompleted: Boolean = false,
    val keyboardEnabled: Boolean = false,
    val selectedLanguages: List<String> = emptyList(),
    val selectedTheme: String? = null
)

class OnboardingManager @Inject constructor(
    private val prefs: DataStore<Preferences>
) {
    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state

    suspend fun checkOnboarding(): Boolean {
        val completed = prefs.data.map { it[KEY_ONBOARDING_COMPLETED] ?: false }.first()
        return completed
    }

    suspend fun completeOnboarding() {
        prefs.edit { it[KEY_ONBOARDING_COMPLETED] = true }
        _state.update { it.copy(isCompleted = true) }
    }

    companion object {
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }
}
```

#### 4.15.3 引导页面 UI

```kotlin
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val viewModel: OnboardingViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    when (state.currentStep) {
        0 -> WelcomeStep(onNext = { viewModel.nextStep() })
        1 -> EnableKeyboardStep(onNext = { viewModel.nextStep() }, onSkip = { viewModel.nextStep() })
        2 -> LanguageSelectionStep(
            selectedLanguages = state.selectedLanguages,
            onToggle = { viewModel.toggleLanguage(it) },
            onNext = { viewModel.nextStep() }
        )
        3 -> ThemeSelectionStep(
            selectedTheme = state.selectedTheme,
            onSelect = { viewModel.selectTheme(it) },
            onNext = { viewModel.nextStep() }
        )
        4 -> CompletionStep(onComplete = {
            viewModel.completeOnboarding()
            onComplete()
        })
    }
}
```

---

## 五、任务计划与里程碑

### 5.1 阶段规划

| 阶段 | 名称 | 目标 | 周期 | 依赖 |
|------|------|------|------|------|
| **P1** | 核心框架 | IME 服务 + 基础英文键盘 | 2 周 | - |
| **P2** | 布局系统 | JSON 解析 + 多语言布局 | 2 周 | P1 |
| **P3** | 词典系统 | Trie 词典 + 联想引擎 | 2 周 | P1 |
| **P4** | 多语言输入 | 中/英/日/韩输入引擎 | 3 周 | P2, P3 |
| **P5** | 高度自定义 | Code 键盘 + Remap + 候选/工具栏定制 | 2 周 | P4 |
| **P6** | 主题系统 | 内置主题 + 动效 + 图片主题 | 2 周 | P5 |
| **P7** | LLM 集成 | 本地/云端 LLM + 翻译/美化/联想 | 2 周 | P4 |
| **P8** | STT 集成 | 系统/端侧 STT | 1 周 | P4 |
| **P9** | 扩展语言 | 藏语/法语/阿拉伯语等 | 2 周 | P4 |
| **P10** | 优化发布 | 性能优化 + 测试 + 打包 | 2 周 | All |

**总周期**: 约 20 周（5 个月）

### 5.2 里程碑

| 里程碑 | 阶段 | 交付物 | 验收标准 |
|--------|------|--------|---------|
| **M1** | P1 | 基础英文键盘 | 可在设备上输入英文，支持 Shift/Backspace/Space/Enter |
| **M2** | P2 | JSON 布局系统 | 可通过 JSON 定义任意布局，支持模板继承 |
| **M3** | P3 | 词典联想 | 输入前缀可联想单词，支持用户词典 |
| **M4** | P4 | 多语言输入 | 中文拼音可输入汉字，日韩可输入对应文字 |
| **M5** | P5 | 高度自定义 | 可重映射按键，可自定义候选栏/工具栏 |
| **M6** | P6 | 主题系统 | 内置 5+ 主题，支持动效和图片主题 |
| **M7** | P7 | LLM 集成 | LLM 可补全、翻译、美化文本 |
| **M8** | P8 | STT 集成 | 语音输入可插入文本 |
| **M9** | P9 | 扩展语言 | 藏语/法语等可用 |
| **M10** | P10 | 发布版本 | APK < 10MB，启动 < 500ms，60fps 渲染 |

---

## 六、详细任务分解

### 6.1 P1: 核心框架 (2 周)

#### Task 1.1: IME 服务搭建 (3 天)
- [ ] 创建 `MyBoardImeService`
- [ ] 配置 `input_method.xml`
- [ ] 实现 `onCreateInputView`
- [ ] 实现 `onStartInput` / `onFinishInput`
- [ ] 测试：可在设置中启用并弹出键盘

#### Task 1.2: 键盘状态机 (2 天)
- [ ] 定义 `KeyboardState` 数据类
- [ ] 实现 `KeyboardStateManager`
- [ ] 实现 Arrangement/Modifier/InputMode 状态管理
- [ ] 测试：状态切换正确

#### Task 1.3: Action 分发系统 (2 天)
- [ ] 定义 `InputAction` sealed interface
- [ ] 实现 `ActionDispatcher`
- [ ] 实现基础 Action 处理
- [ ] 测试：Action 可正确执行

#### Task 1.4: Canvas 渲染器 (3 天)
- [ ] 实现 `KeyboardCanvas` View
- [ ] 实现按键几何计算
- [ ] 实现按键绘制
- [ ] 实现触摸事件处理
- [ ] 实现手势识别
- [ ] 测试：硬编码 QWERTY 布局可渲染并响应触摸

---

### 6.2 P2: 布局系统 (2 周)

#### Task 2.1: JSON 布局模型 (2 天)
- [ ] 定义 `KeyboardLayout` 数据模型
- [ ] 定义 `KeyData`, `RowData`, `ArrangementData`
- [ ] 定义 `KeyTemplate`, `GestureMap`
- [ ] 测试：模型可正确序列化/反序列化

#### Task 2.2: 布局解析器 (2 天)
- [ ] 实现 `LayoutParser`（kotlinx.serialization）
- [ ] 实现 JSON 行注释支持
- [ ] 实现模板继承解析
- [ ] 测试：可解析 `qwerty.json`

#### Task 2.3: 布局渲染适配 (3 天)
- [ ] 修改 `KeyboardCanvas` 支持 JSON 布局
- [ ] 实现几何配置解析
- [ ] 实现 Arrangement 切换
- [ ] 测试：JSON 布局可正确渲染

#### Task 2.4: 布局自定义 Patch (3 天)
- [ ] 定义 `LayoutPatch` 数据模型
- [ ] 实现 `PatchApplier`
- [ ] 实现 Replace/Insert/Remove 操作
- [ ] 测试：用户 Patch 可正确应用

#### Task 2.5: 多语言布局文件 (2 天)
- [ ] 编写 `qwerty.json`（英文）
- [ ] 编写 `zh_pinyin.json`（中文拼音）
- [ ] 编写 `ja_romaji.json`（日文罗马字）
- [ ] 编写 `ko_hangul.json`（韩文谚文）
- [ ] 测试：各布局可正确渲染

---

### 6.3 P3: 词典系统 (2 周)

#### Task 3.1: Trie 词典引擎 (3 天)
- [ ] 实现 `TrieDict` 数据结构
- [ ] 实现 `insert`, `prefixSearch`
- [ ] 实现 `fuzzySearch`（编辑距离）
- [ ] 测试：前缀查询性能 < 10ms

#### Task 3.2: 用户词典 (2 天)
- [ ] 实现 `UserDict`（Room 存储）
- [ ] 实现 `addWord`, `removeWord`
- [ ] 实现 Bigram 预测
- [ ] 测试：用户词典可持久化

#### Task 3.3: 词频词典 (2 天)
- [ ] 实现 `FrequencyDict`
- [ ] 加载系统词频数据
- [ ] 实现词频排序
- [ ] 测试：候选按词频排序

#### Task 3.4: 联想引擎 (3 天)
- [ ] 实现 `SuggestionEngine`
- [ ] 实现多源聚合
- [ ] 实现上下文预测
- [ ] 测试：输入前缀可联想

#### Task 3.5: 词典导入导出 (2 天)
- [ ] 实现词典 JSON 格式
- [ ] 实现导入功能
- [ ] 实现导出功能
- [ ] 测试：可导入/导出自定义词典

---

### 6.4 P4: 多语言输入 (3 周)

#### Task 4.1: 输入引擎框架 (2 天)
- [ ] 定义 `InputEngine` 接口
- [ ] 实现 `EngineManager`
- [ ] 实现引擎切换
- [ ] 测试：可动态切换引擎

#### Task 4.2: 直接输入引擎 (1 天)
- [ ] 实现 `DirectInputEngine`
- [ ] 支持英文等直接输入语言
- [ ] 测试：英文输入正常

#### Task 4.3: 拼音输入引擎 (5 天)
- [ ] 实现 `PinyinEngine`
- [ ] 实现拼音缓冲区
- [ ] 实现声调标记
- [ ] 实现模糊拼音
- [ ] 加载拼音词典
- [ ] 测试：拼音输入可联想汉字

#### Task 4.4: 日文输入引擎 (4 天)
- [ ] 实现 `JapaneseEngine`
- [ ] 实现罗马字→假名转换
- [ ] 实现假名→汉字转换
- [ ] 加载日文词典
- [ ] 测试：日文输入正常

#### Task 4.5: 韩文输入引擎 (4 天)
- [ ] 实现 `HangulEngine`
- [ ] 实现 Jamo 组合规则
- [ ] 实现音节分解
- [ ] 测试：韩文输入正常

#### Task 4.6: 混合输入支持 (4 天)
- [ ] 实现中英混合输入
- [ ] 实现混合联想
- [ ] 实现语言自动检测
- [ ] 测试：中英混合输入正常

---

### 6.5 P5: 高度自定义 (2 周)

#### Task 5.1: Code 键盘系统 (3 天)
- [ ] 定义 `KeySlot` 模型
- [ ] 实现 `KeyBindingManager`
- [ ] 实现多层绑定
- [ ] 测试：Code 键盘可正常工作

#### Task 5.2: 按键映射 (2 天)
- [ ] 定义 `RemapConfig` 模型
- [ ] 实现 `KeyRemapper`
- [ ] 实现映射应用
- [ ] 测试：用户映射可生效

#### Task 5.3: 候选栏自定义 (3 天)
- [ ] 定义 `CandidateBarConfig`
- [ ] 实现多种布局模式
- [ ] 实现数据源配置
- [ ] 测试：候选栏可自定义

#### Task 5.4: 工具栏自定义 (2 天)
- [ ] 定义 `ToolbarConfig`
- [ ] 实现工具栏项配置
- [ ] 实现排序/显隐
- [ ] 测试：工具栏可自定义

#### Task 5.5: 可视化编辑器 (4 天)
- [ ] 实现布局编辑器 UI
- [ ] 实现主题编辑器 UI
- [ ] 实现预览功能
- [ ] 测试：可视化编辑可用

---

### 6.6 P6: 主题系统 (2 周)

#### Task 6.1: 主题模型 (2 天)
- [ ] 定义 `KeyboardTheme` 模型
- [ ] 定义颜色/几何/字体配置
- [ ] 定义动画配置
- [ ] 测试：模型可序列化

#### Task 6.2: 内置主题 (3 天)
- [ ] 实现 `DefaultTheme`
- [ ] 实现 `DarkTheme`
- [ ] 实现 `DraculaTheme`
- [ ] 实现 `NordTheme`
- [ ] 实现 `SolarizedTheme`
- [ ] 测试：5 个内置主题可用

#### Task 6.3: 主题解析器 (3 天)
- [ ] 实现 `ThemeResolver`
- [ ] 实现颜色解析
- [ ] 实现几何解析
- [ ] 实现字体解析
- [ ] 测试：主题可正确应用

#### Task 6.4: 动效支持 (3 天)
- [ ] 实现按键动效
- [ ] 实现候选切换动效
- [ ] 实现主题切换过渡
- [ ] 测试：动效流畅

#### Task 6.5: 图片/GIF 主题 (3 天)
- [ ] 实现图片背景支持
- [ ] 实现 9-patch 支持
- [ ] 实现 GIF 动图支持
- [ ] 测试：图片主题可用

---

### 6.7 P7: LLM 集成 (2 周)

#### Task 7.1: LLM 框架 (2 天)
- [ ] 定义 `LLMProvider` 接口
- [ ] 实现 `LLMBridge`
- [ ] 实现 Provider 管理
- [ ] 测试：框架可运行

#### Task 7.2: 本地 LLM (4 天)
- [ ] 集成 ONNX Runtime
- [ ] 实现模型加载
- [ ] 实现流式推理
- [ ] 优化内存占用
- [ ] 测试：本地推理可用

#### Task 7.3: 云端 LLM (3 天)
- [ ] 实现 OkHttp 客户端
- [ ] 实现 SSE 流式解析
- [ ] 实现 API Key 管理
- [ ] 测试：云端调用可用

#### Task 7.4: LLM 功能实现 (3 天)
- [ ] 实现自动补全
- [ ] 实现翻译功能
- [ ] 实现语句美化
- [ ] 实现智能联想
- [ ] 测试：各功能可用

---

### 6.8 P8: STT 集成 (1 周)

#### Task 8.1: STT 框架 (1 天)
- [ ] 定义 `STTProvider` 接口
- [ ] 实现 `STTBridge`
- [ ] 测试：框架可运行

#### Task 8.2: 系统 STT (2 天)
- [ ] 集成 Android SpeechRecognizer
- [ ] 实现流式结果
- [ ] 测试：系统 STT 可用

#### Task 8.3: 端侧 STT (2 天)
- [ ] 集成 Whisper.cpp
- [ ] 实现 JNI 桥接
- [ ] 测试：端侧 STT 可用

---

### 6.9 P9: 扩展语言 (2 周)

#### Task 9.1: 藏语支持 (4 天)
- [ ] 实现藏文基字+叠加组合
- [ ] 实现元音符号位置
- [ ] 编写藏语布局
- [ ] 测试：藏语输入正常

#### Task 9.2: 法语支持 (2 天)
- [ ] 编写 AZERTY 布局
- [ ] 实现重音符号
- [ ] 测试：法语输入正常

#### Task 9.3: 阿拉伯语支持 (4 天)
- [ ] 实现 RTL 布局
- [ ] 实现连字塑形
- [ ] 编写阿拉伯语布局
- [ ] 测试：阿拉伯语输入正常

#### Task 9.4: 其他语言 (4 天)
- [ ] 德语 QWERTZ
- [ ] 西班牙语
- [ ] 俄语
- [ ] 印地语
- [ ] 泰语
- [ ] 测试：各语言输入正常

---

### 6.10 P10: 优化发布 (2 周)

#### Task 10.1: 性能优化 (4 天)
- [ ] 优化启动速度
- [ ] 优化内存占用
- [ ] 优化触摸响应
- [ ] 优化词典查询
- [ ] 测试：启动 < 500ms，60fps

#### Task 10.2: 测试 (4 天)
- [ ] 编写单元测试
- [ ] 编写集成测试
- [ ] 编写 UI 测试
- [ ] 修复 Bug
- [ ] 测试：所有测试通过

#### Task 10.3: 打包发布 (2 天)
- [ ] 配置 ProGuard
- [ ] 配置签名
- [ ] 生成 Release APK
- [ ] 编写发布说明
- [ ] 测试：APK 可安装使用

---

## 七、交付物清单

### 7.1 代码交付

| 模块 | 交付物 | 说明 |
|------|--------|------|
| **core** | 键盘核心库 | 状态机、Action、布局引擎 |
| **input** | 输入引擎 | 直接/组合/复杂输入 |
| **dictionary** | 词典系统 | Trie + 用户词典 + 联想 |
| **ai** | AI 模块 | LLM + STT |
| **ui** | UI 组件 | 键盘、候选、工具栏、设置 |
| **service** | IME 服务 | 系统服务入口 |

### 7.2 资源交付

| 类型 | 数量 | 说明 |
|------|------|------|
| **布局文件** | 15+ | 各语言 JSON 布局 |
| **词典文件** | 10+ | 各语言词典 |
| **主题文件** | 10+ | 内置主题 |
| **Prompt 模板** | 10+ | LLM Prompt |

### 7.3 文档交付

| 文档 | 说明 |
|------|------|
| **DESIGN.md** | 概要设计文档 |
| **API.md** | API 接口文档 |
| **USER_GUIDE.md** | 用户使用指南 |
| **DEV_GUIDE.md** | 开发者指南 |
| **CHANGELOG.md** | 变更日志 |

---

## 八、风险与应对

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 词典文件过大 | 安装包膨胀 | 高 | 分包下载，按需加载 |
| LLM 模型内存高 | 低端设备卡顿 | 高 | 提供关闭选项，使用量化模型 |
| 复杂文字渲染 | 藏文等显示异常 | 中 | 参考系统实现，逐步适配 |
| 多语言切换复杂 | 状态不一致 | 中 | 统一状态机，严格测试 |
| ONNX 兼容性 | 部分设备不支持 | 低 | Fallback 到云端 |

---

## 九、附录

### 9.1 术语表

| 术语 | 说明 |
|------|------|
| **IME** | Input Method Editor，输入法编辑器 |
| **Layout** | 键盘布局，定义按键排列 |
| **Arrangement** | 布局排列，如 alpha/symbols/numbers |
| **Template** | 按键模板，定义行为模式 |
| **Slot** | 物理按键槽位 |
| **Binding** | 按键绑定，Slot → Output 的映射 |
| **Layer** | 层，如 Base/Shift/Symbol |
| **Remap** | 重映射，用户自定义按键输出 |
| **Trie** | 前缀树，高效字符串查询数据结构 |
| **Jamo** | 韩文字母 |
| **Abugida** | 元音附标文字，如天城文、藏文 |

### 9.2 参考资源

- [Android IME 开发指南](https://developer.android.com/develop/ui/views/touch-and-input/input-methods)
- [FlorisBoard](https://github.com/florisboard/florisboard) - 开源输入法参考
- [fcitx5-android](https://github.com/fcitx5-android/fcitx5-android) - fcitx5 Android 版
- [AnySoftKeyboard](https://github.com/AnySoftKeyboard/AnySoftKeyboard) - 开源输入法
- [Whisper.cpp](https://github.com/ggerganov/whisper.cpp) - 端侧语音识别

---

**文档结束**
