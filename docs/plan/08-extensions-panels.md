# 阶段 08：扩展面板与语音/LLM 接入

> 顺序：08  
> 目标：实现扩展面板系统，接入剪贴板、表情、符号、颜文字、语音输入、LLM 面板。  
> 依据：`docs/core.md` 第 8 节「主题、反馈与扩展」

## 1. 预期目标

本阶段结束时：

- 面板系统可注册、切换、渲染扩展面板。
- 剪贴板面板可用。
- 表情/符号/颜文字面板可用。
- 语音输入面板可用（使用现有 `STTProvider` 接口，新 Schema 接入）。
- LLM 面板可用（使用现有 `LLMProvider` 接口，新 Schema 接入）。
- 面板状态由 `KeyboardContextManager` 管理，`bindings.visibleWhen` 控制显隐。
- 旧面板相关代码已直接删除（如有）。

**依赖说明**：本阶段使用阶段 02 的 `KeyboardContextManager`（管理 `activePanel`）、阶段 04 的 `BindingsEvaluator`（控制 region 显隐）、阶段 06 的 `MyBoardImeService.kt`（面板 UI 渲染）。

## 2. 前置依赖

- 阶段 01 的 `PanelType` enum 已定义。
- 阶段 02 的 `KeyboardContextManager` 可管理 `activePanel`。
- 阶段 04 的 `BindingsEvaluator` 可评估 `visibleWhen`。
- 阶段 06 的 `MyBoardImeService.kt` 可渲染面板 UI。

## 3. 实施步骤

### 8.1 面板系统架构

做什么：

- 定义 `PanelProvider` interface：
  - `val panelType: PanelType`
  - `@Composable fun Content(state: PanelState)`
- 定义 `PanelState` data class：`visible`、`data`、`error`。
- 实现 `PanelRegistry`：注册、查询、切换面板。
- 面板切换通过 `KeyboardContextManager.openPanel(panelType)` 和 `closePanel()`。

测试：

- 面板注册成功。
- 面板切换正确。
- `closePanel` 恢复 `activePanel = null`。

预期目标：

- 面板系统可扩展，新面板只需实现 `PanelProvider`。

性能：

- 面板切换目标小于 100 ms。

### 8.2 剪贴板面板

做什么：

- 实现剪贴板 `PanelProvider`。
- 使用现有 `ClipboardManager.kt` 中的逻辑（保留并接入新面板结构）。
- 显示最近剪贴板条目。
- 点击条目提交文本。
- 支持删除单个条目。

测试：

- 剪贴板面板渲染正确。
- 点击条目提交文本。
- 删除条目生效。

预期目标：

- 剪贴板面板可用。

性能：

- 剪贴板数据加载目标小于 50 ms。

### 8.3 表情/符号/颜文字面板

做什么：

- 实现表情 `PanelProvider`。
- 实现符号 `PanelProvider`。
- 实现颜文字 `PanelProvider`。
- 使用现有 `EmojiRepository.kt`、`SymbolRepository.kt`、`KaomojiRepository.kt` 中的数据（保留并接入新面板结构）。
- 支持分类和搜索。

测试：

- 表情面板渲染正确。
- 符号面板渲染正确。
- 颜文字面板渲染正确。
- 点击条目提交文本。

预期目标：

- 表情/符号/颜文字面板可用。

性能：

- 数据加载目标小于 100 ms。

### 8.4 语音输入面板

做什么：

- 实现语音输入 `PanelProvider`。
- 使用现有 `STTProvider.kt` 接口（保留并接入新 Schema `VOICE`）。
- 语音识别结果通过 `InputPipeline` 提交。
- 显示录音状态（等待、录音中、识别中、错误）。
- 权限检查使用 `PermissionGateway`。

测试：

- 语音面板渲染正确。
- 权限未授予时显示提示。
- 录音状态切换正确。

预期目标：

- 语音输入面板可用。

性能：

- 语音识别延迟取决于 STT 服务。

### 8.5 LLM 面板

做什么：

- 实现 LLM `PanelProvider`。
- 使用现有 `LLMProvider.kt` 接口（保留并接入新面板结构）。
- 支持文本输入和 LLM 响应显示。
- 响应文本可插入到输入框。

测试：

- LLM 面板渲染正确。
- 文本输入和响应显示正确。
- 响应文本可插入。

预期目标：

- LLM 面板可用。

性能：

- LLM 响应延迟取决于 LLM 服务。

### 8.6 面板与布局集成

做什么：

- 面板 region 使用 `bindings.visibleWhen = "activePanel == 'emoji'"` 控制显隐。
- 面板切换时 `KeyboardContextManager` 更新 `activePanel`。
- `BindingsEvaluator` 评估 `visibleWhen` 表达式。

测试：

- 打开表情面板时，表情 region 显示。
- 关闭面板时，表情 region 隐藏。

预期目标：

- 面板显隐由 `KeyboardContext` 驱动。

性能：

- `visibleWhen` 评估小于 0.5 ms。

### 8.7 删除旧面板代码

做什么：

- 检查现有面板相关代码，如有旧实现直接删除。
- 保留 `EmojiRepository.kt`、`SymbolRepository.kt`、`KaomojiRepository.kt`、`ClipboardManager.kt`（数据/逻辑保留，接入新面板结构）。
- 保留 `STTProvider.kt`、`LLMProvider.kt`（接口保留，接入新 Schema/面板）。

测试：

- 编译通过，无旧面板引用。

预期目标：

- 面板系统完全使用新架构。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

验收标准：

- 面板系统可注册、切换、渲染。
- 剪贴板、表情、符号、颜文字面板可用。
- 语音输入面板可用。
- LLM 面板可用。
- 面板显隐由 `KeyboardContext` 驱动。
- 旧面板代码已删除。
