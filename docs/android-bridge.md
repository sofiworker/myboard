# MyBoard Android 平台桥接层详细设计

> 版本：v1.0  
> 状态：Draft  
> 日期：2026-06-16  
> 定位：本文档是 MyBoard Android 平台桥接层的唯一实现标准。  
> 依据：`docs/core.md` 的「Android 系统桥接」概念，`docs/orthogonal-state-management.md` 的 `KeyboardContext`、`ResetReason`，`docs/engine.md` 的 `InputPipeline` 与 `EngineResult`。  
> 原则：与当前 `MyBoardImeService` 实现冲突时，允许破坏性重构。`InputConnection` 的所有调用、`EditorInfo` 的所有解析、`InputMethodService` 的所有生命周期钩子都必须收敛到本层。

## 1. 目标与职责边界

Android 平台桥接层只负责一件事：把 Android 系统的 `InputMethodService` 生命周期、`EditorInfo`、`InputConnection`、硬件按键、系统 Subtype 等「外部输入源/外部宿主」翻译成 MyBoard 内部的 `InputAction` 流、`ResetReason` 信号和 `EngineResult` 执行动作；同时把内部状态、候选、`EngineResult` 翻译回对 `InputConnection` 和系统 UI 的调用。

它不负责：

- 判断 `Locale + Script + Schema` 是否合法（交给 `TransitionEngine`）。
- 维护编码 buffer、组合串、候选（交给 `InputSession`）。
- 决定按键如何映射到 token（交给引擎层与布局层）。
- 渲染键盘视觉（交给布局层，但桥接层提供 `IME View` 容器和主题环境）。

它负责：

- 收敛所有 `InputConnection` 调用：`commitText`、`setComposingText`、`finishComposingText`、`deleteSurroundingText`、`setSelection`、`getExtractedText`、`performEditorAction`、`sendKeyEvent`。
- 解析 `EditorInfo.inputType` / `EditorInfo.imeOptions` / `EditorInfo.imeActionLabel`，产出布局/Schema 建议、回车键标签、是否禁用候选。
- 监听宿主光标与文本变化（`onUpdateSelection`、`onUpdateExtractingText`），在组合态不可信时触发 `InputPipeline.reset`。
- 处理硬件键盘事件（`onKeyDown` / `onKeyLongPress` / `onKeyUp`），转成 `InputEvent`。
- 桥接系统 `InputMethodSubtype` 流入流出。
- 集中申请运行时权限（语音麦克风）。
- 提供横屏 Extracted Text 全屏编辑的稳定支持。

## 2. 分层与组件总览

```text
Android System
  -> InputMethodService (MyBoardImeService)
       |- EditorInfoResolver        解析 inputType/imeOptions
       |- SelectionTracker          监听 onUpdateSelection
       |- HardwareKeyRouter         物理键盘事件路由
       |- InputConnectionGateway    唯一 InputConnection 调用出口
       |- SubtypeBridge             系统子类型桥接
       |- PermissionGateway         集中权限申请
  -> InputAction / ResetReason / EngineResult
  -> InputPipeline (引擎层)
  -> KeyboardContextManager (状态层)
```

| 组件 | 职责 | 是否新建 |
| --- | --- | --- |
| `MyBoardImeService` | `InputMethodService` 子类，薄壳，只做钩子转发 | 重构现有 |
| `EditorInfoResolver` | `EditorInfo` → `EditorProfile`（布局建议、回车标签、候选开关） | 新建 |
| `SelectionTracker` | 缓存上次 selection/composing region，判定是否需要 reset | 新建 |
| `HardwareKeyRouter` | 物理按键 `KeyEvent` → `InputEvent` | 新建 |
| `InputConnectionGateway` | 所有 `InputConnection` 调用的唯一出口与失败处理 | 新建 |
| `SubtypeBridge` | 解析 subtype `extraValue`、同步系统状态栏 | 新建 |
| `PermissionGateway` | 统一权限申请接口，UI 只表达「请求能力」 | 新建 |

**硬性要求**：任何非桥接层组件（引擎、状态、布局、UI）禁止直接持有或调用 `InputConnection`。`InputPipeline` 通过 `InputConnectionGateway` 间接访问，不直接拿 `currentInputConnection`。

## 3. MyBoardImeService 重构原则

当前 `MyBoardImeService`（`app/src/main/java/xyz/xiao6/myboard/ime/MyBoardImeService.kt`）承担了过多职责：直接 `ic.commitText` / `ic.deleteSurroundingText`（:251, :255, :296, :304, :315, :285）、自己解析 `inputType` 只分 3 档（:327-333）、自己持有 `engines` map 和 `languageSwitchManager`。重构后它必须是**薄壳**。

必须重写的生命周期钩子（当前缺失或职责错位）：

| 钩子 | 当前状态 | 重构后职责 |
| --- | --- | --- |
| `onCreate` | :74 初始化组件 | 创建桥接层组件、`InputPipeline`、`KeyboardContextManager`，注册 Manifest |
| `onCreateInputView` | :145 硬编码颜色、内嵌逻辑 | 仅创建 `IME ComposeView` 容器并套主题环境，不写输入逻辑 |
| `onStartInput(attribute, restarting)` | :325 只改 arrangement | 透传 `EditorInfo` 给 `EditorInfoResolver`，触发 `reset(InputStarted)` |
| `onStartInputView(finishing)` | 未重写 | 每次输入视图出现时重新绑定 `InputConnection`、刷新 `EditorProfile` |
| `onFinishInputView(finishing)` | :336 只 clearComposing | 触发 `reset(InputFinished)`，释放当前 `InputConnection` 绑定 |
| `onUpdateSelection` | **未重写（重大缺口）** | 交给 `SelectionTracker` 判定组合态是否可信 |
| `onUpdateExtractingText` | 未重写 | 横屏全屏编辑时同步宿主文本 |
| `onKeyDown / onKeyLongPress / onKeyUp` | **未重写（缺口）** | 交给 `HardwareKeyRouter` |
| `onConfigureWindow` | 未重写 | 配置 IME window 软引用、避免泄漏 |
| `onCreateCandidatesView` | 未重写 | 首版返回 `null`（候选内嵌在 input view） |
| `onDestroy` | :341 cancel scope | 取消所有协程、关闭 pipeline、释放资源 |

禁止事项：

- 禁止 `MyBoardImeService` 直接 `ic.commitText` / `ic.deleteSurroundingText`。
- 禁止 `MyBoardImeService` 直接读取 `attribute.inputType` 后写 `arrangement`（已废弃字段）。
- 禁止 `MyBoardImeService` 持有 `engines: Map<String, InputEngine>`，引擎由 `EngineRegistry` 管理。
- 禁止 `MyBoardImeService` 维护 `currentLayout` / `currentEngine`，这些由状态层和引擎层派生。

### 3.1 IME ComposeView 容器与主题

当前 `onCreateInputView` 内部硬编码了 `Color(0xFFF1F3F4)`、`Color(0xFF5F6368)` 等颜色（:176, :195, :205, …），且未套 `MaterialTheme`，导致 `ThemeColors` 模型无法作用到键盘。重构要求：

- `onCreateInputView` 只返回一个套好主题的 `ComposeView`。
- Compose 根节点必须套 `MyBoardTheme`（消费 `ThemeResolver` 解析的 token），并提供 `darkColorScheme` / `lightColorScheme` 切换。
- 夜间模式由 `isSystemInDarkTheme()` 驱动，不写死。
- Dynamic Color（`dynamicColorScheme`）作为后续可选增强，首版可基于静态主题 token。
- 所有键盘按键、工具栏、候选栏颜色只读主题 token，不允许出现 `Color(0xFFxxxxxx)` 字面量。
- `decorView` 的 `setViewTreeLifecycleOwner` / `setViewTreeSavedStateRegistryOwner`（:235-238）保留。

## 4. EditorInfoResolver

`EditorInfo` 是宿主编辑器对输入法的约束声明。当前完全未解析 `imeOptions`、未识别密码/邮件/URL 变体（`MyBoardImeService.kt:327` 只 `inputType and 0xF`）。必须新建 `EditorInfoResolver`。

```kotlin
data class EditorProfile(
    val layoutHint: LayoutHint,
    val enterAction: EnterAction,
    val enterLabel: String?,
    val candidateDisabled: Boolean,
    val composingDisabled: Boolean,
    val rawInputType: Int,
    val rawImeOptions: Int
)

enum class LayoutHint { ALPHA, NUMBER, PHONE, DATETIME, URL, EMAIL, PASSWORD }
enum class EnterAction { UNSPECIFIED, DONE, GO, NEXT, PREVIOUS, SEARCH, SEND, NONE }

class EditorInfoResolver {
    fun resolve(editorInfo: EditorInfo?, currentLocale: LocaleTag): EditorProfile
}
```

解析规则（必须覆盖）：

| 类别 | inputType 判定 | 对 `OrthogonalState` / 布局的影响 |
| --- | --- | --- |
| 数字 | `TYPE_CLASS_NUMBER` | 强制 `LayoutHint.NUMBER`，候选关闭 |
| 电话 | `TYPE_CLASS_PHONE` | 强制 `LayoutHint.PHONE`，候选关闭 |
| 日期时间 | `TYPE_CLASS_DATETIME` | 强制 `LayoutHint.DATETIME`，候选关闭 |
| 文本-密码 | `TYPE_TEXT_VARIATION_PASSWORD` / `VISIBLE_PASSWORD` / `WEB_PASSWORD` | `LayoutHint.ALPHA`，`candidateDisabled=true`，`composingDisabled=true` |
| 文本-邮件 | `TYPE_TEXT_VARIATION_EMAIL_ADDRESS` | `LayoutHint.EMAIL`，建议切 `LATIN_DIRECT` |
| 文本-URL | `TYPE_TEXT_VARIATION_URI` | `LayoutHint.URL`，建议切 `LATIN_DIRECT` |
| 普通文本 | `TYPE_CLASS_TEXT` 其余 | `LayoutHint.ALPHA`，正常候选 |

回车键规则：

- `enterAction` 来自 `EditorInfo.imeOptions and EditorInfo.IME_MASK_ACTION`，不能写死 `IME_ACTION_DONE`（当前 `:285` 写死，必须修正）。
- `enterLabel` 来自 `EditorInfo.actionLabel`，非空时回车键显示该文本。
- `IME_ACTION_NONE` 时回车键不触发 editor action，而是产生换行或被 `CandidatePolicy` 解释。

约束：

- `EditorProfile` 只是「建议」，最终 `OrthogonalState` 仍由 `KeyboardContextManager` 经 `TransitionEngine` 决定。例如邮件框建议 `LATIN_DIRECT`，但如果用户当前 Locale 不提供该 Schema，状态层按降级规则处理。
- `EditorInfoResolver` 不直接改 `KeyboardContext`，它把 `EditorProfile` 交给 `MyBoardImeService`，由后者调用 `KeyboardContextManager.applyEditorProfile()`（新增方法）。
- 数字/电话/密码框强制 `candidateDisabled` 时，候选栏不显示，`InputPipeline` 收到后跳过字典查询。

## 5. SelectionTracker 与 onUpdateSelection

**这是当前最大的功能缺口**：全工程 0 处重写 `onUpdateSelection`。没有它，宿主光标移动、宿主外部删字、宿主替换选区都会导致 `InputSession` buffer 与宿主文本错位，拼音 composing 区间错乱。

`onUpdateSelection` 必须重写，并把判定逻辑收敛到 `SelectionTracker`：

```kotlin
class SelectionTracker {
    fun onSelectionChanged(
        oldSel: SelectionSnapshot,
        newSel: SelectionSnapshot,
        composingActive: Boolean
    ): SelectionDecision
}

data class SelectionSnapshot(
    val oldSelStart: Int, val oldSelEnd: Int,
    val newSelStart: Int, val newSelEnd: Int,
    val candidatesStart: Int, val candidatesEnd: Int
)

sealed interface SelectionDecision {
    data object Trusted : SelectionDecision          // 光标在 composing region 内正常移动
    data class MustReset(val reason: ResetReason) : SelectionDecision
}
```

判定规则（必须实现）：

| 场景 | 判定 | 动作 |
| --- | --- | --- |
| composing 激活，光标仍在 composing region 内 | `Trusted` | 不 reset |
| composing 激活，光标移出 composing region | `MustReset(CursorMoved)` | `finishComposingText()` + `reset(CursorMoved)` |
| composing 激活，`candidatesStart/End` 被清空且 selection 突变 | `MustReset(InputConnectionInvalid)` | reset，清空 `KeyboardContext` composing |
| 无 composing，仅 selection 变化 | `Trusted` | 不影响引擎（无 buffer 可错位） |
| `onUpdateSelection` 在 `onStartInput` 之前/之后成对触发 | `Trusted` | 忽略陈旧信号 |

关键原则：

- 不尝试根据宿主文本反推引擎 buffer（`engine.md:741` 一致）。
- reset 后 `KeyboardContext` 的 composing/candidates 必须清空。
- `SelectionTracker` 只在桥接层运行，不触碰 `InputConnection`；reset 由 `InputPipeline` 执行。
- `onUpdateSelection` 必须做防抖：连续多次触发只产生一次 reset 决策。

## 6. InputConnectionGateway

当前 `InputConnection` 调用分散在 `MyBoardImeService.kt`（:251, :255, :296, :304, :315）和 `ActionDispatcher.kt`（:36, :42, :78, :85, :97），且 `ActionDispatcher` 与 Service 是两套并行逻辑（`ActionDispatcher` 实际未用于真实输入路径，属死代码）。必须收敛为单一出口。

```kotlin
class InputConnectionGateway(
    private val provider: () -> InputConnection?
) {
    fun commitText(text: String): Boolean
    fun setComposingText(text: String): Boolean
    fun finishComposingText(): Boolean
    fun deleteSurroundingText(before: Int, after: Int): Boolean
    fun setSelection(start: Int, end: Int): Boolean
    fun getExtractedText(): ExtractedText?
    fun performEditorAction(action: Int): Boolean
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean
    fun finishAndCommit(commit: String): Boolean  // finishComposingText + commitText 组合
}
```

规则：

- 每个 `EngineResult` 到 `InputConnection` 的映射（`engine.md:578-587`）必须通过 `InputConnectionGateway` 执行，`InputPipeline` 不直接调用 `InputConnection`。
- 任何调用返回 `false` 或 `InputConnection` 为 `null` 时，必须触发 `InputPipeline.reset(InputConnectionInvalid)` 并清空 `KeyboardContext` composing（`engine.md:593`）。
- `setComposingText` 失败时不能静默吞掉，必须 reset。
- `ActionDispatcher` 中的 `moveCursor`（:76-86）逻辑迁移到 `InputConnectionGateway.setSelection`，`ActionDispatcher` 本身重构后删除或并入 `InputPipeline`。
- `MyBoardImeService` 不再直接持有 `InputConnection` 引用，每次操作都通过 gateway 拿当前 connection。

`InputPipeline` 构造改为：

```kotlin
class InputPipeline(
    private val contextManager: KeyboardContextManager,
    private val engineRegistry: EngineRegistry,
    private val resourceResolver: EngineResourceResolver,
    private val gateway: InputConnectionGateway   // 替代原来的 () -> InputConnection?
)
```

## 7. HardwareKeyRouter（物理键盘）

当前全工程 0 处硬件键盘处理，平板/DeX/外接键盘无法输入。必须新建 `HardwareKeyRouter`。

```kotlin
class HardwareKeyRouter(
    private val pipeline: InputPipeline
) {
    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean
    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean
    fun onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean
}
```

映射规则：

| 物理键 | 行为 |
| --- | --- |
| `KEYCODE_A`..`KEYCODE_Z` | 根据 Shift 状态转 `InputEvent.PushToken` |
| `KEYCODE_0`..`KEYCODE_9` | 转 `PushToken` 或 `SelectCandidate`（由 `CandidatePolicy` 决定） |
| `KEYCODE_SPACE` | 转 `InputEvent.Space` |
| `KEYCODE_ENTER` | 转 `InputEvent.Enter` |
| `KEYCODE_DEL` | 组合态激活时转 `Backspace`，否则走 gateway `deleteSurroundingText` |
| `KEYCODE_SHIFT_LEFT/RIGHT` | 长按 → Caps Lock；短按 → 切 `LayoutLayer` |
| `KEYCODE_SYM` / `KEYCODE_LANGUAGE_SWITCH` | 转 `SWITCH_LOCALE` / `SWITCH_SCRIPT` |
| 方向键 | 光标移动（经 gateway `setSelection`） |

约束：

- 物理键盘事件必须先经过 `InputSession.handle()`，不能跳过引擎直接 commit。
- 硬件 Shift 与软键盘 `LayoutLayer` 必须共享同一状态源（`KeyboardContext.layer`），不能维护两份。
- `MyBoardImeService.onKeyDown` 返回 `true` 表示已消费，避免事件穿透到宿主。
- 物理键盘与触摸键盘共存时，以最后一次输入为准。

## 8. SubtypeBridge

`orthogonal-state-management.md` 第 7 节定义了 Subtype 生成规则和 extraValue 格式，本节补充运行时桥接实现。当前 `method.xml` 只有 1 个泛 subtype（`app/src/main/res/xml/method.xml:1-6`），无 `imeSubtypeLocale`、无 `extraValue`，系统语言栏看不到中/英子类型。

```kotlin
class SubtypeBridge(
    private val imm: InputMethodManager,
    private val contextManager: KeyboardContextManager
) {
    fun onCurrentSubtypeChanged(subtype: InputMethodSubtype)
    fun syncOutbound(state: OrthogonalState)
    fun switchToNext()
}
```

流入（系统 → MyBoard）规则（`orthogonal-state-management.md:694-701` 的实现）：

1. 解析 `subtype.extraValue` 中的 `script` 与 `schema`。
2. 结合 `subtype.locale` 构造目标 `OrthogonalState`。
3. 交给 `KeyboardContextManager.switchLocale/switchScript/switchSchema` 校验。
4. 非法时回退该 Locale 默认状态。

流出（MyBoard → 系统）规则：

1. 状态层变化后，查找匹配的 subtype。
2. 找到时调 `imm.setInputMethod(token, subtypeId)` 同步系统语言栏。
3. 找不到时只更新内部状态，不阻断。

### 8.1 method.xml 构建期生成

当前 `method.xml` 是手写静态文件，且不符合 `orthogonal-state-management.md:656-690` 的生成要求。必须新建 Gradle 任务 `generateMyBoardSubtypes`：

- 输入：`app/src/main/assets/languages/**/language.manifest.json`。
- 输出：`app/build/generated/res/myboardSubtype/xml/method.xml`。
- 将 generated res 目录加入 Android source set（`android.sourceSets.main.res.srcDirs`）。
- 删除手写的 `app/src/main/res/xml/method.xml`，改由任务生成。
- 排序稳定：`locale -> script -> schema -> packageId`。
- `subtype.labelKey` 在 `strings.xml` 找不到对应字符串时构建失败。
- 重复 `locale + script + schema` 时构建失败。
- 外部下载语言包不参与生成。

任务实现要点：

- 用 Gradle 自定义任务（Kotlin DSL 或 Groovy），在 `preBuild` 之前执行。
- 解析 Manifest JSON，过滤 `subtype` 字段非空的 Schema。
- 生成 `<input-method>` 根节点，包含 `supportsSwitchingToNextInputMethod="true"`、`isV5="true"`。
- 每个 subtype 写入 `imeSubtypeLocale`、`imeSubtypeMode="keyboard"`、`imeSubtypeExtraValue="script=X,schema=Y"`、`android:label`。

## 9. PermissionGateway

当前 `AndroidManifest.xml` 只有 `VIBRATE`（:5），全工程 0 处 `RECORD_AUDIO` / `requestPermission`，但 UI 有语音按钮（`MyBoardImeService.kt:207`），导致语音功能无法运行。必须集中封装。

```kotlin
class PermissionGateway(
    private val service: MyBoardImeService
) {
    fun isGranted(permission: String): Boolean
    fun requestMicrophone(callback: (granted: Boolean) -> Unit)
    fun requestFileImport(callback: (granted: Boolean) -> Unit)
}
```

`InputMethodService` 不是一个标准 `Activity`，无法直接用 `ActivityCompat.requestPermissions`，因此权限申请路径必须特殊处理：

| 权限 | 申请方式 |
| --- | --- |
| `RECORD_AUDIO` | 切到 `VOICE` Schema 时检查；未授权则启动一个透明 `Activity` 申请；结果回调驱动 Schema 切换 |
| 文件导入（词库） | 使用 `ACTION_OPEN_DOCUMENT` 系统文件选择器，不需要存储权限 |
| `VIBRATE` | 普通权限，Manifest 声明即可 |

规则：

- UI 只表达「请求能力」（如点击语音按钮），不直接判断 `checkSelfPermission`。
- 权限被拒时，`VOICE` Schema 不进入，恢复 `previousRegularState`（`orthogonal-state-management.md:394-403`）。
- 权限申请必须按 Android 版本分支（`Build.VERSION.SDK_INT`），封装在 `PermissionGateway` 内。
- `AndroidManifest.xml` 必须声明 `RECORD_AUDIO`（当前缺失，需补）。
- 不同 Android 版本的处理集中封装，禁止散落权限判断。

## 10. 横屏 Extracted Text

当前未重写 `onExtractText` / `onUpdateExtractingVisibility` / `onStartExtractingText`，横屏全屏编辑未支持。必须补齐：

- `onUpdateExtractingVisibility(editorInfo)`：根据 `EditorInfo` 决定是否显示全屏提取 UI。
- `onStartExtractingText(editorInfo)`：初始化提取文本状态。
- `onExtractingInputShown`：提取 UI 显示时同步状态。
- 全屏模式下候选栏仍由 MyBoard 内嵌渲染（首版不依赖系统 extracted view 的候选）。

约束：

- 全屏模式切换不触发 `InputPipeline.reset`，但 `SelectionTracker` 必须感知提取状态，避免误判 reset。
- 旋转屏幕（配置变化）导致 input view 重建时，`InputSession` 不重建，只重建 ComposeView。

## 11. 配置变化与生命周期边界

当前 `AndroidManifest.xml` Service 块（:34-45）无 `android:configChanges`，且全工程无 `onConfigurationChanged`。必须明确：

- 键盘 input view 的重建由 IME 框架自动处理，`MyBoardImeService` 不需要在 manifest 声明 `configChanges`。
- 主题、夜间模式、语言、屏幕方向变化通过 `isSystemInDarkTheme()`、`LocalConfiguration` 在 Compose 内响应，不依赖 `onConfigurationChanged`。
- 输入框切换（`onStartInput` restarting=true）触发 `reset(InputStarted)`，不重建 session。
- Locale/Script/Schema 变化触发 close + 新建 session（`engine.md:704-707`）。
- `InputSession` 的生命周期与 `OrthogonalState` 绑定，不与 Activity 生命周期绑定。

生命周期状态映射：

| IME 事件 | `KeyboardContextManager` / `InputPipeline` 动作 |
| --- | --- |
| `onCreate` | 创建所有组件、注册 Manifest、加载内置语言包 |
| `onCreateInputView` | 建 ComposeView、套主题、订阅 `KeyboardContext` |
| `onStartInput(attribute, false)` | `EditorInfoResolver.resolve` → `applyEditorProfile` → `reset(InputStarted)` |
| `onStartInput(attribute, true)` | 仅刷新 `EditorProfile`，不 reset（同一输入框重启） |
| `onStartInputView` | 绑定 `InputConnectionGateway`、刷新候选显示 |
| `onUpdateSelection` | `SelectionTracker` 判定 → 可能 `reset(CursorMoved)` |
| `onFinishInputView` | `reset(InputFinished)`、解绑 gateway |
| `onDestroy` | 取消 scope、关闭 pipeline、释放资源 |

## 12. 候选栏渲染归属

当前存在命名/包不一致问题：`MyBoardImeService.kt:48` import `xyz.xiao6.myboard.ui.candidate.CandidateBar`，但实际文件是 `ComposeCandidateBar.kt`（编译性风险）。重构要求：

- 候选栏首版内嵌在 input view 内（不使用系统 `onCreateCandidatesView`），`onCreateCandidatesView` 返回 `null`。
- 候选栏组件统一命名，修复 import 不一致。
- 候选栏只读 `KeyboardContext.candidates` 快照，不直接查字典、不保存 buffer（`core.md:90`）。
- 候选选择发出 `InputAction.CommitCandidate(index)`，由 `InputPipeline` 转 `InputEvent.SelectCandidate`，不直接 `ic.commitText`（当前 `:161-167` 直接调 engine 再 `clearComposing`，需改为走 Pipeline）。
- 后续若需系统级候选（如某些输入法管理器场景），再启用 `onCreateCandidatesView` 路径，两条路径互斥。

## 13. 首版实现要求

首版必须实现：

- `MyBoardImeService` 薄壳重构，移除所有直接 `InputConnection` 调用。
- `EditorInfoResolver`，覆盖数字/电话/日期/密码/邮件/URL/普通文本。
- `SelectionTracker` 与 `onUpdateSelection` 重写。
- `InputConnectionGateway`，收敛所有 `InputConnection` 调用。
- `HardwareKeyRouter` 基础版（字母/数字/空格/回车/退格/Shift/语言切换）。
- `SubtypeBridge` 流入流出。
- `generateMyBoardSubtypes` Gradle 任务，替换手写 `method.xml`。
- `PermissionGateway`，补 `RECORD_AUDIO` 声明。
- `onCreateInputView` 套主题、移除硬编码颜色。
- 横屏 Extracted Text 基础支持。
- 修复候选栏命名/import 不一致。
- 删除或并入 `ActionDispatcher`（消除死代码）。

首版可暂缓：

- Dynamic Color 主题（先用静态 token）。
- `onCreateCandidatesView` 系统候选路径。
- 复杂手势（滑动输入）。
- Inline suggestions（`Autofill` 集成）。

## 14. 测试计划

### 14.1 EditorInfoResolver

- 密码框（`TYPE_TEXT_VARIATION_PASSWORD`）→ `candidateDisabled=true`、`composingDisabled=true`。
- 邮件框 → `LayoutHint.EMAIL`、建议 `LATIN_DIRECT`。
- 数字框 → `LayoutHint.NUMBER`、候选关闭。
- `IME_ACTION_SEARCH` → `enterAction=SEARCH`、回车键不写死 DONE。
- `actionLabel` 非空 → `enterLabel` 透传。

### 14.2 SelectionTracker

- composing 激活，光标在 region 内 → `Trusted`，不 reset。
- composing 激活，光标移出 region → `MustReset(CursorMoved)`。
- 无 composing，selection 变化 → `Trusted`。
- 连续多次 `onUpdateSelection` 只触发一次 reset。

### 14.3 InputConnectionGateway

- `commitText` 返回 `false` → 触发 `reset(InputConnectionInvalid)`。
- `setComposingText` 失败 → reset 并清空 context composing。
- `InputConnection` 为 null → 所有调用安全返回 `false`。

### 14.4 SubtypeBridge 与构建

- `extraValue="script=HANI,schema=PINYIN"` + `locale=zh-CN` → 目标 `OrthogonalState` 正确。
- 缺失字段 → 回退默认状态。
- `generateMyBoardSubtypes` 任务生成多 subtype，顺序稳定。
- `labelKey` 缺字符串 → 构建失败。
- 重复 subtype → 构建失败。

### 14.5 权限

- `RECORD_AUDIO` 未授权 → 进入语音时被拦，恢复 `previousRegularState`。
- 授权成功 → 进入 `VOICE` Schema。
- 权限判断不散落在 UI 层。

### 14.6 硬件键盘

- `KEYCODE_A` → `PushToken("a")`。
- `KEYCODE_DEL` 有 composing → `Backspace`；无 composing → `deleteSurroundingText`。
- 物理 Shift 与软键盘 Shift 共享状态源。

### 14.7 生命周期与集成

- `onStartInput` 透传 `EditorInfo` 给 resolver。
- `onFinishInputView` 触发 `reset(InputFinished)`。
- `MyBoardImeService` 内无直接 `InputConnection` 调用（静态检查）。
- `MyBoardImeService` 内无 `Color(0xFF` 字面量（静态检查）。
- `engines: Map` 字段已删除（静态检查）。
- 横屏全屏切换不重建 `InputSession`。

### 14.8 构建验收

```bash
./gradlew test
./gradlew assembleDebug
./gradlew generateMyBoardSubtypes
```

能通过并生成 APK 且 `method.xml` 含多个 locale subtype。

本次仅新增设计文档时，不要求运行 Gradle 构建。

## 15. 关键原则

- `MyBoardImeService` 是薄壳，只做钩子转发，不承载输入逻辑。
- 所有 `InputConnection` 调用必须收敛到 `InputConnectionGateway`，引擎层、状态层、UI 层禁止直接持有 `InputConnection`。
- `onUpdateSelection` 必须重写，`SelectionTracker` 判定组合态是否可信；不反推引擎 buffer。
- `EditorInfo` 必须完整解析（密码/邮件/URL/数字/电话/日期），回车键不写死 `IME_ACTION_DONE`。
- 物理键盘事件必须经 `HardwareKeyRouter` 转 `InputEvent`，与触摸键盘共享状态源。
- `method.xml` 必须由构建期 Gradle 任务从内置 Manifest 生成，不得手写。
- 权限必须集中封装在 `PermissionGateway`，UI 只表达「请求能力」。
- 键盘渲染必须套主题、读 token，禁止硬编码颜色。
- 横屏 Extracted Text、配置变化、输入框切换的生命周期边界必须明确，与 `InputPipeline`/`KeyboardContextManager` 协作。
- 外部语言包不参与 `method.xml` 生成，只在 MyBoard 内部切换面板出现。
