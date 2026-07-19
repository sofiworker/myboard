# MyBoard 语言能力与资源系统设计

**日期：** 2026-07-19

**状态：** Design

**范围：** Language Pack、Dictionary、Engine、Layout 与输入链路接入

**前置背景：** 当前项目已使用 `Locale + Script + Schema` 的正交状态模型，并已实现输入引擎、布局、字典和 Manifest 的初步契约。当前输入 Pipeline 尚未完成从能力解析到 `InputSession` 的运行时接入。

## 1. 目标与非目标

### 1.1 目标

- 保持 `OrthogonalState` 只表达输入语义，不引入包路径、字典路径或引擎实现细节。
- 支持中英日作为内置语言包，并为后续新增韩语、阿拉伯语、泰语、越南语、印地语等语言提供统一扩展路径。
- 允许一个语言包声明多个 Script、Schema 和输入能力。
- 允许语言包携带或引用字典、映射、FSM、编码器配置和专用布局。
- 允许字典和布局被多个语言包复用。
- 只允许应用内置引擎或受信任 Engine Plugin 提供可执行输入算法。
- 让 `InputPipeline` 根据当前正交状态解析能力、资源和引擎，并创建正确的 `InputSession`。
- 让内置包、用户导入包和未来下载包遵循同一套校验、注册、卸载和回退流程。
- 在安装、卸载、资源损坏和能力冲突时保持 IME 可回退，不因单个包导致服务崩溃。

### 1.2 非目标

- 本设计不实现 Engine Plugin 的 APK 接口、签名协议或动态代码加载。
- 本设计不改变主题包、LLM、STT、剪贴板和文本扩展的业务模型。
- 本设计不把 Android `InputMethodSubtype` 当作内部状态源。
- 本设计不要求第一阶段实现网络商店或在线包目录。
- 本设计不允许语言包携带 Kotlin、Java、DEX、SO、脚本或其他可执行代码。

## 2. 核心原则

### 2.1 语义状态与能力提供者分离

`OrthogonalState` 继续只包含三个正交轴：

```kotlin
data class OrthogonalState(
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
)
```

它表示用户希望如何输入，例如：

```text
zh-CN + HANI + PINYIN
zh-CN + HANI + SHUANGPIN_ZIRAN
ja-JP + HIRA + ROMAJI
en-US + LATN + LATIN_DIRECT
```

以下内容不得进入 `OrthogonalState`：

- `packageId`
- 字典路径
- Mapping/FSM 路径
- Layout 文件路径
- Kotlin 类名或插件入口

能力包是状态的提供者，而不是状态轴。运行时通过能力目录把状态解析为具体实现。

### 2.2 数据包不提供可执行代码

Language Pack 只能提供声明和数据。`EngineRegistry` 中的内置引擎负责执行逻辑；未来新算法通过独立、受信任、签名的 Engine Plugin 接入，不走普通语言包导入流程。

### 2.3 注册时校验，运行时回退

Manifest、依赖、资源路径、资源完整性、引擎 ID、布局 ID、策略 ID 和版本兼容性必须在注册前校验。运行时资源加载仍需防御性处理，并在失败时回退到可用能力，不允许因为包损坏导致 IME 崩溃。

### 2.4 语言包是用户安装单位，资源是运行时管理单位

用户可以安装一个语言包获得完整能力；系统内部仍分别管理 Language Capability、Dictionary、Layout、Mapping 和 FSM，以支持复用、缓存、升级和依赖关系。

### 2.5 Script 是开放标准标识

`Script` 不使用封闭 Kotlin `enum`。它表示 ISO 15924 风格的四字母文字系统标识，语言包可以声明应用未预置的合法 Script。常用值提供内置常量，但内置常量不是第三方包的注册白名单。

```kotlin
@JvmInline
@Serializable
value class Script private constructor(val value: String) {
    companion object {
        val LATN = Script("LATN")
        val HANI = Script("HANI")
        val HIRA = Script("HIRA")
        val KANA = Script("KANA")
        val HANG = Script("HANG")
        val ARAB = Script("ARAB")
        val THAI = Script("THAI")
        val DEVA = Script("DEVA")

        fun parse(raw: String): Script
    }
}
```

当前 `KATA` 统一为 `KANA`，`HANGUL` 统一为 `HANG`。越南语通常使用 `LATN`，韩语使用 `HANG`，阿拉伯语/波斯语/乌尔都语通常使用 `ARAB`，印地语使用 `DEVA`。

开放 Script 只代表标识合法，不代表当前一定存在可运行能力。未知 Script 由能力注册表决定是否可用，不能因为不在内置目录中就被状态层拒绝。

## 3. 领域模型

### 3.1 PackageId 与版本

所有包使用稳定的命名空间 ID：

```kotlin
data class PackageIdentity(
    val packageId: String,
    val version: SemVer
)
```

`packageId` 必须全局唯一，内置包也使用包 ID，例如：

```text
builtin.en-us
builtin.zh-cn
builtin.ja-jp
builtin.zh-dictionary
```

Manifest 必须声明 `manifestVersion`、`packageVersion`、`minAppVersion` 和可选的资源 API 版本。

### 3.2 Script 与 ScriptCatalog

```kotlin
data class ScriptDescriptor(
    val script: Script,
    val displayNames: Map<String, String>,
    val direction: TextDirection,
    val layoutMirror: LayoutMirrorPolicy,
    val preferredFont: String? = null
)

enum class TextDirection { LTR, RTL }
enum class LayoutMirrorPolicy { NONE, MIRROR_HORIZONTAL }
```

`ScriptCatalog` 只提供常用 Script 的本地化名称、书写方向、镜像策略、字体和默认 UI 元数据，不决定 Script 是否允许注册。Language Pack 的 `ScriptManifest.descriptor` 是运行时权威来源；未知 Script 必须在包内显式提供 descriptor，不能静默使用 LTR。对于内置 Script，包未覆盖的可选显示字段可以回退到 ScriptCatalog，但 `direction` 和 `layoutMirror` 必须有确定值。

外部 Manifest 的 scripts 使用数组，而不是依赖 enum 或 JSON 动态对象 key：

```json
{
  "scripts": [
    {
      "id": "LATN",
      "direction": "LTR",
      "layoutMirror": "NONE",
      "defaultSchema": "LATIN_DIRECT"
    },
    {
      "id": "ARAB",
      "direction": "RTL",
      "layoutMirror": "MIRROR_HORIZONTAL",
      "defaultSchema": "ARABIC_DIRECT"
    }
  ]
}
```

对应模型：

```kotlin
data class ScriptManifest(
    val id: Script,
    val descriptor: ScriptDescriptor,
    val defaultSchema: Schema
)
```

解析后再在内存中建立 `Map<Script, ScriptManifest>` 索引。

### 3.3 逻辑资源引用与已解析资源身份

Manifest 中保存逻辑资源引用。逻辑引用描述依赖目标，但不代表某个已安装版本的物理文件：

```kotlin
data class ResourceRef(
    val packageId: String,
    val path: String,
    val kind: ResourceKind,
    val versionRange: VersionRange? = null,
    val sha256: String? = null,
    val onMissing: MissingResourcePolicy = MissingResourcePolicy.REJECT_PACKAGE
)

enum class MissingResourcePolicy {
    REJECT_PACKAGE,
    DISABLE_CAPABILITY,
    USE_CAPABILITY_FALLBACK
}

enum class ResourceKind {
    DICTIONARY,
    FREQUENCY,
    MAPPING,
    FSM,
    ENCODER_CONFIG,
    LAYOUT,
    I18N,
    MODEL
}
```

路径始终是包内相对路径。解析器不得接受绝对路径、`..` 越界路径或包外路径。

安装和运行时解析后生成不可变资源身份：

```kotlin
data class ResolvedResourceKey(
    val packageId: String,
    val packageVersion: SemVer,
    val normalizedPath: String,
    val kind: ResourceKind,
    val sha256: String
)
```

`InputSession` 不直接持有文件路径，而是持有 `ResourceHandle<T>`。Handle 对已解析资源版本建立 lease；包升级后旧 session 可以继续读取旧版本，只有所有 lease 释放后，旧版本资源才允许清理。

`USE_CAPABILITY_FALLBACK` 要求所属 `LanguageCapability.fallbackCapabilityIds` 非空；`DISABLE_CAPABILITY` 会让该 capability 不进入可用索引。Layout、FSM、Mapping、Encoder Config 等必需资源默认使用 `REJECT_PACKAGE`，不能因为 optional 包缺失而被隐式忽略。

### 3.4 CapabilityId

能力身份保留包信息：

```kotlin
data class CapabilityId(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
)
```

`CapabilityId` 是能力目录和 `InputSession` 的身份，不是新的正交状态轴。

### 3.5 LanguagePackManifest

```kotlin
data class LanguagePackManifest(
    val manifestVersion: Int,
    val identity: PackageIdentity,
    val minAppVersion: SemVer,
    val locale: LocaleTag,
    val displayName: LocalizedText,
    val defaults: LocaleDefaults,
    val scripts: List<ScriptManifest>,
    val dependencies: List<PackageDependency>,
    val capabilities: List<LanguageCapability>
)
```

一个语言包可以包含多个 Script 和 Schema。语言包不要求一个 Locale 只能有一个包；同一状态由多个包提供时，由能力选择策略决定当前 provider。

`defaults` 定义 Locale 默认 Script、Schema 和基础布局；每个 `ScriptManifest` 定义该 Script 的方向、镜像策略和默认 Schema。初始化、切换 Locale 和切换 Script 都必须从当前 provider 的默认值解析。外部 provider 默认不能覆盖内置默认值，除非用户显式选择它为该 Locale 的默认能力来源。

### 3.6 LanguageCapability

```kotlin
data class LanguageCapability(
    val id: CapabilityId,
    val engine: EngineBinding,
    val layout: ResourceRef,
    val dictionaries: List<DictionaryBinding>,
    val mapping: ResourceRef? = null,
    val fsm: ResourceRef? = null,
    val candidatePolicyId: String,
    val displayPolicyId: String?,
    val supportsShift: Boolean,
    val outputScript: Script? = null,
    val subtype: SubtypeInfo? = null,
    val fallbackCapabilityIds: List<CapabilityId> = emptyList()
)
```

能力解析后生成只读的运行时投影，供布局、候选栏和输入服务消费：

```kotlin
data class ResolvedCapability(
    val capability: LanguageCapability,
    val scriptDescriptor: ScriptDescriptor,
    val layoutKey: LayoutKey,
    val resources: ResolvedResources
)
```

`scriptDescriptor.direction` 和 `layoutMirror` 只能通过该解析结果进入渲染层；Layout 文档本身不读取 Locale、Script 或 Settings。候选栏、工具栏和键盘布局根据 `ResolvedCapability` 的 presentation 投影决定 RTL 排列和水平镜像，`OrthogonalState` 不增加方向字段。

引擎绑定只引用已注册的引擎和资源：

```kotlin
data class EngineBinding(
    val engineId: String,
    val encoderId: String? = null,
    val encoderConfig: ResourceRef? = null
)
```

字典必须声明角色和必需性：

```kotlin
data class DictionaryBinding(
    val role: DictionaryRole,
    val resource: ResourceRef,
    val required: Boolean
)

enum class DictionaryRole {
    PRIMARY,
    CONVERSION,
    FREQUENCY,
    SPELLING,
    EMOJI
}
```

这覆盖现有 `dictionaryOptional`、`conversionDictionary` 和不同候选资源的职责。`subtype` 保留内置能力导出 Android subtype 所需元数据；外部包仍不参与构建期 `method.xml` 生成。

### 3.7 Dictionary

字典是可复用的只读运行时资源。字典可以随语言包提供，也可以由独立 Dictionary Pack 提供。

```kotlin
enum class DictionaryKind {
    WORD,
    PHRASE,
    CONVERSION,
    FREQUENCY,
    SPELLING,
    EMOJI
}
```

用户词典不属于 Language Pack，单独存储并通过统一 `Dictionary` 接口参与候选查询。

### 3.8 包依赖

```kotlin
data class PackageDependency(
    val packageId: String,
    val versionRange: VersionRange,
    val optional: Boolean = false
)
```

`VersionRange` 第一阶段采用明确的 SemVer 区间，不接受任意表达式，例如 `>=1.2.0 <2.0.0`。安装器必须拒绝依赖环、版本区间无交集和缺失的 required 依赖。

Optional 依赖只有在所有指向该包的 `ResourceRef` 都明确使用 `DISABLE_CAPABILITY` 或 `USE_CAPABILITY_FALLBACK` 时才合法；否则 Manifest 校验失败。安装器通过资源引用反向索引确定受影响 capability，不允许存在“optional 但缺失后行为未知”的依赖。

### 3.9 Layout

Layout 只描述键位结构、显示内容、动作、层级、命中区域和测量属性。Layout 不读取语言设置，不查询字典，也不实现输入算法。

语言包可以：

- 引用内置公共布局，如 `builtin/qwerty`。
- 引用另一个 Layout Pack 的布局。
- 携带语言专用布局，并在导入时注册到 `LayoutRegistry`。

无论来源如何，布局注册、校验、查询和卸载都由 `LayoutRegistry` 统一管理。

布局运行时使用规范化身份，不再以裸文件名作为全局 ID：

```kotlin
data class LayoutKey(
    val packageId: String,
    val layoutId: String,
    val packageVersion: SemVer
)
```

Manifest 中的 `ResourceRef` 解析为 `LayoutKey`，`KeyboardContext` 第一阶段保存可序列化的 canonical ID，例如 `builtin:qwerty`。不同包提供同名 `qwerty` 时不会覆盖。用户布局覆盖必须通过目标 capability 的 token/action 契约校验。

### 3.10 Engine

第一阶段只允许以下内置引擎：

```text
direct
table_composing
transliteration
```

`InputEngine` 是无状态工厂，`InputSession` 保存运行时 buffer、候选和 FSM 状态。Language Pack 只能引用 `engineId`，不能提供新的可执行实现。

## 4. 包结构

### 4.1 Language Pack

```text
language-pack/
  manifest.jsonc
  i18n/
  capabilities/
  mappings/
  fsm/
  dictionaries/       # 可选，适合包内专用小资源
  layouts/            # 可选，适合语言专用布局
```

### 4.2 Dictionary Pack

```text
dictionary-pack/
  manifest.jsonc
  dictionaries/
  frequency/
  licenses/
```

多个 Language Pack 可以依赖同一个 Dictionary Pack。

Dictionary Pack Manifest 至少声明包身份、版本、资源清单、字典角色、适用 Locale/Script、许可证和 hash，不声明语言状态默认值。

### 4.3 Layout Pack

```text
layout-pack/
  manifest.jsonc
  layouts/
  previews/
```

Layout Pack 不绑定某个具体语言，除非 Manifest 显式声明适用范围。

Layout Pack Manifest 至少声明包身份、版本、布局资源清单、兼容的 Layout schema 版本和 hash。它不声明 Engine、Dictionary 或正交状态默认值。

### 4.4 Engine Plugin

Engine Plugin 是未来独立设计，至少需要：

- 独立 APK 或受信任模块。
- 签名校验。
- Engine API 版本。
- 明确的 `engineId`。
- 不允许普通 ZIP Language Pack 动态加载代码。

## 5. 注册与能力解析

### 5.1 注册表结构

当前 `OrthogonalRegistry` 只按 `OrthogonalState` 保存单个 `SchemaCapability`，需要扩展为：

```text
OrthogonalState -> List<CapabilityId>
CapabilityId -> LanguageCapability
ResourceRef -> ResolvedResourceKey -> ResourceHandle
```

第一阶段每个状态可以只有一个 provider，但数据结构必须允许多个 provider。

### 5.2 Provider 选择

Locale 初始化和 `switchLocale()` 先选择 Locale 级默认 provider，再从该 provider 的 Manifest 读取默认 Script/Schema：

```kotlin
data class LocaleDefaultProviderPreference(
    val locale: LocaleTag,
    val packageId: String
)
```

Locale 默认 provider 的确定性顺序：

1. 用户在设置中显式选择的 Locale 默认 provider。
2. 提供该 Locale 且依赖完整的内置包。
3. 已启用外部包，按 `packageId` 升序选择；同包使用最高兼容版本。

选定 Manifest 后读取 `defaults` 构造完整 `OrthogonalState`，然后才进入按完整状态解析 capability provider 的流程。这样不存在“先知道完整状态还是先选择 provider”的循环。

能力选择必须确定且可复现，优先级为：

1. 用户显式选择的 provider。
2. 当前已启用且满足依赖的内置 provider。
3. 当前已启用的外部 provider，按 `packageId` 升序作为稳定 tie-breaker；版本只在同一个 `packageId` 内选择最高兼容版本。
4. capability 显式声明的 `fallbackCapabilityIds`，按声明顺序解析。

provider 选择结果不写入 `OrthogonalState`。如果需要持久化，使用单独设置键保存：

```text
capability_provider.zh-CN.HANI.PINYIN = builtin.zh-cn
locale_default_provider.zh-CN = builtin.zh-cn
```

不存在“仅凭同 Locale 自动兼容”的隐式回退。`HANI/PINYIN` 不能因为资源缺失而静默切到 `LATN/LATIN_DIRECT`；跨 Script 或跨 Schema 回退必须由 Manifest 显式声明，或者由用户确认。失效的用户 provider 偏好保留为不可用记录并回退运行，但设置页必须显示问题，不能静默覆盖用户选择。

所有 provider、已启用语言包、默认语言和布局覆盖设置都通过同一个 `SettingsRepository` 持久化并以 `Flow` 提供。设置键必须类型化封装，所有入口都必须在 `SettingsActivity.kt` 可达，不允许 IME 服务和设置页维护各自副本。

### 5.3 注册顺序

IME 服务初始化必须调整为：

```text
1. 创建 EngineRegistry
2. 注册内置 Engine
3. 创建 Policy Registries
4. 创建 DictionaryRegistry 并注册内置字典
5. 创建 LayoutRegistry 并注册内置布局
6. 加载并校验内置 Language Pack
7. 注册 LanguageCapability
8. 创建 KeyboardContextManager
9. 创建 InputPipeline
```

`KeyboardContextManager` 不得在 Manifest 注册前创建并依赖硬编码回退状态。

### 5.4 安装流程

包安装由持久化 `PackageStore` 管理，使用 staging/activation 流程：

```text
解包到私有临时版本目录
  -> 完整校验 Manifest、路径、大小、依赖、hash 和引用
  -> 构建未发布的 immutable RegistrySnapshot
  -> 持久化 staged 安装记录
  -> 原子重命名为版本目录并切换 active-version 指针
  -> 一次性发布新的 RegistrySnapshot
```

校验或激活失败时删除 staging 目录并保留旧 active 版本。进程在 staging 阶段死亡时，下一次启动清理未激活记录；进程在 active 指针切换后死亡时，以持久化 active 版本重建完整 RegistrySnapshot。内存 Registry 不允许逐项暴露半注册状态。

升级不会覆盖旧版本目录。旧版本从 active 索引移除后仍保留，直到所有 `ResourceHandle` lease 和 InputSession 引用释放，再由 PackageStore 清理。

### 5.5 卸载流程

卸载同样由 PackageStore 串行执行。卸载前必须检查：

- 是否有其他包依赖该包资源。
- 当前 active capability 是否由该包提供。
- 是否存在可用替代能力。

通过检查后执行：

```text
持久化 packageState = DEACTIVATING
  -> 阻止新 snapshot 和新 session 获取该包资源
  -> 构建移除该包后的 RegistrySnapshot
  -> 若当前 session 使用该包，在 Pipeline 串行域切换到显式 fallback
  -> 原子发布新 snapshot 并持久化 packageState = LOGICALLY_UNINSTALLED
  -> 等待所有 ResourceHandle lease 释放
  -> 删除物理版本目录并持久化 packageState = REMOVED
```

非当前 session 或后台任务仍持有 lease 时，不拒绝逻辑卸载，但延迟物理清理。`DEACTIVATING` 状态禁止创建新 lease，避免依赖检查后又有新 session 获取待卸载资源。

进程恢复规则：

- `DEACTIVATING` 且旧 snapshot 仍为 active：回滚为 INSTALLED。
- `DEACTIVATING` 且新 snapshot 已持久化：继续完成逻辑卸载。
- `LOGICALLY_UNINSTALLED`：不重新注册该包；进程重启后不存在旧的内存 lease，可直接完成物理清理。
- active session 恢复时如果 capability 已不可用，按显式 fallback 重建，不恢复旧 composing。

如果当前能力被卸载：

```text
关闭旧 InputSession
  -> 清空 composing/candidates
  -> 选择兼容回退 capability
  -> 更新 KeyboardContext
  -> 创建新 session
```

## 6. 输入链路接入

### 6.1 解析流程

```text
KeyboardContext.orthogonal
  -> CapabilityRegistry.resolve(state)
  -> LanguageCapability
  -> EngineRegistry.get(engine.engineId)
  -> ResourceResolver.resolve(capability)
  -> EngineContext
  -> InputEngine.createSession(context)
```

Pipeline 不得使用 `schema.value` 作为引擎 ID。`Schema` 与 `engineId` 是不同概念：

```text
Schema:   PINYIN
Engine:   table_composing
```

### 6.2 Session 生命周期

Pipeline 使用独立的已解析运行时身份判断是否重建 session：

```kotlin
data class ResolvedCapabilityKey(
    val capabilityId: CapabilityId,
    val packageVersion: SemVer,
    val resourceGeneration: Long
)
```

以下任一情况都会改变该身份：

- Locale、Script 或 Schema 变化。
- 用户切换 provider。
- 语言包启用或停用。
- 包升级或 active version 改变。
- 依赖资源版本改变或资源失效。

任何 `ResolvedCapabilityKey` 变化都必须在 Pipeline 的同一串行域内执行：

1. 串行关闭旧 session。
2. 清理旧 composing 和候选。
3. 解析新 capability。
4. 加载或获取缓存资源。
5. 创建新的 `EngineContext`。
6. 创建新的 `InputSession`。

每个 Pipeline 使用 `Mutex` 或 actor 串行处理 `InputAction`。字典查询必须限制数量、支持 query sequence，并丢弃旧 session 的迟到结果。

Capability Registry 以不可变 snapshot 和递增 `resourceGeneration` 发布变化。Pipeline 观察 snapshot/设置流，不依赖 Compose 重组触发 session 更新。

### 6.3 Layout 与 Pipeline

Layout 只产生 `InputAction`：

```text
Layout key
  -> InputAction.PushToken / Delete / Space / Enter / CommitCandidate
  -> InputPipeline
```

Pipeline 才负责将 Action 转换成 InputEvent、调用 Session 和执行 EngineResult。布局不能直接调用 `InputConnection` 或字典。

布局中的 `switch_script` action 使用字符串 Script ID：

```json
{ "action": "switch_script", "script": "ARAB" }
```

`ActionDispatcher` 通过 `Script.parse` 解析；`LayoutRegistry` 通过开放值校验器检查格式，不再调用 `enumPayload<Script>()`。解析成功只表示 Script 标识合法，`TransitionEngine` 仍需通过当前 Locale 的能力目录判断是否支持该 Script。

### 6.4 KeyboardContext 投影

第一阶段保留当前 `KeyboardContext.layoutId` 字段，但约束为：

- `orthogonal` 是语义状态。
- `layoutId` 是当前 capability 的 canonical `LayoutKey` 投影。
- Schema 切换后由 capability resolver 更新默认布局。
- 用户自定义布局以后通过独立的布局偏好覆盖。
- `ResolvedCapability.scriptDescriptor` 是当前布局和候选区方向/镜像策略的唯一运行时来源。

后续如支持主题布局绑定，再引入 `baseLayoutId` 与 `effectiveLayoutId`，不把它们加入正交状态。

## 7. 内置包迁移

中英日先改造成同一模型下的内置包：

```text
assets/packs/builtin.en-us/manifest.jsonc
assets/packs/builtin.zh-cn/manifest.jsonc
assets/packs/builtin.ja-jp/manifest.jsonc
assets/dictionaries/builtin.zh/pinyin.dict
assets/layouts/builtin/qwerty.jsonc
assets/layouts/builtin/shuangpin_ziran.jsonc
assets/layouts/builtin/t9_chinese.jsonc
```

`BuiltInManifests` 可以在迁移期作为内置数据适配器保留，但注册流程必须与未来外部 Language Pack 使用同一套校验和能力注册接口。

中文能力示例：

```text
builtin.zh-cn / zh-CN / HANI / PINYIN
  engine: table_composing
  encoder: identity
  dictionary: builtin.zh/pinyin
  layout: builtin/qwerty

builtin.zh-cn / zh-CN / HANI / SHUANGPIN_ZIRAN
  engine: table_composing
  encoder: shuangpin_ziran
  mapping: builtin.zh-cn/mappings/ziran.json
  dictionary: builtin.zh/pinyin
  layout: builtin/shuangpin_ziran
```

## 8. 失败与回退

### 8.1 安装期

- Manifest 非法：拒绝整包。
- 每个 `ScriptManifest` 必须提供可解析的 `direction` 和 `layoutMirror`；未知 RTL Script 缺少方向声明时拒绝该 Manifest。
- `scripts` 数组中的 Script ID 必须唯一；`descriptor.script` 必须与 `id` 一致；每个 `defaultSchema` 必须对应 capability；所有 `CapabilityId` 必须唯一，否则拒绝 Manifest。
- 依赖缺失：拒绝整包，除非依赖标记为 optional 且能力可以安全降级。
- 引擎 ID 不存在：拒绝引用该能力，必要时拒绝整包。
- 布局或字典缺失：拒绝依赖该资源的能力。
- ZIP 越界、重复路径或超限：拒绝整包。

### 8.2 运行期

- 资源加载失败：当前 capability 标记不可用。
- 字典查询失败：返回空候选，不阻塞直接输入。
- InputConnection 操作失败：关闭 session 并清空 composing。
- 当前包卸载：只按显式 `fallbackCapabilityIds` 回退；没有兼容能力时停止当前组合输入并要求用户选择可用能力。
- 旧资源查询返回：通过 session generation 丢弃。

## 9. 安全与版本

- 普通 Language Pack 不允许包含代码、DEX、SO、脚本或反射入口。
- `builtin.*` 命名空间仅允许 APK 内置并由应用签名信任的包使用，外部导入包不得抢占。
- 所有资源路径必须规范化并限制在包目录内。
- 路径校验必须处理大小写冲突、Unicode 规范化、重复 entry、符号链接和解压炸弹。
- 包、Manifest 和资源必须有大小限制。
- 字典、模型和图片资源必须有独立大小限制。
- 需要记录许可证和来源，尤其是第三方词库。
- `manifestVersion` 和 `resourceApiVersion` 用于兼容校验。
- 语言包升级采用新版本原子替换，不覆盖正在使用的旧资源，直到新包验证成功。

## 10. 测试要求

### 10.1 契约与注册

- 合法 Language Pack 注册成功。
- 缺失 Manifest、依赖、字典、布局或 FSM 时注册失败。
- 不存在的 engineId、encoderId、policyId 被拒绝。
- 同一 `OrthogonalState` 的多个 provider 能按优先级解析。
- Locale 切换先按 LocaleDefaultProviderPreference 选择 Manifest，再解析默认 Script/Schema。
- `THAI`、`ARAB`、`DEVA` 等常用 Script，以及未来不在内置常量目录中的合法 Script，都可以完成 Manifest 解析；格式非法的 Script 被拒绝。
- `KANA` 和 `HANG` 使用新的标准标识，Manifest 不再依赖 `KATA` 或 `HANGUL` enum 名称。
- 未知 RTL Script 的 descriptor 会驱动 RTL 候选区和键盘镜像；缺少 capability 时不会污染 `OrthogonalState`。
- 重复 Script ID、descriptor/script 不一致、缺失 default capability 和重复 CapabilityId 都会在数组转索引前被拒绝，不允许静默覆盖。
- 包卸载后 capability 和资源索引被清理。
- 同优先级 provider 使用稳定 tie-breaker，重复解析结果一致。
- 依赖环、版本区间冲突和 optional 降级分别被正确处理。
- Optional 依赖存在 REJECT_PACKAGE 引用时 Manifest 校验失败。

### 10.2 输入 Pipeline

- `PINYIN` 使用 `table_composing`，而不是把 `PINYIN` 当作 engineId。
- Schema 切换会关闭旧 session 并创建新 session。
- PushToken、Backspace、Space、Enter 和 Candidate 事件串行处理。
- 字典迟到结果不能覆盖新 session。
- 资源异常时能回退并清空 composing。
- provider 切换、包升级和资源 generation 变化会在正交状态不变时重建 session。
- 输入事件与 provider 切换、升级、卸载并发时仍保持单一串行顺序。

### 10.3 资源复用

- 多个 Schema 可以共享同一个 Dictionary。
- 多个 Locale 可以引用同一个 Layout。
- 语言包内置 Layout 与独立 Layout Pack 均通过 `LayoutRegistry` 注册。
- 删除资源被依赖时卸载失败或先完成迁移。
- 旧 session 持有 lease 时旧包版本不会被清理。

### 10.4 安装、升级与恢复

- 新版本验证失败后旧 active 版本继续可用。
- 安装、激活和卸载中途进程死亡后可从 PackageStore 恢复。
- DEACTIVATING 包不能创建新 lease，逻辑卸载后等待旧 lease 再清理物理文件。
- RegistrySnapshot 只发布完整版本，不暴露半注册状态。
- `builtin.*` 命名空间抢占被拒绝。
- ZIP 大小写冲突、Unicode 规范化冲突、重复 entry 和压缩炸弹被拒绝。

### 10.5 内置包回归

- 中英日内置包都能完成安装和默认 capability 解析。
- 中文拼音、双拼、T9、英文直输和日文罗马字分别解析到正确引擎。
- 内置包加载顺序不会产生硬编码初始状态回退。
- 现有 `dictionaryOptional`、`conversionDictionary`、`outputScript` 和 subtype 元数据完成无损迁移。
- 未知但格式合法的 Script 可被能力目录保存和查询；缺少能力时不会被状态层误认为非法。
- `ARAB` 等 RTL Script 的 descriptor 会驱动键盘镜像和候选区方向，且不污染 `OrthogonalState`。

### 10.6 设置同步

- provider、已启用语言包、默认语言和布局覆盖均从统一 SettingsRepository 的 Flow 驱动。
- SettingsActivity 中存在所有相关设置入口。
- IME 服务重建前后不会产生两份设置状态。

## 11. 实施顺序

1. 将 `Script` 从 enum 改为开放标准值类型，统一 `KANA`/`HANG` 标识并增加 `ScriptCatalog`。
2. 定义 `ScriptManifest`、方向/镜像策略和 `ResolvedCapability` presentation 投影。
3. 定义 `ResourceRef`、Package Identity、Manifest 和 Capability 模型。
4. 将外部 Manifest 的 scripts 改为数组，扩展 Script 解析、布局动作和校验逻辑。
5. 扩展 `OrthogonalRegistry`，支持 `state -> capabilities` 和 provider 选择。
6. 为 `DictionaryRegistry`、`LayoutRegistry` 和资源解析器增加包身份。
7. 将内置中英日数据适配为内置 Language Pack。
8. 调整 IME 初始化顺序，先注册能力再创建 Context Manager。
9. 为 `InputPipeline` 注入 Capability Registry 和 Resource Resolver。
10. 实现 EngineContext 和 InputSession 创建、关闭、串行处理。
11. 增加 Pipeline、Manifest、开放 Script、RTL presentation、资源依赖和回退测试。
12. 在完整输入链路稳定后，再实现外部 Language Pack 导入。
13. 最后评估独立 Dictionary Pack、Layout Pack 和 Engine Plugin。

## 12. 设计结论

MyBoard 采用以下稳定边界：

```text
LanguagePack
  = 用户安装和启用的语言能力集合

Dictionary
  = 可共享的候选和语言资源

Engine
  = 可执行的输入算法

Layout
  = 可复用的键盘结构和动作资源
```

依赖方向保持为：

```text
LanguagePack
  -> 引用 Dictionary
  -> 引用 Layout
  -> 引用 Engine ID
  -> 提供 Engine 所需的数据资源
```

正交状态仍然只有 `Locale + Script + Schema`。包身份、资源路径和实现细节属于能力解析层，不会成为新的状态轴。
