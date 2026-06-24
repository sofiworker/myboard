package xyz.xiao6.myboard.contract.state

/**
 * 正交输入状态。
 * Locale + Script + Schema 三者共同描述"当前用户希望用什么语境、输出什么文字、通过什么方案输入"。
 */
data class OrthogonalState(
    val locale: LocaleTag,
    val script: Script,
    val schema: Schema
) {
    override fun toString(): String = "$locale | $script | $schema"
}
