package xyz.xiao6.myboard.contract.language

import xyz.xiao6.myboard.contract.state.LocaleTag
import xyz.xiao6.myboard.contract.state.Script

typealias LocalizedText = Map<String, String>

data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SemVer> {
    init {
        require(major >= 0 && minor >= 0 && patch >= 0) { "Semantic version components must be non-negative" }
    }

    override fun compareTo(other: SemVer): Int = compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString(): String = "$major.$minor.$patch"
}

data class VersionRange(
    val minimum: SemVer? = null,
    val maximumExclusive: SemVer? = null
) {
    init {
        require(minimum == null || maximumExclusive == null || minimum < maximumExclusive) {
            "Version range minimum must be lower than its exclusive maximum"
        }
    }

    operator fun contains(version: SemVer): Boolean =
        (minimum == null || version >= minimum) && (maximumExclusive == null || version < maximumExclusive)
}

data class PackageIdentity(
    val packageId: String,
    val version: SemVer
) {
    init {
        require(packageId.isNotBlank()) { "Package id must not be blank" }
    }
}

data class PackageDependency(
    val packageId: String,
    val versionRange: VersionRange,
    val optional: Boolean = false
) {
    init {
        require(packageId.isNotBlank()) { "Dependency package id must not be blank" }
    }
}

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
