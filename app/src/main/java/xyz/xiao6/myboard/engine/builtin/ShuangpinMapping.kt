package xyz.xiao6.myboard.engine.builtin

import kotlinx.serialization.json.*

/**
 * 自然码双拼映射数据。
 */
data class ShuangpinMapping(
    val initialMap: Map<String, String>,
    val finalMap: Map<String, String>
) {
    companion object {
        fun fromJson(jsonString: String): ShuangpinMapping {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val initials = json["initials"]?.jsonObject?.mapValues { (_, v) ->
                v.jsonPrimitive.content
            } ?: emptyMap()
            val finals = json["finals"]?.jsonObject?.mapValues { (_, v) ->
                v.jsonPrimitive.content
            } ?: emptyMap()
            return ShuangpinMapping(initials, finals)
        }
    }

    /**
     * 将双拼的声母键转换为全拼声母。
     * 例如：'v' → "zh", 'u' → "sh"
     */
    fun resolveInitial(key: String): String {
        return initialMap[key] ?: key
    }

    /**
     * 将双拼的韵母键转换为全拼韵母。
     * 例如：'q' → "iu", 'w' → "ei"
     */
    fun resolveFinal(key: String): String {
        return finalMap[key] ?: key
    }
}
