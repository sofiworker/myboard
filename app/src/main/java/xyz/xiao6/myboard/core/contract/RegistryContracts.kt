package xyz.xiao6.myboard.core.contract

/**
 * 注册结果。
 */
sealed interface RegisterResult {
    data class Success(val id: String) : RegisterResult
    data class Failed(val errors: List<String>) : RegisterResult
}

/**
 * 布局校验问题。
 */
data class LayoutIssue(
    val severity: IssueSeverity,
    val message: String,
    val path: String? = null
)

enum class IssueSeverity { ERROR, WARNING }

/**
 * 布局来源。
 */
enum class LayoutSource { BUILT_IN, LANGUAGE_PACK, USER }

/**
 * 字典加载键。
 */
data class DictionaryKey(
    val packageId: String,
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema,
    val path: String
)

/**
 * 导入结果。
 */
sealed interface ImportResult {
    data class Success(val packageId: String) : ImportResult
    data class PartialSuccess(val packageId: String, val warnings: List<String>) : ImportResult
    data class Failed(val errors: List<String>) : ImportResult
}