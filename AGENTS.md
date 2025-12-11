# Agent Playbook

## 概要
- 项目：Compose 自定义输入法（IME）。核心目标是让自定义键盘 UI 按照布局数据渲染，并能作为输入法窗口正常弹出并输入文字。
- 当前状态：IME Service + ViewModel 已搭好，主题与数据仓库已存在，但键盘 UI 仍是占位，`onKeyAction` 与 `currentInputConnection` 未贯通，软键盘弹出/渲染需优先解决。

## P0（紧急）自定义键盘 UI 合成并确保软键盘弹出
- 目标：在任何文本框中选择本输入法时，键盘界面可见、按键布局来自 assets/用户布局，点击按键能输入文字。
- 关键检查/行动
  - IME 视图创建：`MyboardImeService.onCreateInputView` 已包裹 `MyboardTheme`，确认 `view.setViewTreeLifecycleOwner` 等已设置；若仍不显示，检查 manifest `<service>`、`method.xml`、以及应用是否设为默认输入法。
  - 数据→UI：实现 `KeyboardScreen` 内的 `Toolbar`、`DockedKeyboard`（或 `FloatingKeyboard`）真实渲染。读取 `KeyboardViewModel.keyboardLayout`（由 `KeyboardRepository` 拉取 JSON）生成行/键。
  - 按键事件：在 UI 层调用 `viewModel.onKeyPress(action)`。在 Service 里监听 `viewModel.keyAction`（SharedFlow）并调用 `handleKeyAction`，将文本/删除/移动光标真正写入 `currentInputConnection`。
  - 候选栏：`CandidateView` 需放在键盘上方并使用主题颜色；确保 `suggestions` Flow 触发显示。
  - 输入连接：`handleKeyAction` 中已用 `currentInputConnection`，确保空引用保护；必要时实现 `onStartInput/onStartInputView` 初始化状态。
  - 验证：真实设备/模拟器上选中输入框，确认键盘窗口出现、按键能输出/删除/空格，候选栏显示。

## P1（高）键盘交互与布局细节
- 布局解析：完善 `KeyboardRepository` 对自定义布局的读取/保存；定义键位数据到 `KeyAction` 的映射（Shift、符号面板切换、数字键等）。
- 状态管理：实现 Shift 状态切换与 Caps Lock 行为；处理 `SwitchToLayout`、`SystemEmoji`、`SystemClipboard`、`SystemVoice` 状态切换。
- 光标/删除：补全 `KeyAction.MoveCursor` 方向和长按删除/重复输入的策略。
- 漂浮模式：`isFloatingMode` 流、`MoveFloatingWindow` 拖动逻辑与 UI 实现。

## P2（中）功能完善与可用性
- Emoji/剪贴板面板：实现 `EmojiScreen` 与 `ClipboardScreen`，与主键盘切换逻辑连通。
- 手写/语音：保证 `HandwritingPad`、`VoiceInputManager` UI/权限提示正常，对识别结果写入 `composingText`/建议列表。
- 主题与背景：`ThemeRepository` 支持切换主题、背景图透明度；UI 侧根据 `ThemeData` 应用背景颜色/图片。
- 设置页：完善设置项（键盘高度、主题选择、布局管理、模糊拼音、字典管理），并确保读写 `SettingsRepository`。

## P3（低）体验与质量
- 视觉设计：按键尺寸/间距、圆角、按压反馈、暗/亮主题适配。
- 动画与过渡：键盘弹出、布局切换、候选栏更新的过渡动画。
- 可访问性：TalkBack 标签、按键描述，音效/震动反馈开关。
- 性能/稳定性：异步加载资源、冷启动耗时监控，异常日志收集。

## 提示与注意事项
- Compose 与 IME：必须在 IME 根视图设置 `Lifecycle/SavedState/ViewModelStore` owners，否则 Compose 将无法获取环境并导致空白。
- 输入法权限：测试前确保在系统设置中启用并切换为本输入法；如需语音/录音，确认权限授予。
- 测试建议：在模拟器/真机的短信、记事本中逐步验证输入、删除、切换布局、候选词、语音/手写（若实现）。
- 数据文件：布局位于 `assets/keyboards/layouts/*.json`，主题位于 `assets/themes/*.json`；避免破坏现有 JSON 结构。
