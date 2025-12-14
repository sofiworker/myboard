package xyz.xiao6.myboard.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
object DictionaryParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
    }

    fun parsePack(text: String): DictionaryPack {
        val normalized = stripJsonLineComments(text)
        return json.decodeFromString(DictionaryPack.serializer(), normalized)
    }

    fun parseSpec(text: String): DictionarySpec {
        val normalized = stripJsonLineComments(text)
        return json.decodeFromString(DictionarySpec.serializer(), normalized)
    }

    private fun stripJsonLineComments(text: String): String {
        val out = StringBuilder(text.length)
        var inString = false
        var escaped = false

        var i = 0
        while (i < text.length) {
            val c = text[i]

            if (!inString && c == '/' && i + 1 < text.length && text[i + 1] == '/') {
                while (i < text.length && text[i] != '\n') i++
                continue
            }

            out.append(c)

            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
            } else if (c == '"') {
                inString = true
            }

            i++
        }

        return out.toString()
    }
}

