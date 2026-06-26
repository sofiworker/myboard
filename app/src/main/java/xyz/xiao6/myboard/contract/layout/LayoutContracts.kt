package xyz.xiao6.myboard.contract.layout

import android.graphics.Path
import android.graphics.RectF
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 按键模型。
 * type=CHAR 时 TAP 动作自动推断为 commitToken(output 或 id)。
 * 其他 KeyType 有默认 TAP 动作（见 ActionResolver）。
 * longPress / swipeActions 优先级高于默认 TAP 动作。
 */
@Serializable
data class KeyModel(
    val id: String,
    val type: KeyType = KeyType.CHAR,
    val label: String? = null,
    val output: String? = null,
    val icon: String? = null,
    val width: Dimension? = null,
    val height: Dimension? = null,
    val hint: Map<HintPosition, String> = emptyMap(),
    val longPress: List<KeyAction>? = null,
    val swipeActions: Map<Direction, KeyAction>? = null,
    val style: String? = null
)

@Serializable
enum class KeyType {
    CHAR, FUNCTION, SPACE, ENTER, BACKSPACE, SHIFT,
    SYMBOL_SWITCH, EMOJI_SWITCH, EMPTY, PLACEHOLDER
}

@Serializable
data class KeyAction(
    val action: String,
    val payload: Map<String, String> = emptyMap()
)

@Serializable
enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * 角标位置。
 */
@Serializable
enum class HintPosition {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
}

/**
 * 尺寸多态类型。
 * JSONC 简化表达通过自定义解析器实现：
 * null → MATCH_PARENT, -1 → WRAP_CONTENT,
 * 5 → Weight(5), 40 → Dp(40)
 */
@Serializable
sealed class Dimension {
    @Serializable @SerialName("match") data object MATCH_PARENT : Dimension()
    @Serializable @SerialName("wrap") data object WRAP_CONTENT : Dimension()
    @Serializable @SerialName("weight") data class Weight(val value: Float = 1f) : Dimension()
    @Serializable @SerialName("dp") data class Dp(val value: Float) : Dimension()
}

/**
 * 布局容器（5 种类型）。
 */
@Serializable
sealed class LayoutContainer {
    abstract val width: Dimension?
    abstract val height: Dimension?
}

/**
 * 1. 行布局（标准 QWERTY 行）。
 */
@Serializable @SerialName("row")
data class RowLayout(
    val keys: List<KeyModel>,
    val keySpacing: Float = 0f,
    override val width: Dimension? = null,
    override val height: Dimension? = null
) : LayoutContainer()

/**
 * 2. 网格布局（T9/数字键盘）。
 */
@Serializable @SerialName("grid")
data class GridLayout(
    val rows: Int,
    val cols: Int,
    val keys: List<GridKey>,
    val rowSpacing: Float = 0f,
    val colSpacing: Float = 0f,
    override val width: Dimension? = null,
    override val height: Dimension? = null
) : LayoutContainer() {
    @Serializable
    data class GridKey(
        val key: KeyModel,
        val row: Int,
        val col: Int,
        val rowSpan: Int = 1,
        val colSpan: Int = 1
    )
}

/**
 * 3. 流式布局（符号页、自适应换行）。
 */
@Serializable @SerialName("flow")
data class FlowLayout(
    val keys: List<KeyModel>,
    val maxCols: Int? = null,
    val horizontalSpacing: Float = 0f,
    val verticalSpacing: Float = 0f,
    override val width: Dimension? = null,
    override val height: Dimension? = null
) : LayoutContainer()

/**
 * 4. 绝对布局（不规则跨行跨列）。
 */
@Serializable @SerialName("absolute")
data class AbsoluteLayout(
    val keys: List<AbsoluteKey>,
    override val width: Dimension? = null,
    override val height: Dimension? = null
) : LayoutContainer() {
    @Serializable
    data class AbsoluteKey(
        val key: KeyModel,
        val x: Float,
        val y: Float,
        val w: Float,
        val h: Float,
        val anchor: HintPosition = HintPosition.TOP_LEFT,
        val zIndex: Int = 0
    )
}

/**
 * 5. 复合布局（容器嵌套）。
 */
@Serializable @SerialName("composite")
data class CompositeLayout(
    val children: List<LayoutContainer>,
    val orientation: String = "vertical",
    override val width: Dimension? = null,
    override val height: Dimension? = null
) : LayoutContainer()

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
 * 布局文档。
 */
@Serializable
data class LayoutDoc(
    val schemaVersion: String = "1.0.0",
    val id: String,
    val meta: LayoutMeta? = null,
    val supportedLayers: List<String>? = null,
    val layout: LayoutContainer
)

/**
 * 测量输出：按键。
 */
data class MeasuredKey(
    val key: KeyModel,
    val rect: RectF,
    val hitPath: Path? = null,
    val zIndex: Int = 0
)

/**
 * 测量输出：完整布局（拍平为全量 key 列表）。
 */
data class MeasuredLayout(
    val doc: LayoutDoc,
    val keys: List<MeasuredKey>,
    val viewWidth: Int,
    val viewHeight: Int
)
