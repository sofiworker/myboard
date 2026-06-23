# MyBoard 正交状态管理与转移规则详细设计

> 版本：v1.0  
> 状态：Draft  
> 日期：2026-06-15  
> 定位：本文档是 MyBoard 状态层的唯一实现标准。  
> 依据：`docs/core.md` 中“正交状态矩阵”概览。允许破坏性重构当前实现。

## 1. 背景与目标

MyBoard 的长期目标是构建一套全球化、高扩展性的输入法状态体系。传统输入法常用“语言 -> 模式 -> 布局”的链式状态组织方式，例如“中文输入法里切英文”“日文输入法里切假名/罗马字”等场景都容易把语言、文字系统、输入方案混在一起，最终导致状态不可组合、切换逻辑分散、UI 与输入逻辑互相耦合。

本设计采用正交状态矩阵，将键盘运行时的核心输入状态拆成三个独立维度：

- `Locale`：语境或语言区域，例如 `zh-CN`、`en-US`、`ja-JP`。
- `Script`：目标输出文字系统，例如 `HANI`、`LATN`、`HIRA`、`KATA`。
- `Schema`：输入方案，例如 `PINYIN`、`DOUBLE_PINYIN`、`LATIN_DIRECT`、`ROMAJI`。

三者共同组成唯一正交输入状态：

```text
OrthogonalState = Locale + Script + Schema
```

设计目标：

- 以 `KeyboardContext` 作为键盘运行时的唯一状态源。
- 所有状态转移必须经过合法矩阵校验。
- 输入引擎、布局、字典、候选栏、Android Subtype 都从同一份状态派生，不维护重复变量。
- 支持未来中英日、多输入方案、语音、手写和插件化扩展。
- 以本文档为实现标准，允许删除、重命名、重建当前不符合设计的代码。

## 2. 破坏性重构原则

当前仓库已有键盘状态和输入引擎雏形，但状态模型仍偏平面化：

```kotlin
data class KeyboardState(
    val languageId: String = "en_us",
    val inputMethodId: String = "en_qwerty",
    val arrangement: String = "alpha",
    val shiftState: ShiftState = ShiftState.OFF,
    val capsLock: Boolean = false,
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val activePanel: PanelType = PanelType.NONE
)
```

当前实现中与本文档不一致的状态模型、切换逻辑和路由逻辑都应直接替换，不做兼容层。

当前主要问题：

- `languageId` 同时承担语境和输入模式含义，例如 `en_us`、`zh_cn`。
- `inputMethodId` 与 `languageId` 有强绑定关系，无法表达“中文语境下直接输入英文”。
- `arrangement` 同时表达布局、输入方案、数字页等概念。
- `shiftState` 和 `capsLock` 分成两个字段，存在状态不一致风险。
- `LanguageSwitchManager` 以语言类型切换为中心，无法表达 `Script` 与 `Schema` 的独立切换。
- `method.xml` 目前只有一个泛 subtype，尚未桥接 Android 系统输入法子类型。

破坏性替换要求：

| 当前字段/模块 | 处理方式 |
| --- | --- |
| `KeyboardState.languageId` | 删除，使用 `KeyboardContext.orthogonal.locale` |
| `KeyboardState.inputMethodId` | 删除，由 `SchemaCapability.engineId` 路由输入引擎 |
| `KeyboardState.arrangement` | 删除，拆为 `KeyboardContext.layoutId` 与 `KeyboardContext.layer` |
| `KeyboardState.shiftState` | 删除，使用 `LayoutLayer.SHIFTED` 或 `LayoutLayer.CAPS_LOCK` |
| `KeyboardState.capsLock` | 删除，使用 `LayoutLayer.CAPS_LOCK` |
| `KeyboardStateManager` | 删除或改名重建为 `KeyboardContextManager` |
| `LanguageSwitchManager` | 删除，由 `TransitionEngine` 统一处理 Locale/Script/Schema 转移 |
| `inputMethodId` 路由 | 删除，改为 Manifest 驱动的 `SchemaCapability` 路由 |
| `SWITCH_MODE` 动作 | 删除，改为 `SWITCH_LOCALE`、`SWITCH_SCRIPT`、`SWITCH_SCHEMA`、`SWITCH_LAYER` |

允许直接修改现有 public/internal 类型签名、JSON action 名称、布局文件字段和测试断言。实现完成后，以本文档定义的类型、Manifest、状态转移和测试为准。

## 3. 正交状态模型

### 3.1 核心类型

```kotlin
@JvmInline
value class LocaleTag(val value: String)

enum class Script {
    LATN,
    HANI,
    HIRA,
    KATA,
    HANGUL
}

@JvmInline
value class Schema(val value: String)

object BuiltInSchemas {
    val LATIN_DIRECT = Schema("LATIN_DIRECT")
    val PINYIN = Schema("PINYIN")
    val DOUBLE_PINYIN = Schema("DOUBLE_PINYIN")
    val ROMAJI = Schema("ROMAJI")
    val VOICE = Schema("VOICE")
    val HANDWRITING = Schema("HANDWRITING")
}

enum class LayoutLayer {
    NORMAL,
    SHIFTED,
    CAPS_LOCK,
    SYMBOL,
    NUMBER
}

data class OrthogonalState(
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
)

data class KeyboardContext(
    val orthogonal: OrthogonalState,
    val layoutId: String,
    val layer: LayoutLayer,
    val composingText: String = "",
    val candidates: List<Candidate> = emptyList(),
    val selectedCandidateIndex: Int = -1,
    val activePanel: PanelType = PanelType.NONE
)
```

### 3.2 状态职责边界

`OrthogonalState` 只表达输入语义，不表达 UI 临时状态：

- 它决定使用哪个输入引擎。
- 它决定加载哪个字典。
- 它决定布局动作如何解释。
- 它决定 Android Subtype 如何同步。

`KeyboardContext` 是完整运行时上下文：

- 包含 `OrthogonalState`。
- 包含当前布局和布局层。
- 包含组合输入、候选词、面板等运行时信息。
- 作为 Compose UI、输入服务、候选栏、布局渲染的唯一观察对象。

## 4. LanguageManifest 与动态能力注册

正交状态模型中的 `Locale`、`Script`、`Schema` 类型可以在代码中定义为稳定概念，但“某个语言支持哪些 Script、某个 Script 支持哪些 Schema、默认 Schema 是什么、使用哪个引擎和字典”不能硬编码在 Kotlin 状态机里。

为避免语言数量增长后维护成本爆炸，系统采用数据驱动和动态注册：

- 语言能力由 `LanguageManifest` 声明。
- 内置语言、下载语言包、插件语言包都通过同一份 Manifest 格式注册。
- `OrthogonalRegistry` 只负责加载、校验、合并 Manifest。
- `TransitionEngine` 只执行通用状态转移规则，不关心中文、英文、日文的具体能力。

### 4.1 LanguageManifest 职责

每个语言包必须包含一个 `LanguageManifest`。它描述该语言包“能做什么”，不描述运行时“当前选中了什么”。

Manifest 负责声明：

- 语言包 ID、版本、Manifest Schema 版本。
- Locale 与本地化显示名称。
- 默认 `Script`、默认 `Schema`、默认 `layoutId`。
- 支持的 Script 列表。
- 每个 Script 下支持的 Schema 列表。
- 每个 Schema 使用的引擎、解析器、字典、布局、Shift 能力。
- 该语言包是否提供 Android Subtype 导出信息。

Manifest 不负责保存：

- 用户当前选择的 Locale。
- 用户当前选择的 Schema。
- 用户是否启用该语言。
- 用户自定义排序和偏好。

这些用户偏好必须由 `SettingsManager` 作为单一来源保存。

### 4.2 Manifest 示例

```json
{
  "manifestVersion": 1,
  "packageId": "language.zh-CN",
  "packageVersion": "1.0.0",
  "locale": "zh-CN",
  "displayName": {
    "zh-CN": "中文",
    "en-US": "Chinese"
  },
  "defaults": {
    "script": "HANI",
    "schema": "PINYIN",
    "layoutId": "qwerty"
  },
  "scripts": {
    "HANI": {
      "defaultSchema": "PINYIN",
      "schemas": {
        "PINYIN": {
          "engineId": "table_composing",
          "encoderId": "identity",
          "dictionary": "dicts/pinyin_main.dict",
          "layoutId": "qwerty",
          "supportsShift": false,
          "candidatePolicy": "chinese_default",
          "displayPolicy": "show_query",
          "subtype": {
            "labelKey": "subtype_zh_pinyin"
          }
        },
        "DOUBLE_PINYIN": {
          "engineId": "table_composing",
          "encoderId": "double_pinyin",
          "encoderConfig": "encoders/double_pinyin_xiaohe.json",
          "dictionary": "dicts/pinyin_main.dict",
          "layoutId": "shuangpin",
          "supportsShift": false,
          "candidatePolicy": "chinese_default",
          "displayPolicy": "show_raw",
          "subtype": {
            "labelKey": "subtype_zh_double_pinyin"
          }
        }
      }
    },
    "LATN": {
      "defaultSchema": "LATIN_DIRECT",
      "schemas": {
        "LATIN_DIRECT": {
          "engineId": "direct",
          "layoutId": "qwerty",
          "supportsShift": true,
          "mapping": "maps/latin_qwerty.json",
          "candidatePolicy": "direct_default",
          "displayPolicy": "hidden",
          "subtype": {
            "labelKey": "subtype_zh_latin"
          }
        }
      }
    }
  }
}
```

英文语言包示例：

```json
{
  "manifestVersion": 1,
  "packageId": "language.en-US",
  "packageVersion": "1.0.0",
  "locale": "en-US",
  "displayName": {
    "zh-CN": "英语",
    "en-US": "English"
  },
  "defaults": {
    "script": "LATN",
    "schema": "LATIN_DIRECT",
    "layoutId": "qwerty"
  },
  "scripts": {
    "LATN": {
      "defaultSchema": "LATIN_DIRECT",
      "schemas": {
        "LATIN_DIRECT": {
          "engineId": "direct",
          "layoutId": "qwerty",
          "supportsShift": true,
          "mapping": "maps/latin_qwerty.json",
          "candidatePolicy": "direct_default",
          "displayPolicy": "hidden",
          "subtype": {
            "labelKey": "subtype_en_direct"
          }
        }
      }
    }
  }
}
```

这些 JSON 是内置语言包或外部语言包的数据示例，不应转换成 Kotlin `when(locale)` 硬编码。

### 4.3 SchemaCapability 字段规范

`SchemaCapability` 是状态层、引擎层、布局层共同依赖的能力契约。字段名必须在所有 Manifest、Kotlin 数据模型和校验逻辑中保持一致。

通用字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `engineId` | 是 | 稳定引擎族 ID，必须能在 `EngineRegistry` 找到 |
| `layoutId` | 是 | 当前 Schema 默认布局，必须能被 `LayoutRegistry` 加载 |
| `supportsShift` | 是 | 是否允许 `SHIFTED` 与 `CAPS_LOCK` 层 |
| `candidatePolicy` | 是 | 控制空格、回车、退格、候选选择和候选展示截断 |
| `displayPolicy` | 否 | composing 显示策略；缺省时由引擎族默认值决定 |
| `subtype` | 否 | 是否生成 Android subtype；只对内置 Manifest 生效 |

按引擎族追加字段：

| engineId | 必填字段 | 可选字段 | 说明 |
| --- | --- | --- | --- |
| `direct` | `mapping` | 无 | 直接按键映射输入 |
| `table_composing` | `encoderId`, `dictionary` | `encoderConfig`, `dictionaryOptional` | 编码查表候选输入；`dictionaryOptional = true` 时可不声明 `dictionary` |
| `transliteration` | `fsm` | `conversionDictionary`, `outputScript` | 规则转写输入 |

字段约束：

- `Schema` 是稳定字符串 ID，不是固定枚举；内置 Schema 只作为常量提供。
- `engineId`、`encoderId`、`candidatePolicy`、`displayPolicy` 都只能引用已注册 ID，不能写 Kotlin class 名。
- `commitPolicy` 不是合法字段；Direct 的提交规则由 `candidatePolicy`、`mapping.fallback` 和引擎默认行为共同定义。
- 全拼使用通用 `identity` encoder；双拼使用 `double_pinyin` encoder，并通过 `encoderConfig` 指定小鹤、自然码等具体方案。
- `dictionaryOptional` 默认 `false`。首版不建议使用；只有 Manifest 明确声明后，字典缺失才允许回退提交 raw/query。
- 外部语言包的所有资源路径必须是相对路径，规范化后仍位于语言包目录内。

### 4.4 运行时注册表

`OrthogonalRegistry` 是 Manifest 加载后的运行时能力索引：

```kotlin
class OrthogonalRegistry(
    private val engineRegistry: EngineRegistry,
    private val layoutRegistry: LayoutRegistry,
    private val dictionaryRegistry: DictionaryRegistry
) {
    fun register(manifest: LanguageManifest): RegisterResult
    fun unregister(packageId: String): RegisterResult
    fun getLocale(locale: LocaleTag): LocaleCapability?
    fun isSupported(state: OrthogonalState): Boolean
    fun defaultState(locale: LocaleTag): OrthogonalState?
    fun defaultSchema(locale: LocaleTag, script: Script): Schema?
    fun schemaCapability(state: OrthogonalState): SchemaCapability?
}
```

注册表必须在注册前完成校验：

- `manifestVersion` 是否受当前 app 支持。
- `locale` 是否合法。
- `defaults.script` 是否存在于 `scripts`。
- `defaults.schema` 是否存在于默认 Script。
- 每个 Script 必须有 `defaultSchema`。
- 每个 `defaultSchema` 必须存在于该 Script 的 `schemas`。
- `engineId` 必须已注册。
- `layoutId` 必须能被布局仓库加载。
- `supportsShift` 必须显式声明。
- `candidatePolicy` 必须已注册。
- `displayPolicy` 如果声明，必须已注册；未声明时按引擎族默认值补齐。
- `direct` 必须声明 `mapping`。
- `table_composing` 必须声明 `encoderId`；除非 `dictionaryOptional = true`，否则必须声明 `dictionary`。
- `transliteration` 必须声明 `fsm`。
- `dictionary` 如果声明，必须存在且大小受控；未声明且 `dictionaryOptional != true` 时注册失败。
- subtype 的 `labelKey` 必须能被 i18n 资源或语言包文案解析。

注册失败时不得产生半注册状态。

### 4.5 冲突与优先级

多个语言包可能声明同一个 Locale。必须定义确定性合并策略：

1. 内置语言包作为基础能力。
2. 用户安装语言包默认不能覆盖内置能力，只能新增 Script 或 Schema。
3. 如果允许覆盖，必须由用户在设置中显式启用。
4. 同一个 `Locale + Script + Schema` 出现多个实现时，按 `SettingsManager` 中的用户选择决定。
5. 未设置用户选择时，优先使用内置包。

建议用稳定 ID 标识能力来源：

```text
CapabilityId = packageId + locale + script + schema
```

这样可以支持同一个 Schema 的多种实现，例如内置拼音和第三方拼音。

### 4.6 特殊 Schema

`VOICE` 和 `HANDWRITING` 属于特殊输入 Schema：

- 可挂载到任意合法 `Locale + Script`。
- 切入时保存上一个普通 Schema。
- 退出时恢复上一个普通 Schema。
- 不改变当前 `Locale` 与 `Script`。
- `VOICE` 仅在用户主动进入语音输入时申请麦克风权限。
- `HANDWRITING` 初版不需要额外系统权限。

特殊 Schema 可以由核心包内置，也可以由插件包声明。但即使由插件声明，也必须经过 `OrthogonalRegistry` 校验后才能进入状态转移。

特殊 Schema 恢复规则：

1. `KeyboardContextManager` 维护 `previousRegularState: OrthogonalState?`，只记录非特殊 Schema。
2. 从普通 Schema 切入 `VOICE` 或 `HANDWRITING` 时，保存当前 `OrthogonalState` 到 `previousRegularState`。
3. 从 `VOICE/HANDWRITING` 退出时，优先恢复 `previousRegularState`。
4. 如果 `previousRegularState` 对应 capability 已不可用，则恢复当前 `Locale + Script` 下的默认普通 Schema。
5. 如果当前 Script 下没有可用普通 Schema，则恢复当前 Locale 默认状态。
6. 如果用户在特殊 Schema 中切换 Locale 或 Script，必须清空 `previousRegularState`，并按新 Locale/Script 的默认状态退出。
7. 特殊 Schema 之间互相切换时，不覆盖 `previousRegularState`。

特殊 Schema 不参与普通 Schema 轮换键。例如“拼音/双拼”切换不应切到 `VOICE`；语音和手写必须由明确入口触发。

### 4.7 内置矩阵只是 Manifest 数据

首版内置能力可以包含以下 Manifest 数据：

| Locale | Script | Schema |
| --- | --- | --- |
| `zh-CN` | `HANI` | `PINYIN`, `DOUBLE_PINYIN` |
| `zh-CN` | `LATN` | `LATIN_DIRECT` |
| `en-US` | `LATN` | `LATIN_DIRECT` |
| `ja-JP` | `HIRA` | `ROMAJI` |
| `ja-JP` | `KATA` | `ROMAJI` |
| `ja-JP` | `LATN` | `LATIN_DIRECT` |

该表只用于说明内置语言包初始内容，不代表代码中的静态合法矩阵。实际合法性始终来自已注册的 Manifest 能力。

## 5. 状态管理器设计

### 5.1 单一入口

所有状态变更必须经过 `KeyboardContextManager`：

```kotlin
class KeyboardContextManager(
    private val registry: OrthogonalRegistry,
    private val settings: SettingsManager
) {
    val context: StateFlow<KeyboardContext>

    fun switchLocale(locale: LocaleTag): TransitionResult
    fun switchScript(script: Script): TransitionResult
    fun switchSchema(schema: Schema): TransitionResult
    fun switchLayer(layer: LayoutLayer): TransitionResult
    fun openPanel(panel: PanelType): TransitionResult
    fun closePanel(): TransitionResult
    fun setComposing(text: String, candidates: List<Candidate>): TransitionResult
    fun clearComposing(): TransitionResult
}
```

`KeyboardStateManager` 不再作为状态管理入口。实现时应删除或重建为 `KeyboardContextManager`，禁止继续维护两套状态。

`KeyboardContextManager` 不持有任何语言硬编码表。它只依赖 `OrthogonalRegistry` 查询当前已注册能力，并将用户偏好写入 `SettingsManager`。

### 5.2 转移结果

状态转移不能静默失败，必须返回明确结果：

```kotlin
sealed interface TransitionResult {
    data class Applied(val context: KeyboardContext) : TransitionResult
    data class Rejected(val reason: TransitionRejectReason) : TransitionResult
}

enum class TransitionRejectReason {
    UNSUPPORTED_LOCALE,
    UNSUPPORTED_SCRIPT,
    UNSUPPORTED_SCHEMA,
    ILLEGAL_COMBINATION,
    CAPABILITY_NOT_REGISTERED,
    CAPABILITY_CONFLICT
}
```

UI 可以忽略失败提示，但测试必须覆盖拒绝原因。

### 5.3 通用转移引擎

状态转移逻辑由 `TransitionEngine` 执行：

```kotlin
class TransitionEngine(
    private val registry: OrthogonalRegistry
) {
    fun reduce(
        current: KeyboardContext,
        event: TransitionEvent
    ): TransitionResult
}
```

`TransitionEngine` 只处理通用规则：

- 切换 Locale 时选择该 Locale 的 Manifest 默认状态。
- 切换 Script 时选择该 Script 的 Manifest 默认 Schema。
- 切换 Schema 时校验该 Schema 是否已注册。
- 切换输入语义状态时清空 composing 和 candidates。
- 切换普通布局层时不改变 `OrthogonalState`。
- 非法目标状态返回 `Rejected`，并保持原状态不变。

禁止在 `TransitionEngine` 中写入具体语言判断，例如 `if (locale == "zh-CN")`。

## 6. 状态转移规则

### 6.1 Locale 切换

触发来源：

- Android 系统 Subtype 切换。
- 设置页修改默认语言。
- 语言切换键明确切到另一个 Locale。

规则：

1. 通过 `OrthogonalRegistry` 校验目标 Locale 是否已由 Manifest 注册。
2. 清空 `composingText`、`candidates`、`selectedCandidateIndex`。
3. 关闭所有面板，设置 `activePanel = NONE`。
4. 从目标 Locale 的 Manifest 读取默认 `Script + Schema`。
5. 从目标 Schema 能力读取默认 `layoutId`。
6. 设置 `layer = NORMAL`。
7. 持久化 `SettingsManager.currentLocale`。

示例：

```text
zh-CN + HANI + PINYIN
switchLocale(en-US)
=> en-US + LATN + LATIN_DIRECT
```

### 6.2 Script 切换

触发来源：

- 中英切换键。
- 日文假名/英文切换键。
- 布局动作 `SWITCH_SCRIPT`。

规则：

1. 通过 `OrthogonalRegistry` 在当前 Locale 下校验目标 Script 是否可用。
2. 清空组合输入和候选词。
3. 从 Manifest 中读取该 Script 的 `defaultSchema`。
4. 从目标 Schema 能力更新布局 ID。
5. 重置 `layer = NORMAL`。
6. 关闭普通面板。

示例：

```text
zh-CN + HANI + PINYIN
switchScript(LATN)
=> zh-CN + LATN + LATIN_DIRECT
```

```text
zh-CN + LATN + LATIN_DIRECT
switchScript(HANI)
=> zh-CN + HANI + PINYIN
```

### 6.3 Schema 切换

触发来源：

- 拼音/双拼切换。
- 设置页选择默认输入方案。
- 语音或手写输入入口。

规则：

1. 通过 `OrthogonalRegistry` 校验目标 Schema 是否属于当前 `Locale + Script`。
2. 合法时仅改变 Schema。
3. 清空组合输入和候选词。
4. 默认不改变 `Locale`、`Script`、`layoutId`。
5. 如果目标 Schema 在 Manifest 中声明专属布局，则更新 `layoutId`。
6. 非法组合直接拒绝，不修改当前状态。

示例：

```text
zh-CN + HANI + PINYIN
switchSchema(DOUBLE_PINYIN)
=> zh-CN + HANI + DOUBLE_PINYIN
```

```text
zh-CN + HANI + PINYIN
switchSchema(ROMAJI)
=> Rejected(ILLEGAL_COMBINATION)
```

### 6.4 LayoutLayer 切换

触发来源：

- Shift。
- Caps Lock。
- 数字键盘。
- 符号键盘。

规则：

1. `LayoutLayer` 只影响布局查找和显示层。
2. 不改变 `Locale`、`Script`、`Schema`。
3. 是否允许 `SHIFTED` 与 `CAPS_LOCK` 由当前 Schema 能力的 `supportsShift` 决定。
4. `supportsShift = false` 时，Shift/Caps 动作返回 `Applied` 但保持当前普通层。
5. `SYMBOL` 和 `NUMBER` 是临时层，退出后恢复进入前的普通层。

权责边界：

- `LayoutLayer` 属于状态层，由 `TransitionEngine` 和 `KeyboardContextManager` 管理。
- 输入引擎不保存 Shift/Caps 状态，也不直接切换 `LayoutLayer`。
- 布局层根据当前 `LayoutLayer` 解析出最终 token，再交给 `InputPipeline`。
- 当 `supportsShift = false` 时，UI 不能进入 `SHIFTED` 或 `CAPS_LOCK` 显示状态。
- 光标移动、输入框切换或宿主应用外部修改文本导致组合态不可信时，由 `InputPipeline` reset 输入 session，并通过 `KeyboardContextManager` 清空 composing/candidates；状态层不尝试从宿主文本反推引擎 buffer。

示例：

```text
en-US + LATN + LATIN_DIRECT + NORMAL
switchLayer(SHIFTED)
=> en-US + LATN + LATIN_DIRECT + SHIFTED
```

```text
zh-CN + HANI + PINYIN + NORMAL
switchLayer(SHIFTED)
=> Applied，但 layer 保持 NORMAL
```

### 6.5 Panel 切换

普通工具面板不属于正交输入状态：

- Emoji
- Symbol
- Clipboard
- LLM

规则：

1. 打开普通面板不改变 `OrthogonalState`。
2. 关闭普通面板恢复主键盘。
3. STT 如果作为语音输入能力，应走 `switchSchema(VOICE)`。
4. Handwriting 应走 `switchSchema(HANDWRITING)`。

## 7. Android Subtype 桥接

Android `InputMethodSubtype` 是扁平结构，不能直接表达三维矩阵，因此使用桥接模式。

### 7.1 Subtype 生成策略

Android subtype 可以采用两种策略：

- 构建期生成：Gradle 脚本读取内置 `LanguageManifest`，生成 `method.xml` 和对应字符串资源。
- 运行时桥接：只在 `method.xml` 声明少量泛 subtype，内部状态完全由 `KeyboardContext` 管理。

推荐首版采用构建期生成内置 subtype。原因是 Android 系统输入法选择器主要依赖静态 XML，构建期生成能让系统语言栏稳定展示内置语言能力。

外部下载语言包无法动态修改 `method.xml`，因此外部能力只能在 MyBoard 内部语言切换面板中展示；如果未来需要系统级展示，应通过插件 APK 或重新打包内置包解决。

### 7.2 构建期生成 method.xml

构建脚本读取内置 Manifest 中声明了 `subtype` 的 Schema，并生成 subtype。每个需要暴露给系统的组合声明一个 subtype，通过 `extraValue` 写入正交信息：

```xml
<subtype
    android:label="@string/subtype_zh_pinyin"
    android:imeSubtypeLocale="zh-CN"
    android:imeSubtypeMode="keyboard"
    android:imeSubtypeExtraValue="script=HANI,schema=PINYIN" />

<subtype
    android:label="@string/subtype_en_direct"
    android:imeSubtypeLocale="en-US"
    android:imeSubtypeMode="keyboard"
    android:imeSubtypeExtraValue="script=LATN,schema=LATIN_DIRECT" />
```

生成规则：

1. 只处理内置 Manifest。
2. 只处理声明了 `subtype` 的 Schema。
3. `android:imeSubtypeLocale` 来自 Manifest 的 `locale`。
4. `script` 和 `schema` 写入 `android:imeSubtypeExtraValue`。
5. `android:label` 来自 Manifest 的 `subtype.labelKey`，并要求 i18n 字符串存在。

生成脚本要求：

- 输入：`app/src/main/assets/languages/**/language.manifest.json`。
- 输出：生成到 `app/build/generated/res/myboardSubtype/xml/method.xml`。
- 字符串资源引用必须来自已有 `strings.xml`，脚本不直接生成翻译内容。
- Gradle 将 generated res 目录加入 Android source set。
- 生成顺序必须稳定，按 `locale -> script -> schema -> packageId` 排序。
- 如果 Manifest 中 `subtype.labelKey` 找不到对应字符串，构建失败。
- 如果两个内置 Manifest 生成完全相同的 `locale + script + schema` subtype，构建失败。
- 外部下载语言包不参与 method.xml 生成。

### 7.3 系统流入

当系统触发 subtype 变化：

1. 读取 subtype locale。
2. 解析 `extraValue` 中的 `script` 与 `schema`。
3. 构造目标 `OrthogonalState`。
4. 交给 `KeyboardContextManager` 校验并应用。
5. 非法或缺失字段时回退到该 Locale 默认状态。

### 7.4 内部流出

当内部状态切换成功：

1. 根据当前 `Locale + Script + Schema` 查找匹配 subtype。
2. 找到时同步 Android 系统状态栏。
3. 找不到时只更新内部状态，不阻断输入。

Subtype 只是系统展示和切换桥，不是状态源。

## 8. 引擎、布局、字典路由

### 8.1 输入引擎路由

输入引擎由当前 `SchemaCapability` 决定，而不是由代码里的 `when(schema)` 决定。Manifest 中的 `engineId` 指向一个已注册引擎工厂：

| engineId | Engine |
| --- | --- |
| `direct` | `DirectEngine` |
| `table_composing` | `TableComposingEngine` |
| `transliteration` | `TransliterationEngine` |

`VOICE` 与 `HANDWRITING` 是特殊 Schema。首版如果作为完整输入会话实现，必须通过独立的 `voice` 或 `handwriting` capability 明确接入 Pipeline；如果只是工具面板能力，则不得伪装成普通 `InputEngine`。二者的权限、生命周期和结果回传协议必须在对应能力文档中单独定义。

表音/字形组合输入的具体编码器由 `encoderId` 决定：

| encoderId | Encoder |
| --- | --- |
| `identity` | token 原样拼接，适合全拼 |
| `double_pinyin` | 双拼编码器，具体方案由 `encoderConfig` 决定 |
| `table_mapping` | 表形码映射编码器 |

引擎只读取当前 `KeyboardContext.orthogonal`，不持久保存语言状态。

Manifest 不能直接写 Kotlin class 名，只能写稳定 ID。这样可以避免外部语言包任意指定类名执行代码。

### 8.2 字典路由

字典路径来自当前 `SchemaCapability.dictionary`。字典缓存键至少包含能力来源，避免不同语言包的同名 Schema 冲突：

```kotlin
data class DictionaryKey(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
)
```

布局不参与字典选择。这样可以保证同一输入方案在不同布局上复用同一字典。

如果同一个 `Locale + Script + Schema` 存在多个实现，必须使用 `CapabilityId` 精确定位字典来源。

### 8.3 布局动作定义

布局动作必须使用正交动作。`SWITCH_MODE` 不再是合法动作，应直接删除：

```json
{
  "actionType": "SWITCH_SCRIPT",
  "payload": {
    "script": "LATN"
  }
}
```

推荐动作集合：

- `SWITCH_LOCALE`
- `SWITCH_SCRIPT`
- `SWITCH_SCHEMA`
- `SWITCH_LAYER`
- `RESTORE_PREVIOUS_SCHEMA`
- `PUSH_TOKEN`
- `DELETE`
- `SPACE`
- `ENTER`
- `COMMIT_CANDIDATE`

## 9. 设置单一来源与 i18n

设置必须以 `SettingsManager` 为持久化单一来源，并且所有设置入口必须能从 `SettingsActivity.kt` 到达。

需要提供的设置：

- 当前默认 Locale。
- 已启用 Locale 列表。
- 每个 Locale 的默认 Script。
- 每个 `Locale + Script` 的默认 Schema。
- 是否启用双拼。
- 是否启用语音输入。
- 是否启用手写输入。

要求：

- 不允许 UI 层维护独立设置副本。
- 不允许布局 JSON、IME Service、SettingsScreen 各自保存默认语言或默认模式。
- 所有用户可见文案必须同时维护：
  - `app/src/main/res/values/strings.xml`
  - `app/src/main/res/values-zh-rCN/strings.xml`

## 10. 权限要求

必须最小化权限申请：

- 普通键盘输入不申请额外权限。
- 拼音、双拼、英文直出不申请额外权限。
- 语音输入只在用户主动进入 `VOICE` Schema 时请求 `RECORD_AUDIO`。
- 手写输入初版不申请权限。
- 如果后续支持外部词库导入，使用系统文件选择器，不直接申请宽泛存储权限。

不同 Android 版本的权限处理必须集中封装，UI 只表达“请求能力”，不直接散落权限判断。

## 11. 测试与验收标准

### 11.1 单元测试

必须覆盖：

- 合法 `LanguageManifest` 可以注册。
- 缺少默认 Script 的 Manifest 注册失败。
- 缺少默认 Schema 的 Manifest 注册失败。
- 引用了不存在 `engineId` 的 Manifest 注册失败。
- 引用了不存在 `layoutId` 的 Manifest 注册失败。
- 多个语言包声明同一能力时按冲突策略解析。
- `zh-CN + HANI + PINYIN` 合法。
- `zh-CN + HANI + DOUBLE_PINYIN` 合法。
- `zh-CN + HANI + ROMAJI` 非法。
- `ja-JP + HANI + PINYIN` 非法。
- `switchLocale(en-US)` 后得到 `en-US + LATN + LATIN_DIRECT`。
- `switchScript(LATN)` 在 `zh-CN` 下得到 `zh-CN + LATN + LATIN_DIRECT`。
- `switchSchema(DOUBLE_PINYIN)` 在 `zh-CN + HANI` 下成功。
- `switchSchema(ROMAJI)` 在 `zh-CN + HANI` 下失败且状态不变。
- `supportsShift = true` 时 Shift 生效。
- `supportsShift = false` 时 Shift 默认不改变 layer。
- `TransitionEngine` 中不存在按具体 Locale 分支的硬编码逻辑。
- 从普通 Schema 切入 `VOICE` 后，退出时恢复之前的普通 Schema。
- 特殊 Schema 之间切换不覆盖 `previousRegularState`。
- 特殊 Schema 中切换 Locale/Script 会清空 `previousRegularState`。

### 11.2 集成测试

必须覆盖：

- 同一按键在 `PINYIN` 下进入 composing。
- 同一按键在 `LATIN_DIRECT` 下直接 commit。
- 切换 Locale 时清空 composing 和 candidates。
- 打开普通面板不改变 `OrthogonalState`。
- 进入语音输入时走 `VOICE` Schema。
- Android subtype extraValue 能解析到正确正交状态。
- 构建期 subtype 生成脚本能从内置 Manifest 生成 `method.xml`。
- method.xml 生成顺序稳定。
- subtype labelKey 缺少 i18n 字符串时构建失败。
- 重复 `locale + script + schema` subtype 时构建失败。
- 外部语言包注册后能在内部语言切换面板出现，但不会要求动态修改 `method.xml`。

### 11.3 构建验收

实现代码修改后必须保证：

```bash
./gradlew test
./gradlew assembleDebug
```

能通过并生成 APK。

本次仅新增设计文档时，不要求运行 Gradle 构建。

## 12. 实施顺序

建议按以下顺序实施破坏性重构：

1. 新增 `OrthogonalState`、`KeyboardContext`、`OrthogonalRegistry`、`KeyboardContextManager`。
2. 定义 `LanguageManifest` 数据模型、JSON Schema、Manifest 解析器和校验器。
3. 将内置中英文能力定义为内置 Manifest 数据，不在 Kotlin 中硬编码矩阵。
4. 新增 `EngineRegistry`、`LayoutRegistry`、`DictionaryRegistry`，让 Manifest 通过稳定 ID 引用能力。
5. 实现 `TransitionEngine`，只保留通用转移规则。
6. 删除 `KeyboardStateManager` 的状态源职责，所有调用点改为 `KeyboardContextManager`。
7. 删除 `LanguageSwitchManager`，语言、文字系统、输入方案切换全部进入 `TransitionEngine`。
8. 删除 `inputMethodId` 路由，输入引擎从 `SchemaCapability.engineId` 派生。
9. 删除 `SWITCH_MODE` 动作，布局统一使用 `SWITCH_LOCALE`、`SWITCH_SCRIPT`、`SWITCH_SCHEMA`、`SWITCH_LAYER`。
10. 增加构建期 subtype 生成脚本，读取内置 Manifest 生成 `method.xml` 和 i18n 字符串引用。
11. 实现 subtype extraValue 解析和内部状态同步。
12. 将相关默认值定义到 `SettingsManager`，并保证 `SettingsActivity.kt` 可进入设置。
13. 补齐 i18n 字符串。
14. 添加 Manifest 注册、状态转移、引擎路由、Subtype 生成相关测试。
15. 运行测试和 assemble，确认可生成 APK。

## 13. 关键原则

- `KeyboardContext` 是运行时唯一事实源。
- `SettingsManager` 是设置持久化唯一事实源。
- 本文档是状态模型和转移逻辑的唯一标准；与现有代码冲突时，删除或重建现有代码。
- 语言能力来自 `LanguageManifest`，不能在 Kotlin 状态机中硬编码。
- `Locale`、`Script`、`Schema` 只能通过已注册 Manifest 能力组合。
- `TransitionEngine` 只包含通用状态转移规则，不包含具体语言判断。
- `OrthogonalRegistry` 负责能力注册、合并、校验和冲突处理。
- UI 只观察状态和发出动作，不直接决定输入语义。
- 输入引擎只处理输入逻辑，不负责布局和系统 subtype。
- 布局只描述显示与动作，不持有语言状态。
- 权限按能力申请，避免提前申请和宽泛申请。
- 所有用户可见内容必须 i18n。
