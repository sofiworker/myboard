package xyz.xiao6.myboard.contract.layout

import android.graphics.Path
import android.graphics.RectF
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import xyz.xiao6.myboard.common.SchemaVersion
import xyz.xiao6.myboard.contract.language.SemVer
import xyz.xiao6.myboard.contract.state.LayoutLayer
import xyz.xiao6.myboard.layout.LayoutRegistry

/**
 * Versioned identity of a registered layout resource.
 *
 * A layout document name is scoped to its package, and package versions remain
 * part of the runtime identity even though they are intentionally omitted from
 * persisted context state.
 */
data class LayoutKey(
    val packageId: String,
    val layoutId: String,
    val packageVersion: SemVer
) {
    init {
        require(packageId.isNotBlank() && ':' !in packageId) { "Layout package ID must be non-blank and must not contain ':'" }
        require(layoutId.isNotBlank() && ':' !in layoutId) { "Layout ID must be non-blank and must not contain ':'" }
    }
}

/** Version-independent layout ID persisted by [xyz.xiao6.myboard.contract.state.KeyboardContext]. */
@JvmInline
value class LayoutCanonicalId(val value: String) {
    init {
        parse(value)
    }

    internal fun components(): Pair<String, String> = parse(value)

    companion object {
        fun of(packageId: String, layoutId: String): LayoutCanonicalId {
            require(packageId.isNotBlank() && ':' !in packageId) { "Layout package ID must be non-blank and must not contain ':'" }
            require(layoutId.isNotBlank() && ':' !in layoutId) { "Layout ID must be non-blank and must not contain ':'" }
            return LayoutCanonicalId("$packageId:$layoutId")
        }

        fun parse(value: String): Pair<String, String> {
            val separator = value.indexOf(':')
            require(separator > 0 && separator < value.lastIndex && value.indexOf(':', separator + 1) == -1) {
                "Invalid layout canonical ID"
            }
            return value.substring(0, separator) to value.substring(separator + 1)
        }
    }
}

fun LayoutKey.toCanonicalId(): LayoutCanonicalId = LayoutCanonicalId.of(packageId, layoutId)

/** Restores a versioned key only after the caller has selected a package version. */
fun LayoutCanonicalId.resolve(packageVersion: SemVer, registry: LayoutRegistry): LayoutKey {
    val (packageId, layoutId) = components()
    return registry.resolve(packageId, layoutId, packageVersion)
}

/**
 * Layout v2 data contract.
 *
 * The model matches docs/layout.md and bundled layout JSONC root/key structure.
 * Layouts describe key geometry, presentation, actions and optional internal
 * regions. Keyboard chrome such as inline toolbar/candidate switching is owned
 * by the page frame.
 */

@Serializable
sealed class Dimension {
    @Serializable
    @SerialName("match")
    data object Match : Dimension()

    @Serializable
    @SerialName("wrap")
    data object Wrap : Dimension()

    @Serializable
    @SerialName("weight")
    data class Weight(val value: Float = 1f) : Dimension()

    @Serializable
    @SerialName("dp")
    data class Dp(val value: Float) : Dimension()

    @Serializable
    @SerialName("percent")
    data class Percent(val value: Float) : Dimension()

    @Serializable
    @SerialName("ratio_w")
    data class RatioW(val value: Float) : Dimension()
}

@Serializable
data class BoxSpacing(
    val start: Float = 0f,
    val top: Float = 0f,
    val end: Float = 0f,
    val bottom: Float = 0f
)

@Serializable
enum class Orientation {
    VERTICAL,
    HORIZONTAL
}

@Serializable
enum class Gravity {
    START,
    CENTER,
    END,
    SPACE_BETWEEN,
    SPACE_AROUND,
    SPACE_EVENLY,
    STRETCH
}

@Serializable
enum class HintPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

@Serializable
enum class GestureType {
    TAP,
    LONG_PRESS,
    DOUBLE_TAP,
    REPEAT,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT
}

@Serializable
enum class LayoutActionType {
    PUSH_TOKEN,
    COMMIT_TEXT,
    DELETE,
    SPACE,
    ENTER,
    SWITCH_LAYER,
    CYCLE_LAYER,
    SWITCH_LOCALE,
    SWITCH_SCRIPT,
    SWITCH_SCHEMA,
    OPEN_PANEL,
    CLOSE_PANEL,
    COMMIT_CANDIDATE,
    PAGE_NEXT,
    PAGE_PREV,
    PAGE_CANDIDATE,
    RESTORE_PREVIOUS_SCHEMA,
    NOOP
}

@Serializable
data class ActionDef(
    val actionType: LayoutActionType,
    val payload: Map<String, JsonElement> = emptyMap()
)

@Serializable
data class ActionMap(
    val gestures: Map<GestureType, ActionDef> = emptyMap()
)

@Serializable
data class ContentSpec(
    val label: String? = null,
    val icon: String? = null,
    val hint: Map<HintPosition, String> = emptyMap()
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
    @Serializable
    @SerialName("rect")
    data class Rect(val cornerRadius: Float = 0f) : HitShape()

    @Serializable
    @SerialName("circle")
    data class Circle(val cx: Float, val cy: Float, val r: Float) : HitShape()

    @Serializable
    @SerialName("rounded")
    data class Rounded(val cornerRadius: Float) : HitShape()
}

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

@Serializable
data class Bindings(
    val visibleWhen: String? = null,
    val enabledWhen: String? = null
)

@Serializable
sealed class LayoutNode {
    @Serializable
    @SerialName("key")
    data class KeyNode(val key: KeyDef) : LayoutNode()

    @Serializable
    @SerialName("spacer")
    data class SpacerNode(
        val width: Dimension = Dimension.Weight(1f),
        val height: Dimension = Dimension.Match
    ) : LayoutNode()

    @Serializable
    @SerialName("divider")
    data class DividerNode(
        val width: Dimension = Dimension.Dp(1f),
        val height: Dimension = Dimension.Match
    ) : LayoutNode()
}

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
@SerialName("row")
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

@Serializable
@SerialName("grid")
data class GridLayout(
    override val id: String,
    val columns: Int,
    val rows: Float = 0f,
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
        val col: Float,
        val row: Float,
        val colSpan: Float = 1f,
        val rowSpan: Float = 1f
    )
}

@Serializable
@SerialName("linear")
data class LinearLayout(
    override val id: String,
    val orientation: Orientation,
    val children: List<LayoutNode> = emptyList(),
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

@Serializable
@SerialName("absolute")
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

@Serializable
@SerialName("composite")
data class CompositeLayout(
    override val id: String,
    val orientation: Orientation,
    val regions: List<Region>,
    val gap: Float = 0f,
    override val width: Dimension = Dimension.Match,
    override val height: Dimension = Dimension.Match,
    override val padding: BoxSpacing = BoxSpacing(),
    override val styleRef: String? = null,
    override val bindings: Bindings? = null
) : LayoutContainer()

@Serializable
data class Region(
    val id: String,
    val role: String? = null,
    val tags: List<String> = emptyList(),
    val container: LayoutContainer,
    val bindings: Bindings? = null
)

@Serializable
enum class LayoutPresentationMode {
    CHROME_AND_CONTENT,
    FULL_SURFACE
}

@Serializable
data class LayoutMeta(
    val name: String? = null,
    val description: String? = null,
    val author: String? = null,
    val tags: List<String> = emptyList()
)

@Serializable
data class LayoutEnv(
    val orientation: Orientation? = null,
    val minWidthDp: Float? = null,
    val maxWidthDp: Float? = null
)

@Serializable
data class LayoutDoc(
    val schemaVersion: String = SchemaVersion.CURRENT_STR,
    val id: String,
    val meta: LayoutMeta = LayoutMeta(),
    val env: LayoutEnv? = null,
    val presentationMode: LayoutPresentationMode = LayoutPresentationMode.CHROME_AND_CONTENT,
    val root: LayoutContainer,
    val supportedLayers: List<LayoutLayer> = listOf(LayoutLayer.NORMAL)
)

data class MeasuredKey(
    val key: KeyDef,
    val resolvedContent: ContentSpec,
    val rect: RectF,
    val hitPath: Path,
    val zIndex: Int = 0
)

data class MeasuredLayout(
    val doc: LayoutDoc,
    val keys: List<MeasuredKey>,
    val viewWidth: Int,
    val viewHeight: Int,
    val layer: LayoutLayer,
    val regions: List<MeasuredRegion> = emptyList()
)

data class MeasuredRegion(
    val region: Region,
    val rect: RectF,
    val keys: List<MeasuredKey>
)
