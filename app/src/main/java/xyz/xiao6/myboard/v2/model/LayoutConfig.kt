package xyz.xiao6.myboard.v2.model

/**
 * 整个布局配置的顶层对象。
 *
 * 一个完整的 json 文件通常对应一个 LayoutConfig。
 */
data class LayoutConfig(
    /** 固定类型，建议始终为 "layout" */
    val type: String = "layout",

    /** 布局唯一 ID，例如 qwerty_default / t9_default */
    val id: String,

    /** 配置版本号，后续 schema 升级可用 */
    val version: Int,

    /** 元信息，可选 */
    val meta: LayoutMeta? = null,

    /** 环境信息，可选，例如横竖屏、主题、密度 */
    val env: LayoutEnv? = null,

    /** 核心布局内容 */
    val layout: LayoutBody,

    /** 样式表，可通过 styleRef 在 region/node 上引用 */
    val styles: Map<String, StyleConfig>? = null,

    /** 布局状态定义，例如支持的 layer / mode */
    val state: LayoutState? = null
)

/**
 * 布局的元信息。
 */
data class LayoutMeta(
    /** 展示名 */
    val name: String? = null,

    /** 描述信息 */
    val description: String? = null,

    /** 作者 */
    val author: String? = null,

    /** 标签 */
    val tags: List<String>? = null
)

/**
 * 布局生效环境。
 */
data class LayoutEnv(
    /** 适用方向：横屏、竖屏、自动 */
    val orientation: EnvOrientation? = null,

    /** 主题：亮色、暗色、自动 */
    val theme: EnvTheme? = null,

    /** 密度档位 */
    val density: Density? = null
)

/**
 * 布局的状态定义。
 */
data class LayoutState(
    /** 默认 layer，例如 default */
    val defaultLayer: String? = null,

    /** 支持的所有 layer，例如 default / shift / number / symbol */
    val supportedLayers: List<String>? = null,

    /** 支持的模式，例如 pinyin / english / shuangpin / t9 */
    val supportedModes: List<String>? = null
)

/**
 * layout 节点主体。
 */
data class LayoutBody(
    /** 整个页面 region 的主排列方向 */
    val direction: Orientation,

    /** 页面中的多个区域 */
    val regions: List<RegionConfig>
)