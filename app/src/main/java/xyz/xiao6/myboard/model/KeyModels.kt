package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * 特殊按键的固定 keyId 常量（用于 JSON 约束与运行时识别）。
 * Fixed keyId constants for special keys (used for JSON validation and runtime identification).
 */
object KeyIds {
    /**
     * 返回到上一次布局（或回到主布局）的“返回键”。
     * Back key that navigates to previous layout (or primary layout).
     */
    const val BACK_LAYOUT = "key_back_layout"

    /**
     * 删除/退格键（Backspace）。
     * Delete/backspace key.
     */
    const val BACKSPACE = "key_backspace"

    /**
     * 空格键（Space）。
     * Space key.
     */
    const val SPACE = "key_space"
}

@Serializable
/**
 * 布局内“特殊功能键”枚举：用于把某些 key 做统一的特殊处理（不依赖 label / behaviors）。
 * Special function keys (layout-level): handled specially regardless of label/behaviors.
 */
enum class SpecialKey {
    /** 回车/换行；Enter. */
    ENTER,
    /** 中英文（locale）切换；Toggle locale. */
    TOGGLE_LOCALE,
    /** 分词/断开当前 composing（提交当前拼音字母串为原文）；Commit composing as raw text. */
    SEGMENT,
}

@Serializable
/**
 * 按键模型：逻辑输出 + 行为映射 + 主题引用 + 网格几何。
 * Key model: logical output + behavior mapping + theme reference + grid geometry.
 */
data class Key(
    /**
     * 按键唯一标识（例如 "key_q", "key_shift"）。
     * Unique key id (e.g. "key_q", "key_shift").
     */
    val keyId: String,
    /**
     * 可选：特殊功能键类型（用于统一的特殊处理）。
     * Optional special function key type.
     */
    val specialKey: SpecialKey? = null,
    /**
     * 默认显示的标签文本（例如 "q"）。
     * Default label shown on the key (e.g. "q").
     */
    val label: String,
    /**
     * 默认点击输出码值（通常是 Unicode code point；也可用负数表示自定义功能码）。
     * Primary output code (usually Unicode code point; negative values can represent custom function codes).
     */
    val primaryCode: Int,
    /**
     * 手势触发器 -> 动作 的映射表（可为空）。
     * Mapping from gesture trigger -> action (can be empty).
     */
    val behaviors: Map<KeyTrigger, KeyAction> = emptyMap(),
    /**
     * 主题样式 ID（从 ThemeManager/Theme JSON 的 styles 中引用）。
     * Theme style id (references Theme JSON `styles`).
     */
    val styleId: String,
    /**
     * 可选：图标资源 ID（0 表示无图标/未使用）。
     * Optional icon resource id (0 means none).
     */
    val iconResId: Int = 0,
    /**
     * 提示文本：以 9 宫格位置为 key 的小提示字符串（例如右上角的 "1"）。
     * Hints: small hint strings keyed by 3x3 positions (e.g. "1" at top-right).
     */
    val hints: Map<HintPosition, String> = emptyMap(),
    /**
     * 网格定位（起始行列 + 跨列/跨行）。
     * Grid position (start row/col + column/row span).
     */
    val gridPosition: KeyPosition,
    /**
     * 弹性宽度权重（用于同一行内按剩余空间分配）。
     * Flexible width weight (used to distribute remaining space within a row).
     */
    val widthWeight: Float = 1.0f,
    /**
     * 固定宽度（dp）；当不为 null 时优先于 [widthWeight]。
     * Fixed width in dp; when non-null, it overrides [widthWeight].
     */
    val widthDp: Float? = null,
)

@Serializable
/**
 * 提示文本的位置（将按键矩形划分为 3x3 九宫格）。
 * Hint position in a 3x3 grid within the key bounds.
 */
enum class HintPosition {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT,
}

@Serializable
/**
 * 按键在网格中的位置与跨度（支持跨行/跨列）。
 * Key position and span in the grid (supports row/column spanning).
 */
data class KeyPosition(
    /**
     * 起始列（从 0 开始）。
     * Start column (0-based).
     */
    val startCol: Int,
    /**
     * 起始行（从 0 开始）。
     * Start row (0-based).
     */
    val startRow: Int,
    /**
     * 横向跨列数（>= 1）。
     * Column span (>= 1).
     */
    val spanCols: Int = 1,
    /**
     * 纵向跨行数（>= 1）。
     * Row span (>= 1).
     */
    val spanRows: Int = 1,
)

@Serializable
/**
 * 触发器：由触摸/手势识别层产出。
 * Trigger: produced by touch/gesture recognition.
 */
enum class KeyTrigger {
    /** 点击；Tap. */
    TAP,
    /** 长按；Long press. */
    LONG_PRESS,
    /** 上滑；Swipe up. */
    SWIPE_UP,
    /** 下滑；Swipe down. */
    SWIPE_DOWN,
}

@Serializable
/**
 * 动作：由 Controller 执行或分发给上层输入逻辑。
 * Action: executed by controller or dispatched to input logic.
 */
data class KeyAction(
    /**
     * 动作类型。
     * Action type.
     */
    val actionType: ActionType,
    /**
     * 单值参数（例如 COMMIT 的字符、SWITCH_LAYOUT 的布局 ID）。
     * Single value parameter (e.g. COMMIT text, SWITCH_LAYOUT target layout id).
     */
    val value: String? = null,
    /**
     * 多值参数（例如 SHOW_POPUP 的候选列表）。
     * Multiple values parameter (e.g. SHOW_POPUP candidates list).
     */
    val values: List<String>? = null,
)

@Serializable
/**
 * 动作类型枚举。
 * Action type enum.
 */
enum class ActionType {
    /** 提交文本；Commit text. */
    COMMIT,
    /** 切换 Shift 状态；Toggle shift state. */
    TOGGLE_SHIFT,
    /** 同一布局内切换页/层；Switch in-layout layer/page. */
    SET_LAYER,
    /** 切换布局；Switch layout. */
    SWITCH_LAYOUT,
    /** 切换语言环境（localeTag）；Switch locale (localeTag). */
    SWITCH_LOCALE,
    /** 返回上一次布局；Back to previous layout. */
    BACK_LAYOUT,
    /** 删除/退格；Backspace. */
    BACKSPACE,
    /** 空格；Space. */
    SPACE,
    /** 回车/换行；Enter/newline. */
    ENTER,
    /** 切换语言环境（在可用 locales 中循环或中英互切）；Toggle locale. */
    TOGGLE_LOCALE,
    /** 提交当前 composing 为原文并清空 composing；Commit composing as raw text. */
    COMMIT_COMPOSING,
    /** 热词高亮切换；Toggle hotword highlight. */
    TOGGLE_HOTWORD_HIGHLIGHT,
    /** 显示弹窗/候选；Show popup/candidates. */
    SHOW_POPUP,
    /** 显示符号面板（整块覆盖 toolbar+keyboard）；Show symbols panel overlay. */
    SHOW_SYMBOLS,
}
