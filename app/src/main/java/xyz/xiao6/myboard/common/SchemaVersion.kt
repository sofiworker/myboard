package xyz.xiao6.myboard.common

/**
 * 语义化版本: "a.b.c"，主版本号定义兼容性。
 *
 * 兼容策略: 主版本号相同且版本号 >= 所需版本。
 * 例如: "1.2.0" 兼容 "1.0.0"，但 "2.0.0" 不兼容 "1.0.0"。
 *
 * LayoutDoc / ThemeDoc 的 schemaVersion 字段为 String 类型（"a.b.c" 格式），
 * 需要比较时通过 [parse] 或 [parseOrNull] 转换为本类实例。
 */
data class SchemaVersion(
    val major: Int,
    val minor: Int,
    val patch: Int
) : Comparable<SchemaVersion> {

    init {
        require(major >= 0) { "Major version must be non-negative" }
        require(minor >= 0) { "Minor version must be non-negative" }
        require(patch >= 0) { "Patch version must be non-negative" }
    }

    /**
     * 检查与所需版本的兼容性。
     * 兼容条件: 主版本号相同 且 this >= required。
     *
     * 示例:
     * - "1.2.0".isCompatibleWith("1.0.0") == true
     * - "1.0.0".isCompatibleWith("1.2.0") == false
     * - "2.0.0".isCompatibleWith("1.0.0") == false
     */
    fun isCompatibleWith(required: SchemaVersion): Boolean {
        return major == required.major && this >= required
    }

    override fun compareTo(other: SchemaVersion): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** 当前 schema 版本 */
        val CURRENT = SchemaVersion(1, 0, 0)

        /**
         * 当前 schema 版本字符串。
         * LayoutDoc / ThemeDoc 的 schemaVersion 字段应使用此值。
         */
        const val CURRENT_STR = "1.0.0"

        /**
         * 解析 "a.b.c" 格式的版本字符串。
         * @throws IllegalArgumentException 格式无效
         */
        fun parse(version: String): SchemaVersion {
            val parts = version.split(".")
            require(parts.size == 3) {
                "Invalid semantic version format '$version', expected 'a.b.c'"
            }
            val nums = parts.map { part ->
                require(part.toIntOrNull() != null) {
                    "Invalid version component '$part' in '$version'"
                }
                part.toInt()
            }
            return SchemaVersion(nums[0], nums[1], nums[2])
        }

        /**
         * 安全解析版本字符串，解析失败时返回 null。
         * 支持语义化版本 "a.b.c" 和遗留整数格式（如 "2"）。
         */
        fun parseOrNull(version: String): SchemaVersion? {
            return try {
                if (version.toIntOrNull() != null) {
                    SchemaVersion(version.toInt(), 0, 0)
                } else {
                    parse(version)
                }
            } catch (_: Exception) {
                null
            }
        }

        /**
         * 检查版本字符串是否与当前版本兼容。
         * @return true 如果兼容（主版本号相同且版本 >= CURRENT）
         */
        fun isCompatible(versionStr: String): Boolean {
            val parsed = parseOrNull(versionStr) ?: return false
            return parsed.isCompatibleWith(CURRENT)
        }
    }
}
