package xyz.xiao6.myboard.core.layout

import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import xyz.xiao6.myboard.core.contract.LayoutDoc

/**
 * 布局文档解析器。
 * 将 JSONC 文本解析为 LayoutDoc 对象。
 */
object LayoutDocParser {
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }
    
    /**
     * 从 JSONC 字符串解析 LayoutDoc。
     */
    fun parse(jsoncText: String): LayoutDoc {
        val stripped = JsoncParser.stripTrailingCommas(
            JsoncParser.stripComments(jsoncText)
        )
        return json.decodeFromString<LayoutDoc>(stripped)
    }
    
    /**
     * 从纯 JSON 字符串解析 LayoutDoc。
     */
    fun parseJson(jsonText: String): LayoutDoc {
        return json.decodeFromString<LayoutDoc>(jsonText)
    }
}
