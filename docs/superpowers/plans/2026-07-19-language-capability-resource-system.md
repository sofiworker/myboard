# Language Capability Resource System Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 MyBoard 从当前嵌套 Manifest 和局部资源注册模型迁移到以 `LanguagePackManifest`、Capability、Dictionary、Layout、Engine Registry 和 `InputPipeline` 为核心的全球化输入架构，并保持 `Locale + Script + Schema` 正交状态与设置单一来源。

**Architecture:** 先建立开放 Script、目标 Manifest、资源引用和 Registry 契约，再将内置中英日数据转换为 Language Pack。`CapabilityRegistry` 根据正交状态选择 provider，`ResourceResolver` 将逻辑引用解析为带版本的资源句柄，Pipeline 在单一串行域内创建和切换 `InputSession`。`SettingsActivity` 是设置 Repository/Factory 的唯一组装根。

**Tech Stack:** Kotlin/JVM inline value class、kotlinx.serialization、Android/Jetpack、Compose、Room、Kotlin Coroutines/Flow、现有 JUnit/Android test、Gradle。

## Global Constraints

- `OrthogonalState` 永远只包含 `Locale + Script + Schema`；`packageId`、字典、布局、引擎和资源版本不得成为状态轴。
- `Script` 使用开放的 ISO 15924 风格四字母 value class；`KATA` 统一为 `KANA`，`HANGUL` 统一为 `HANG`，未知但格式合法的 Script 可由 Manifest 注册。
- 目标模型破坏性替换旧模型；不得保留 `LanguageManifest`、`LocaleCapability`、`ScriptCapability`、`SchemaCapability` 的运行时双写、兼容适配层或 fallback 读取路径。
- Language Pack 只能提供声明和数据，不能携带 Kotlin/Java/DEX/SO/脚本或其他可执行代码。
- 所有 Manifest、依赖、资源路径、引擎/策略 ID、版本和完整性必须在注册前校验；运行时失败必须回退到可用能力，不得导致 IME 崩溃。
- `LayoutCanonicalId` 固定为 `${packageId}:${layoutId}`，不包含版本；恢复 `LayoutKey` 必须经过 Registry 和明确的 package version。
- `DictionaryKind` 与 `DictionaryRole` 必须按设计文档兼容矩阵校验，不能用 role 绕过资源类型约束。
- `SettingsActivity` 是设置 Repository/Factory 的唯一组装根；生产 Compose 默认参数不得创建真实 `SettingsRepository`。
- 每个任务完成后运行其列出的最小测试；阶段合并前运行 `./gradlew testDebugUnitTest assembleDebug`。
- 不撤销工作区中用户已有的 onboarding、settings UI 和 Gradle wrapper 修改；只修改任务列出的文件。

---

## 文件与模块边界

- 契约：`app/src/main/java/xyz/xiao6/myboard/contract/{manifest,language,engine,layout,registry,state}`。
- 注册与解析：`app/src/main/java/xyz/xiao6/myboard/{engine,layout,dictionary,pack,state}`。
- 输入运行时：`app/src/main/java/xyz/xiao6/myboard/engine/InputPipeline*`、`state/KeyboardContextManager*`、`state/TransitionEngine*`。
- 设置注入：`app/src/main/java/xyz/xiao6/myboard/activity/SettingsActivity.kt`、`ui/settings/*`。
- 测试：与生产包名对应的 `app/src/test/java`，需要 Android 生命周期或真实数据库时使用 `app/src/androidTest/java`。

## Task 1: 建立开放 Script 与基础领域类型

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/manifest/ManifestContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/state/KeyboardContext.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/BuiltInManifests.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/TransitionEngine.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/TransitionEngineImpl.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/contract/ScriptTest.kt`

**Interfaces:**
- Produces `@JvmInline @Serializable value class Script` with `parse`, `LATN/HANI/HIRA/KANA/HANG/ARAB/THAI/DEVA` constants.
- Produces `ScriptDescriptor`, `TextDirection`, `LayoutMirrorPolicy` and `ScriptCatalog` lookup metadata.

- [ ] 写失败测试：合法四字母值、大小写规范化、非法长度/字符、`KANA`/`HANG` 常量和未知合法 Script。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*ScriptTest'`，确认新契约尚不存在或测试失败。
- [ ] 实现开放 value class，删除业务代码对封闭 Script enum 和 `enumPayload<Script>()` 的依赖。
- [ ] 更新 `BuiltInManifests.kt` 中的内置 Script 标识为 `KANA`/`HANG`；该文件在 Task 4 完成目标 Language Pack 转换后删除，不保留 `KATA`/`HANGUL` 兼容别名。
- [ ] 更新状态切换和布局 action 的 Script 解析，只校验标识格式，再由能力目录判断是否支持。
- [ ] 再次运行 Script 测试和 `./gradlew testDebugUnitTest`。
- [ ] 提交 `feat: make script identifiers extensible`。

## Task 2: 替换 Manifest 与 Capability 契约

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/manifest/ManifestContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/language/LanguagePackContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/engine/EngineContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/registry/RegistryContracts.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/contract/ManifestContractTest.kt`

**Interfaces:**
- Produces `LanguagePackManifest`, `ScriptManifest`, `LocaleDefaults(layout: ResourceRef)`, `LanguageCapability`, `CapabilityId`, `EngineBinding`, `DictionaryBinding`, `DictionaryRole`.
- Removes runtime references to `LanguageManifest`, `LocaleCapability`, `ScriptCapability`, `SchemaCapability`.

- [ ] 写契约测试：扁平 capability、重复 Script/CapabilityId、descriptor/script 不一致、缺失 default capability 均被拒绝。
- [ ] 运行目标测试确认旧模型仍被使用或新模型未定义。
- [ ] 按设计文档直接替换旧嵌套模型；Task 1 更新后的 `BuiltInManifests` 只作为一次性 fixture 转换入口，Task 4 完成转换后立即删除。
- [ ] 增加 Dictionary kind/role 兼容矩阵校验：`WORD/PHRASE -> PRIMARY`，其余类型只绑定同名角色。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*ManifestContractTest'` 和全量单元测试。
- [ ] 提交 `feat: replace language manifest contracts`。

## Task 3: 实现 ResourceRef、LayoutKey 与资源解析

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/layout/LayoutContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/manifest/ManifestContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/LayoutRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolver.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/layout/LayoutCanonicalIdTest.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/engine/ResourceResolverTest.kt`

**Interfaces:**
- Produces `ResourceRef`, `ResolvedResourceKey`, `LayoutKey`, `LayoutCanonicalId`, `LayoutKey.toCanonicalId()` and Registry-based `LayoutCanonicalId.resolve(version, registry)`.

- [ ] 写布局 canonical 测试：`builtin:qwerty` 格式、无版本序列化、非法 `:`、同名不同 package 不冲突、恢复必须提供版本。
- [ ] 写资源解析测试：路径越界、hash/version/kind 校验、`REJECT_PACKAGE`/`DISABLE_CAPABILITY`/`USE_CAPABILITY_FALLBACK` 行为。
- [ ] 实现逻辑引用到已解析资源身份的转换，并让 LayoutRegistry 以 `(packageId, layoutId, packageVersion)` 查询。
- [ ] 将 `KeyboardContext.layoutId` 约束为 canonical 字符串投影，禁止 provider 自行拼接。
- [ ] 运行两个目标测试及全量单元测试。
- [ ] 提交 `feat: add versioned resource resolution`。

## Task 4: 扩展 Registry 与内置 Language Pack 注册

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/DictionaryRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/DictionaryRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/BuiltInLayouts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackManagerImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackImporter.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/BuiltInLanguagePacks.kt`
- Delete after conversion: `app/src/main/java/xyz/xiao6/myboard/state/BuiltInManifests.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/pack/LanguagePackRegistrationTest.kt`

**Interfaces:**
- Produces immutable `RegistrySnapshot`/capability index：`OrthogonalState -> List<CapabilityId>` and package/resource generation。
- `BuiltInLanguagePacks` 将中英日 fixture 转换为目标 Manifest 后注册，运行时不再直接消费旧 fixture。

- [ ] 写注册测试：内置中英日、多个 provider、稳定 tie-breaker、重复包 ID、缺失 engine/layout/dictionary、卸载后索引清理。
- [ ] 实现 Engine/Policy/Dictionary/Layout 注册和快照发布，禁止半注册状态暴露。
- [ ] 迁移内置英文直输、中文拼音/双拼/T9、日文罗马字的 Manifest 和资源引用。
- [ ] 完成所有内置 fixture 到目标 Manifest 的转换后删除 `BuiltInManifests.kt`，并确认 Registry、Resolver、ContextManager、Pipeline 不再引用该文件。
- [ ] 验证 capability 的默认 Locale/Script/Schema 与 ResourceRef 能解析到正确 LayoutKey。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*LanguagePackRegistrationTest'`。
- [ ] 提交 `feat: register built-in language packs`。

## Task 5: 重构 KeyboardContextManager 初始化与状态解析

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/KeyboardContextManager.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/KeyboardContextManagerImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/TransitionEngineImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/state/KeyboardContextManagerTest.kt`

**Interfaces:**
- `createInitialContext()` 只从已注册 capability 解析默认状态和布局。
- `MyBoardImeService` 初始化顺序固定为 Engine -> Policy -> Dictionary -> Layout -> Language Pack -> Capability -> ContextManager -> Pipeline。

- [ ] 写测试证明空 Registry 不能正常创建 Manager，已注册内置包可创建，`en-US` 仅来自内置 manifest 默认值。
- [ ] 拆分 `initCoreComponents()` 为注册阶段和运行时组件阶段。
- [ ] 让 Locale/provider preference、Script/Schema 默认值和布局均从 Registry/Settings Flow 解析。
- [ ] 只在启动恢复/包损坏时走显式 direct 安全回退，不写回 Registry、不掩盖注册错误。
- [ ] 运行状态测试和 `./gradlew testDebugUnitTest`。
- [ ] 提交 `refactor: initialize context after capabilities`。

## Task 6: 接入 InputPipeline 与 InputSession 生命周期

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/InputPipeline.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/InputPipelineImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/builtin/DirectEngine.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/builtin/TableComposingEngine.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/builtin/TransliterationEngine.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/engine/InputPipelineTest.kt`

**Interfaces:**
- Pipeline 消费 `OrthogonalState`、`CapabilityRegistry`、`EngineRegistry`、`ResourceResolver`，产生带 `ResolvedCapabilityKey` 的 `InputSession`。
- 每个 Pipeline 使用 Mutex/actor 串行处理 `InputAction`；旧 session 的迟到字典结果必须被 generation 丢弃。

- [ ] 写测试覆盖 PushToken、Backspace、Space、Enter、Candidate、Schema/provider 切换和资源 generation 变化。
- [ ] 实现 `CapabilityRegistry.resolve(state)` -> `LanguageCapability` -> Engine -> ResourceHandle -> EngineContext -> InputSession。
- [ ] 在同一串行域内关闭旧 session、清空 composing/candidates、解析新 capability、创建新 session。
- [ ] 对字典查询增加 query sequence/generation 检查，限制查询数量并丢弃过期结果。
- [ ] 验证资源失败、InputConnection 失败和无 fallback 时的安全行为。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*InputPipelineTest'`。
- [ ] 提交 `feat: connect capability resolution to input pipeline`。

## Task 7: 完成包安装、升级、卸载和资源 lease

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackImporter.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackManagerImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/language/LanguagePackContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/dictionary/DictionaryUpdater.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/pack/PackageStore.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/pack/PackageLifecycleTest.kt`

**Interfaces:**
- PackageStore 提供 staging/active/version 指针和 `RegistrySnapshot` 原子发布。
- `ResourceHandle<T>` 持有旧版本 lease；DEACTIVATING 禁止新 lease，旧 session 释放后才物理清理。

- [ ] 写测试覆盖 ZIP 越界/重复 entry/Unicode 规范化/大小限制、依赖环、版本冲突、optional 降级和签名/来源约束。
- [ ] 实现解包到 staging、完整校验、immutable snapshot、原子激活和崩溃恢复。
- [ ] 实现升级保留旧版本、卸载前依赖检查、显式 fallback 和 lease 延迟清理。
- [ ] 将 `LanguagePackManager`、`LanguagePackInfo` 和 `LanguagePackManagerImpl` 的旧 `listInstalled/install/get` 接口替换为 `PackageStore`、Manifest validator 和 Registry snapshot 流程；迁移所有调用方后删除旧接口，不保留兼容适配层或双写。
- [ ] 运行 `./gradlew testDebugUnitTest --tests '*PackageLifecycleTest'`。
- [ ] 提交 `feat: add transactional language pack lifecycle`。

## Task 8: 修复设置单一来源并接入设置 Flow

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/activity/SettingsActivity.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/ToolbarSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/ThemeSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/FeedbackSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`
- Modify: `app/src/main/res/values-ja/strings.xml`
- Test: `app/src/test/java/xyz/xiao6/myboard/data/settings/SettingsRepositorySingleSourceTest.kt`

**Interfaces:**
- SettingsActivity 创建一个 `SettingsRepository` 和共享 ViewModel factories，所有路由显式注入。
- Compose 默认参数只允许无状态 callback 或显式 fake，不得调用 `LocalContext`、`remember`、`SettingsDatabase.getInstance()`。

- [ ] 写测试/静态检查证明默认参数不创建真实 Repository，并验证同一 Activity 内各页面观察同一 Flow。
- [ ] 删除 `SettingsScreen` 默认参数中的真实 Repository 创建，保留 Preview/Test 的显式 fake 入口。
- [ ] 将所有设置 route 改为使用 Activity 组装的同一 Repository/Factory，页面本地状态只作为未提交草稿。
- [ ] 验证 IME 服务和设置页通过同一 Room 数据库 Flow 同步 provider、启用包、默认语言和布局覆盖。
- [ ] 为新增设置入口、语言包启用/provider 选择和布局覆盖同步增加 `values`、`values-zh-rCN`、`values-ja` 文案；设置 UI 不得新增硬编码用户可见字符串。
- [ ] 运行设置测试和 `./gradlew testDebugUnitTest`。
- [ ] 提交 `fix: keep settings repository single sourced`。

## Task 9: 清理旧模型并完成 i18n/RTL/布局表现

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/PanelLayoutResolver.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/LayoutRenderer.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/LocaleLayoutSwitchPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/toolbar/LayoutSwitcher.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/state/Script.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/layout/ActionDispatcher.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistry.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/state/OrthogonalRegistryImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolver.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/engine/EngineResourceResolverImpl.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/contract/engine/EngineContracts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/OnboardingViewModel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/onboarding/LanguageSelectionPage.kt`
- Modify: `app/src/main/res/values*/strings.xml`
- Test: `app/src/test/java/xyz/xiao6/myboard/layout/PanelLayoutResolverTest.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/layout/RtlPresentationTest.kt`

- [ ] 使用 `rg "LanguageManifest|LocaleCapability|ScriptCapability|SchemaCapability|enumPayload<Script>|\\bKATA\\b|\\bHANGUL\\b" app/src` 检查上述文件，并清零运行时旧引用；`BuiltInManifests.kt` 已在 Task 4 删除。
- [ ] 让 `ResolvedCapability.scriptDescriptor` 成为 RTL 候选区方向和键盘镜像的唯一来源，不把方向写入正交状态。
- [ ] 验证未知 RTL Script 缺失 descriptor 时 Manifest 被拒绝，已声明 `ARAB` 能驱动镜像和候选区方向。
- [ ] 补齐 RTL/Script/错误状态相关 i18n 文案，不在 UI 中硬编码用户可见文本；设置入口新增文案由 Task 8 同步写入 `app/src/main/res/values*/strings.xml`。
- [ ] 运行布局测试、`./gradlew lintDebug` 和 `./gradlew testDebugUnitTest`。
- [ ] 提交 `refactor: remove legacy capability references`。

## Task 10: 外部 Language Pack 导入与最终回归

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/pack/LanguagePackImporter.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/LanguageSettingsScreen.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/pack/ExternalLanguagePackTest.kt`
- Test: `app/src/androidTest/java/xyz/xiao6/myboard/GlobalInputFlowTest.kt`

**Interfaces:**
- 导入流程只接受声明/数据资源，复用 PackageStore、Manifest validator 和 Registry snapshot。
- 设置页通过统一 Repository 保存启用包/provider 偏好，IME 通过 Flow 重建 capability/session。

- [ ] 写端到端测试：导入含 `THAI`/`ARAB`/`DEVA` 或未知合法 Script 的包，完成校验、注册、选择和输入。
- [ ] 实现用户导入入口、启用/停用、provider 选择、错误展示和显式 fallback。
- [ ] 验证包升级/卸载时 active session 不崩溃，旧资源 lease 正确延迟清理。
- [ ] 无 Android 设备时，将升级/卸载回退用例放入 `app/src/test/java/xyz/xiao6/myboard/pack/PackageLifecycleTest.kt`，使用 fake `PackageStore`、Registry snapshot 和 ResourceHandle lease 完成 JVM 验证；不得将未执行的 connected test 标记为通过。
- [ ] 运行完整验证：`./gradlew testDebugUnitTest lintDebug assembleDebug connectedDebugAndroidTest`（设备可用时）。
- [ ] 提交 `feat: support external language pack import`。

## Final Verification Checklist

- [ ] 使用 `rg "LanguageManifest|LocaleCapability|ScriptCapability|SchemaCapability|enumPayload<Script>|\\bKATA\\b|\\bHANGUL\\b" app/src` 清零旧 Manifest、封闭 Script 和旧 Script 标识的运行时引用；另用 `rg "layoutId: String" app/src` 确认不存在裸默认布局契约。
- [ ] `./gradlew testDebugUnitTest` 通过。
- [ ] `./gradlew lintDebug` 通过。
- [ ] `./gradlew assembleDebug` 成功生成 `app/build/outputs/apk/debug/app-debug.apk`。
- [ ] 设备可用时 `./gradlew connectedDebugAndroidTest` 通过；不可用时记录原因，不宣称通过。
- [ ] 手工验证：启动初始化顺序、Locale/Script/Schema 切换、拼音/双拼/T9/日文罗马字、RTL 布局、设置同步、包失败回退。
- [ ] 复查 `git diff`，确保未覆盖用户已有 onboarding/settings UI 修改。
