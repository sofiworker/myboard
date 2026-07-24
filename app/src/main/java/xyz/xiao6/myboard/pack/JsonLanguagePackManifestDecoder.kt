package xyz.xiao6.myboard.pack

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.contract.manifest.validate

@OptIn(ExperimentalSerializationApi::class)
class JsonLanguagePackManifestDecoder(
    private val json: Json = Json {
        ignoreUnknownKeys = false
        isLenient = false
        explicitNulls = false
    }
) : LanguagePackManifestDecoder {

    override fun decode(bytes: ByteArray) = runCatching {
        val text = StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        val manifest = json.decodeFromString<LanguagePackManifestDto>(text).toDomain()
        val validation = manifest.validate()
        require(validation.isValid) {
            "Manifest validation failed: ${validation.errors.joinToString()}"
        }
        manifest
    }.getOrElse { error ->
        throw IllegalArgumentException(
            error.message?.takeIf(String::isNotBlank) ?: "Language pack manifest is invalid",
            error
        )
    }
}
