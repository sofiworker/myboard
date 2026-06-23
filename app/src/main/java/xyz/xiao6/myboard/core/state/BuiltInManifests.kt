package xyz.xiao6.myboard.core.state

import xyz.xiao6.myboard.core.contract.*

/**
 * 内置语言包 Manifest 常量。
 * 定义在 Kotlin 中用于初始化，JSONC 格式 Manifest 文件在 assets/languages/ 下。
 */
object BuiltInManifests {
    
    val zhCN = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.zh-CN",
        packageVersion = "1.0.0",
        locale = LocaleTag("zh-CN"),
        displayName = mapOf(
            "zh-CN" to "中文",
            "en-US" to "Chinese"
        ),
        defaults = LocaleDefaults(
            script = Script.HANI,
            schema = BuiltInSchemas.PINYIN,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.HANI to ScriptManifest(
                defaultSchema = BuiltInSchemas.PINYIN,
                schemas = mapOf(
                    BuiltInSchemas.PINYIN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "qwerty",
                        supportsShift = false,
                        encoderId = "identity",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_query",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_pinyin")
                    ),
                    BuiltInSchemas.DOUBLE_PINYIN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "shuangpin",
                        supportsShift = false,
                        encoderId = "double_pinyin",
                        encoderConfig = "encoders/double_pinyin_xiaohe.json",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_raw",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_double_pinyin")
                    )
                )
            ),
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_latin")
                    )
                )
            )
        )
    )
    
    val enUS = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.en-US",
        packageVersion = "1.0.0",
        locale = LocaleTag("en-US"),
        displayName = mapOf(
            "zh-CN" to "英语",
            "en-US" to "English"
        ),
        defaults = LocaleDefaults(
            script = Script.LATN,
            schema = BuiltInSchemas.LATIN_DIRECT,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_en_direct")
                    )
                )
            )
        )
    )
    
    val jaJP = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.ja-JP",
        packageVersion = "1.0.0",
        locale = LocaleTag("ja-JP"),
        displayName = mapOf(
            "zh-CN" to "日语",
            "en-US" to "Japanese",
            "ja-JP" to "日本語"
        ),
        defaults = LocaleDefaults(
            script = Script.HIRA,
            schema = BuiltInSchemas.ROMAJI,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.HIRA to ScriptManifest(
                defaultSchema = BuiltInSchemas.ROMAJI,
                schemas = mapOf(
                    BuiltInSchemas.ROMAJI to SchemaCapability(
                        engineId = "transliteration",
                        layoutId = "qwerty",
                        supportsShift = false,
                        fsm = "rules/romaji_hira.fsm.json",
                        candidatePolicy = "japanese_kana_default",
                        displayPolicy = "show_composing",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_romaji_hira")
                    )
                )
            ),
            Script.KATA to ScriptManifest(
                defaultSchema = BuiltInSchemas.ROMAJI,
                schemas = mapOf(
                    BuiltInSchemas.ROMAJI to SchemaCapability(
                        engineId = "transliteration",
                        layoutId = "qwerty",
                        supportsShift = false,
                        fsm = "rules/romaji_kata.fsm.json",
                        candidatePolicy = "japanese_kana_default",
                        displayPolicy = "show_composing",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_romaji_kata")
                    )
                )
            ),
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_latin")
                    )
                )
            )
        )
    )
    
    /** 所有内置 Manifest 列表 */
    val all: List<LanguageManifest> = listOf(zhCN, enUS, jaJP)
}
