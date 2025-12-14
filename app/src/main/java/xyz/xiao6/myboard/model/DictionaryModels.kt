package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * 字典元数据：用于选择/匹配/加载字典，不包含具体词典内容本身。
 * Dictionary metadata: used for selection/matching/loading, not the dictionary content itself.
 */
@Serializable
data class DictionarySpec(
    /**
     * 字典唯一 ID（运行时由输入模式/用户选择决定）。
     * Unique dictionary id (chosen at runtime by input mode/user settings).
     */
    val dictionaryId: String,
    /**
     * 展示名称（可选）。
     * Display name (optional).
     */
    val name: String? = null,
    /**
     * 支持的语言标签（BCP-47 或下划线形式均可）。
     * Supported locale tags (BCP-47 or underscore style).
     *
     * 为空表示语言无关（例如 dict_none）。
     * Empty means language-agnostic (e.g. dict_none).
     */
    val localeTags: List<String> = emptyList(),
    /**
     * 可搭配的布局列表（为空表示不限制）。
     * Allowed layout ids (empty means no restriction).
     */
    val layoutIds: List<String> = emptyList(),
    /**
     * 词典数据资源路径（可选，指向 assets 内部路径；例如 "dictionary/base.mybdict"）。
     * Dictionary data asset path (optional; points to an assets file).
     */
    val assetPath: String? = null,
    /**
     * 用户字典文件路径（可选）。当 [assetPath] 为空时，可用它从 filesDir 加载。
     * User dictionary file path (optional). If [assetPath] is null, it can be loaded from app storage.
     */
    val filePath: String? = null,
    /**
     * 字典版本（a.b.c），用于更新/兼容性判断（可选）。
     * Dictionary version (a.b.c), optional.
     */
    val dictionaryVersion: String? = null,
    /**
     * MyBoard 规范化后的 code scheme（由 convert 层写入；运行时 decoder 按此生成 search key）。
     * Canonical code scheme (written by convert layer; decoder generates search keys accordingly).
     *
     * 示例：PINYIN_FULL
     */
    val codeScheme: String? = null,
    /**
     * 输入方案分类（可选）：PINYIN / HANDWRITING / STROKE / ...
     * Input kind (optional).
     */
    val kind: String? = null,
    /**
     * 引擎分类（可选）：PINYIN_CORE / HANDWRITING_CORE / ...
     * Engine core (optional).
     */
    val core: String? = null,
    /**
     * 同一 kind 下的变体（可选）：quanpin / shuangpin / ...
     * Variant (optional).
     */
    val variant: String? = null,
    /**
     * 是否为默认候选（可选）：用于同 kind 多字典时的默认选择。
     * Default flag (optional).
     */
    val isDefault: Boolean = false,
    /**
     * 启用开关。
     * Enabled flag.
     */
    val enabled: Boolean = true,
    /**
     * 优先级（用于匹配同 locale 时排序）。
     * Priority for tie-breaking when matching a locale.
     */
    val priority: Int = 0,
)

@Serializable
data class DictionaryPack(
    val version: Int = 1,
    val dictionaries: List<DictionarySpec> = emptyList(),
)
