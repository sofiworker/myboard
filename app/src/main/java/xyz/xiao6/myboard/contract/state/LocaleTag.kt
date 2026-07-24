package xyz.xiao6.myboard.contract.state

import java.io.Serializable
/**
 * 语言区域标签。
 * 值为 BCP 47 格式，例如 "zh-CN"、"en-US"、"ja-JP"。
 */
@JvmInline
value class LocaleTag(val value: String) : Serializable {
    override fun toString(): String = value
}
