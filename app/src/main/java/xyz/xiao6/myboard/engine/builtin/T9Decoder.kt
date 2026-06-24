package xyz.xiao6.myboard.engine.builtin

import kotlinx.serialization.json.*

/**
 * T9 九键解码器。
 * 将数字序列转换为拼音候选列表，配合词典进行查找。
 */
class T9Decoder(private val keyMap: Map<String, List<String>>) {

    companion object {
        fun fromJson(jsonString: String): T9Decoder {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val keyMapObj = json["keyMap"]?.jsonObject ?: return T9Decoder(emptyMap())
            val map = keyMapObj.mapValues { (_, value) ->
                value.jsonArray.map { it.jsonPrimitive.content }
            }
            return T9Decoder(map)
        }
    }

    /**
     * 将数字序列的所有拼音组合解码出来。
     * 例如："22" → ["aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc"]
     */
    fun decode(sequence: String): List<String> {
        if (sequence.isEmpty()) return emptyList()
        val validSequence = sequence.filter { it.isDigit() && keyMap.containsKey(it.toString()) }
        if (validSequence.isEmpty()) return emptyList()

        val charsList = validSequence.map { keyMap[it.toString()] ?: emptyList() }
        return charsList.fold(listOf("")) { acc, list ->
            acc.flatMap { prefix -> list.map { suffix -> prefix + suffix } }
        }
    }

    /**
     * 获取单个按键对应的候选字符。
     */
    fun getCandidatesForKey(key: String): List<String> {
        return keyMap[key] ?: emptyList()
    }
}
