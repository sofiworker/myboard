# 阶段 10：集成测试、性能优化与发布准备

> 顺序：10  
> 目标：完成集成测试、性能优化、最终验收，准备发布。  
> 依据：`docs/core.md` 第 10 节

## 1. 预期目标

本阶段结束时：

- 所有集成测试通过。
- 性能指标达标。
- 所有旧代码已删除，代码库干净。
- APK 可发布。

**依赖说明**：本阶段是收尾阶段，所有前置阶段完成。

## 2. 前置依赖

- 阶段 01-09 全部完成。
- 所有新代码已实现。
- 所有旧代码已删除。

## 3. 实施步骤

### 10.1 集成测试

做什么：

- 编写端到端集成测试：
  - 启动 IME → 选择 subtype → 输入文本 → 提交。
  - 切换 subtype → 输入文本 → 提交。
  - 打开面板 → 选择条目 → 提交。
  - 导入语言包 → 切换 subtype → 输入文本。
  - 导入自定义布局 → 使用布局输入。
  - 导入自定义主题 → 切换主题 → 验证渲染。
- 使用 Android Instrumentation Test 或类似框架。

测试：

- 所有集成测试通过。

预期目标：

- 核心流程无回归。

性能：

- 无影响。

### 10.2 性能测试与优化

做什么：

- 性能指标：
  - 冷启动时间：小于 1 秒。
  - 热启动时间：小于 200 ms。
  - 按键响应延迟：小于 16 ms（60 fps）。
  - 布局测量时间：小于 5 ms。
  - 字典查询时间：小于 5 ms。
  - 主题解析时间：小于 10 ms。
  - 内存占用：小于 100 MB（典型使用场景）。
- 使用 Android Profiler 分析性能瓶颈。
- 优化热点代码。

测试：

- 所有性能指标达标。

预期目标：

- 性能满足用户体验要求。

性能：

- 优化后达标。

### 10.3 最终确认旧代码已删除

做什么：

- 确认所有旧代码和旧资源已删除：
  - 旧 Kotlin 代码（各阶段删除）：
    - `KeyboardState`、`KeyboardStateManager`、`ShiftState`（阶段 02）
    - `LanguageSwitchManager`、`InputMethodConfig`、`EngineType`（阶段 02）
    - 旧 `InputAction.kt`（阶段 02）
    - `ThemeColors`/`KeyColors`/`ActionColors`/`BuiltInThemes`、旧 `ThemeResolver`、旧 `ThemeModels.kt`（阶段 03）
    - `LayoutModels.kt`、`GridCalculator.kt`、`LayoutRepository.kt`、`PatchApplier.kt`、旧 `LayoutParser.kt`（阶段 04）
    - `KeyBindingModels.kt`、`KeyBindingManager.kt`、旧 `ActionDispatcher.kt`、`mapKeyToAction` 硬编码（阶段 04）
    - `InputEngine.kt`、`DirectInputEngine.kt`、`CompositionInputEngine.kt`（阶段 05）
    - `TrieDict.kt`、`SuggestionEngine.kt`、`DictionaryImporter.kt`（阶段 05）
    - 硬编码颜色（阶段 06）
  - 旧资源文件（阶段 04 删除）：
    - `assets/layouts/*.json`（v1 格式）
    - `assets/layouts/v2/` 整个子目录
    - `assets/subtypes/` 整个目录
  - 旧配置（阶段 06 删除）：
    - 旧 `method.xml`
- 使用 `rg` 和 `find` 确认无旧代码引用、无旧资源文件。

测试：

- `rg` 搜索无旧代码引用。
- `find` 搜索无旧资源文件。
- 编译通过。
- 所有测试通过。

预期目标：

- 代码库干净，无旧代码和旧资源残留。

性能：

- 无影响。

### 10.4 代码审查与文档

做什么：

- 代码审查：确保代码质量、命名规范、注释完整。
- 更新文档：
  - `README.md`：更新项目介绍、构建说明、使用方法。
  - `docs/` 目录：确保文档与代码一致。
- 更新 `CHANGELOG.md`：记录版本变更。

测试：

- 文档与代码一致。

预期目标：

- 代码和文档可维护。

性能：

- 无影响。

### 10.5 发布准备

做什么：

- 更新版本号：`build.gradle` 中的 `versionCode` 和 `versionName`。
- 签名配置：确保 release 签名正确。
- 构建 release APK：`./gradlew assembleRelease`。
- 测试 release APK：安装并验证功能。

测试：

- release APK 功能正常。

预期目标：

- APK 可发布。

性能：

- 无影响。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleRelease
```

静态检查（Kotlin 代码）：

```bash
rg "KeyboardState|KeyboardStateManager|LanguageSwitchManager|InputEngine|DirectInputEngine|CompositionInputEngine|EngineType|LayoutModels|GridCalculator|LayoutRepository|PatchApplier|KeyBindingModels|KeyBindingManager|ThemeColors|KeyColors|ActionColors|BuiltInThemes|TrieDict|SuggestionEngine|DictionaryImporter|mapKeyToAction|LayoutParser" app/src/main/java
```

静态检查（assets 资源）：

```bash
find app/src/main/assets/layouts -name "*.json"  # 应无结果
ls app/src/main/assets/layouts/v2 2>/dev/null     # 应不存在
ls app/src/main/assets/subtypes 2>/dev/null       # 应不存在
```

验收标准：

- 所有集成测试通过。
- 性能指标达标。
- 所有旧 Kotlin 代码已删除，`rg` 搜索无引用。
- `assets/layouts/` 下只有 `.jsonc` 文件，无 `.json` 文件，无 `v2` 子目录。
- `assets/subtypes/` 目录不存在。
- 代码审查完成。
- 文档更新完成。
- release APK 构建成功并验证通过。
