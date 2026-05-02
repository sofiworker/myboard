package xyz.xiao6.myboard.v2.model


/**
 * Key 节点的业务数据。
 */
data class KeyData(
    /** 传统 keyCode，可选 */
    val keyCode: Int? = null,

    /** 点击后主输出 token，例如 "q" / "a" / "2" */
    val token: String? = null,

    /** 显示文本，可与 content.label 区分 */
    val displayText: String? = null,

    /** 长按或特殊行为对应的 popup ID */
    val popupId: String? = null,

    /** 是否支持按住连发，例如退格键 */
    val repeatable: Boolean? = null,

    /** 功能修饰键类型，例如 SHIFT / ALT */
    val modifier: KeyModifier? = null,

    /** 对应 IME action */
    val imeAction: ImeActionType? = null,

    /** 双拼映射，例如 "iu" / "ong" */
    val spellMap: String? = null,

    /** T9 场景中的字母集合，例如 "abc" */
    val letters: String? = null,

    /** 扩展字段，尽量只放轻量字符串类数据 */
    val extra: Map<String, String>? = null
)

/**
 * Candidate 节点的业务数据。
 */
data class CandidateData(
    /** 候选下标，通常用于提交指定候选 */
    val index: Int? = null,

    /** 绑定候选文字的表达式，例如 ${candidate0.text} */
    val textBinding: String? = null,

    /** 绑定候选注释的表达式，例如 ${candidate0.comment} */
    val commentBinding: String? = null,

    /** 固定候选值，可选 */
    val value: String? = null,

    /** 固定候选注释，可选 */
    val comment: String? = null,

    /** 是否允许删除该候选，例如用户词条 */
    val removable: Boolean? = null
)

/**
 * Button 节点的业务数据。
 */
data class ButtonData(
    /** 按钮语义角色 */
    val buttonRole: ButtonRole? = null,

    /** 目标面板名，例如 emoji / clipboard / settings */
    val targetPanel: String? = null,

    /** 目标 layer，例如 number / symbol */
    val targetLayer: String? = null,

    /** 目标 mode，例如 english / pinyin */
    val targetMode: String? = null,

    /** 是否可选中/切换态 */
    val checkable: Boolean? = null,

    /** 当前是否选中 */
    val checked: Boolean? = null
)