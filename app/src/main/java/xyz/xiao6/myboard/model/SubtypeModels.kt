package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * Subtype 资源（方案2，最终定义）：
 *
 * 构建期生成一个 “locale -> layoutIds[]” 的映射表，用于运行时：
 * - 先根据当前 locale 拿到可用布局列表（qwerty/t9/笔画/手写...）
 * - 再由用户或 layout 内按钮/toolbar 选择具体 layout
 * - 输入时 mode（拼音/手写/笔画...）动态切换并据此加载字典/识别器（字典不绑定在 subtype 里）
 */
@Serializable
data class LocaleLayoutProfile(
    /** Locale tag (BCP-47 or underscore style; e.g. "zh-CN", "en_US"). */
    val localeTag: String,
    /** Layout ids available for this locale. */
    val layoutIds: List<String> = emptyList(),
    /** Optional default layout id for this locale (if omitted, runtime picks the first). */
    val defaultLayoutId: String? = null,
    val enabled: Boolean = true,
    val priority: Int = 0,
)

@Serializable
data class SubtypePack(
    val version: Int = 3,
    val locales: List<LocaleLayoutProfile> = emptyList(),
)
