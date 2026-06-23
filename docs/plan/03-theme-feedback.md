# 阶段 03：主题与反馈系统

> 顺序：03  
> 目标：实现以 token 为中心的主题系统和反馈参数数据化，为阶段 04 的布局渲染和阶段 06 的反馈执行提供基础。  
> 依据：`docs/core.md` 第 8 节「主题、反馈与扩展」、`docs/android-bridge.md` 第 3.1 节

## 1. 预期目标

本阶段结束时：

- `ThemeResolver` 实现阶段 01 定义的接口，能解析 `styleRef` 到 `KeyStyle`。
- 主题 token 模型覆盖颜色、形状、字号、圆角、反馈参数。
- 内置浅色/深色静态主题，夜间模式由 `isSystemInDarkTheme()` 驱动。
- 反馈参数数据化为 token，`FeedbackPolicy` 可被 `FeedbackPlayer` 消费。
- 阶段 04 的渲染器可依赖 `ThemeResolver`，渲染期零硬编码颜色。
- 旧 `ThemeModels.kt` 中的旧主题模型已直接删除。

## 2. 前置依赖

- 阶段 01 的 `ThemeResolver`、`KeyStyle`、`FeedbackPolicy`、`HapticToken`、`SoundToken` 契约已定义。
- 阶段 02 的 `SettingsManager` 已包含 `themeMode`、`hapticEnabled`、`soundEnabled` 设置项。

## 3. 实施步骤

### 3.1 主题 token 数据模型

做什么：

- 定义 `ThemeDoc` 数据模型，覆盖 `colors`、`typography`、`shapes`、`motion`、`feedback`。
- 定义 `KeyStyle` data class 实现：`background`、`pressedBackground`、`textColor`、`pressedTextColor`、`fontSize`、`cornerRadius`、`iconTint`。
- `ThemeDoc` 全部 `@Serializable`，支持 JSONC 解析。
- **直接删除**旧 `ThemeModels.kt` 中的 `ThemeColors`/`KeyColors`/`ActionColors`/`BuiltInThemes`/旧 `ThemeResolver`。
- 新 `ThemeResolver` 放在 `core.theme` 新包下。

测试：

- JSONC 主题文件解析成功。
- `KeyStyle` 所有字段可反序列化。
- 缺 `key_default` 基础 token 时解析失败。

预期目标：

- 主题数据完全数据化，无旧 Kotlin 枚举硬编码颜色。

性能：

- 主题解析只在启动或主题切换时发生。
- 单个主题文件解析目标小于 10 ms。

### 3.2 实现 `ThemeResolver`

做什么：

- 实现阶段 01 定义的 `ThemeResolver` 接口（新实现，新包）。
- `resolveKeyStyle(styleRef: String): KeyStyle`：查 token，未命中回退 `key_default`。
- `resolveFeedbackPolicy(): FeedbackPolicy`：返回当前主题的反馈参数集合。
- `isDark(): Boolean`：返回当前激活主题是浅色还是深色。
- 内部维护 `StateFlow<ThemeDoc>`，主题切换时发射新值。

测试：

- `resolveKeyStyle("key_function")` 返回对应样式。
- `resolveKeyStyle("unknown_ref")` 回退到 `key_default`。
- 主题切换后 `StateFlow` 发射新 `ThemeDoc`。
- `isDark()` 在深色主题返回 true。

预期目标：

- 阶段 04 渲染器只调 `resolveKeyStyle`，不接触原始 `ThemeDoc`。

性能：

- `resolveKeyStyle` 目标小于 0.5 ms。

### 3.3 内置浅色/深色主题

做什么：

- 在 `app/src/main/assets/themes/` 下新建 `light.jsonc` 和 `dark.jsonc`。
- token 命名遵循 `core.md` 第 8 节。
- 参考旧 `BuiltInThemes` 中的颜色值，在新 JSONC 中重新定义（不迁移旧代码，只参考旧值重新编写）。
- `MyBoardImeService.kt` 中的硬编码颜色在阶段 06 彻底删除，本阶段先准备好替代 token。

测试：

- 浅色和深色主题都能被 `ThemeResolver` 加载。
- 浅色与深色 token 值不同。
- 主题切换由 `isSystemInDarkTheme()` 驱动。

预期目标：

- 夜间模式自动切换，不写死。

性能：

- 主题文件小（小于 5 KB），加载开销可忽略。

### 3.4 反馈参数数据化

做什么：

- 定义 `FeedbackPolicy` 实现：`hapticTokens` 和 `soundTokens`。
- `HapticToken` data class：`durationMs`、`amplitude`、`fallbackVibration`。
- `SoundToken` data class：`soundResName`、`volume`。
- token 命名：`key_tap`、`key_long_press`、`key_backspace`、`candidate_select`、`enter`。
- 触觉/声音开关由 `SettingsManager` 控制。
- 反馈参数写入主题 JSONC 文件的 `feedback` 段。

测试：

- `resolveFeedbackPolicy()` 返回的 policy 含全部预定义 token。
- `HapticToken.amplitude` 在有效范围 [1, 255]。
- 反馈开关关闭时 `FeedbackPolicy` 仍可获取但执行层跳过。

预期目标：

- 反馈参数不硬编码在调用点，全部来自主题或设置。

性能：

- `FeedbackPolicy` 解析缓存为 immutable，查询小于 0.5 ms。

### 3.5 反馈执行 fake 实现

做什么：

- 确认阶段 01 的 `FakeFeedbackPlayer` 签名与真实反馈 token 一致。

测试：

- `FakeFeedbackPlayer.playHaptic(keyTap)` 记录调用。

预期目标：

- 阶段 04/05 可验证"按键触发反馈 token"而不依赖真实硬件。

性能：

- fake 无开销。

### 3.6 设置入口与 i18n

做什么：

- 在 `SettingsManager` 中确认 `hapticEnabled`、`soundEnabled`、`themeMode` 字段。
- 在 `SettingsActivity.kt` 中增加主题模式、触觉开关、声音开关入口（在现有 `SettingsScreen` 基础上扩展）。
- 补齐 `values/strings.xml` 和 `values-zh-rCN/strings.xml`。

测试：

- 设置项读写单元测试。
- Activity 启动 smoke test。
- 中英文字符串 key 一致。

预期目标：

- 主题和反馈设置可从 `SettingsActivity.kt` 到达。

性能：

- 设置读写轻量。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "ThemeColors|KeyColors|ActionColors|BuiltInThemes" app/src/main/java
```

验收标准：

- `ThemeResolver` 实现阶段 01 接口，`resolveKeyStyle` 小于 0.5 ms。
- 浅色/深色内置主题可加载，夜间模式自动切换。
- 旧 `ThemeColors`/`KeyColors`/`ActionColors`/`BuiltInThemes` 已直接删除。
- 反馈参数 token 化，`FeedbackPolicy` 可被 `FakeFeedbackPlayer` 消费。
- 主题模式、触觉、声音设置可从 `SettingsActivity.kt` 到达。
- 中英文 i18n 字符串齐全。
