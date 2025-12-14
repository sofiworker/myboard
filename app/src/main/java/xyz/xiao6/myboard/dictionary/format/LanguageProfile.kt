package xyz.xiao6.myboard.dictionary.format

/**
 * Language/script/feature definitions inspired by `dictionary/a.cc`.
 *
 * MYBDF v1 stores a *primary* (language, region, script, feature_flags) in the fixed header
 * for fast filtering/matching. The full list of applicable languages remains in metadata JSON.
 */
object LanguageProfile {
    /**
     * Script type (writing system).
     *
     * Keep numeric values stable once shipped; they are persisted in `.mybdict` headers.
     */
    enum class ScriptType(val id: Int) {
        UNKNOWN(0),
        LATIN(1),
        HAN(2),
        HIRAGANA(3),
        KATAKANA(4),
        HANGUL(5),
        TIBETAN(6),
        ARABIC(7),
        THAI(8),
        DEVANAGARI(9),
    }

    /**
     * Language / IME feature flags bitset (u32), conceptually aligned with `FeatureFlags` in `a.cc`.
     */
    object FeatureFlags {
        const val HAS_TONES: Int = 1 shl 0
        const val HAS_PITCH: Int = 1 shl 1
        const val HAS_DIACRITICS: Int = 1 shl 2
        const val REQUIRES_COMPOSE: Int = 1 shl 3
        const val IS_LOGGRAPHIC: Int = 1 shl 4
        const val IS_SYLLABIC: Int = 1 shl 5
        const val IS_ABJAD: Int = 1 shl 6
        const val HAS_CASE: Int = 1 shl 7
        const val RTL_WRITING: Int = 1 shl 8
    }

    /**
     * Region codes in MYBDF header (u8).
     *
     * This is a small internal enum (not ISO numeric region) for fast matching/filtering.
     */
    enum class RegionCode(val id: Int) {
        UNKNOWN(0),
        CN(1),
        TW(2),
        HK(3),
        MO(4),
        US(10),
    }

    /**
     * Derive a default primary header profile from a BCP-47 tag (e.g. "zh-CN").
     *
     * The output is used to populate MYBDF header fields.
     */
    fun fromPrimaryLanguageTag(languageTag: String?): HeaderProfile {
        val tag = (languageTag ?: "").trim().replace('_', '-')
        val parts = tag.split('-').filter { it.isNotBlank() }
        val language = parts.getOrNull(0)?.lowercase().orEmpty()
        val region = parts.getOrNull(1)?.uppercase().orEmpty()

        val regionCode = when (region) {
            "CN" -> RegionCode.CN
            "TW" -> RegionCode.TW
            "HK" -> RegionCode.HK
            "MO" -> RegionCode.MO
            "US" -> RegionCode.US
            else -> RegionCode.UNKNOWN
        }

        return when (language) {
            "zh" -> HeaderProfile(
                languageCode = packLanguage2("zh"),
                regionCode = regionCode,
                scriptType = ScriptType.HAN,
                featureFlags = FeatureFlags.HAS_TONES or FeatureFlags.IS_LOGGRAPHIC,
            )
            "ja" -> HeaderProfile(
                languageCode = packLanguage2("ja"),
                regionCode = regionCode,
                scriptType = ScriptType.HIRAGANA,
                featureFlags = FeatureFlags.HAS_PITCH or FeatureFlags.IS_SYLLABIC,
            )
            "ko" -> HeaderProfile(
                languageCode = packLanguage2("ko"),
                regionCode = regionCode,
                scriptType = ScriptType.HANGUL,
                featureFlags = FeatureFlags.REQUIRES_COMPOSE or FeatureFlags.IS_SYLLABIC,
            )
            "ar" -> HeaderProfile(
                languageCode = packLanguage2("ar"),
                regionCode = regionCode,
                scriptType = ScriptType.ARABIC,
                featureFlags = FeatureFlags.RTL_WRITING or FeatureFlags.IS_ABJAD,
            )
            else -> HeaderProfile(
                languageCode = packLanguage2(language.ifBlank { "un" }),
                regionCode = regionCode,
                scriptType = ScriptType.LATIN,
                featureFlags = FeatureFlags.HAS_CASE,
            )
        }
    }

    data class HeaderProfile(
        val languageCode: Int,
        val regionCode: RegionCode,
        val scriptType: ScriptType,
        val featureFlags: Int,
    )

    /**
     * Packs a 2-letter lower-case ASCII language code into u16.
     *
     * Example:
     * - "zh" -> 0x687A (bytes 'z','h')
     */
    fun packLanguage2(twoLetters: String): Int {
        val s = twoLetters.trim().lowercase()
        require(s.length >= 2) { "language must have at least 2 letters: $twoLetters" }
        val b0 = s[0].code and 0xFF
        val b1 = s[1].code and 0xFF
        return b0 or (b1 shl 8)
    }
}

