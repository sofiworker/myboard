## Subtype 方案2（修正版：Locale <-> Layout 绑定）执行计划（直接修改）


### 目标（按产品流程）

1) 系统输入法列表只显示一个输入法：`MyBoard`（不暴露多个系统 Subtype）。
2) 安装/首次进入引导用户启用并切换到 `MyBoard`。
3) 运行时根据系统语言默认选择 `locale`，或用户手动选择 `locale`。
4) 在该 `locale` 下让用户选择可用 `layout`（例如 `qwerty`/`t9`）。
5) 正式输入时，允许在 `mode`（拼音/手写/直通等）之间动态切换，并按 `mode` 加载/切换字典或识别器。

### Subtype 定义（方案2：locale -> layoutIds[] 映射表）

Subtype 资源生成的是一个 “locale -> layout 列表” 的映射表，用于先根据语言得到可选布局集合（而不是直接生成 subtype 列表）。

- `SubtypePack.version=3`
- `locales: LocaleLayoutProfile[]`
  - `localeTag: String`：locale（BCP-47/下划线均可；运行期统一 normalize）
  - `layoutIds: String[]`：该 locale 下可用的布局列表
  - `defaultLayoutId?: String`：默认布局（可选；没有则运行期取 `layoutIds[0]`）
  - `enabled: Boolean`
  - `priority: Int`

对应实现：
- 数据模型：`app/src/main/java/xyz/xiao6/myboard/model/SubtypeModels.kt`
- 解析器：`app/src/main/java/xyz/xiao6/myboard/model/SubtypeParser.kt`
- 生成器：`scripts/generate_subtypes.py`（输出 `SubtypePack.version=2`）

### 代码层执行步骤（每步都有“目的/方法/落点”）

1) 系统只保留一个 IME 入口（不靠系统 Subtype 做语言切换）
   - 目的：系统选择输入法时只看到 “MyBoard” 一项，语言/模式切换都在 IME 内部完成。
   - 方法：在 `InputMethodService`（待实现/接入）里不注册 per-locale 的 `InputMethodSubtype` 列表；如必须返回 subtype，则返回单个固定 subtype。
   - 落点：新增/修改 IME Service 相关类（例如 `.../ime/MyBoardImeService.kt`），并更新 `AndroidManifest.xml` 的 `<service android:name=...>`。

2) locale 选择（运行时）
   - 目的：以系统语言为默认 locale，允许用户手动覆盖。
   - 方法：
     - 默认：`Locale.getDefault()`
     - 可用 locale 列表：`SubtypeManager.findByLocale(locale)`
     - 默认 subtype（仅用于“locale 候选”）：`SubtypeManager.resolve(locale)`
   - 落点：
     - `app/src/main/java/xyz/xiao6/myboard/manager/SubtypeManager.kt`
     - 新增设置存储（建议 DataStore/SharedPreferences），保存 `userLocaleTag`（待实现）。

3) layout 选择/切换（运行时，依赖 locale）
   - 目的：在当前 locale 下选择可用布局；并允许布局内按钮/工具栏触发布局切换（例如切到笔画/手写布局）。
   - 方法：
     - 取当前 locale 的布局列表：`SubtypeManager.resolve(locale)?.layoutIds`
     - 默认 layout：`profile.defaultLayoutId ?: profile.layoutIds.firstOrNull()`
     - 应用布局：`InputMethodController.loadLayout(layoutId)`
     - 布局内切换：通过 `ActionType.SWITCH_LAYOUT`（已存在）触发 `InputMethodController` 切换布局
   - 落点：
     - `app/src/main/java/xyz/xiao6/myboard/manager/SubtypeManager.kt`
     - `app/src/main/java/xyz/xiao6/myboard/controller/InputMethodController.kt`
     - 设置存储：保存 `layoutId`（建议按 `localeTag` 分桶，待实现）。

4) mode 动态切换（输入中；可伴随 layout 切换；不切 subtype）
   - 目的：同一 locale 下，输入时可在“拼音/手写/直通”等模式切换；并允许某些 mode 需要切换到专用 layout（例如手写面板/笔画布局）。
   - 方法（需要新增）：
     - 新增 `InputMode`（如 `PINYIN/HANDWRITING/DIRECT`）。
     - 在 `InputMethodController` 增加运行时切换 decoder 的接口（例如 `setDecoder(decoder: Decoder)`），并在切换时调用 `decoder.reset()` 清理候选/状态。
     - 当 mode 需要专用布局时，复用现有 `ActionType.SWITCH_LAYOUT` 切换到对应 layout（例如 toolbar 选择笔画 layout）。
   - decoder 映射建议：
     - `PINYIN`：`PinyinDictionaryDecoder(DictionaryLookup(...))`
     - `HANDWRITING`：接入 Digital Ink（此仓库已引入依赖），实现一个 recognizer-backed decoder（待实现）
     - `DIRECT`：`PassthroughDecoder`
   - 落点：
     - `app/src/main/java/xyz/xiao6/myboard/controller/InputMethodController.kt`
     - `app/src/main/java/xyz/xiao6/myboard/decoder/*`

5) 字典加载（locale 初筛 -> layout 二筛 -> kind/core 决策；不属于 subtype）
   - 目标：字典/识别器由运行时 mode 决定；字典候选按 locale 与 layout 逐层筛选，最终落到一个可用的 `DictionarySpec`（或某个 recognizer）。
   - 关键定义（需要新增/调整 schema）：
     - `DictionarySpec.kind`：输入方案/功能分类（例：`PINYIN`/`HANDWRITING`/`STROKE`/`CANGJIE`/`DIRECT`）
     - `DictionarySpec.core`：实现引擎分类（例：`PINYIN_CORE`/`HANDWRITING_CORE`/`STROKE_CORE`，决定用哪个 Decoder/Recognizer 工厂）
     - `DictionarySpec.variant`（可选）：同一 kind 下的变体（例：全拼/双拼/仓颉版本号）
     - `DictionarySpec.isDefault`（可选）：同一 kind 多字典时的默认候选
     - `DictionarySpec.layoutIds`：继续作为“二筛”约束（空=全布局可用）
   - 初筛规则（按 locale）：
     - 输入：`Locale`（运行时当前语言环境，或用户选择）
     - 规则：优先匹配 `zh-CN`，其次 `zh`，再其次 `[]`（语言无关兜底）
     - 落点：扩展/复用 `DictionaryManager.findByLocale(locale)`
   - 二筛规则（按 layoutId）：
     - 输入：当前 `layoutId`
     - 规则：`layoutIds=[]` 不限制；否则要求 `layoutId in layoutIds`
   - “多个候选字典”处理策略（建议固定为可解释的排序）：
     1) 先按 `kind==当前mode(kind)` 过滤（拼音模式只看拼音类字典）
     2) 若用户在 `(localeTag, layoutId, kind)` 下保存过 `preferredDictionaryId`，优先命中它
     3) 再按 `priority desc`
     4) 再按 `isDefault=true`（若有）
     5) 最后按 `dictionaryId` 字典序保证稳定性
   - core/kind 如何驱动加载：
     - `core` 决定构建哪个 decoder/recognizer：
       - `PINYIN_CORE` -> `PinyinDictionaryDecoder(DictionaryLookup(...))`
       - `HANDWRITING_CORE` -> Digital Ink recognizer-backed decoder（待实现）
       - `DIRECT` -> `PassthroughDecoder`
     - 运行时切换 mode 时：
       - 先调用 resolver 选出 `DictionarySpec`（或 recognizer 配置）
       - 再 `InputMethodController.setDecoder(newDecoder)` 并 `decoder.reset()`
   - 需要新增的“决策器/解析器”（推荐）：
     - 新增 `DictionaryResolver`（或 `InputResolver`）集中实现规则，避免散落在 UI/Controller：
       - `listCandidates(locale: Locale, layoutId: String): List<DictionarySpec>`
       - `resolve(locale: Locale, layoutId: String, kind: InputKind, preferredDictionaryId: String?): DictionarySpec?`
     - 文件建议：`app/src/main/java/xyz/xiao6/myboard/manager/DictionaryResolver.kt`
   - 需要调整/新增的落点文件：
     - Model：`app/src/main/java/xyz/xiao6/myboard/model/DictionaryModels.kt`
     - Manager：`app/src/main/java/xyz/xiao6/myboard/manager/DictionaryManager.kt`（校验/读取新字段）
     - Resolver：`app/src/main/java/xyz/xiao6/myboard/manager/DictionaryResolver.kt`（新增）
     - Decoder 工厂：`app/src/main/java/xyz/xiao6/myboard/decoder/*`（新增 mode->decoder 组装）
     - 设置存储（新增）：保存用户偏好
       - `preferredKindByLocaleLayout`
       - `preferredDictionaryIdByLocaleLayoutKind`
       - 建议实现：DataStore（优先）或 SharedPreferences（先跑通）

6) layout 与 kind 的关系（用于“layout 内切换按钮/toolbar 切笔画/手写布局，并动态调整字典”）
   - 目标：layout 决定“能切哪些 kind”、以及某些 kind 是否需要专用 layout（例如手写面板/笔画键盘）。
   - 建议改法（需要扩展 layout schema）：
     - `supportedKinds: ["PINYIN","DIRECT",...]`（可选；缺省表示不限制）
     - `defaultKind: "PINYIN"`（可选）
   - 运行时规则：
     - 切 layout（`ActionType.SWITCH_LAYOUT`）后：
       - 若新 layout 不支持当前 kind，则切到 `defaultKind` 或回退到 `DIRECT`
       - 然后触发字典/decoder 重选（走 DictionaryResolver）
     - 切 kind（新增 `ActionType.SWITCH_MODE` 或 toolbar 事件）后：
       - 若该 kind 需要专用 layout，可先 `SWITCH_LAYOUT` 到目标 layout
       - 再触发字典/decoder 重选
   - 落点：
     - Layout model/parser：`app/src/main/java/xyz/xiao6/myboard/model/LayoutModels.kt`、`.../LayoutParser.kt`
     - Key action：`app/src/main/java/xyz/xiao6/myboard/model/KeyModels.kt`（新增 `ActionType.SWITCH_MODE`）
     - State machine：`app/src/main/java/xyz/xiao6/myboard/controller/KeyboardStateMachine.kt`（产出 effect）
     - Controller：`app/src/main/java/xyz/xiao6/myboard/controller/InputMethodController.kt`（执行切换与 decoder 重选）

7) 构建期资源生成（仍然保留；subtype 生成 locale->layouts 映射）
   - 目的：构建时自动生成 `subtypes/generated.json`（v3），供运行时按 locale 找到可用 layout 列表。
   - 方法：
     - `scripts/generate_subtypes.py` 读取 `assets/layouts/*.json`，把每个 layout 的 `locale` 索引成 `localeTag -> layoutIds[]` 映射表（不绑定字典）。
     - Gradle task：`generateSubtypes`（`app/build.gradle.kts`）产物目录：`build/generated/subtypesAssets/subtypes/generated.json`

8) 资源落地建议（示例）
   - 目标：支持“中文 + qwerty/t9/笔画/手写”等布局；字典按 kind 分类且能被 layout 二筛过滤。
   - 建议：
     - Layout：`assets/layouts/qwerty.json`、`assets/layouts/t9.json`、`assets/layouts/stroke.json`、`assets/layouts/handwriting.json`
       - `locale=["zh_CN"]` 或 `["zh_CN","en_US"]`（按需要）
       - `supportedKinds` 按布局能力声明
     - Dictionary：`assets/dictionary/dict_pinyin.json`、`dict_shuangpin.json`、`dict_stroke.json`、`dict_handwriting.json`、`dict_en.json`
       - `localeTags=["zh_CN"]`
       - `kind`/`core`/`variant`/`isDefault` 填好
       - `layoutIds=["qwerty"]`（拼音/双拼限定）或 `["stroke"]`（笔画限定）等
     - 多字典同 kind：例如 `dict_pinyin_quanpin` 与 `dict_pinyin_shuangpin` 同为 `kind=PINYIN`，用 `variant` 区分，并在设置里提供选择。

9) 键盘存在语言切换的按钮时
    - 获取当前语言layout，判断该layout是否在目标语言中也有
    - 如果目标语言存在该layout，禁止重绘键盘，直接进行符号替换
    - 如果目标语言不存在该layout，判断用户的layout manager中是否有该语言的layout，如果存在直接切换过去
    - 如果目标语言不存在该layout，则选用语言默认的layout

### 验证方式（必须可用 `./gradlew` 验证）

1) 生成 subtype 资源并编译 Debug 包：
   - `./gradlew :app:assembleDebug`
2) 只验证 Kotlin 编译（更快）：
   - `./gradlew :app:compileDebugKotlin`
3) 验证 subtype 生成结果存在：
   - 编译完成后检查：`app/build/generated/subtypesAssets/subtypes/generated.json`
4)（实现 DictionaryResolver 等后）建议补充的验证点：
   - `./gradlew :app:testDebugUnitTest`（若后续加了单元测试）
   - 打开 Demo/IME 后，切换 layout 与 mode 时候选/decoder 会跟随变更（人工回归）
