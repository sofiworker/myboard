# 阶段 02：正交状态、Manifest 与设置单一来源

> 顺序：02  
> 目标：建立 `KeyboardContext` 唯一运行时状态源，并让语言能力由 Manifest 注册。  
> 依据：`docs/orthogonal-state-management.md`、`docs/core.md`

## 1. 预期目标

本阶段结束时：

- `Locale + Script + Schema` 成为输入语义状态。
- `KeyboardContextManager` 替代旧 `KeyboardStateManager`，旧 `KeyboardStateManager` 直接删除。
- `OrthogonalRegistry` 能注册内置 Manifest，并校验合法矩阵。
- `SettingsManager` 保存默认 Locale、启用 Locale、默认 Script、默认 Schema 和能力开关。
- `SettingsActivity.kt` 能进入相关设置入口。

**依赖说明**：本阶段注入阶段 01 定义的 7 个 stub Registry + 1 个 stub Resolver。Manifest 注册时只校验 JSON 结构和必填字段，`engineId`/`layoutId`/`encoderId`/`candidatePolicy`/`displayPolicy` 存在性校验推迟到阶段 05 补全。`OrthogonalRegistry` 依赖的 Registry 用接口注入，便于阶段 05 替换真实实现。

## 2. 前置依赖

- 阶段 01 的跨层接口契约已定义（7 个 Registry 接口 + stub 实现）。
- 阶段 01 的 `InputAction`、`ResetReason`、`Candidate`、`OrthogonalState`、`KeyboardContext`、`LanguageManifest`、`SchemaCapability` 等契约 data class 已就绪。

## 3. 实施步骤

### 2.1 实现正交核心类型并删除旧状态

做什么：

- 实现 `LocaleTag`、`Script`、`Schema`、`BuiltInSchemas`、`LayoutLayer`、`OrthogonalState`、`KeyboardContext`、`PanelType`（阶段 01 已定义签名）。
- **直接删除**旧 `KeyboardState.kt`（`KeyboardState`、`ShiftState`）和旧 `KeyboardStateManager.kt`。
- **直接删除**旧 `InputAction.kt`，替换为阶段 01 定义的新 `InputAction` sealed interface。
- 所有引用旧状态的调用点改为引用新 `KeyboardContext`。

测试：

- `KeyboardContext` 默认值可构造，没有旧字段。
- `PanelType` 各值可构造。
- 编译通过，旧 `KeyboardState`/`KeyboardStateManager` 引用已全部清除。

预期目标：

- 状态模型能表达 `zh-CN + HANI + PINYIN`、`zh-CN + LATN + LATIN_DIRECT`、`ja-JP + HIRA + ROMAJI`。

性能：

- 状态对象保持不可变 data class。
- 单次复制更新不分配大集合，候选列表使用不可变快照。

### 2.2 实现 Manifest 数据模型并删除旧语言管理

做什么：

- 实现 `LanguageManifest`、`LocaleCapability`、`ScriptCapability`、`SchemaCapability`、`CapabilityId`（阶段 01 已定义签名）。
- 字段按 `orthogonal-state-management.md:4.2` 固定。
- 支持 JSONC 解析（提取现有 `LayoutParser.stripJsonLineComments` 为独立工具函数，放在新包中）。
- **直接删除**旧 `LanguageSwitchManager.kt`（`LanguageSwitchManager`、`SwitchRule`、`LanguageRegistry`、`LanguageInfo`）。
- **直接删除**旧 `InputEngine.kt` 中的 `InputMethodConfig` 及相关配置类（`EngineType` 等）。

测试：

- 解析中文、英文、日文内置 Manifest 示例。
- 缺少 `defaults.script`、`defaults.schema`、`supportsShift` 时注册失败。
- 字段非空校验通过。
- 旧 `LanguageSwitchManager` 引用已全部清除。

预期目标：

- Kotlin 状态机不再通过 `if (locale == "zh-CN")` 决定能力。
- Manifest 数据结构完全对齐设计文档。

性能：

- 单个内置 Manifest 解析目标小于 20 ms。

### 2.3 实现 `OrthogonalRegistry`

做什么：

- 实现 `register`、`unregister`、`getLocale`、`isSupported`、`defaultState`、`defaultSchema`、`schemaCapability`。
- 注册前完成**结构校验**，失败不产生半注册状态。
- 支持内置包优先、外部包默认只新增、用户显式选择覆盖。
- 使用 `CapabilityId = packageId + locale + script + schema` 区分同名能力。
- **注入 7 个 stub Registry + 1 个 stub Resolver**。

**stub 阶段校验范围**：

- JSON 结构合法、必填字段存在、`manifestVersion` 受支持。
- `defaults.script` 在 `scripts` 中、`defaults.schema` 在默认 Script 中、每个 Script 有 `defaultSchema`。
- `engineId`/`layoutId`/`encoderId`/`candidatePolicy`/`displayPolicy`/`mapping`/`dictionary`/`fsm` 字段**非空校验**。
- **存在性校验跳过**（stub Registry 无法判断是否真实存在）。
- 资源路径格式校验：禁止 `../` 逃逸，禁止绝对路径。
- subtype labelKey 非空校验（存在性校验在阶段 07 补齐）。

测试：

- 合法 Manifest 结构注册成功。
- `zh-CN + HANI + PINYIN` 合法。
- `zh-CN + HANI + ROMAJI` 非法。
- `ja-JP + HANI + PINYIN` 非法。
- 多包冲突按确定性策略解析。
- 缺必填字段时注册失败。
- 资源路径包含 `../` 时注册失败。

预期目标：

- 任意状态合法性只来自 registry。
- 阶段 05 替换真实 Registry 后，存在性校验无需改代码，只换注入对象。

性能：

- `isSupported` 和 `schemaCapability` 使用 map 查询，目标 O(1)。
- 注册后 capability 对象不可变，可安全共享。

### 2.4 实现 `TransitionEngine`

做什么：

- 实现 `TransitionEvent` 和 `TransitionResult` sealed interface。
- 实现 Locale、Script、Schema、Layer、Panel 的通用 reduce 规则。
- 切换输入语义时清空 composing、candidates、selectedCandidateIndex。
- `supportsShift = false` 时 Shift/Caps 请求返回 Applied 但保持普通层。
- 特殊 Schema `VOICE`、`HANDWRITING` 维护 `previousRegularState`。
- `openPanel`/`closePanel` 不改变 `OrthogonalState`。

测试：

- `switchLocale(en-US)` 得到 `en-US + LATN + LATIN_DIRECT`。
- `switchScript(LATN)` 在 `zh-CN` 下得到 `zh-CN + LATN + LATIN_DIRECT`。
- `switchSchema(DOUBLE_PINYIN)` 成功，`switchSchema(ROMAJI)` 失败且状态不变。
- `switchLayer(SHIFTED)` 在 `supportsShift=false` 下 Applied 但保持 NORMAL。
- `openPanel(EMOJI)` 设置 `activePanel=EMOJI`，不改变 `OrthogonalState`。
- 特殊 Schema 切换恢复规则正确。

预期目标：

- 状态转移不能静默失败。

性能：

- 单次状态转移目标小于 1 ms。
- 不在 reduce 中做 IO、解析 JSON 或加载字典。

### 2.5 实现 `KeyboardContextManager`

做什么：

- 暴露 `StateFlow<KeyboardContext>`。
- 所有状态变更必须经 `KeyboardContextManager`。
- 提供 `switchLocale`、`switchScript`、`switchSchema`、`switchLayer`、`openPanel`、`closePanel`、`setComposing`、`clearComposing`、`applyEditorProfile`。
- `setComposing` 标记为输入管线内部入口。
- `applyEditorProfile` 接收 `EditorProfile`，根据 `LayoutHint`、`candidateDisabled`、`composingDisabled` 调整状态。
- 维护 `previousRegularState: OrthogonalState?`。

测试：

- `clearComposing` 清空 text、candidate、selected index。
- `openPanel(EMOJI)` 不改变 `OrthogonalState`。
- `applyEditorProfile` 接收 `EditorProfile(candidateDisabled=true)` 后候选栏 region 隐藏。

预期目标：

- Compose UI、候选栏、布局渲染只观察这一份 StateFlow。

性能：

- 状态更新在主线程执行。
- 避免高频输入时重复发送相同 context。

### 2.6 改造 `SettingsManager` 和 `SettingsActivity.kt`

做什么：

- 在现有 `SettingsManager`（`core/settings/SettingsManager.kt`）基础上新增：
  - 当前默认 Locale。
  - 已启用 Locale 列表。
  - 每个 Locale 的默认 Script。
  - 每个 `Locale + Script` 的默认 Schema。
  - 是否启用双拼、语音、手写。
  - 用户对同名 capability 的选择。
  - 主题模式（auto/light/dark，阶段 03 补充）。
  - 触觉反馈开关（阶段 03 补充）。
  - 声音反馈开关（阶段 03 补充）。
- `SettingsActivity.kt` 必须能进入这些设置，不允许只有隐藏入口。
- 破坏性替换旧设置键，无需兼容旧值。

测试：

- 设置读写单元测试。
- Activity 启动 smoke test。
- 所有设置项都有英文和中文字符串。

预期目标：

- UI 层、布局 JSON、IME Service 不再各自保存默认语言或默认模式。

性能：

- 设置读写使用轻量持久化（`SharedPreferences`），主线程不做大 JSON 解析。

### 2.7 创建内置 Manifest 文件

做什么：

- 在 `app/src/main/assets/languages/` 下创建内置语言包：
  - `zh-CN/language.manifest.json`
  - `en-US/language.manifest.json`
  - `ja-JP/language.manifest.json`
- 每个 Manifest 声明 `subtype.labelKey`。
- 确保资源路径指向待创建资源（阶段 05 补齐）。

测试：

- 所有内置 Manifest 注册成功。
- 缺 `subtype.labelKey` 时注册失败。

预期目标：

- 阶段 07 subtype 生成有稳定输入。

性能：

- 内置 Manifest 总解析目标小于 100 ms。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "KeyboardState|KeyboardStateManager|LanguageSwitchManager|ShiftState|InputMethodConfig|EngineType" app/src/main/java
```

验收标准：

- 正交状态相关单元测试全部通过。
- 旧 `KeyboardState`/`KeyboardStateManager`/`LanguageSwitchManager`/`InputAction` 已直接删除，引用已清除。
- `KeyboardContextManager`、`TransitionEngine`、`OrthogonalRegistry` 可编译可测。
- 默认状态来自 Manifest + Settings，而不是旧硬编码路由。
- `OrthogonalRegistry` 注入 7+1 个 stub 可编译可测，存在性校验缺失项已记录待阶段 05 补齐。
- 内置 Manifest 文件创建完成，结构校验通过。
