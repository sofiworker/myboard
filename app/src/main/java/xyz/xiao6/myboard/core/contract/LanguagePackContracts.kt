package xyz.xiao6.myboard.core.contract

/**
 * 语言包管理器接口。
 */
interface LanguagePackManager {
    fun listInstalled(): List<LanguagePackInfo>
    fun install(pack: LanguagePackInfo): Boolean
    fun uninstall(packageId: String): Boolean
    fun get(packageId: String): LanguagePackInfo?
}

/**
 * 语言包信息。
 */
data class LanguagePackInfo(
    val packageId: String,
    val displayName: String,
    val version: Int,
    val locale: LocaleTag,
    val scripts: List<Script>
)
