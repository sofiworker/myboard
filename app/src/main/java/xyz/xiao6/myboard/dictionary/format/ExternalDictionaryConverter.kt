package xyz.xiao6.myboard.dictionary.format

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

/**
 * Converts external dictionary files into internal `.mybdict` (MYBDF v1).
 *
 * This is intended for user-upload import on device.
 * Build-time conversion is handled by `scripts/dict_tool.py`.
 */
/**
 * Converts external dictionary sources into MyBoard `.mybdict` (MYBDF v1 + MYBDICT v1).
 *
 * This is intended for user-upload import on device.
 * Build-time conversion is handled by `scripts/dict_tool.py`.
 */
object ExternalDictionaryConverter {
    private val parsers: List<ExternalDictionaryParser> = listOf(
        RimeDictYamlExternalParser(),
    )

    fun convertToMybdf(
        input: File,
        formatId: String,
        output: File,
        dictionaryId: String,
        name: String?,
        languages: List<String>,
        dictVersion: SemVer,
        compression: MyBoardDictionaryFileV1.Compression = MyBoardDictionaryFileV1.Compression.ZLIB,
    ) {
        val parser = parsers.firstOrNull { it.formatId == formatId }
            ?: error("Unknown format: $formatId (available=${parsers.joinToString { it.formatId }})")

        val (sourceVersionText, payload) = FileInputStream(input).use { fis ->
            BufferedReader(InputStreamReader(fis, Charsets.UTF_8)).use { reader ->
                when (parser) {
                    is RimeDictYamlExternalParser -> {
                        val (header, seq) = parser.parseWithHeader(reader)
                        header.versionText to MyBoardDictionaryPayloadV1.encode(seq)
                    }
                    else -> null to MyBoardDictionaryPayloadV1.encode(parser.parse(reader))
                }
            }
        }

        val meta = MyBoardDictionaryFileV1.DictionaryFileMeta(
            dictionaryId = dictionaryId,
            name = name,
            languages = languages,
            sourceFormat = formatId,
            sourceVersion = sourceVersionText,
            createdBy = "myboard_app",
            createdAtEpochMs = System.currentTimeMillis(),
        )

        val bytes = MyBoardDictionaryFileV1.encode(
            meta = meta,
            dictVersion = dictVersion,
            payloadUncompressed = payload,
            compression = compression,
        )

        output.parentFile?.mkdirs()
        output.writeBytes(bytes)
    }
}
