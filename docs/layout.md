# MyBoard 布局引擎详细设计

> 版本：v2.0
> 状态：Draft
> 日期：2026-06-20
> 定位：本文档是 MyBoard 布局层的唯一实现标准。
> 依据：`docs/core.md` 第 5 节「数据驱动布局」与第 8 节「主题、反馈与扩展」；`docs/orthogonal-state-management.md` 的 `KeyboardContext`、`LayoutLayer`、`SchemaCapability`、正交动作集合；`docs/engine.md` 的 `InputAction`、`InputPipeline`；`docs/android-bridge.md` 的 `EditorProfile`、`LayoutHint`。
> 原则：与当前布局实现冲突时，允许破坏性重构。布局层只描述几何与展示，不承载输入语义、不持有语言状态、不直接调用 `InputConnection`。

## 0. 版本说明

v2.0 相对历史草稿的核心变更：

- 删除独立 `StateMachine` 和 `Page` 概念，改为观察 `KeyboardContext` 作为唯一状态源。
- 删除 `KeyType` 枚举和 `KeyModel.output` 直出字段，改为引用正交动作集合，所有按键事件经 `InputPipeline` 进入引擎。
- 删除焊死在按键上的 `background` / `textColor`，改为 `styleRef` 引用主题 token。
- 新增全球化章节：同一物理排列服务多个 `Schema` 的机制。
- 新增高度自定义章节：关注点分离与导入导出。
- 保留并强化几何优势：`Dimension` 多态尺寸、5 种 `LayoutContainer`、异形按键 `hitPath` 命中。

## 1. 目标与职责边界

布局层只负责一件事：把数据描述的布局结构，结合当前 `KeyboardContext`，渲染成可交互的按键视图，并把用户的触摸/手势事件转译为正交动作交给 `InputPipeline`。

它不负责：

- 判断 `Locale + Script + Schema` 是否合法（交给 `TransitionEngine`）。
- 维护编码 buffer、组合串、候选（交给 `InputSession`）。
- 把 token 映射为上屏文字或候选（交给引擎层）。
- 直接操作 `InputConnection`（交给 `InputConnectionGateway`）。
- 切换 Locale、Script、Schema（布局只发出 `SWITCH_LOCALE` 等动作，真正切换由状态层经校验后执行）。

它负责：

- 加载、解析、缓存布局数据（`LayoutRegistry`）。
- 根据 `KeyboardContext.layoutId` 选定布局，根据 `KeyboardContext.layer` 选定按键内容变体。
- 测量按键几何位置（`LayoutEngine`），渲染按键视觉（`LayoutRenderer`）。
- 把触摸事件映射为手势，把手势映射为正交动作，交给 `InputPipeline`。
- 根据 `EditorProfile`（来自 `android-bridge`）应用 `LayoutHint` 覆盖（密码框切布局、关闭候选等）。
- 仅消费主题 token，不硬编码颜色。

布局层是数据与视图分离的范本：布局数据是纯数据，布局渲染是纯视图，二者通过不可变的 `ResolvedLayout` 解耦。

## 2. 布局与正交状态的契约

### 2.1 输入：只读 `KeyboardContext`

布局渲染的唯一运行时输入是 `KeyboardContext`（定义见 `orthogonal-state-management.md:120`）。布局层只读以下字段：

| 字段 | 布局用途 |
| --- | --- |
| `layoutId` | 选定布局文件 |
| `layer`（`LayoutLayer`） | 选定按键内容变体（`NORMAL`/`SHIFTED`/`CAPS_LOCK`/`SYMBOL`/`NUMBER`） |
| `composingText` / `candidates` / `selectedCandidateIndex` | 候选栏 region 展示 |
| `orthogonal` | 决定布局是否支持 Shift（读 `SchemaCapability.supportsShift`，通过 registry 查询） |

布局层**禁止**读写 `KeyboardContext` 以外的运行时状态，禁止维护自己的 `StateMachine`、`currentPage`、`shiftState` 等字段。

### 2.2 输出：正交动作，经 `InputPipeline`

用户在按键上的所有交互，最终都转译为正交动作（定义见 `orthogonal-state-management.md:8.3`）交给 `InputPipeline`。布局层不直接 `commitText`、不直接切语言。

合法动作集合 = `orthogonal-state-management.md:769-780` 定义的核心动作 ∪ 本文档补充的 UI 动作。

核心动作（来自正交状态文档）：

- `PUSH_TOKEN`：推入编码 token（拼音 `q`、罗马音 `ka` 等），由引擎决定后续行为。
- `DELETE` / `SPACE` / `ENTER`：控制键。
- `SWITCH_LOCALE` / `SWITCH_SCRIPT` / `SWITCH_SCHEMA`：正交状态切换请求。
- `SWITCH_LAYER`：布局层切换（Shift、符号页、数字页）。
- `RESTORE_PREVIOUS_SCHEMA`：退出 `VOICE` / `HANDWRITING` 时恢复。
- `COMMIT_CANDIDATE`：候选栏选择，payload 带 `index`。

UI 动作（本文档补充，正交状态文档未列出，后续应反向同步）：

- `OPEN_PANEL` / `CLOSE_PANEL`：工具面板（Emoji、符号、剪贴板、LLM 等）。
- `PAGE_NEXT` / `PAGE_PREV`：候选栏分页。

动作数据契约：

```json
{
  "actionType": "PUSH_TOKEN",
  "payload": { "token": "q" }
}
```

```json
{
  "actionType": "SWITCH_SCRIPT",
  "payload": { "script": "LATN" }
}
```

`actionType` 和 `payload` 字段名是跨文档固定的契约，`InputPipeline` 按此解析。

### 2.3 与引擎的边界

引擎层不读布局 key id（`engine.md:318`）。布局层产出的 `InputAction` 是唯一的桥梁：

```text
触摸事件
  -> 布局层命中测试 + 手势识别
  -> KeyDef.actions[gesture] (布局数据)
  -> InputAction (正交动作)
  -> InputPipeline
  -> InputEvent (引擎事件)
  -> InputSession / Engine
```

布局数据中的 `actions` 是动作的**数据来源**，但**不携带语义判断**。例如 `SWITCH_SCRIPT` 只表示「用户请求切到 LATN」，是否真的切换由 `TransitionEngine` 校验当前 Locale 是否支持 LATN 后决定，布局不预判结果。

## 3. 布局数据模型

数据模型分三层：**尺寸（Dimension）、节点（Node）、文档（LayoutDoc）**。全部 `@Serializable`，支持 JSONC（JSON + 行注释，沿用现有 `LayoutParser.stripJsonLineComments`）。

### 3.1 尺寸与基础类型

```kotlin
@Serializable
sealed class Dimension {
    @Serializable @SerialName("match")     object Match : Dimension()
    @Serializable @SerialName("wrap")      object Wrap : Dimension()
    @Serializable @SerialName("weight")    data class Weight(val value: Float = 1f) : Dimension()
    @Serializable @SerialName("dp")        data class Dp(val value: Float) : Dimension()
    @Serializable @SerialName("percent")   data class Percent(val value: Float) : Dimension()  // 0.0~1.0
    @Serializable @SerialName("ratio_w")   data class RatioW(val value: Float) : Dimension()   // 屏宽比例
}
```

相比历史草稿，新增 `Dp`（绝对值，比 `Pixel` 更适合 IME）和 `RatioW`（屏宽比例，响应式布局）。`Percent` 语义收紧为父容器比例。

```kotlin
@Serializable
data class BoxSpacing(
    val start: Float = 0f, val top: Float = 0f,
    val end: Float = 0f, val bottom: Float = 0f
)

enum class Orientation { VERTICAL, HORIZONTAL }
enum class Gravity { START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY, STRETCH }

enum class HintPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

enum class GestureType {
    TAP, LONG_PRESS, DOUBLE_TAP, REPEAT,
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT
}
```

### 3.2 动作定义

```kotlin
@Serializable
data class ActionDef(
    val actionType: String,          // 见 §2.2 合法动作集合
    val payload: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class ActionMap(
    val gestures: Map<GestureType, ActionDef> = emptyMap()
)
```

`actionType` 是字符串而非枚举，因为外部语言包、用户自定义布局可能引用新动作；合法性校验在 `LayoutRegistry.register` 时完成，未注册的动作会让注册失败。

### 3.3 按键节点

```kotlin
@Serializable
data class KeyDef(
    val id: String,
    val styleRef: String? = null,              // 引用主题 token，不焊颜色
    val content: ContentSpec = ContentSpec(),
    val actions: ActionMap = ActionMap(),
    val width: Dimension = Dimension.Weight(1f),
    val height: Dimension = Dimension.Match,
    val variants: Map<String, VariantPatch> = emptyMap(),  // key = LayoutLayer 名
    val hitShape: HitShape? = null,            // 异形按键命中区域
    val repeatable: Boolean = false,           // 长按连续触发（退格等）
    val longPressDelay: Int = 400,
    val repeatInterval: Int = 50
)

@Serializable
data class ContentSpec(
    val label: String? = null,          // 显示文本，可带 i18n key 前缀 "@string/..."
    val icon: String? = null,           // 图标资源名或 Material icon 名
    val hint: Map<HintPosition, String> = emptyMap()  // 角标（如数字键上的字母）
)

@Serializable
data class VariantPatch(
    val label: String? = null,
    val icon: String? = null,
    val hint: Map<HintPosition, String>? = null,
    val visible: Boolean = true
)

@Serializable
sealed class HitShape {
    @Serializable @SerialName("rect")     data class Rect(val cornerRadius: Float = 0f) : HitShape()
    @Serializable @SerialName("circle")   data class Circle(val cx: Float, val cy: Float, val r: Float) : HitShape()
    @Serializable @SerialName("rounded")  data class Rounded(val cornerRadius: Float) : HitShape()
}
```

**关键设计**：

- `styleRef` 替代历史草稿的 `background`/`textColor`。颜色归主题层（`core.md:8`），布局只引用 token 名（如 `"key_function"`、`"key_action"`）。
- `variants` 按 `LayoutLayer` 名（`NORMAL`/`SHIFTED`/`SYMBOL`/...）提供内容覆盖。布局渲染时读 `KeyboardContext.layer`，把对应 `VariantPatch` 叠加到 `ContentSpec` 上。这就是 Shift 大小写、符号页切换的实现机制——不需要切布局文件，只切 layer。
- `actions` 是按键的全部行为来源。`ComposeInputView.mapKeyToAction` 的 keyId 硬编码在 v2.0 中必须删除，渲染器只读 `KeyDef.actions`。
- `hitShape` 支持异形按键（游戏键盘 ABXY、圆形键），命中测试用 `Path` + `Region`。

### 3.4 容器节点（5 种布局类型）

容器用 sealed class 表达多态，序列化鉴别器为 `type` 字段。5 种容器覆盖所有 IME 场景：

```kotlin
@Serializable
sealed class LayoutContainer {
    abstract val id: String
    abstract val width: Dimension
    abstract val height: Dimension
    abstract val padding: BoxSpacing
    abstract val styleRef: String?
    abstract val bindings: Bindings?
}

@Serializable
data class Bindings(
    val visibleWhen: String? = null,      // 表达式，见 §6.2
    val enabledWhen: String? = null
)
```

**① RowLayout —— 行容器（主键盘主力）**

```kotlin
@Serializable @SerialName("row")
data class RowLayout(
    override val id: String,
    val keys: List<KeyDef>,
    val gap: Float = 0f,
    val gravity: Gravity = Gravity.START,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Wrap,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer()
```

测量规则：按键按 `width` 分三类——`Dp`/`Percent` 占固定宽，`Weight` 按权重瓜分剩余空间，`Match`/`Wrap` 在行内退化为 `Weight(1f)` / 按内容测量。这是 QWERTY、T9 主行、底行（空格 weight=6、回车 weight=2）的标准布局。

**② GridLayout —— 网格容器（T9、数字、紧凑布局）**

```kotlin
@Serializable @SerialName("grid")
data class GridLayout(
    override val id: String,
    val columns: Int,
    val rows: Int = 0,                      // 0 表示按内容自动
    val cells: List<GridCell>,
    val rowGap: Float = 0f,
    val colGap: Float = 0f,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Match,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer() {
    @Serializable
    data class GridCell(
        val key: KeyDef,
        val col: Int, val row: Int,
        val colSpan: Int = 1, val rowSpan: Int = 1
    )
}
```

测量规则：按 `columns` 等分列宽，`colSpan`/`rowSpan` 合并单元格。校验：`col + colSpan ≤ columns`，`row + rowSpan ≤ rows`（若 rows>0），同一格不能被多个 cell 占用。T9 的 5×4 布局、数字键盘的 3×4 都用这个。

**③ LinearLayout —— 线性容器（候选栏、工具栏）**

```kotlin
@Serializable @SerialName("linear")
data class LinearLayout(
    override val id: String,
    val orientation: Orientation,
    val children: List<LayoutNode>,         // 可嵌套任意节点
    val gap: Float = 0f,
    val gravity: Gravity = Gravity.START,
    val scroll: ScrollSpec? = null,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Wrap,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer()

@Serializable
data class ScrollSpec(
    val enabled: Boolean = true,
    val direction: Orientation = Orientation.HORIZONTAL,
    val paging: Boolean = false
)
```

`LinearLayout` 的 `children` 可以是 `KeyDef`、`DividerNode`、`SpacerNode` 或嵌套容器。候选栏（水平滚动 + 每个候选是一个节点）、工具栏（图标按钮排列）用它。

**④ AbsoluteLayout —— 绝对定位容器（浮层、特殊键）**

```kotlin
@Serializable @SerialName("absolute")
data class AbsoluteLayout(
    override val id: String,
    val items: List<AbsoluteItem>,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Match,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer() {
    @Serializable
    data class AbsoluteItem(
        val key: KeyDef,
        val x: Dimension, val y: Dimension,
        val width: Dimension, val height: Dimension,
        val anchor: HintPosition = HintPosition.TOP_LEFT,
        val zIndex: Int = 0
    )
}
```

手写板、悬浮工具岛、特殊定位键用它。`zIndex` 控制层叠，`anchor` 支持右下角定位。

**⑤ CompositeLayout —— 复合容器（键盘 + 候选 + 工具一体化）**

```kotlin
@Serializable @SerialName("composite")
data class CompositeLayout(
    override val id: String,
    val orientation: Orientation,
    val regions: List<Region>,
    val gap: Float = 0f,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Wrap,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer()
```

`Region` 是带语义角色的容器包装（`keyboard`/`candidate`/`toolbar`/`sidebar`/`header`/`footer`）。一个完整的 IME 视图通常是 `CompositeLayout` 包含 `[toolbar/header region, candidate region, keyboard region]`。`bindings.visibleWhen` 让候选栏和工具栏在 composing 状态下互斥显示。

```kotlin
@Serializable
data class Region(
    val id: String,
    val role: RegionRole,                   // keyboard/candidate/toolbar/sidebar/header/footer/popup
    val container: LayoutContainer,
    val bindings: Bindings? = null
)

enum class RegionRole { KEYBOARD, CANDIDATE, TOOLBAR, SIDEBAR, HEADER, FOOTER, POPUP }
```

`Region` 解决了历史草稿把候选栏、工具栏、键盘割裂成多个文件的问题：现在它们是同一份布局文档里的不同 region，一起渲染、一起切换。

### 3.5 完整文档结构

```kotlin
@Serializable
data class LayoutDoc(
    val schemaVersion: Int = 2,
    val id: String,                          // layoutId，Manifest 通过它引用
    val meta: LayoutMeta = LayoutMeta(),
    val env: LayoutEnv? = null,              // 适配条件
    val root: LayoutContainer,               // 通常为 CompositeLayout
    val styles: Map<String, StyleRef> = emptyMap(),  // 本布局内的样式别名，最终解析到主题 token
    val supportedLayers: List<String> = listOf("NORMAL")  // 该布局支持的 LayoutLayer
)

@Serializable
data class LayoutMeta(
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class LayoutEnv(
    val orientation: Orientation? = null,    // portrait/landscape 适配
    val minWidthDp: Float? = null,
    val maxWidthDp: Float? = null
)
```

`LayoutDoc.root` 通常是一个 `CompositeLayout`，它把候选栏、工具栏、键盘区组合成完整视图。布局文件数量不再爆炸——一个 Locale + Script 组合一份主布局，layer 切换在同一文件内通过 `variants` 完成。

## 4. LayoutEngine —— 测量

`LayoutEngine` 把 `LayoutContainer` 树递归测量为扁平的 `MeasuredKey` 列表，供渲染层和命中测试使用。

### 4.1 测量输出

```kotlin
data class MeasuredKey(
    val key: KeyDef,
    val resolvedContent: ContentSpec,        // 已应用 layer variant 的最终内容
    val rect: RectF,
    val hitPath: Path,                       // 异形按键命中路径
    val zIndex: Int
)

data class MeasuredRegion(
    val region: Region,
    val rect: RectF,
    val keys: List<MeasuredKey>
)

data class MeasuredLayout(
    val doc: LayoutDoc,
    val regions: List<MeasuredRegion>,
    val viewWidth: Int,
    val viewHeight: Int,
    val layer: LayoutLayer                   // 测量时所用的层，用于缓存键
)
```

### 4.2 测量算法

```kotlin
class LayoutEngine {
    fun measure(
        doc: LayoutDoc,
        layer: LayoutLayer,
        viewWidth: Int,
        viewHeight: Int
    ): MeasuredLayout {
        val regions = mutableListOf<MeasuredRegion>()
        measureContainer(doc.root, 0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), layer, regions, 0)
        return MeasuredLayout(doc, regions, viewWidth, viewHeight, layer)
    }

    private fun measureContainer(
        c: LayoutContainer, x: Float, y: Float, w: Float, h: Float,
        layer: LayoutLayer, out: MutableList<MeasuredRegion>, z: Int
    ) {
        val inner = applyPadding(x, y, w, h, c.padding)
        when (c) {
            is RowLayout     -> measureRow(c, inner, layer, out, z)
            is GridLayout    -> measureGrid(c, inner, layer, out, z)
            is LinearLayout  -> measureLinear(c, inner, layer, out, z)
            is AbsoluteLayout -> measureAbsolute(c, inner, layer, out, z)
            is CompositeLayout -> measureComposite(c, inner, layer, out, z)
        }
    }
}
```

**RowLayout 测量**（权重分离，历史草稿算法保留并校正）：

```kotlin
private fun measureRow(c: RowLayout, area: RectF, layer: LayoutLayer, out: MutableList<MeasuredRegion>, z: Int) {
    if (c.keys.isEmpty()) return
    var totalWeight = 0f
    var fixedWidth = 0f
    c.keys.forEach { key ->
        when (val w = effectiveWidth(key, layer)) {
            is Dimension.Dp       -> fixedWidth += w.value
            is Dimension.Percent  -> fixedWidth += w.value * area.width()
            is Dimension.RatioW   -> fixedWidth += w.value * viewWidth
            is Dimension.Weight   -> totalWeight += w.value
            Dimension.Match, Dimension.Wrap -> totalWeight += 1f
        }
    }
    val available = (area.width() - fixedWidth - c.gap * (c.keys.size - 1)).coerceAtLeast(0f)
    var curX = area.left
    c.keys.forEach { key ->
        val kw = when (val w = effectiveWidth(key, layer)) {
            is Dimension.Dp       -> w.value
            is Dimension.Percent  -> w.value * area.width()
            is Dimension.RatioW   -> w.value * viewWidth
            is Dimension.Weight   -> if (totalWeight > 0) (w.value / totalWeight) * available else 0f
            Dimension.Match, Dimension.Wrap -> if (totalWeight > 0) (1f / totalWeight) * available else 0f
        }
        val kh = area.height()
        val content = resolveContent(key, layer)
        out.addKey(key, content, RectF(curX, area.top, curX + kw, area.top + kh), z)
        curX += kw + c.gap
    }
}
```

**GridLayout 测量**（跨行跨列，支持 T9）：

```kotlin
private fun measureGrid(c: GridLayout, area: RectF, layer: LayoutLayer, out: MutableList<MeasuredRegion>, z: Int) {
    val colWidth = (area.width() - c.colGap * (c.columns - 1)) / c.columns
    val rowCount = if (c.rows > 0) c.rows else (c.cells.maxOfOrNull { it.row + it.rowSpan } ?: 0)
    if (rowCount == 0) return
    val rowHeight = (area.height() - c.rowGap * (rowCount - 1)) / rowCount
    c.cells.forEach { cell ->
        val left = area.left + cell.col * (colWidth + c.colGap)
        val top = area.top + cell.row * (rowHeight + c.rowGap)
        val w = cell.colSpan * colWidth + c.colGap * (cell.colSpan - 1)
        val h = cell.rowSpan * rowHeight + c.rowGap * (cell.rowSpan - 1)
        val content = resolveContent(cell.key, layer)
        out.addKey(cell.key, content, RectF(left, top, left + w, top + h), z)
    }
}
```

**CompositeLayout 测量**（按 orientation 分配 region 高度）：

```kotlin
private fun measureComposite(c: CompositeLayout, area: RectF, layer: LayoutLayer, out: MutableList<MeasuredRegion>, z: Int) {
    // region 高度分配：keyboard region 用 Match/Wrap，candidate/toolbar 用固定 Dp 或 Wrap
    // 先测量固定高度 region，剩余给 keyboard region
    val (fixed, flex) = c.regions.partition { it.container.height is Dimension.Dp || it.container.height is Dimension.Percent }
    var consumed = 0f
    val isVertical = c.orientation == Orientation.VERTICAL
    // ... 按方向累加，flex region 瓜分剩余空间
}
```

### 4.3 layer 变体解析

```kotlin
private fun resolveContent(key: KeyDef, layer: LayoutLayer): ContentSpec {
    val variant = key.variants[layer.name] ?: return key.content
    return ContentSpec(
        label = variant.label ?: key.content.label,
        icon = variant.icon ?: key.content.icon,
        hint = variant.hint ?: key.content.hint
    )
}
```

这就是 `KeyboardContext.layer = SHIFTED` 时所有字母键变大写的机制——无需切布局文件，无需状态机，只读 variant。

### 4.4 异形命中路径

```kotlin
private fun buildHitPath(key: KeyDef, rect: RectF): Path {
    val path = Path()
    when (val shape = key.hitShape ?: HitShape.Rounded(cornerRadius = 0f)) {
        is HitShape.Rect    -> path.addRect(rect, Path.Direction.Cw)
        is HitShape.Circle  -> path.addCircle(rect.centerX(), rect.centerY(), min(rect.width(), rect.height()) / 2f, Path.Direction.Cw)
        is HitShape.Rounded -> path.addRoundRect(rect, shape.cornerRadius, shape.cornerRadius, Path.Direction.Cw)
    }
    return path
}
```

命中测试用 `Region.setPath` 做像素级碰撞，从 zIndex 高到低遍历：

```kotlin
fun findHit(layout: MeasuredLayout, x: Float, y: Float): MeasuredKey? {
    val allKeys = layout.regions.flatMap { it.keys }.sortedByDescending { it.zIndex }
    val touchRegion = Region()
    for (mk in allKeys) {
        val bounds = Region(mk.rect.left.toInt(), mk.rect.top.toInt(), mk.rect.right.toInt(), mk.rect.bottom.toInt())
        touchRegion.setPath(mk.hitPath, bounds)
        if (touchRegion.contains(x.toInt(), y.toInt())) return mk
    }
    return null
}
```

### 4.5 测量缓存

测量结果按 `(layoutId, layer, viewWidth, viewHeight)` 缓存。`KeyboardContext` 变化时，只有真正影响几何的字段变化才重新测量：

```kotlin
class LayoutMeasurer {
    private val cache = LruCache<String, MeasuredLayout>(maxSize = 8)

    fun measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int): MeasuredLayout {
        val key = "${doc.id}:${layer.name}:${w}x${h}"
        return cache.get(key) ?: run {
            val measured = engine.measure(doc, layer, w, h)
            cache.put(key, measured)
            measured
        }
    }

    fun invalidate(layoutId: String? = null) {
        if (layoutId == null) cache.evictAll()
        else cache.snapshot().keys.filter { it.startsWith("$layoutId:") }.forEach { cache.remove(it) }
    }
}
```

旋转屏幕、切 layoutId、切 layer 时按需失效。composing/candidates 变化只影响候选栏 region，不触发整个布局重测。

### 4.6 嵌套深度与防失控

```kotlin
private const val MAX_DEPTH = 10

private fun measureContainer(c: LayoutContainer, ..., depth: Int) {
    if (depth > MAX_DEPTH) throw LayoutConfigException("布局嵌套超过 $MAX_DEPTH 层")
    ...
}
```

解析前限制 JSON 大小（默认 1MB），`GridLayout` 限制 `columns × rows ≤ 2500`，防止恶意/错误配置导致 OOM。

## 5. LayoutRegistry —— 注册与查询

### 5.1 注册来源

布局有三个来源，统一注册到 `LayoutRegistry`：

| 来源 | 路径 | 参与构建期 method.xml |
| --- | --- | --- |
| 内置布局 | `app/src/main/assets/layouts/*.jsonc` | 是（通过 Manifest 引用） |
| 外部语言包布局 | `files/language_packs/{packageId}/layouts/*.jsonc` | 否 |
| 用户自定义布局 | `files/user_layouts/*.jsonc` | 否 |

### 5.2 注册校验

`LayoutRegistry.register` 时必须校验（未通过则注册失败，不产生半注册状态）：

- `schemaVersion` 受当前 app 支持。
- `id` 非空且在当前 registry 内唯一。
- `root` 必须是合法 `LayoutContainer`。
- 所有 `KeyDef.id` 在同一布局内唯一。
- 所有 `KeyDef.actions` 中的 `actionType` 都在合法动作集合内（§2.2）。
- 所有 `KeyDef.styleRef` 和 `LayoutContainer.styleRef` 都能在主题层解析（或回退到默认 token）。
- `GridLayout` 无单元格越界、无重叠。
- `variants` 的 key 必须是该布局 `supportedLayers` 的子集。
- 外部布局的资源路径必须在沙箱内，禁止 `../` 逃逸。

### 5.3 查询接口

```kotlin
class LayoutRegistry {
    fun register(doc: LayoutDoc, source: LayoutSource): RegisterResult
    fun unregister(layoutId: String)
    fun get(layoutId: String): LayoutDoc?
    fun listByTags(tags: List<String>): List<LayoutDoc>
    fun validate(doc: LayoutDoc): List<LayoutIssue>
}

enum class LayoutSource { BUILT_IN, LANGUAGE_PACK, USER }
```

`SchemaCapability.layoutId`（见 `orthogonal-state-management.md:4.3`）通过 `LayoutRegistry.get` 解析。`LayoutHint`（见 `android-bridge.md:4`）到 `layoutId` 的映射也走这里（见 §7.2）。

## 6. 全球化布局

### 6.1 同一排列服务多个 Schema

这是全球化布局的核心机制。物理按键排列（QWERTY）与输入行为（拼音/双拼/英文直出）解耦：

- **布局数据只描述几何与动作类型**：`KeyDef` 的 `actions.TAP.actionType = "PUSH_TOKEN"`，payload 是 `{"token": "q"}`。
- **引擎决定 token 的后续行为**：同一个 `PUSH_TOKEN "q"`，在 `PINYIN` 下进 `TableComposingEngine` 查表产生候选，在 `LATIN_DIRECT` 下进 `DirectEngine` 直接 commit `q`，在 `ROMAJI` 下进 `TransliterationEngine` 喂给 FSM。布局完全不感知这些差异。

因此：

- `zh-CN + HANI + PINYIN` 用 `qwerty.jsonc`。
- `zh-CN + HANI + DOUBLE_PINYIN` 用 `shuangpin.jsonc`（仅按键 token 定义不同，几何结构可相同）。
- `zh-CN + LATN + LATIN_DIRECT` 复用 `qwerty.jsonc`（同一物理排列，引擎不同）。
- `en-US + LATN + LATIN_DIRECT` 复用 `qwerty.jsonc`。

Manifest 通过 `layoutId` 引用布局，多个 Schema 可指向同一 `layoutId`。新增语言时，若其物理排列与现有布局兼容，直接复用，只新增 Manifest 和引擎资源。

### 6.2 候选栏与组合态显示

候选栏是布局的一个 `role: CANDIDATE` region，通过 `bindings.visibleWhen` 控制显隐：

```json
{
  "id": "candidate_region",
  "role": "CANDIDATE",
  "bindings": { "visibleWhen": "context.composing" },
  "container": {
    "type": "linear",
    "orientation": "HORIZONTAL",
    "scroll": { "enabled": true, "direction": "HORIZONTAL" },
    "children": [
      { "type": "button", "id": "prev_page", "content": { "label": "‹" }, "actions": { "TAP": { "actionType": "PAGE_PREV" } } },
      { "type": "candidate_slot", "id": "cand_0", "actions": { "TAP": { "actionType": "COMMIT_CANDIDATE", "payload": { "index": 0 } } } },
      { "type": "button", "id": "next_page", "content": { "label": "›" }, "actions": { "TAP": { "actionType": "PAGE_NEXT" } } }
    ]
  }
}
```

`visibleWhen` 表达式求值器读 `KeyboardContext`：

| 表达式 | 含义 |
| --- | --- |
| `context.composing` | `KeyboardContext.composingText` 非空 |
| `!context.composing` | 无组合态（显示工具栏） |
| `context.layer == 'SYMBOL'` | 当前是符号层 |
| `context.candidates.size > 0` | 有候选 |

候选 slot 的内容（文本、高亮）由渲染层从 `KeyboardContext.candidates[index]` 填充，布局只定义结构和点击动作。候选栏不查字典、不保存 buffer（`core.md:90`）。

### 6.3 多 Script 共存

同一 Locale 下多个 Script（如 `zh-CN` 下 `HANI` 和 `LATN`）共用同一份主布局，通过 `SWITCH_SCRIPT` 动作在 region 间切换。布局不需要为每个 Script 写一份文件——Script 切换由状态层处理，布局只读 `KeyboardContext.orthogonal.script` 决定是否显示某些 region 或变体。

### 6.4 RTL 支持

布局层支持 RTL（阿拉伯语、希伯来语）：

- `RowLayout` 和 `LinearLayout` 在 RTL Locale 下自动镜像排列方向。
- `Gravity.START` / `Gravity.END` 在 RTL 下语义反转。
- 按键内容（label）的文本方向由系统 `Bidi` 决定，布局不干预。

## 7. 布局选择与 EditorProfile

### 7.1 layoutId 的流转

```text
Manifest.defaults.layoutId  ──┐
SchemaCapability.layoutId   ──┼──> KeyboardContext.layoutId ──> LayoutRegistry.get ──> LayoutDoc
EditorProfile.layoutHint    ──┘         (状态层唯一来源)              (布局层查询)
```

`KeyboardContext.layoutId` 是布局渲染的唯一输入。它由状态层根据 Manifest 默认值、当前 Schema、用户覆盖综合决定。

### 7.2 LayoutHint 覆盖

`EditorInfoResolver`（`android-bridge.md:4`）产出 `EditorProfile.layoutHint`，布局层提供 `LayoutHint` 到 `layoutId` 的映射表：

```kotlin
class LayoutHintResolver(private val registry: LayoutRegistry) {
    fun resolve(hint: LayoutHint, currentLayoutId: String): String {
        return when (hint) {
            LayoutHint.NUMBER  -> registry.findBuiltIn("numeric") ?: currentLayoutId
            LayoutHint.PHONE   -> registry.findBuiltIn("phone") ?: currentLayoutId
            LayoutHint.DATETIME -> registry.findBuiltIn("datetime") ?: currentLayoutId
            LayoutHint.PASSWORD -> currentLayoutId  // 用当前布局，但禁候选
            LayoutHint.EMAIL, LayoutHint.URL -> currentLayoutId  // 建议 LATIN_DIRECT 由状态层处理
            LayoutHint.ALPHA -> currentLayoutId
        }
    }
}
```

映射规则在布局层定义（因为布局层知道自己有哪些内置布局），但最终是否切换由状态层决定（`KeyboardContextManager.applyEditorProfile`）。密码框不强切布局，只让候选 region 隐藏（`EditorProfile.candidateDisabled` 作用于 `bindings.visibleWhen`）。

## 8. 高度自定义

### 8.1 关注点分离

历史草稿把几何、动作、颜色焊死在一棵树里，导致换主题要改布局、换 Dvorak 要复制整个文件。v2.0 实现四层正交分离：

| 维度 | 数据归属 | 替换粒度 |
| --- | --- | --- |
| **几何结构**（按键位置、容器类型） | 布局 `LayoutDoc` | 换布局文件 |
| **输入行为**（按键做什么） | 布局 `KeyDef.actions` + 引擎 Schema | 换 Schema（Manifest 层） |
| **视觉外观**（颜色、字号、圆角） | 主题 token（`core.md:8`） | 换主题文件 |
| **组合区域**（候选栏、工具栏） | 布局 `Region` + `bindings` | 改 region 结构 |

四层独立替换：

- 换 Dvorak：只换布局文件的 `RowLayout.keys` 排列，行为/主题/候选结构不动。
- 拼音切双拼：只换 `SchemaCapability`（Manifest 层），布局可复用同一份 QWERTY。
- 换暗色主题：只换主题 token，布局文件零改动（因为布局只引用 `styleRef`）。
- 调候选栏高度：只改候选 region 的 `height`，键盘 region 不动。

### 8.2 跨布局复用与继承

支持布局继承，避免复制：

```json
{
  "schemaVersion": 2,
  "id": "qwerty_dvorak",
  "extends": "qwerty",
  "patches": [
    { "op": "swap_row_keys", "rowId": "r1", "keys": ["'", ",", ".", "p", "y", "f", "g", "c", "r", "l"] }
  ]
}
```

`LayoutRegistry` 解析时先加载 `qwerty`，再应用 patches。Dvorak 布局文件只声明差异，不是 200 行复制。Patch 操作集合：

| op | 作用 |
| --- | --- |
| `swap_row_keys` | 替换某行的按键列表 |
| `replace_key` | 替换单个按键定义 |
| `insert_key_after` / `insert_key_before` | 插入按键 |
| `remove_key` | 删除按键 |
| `override_style` | 覆盖某按键的 styleRef |
| `override_region` | 覆盖某 region 的容器 |

继承链限制深度（默认 3 层），防止循环。

### 8.3 导入导出

用户自定义布局通过设置页导入导出 `.jsonc` 文件：

- 导入时经 `LayoutRegistry.validate` 全量校验，失败给出明确错误（行号 + 原因）。
- 导入的布局标记为 `LayoutSource.USER`，不参与构建期 method.xml。
- 用户布局可绑定到任意已启用 Locale 的 Schema（在设置页选择），写入 `SettingsManager`。
- 导出时连同其依赖的主题 token 引用一起导出（主题本身单独导出，见主题文档）。

### 8.4 可视化编辑（roadmap）

首版只支持 JSONC 编辑 + 实时预览。可视化拖拽编辑列入后续 roadmap，不在 v2.0 范围内。预览组件复用 `LayoutRenderer`，传入模拟的 `KeyboardContext`。

## 9. LayoutRenderer —— 渲染

### 9.1 渲染策略

- **常规布局（按键 < 100）**：Compose `Canvas` 自绘，遍历 `MeasuredLayout.regions[].keys`，避免为每个按键创建独立 Composable（`layout.md` 历史草稿的顾虑保留）。
- **海量布局（Emoji 页 1000+ 项）**：`LazyVerticalGrid`，此时 `LayoutEngine` 只算列宽行高，每项按需测量。
- 渲染层只读 `MeasuredLayout` + 主题 token，不读原始 `LayoutDoc`，不重新测量。

### 9.2 Compose 渲染入口

```kotlin
@Composable
fun LayoutRenderer(
    measured: MeasuredLayout,
    context: KeyboardContext,
    themeResolver: ThemeResolver,
    onGesture: (KeyDef, GestureType) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        measured.regions.forEach { region ->
            if (!evaluateBindings(region.bindings, context)) return@forEach
            region.keys.forEach { mk ->
                drawKey(mk, themeResolver, context)
            }
        }
    }
    // 手势识别层（overlay）
    GestureOverlay(measured, onGesture, modifier)
}
```

`onGesture` 回调把 `(KeyDef, GestureType)` 交给 `ActionDispatcher`，由它查 `KeyDef.actions[gesture]` 转成 `InputAction` 发给 `InputPipeline`。

### 9.3 ActionDispatcher —— 动作派发

```kotlin
class ActionDispatcher(private val pipeline: InputPipeline) {
    fun dispatch(key: KeyDef, gesture: GestureType) {
        val action = key.actions.gestures[gesture] ?: return
        val inputAction = when (action.actionType) {
            "PUSH_TOKEN"            -> InputAction.PushToken(action.payload["token"]?.jsonPrimitive?.content ?: "")
            "DELETE"                -> InputAction.Delete
            "SPACE"                 -> InputAction.Space
            "ENTER"                 -> InputAction.Enter
            "SWITCH_LOCALE"         -> InputAction.SwitchLocale(parse(action.payload, "locale"))
            "SWITCH_SCRIPT"         -> InputAction.SwitchScript(parse(action.payload, "script"))
            "SWITCH_SCHEMA"         -> InputAction.SwitchSchema(parse(action.payload, "schema"))
            "SWITCH_LAYER"          -> InputAction.SwitchLayer(parse(action.payload, "layer"))
            "COMMIT_CANDIDATE"      -> InputAction.CommitCandidate(action.payload["index"]?.jsonPrimitive?.intOrNull ?: 0)
            "OPEN_PANEL"            -> InputAction.OpenPanel(parse(action.payload, "panel"))
            "CLOSE_PANEL"           -> InputAction.ClosePanel
            "RESTORE_PREVIOUS_SCHEMA" -> InputAction.RestorePreviousSchema
            "PAGE_NEXT", "PAGE_PREV"-> InputAction.PageCandidate(if (action.actionType == "PAGE_NEXT") 1 else -1)
            else -> InputAction.Noop
        }
        pipeline.handle(inputAction)
    }
}
```

这是对当前 `ComposeInputView.mapKeyToAction`（keyId 硬编码）的彻底替代。渲染器不再认识 `shift`/`backspace`/`space` 这些字符串，只认数据驱动的动作。

### 9.4 主题消费

```kotlin
private fun DrawScope.drawKey(mk: MeasuredKey, themeResolver: ThemeResolver, context: KeyboardContext) {
    val styleId = mk.key.styleRef ?: "key_default"
    val style = themeResolver.resolveKeyStyle(styleId)
    val isPressed = mk.key.id == pressedKeyId
    val bgColor = if (isPressed) style.pressedBackground else style.background
    drawRoundRect(color = bgColor, topLeft = Offset(mk.rect.left, mk.rect.top),
                  size = Size(mk.rect.width, mk.rect.height), cornerRadius = CornerRadius(style.cornerRadius))
    // label / icon / hint 全部从 style 取 fontSize/textColor
}
```

**禁止**在布局渲染中出现 `Color(0xFF...)` 字面量，所有颜色来自 `themeResolver.resolveKeyStyle(styleRef)`。`ThemeResolver` 定义见主题文档（`core.md:8`）。

## 10. 布局事件流（完整链路）

一次拼音输入 `ni` 的完整流转，展示布局层在体系中的位置：

```text
1. 用户点 'n' 键
   -> GestureOverlay 识别 TAP
   -> ActionDispatcher 查 key_n.actions.TAP = {actionType: PUSH_TOKEN, payload: {token: "n"}}
   -> InputAction.PushToken("n")
   -> InputPipeline.handle()

2. Pipeline 转 InputEvent.PushToken("n")
   -> InputSession.handle() (TableComposingEngine, identity encoder)
   -> rawBuffer = "n", queryBuffer = "n"
   -> Dictionary 查询 "n"
   -> EngineResult.UpdateComposing(composing="n", candidates=[你, 尼, ...])

3. Pipeline 执行结果
   -> InputConnectionGateway.setComposingText("n")
   -> KeyboardContextManager.setComposing("n", [你, 尼, ...])

4. KeyboardContext 变化
   -> LayoutRenderer 重组
   -> candidate region 的 visibleWhen "context.composing" 变 true，显示候选栏
   -> toolbar region 的 visibleWhen "!context.composing" 变 false，隐藏工具栏
```

布局层全程不查字典、不 commit、不持有 buffer，只观察 `KeyboardContext` 重绘、把触摸转成动作。

## 11. 首版实现要求

首版必须实现：

- `LayoutDoc` 及全部节点数据模型（`Dimension`、`KeyDef`、5 种 `LayoutContainer`、`Region`）。
- `LayoutParser` 支持 JSONC，解析 `LayoutDoc`。
- `LayoutEngine` 实现 Row / Grid / Linear / Absolute / Composite 五种测量。
- `MeasuredLayout` + LRU 缓存 + layer 变体解析。
- 异形 `hitPath` 命中测试。
- `LayoutRegistry` 注册、校验、查询。
- `LayoutRenderer` Compose 渲染，消费主题 token，零硬编码颜色。
- `ActionDispatcher`，替代 `ComposeInputView.mapKeyToAction`。
- 布局继承与 patch（`swap_row_keys` / `replace_key` 等）。
- 内置布局迁移：`qwerty` / `shuangpin` / `t9` / `numeric` / `phone` / `datetime` / `symbols` 全部从 v1 重写为 v2 格式，删除 `test_qwerty.json` 等废弃文件。

首版可暂缓：

- 可视化拖拽编辑器（roadmap）。
- 异形按键（Circle / Rounded）的高级渲染——先支持 Rect。
- RTL Locale 的完整镜像（先支持 LTR，RTL 列入后续）。

## 12. 测试计划

### 12.1 数据模型与解析

- JSONC 解析能剥离 `//` 注释和尾逗号。
- 5 种 `LayoutContainer` 都能正确反序列化。
- `Dimension` 多态（match/wrap/weight/dp/percent/ratio_w）解析正确。
- `KeyDef.variants` 按 `LayoutLayer` 名解析。

### 12.2 测量算法

- `RowLayout` 权重分配：3 个 `Weight(1f)` + 1 个 `Weight(2f)` 在宽 300 容器内宽度比为 1:1:1:2。
- `GridLayout` 跨列：`colSpan=2` 的 cell 宽度 = 2 列 + 1 gap。
- `GridLayout` 越界检测：`col + colSpan > columns` 抛 `LayoutConfigException`。
- `AbsoluteLayout` 锚点：`BOTTOM_RIGHT` anchor 的 item 定位正确。
- `CompositeLayout` 垂直方向：candidate region 固定 40dp，keyboard region 占剩余高度。
- 嵌套深度超 10 层抛异常。

### 12.3 layer 变体

- `layer=NORMAL` 时字母键 label 为小写。
- `layer=SHIFTED` 时 label 为大写（来自 variant）。
- `layer=SYMBOL` 时 region 切换（来自 region.bindings.visibleWhen）。

### 12.4 命中测试

- 矩形按键点击中心命中。
- 圆形按键点击角落不命中。
- zIndex 高的按键优先命中。

### 12.5 动作派发

- `key_q.actions.TAP = PUSH_TOKEN "q"` → `InputAction.PushToken("q")`。
- `SWITCH_SCRIPT` payload 解析正确。
- 未知 `actionType` → 注册失败。
- 删除 `mapKeyToAction` 后，所有按键行为来自 `KeyDef.actions`。

### 12.6 全球化

- 同一份 `qwerty.jsonc` 在 `PINYIN` 和 `LATIN_DIRECT` 下行为不同（前者进 composing，后者直接 commit），证明排列与行为解耦。
- 候选 region 在 `context.composing` 时显示，无 composing 时隐藏。
- 多 Script 共用同一布局文件，切 Script 不切布局文件。

### 12.7 自定义

- 布局继承：`qwerty_dvorak extends qwerty` 只声明 patch，解析后等价于完整 Dvorak。
- 用户导入非法 JSONC → 校验失败并返回行号。
- 用户布局标记为 `USER`，不进 method.xml。

### 12.8 构建验收

```bash
./gradlew test
./gradlew assembleDebug
```

通过并生成 APK。本次仅新增设计文档时，不要求运行 Gradle 构建。

## 13. 关键原则

- 布局只描述几何与展示，不承载输入语义、不持有语言状态、不直接调 `InputConnection`。
- 布局渲染的唯一运行时输入是 `KeyboardContext`；唯一输出口是 `InputPipeline`（经 `ActionDispatcher`）。
- 物理排列（几何）与输入行为（Schema）解耦：同一布局服务多个 Schema，同一 Schema 可换用多个布局。
- 按键动作全部数据化，渲染器不硬编码 keyId→动作映射；`ComposeInputView.mapKeyToAction` 必须删除。
- 视觉归主题层：按键只引用 `styleRef` token，禁止硬编码颜色。
- 候选栏、工具栏、键盘区是同一份布局文档的不同 `Region`，通过 `bindings.visibleWhen` 协作显隐。
- `LayoutLayer` 切换（Shift、符号、数字）通过 `KeyDef.variants` 在同一文件内完成，不切布局文件。
- 布局数据来自内置 / 语言包 / 用户三源，统一经 `LayoutRegistry` 注册校验。
- 布局继承与 patch 避免复制，Dvorak 等变体只声明差异。
- 测量结果按 `(layoutId, layer, w, h)` 缓存，状态变化按需失效，composing 变化不触发整体重测。
- 嵌套深度、文件大小、网格规模受限，防止恶意配置导致 OOM。
- 所有用户可见文案必须 i18n，布局 label 支持 `@string/...` 前缀引用。
