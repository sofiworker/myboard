# 阶段 09：语言包与自定义

> 顺序：09  
> 目标：实现语言包导入、自定义布局、自定义主题、文本扩展。  
> 依据：`docs/core.md` 第 9 节

## 1. 预期目标

本阶段结束时：

- `LanguagePackImporter` 真实实现，可导入 ZIP 格式语言包。
- 用户可自定义布局（导入 JSONC 文件）。
- 用户可自定义主题（导入 JSONC 文件）。
- `TextExpansionManager` 接入新系统。
- 旧自定义相关代码已直接删除（如有）。

**依赖说明**：本阶段使用阶段 02 的 `OrthogonalRegistry`（注册新 Manifest）、阶段 04 的 `LayoutRegistry`（注册新布局）、阶段 03 的 `ThemeResolver`（加载新主题）。

## 2. 前置依赖

- 阶段 01 的 `LanguagePackImporter` 契约已定义。
- 阶段 02 的 `OrthogonalRegistry` 可注册新 Manifest。
- 阶段 04 的 `LayoutRegistry` 可注册新布局。
- 阶段 03 的 `ThemeResolver` 可加载新主题。

## 3. 实施步骤

### 9.1 `LanguagePackImporter` 真实实现

做什么：

- 实现阶段 01 定义的 `LanguagePackImporter` 接口。
- `import(zipFile: Uri, registry: OrthogonalRegistry): ImportResult`：导入 ZIP 格式语言包。
- ZIP 结构：
  - `language.manifest.json`：语言 Manifest。
  - `layouts/*.jsonc`：布局文件。
  - `dictionaries/*`：字典文件。
  - `fsm/*`：FSM 文件。
  - `mappings/*`：映射表文件。
- 导入流程：
  1. 解压 ZIP 到临时目录。
  2. 解析 `language.manifest.json`。
  3. 校验 Manifest 结构和必填字段。
  4. 校验资源文件存在性。
  5. 注册到 `OrthogonalRegistry`。
  6. 清理临时目录。
- `ImportResult` sealed interface：`Success`、`PartialSuccess(warnings)`、`Failed(errors)`。

测试：

- 合法 ZIP 导入成功。
- 缺 `language.manifest.json` 时导入失败。
- Manifest 校验失败时导入失败。
- 资源文件缺失时导入失败。

预期目标：

- 用户可导入第三方语言包。

性能：

- ZIP 解压和注册目标小于 2 秒。

### 9.2 自定义布局导入

做什么：

- 实现自定义布局导入功能。
- 用户通过文件选择器选择 JSONC 文件。
- 文件解析为 `LayoutDoc`。
- 校验布局结构。
- 注册到 `LayoutRegistry`。
- 权限检查使用 `PermissionGateway`。

测试：

- 合法 JSONC 导入成功。
- 非法 JSONC 导入失败。
- 布局校验失败时导入失败。

预期目标：

- 用户可导入自定义布局。

性能：

- 单个布局文件导入目标小于 500 ms。

### 9.3 自定义主题导入

做什么：

- 实现自定义主题导入功能。
- 用户通过文件选择器选择 JSONC 文件。
- 文件解析为 `ThemeDoc`。
- 校验主题结构（必须包含 `key_default` 基础 token）。
- 注册到 `ThemeResolver`。
- 权限检查使用 `PermissionGateway`。

测试：

- 合法 JSONC 导入成功。
- 缺 `key_default` 时导入失败。
- 主题校验失败时导入失败。

预期目标：

- 用户可导入自定义主题。

性能：

- 单个主题文件导入目标小于 500 ms。

### 9.4 `TextExpansionManager` 接入

做什么：

- 使用现有 `TextExpansionManager.kt` 中的逻辑（保留并接入新系统）。
- 文本扩展作为引擎扩展能力，在 `InputPipeline` 中调用。
- 扩展结果作为候选显示。

测试：

- 文本扩展触发正确。
- 扩展结果可提交。

预期目标：

- 文本扩展功能可用。

性能：

- 文本扩展查询目标小于 10 ms。

### 9.5 设置入口

做什么：

- 在 `SettingsActivity.kt` 中增加：
  - 语言包导入入口。
  - 自定义布局导入入口。
  - 自定义主题导入入口。
  - 已导入语言包/布局/主题管理（查看、删除）。
- 补齐 i18n 字符串。

测试：

- 设置入口可到达。
- 导入流程可执行。
- 管理功能可用。

预期目标：

- 自定义功能可从设置到达。

性能：

- 无影响。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

验收标准：

- `LanguagePackImporter` 真实实现，可导入 ZIP 格式语言包。
- 自定义布局和主题可导入。
- `TextExpansionManager` 接入新系统。
- 设置入口可到达。
- i18n 字符串齐全。
