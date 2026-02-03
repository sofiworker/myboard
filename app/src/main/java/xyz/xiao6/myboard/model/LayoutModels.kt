package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * 布局功能开关配置，控制整个布局的显示和行为特性。
 */
@Serializable
data class LayoutFeatures(
    /** 是否显示 hints（所有键的提示文字） */
    val showHints: Boolean = true,
    /** 是否显示 labels（键的主标签） */
    val showLabels: Boolean = true,
    /** 是否显示图标 */
    val showIcons: Boolean = true,
    /** 是否启用长按功能 */
    val enableLongPress: Boolean = true,
    /** 是否启用手势功能 */
    val enableGestures: Boolean = true,
    /** 是否启用预览气泡 */
    val enableKeyPreview: Boolean = true,
    /** 是否启用震动反馈 */
    val enableHapticFeedback: Boolean = true,
    /** 是否启用声音反馈 */
    val enableSoundFeedback: Boolean = false,
    /** 自定义功能开关 */
    val customFlags: Map<String, Boolean> = emptyMap(),
)

@Serializable
/**
 * 布局元数据，描述布局的特性和能力。
 */
data class LayoutMetadata(
    /** 布局类型（如 "qwerty", "t9", "numeric"） */
    val layoutType: String? = null,
    /** 默认输入引擎 */
    val defaultEngine: String? = null,
    /** 布局描述 */
    val description: String? = null,
    /** 作者 */
    val author: String? = null,
    /** 版本 */
    val version: String? = null,
    /** 自定义属性 */
    val properties: Map<String, String> = emptyMap(),
)

@Serializable
/**
 * 键盘布局（结构 + 几何参数），用于从 JSON 驱动渲染与交互。
 */
data class KeyboardLayout(
    /**
     * 布局唯一 ID（例如 "qwerty"）。
     */
    val layoutId: String,
    /**
     * 布局显示名称（可选，用于 UI/调试）。
     */
    val name: String? = null,
    /**
     * 该布局可用于哪些语言/地区（例如 ["en_US","zh_CN"]）。
     */
    val locale: List<String> = emptyList(),
    /**
     * 按屏幕宽度比例定义键盘宽度（例如 1.0 表示占满屏幕），取值范围 (0, 1]。
     */
    val totalWidthRatio: Float = 1.0f,
    /**
     * 当使用 [totalWidthRatio] 时的 dp 微调。
     */
    val totalWidthDpOffset: Float = 0f,
    /**
     * 按屏幕高度比例定义键盘高度（例如 0.2 表示占满屏幕 1/5），取值范围 (0, 1]。
     */
    val totalHeightRatio: Float,
    /**
     * 当使用 [totalHeightRatio] 时的 dp 微调。
     */
    val totalHeightDpOffset: Float = 0f,
    /**
     * 全局默认值（间距、内边距等）。
     */
    val defaults: LayoutDefaults = LayoutDefaults(),
    /**
     * 布局元数据。
     */
    val metadata: LayoutMetadata = LayoutMetadata(),
    /**
     * 布局功能开关。
     */
    val features: LayoutFeatures = LayoutFeatures(),
    /**
     * 行列表（从上到下）。
     */
    val rows: List<KeyboardRow>,
)

@Serializable
/**
 * 全局默认布局参数。
 */
data class LayoutDefaults(
    /**
     * 默认按键水平间距（dp）。
     */
    val horizontalGapDp: Float = 0f,
    /**
     * 默认行间距（dp）。
     */
    val verticalGapDp: Float = 0f,
    /**
     * 默认键盘内边距（dp）。
     */
    val padding: LayoutPadding = LayoutPadding(),
)

@Serializable
/**
 * 内边距（dp）。
 */
data class LayoutPadding(
    /**
     * 顶部内边距（dp）。
     */
    val topDp: Float = 0f,
    /**
     * 底部内边距（dp）。
     */
    val bottomDp: Float = 0f,
    /**
     * 左侧内边距（dp）。
     */
    val leftDp: Float = 0f,
    /**
     * 右侧内边距（dp）。
     */
    val rightDp: Float = 0f,
)

@Serializable
/**
 * 行类型，用于区分不同类型的行。
 */
enum class RowType {
    /** 字母行（默认） */
    ALPHA,
    /** 独立数字行（如 PC 键盘顶部 0-9） */
    NUMBER,
    /** 符号行 */
    SYMBOL,
    /** Emoji 行 */
    EMOJI,
    /** 功能键行（空格、回车等） */
    FUNCTION,
    /** 导航键行（方向键等） */
    NAVIGATION,
    /** 编辑键行（复制、粘贴等） */
    EDITING,
    /** 自定义类型 */
    CUSTOM,
}

@Serializable
/**
 * 单行定义。
 */
data class KeyboardRow(
    /**
     * 行唯一 ID。
     */
    val rowId: String,
    /**
     * 按键盘宽度比例定义本行宽度。
     */
    val widthRatio: Float = 1.0f,
    /**
     * dp 微调。
     */
    val widthDpOffset: Float = 0f,
    /**
     * 按键盘高度比例定义行高。
     */
    val heightRatio: Float,
    /**
     * dp 微调。
     */
    val heightDpOffset: Float = 0f,
    /**
     * 行内按键水平间距（dp）。
     */
    val horizontalGapDp: Float? = null,
    /**
     * 行内对齐方式。
     */
    val alignment: RowAlignment = RowAlignment.JUSTIFY,
    /**
     * 行起始内边距（dp）。
     */
    val startPaddingDp: Float? = null,
    /**
     * 行结束内边距（dp）。
     */
    val endPaddingDp: Float? = null,
    /**
     * 行类型。
     */
    val rowType: RowType = RowType.ALPHA,
    /**
     * 针对此行的默认样式 ID。
     */
    val rowStyleId: String? = null,
    /**
     * 本行包含的按键列表。
     */
    val keys: List<Key>,
)

@Serializable
enum class RowAlignment {
    CENTER,
    JUSTIFY,
    LEFT,
}
