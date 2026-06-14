# P4: 多语言输入 (3 周)

## 1. 目标

实现中文拼音、五笔、双拼、笔画，日文罗马字，韩文谚文输入引擎，以及中英日韩等多语言链式切换。

## 2. 里程碑验收标准

- [x] 中文拼音可输入汉字
- [x] 中文五笔可输入汉字
- [x] 日文罗马字可输入假名→汉字
- [x] 韩文谚文可组合输入
- [x] 任意语言对链式切换正确
- [x] 中英混合输入可用

## 3. 详细设计

### 3.1 通用引擎实现

#### 3.1.1 DirectInputEngine

```kotlin
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
                if (config.shift?.autoOffAfterKeys == true) shiftState = ShiftState.OFF
                char.uppercase()
            }
            else -> char.lowercase()
        }
        userDict.recordWord(output)
        return EngineResult.CommitText(output)
    }

    override suspend fun onShift(): EngineResult {
        if (config.shift?.mode == "disabled") return EngineResult.Nothing
        shiftState = when (shiftState) {
            ShiftState.OFF -> ShiftState.ON
            ShiftState.ON -> ShiftState.OFF
            ShiftState.CAPS_LOCK -> ShiftState.OFF
        }
        capsLock = false
        return EngineResult.Nothing
    }

    override suspend fun onDoubleShift(): EngineResult {
        if (config.shift?.mode == "disabled") return EngineResult.Nothing
        capsLock = !capsLock
        shiftState = if (capsLock) ShiftState.CAPS_LOCK else ShiftState.OFF
        return EngineResult.Nothing
    }

    override suspend fun onBackspace(): EngineResult = EngineResult.Delete(1)
    override suspend fun onSpace(): EngineResult = EngineResult.CommitText(" ")
    override suspend fun onEnter(): EngineResult = EngineResult.Nothing
    override fun getShiftState(): ShiftState = shiftState
}
```

#### 3.1.2 CompositionInputEngine

```kotlin
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
        val resolved = composingResolver.resolve(composingBuffer.toString(), config.engineParams)
        candidates = dictLookup.lookup(resolved, config.engineParams)
        return EngineResult.Combined(resolved.displayText, candidates, null)
    }

    override suspend fun onSpace(): EngineResult {
        return when {
            composingBuffer.isEmpty() -> EngineResult.CommitText(" ")
            candidates.isNotEmpty() -> {
                val first = candidates.first()
                reset()
                EngineResult.CommitText(first.text)
            }
            else -> {
                if (config.engineParams["autoCommitOnSpace"] == true) {
                    val text = composingBuffer.toString()
                    reset()
                    EngineResult.CommitText(text)
                } else EngineResult.Nothing
            }
        }
    }

    override suspend fun onEnter(): EngineResult {
        return when (config.enter?.composing) {
            "commitThenAction" -> {
                val text = composingBuffer.toString()
                reset()
                EngineResult.Combined(null, null, text)
            }
            "selectFirst" -> {
                if (candidates.isNotEmpty()) {
                    val first = candidates.first()
                    reset()
                    EngineResult.CommitText(first.text)
                } else {
                    val text = composingBuffer.toString()
                    reset()
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
                    val resolved = composingResolver.resolve(composingBuffer.toString(), config.engineParams)
                    candidates = dictLookup.lookup(resolved, config.engineParams)
                    EngineResult.Combined(resolved.displayText, candidates, null)
                }
            }
            else -> EngineResult.Delete(1)
        }
    }

    override suspend fun onCandidateSelected(index: Int): EngineResult {
        val candidate = candidates.getOrNull(index) ?: return EngineResult.Nothing
        reset()
        return EngineResult.CommitText(candidate.text)
    }

    override fun reset() {
        composingBuffer.clear()
        candidates = emptyList()
    }

    override fun getComposingText(): String = composingBuffer.toString()
    override fun getCandidates(): List<Candidate> = candidates
    override fun getShiftState(): ShiftState = ShiftState.OFF
}
```

#### 3.1.3 ComplexInputEngine

```kotlin
class ComplexInputEngine(
    private val config: InputMethodConfig,
    private val composingResolver: ComposingResolver
) : InputEngine {
    override val id = config.id
    override val type = EngineType.COMPLEX
    private val buffer = mutableListOf<String>()

    override suspend fun onKeyInput(char: String): EngineResult {
        buffer.add(char)
        val resolved = composingResolver.resolve(buffer.joinToString(""), config.engineParams)
        return EngineResult.UpdateComposing(resolved.displayText)
    }

    override suspend fun onBackspace(): EngineResult {
        if (buffer.isNotEmpty()) {
            buffer.removeAt(buffer.size - 1)
            if (buffer.isEmpty()) return EngineResult.UpdateComposing("")
            val resolved = composingResolver.resolve(buffer.joinToString(""), config.engineParams)
            return EngineResult.UpdateComposing(resolved.displayText)
        }
        return EngineResult.Delete(1)
    }

    override suspend fun onSpace(): EngineResult {
        val text = buffer.joinToString("")
        buffer.clear()
        return EngineResult.CommitText(text)
    }

    override fun reset() { buffer.clear() }
}
```

### 3.2 语言切换状态机

#### 3.2.1 LanguageSwitchManager

```kotlin
class LanguageSwitchManager(
    private val rules: List<SwitchRule>,
    private val languageRegistry: LanguageRegistry
) {
    private val languageStates = mutableMapOf<String, LanguageState>()
    private val history = ArrayDeque<String>(10)

    fun switch(from: String, to: String): SwitchAction {
        val fromLang = languageRegistry.get(from) ?: return SwitchAction.Noop
        val toLang = languageRegistry.get(to) ?: return SwitchAction.Noop

        val rule = rules.firstOrNull {
            (it.fromType == fromLang.type || it.fromType.name == "*") &&
            (it.toType == toLang.type || it.toType.name == "*")
        } ?: rules.last()

        saveState(from)
        val restoredState = if (rule.preservePerLanguageState) languageStates[to] else null
        history.addLast(from)

        return SwitchAction(
            targetLanguage = to,
            clearComposing = rule.clearComposing,
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
        languageStates[langId] = LanguageState(ShiftState.OFF, "alpha")
    }
}

data class LanguageState(
    val shift: ShiftState,
    val arrangement: String,
    val capsLock: Boolean = false
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

### 3.3 输入法配置

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
      "composingType": "pinyin"
    },
    "shift": { "mode": "disabled" },
    "enter": { "idle": "editorAction", "composing": "commitThenAction", "hasCandidates": "selectFirst" },
    "space": { "idle": "commitText", "composing": "commitComposition", "hasCandidates": "selectFirst" },
    "backspace": { "idle": "delete", "composing": "deleteComposition" }
  }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `input/engine/InputEngine.kt` | 引擎接口 |
| `input/direct/DirectInputEngine.kt` | 直接输入引擎 |
| `input/composing/CompositionInputEngine.kt` | 组合输入引擎 |
| `input/complex/ComplexInputEngine.kt` | 复杂文字引擎 |
| `input/engine/InputMethodRegistry.kt` | 引擎注册表 |
| `core/keyboard/LanguageSwitchManager.kt` | 语言切换 |
| `assets/input_methods/zh_pinyin.json` | 拼音配置 |
| `assets/input_methods/zh_wubi.json` | 五笔配置 |
| `assets/input_methods/ja_romaji.json` | 罗马字配置 |
| `assets/input_methods/ko_hangul.json` | 谚文配置 |
