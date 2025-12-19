package xyz.xiao6.myboard.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
object LayoutParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        coerceInputValues = true
    }

    fun parse(text: String): KeyboardLayout {
        val normalized = stripJsonLineComments(text)
        return json.decodeFromString(KeyboardLayout.serializer(), normalized)
    }

    /**
     * Best-effort parse: returns null when the JSON doesn't look like a layout.
     *
     * This is used to keep example/schema JSON files under assets/layouts (e.g. key.json/token.json)
     * without breaking runtime loading.
     */
    fun parseOrNull(text: String): KeyboardLayout? {
        val normalized = stripJsonLineComments(text)
        val root =
            runCatching { json.parseToJsonElement(normalized) }
                .getOrNull()
                ?: return null
        val obj: JsonObject = runCatching { root.jsonObject }.getOrNull() ?: return null
        val layoutId = obj["layoutId"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        if (layoutId.isBlank()) return null
        return runCatching { json.decodeFromJsonElement(KeyboardLayout.serializer(), root) }.getOrNull()
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
