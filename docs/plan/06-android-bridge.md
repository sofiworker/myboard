# 阶段 06：Android 桥接层真实实现

> 顺序：06  
> 目标：实现 `InputConnectionGateway`、`FeedbackPlayer`、`EditorInfoResolver`、`SelectionTracker`、`HardwareKeyRouter`，替换所有 fake 组件，让 IME 真正可用。  
> 依据：`docs/android-bridge.md`、`docs/core.md` 第 6 节

## 1. 预期目标

本阶段结束时：

- `InputConnectionGateway` 真实实现，能调用 Android `InputConnection`。
- `FeedbackPlayer` 真实实现，能播放触觉和声音反馈。
- `EditorInfoResolver` 真实实现，能解析 `EditorInfo` 到 `EditorProfile`。
- `SelectionTracker` 真实实现，能追踪光标变化。
- `HardwareKeyRouter` 真实实现，能处理物理键盘事件。
- `MyBoardImeService.kt` 中的硬编码颜色全部删除，依赖 `ThemeResolver`。
- 旧 `method.xml`（只有 1 个泛 subtype）已直接删除，替换为动态 subtype（阶段 07 实现）。
- `FakeInputConnectionGateway` 和 `FakeFeedbackPlayer` 保留供测试。

**依赖说明**：本阶段使用阶段 05 的真实 `InputPipeline` 和阶段 03 的真实 `ThemeResolver`。

## 2. 前置依赖

- 阶段 01 的 `InputConnectionGateway`、`FeedbackPlayer`、`EditorInfoResolver`、`SelectionTracker`、`HardwareKeyRouter` 契约已定义。
- 阶段 03 的 `ThemeResolver`、`FeedbackPolicy` 可用。
- 阶段 05 的 `InputPipeline` 真实实现可用。

## 3. 实施步骤

### 6.1 `InputConnectionGateway` 真实实现

做什么：

- 实现阶段 01 定义的 `InputConnectionGateway` 接口，替换 `FakeInputConnectionGateway`。
- `commitText(text: String): Boolean`：调用 `InputConnection.commitText`。
- `setComposingText(text: String): Boolean`：调用 `InputConnection.setComposingText`。
- `finishComposingText(): Boolean`：调用 `InputConnection.finishComposingText`。
- `deleteSurroundingText(before: Int, after: Int): Boolean`：调用 `InputConnection.deleteSurroundingText`。
- `setSelection(start: Int, end: Int): Boolean`：调用 `InputConnection.setSelection`。
- `performEditorAction(action: Int): Boolean`：调用 `InputConnection.performEditorAction`。
- `getExtractedText(): ExtractedText?`：调用 `InputConnection.getExtractedText`。
- `sendKeyEvent(keyEvent: KeyEvent): Boolean`：调用 `InputConnection.sendKeyEvent`。
- `finishAndCommit(commit: String): Boolean`：先 `finishComposingText` 再 `commitText`。
- 处理 `InputConnection` 为 `null` 的情况，返回 `false` 触发 reset。

测试：

- 单元测试使用 `FakeInputConnectionGateway`。
- 集成测试在真实 `InputConnection` 上验证。

预期目标：

- IME 可向应用提交文本。

性能：

- 单次调用目标小于 1 ms。

### 6.2 `FeedbackPlayer` 真实实现

做什么：

- 实现阶段 01 定义的 `FeedbackPlayer` 接口，替换 `FakeFeedbackPlayer`。
- `playHaptic(token: HapticToken)`：播放触觉反馈。
- `playSound(token: SoundToken)`：播放声音反馈。
- 触觉使用 `Vibrator` API，声音使用 `SoundPool`。
- 开关由 `SettingsManager` 控制。
- 处理无触觉/声音硬件的情况。

测试：

- 单元测试使用 `FakeFeedbackPlayer`。
- 集成测试在真机上验证。

预期目标：

- 按键触发触觉和声音反馈。

性能：

- 触觉和声音播放异步，不阻塞主线程。

### 6.3 `EditorInfoResolver` 真实实现

做什么：

- 实现阶段 01 定义的 `EditorInfoResolver` 接口。
- `resolve(editorInfo: EditorInfo?, currentLocale: LocaleTag): EditorProfile`：解析 `EditorInfo` 到 `EditorProfile`。
- `EditorProfile` 包含：`inputType`、`imeOptions`、`enterAction`、`layoutHint`、`candidateDisabled`、`composingDisabled`、`private`。
- 映射规则：
  - `InputType.TYPE_CLASS_NUMBER` → `layoutHint = LAYOUT_HINT_NUMBER`。
  - `InputType.TYPE_CLASS_PHONE` → `layoutHint = LAYOUT_HINT_PHONE`。
  - `EditorInfo.IME_ACTION_GO` → `enterAction = GO`。
  - `EditorInfo.IME_ACTION_SEARCH` → `enterAction = SEARCH`。
  - `InputType.TYPE_TEXT_VARIATION_PASSWORD` → `candidateDisabled = true`。
  - `InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` → `composingDisabled = true`。

测试：

- 各种 `EditorInfo` 解析正确。
- `null` `EditorInfo` 返回默认 `EditorProfile`。

预期目标：

- IME 可根据输入框类型调整行为。

性能：

- 单次 resolve 小于 1 ms。

### 6.4 `SelectionTracker` 真实实现

做什么：

- 实现阶段 01 定义的 `SelectionTracker` 接口。
- `onSelectionChanged(oldSel: SelectionSnapshot, newSel: SelectionSnapshot, composingActive: Boolean): SelectionDecision`：处理光标变化。
- `SelectionSnapshot` 包含：`start`、`end`、`hashCode`。
- `SelectionDecision` sealed interface：`Ignore`、`ResetComposing`、`RestoreComposing`。
- 规则：
  - 光标移动且 composing 激活 → `ResetComposing`。
  - 光标未变 → `Ignore`。
  - 选择范围变化 → `ResetComposing`。

测试：

- 光标移动触发 `ResetComposing`。
- 光标未变触发 `Ignore`。

预期目标：

- 光标移动可清空 composing。

性能：

- 单次判断小于 0.5 ms。

### 6.5 `HardwareKeyRouter` 真实实现

做什么：

- 实现阶段 01 定义的 `HardwareKeyRouter` 接口。
- `onKeyDown(keyCode: Int, event: KeyEvent): Boolean`：处理物理键盘按下。
- `onKeyUp(keyCode: Int, event: KeyEvent): Boolean`：处理物理键盘释放。
- `onKeyLongPress(keyCode: Int, event: KeyEvent): Boolean`：处理物理键盘长按。
- 映射规则：
  - `KeyEvent.KEYCODE_ENTER` → `Enter` 动作。
  - `KeyEvent.KEYCODE_DEL` → `Delete` 动作。
  - `KeyEvent.KEYCODE_SPACE` → `Space` 动作。
  - 其他按键 → `PushToken` 动作。
- 返回 `true` 表示已处理，`false` 表示未处理。

测试：

- 物理键盘事件映射正确。
- 未处理的按键返回 `false`。

预期目标：

- 物理键盘可输入。

性能：

- 单次路由小于 0.5 ms。

### 6.6 改造 `MyBoardImeService.kt`

做什么：

- 删除所有硬编码颜色，依赖 `ThemeResolver`。
- 使用真实 `InputConnectionGateway`、`FeedbackPlayer`、`EditorInfoResolver`、`SelectionTracker`、`HardwareKeyRouter`。
- 使用真实 `InputPipeline`。
- 使用 `KeyboardContextManager` 管理状态。
- 初始化 `OrthogonalRegistry` 并注册内置 Manifest。
- **直接删除**旧 `method.xml`（只有 1 个泛 subtype）。

测试：

- IME 启动成功。
- 硬编码颜色已全部删除。
- 状态由 `KeyboardContextManager` 管理。

预期目标：

- IME 真正可用，无硬编码颜色。

性能：

- 启动目标小于 1 秒。

### 6.7 保留 fake 组件供测试

做什么：

- 确认 `FakeInputConnectionGateway` 和 `FakeFeedbackPlayer` 保留在测试代码中。
- 不删除，供后续测试使用。

测试：

- 测试编译通过。

预期目标：

- fake 组件可供测试使用。

性能：

- 无影响。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "FakeInputConnectionGateway|FakeFeedbackPlayer" app/src/main/java
```

验收标准：

- `InputConnectionGateway`、`FeedbackPlayer`、`EditorInfoResolver`、`SelectionTracker`、`HardwareKeyRouter` 真实实现。
- `MyBoardImeService.kt` 无硬编码颜色，依赖 `ThemeResolver`。
- 旧 `method.xml` 已删除。
- IME 可启动，可输入文本。
- fake 组件保留供测试。
