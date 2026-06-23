# MyBoard 输入法引擎详细设计

> 版本：v1.0  
> 状态：Draft  
> 日期：2026-06-15  
> 定位：本文档是 MyBoard 输入引擎层的唯一实现标准。  
> 依据：`docs/orthogonal-state-management.md` 中的 `LanguageManifest`、`SchemaCapability`、`KeyboardContext` 与 `TransitionEngine` 设计；`docs/android-bridge.md` 中的 `InputConnectionGateway`、`SelectionTracker`、`ResetReason` 产生来源。  
> 原则：与当前输入引擎相关实现冲突时，允许破坏性重构。

## 1. 目标

输入引擎层只负责一件事：在当前已经合法的 `KeyboardContext.orthogonal` 下，把输入事件处理成可执行结果。

它不负责：

- 判断 `Locale + Script + Schema` 是否合法。
- 切换 Locale、Script、Schema。
- 保存用户设置。
- 渲染键盘 UI。
- 直接读取布局 JSON。
- 直接决定 Android Subtype。

它负责：

- 接收按键、退格、空格、回车、候选选择等输入事件。
- 维护当前输入会话的临时状态，例如编码 buffer、组合串、候选分页。
- 根据 `SchemaCapability` 加载映射表、编码器、FSM、字典和候选策略。
- 输出 `EngineResult`，由上层 `InputPipeline` 执行 commit、delete、更新 composing/candidates。

## 2. 总体架构

```text
Layout/UI
  -> InputAction
  -> InputPipeline
  -> InputSession
  -> InputEngine
  -> Mapping / Encoder / FSM / Dictionary / CandidatePolicy
  -> EngineResult
  -> InputConnection + KeyboardContextManager
```

核心分层：

| 层级 | 职责 |
| --- | --- |
| `InputPipeline` | 连接 UI action、当前状态、输入会话和 Android `InputConnection` |
| `InputEngine` | 引擎工厂，根据能力配置创建 `InputSession` |
| `InputSession` | 单个 Schema 下的运行时输入会话，持有 buffer 和候选 |
| `EngineRegistry` | 通过稳定 `engineId` 找到引擎工厂 |
| `EncoderRegistry` | 通过 `encoderId` 找到编码器 |
| `DictionaryRegistry` | 加载和缓存字典 |
| `CandidatePolicyRegistry` | 定义空格、数字键、候选排序、分页等策略 |
| `DisplayPolicyRegistry` | 定义 composing 文本如何展示 |

## 3. 三大内置引擎族

MyBoard 首版只内置三类通用引擎。语言差异通过 Manifest 数据、映射表、编码器、FSM 和字典注入，不为每种语言单独写引擎类。

### 3.1 DirectEngine

适用范围：

- 英文、法文、德文等直接上屏语言。
- 藏文、维吾尔文、印地文等可由按键映射直接输出 Unicode 文本的语言。
- 任何“一次按键或手势 -> 一个上屏文本”的输入方案。

核心流程：

```text
PushToken
  -> 查 mapping
  -> 应用 layer/modifier
  -> CommitText
```

能力数据来自 Manifest：

```json
{
  "engineId": "direct",
  "layoutId": "qwerty",
  "supportsShift": true,
  "mapping": "maps/latin_qwerty.json",
  "candidatePolicy": "direct_default",
  "displayPolicy": "hidden"
}
```

Mapping 示例：

```json
{
  "id": "latin_qwerty",
  "layers": {
    "NORMAL": {
      "a": "a",
      "b": "b"
    },
    "SHIFTED": {
      "a": "A",
      "b": "B"
    }
  },
  "fallback": {
    "unknownToken": "commitToken"
  }
}
```

设计要求：

- DirectEngine 不保存 composing buffer。
- Shift/Caps 是否可用由 `SchemaCapability.supportsShift` 决定。
- 未命中 mapping 时按 mapping 的 fallback 策略处理。
- 变音符、长按符号、AltGr 等后续作为 mapping 扩展，不单独新建引擎。

### 3.2 TableComposingEngine

适用范围：

- 全拼、双拼。
- 五笔、仓颉、郑码。
- 注音。
- 任何“输入编码 -> 查表 -> 候选词 -> 选词上屏”的方案。

核心流程：

```text
PushToken
  -> Encoder 将 token 转成查询码
  -> 更新 rawBuffer/queryBuffer
  -> Dictionary 查询候选
  -> CandidatePolicy 排序/截断/分页
  -> UpdateComposing
```

能力数据来自 Manifest：

```json
{
  "engineId": "table_composing",
  "layoutId": "qwerty",
  "supportsShift": false,
  "encoderId": "identity",
  "dictionary": "dicts/pinyin_main.dict",
  "candidatePolicy": "chinese_default",
  "displayPolicy": "show_query"
}
```

双拼示例：

```json
{
  "engineId": "table_composing",
  "layoutId": "shuangpin",
  "supportsShift": false,
  "encoderId": "double_pinyin",
  "encoderConfig": "encoders/double_pinyin_xiaohe.json",
  "dictionary": "dicts/pinyin_main.dict",
  "candidatePolicy": "chinese_default",
  "displayPolicy": "show_raw"
}
```

五笔示例：

```json
{
  "engineId": "table_composing",
  "layoutId": "qwerty",
  "supportsShift": false,
  "encoderId": "table_mapping",
  "encoderConfig": "encoders/wubi86.json",
  "dictionary": "dicts/wubi86.dict",
  "candidatePolicy": "shape_default",
  "displayPolicy": "show_query"
}
```

设计要求：

- 引擎只实现通用 buffer、查表、候选选择流程。
- 全拼、双拼、五笔、仓颉、注音差异只能落在 `Encoder`、`Dictionary`、`CandidatePolicy` 数据中。
- 空格、回车、数字选词、退格行为由 `CandidatePolicy` 决定。
- 字典查询必须可取消，避免快速输入时旧查询覆盖新结果。
- 查询结果写入 `EngineResult.UpdateComposing`，不由引擎直接更新 UI。

### 3.3 TransliterationEngine

适用范围：

- 日文罗马音转假名。
- 韩文谚文组合。
- 其他需要根据上下文按键流实时转写的语言。

核心流程：

```text
PushToken
  -> FSM 接收 token
  -> 产生 composing text 或 commit fragment
  -> 可选接入 conversion dictionary
  -> EngineResult
```

能力数据来自 Manifest：

```json
{
  "engineId": "transliteration",
  "layoutId": "qwerty",
  "supportsShift": false,
  "fsm": "rules/romaji_hira.fsm.json",
  "outputScript": "HIRA",
  "candidatePolicy": "japanese_kana_default",
  "displayPolicy": "show_composing",
  "conversionDictionary": "dicts/japanese_conversion.dict"
}
```

FSM 规则示例：

```json
{
  "id": "romaji_hira",
  "startState": "root",
  "states": {
    "root": {
      "k": { "next": "k" },
      "a": { "emit": "あ", "next": "root" }
    },
    "k": {
      "a": { "emit": "か", "next": "root" },
      "i": { "emit": "き", "next": "root" }
    }
  }
}
```

设计要求：

- FSM 是数据，不把罗马音规则硬编码在 Kotlin 中。
- 基础转写和候选转换分层处理。
- 日文假名转汉字属于 conversion layer，不混入 FSM 核心。
- 韩文谚文组合可以由 FSM 或专用 composition rule 数据驱动，但仍走 `transliteration` 引擎族。

## 4. 核心接口

### 4.1 InputEngine

`InputEngine` 是无状态工厂，不直接处理按键：

```kotlin
interface InputEngine {
    val engineId: String

    fun createSession(
        context: EngineContext
    ): InputSession
}
```

### 4.2 InputSession

`InputSession` 是有状态运行时对象。每次当前 `SchemaCapability` 变化，都必须关闭旧 session 并创建新 session。

```kotlin
interface InputSession {
    val capabilityId: CapabilityId
    val state: StateFlow<InputSessionState>

    suspend fun handle(event: InputEvent): EngineResult
    suspend fun reset(reason: ResetReason)
    suspend fun close()
}
```

### 4.3 EngineContext

```kotlin
data class EngineContext(
    val keyboardContext: KeyboardContext,
    val capability: SchemaCapability,
    val resources: EngineResources,
    val coroutineScope: CoroutineScope
)
```

`EngineResources` 由注册表提前解析，避免 session 自己到处找资源：

```kotlin
data class EngineResources(
    val mapping: KeyMapping? = null,
    val encoder: Encoder? = null,
    val fsm: TransliterationFsm? = null,
    val dictionary: Dictionary? = null,
    val candidatePolicy: CandidatePolicy,
    val displayPolicy: DisplayPolicy
)
```

### 4.4 InputEvent

```kotlin
sealed interface InputEvent {
    data class PushToken(val token: String) : InputEvent
    data object Backspace : InputEvent
    data object Space : InputEvent
    data object Enter : InputEvent
    data class SelectCandidate(val index: Int) : InputEvent
    data class SelectCandidateByLabel(val label: String) : InputEvent
    data object Reset : InputEvent
}
```

布局层只产生 `InputAction`，由 `InputPipeline` 转成 `InputEvent`。引擎不直接读取布局 key id。

### 4.5 EngineResult

```kotlin
sealed interface EngineResult {
    data class CommitText(val text: String) : EngineResult
    data class UpdateComposing(
        val text: String,
        val candidates: List<Candidate>,
        val selectedIndex: Int = -1
    ) : EngineResult
    data class CommitAndUpdate(
        val commit: String,
        val composing: String,
        val candidates: List<Candidate>
    ) : EngineResult
    data class DeleteText(val beforeCursor: Int) : EngineResult
    data object PerformEditorAction : EngineResult
    data object ClearComposing : EngineResult
    data object Nothing : EngineResult
}
```

`EngineResult` 不直接操作 `InputConnection`。只有 `InputPipeline` 可以执行 commit/delete/editor action。

### 4.6 InputSessionState

```kotlin
data class InputSessionState(
    val rawBuffer: String = "",
    val queryBuffer: String = "",
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedIndex: Int = -1,
    val page: Int = 0
)
```

`KeyboardContext` 只保存 UI 需要的快照。完整 buffer 状态属于 `InputSession`。

### 4.7 KeyboardContext 同步协议

`InputSessionState` 是引擎内部状态源，`KeyboardContext` 只保存 UI 所需快照。二者必须通过 `InputPipeline` 单向同步，禁止 UI 或状态层直接修改 session buffer。

同步规则：

| 来源 | 目标 | 触发 |
| --- | --- | --- |
| `InputSessionState.composingText` | `KeyboardContext.composingText` | `EngineResult.UpdateComposing` |
| `InputSessionState.candidates` | `KeyboardContext.candidates` | `EngineResult.UpdateComposing` |
| `InputSessionState.selectedIndex` | `KeyboardContext.selectedCandidateIndex` | `EngineResult.UpdateComposing` |
| `ClearComposing` | 清空 `KeyboardContext` composing/candidates | commit、reset、close |
| `KeyboardContext.orthogonal/layoutId/layer` | 新建 session 的 `EngineContext` | Schema 或布局能力变化 |

约束：

- `KeyboardContext` 不保存 `rawBuffer` 和 `queryBuffer`。
- `KeyboardContextManager.setComposing()` 只能由 `InputPipeline` 调用。
- 候选栏选择候选时，UI 发出 action，Pipeline 转成 `InputEvent.SelectCandidate`，不能直接提交候选。
- session reset/close 后，Pipeline 必须同步清空 `KeyboardContext` 的 composing/candidates。
- 如果 Pipeline 丢弃旧查询结果，不得把该结果写入 `KeyboardContext`。

## 5. 编码器、字典和候选策略

### 5.1 Encoder

```kotlin
interface Encoder {
    val encoderId: String

    fun append(
        state: EncodingState,
        token: String
    ): EncodingState

    fun backspace(
        state: EncodingState
    ): EncodingState
}
```

```kotlin
data class EncodingState(
    val rawBuffer: String,
    val queryBuffer: String,
    val displayText: String = queryBuffer
)
```

内置 Encoder：

| encoderId | 用途 |
| --- | --- |
| `identity` | token 原样拼接，适合全拼 |
| `table_mapping` | 查映射表，适合五笔、仓颉、郑码 |
| `double_pinyin` | 双拼编码器，具体方案由 `encoderConfig` 指定，例如小鹤、自然码 |
| `bopomofo` | 注音编码 |

要求：

- Encoder 必须是纯逻辑，不访问 UI 和 `InputConnection`。
- 编码规则优先数据化。
- 特定算法无法纯数据表达时，使用内置 `encoderId`，但不能按语言散落到引擎类。

### 5.2 Dictionary

```kotlin
interface Dictionary {
    val dictionaryId: String

    suspend fun lookup(
        query: String,
        limit: Int
    ): List<Candidate>
}
```

字典格式首版支持：

- 文本词典：`query<TAB>word<TAB>weight`
- 二进制词典：后续优化使用
- 用户词典：以更高权重合并

字典加载键：

```kotlin
data class DictionaryKey(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema,
    val path: String
)
```

### 5.3 CandidatePolicy

```kotlin
interface CandidatePolicy {
    val policyId: String

    fun sort(candidates: List<Candidate>): List<Candidate>
    fun onSpace(state: InputSessionState): PolicyAction
    fun onEnter(state: InputSessionState): PolicyAction
    fun onCandidateSelected(state: InputSessionState, index: Int): PolicyAction
}
```

```kotlin
sealed interface PolicyAction {
    data class Commit(val text: String) : PolicyAction
    data class Update(val state: InputSessionState) : PolicyAction
    data class Delete(val beforeCursor: Int) : PolicyAction
    data object PerformEditorAction : PolicyAction
    data object Noop : PolicyAction
}
```

内置策略：

| policyId | 行为 |
| --- | --- |
| `direct_default` | 空格上屏空格，回车执行 editor action |
| `chinese_default` | 有候选时空格选首候选，回车提交编码串 |
| `shape_default` | 有候选时空格选首候选，数字键选候选 |
| `japanese_kana_default` | 基础假名转写，后续可接 conversion |

### 5.4 DisplayPolicy

`DisplayPolicy` 决定 session 内部 buffer 如何展示为 composing 文本。它只负责显示转换，不负责字典查询、候选排序或上屏。

```kotlin
interface DisplayPolicy {
    val policyId: String

    fun display(
        state: InputSessionState
    ): String
}
```

内置策略：

| policyId | 行为 |
| --- | --- |
| `show_raw` | 显示用户原始按键序列，适合双拼等需要保留按键感知的方案 |
| `show_query` | 显示编码器生成的查询码，适合全拼、五笔等 |
| `show_composing` | 显示引擎生成的 composingText，适合转写类输入 |
| `hidden` | 不显示 composing，仅显示候选或直接提交 |

规则：

- `TableComposingEngine` 在查询候选前更新 `rawBuffer/queryBuffer`，再由 `DisplayPolicy` 生成 composing 文本。
- `TransliterationEngine` 默认使用 `show_composing`。
- `DirectEngine` 默认使用 `hidden`。
- Manifest 未声明 `displayPolicy` 时，按引擎族默认策略选择。

## 6. InputPipeline

`InputPipeline` 是输入引擎层和状态层之间的唯一协调器。

```kotlin
class InputPipeline(
    private val contextManager: KeyboardContextManager,
    private val engineRegistry: EngineRegistry,
    private val resourceResolver: EngineResourceResolver,
    private val inputConnectionProvider: () -> InputConnection?
) {
    suspend fun handle(action: InputAction)
    suspend fun onContextChanged(context: KeyboardContext)
    suspend fun reset(reason: ResetReason)
}
```

职责：

1. 监听 `KeyboardContext.orthogonal` 和 `SchemaCapability` 变化。
2. 当前能力变化时关闭旧 `InputSession`。
3. 通过 `EngineRegistry` 和 `EngineResourceResolver` 创建新 session。
4. 将布局动作转换为 `InputEvent`。
5. 将 `EngineResult` 执行到 `InputConnection`。
6. 将 composing/candidates 快照写回 `KeyboardContextManager`。
7. 输入目标变化、Subtype 变化、Schema 变化时 reset session。

`InputPipeline` 是唯一允许同时访问 `InputConnection` 和 `KeyboardContextManager` 的输入层组件。

### 6.1 并发与线程模型

输入处理必须串行化。同一个 `InputPipeline` 内不允许多个 `handle(action)` 并发修改同一个 `InputSession`。

实现要求：

- `InputPipeline.handle(action)` 通过 `Mutex` 或 actor 队列串行执行。
- `InputSession` 内部维护递增的 `generation` 或 `querySeq`。
- 每次 buffer 变化并触发异步字典查询时，记录当前 `querySeq`。
- 字典查询必须在 `Dispatchers.IO` 执行。
- 查询返回后必须比对 `querySeq` 和 session 是否仍 active；不匹配的旧结果直接丢弃。
- `InputSession.close()` 必须取消 session scope，终止未完成查询。
- `KeyboardContextManager` 状态更新必须在主线程执行。
- `InputConnection` 调用必须在 IME 主线程执行。

推荐流程：

```text
PushToken
  -> Pipeline 串行进入 session.handle
  -> session 更新 buffer 并递增 querySeq
  -> IO 查询字典
  -> 回到主线程前检查 querySeq
  -> 仍匹配则返回 UpdateComposing
  -> 不匹配则丢弃结果并返回 Nothing
```

这样可以避免快速输入时旧查询结果覆盖新 buffer。

### 6.2 EngineResult 到 InputConnection 的映射

`EngineResult` 只能由 `InputPipeline` 解释并执行。映射规则必须固定，不能散落在引擎实现中。

| EngineResult | Pipeline 行为 |
| --- | --- |
| `CommitText(text)` | 调用 `finishComposingText()`，再 `commitText(text, 1)`，然后清空 `KeyboardContext` 的 composing/candidates |
| `UpdateComposing(text, candidates, selectedIndex)` | 调用 `setComposingText(text, 1)`，并把 text/candidates/selectedIndex 写回 `KeyboardContextManager` |
| `CommitAndUpdate(commit, composing, candidates)` | 先 `finishComposingText()`，再 `commitText(commit, 1)`，然后如有 composing 则 `setComposingText(composing, 1)` |
| `DeleteText(beforeCursor)` | 调用 `deleteSurroundingText(beforeCursor, 0)` |
| `PerformEditorAction` | 根据当前 `EditorInfo.imeOptions` 执行 editor action |
| `ClearComposing` | 调用 `finishComposingText()`，reset session，并清空 `KeyboardContext` 的 composing/candidates |
| `Nothing` | 不操作 `InputConnection` |

补充规则：

- 组合态存在时，Backspace 必须优先进入 `InputSession.handle(Backspace)`，让 session 删除 buffer。
- 只有 session 返回 `DeleteText` 时，Pipeline 才删除宿主文本。
- `setComposingText()` 失败或 `InputConnection` 为空时，必须 reset session 并清空 context composing。
- `InputPipeline` 不使用 `setComposingRegion()` 作为首版主路径；复杂编辑场景统一通过 reset 降级，避免 buffer 与宿主文本区域错位。
- 光标移动、输入框切换、宿主应用外部修改文本导致组合态不可信时，Pipeline 必须 reset session。

## 7. Manifest 对接

`LanguageManifest` 的每个 Schema 必须提供足够信息让引擎层创建 session。

字段规范以 `docs/orthogonal-state-management.md` 的 `SchemaCapability 字段规范` 为唯一来源；本节只从引擎创建 session 的角度重复约束。

通用 SchemaCapability：

```json
{
  "engineId": "table_composing",
  "layoutId": "qwerty",
  "supportsShift": false,
  "encoderId": "identity",
  "dictionary": "dicts/pinyin_main.dict",
  "candidatePolicy": "chinese_default",
  "displayPolicy": "show_query"
}
```

字段规则：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `engineId` | 是 | 必须能在 `EngineRegistry` 找到 |
| `layoutId` | 是 | 当前 Schema 默认布局 |
| `supportsShift` | 是 | 是否允许 Shift/Caps |
| `mapping` | Direct 必填 | 按键映射表 |
| `encoderId` | TableComposing 必填 | 编码器 ID |
| `encoderConfig` | 可选 | 编码器配置文件 |
| `dictionary` | TableComposing 条件必填 | 字典路径；除非 `dictionaryOptional = true`，否则必须声明 |
| `dictionaryOptional` | 可选 | 默认 `false`，缺失字典时是否允许提交 raw/query |
| `fsm` | Transliteration 必填 | FSM 规则文件 |
| `candidatePolicy` | 是 | 候选和控制键策略 |
| `displayPolicy` | 可选 | composing 显示策略 |
| `conversionDictionary` | Transliteration 可选 | 转写后的候选转换词典 |
| `outputScript` | Transliteration 可选 | 显式声明转写输出脚本，缺省使用当前 `OrthogonalState.script` |

注册校验：

- `engineId` 不存在则 Manifest 注册失败。
- `direct` 缺少 `mapping` 则注册失败。
- `table_composing` 缺少 `encoderId` 则注册失败；缺少 `dictionary` 且未声明 `dictionaryOptional = true` 时注册失败。
- `transliteration` 缺少 `fsm` 则注册失败。
- `candidatePolicy` 不存在则注册失败。
- `displayPolicy` 如果声明但 ID 不存在则注册失败；未声明时由引擎族默认值补齐。
- `commitPolicy` 不是合法字段，Direct 提交行为由 `candidatePolicy`、`mapping.fallback` 和引擎默认行为表达。
- 所有资源路径必须在语言包沙箱内，禁止 `../` 路径逃逸。

### 7.1 外部语言包安全沙箱

外部 ZIP 语言包只能提供数据资源，不能执行代码。

沙箱规则：

- 解压目标必须位于应用私有目录，例如 `files/language_packs/{packageId}/`。
- 解压前必须校验 ZIP entry 路径，禁止绝对路径、`../`、空文件名和重复规范化路径。
- 单个文件大小和总解压大小必须受限。
- Manifest 必须先解析并通过基础校验，再加载其他资源。
- Manifest 中的所有资源路径都必须是相对路径，并且规范化后仍位于该语言包目录内。
- 外部语言包不得声明新的 Kotlin class、dex、so 动态库或任意可执行入口。
- 外部语言包只能引用已注册的内置 `engineId`、`encoderId`、`candidatePolicy`、`displayPolicy`。
- 外部语言包不能参与构建期 `method.xml` 生成，只能在 MyBoard 内部语言切换 UI 中出现。
- 删除语言包时，必须先 unregister capabilities，再释放字典和资源缓存，最后删除文件。

插件 APK 属于另一类扩展，不走 ZIP 数据包沙箱。插件 APK 如需提供自定义引擎，必须通过单独插件接口、签名校验和权限边界接入。

### 7.2 错误恢复与降级

错误分为注册期错误和运行期错误。

注册期错误必须阻止能力注册：

- Manifest JSON 结构非法。
- 必填字段缺失。
- `engineId`、`encoderId`、`candidatePolicy` 不存在。
- 资源路径越界。
- 内置 Manifest 引用的资源不存在。

运行期错误不能导致 IME 崩溃：

- 字典文件损坏。
- 外部语言包资源被删除。
- 资源加载超时。
- 字典查询抛出异常。
- session 创建失败。

运行期降级策略：

1. 当前 `SchemaCapability` 资源加载失败时，将该 capability 标记为 temporarily unavailable。
2. `InputPipeline` 请求 `KeyboardContextManager` 回退到同一 `Locale + Script` 下的默认可用 Schema。
3. 如果同一 Script 下没有可用 Schema，则回退到该 Locale 的默认状态。
4. 如果该 Locale 没有任何可用状态，则回退到内置安全状态 `en-US + LATN + LATIN_DIRECT`。
5. 如果安全状态也不可用，显示空键盘或设置入口，不再处理输入。

`TableComposingEngine` 的字典缺失不允许静默产生错误候选。首版策略是 session 创建失败并触发状态回退；后续可通过 Manifest 明确声明 `dictionaryOptional = true`，允许提交 raw/query。

所有错误必须可观测：

- 记录日志。
- 暴露最近一次 capability 加载错误。
- 设置页可显示语言包不可用原因。

## 8. Reset 与生命周期

必须触发 session reset 或 close 的场景：

| 场景 | 行为 |
| --- | --- |
| `Locale` 变化 | close 旧 session，创建新 session，清空 composing |
| `Script` 变化 | close 旧 session，创建新 session，清空 composing |
| `Schema` 变化 | close 旧 session，创建新 session，清空 composing |
| 输入框切换 | reset 当前 session |
| 输入法界面关闭 | reset 当前 session |
| 用户按清空组合 | reset 当前 session |
| 字典更新 | close 当前 session，重新解析资源 |

```kotlin
enum class ResetReason {
    LocaleChanged,
    ScriptChanged,
    SchemaChanged,
    InputStarted,
    InputFinished,
    UserCleared,
    DictionaryUpdated,
    CursorMoved,
    InputConnectionInvalid,
    ResourceFailed
}
```

`ResetReason` 的发送方必须是 Android 平台桥接层（详见 `docs/android-bridge.md`）。`InputPipeline` 只负责接收并执行 reset，不自行判断何时触发。各 reason 的产生来源：

| ResetReason | 产生来源 | 桥接层组件 |
| --- | --- | --- |
| `LocaleChanged` / `ScriptChanged` / `SchemaChanged` | `KeyboardContext.orthogonal` 变化 | `KeyboardContextManager` 状态订阅 |
| `InputStarted` | `MyBoardImeService.onStartInput(attribute, restarting=false)` | `EditorInfoResolver` 解析后触发 |
| `InputFinished` | `MyBoardImeService.onFinishInputView` | 退出输入视图时触发 |
| `UserCleared` | 用户主动清空组合（布局动作） | 布局层 → `InputPipeline` |
| `DictionaryUpdated` | 字典热更新或语言包变更 | `DictionaryRegistry` 通知 |
| `CursorMoved` | `MyBoardImeService.onUpdateSelection` 中光标移出 composing region | `SelectionTracker` |
| `InputConnectionInvalid` | `InputConnection` 为空或调用失败 | `InputConnectionGateway` + `SelectionTracker` |
| `ResourceFailed` | 运行期资源加载失败 | `EngineResourceResolver` |

### 8.1 光标与外部编辑

输入法无法完全控制宿主应用文本。以下情况必须认为 session buffer 与宿主文本可能不一致：

- 用户在宿主应用中移动光标。
- 宿主应用替换选区文本。
- `onUpdateSelection` 表明 selection 或 composing region 与当前 session 不一致。
- `InputConnection` 返回 null 或操作失败。

处理规则：

- 有 composing 时，调用 `finishComposingText()` 并 reset session。
- 清空 `KeyboardContext` 中的 composing/candidates。
- 不尝试根据宿主文本反推引擎 buffer。
- 后续输入从空 session 开始。

### 8.2 内存与性能约束

字典和资源必须受控加载：

- 字典缓存使用 LRU，key 为 `DictionaryKey`。
- LRU 大小按内存预算配置，首版默认最多保留当前语言包常用字典和最近使用字典。
- 大文件加载、解析、索引构建必须在 `Dispatchers.IO`。
- 主线程不得解析词典、FSM 大文件或 mapping 大文件。
- `InputSession.close()` 必须取消其 `CoroutineScope`，释放 buffer、候选和未完成查询。
- 候选列表必须限制数量，首版默认单次查询最多返回 50 条，由 `CandidatePolicy` 再截断展示。
- 文本词典只是首版格式，必须通过 `Dictionary` 接口访问，避免后续二进制词典替换时影响引擎。
- 语言包资源解析后可缓存为不可变对象，多个 session 可共享只读资源。

## 9. 首版实现要求

首版必须实现：

- `InputPipeline`
- `InputEngine`
- `InputSession`
- `EngineRegistry`
- `EngineResourceResolver`
- `DisplayPolicyRegistry`
- session 串行化处理机制
- 字典查询 `querySeq` 防旧结果覆盖机制
- `DirectEngine`
- `TableComposingEngine`
- `TransliterationEngine` 的接口和最小可运行实现
- `identity` Encoder
- `table_mapping` Encoder
- `direct_default` CandidatePolicy
- `chinese_default` CandidatePolicy
- `show_raw`、`show_query`、`show_composing`、`hidden` DisplayPolicy
- 文本字典查询
- mapping JSON 解析
- FSM JSON 解析
- Manifest 注册期错误校验
- 运行期资源失败回退到安全状态

首版可暂缓：

- 第三方插件 APK 自定义引擎。
- 二进制词典。
- 日文汉字转换。
- 用户词频学习。
- 云端候选。

## 10. 测试计划

### 10.1 DirectEngine

- `NORMAL` 层 token `a` 输出 `a`。
- `SHIFTED` 层 token `a` 输出 `A`。
- 未命中 token 按 fallback 处理。
- 不产生 composing 和 candidates。

### 10.2 TableComposingEngine

- `identity` encoder 输入 `n`、`i` 后 query 为 `ni`。
- 双拼 encoder 能根据 JSON 映射生成 query。
- 有候选时空格提交首候选。
- 无候选时回车按策略提交 raw 或 query。
- composing buffer 退格后候选同步刷新。
- 快速输入时旧字典查询结果不能覆盖新 buffer。

### 10.3 TransliterationEngine

- FSM 输入 `k` 后进入中间状态，不立即提交。
- FSM 输入 `k`、`a` 后输出 `か`。
- 无匹配路径时按 fallback 策略处理。
- reset 后 FSM 回到初始状态。

### 10.4 Pipeline

- Schema 切换会 close 旧 session 并创建新 session。
- `EngineResult.CommitText` 只由 Pipeline 执行到 `InputConnection`。
- `EngineResult.UpdateComposing` 会更新 `KeyboardContextManager`。
- 输入框切换会 reset session。
- `CommitText` 会先 `finishComposingText()` 再 `commitText()`。
- `UpdateComposing` 会调用 `setComposingText()`。
- Backspace 在组合态中优先删除 session buffer，不直接删除宿主文本。
- `InputConnection` 为空或失败时 reset session 并清空 context composing。
- 快速输入触发多个字典查询时，旧查询结果不会覆盖新 buffer。

### 10.5 Manifest 校验

- `table_composing` 缺少 `dictionary` 且未声明 `dictionaryOptional = true` 时注册失败。
- `direct` 缺少 `mapping` 注册失败。
- `transliteration` 缺少 `fsm` 注册失败。
- Manifest 引用不存在的 `engineId` 注册失败。
- 资源路径越界注册失败。
- 外部 ZIP 包含 `../` entry 时导入失败。
- 外部语言包声明 dex、so 或 Kotlin class 时导入失败。

### 10.6 错误恢复与性能

- 字典文件损坏时当前 capability 标记不可用，并回退到可用 Schema。
- 外部语言包资源删除后不会导致 IME 崩溃。
- `InputSession.close()` 会取消未完成查询。
- 大词典加载不阻塞主线程。
- 候选数量受 limit 限制。

## 11. 关键原则

- 引擎按输入机制分类，不按语言分类。
- 内置引擎族只有 `direct`、`table_composing`、`transliteration`。
- 语言差异通过 Manifest、mapping、encoder、FSM、dictionary、candidatePolicy 表达。
- `InputEngine` 是工厂，`InputSession` 才持有运行时状态。
- `KeyboardContext` 保存 UI 快照，不保存完整引擎 buffer。
- `TransitionEngine` 负责状态合法性和状态转移，输入引擎不参与。
- `InputPipeline` 是唯一连接引擎结果、`InputConnection` 和 `KeyboardContextManager` 的组件。
- `InputPipeline` 通过 `InputConnectionGateway` 间接访问 `InputConnection`，不直接持有连接对象（见 `docs/android-bridge.md`）。
- `InputPipeline` 必须串行处理输入事件。
- 异步查询必须有版本检查或取消机制，旧结果不得覆盖新状态。
- 所有 `InputConnection` 操作集中在 Pipeline。
- 资源错误必须可恢复，不能导致 IME 崩溃。
- ZIP 语言包只能提供数据资源，不能执行任意代码。
- 插件 APK 自定义引擎属于后续扩展，必须走独立插件接口和权限边界。
