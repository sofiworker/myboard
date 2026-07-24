package xyz.xiao6.myboard.contract.state

import java.io.Serializable as JavaSerializable
import java.util.Locale
import kotlinx.serialization.Serializable

/**
 * 目标输出文字系统的 ISO 15924 风格标识。
 *
 * 仅 [parse] 接受外部输入，调用方应在注册能力前验证该标识是否被支持。
 */
@Serializable
@JvmInline
value class Script(val value: String) : JavaSerializable {

    override fun toString(): String = value

    companion object {
        val LATN = Script("LATN")
        val HANI = Script("HANI")
        val HIRA = Script("HIRA")
        val KANA = Script("KANA")
        val HANG = Script("HANG")
        val ARAB = Script("ARAB")
        val THAI = Script("THAI")
        val DEVA = Script("DEVA")

        /**
         * 将四个 ASCII 字母组成的标识规范化为大写；其余输入返回 null。
         */
        fun parse(value: String): Script? {
            if (value.length != SCRIPT_IDENTIFIER_LENGTH || !value.all(::isAsciiLetter)) {
                return null
            }

            return Script(value.uppercase(Locale.ROOT))
        }

        private fun isAsciiLetter(char: Char): Boolean =
            char in 'A'..'Z' || char in 'a'..'z'

        private const val SCRIPT_IDENTIFIER_LENGTH = 4
    }
}
