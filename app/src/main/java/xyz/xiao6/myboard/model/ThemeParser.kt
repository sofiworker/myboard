package xyz.xiao6.myboard.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

object ThemeParser {
    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    fun parseThemeSpec(text: String): ThemeSpec {
        return json.decodeFromString(ThemeSpec.serializer(), text)
    }
}
