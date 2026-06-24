package xyz.xiao6.myboard.contract.state

/**
 * 语言区域标签。
 * 值为 BCP 47 格式，例如 "zh-CN"、"en-US"、"ja-JP"。
 */
@JvmInline
value class LocaleTag(val value: String) {
    override fun toString(): String = value
}
