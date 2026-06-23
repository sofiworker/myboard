# 阶段 01：契约对齐与跨层接口定义

> 顺序：01  
> 目标：先修正跨文档与代码入口的契约，定义所有跨层接口，让后续阶段只在一个方向上推进。  
> 依据：`docs/core.md`、`docs/orthogonal-state-management.md`、`docs/layout.md`、`docs/engine.md`、`docs/android-bridge.md`

## 1. 预期目标

本阶段不追求完整功能，只建立稳定基线：

- 工程可编译，可运行 `test` 和 `assembleDebug`。
- 所有跨层依赖的接口和契约类型已定义签名，后续阶段只实现不反复改签名。
- 明确旧结构删除清单，旧结构在本阶段后不再扩展，后续阶段逐步直接删除。
- 所有新增代码包名、接口名、资源路径与当前设计文档一致。

## 2. 实施步骤

### 1.1 建立旧结构删除清单

做什么：

- 扫描现有代码中所有需要删除的旧结构，建立完整删除清单：
  - 旧状态：`KeyboardState`、`KeyboardStateManager`、`ShiftState`（`core/keyboard/KeyboardState.kt`、`core/keyboard/KeyboardStateManager.kt`）。
  - 旧语言管理：`LanguageSwitchManager`（`core/input/LanguageSwitchManager.kt`）。
  - 旧引擎：`InputEngine`、`DirectInputEngine`、`CompositionInputEngine`、`EngineType`、`InputMethodConfig`（`core/input/InputEngine.kt`、`core/input/DirectInputEngine.kt`、`core/input/CompositionInputEngine.kt`）。
  - 旧动作分发：`ActionDispatcher`（`core/keyboard/ActionDispatcher.kt`）——当前直接调 `InputConnection`，是死代码/错误实现。
  - 旧布局模型：`LayoutModels.kt` 中的 `KeyboardLayout`/`KeyData`/`RowData`/`ArrangementData`/`GeometryConfig`（`core/layout/LayoutModels.kt`）。
  - 旧布局计算：`GridCalculator.kt`（`core/layout/GridCalculator.kt`）。
  - 旧布局仓库：`LayoutRepository.kt`（`core/layout/LayoutRepository.kt`）。
  - 旧 Patch：`PatchApplier.kt`（`core/layout/PatchApplier.kt`）。
  - 旧按键绑定：`KeyBindingModels.kt`、`KeyBindingManager.kt`（`core/keybinding/`）。
  - 旧主题：`ThemeColors`/`KeyColors`/`ActionColors`/`BuiltInThemes`（`core/theme/ThemeModels.kt`）。
  - 旧 InputAction：`InputAction.kt`（`core/keyboard/InputAction.kt`）——需要按新设计重写。
  - 旧字典：`TrieDict.kt`、`SuggestionEngine.kt`、`DictionaryImporter.kt`（`core/dictionary/`）。
  - 旧 UI：`ComposeInputView.kt` 中的 `mapKeyToAction` 硬编码映射（`ui/keyboard/ComposeInputView.kt`）。
  - 旧布局文件：`assets/layouts/*.json`（v1 格式）、`assets/layouts/v2/` 子目录（含旧版命名和参考提案）。
  - 旧子类型配置：`assets/subtypes/` 整个目录。
- 标出可以保留的资源（保留数据，路径/命名需调整）：
  - `LayoutParser.stripJsonLineComments`（JSONC 注释剥离逻辑，提取为新工具函数 `JsoncParser.stripComments`）。
  - `assets/layouts/v2/qwerty.jsonc`、`shuangpin.jsonc`、`t9.jsonc`、`candidate.jsonc`：按新模型调整字段后，移到 `assets/layouts/` 下，去掉 v2 目录层级。`proposed_extensible_keyboard.jsonc` 是旧参考提案，直接删除。
  - `assets/dictionary/`（词典数据文件保留）。
  - `assets/emoji/emoji.json`、`assets/emoji/kaomoji.json`（数据文件保留）。
  - `assets/symbols/symbols.json`（数据文件保留）。
  - `SettingsManager.kt`（在现有基础上扩展，保留已实现的持久化逻辑）。
  - `EmojiRepository.kt`/`SymbolRepository.kt`/`KaomojiRepository.kt`（数据保留，后续接入新面板结构）。
  - `ClipboardManager.kt`（逻辑保留，后续接入新面板结构）。
  - `STTProvider.kt`/`LLMProvider.kt`（接口保留，后续接入新 Schema/面板）。
  - `TextExpansionManager.kt`（逻辑保留，后续作为扩展能力接入）。
  - `SettingsScreen.kt`/`ThemeSettingsScreen.kt`（UI 保留，在现有基础上扩展）。

测试：

- 使用 `rg` 生成删除清单，保存到阶段实现 PR 描述。
- 执行 `./gradlew test`，确认当前基线状态。

预期目标：

- 明确哪些文件会在后续阶段被直接删除。
- 明确哪些资源文件可保留复用。
- 不改用户行为，避免第一步引入功能回归。

性能：

- 本步骤不引入运行时逻辑，性能无变化。

### 1.2 定义跨层接口契约（核心）

做什么：

定义所有跨层依赖的 `interface` 和 data class 签名，**只写签名不写业务逻辑**。这些接口是后续阶段实现的目标，也是 stub 占位的前提。

必须定义的接口：

| 接口 | 所属层 | 关键方法签名 | 后续实现阶段 |
| --- | --- | --- | --- |
| `InputPipeline` | 引擎层 | `suspend fun handle(action: InputAction)`、`suspend fun onContextChanged(context: KeyboardContext)`、`suspend fun reset(reason: ResetReason)` | 05 |
| `InputConnectionGateway` | 桥接层 | `fun commitText(text: String): Boolean`、`fun setComposingText(text: String): Boolean`、`fun finishComposingText(): Boolean`、`fun deleteSurroundingText(before: Int, after: Int): Boolean`、`fun setSelection(start: Int, end: Int): Boolean`、`fun performEditorAction(action: Int): Boolean`、`fun getExtractedText(): ExtractedText?`、`fun sendKeyEvent(keyEvent: KeyEvent): Boolean`、`fun finishAndCommit(commit: String): Boolean` | 06 |
| `ThemeResolver` | 主题层 | `fun resolveKeyStyle(styleRef: String): KeyStyle`、`fun resolveFeedbackPolicy(): FeedbackPolicy`、`fun isDark(): Boolean` | 03 |
| `EngineRegistry` | 引擎层 | `fun register(engine: InputEngine)`、`fun get(engineId: String): InputEngine?` | 05 |
| `LayoutRegistry` | 布局层 | `fun register(doc: LayoutDoc, source: LayoutSource): RegisterResult`、`fun get(layoutId: String): LayoutDoc?`、`fun validate(doc: LayoutDoc): List<LayoutIssue>`、`fun findBuiltIn(id: String): LayoutDoc?` | 04 |
| `DictionaryRegistry` | 引擎层 | `fun load(key: DictionaryKey): Dictionary?`、`fun invalidate(key: DictionaryKey)` | 05 |
| `EncoderRegistry` | 引擎层 | `fun register(encoder: Encoder)`、`fun get(encoderId: String): Encoder?` | 05 |
| `CandidatePolicyRegistry` | 引擎层 | `fun register(policy: CandidatePolicy)`、`fun get(policyId: String): CandidatePolicy?` | 05 |
| `DisplayPolicyRegistry` | 引擎层 | `fun register(policy: DisplayPolicy)`、`fun get(policyId: String): DisplayPolicy?` | 05 |
| `FeedbackPlayer` | 桥接层 | `fun playHaptic(token: HapticToken)`、`fun playSound(token: SoundToken)` | 06 |
| `EngineResourceResolver` | 引擎层 | `fun resolve(capability: SchemaCapability, packageId: String): EngineResources` | 05 |
| `EditorInfoResolver` | 桥接层 | `fun resolve(editorInfo: EditorInfo?, currentLocale: LocaleTag): EditorProfile` | 06 |
| `SelectionTracker` | 桥接层 | `fun onSelectionChanged(oldSel: SelectionSnapshot, newSel: SelectionSnapshot, composingActive: Boolean): SelectionDecision` | 06 |
| `HardwareKeyRouter` | 桥接层 | `fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean`、`fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean`、`fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean` | 06 |
| `SubtypeBridge` | 桥接层 | `fun onCurrentSubtypeChanged(subtype: InputMethodSubtype)`、`fun syncOutbound(state: OrthogonalState)`、`fun switchToNext()` | 07 |
| `PermissionGateway` | 桥接层 | `fun isGranted(permission: String): Boolean`、`fun requestMicrophone(callback: (granted: Boolean) -> Unit)`、`fun requestFileImport(callback: (granted: Boolean) -> Unit)` | 07 |
| `LayoutMeasurer` | 布局层 | `fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int): MeasuredLayout`、`fun invalidate(layoutId: String? = null)` | 04 |
| `LayoutHintResolver` | 布局层 | `fun resolve(hint: LayoutHint, currentLayoutId: String): String` | 04 |
| `BindingsEvaluator` | 布局层 | `fun evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean>`（visible, enabled） | 04 |
| `LanguagePackImporter` | 扩展层 | `suspend fun import(zipFile: Uri, registry: OrthogonalRegistry): ImportResult` | 09 |

必须定义的 data class / sealed interface（只签名）：

- `InputAction` sealed interface，覆盖 `PushToken`、`Delete`、`Space`、`Enter`、`SwitchLocale`、`SwitchScript`、`SwitchSchema`、`SwitchLayer`、`RestorePreviousSchema`、`CommitCandidate`、`OpenPanel`、`ClosePanel`、`PageCandidate`、`Noop`。
- `ResetReason` enum（沿用 `engine.md:714`）。
- `EngineResult` sealed interface（沿用 `engine.md:323`）。
- `InputEvent` sealed interface（沿用 `engine.md:307`）。
- `InputSessionState` data class（沿用 `engine.md:347`）。
- `Candidate`、`DictionaryKey`、`LayoutIssue`、`RegisterResult`、`LayoutSource`、`ImportResult`。
- `KeyStyle`、`FeedbackPolicy`、`HapticToken`、`SoundToken`（主题/反馈契约）。
- `EditorProfile`、`LayoutHint`、`EnterAction`（桥接层 EditorInfo 契约）。
- `SelectionSnapshot`、`SelectionDecision` sealed interface（桥接层光标追踪契约）。
- `OrthogonalState`、`KeyboardContext`、`LayoutLayer`、`LocaleTag`、`Script`、`Schema`（状态层契约）。
- `LanguageManifest`、`SchemaCapability`、`LocaleCapability`、`ScriptCapability`、`CapabilityId`（Manifest 契约）。
- `TransitionEvent`、`TransitionResult` sealed interface（状态转移契约）。
- `PanelType` enum（面板类型契约）。
- `LayoutDoc`、`LayoutContainer` sealed class（5 种）、`KeyDef`、`ContentSpec`、`VariantPatch`、`Dimension` sealed class（6 种）、`ActionDef`、`ActionMap`、`GestureType`、`HitShape` sealed class、`Region`、`RegionRole`、`LayoutMeta`、`LayoutEnv`、`Bindings`、`ScrollSpec`、`BoxSpacing`、`Orientation`、`Gravity`、`HintPosition`（布局模型契约）。
- `MeasuredLayout`、`MeasuredRegion`、`MeasuredKey`（布局测量输出契约）。
- `Encoder` interface、`EncodingState`（编码器契约）。
- `Dictionary` interface（字典契约）。
- `CandidatePolicy` interface、`PolicyAction` sealed interface（候选策略契约）。
- `DisplayPolicy` interface（显示策略契约）。
- `InputEngine` interface、`InputSession` interface、`EngineContext`、`EngineResources`（引擎契约）。
- `TextRef` sealed class（`Raw`/`StringResource`/`LocalizedMap`）。
- `LayoutDoc.extends` 和 `patches` 签名（继承与 patch 契约）。
- `CandidateSlotNode` 或等效候选位表达。

测试：

- 编译通过：所有接口和 data class 可被引用。
- 单元测试：`InputAction` 各子类型可构造，`EngineResult` 各子类型可构造，`InputEvent` 各子类型可构造。
- 不出现循环依赖：接口定义放在各层 `contract` 子包，不依赖具体实现。

预期目标：

- 阶段 02 的 `OrthogonalRegistry` 可注入 stub Registry 接口。
- 阶段 04 的 `ActionDispatcher` 可依赖 `InputPipeline` 接口。
- 阶段 05 的 `InputPipeline` 可注入 fake `InputConnectionGateway`。
- 阶段 04 的渲染器可依赖 `ThemeResolver` 接口。
- 阶段 04 的 `LayoutMeasurer`、`LayoutHintResolver`、`BindingsEvaluator` 接口已就绪。
- 阶段 06 的 `EditorInfoResolver`、`SelectionTracker`、`HardwareKeyRouter` 接口已就绪。
- 阶段 07 的 `SubtypeBridge`、`PermissionGateway` 接口已就绪。
- 阶段 09 的 `LanguagePackImporter` 接口已就绪。

性能：

- 接口定义无运行时开销。
- data class 保持不可变，避免后续实现期反复改签名导致大量返工。

### 1.3 定义 stub Registry 与 fake Gateway

做什么：

为阶段 02 能编译可测，提供 stub 实现：

- `StubEngineRegistry`：`get()` 永远返回 `null` 或预注册的 `direct`/`table_composing`/`transliteration` 占位工厂（不真实创建 session）。
- `StubLayoutRegistry`：`get()` 返回 `null`；`validate()` 只校验 JSON 结构；`findBuiltIn()` 返回 `null`。
- `StubDictionaryRegistry`：`load()` 返回空字典；`invalidate()` 空操作。
- `StubEncoderRegistry`：`get()` 返回 `null`。
- `StubCandidatePolicyRegistry`：`get()` 返回 `null`。
- `StubDisplayPolicyRegistry`：`get()` 返回 `null`。
- `FakeInputConnectionGateway`：记录调用，可配置返回值，用于阶段 05 测试。
- `FakeFeedbackPlayer`：记录调用的 token，用于阶段 04/05 测试。
- `StubEngineResourceResolver`：返回默认空 `EngineResources`。

stub 规则：

- 只做结构校验、跳过存在性校验（engineId/layoutId 是否真实存在）。
- 明确标注 `// STUB - replaced in stage XX`，避免后续误用。
- stub 不进入 release 构建路径，只在测试和过渡期使用。

测试：

- stub 注入 `OrthogonalRegistry` 后可编译。
- `FakeInputConnectionGateway` 可配置 `commitText` 返回 `false` 触发 reset 测试。
- `FakeFeedbackPlayer` 记录 `playHaptic`/`playSound` 调用。

预期目标：

- 阶段 02 的 Manifest 注册可先跳过 engineId/layoutId 存在性校验，只校验 JSON 结构和必填字段。
- 阶段 05 的 Pipeline 测试不依赖真实 Android `InputConnection`。
- 阶段 04 的渲染和手势测试不依赖真实 `FeedbackPlayer`。

性能：

- stub 无开销或开销可忽略。

### 1.4 对齐布局动作集合

做什么：

- 将 `layout.md` 补充的 `OPEN_PANEL`、`CLOSE_PANEL`、`PAGE_NEXT`、`PAGE_PREV` 纳入 `InputAction`（在 1.2 已定义）。
- 明确未知 action 的处理：内置和用户布局注册期失败，运行期不静默吞错。
- 删除旧 `SWITCH_MODE` 动作定义。
- 确认 `layout.md:769-780` 的核心动作与 `orthogonal-state-management.md:769-780` 完全一致。

测试：

- 单元测试：合法动作解析成功，未知动作注册失败。

预期目标：

- 布局动作、状态转移、引擎事件之间有稳定中间层。

性能：

- `ActionDef -> InputAction` 转换不做 IO。
- 单次动作转换目标小于 1 ms。

### 1.5 对齐布局数据模型契约

做什么：

- 将 `LayoutDoc.extends`、`patches` 纳入正式 data class 签名（实现推迟到阶段 04）。
- 为候选位补齐正式节点类型 `CandidateSlotNode` 或明确候选位用 `KeyDef` 的特殊 role 表达。
- 将 `label: String?` 的 i18n 约定收敛为可校验 `TextRef` sealed class 签名：
  - `TextRef.Raw` 仅允许键面符号、单字符 token、调试文案。
  - `TextRef.StringResource` 引用 `@string/...`。
  - `TextRef.LocalizedMap` 用于语言包内部显示名。

测试：

- data class 签名编译通过。
- `TextRef` 各子类型可构造。

预期目标：

- 阶段 04 实现布局模型时签名已就绪，只填实现。

性能：

- 无运行时影响。

### 1.6 建立包结构

做什么：

- 规划新包：
  - `core.contract`：跨层接口和契约 data class（本阶段产物）。
  - `core.state`：正交状态、Context、Transition。
  - `core.manifest`：语言包模型、解析、注册。
  - `core.theme`：主题 token、`ThemeResolver`、反馈参数。
  - `core.engine`：引擎、Pipeline、字典接口。
  - `core.layout`：布局模型、测量、注册。
  - `core.androidbridge` 或 `ime.bridge`：EditorInfo、Selection、Gateway、Subtype、Permission、Feedback。
  - `core.extension`：剪贴板、表情、符号、LLM 面板。
- 旧代码（`core.keyboard`、`core.input`、`core.layout`、`core.theme`、`core.dictionary`、`core.keybinding`）在后续阶段实现新代码时直接删除，不做迁移。

测试：

- 编译通过。
- 不出现循环依赖：layout 不依赖 engine 实现，engine 不依赖 layout 模型，bridge 可以依赖 state 和 pipeline 接口。

预期目标：

- 后续阶段可以增量实现，旧代码按阶段逐步直接删除。

性能：

- 无运行时影响。

## 3. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "inputConnectionProvider|currentInputConnection|mapKeyToAction|LanguageSwitchManager|KeyboardStateManager|SWITCH_MODE" app/src/main/java
```

验收标准：

- 工程仍可生成 APK。
- 所有跨层接口已定义签名且可编译（20 个接口）。
- stub Registry（7 个）和 fake 组件（`FakeInputConnectionGateway`、`FakeFeedbackPlayer`）已提供，阶段 02 可注入。
- `InputAction` 覆盖所有当前设计动作，`SWITCH_MODE` 已删除。
- 布局模型契约（`extends`、`patches`、`CandidateSlotNode`、`TextRef`）签名已就绪。
- 桥接层契约（`EditorProfile`、`SelectionSnapshot`、`SelectionDecision`）签名已就绪。
- 旧结构删除清单已建立，包含可保留资源列表。
