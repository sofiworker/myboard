package xyz.xiao6.myboard.core.contract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * TextRef：可校验的 i18n 文本引用。
 * - Raw：仅允许键面符号、单字符 token、调试文案
 * - StringResource：引用 @string/...
 * - LocalizedMap：语言包内部显示名
 */
@Serializable
sealed class TextRef {
    @Serializable @SerialName("raw")
    data class Raw(val value: String) : TextRef()
    
    @Serializable @SerialName("stringRes")
    data class StringResource(val key: String) : TextRef()
    
    @Serializable @SerialName("localized")
    data class LocalizedMap(val values: Map<String, String>) : TextRef()
}

/**
 * 尺寸多态类型。
 */
@Serializable
sealed class Dimension {
    @Serializable @SerialName("match")   data object Match : Dimension()
    @Serializable @SerialName("wrap")    data object Wrap : Dimension()
    @Serializable @SerialName("weight")  data class Weight(val value: Float = 1f) : Dimension()
    @Serializable @SerialName("dp")      data class Dp(val value: Float) : Dimension()
    @Serializable @SerialName("percent") data class Percent(val value: Float) : Dimension()
    @Serializable @SerialName("ratio_w") data class RatioW(val value: Float) : Dimension()
}

/**
 * 方向。
 */
enum class Orientation { VERTICAL, HORIZONTAL }

/**
 * 对齐/分布方式。
 */
enum class Gravity { START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY, STRETCH }

/**
 * 角标位置。
 */
enum class HintPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * 手势类型。
 */
enum class GestureType {
    TAP, LONG_PRESS, DOUBLE_TAP, REPEAT,
    SWIPE_UP, SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT
}

/**
 * 内边距。
 */
@Serializable
data class BoxSpacing(
    val start: Float = 0f,
    val top: Float = 0f,
    val end: Float = 0f,
    val bottom: Float = 0f
)

/**
 * 滚动规格。
 */
@Serializable
data class ScrollSpec(
    val enabled: Boolean = true,
    val direction: Orientation = Orientation.HORIZONTAL,
    val paging: Boolean = false
)

/**
 * Region 语义角色。
 */
enum class RegionRole { KEYBOARD, CANDIDATE, TOOLBAR, SIDEBAR, HEADER, FOOTER, POPUP }

/**
 * 动作定义。
 */
@Serializable
data class ActionDef(
    val actionType: String,
    val payload: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()
)

/**
 * 动作映射（按手势类型）。
 */
@Serializable
data class ActionMap(
    val gestures: Map<GestureType, ActionDef> = emptyMap()
)

/**
 * 按键内容规格。
 */
@Serializable
data class ContentSpec(
    val label: String? = null,
    val icon: String? = null,
    val hint: Map<HintPosition, String> = emptyMap()
)

/**
 * Layer 变体 patch。
 */
@Serializable
data class VariantPatch(
    val label: String? = null,
    val icon: String? = null,
    val hint: Map<HintPosition, String>? = null,
    val visible: Boolean = true
)

/**
 * 异形按键命中区域。
 */
@Serializable
sealed class HitShape {
    @Serializable @SerialName("rect")    data class Rect(val cornerRadius: Float = 0f) : HitShape()
    @Serializable @SerialName("circle")  data class Circle(val cx: Float, val cy: Float, val r: Float) : HitShape()
    @Serializable @SerialName("rounded") data class Rounded(val cornerRadius: Float) : HitShape()
}

/**
 * 按键定义。
 */
@Serializable
data class KeyDef(
    val id: String,
    val styleRef: String? = null,
    val content: ContentSpec = ContentSpec(),
    val actions: ActionMap = ActionMap(),
    val width: Dimension = Dimension.Weight(1f),
    val height: Dimension = Dimension.Match,
    val variants: Map<String, VariantPatch> = emptyMap(),
    val hitShape: HitShape? = null,
    val repeatable: Boolean = false,
    val longPressDelay: Int = 400,
    val repeatInterval: Int = 50
)

/**
 * Bindings：region 显隐/启用条件。
 */
@Serializable
data class Bindings(
    val visibleWhen: String? = null,
    val enabledWhen: String? = null
)

/**
 * 布局容器（5 种布局类型的 sealed class）。
 */
@Serializable
sealed class LayoutContainer {
    abstract val id: String
    abstract val width: Dimension
    abstract val height: Dimension
    abstract val padding: BoxSpacing
    abstract val styleRef: String?
    abstract val bindings: Bindings?
}

/**
 * 行容器。
 */
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

/**
 * 网格容器。
 */
@Serializable @SerialName("grid")
data class GridLayout(
    override val id: String,
    val columns: Int,
    val rows: Int = 0,
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
        val col: Int,
        val row: Int,
        val colSpan: Int = 1,
        val rowSpan: Int = 1
    )
}

/**
 * 线性容器（候选栏、工具栏）。
 */
@Serializable @SerialName("linear")
data class LinearLayout(
    override val id: String,
    val orientation: Orientation,
    val children: List<LayoutNode>,
    val gap: Float = 0f,
    val gravity: Gravity = Gravity.START,
    val scroll: ScrollSpec? = null,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Wrap,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer()

/**
 * 绝对定位容器（浮层、特殊键）。
 */
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
        val x: Dimension,
        val y: Dimension,
        val width: Dimension,
        val height: Dimension,
        val anchor: HintPosition = HintPosition.TOP_LEFT,
        val zIndex: Int = 0
    )
}

/**
 * 复合容器（键盘 + 候选 + 工具一体化）。
 */
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

/**
 * Region：带语义角色的容器包装。
 */
@Serializable
data class Region(
    val id: String,
    val role: RegionRole,
    val container: LayoutContainer,
    val bindings: Bindings? = null
)

/**
 * 布局节点（可嵌套）。
 * 首版只支持 KeyDef 和 Spacer。
 */
@Serializable
sealed class LayoutNode {
    @Serializable @SerialName("key")
    data class KeyNode(val key: KeyDef) : LayoutNode()
    
    @Serializable @SerialName("spacer")
    data class SpacerNode(val width: Dimension = Dimension.Weight(1f)) : LayoutNode()
}

/**
 * 布局元数据。
 */
@Serializable
data class LayoutMeta(
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList()
)

/**
 * 布局环境适配条件。
 */
@Serializable
data class LayoutEnv(
    val orientation: Orientation? = null,
    val minWidthDp: Float? = null,
    val maxWidthDp: Float? = null
)

/**
 * 布局文档。
 */
@Serializable
data class LayoutDoc(
    val schemaVersion: Int = 2,
    val id: String,
    val meta: LayoutMeta = LayoutMeta(),
    val env: LayoutEnv? = null,
    val root: LayoutContainer,
    val extends: String? = null,
    val patches: List<PatchOp> = emptyList(),
    val styles: Map<String, StyleRef> = emptyMap(),
    val supportedLayers: List<String> = listOf("NORMAL")
)

/**
 * 本布局内的样式别名。
 */
@Serializable
data class StyleRef(
    val token: String
)

/**
 * 布局 Patch 操作。
 */
@Serializable
sealed class PatchOp {
    @Serializable @SerialName("swap_row_keys")
    data class SwapRowKeys(val rowId: String, val keys: List<KeyDef>) : PatchOp()
    
    @Serializable @SerialName("replace_key")
    data class ReplaceKey(val keyId: String, val key: KeyDef) : PatchOp()
    
    @Serializable @SerialName("insert_key_after")
    data class InsertKeyAfter(val afterKeyId: String, val key: KeyDef) : PatchOp()
    
    @Serializable @SerialName("insert_key_before")
    data class InsertKeyBefore(val beforeKeyId: String, val key: KeyDef) : PatchOp()
    
    @Serializable @SerialName("remove_key")
    data class RemoveKey(val keyId: String) : PatchOp()
    
    @Serializable @SerialName("override_style")
    data class OverrideStyle(val keyId: String, val styleRef: String) : PatchOp()
    
    @Serializable @SerialName("override_region")
    data class OverrideRegion(val regionId: String, val container: LayoutContainer) : PatchOp()
}

/**
 * 测量输出：按键。
 */
data class MeasuredKey(
    val key: KeyDef,
    val resolvedContent: ContentSpec,
    val rect: android.graphics.RectF,
    val hitPath: android.graphics.Path,
    val zIndex: Int
)

/**
 * 测量输出：Region。
 */
data class MeasuredRegion(
    val region: Region,
    val rect: android.graphics.RectF,
    val keys: List<MeasuredKey>
)

/**
 * 测量输出：完整布局。
 */
data class MeasuredLayout(
    val doc: LayoutDoc,
    val regions: List<MeasuredRegion>,
    val viewWidth: Int,
    val viewHeight: Int,
    val layer: LayoutLayer
)

/**
 * 候选位节点（CandidateSlotNode）。
 * 候选 slot 的内容由渲染层从 KeyboardContext.candidates[index] 填充。
 */
@Serializable
data class CandidateSlotNode(
    val index: Int,
    val styleRef: String? = null
)