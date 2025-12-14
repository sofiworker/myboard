package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

@Serializable
/**
 * 键盘布局（结构 + 几何参数），用于从 JSON 驱动渲染与交互。
 * Keyboard layout (structure + geometry), drives rendering/interaction from JSON.
 */
data class KeyboardLayout(
    /**
     * 布局唯一 ID（例如 "qwerty"）。
     * Unique layout id (e.g. "qwerty").
     */
    val layoutId: String,
    /**
     * 布局显示名称（可选，用于 UI/调试）。
     * Human-readable name (optional, for UI/debug).
     */
    val name: String? = null,
    /**
     * 该布局可用于哪些语言/地区（例如 ["en_US","zh_CN"]），不允许重复项。
     * Locales that this layout supports (e.g. ["en_US","zh_CN"]), duplicates are not allowed.
     */
    val locale: List<String> = emptyList(),
    /**
     * 按屏幕宽度比例定义键盘宽度（例如 1.0 表示占满屏幕），取值范围 (0, 1]。
     * Keyboard width ratio relative to the screen, in (0, 1].
     */
    val totalWidthRatio: Float = 1.0f,
    /**
     * 当使用 [totalWidthRatio] 时的 dp 微调（可正可负）。
     * DP fine-tune for [totalWidthRatio] (can be +/-).
     */
    val totalWidthDpOffset: Float = 0f,
    /**
     * 主模式：按屏幕高度比例定义键盘高度（例如 0.2 表示占满屏幕 1/5），取值范围 (0, 1]。
     * Main mode: keyboard height ratio relative to the screen, in (0, 1].
     */
    val totalHeightRatio: Float,
    /**
     * 当使用 [totalHeightRatio] 时的 dp 微调（可正可负）。
     * DP fine-tune for [totalHeightRatio] (can be +/-).
     */
    val totalHeightDpOffset: Float = 0f,
    /**
     * 全局默认值（间距、内边距等），可被 row 覆盖。
     * Global defaults (gaps/padding), can be overridden by rows.
     */
    val defaults: LayoutDefaults = LayoutDefaults(),
    /**
     * 行列表（从上到下）。
     * Rows from top to bottom.
     */
    val rows: List<KeyboardRow>,
)

@Serializable
/**
 * 全局默认布局参数。
 * Global default layout parameters.
 */
data class LayoutDefaults(
    /**
     * 默认按键水平间距（dp）。
     * Default horizontal gap between keys (dp).
     */
    val horizontalGapDp: Float = 0f,
    /**
     * 默认行间距（dp）。
     * Default vertical gap between rows (dp).
     */
    val verticalGapDp: Float = 0f,
    /**
     * 默认键盘内边距（dp）。
     * Default keyboard padding (dp).
     */
    val padding: LayoutPadding = LayoutPadding(),
)

@Serializable
/**
 * 内边距（dp）。
 * Padding values (dp).
 */
data class LayoutPadding(
    /**
     * 顶部内边距（dp）。
     * Top padding (dp).
     */
    val topDp: Float = 0f,
    /**
     * 底部内边距（dp）。
     * Bottom padding (dp).
     */
    val bottomDp: Float = 0f,
    /**
     * 左侧内边距（dp）。
     * Left padding (dp).
     */
    val leftDp: Float = 0f,
    /**
     * 右侧内边距（dp）。
     * Right padding (dp).
     */
    val rightDp: Float = 0f,
)

@Serializable
/**
 * 单行定义（行内对齐、间距、以及包含的 keys）。
 * A single row definition (alignment/gaps and its keys).
 */
data class KeyboardRow(
    /**
     * 行唯一 ID（例如 "row_1_alpha"）。
     * Unique row id (e.g. "row_1_alpha").
     */
    val rowId: String,
    /**
     * 按键盘宽度比例定义本行宽度（例如 1.0 表示占满键盘宽度），取值范围 (0, 1]。
     * Row width ratio relative to the keyboard, in (0, 1].
     */
    val widthRatio: Float = 1.0f,
    /**
     * 当使用 [widthRatio] 时的 dp 微调（可正可负）。
     * DP fine-tune for [widthRatio] (can be +/-).
     */
    val widthDpOffset: Float = 0f,
    /**
     * 主模式：按键盘高度比例定义行高（例如 0.25 表示占键盘 1/4），取值范围 (0, 1]。
     * Main mode: row height ratio relative to the keyboard, in (0, 1].
     */
    val heightRatio: Float,
    /**
     * 当使用 [heightRatio] 时的 dp 微调（可正可负）。
     * DP fine-tune for [heightRatio] (can be +/-).
     */
    val heightDpOffset: Float = 0f,
    /**
     * 行内按键水平间距（dp），为 null 时使用 defaults.horizontalGapDp。
     * Horizontal gap between keys in this row (dp); null falls back to defaults.horizontalGapDp.
     */
    val horizontalGapDp: Float? = null,
    /**
     * 行内对齐方式（CENTER / LEFT / JUSTIFY）。
     * Alignment within the row (CENTER / LEFT / JUSTIFY).
     */
    val alignment: RowAlignment = RowAlignment.JUSTIFY,
    /**
     * 行起始内边距（dp），为 null 时使用 defaults.padding.leftDp。
     * Row start padding (dp); null falls back to defaults.padding.leftDp.
     */
    val startPaddingDp: Float? = null,
    /**
     * 行结束内边距（dp），为 null 时使用 defaults.padding.rightDp。
     * Row end padding (dp); null falls back to defaults.padding.rightDp.
     */
    val endPaddingDp: Float? = null,
    /**
     * 本行包含的按键列表。
     * Keys contained in this row.
     */
    val keys: List<Key>,
)

@Serializable
/**
 * 行内对齐方式。
 * Row alignment mode.
 */
enum class RowAlignment {
    /** 居中对齐；Center within available width. */
    CENTER,
    /** 两端对齐；Justify to fill available width. */
    JUSTIFY,
    /** 左对齐；Left aligned. */
    LEFT,
}
