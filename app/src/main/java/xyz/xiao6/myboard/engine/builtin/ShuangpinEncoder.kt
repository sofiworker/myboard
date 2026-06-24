package xyz.xiao6.myboard.engine.builtin

/**
 * 自然码双拼编码器。
 * 将双拼输入转换为全拼序列。
 *
 * 双拼规则：
 * - 每个汉字由恰好两个键输入（声母+韵母）
 * - 零声母音节（如"啊"）使用特殊处理
 * - 翘舌音 zh/ch/sh 用 v/i/u 表示
 */
class ShuangpinEncoder(private val mapping: ShuangpinMapping) {

    /**
     * 将双拼对转换为全拼。
     * 例如："vs" → "zh" + "ong" → "zhong"
     */
    fun decodePair(initialKey: String, finalKey: String): String {
        val initial = mapping.resolveInitial(initialKey)
        val final_ = mapping.resolveFinal(finalKey)
        return initial + final_
    }

    /**
     * 将完整的双拼字符串解码为全拼序列。
     * 双拼每两个字符为一组（声母+韵母）。
     *
     * 例如："vsgo" → ["zhong", "guo"]
     */
    fun decodeAll(input: String): List<String> {
        if (input.length < 2) return listOf(input)

        val results = mutableListOf<String>()
        var i = 0
        while (i + 1 < input.length) {
            val initialKey = input[i].toString()
            val finalKey = input[i + 1].toString()
            val fullPinyin = decodePair(initialKey, finalKey)
            results.add(fullPinyin)
            i += 2
        }
        // 如果有奇数个字符，最后一个直接作为声母
        if (i < input.length) {
            results.add(mapping.resolveInitial(input[i].toString()))
        }
        return results
    }
}
