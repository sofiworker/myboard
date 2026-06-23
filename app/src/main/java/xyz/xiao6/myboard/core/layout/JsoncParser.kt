package xyz.xiao6.myboard.core.layout

/**
 * JSONC 解析工具。
 * 支持 JSON + 行注释（//）和块注释（/-* *-/）的剥离。
 */
object JsoncParser {
    
    /**
     * 剥离 JSONC 中的注释，返回纯 JSON 字符串。
     */
    fun stripComments(jsonc: String): String {
        val sb = StringBuilder(jsonc.length)
        var i = 0
        val len = jsonc.length
        var inString = false
        
        while (i < len) {
            val c = jsonc[i]
            
            if (inString) {
                sb.append(c)
                if (c == '\\') {
                    if (i + 1 < len) {
                        sb.append(jsonc[i + 1])
                        i += 2
                        continue
                    }
                } else if (c == '"') {
                    inString = false
                }
                i++
                continue
            }
            
            if (c == '"') {
                inString = true
                sb.append(c)
                i++
                continue
            }
            
            if (c == '/') {
                if (i + 1 < len) {
                    val nextChar = jsonc[i + 1]
                    if (nextChar == '/') {
                        // 单行注释，跳到行尾
                        i += 2
                        while (i < len && jsonc[i] != '\n') i++
                        continue
                    }
                    if (nextChar == '*') {
                        // 块注释，跳到 */
                        i += 2
                        while (i < len) {
                            if (jsonc[i] == '*' && i + 1 < len && jsonc[i + 1] == '/') {
                                i += 2
                                break
                            }
                            i++
                        }
                        continue
                    }
                }
            }
            
            sb.append(c)
            i++
        }
        
        return sb.toString()
    }
    
    /**
     * 剥离 JSON 中的尾逗号（trailing commas）。
     */
    fun stripTrailingCommas(json: String): String {
        val result = StringBuilder(json.length)
        var i = 0
        val len = json.length
        
        while (i < len) {
            val c = json[i]
            if (c == '"') {
                result.append(c)
                i++
                while (i < len) {
                    val sc = json[i]
                    result.append(sc)
                    if (sc == '\\') {
                        i++
                        if (i < len) result.append(json[i])
                    } else if (sc == '"') {
                        break
                    }
                    i++
                }
                i++
                continue
            }
            
            if (c == ',' && i + 1 < len) {
                var j = i + 1
                while (j < len && json[j].isWhitespace()) j++
                if (j < len && (json[j] == ']' || json[j] == '}')) {
                    i++
                    continue
                }
            }
            
            result.append(c)
            i++
        }
        
        return result.toString()
    }
}
