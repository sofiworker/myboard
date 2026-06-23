package xyz.xiao6.myboard.core.contract

/**
 * 输入方案的稳定 ID。
 * 不是固定枚举，内置 Schema 只提供常量。
 */
@JvmInline
value class Schema(val value: String) {
    override fun toString(): String = value
}

/**
 * 内置 Schema 常量。
 */
object BuiltInSchemas {
    val LATIN_DIRECT = Schema("LATIN_DIRECT")
    val PINYIN = Schema("PINYIN")
    val DOUBLE_PINYIN = Schema("DOUBLE_PINYIN")
    val ROMAJI = Schema("ROMAJI")
    val VOICE = Schema("VOICE")
    val HANDWRITING = Schema("HANDWRITING")
}
