package xyz.xiao6.myboard.dictionary.format

/**
 * Semantic version in `a.b.c` form.
 *
 * Stored in MYBDF header as three u16 fields (major/minor/patch), so each part is limited to [0, 65535].
 */
data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
) {
    init {
        require(major in 0..65535) { "major out of range: $major" }
        require(minor in 0..65535) { "minor out of range: $minor" }
        require(patch in 0..65535) { "patch out of range: $patch" }
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Parses `a.b.c`.
         */
        fun parse(text: String): SemVer {
            val parts = text.trim().split(".")
            require(parts.size == 3) { "SemVer must be 'a.b.c': $text" }
            val major = parts[0].toIntOrNull() ?: error("Invalid major: ${parts[0]}")
            val minor = parts[1].toIntOrNull() ?: error("Invalid minor: ${parts[1]}")
            val patch = parts[2].toIntOrNull() ?: error("Invalid patch: ${parts[2]}")
            return SemVer(major, minor, patch)
        }
    }
}

