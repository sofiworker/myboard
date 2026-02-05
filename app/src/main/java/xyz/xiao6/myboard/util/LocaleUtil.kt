package xyz.xiao6.myboard.util

import java.util.Locale

/**
 * Utility functions for locale handling.
 */
object LocaleUtil {
    /**
     * Normalize a locale tag to BCP-47 format.
     *
     * Examples:
     * - "zh_CN" → "zh-CN"
     * - "en_us" → "en-US"
     * - "ZH-cn" → "zh-CN"
     *
     * @param tag Locale tag to normalize
     * @return Normalized locale tag in BCP-47 format (language-region with hyphens)
     */
    fun normalizeLocaleTag(tag: String): String {
        val t = tag.trim().replace('_', '-')
        val parts = t.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val language = parts[0].lowercase(Locale.ROOT)
        val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
        return if (region.isNullOrBlank()) language else "$language-$region"
    }
}
