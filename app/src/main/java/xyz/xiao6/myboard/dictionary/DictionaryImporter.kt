package xyz.xiao6.myboard.dictionary

import android.content.Context
import xyz.xiao6.myboard.dictionary.format.ExternalDictionaryConverter
import xyz.xiao6.myboard.dictionary.format.SemVer
import xyz.xiao6.myboard.model.DictionarySpec
import java.io.File

/**
 * On-device dictionary import/conversion helper.
 *
 * Typical flow:
 * 1) User selects an input dictionary file (e.g. *.dict.yaml)
 * 2) Convert it into internal `.mybdict` (MYBDF v1) under filesDir
 * 3) (Optional) write a sidecar DictionarySpec json for extra constraints like layoutIds
 */
object DictionaryImporter {
    data class ImportRequest(
        val inputFile: File,
        val formatId: String,
        val dictionaryId: String,
        val name: String? = null,
        val languages: List<String> = emptyList(),
        val dictionaryVersion: String = "1.0.0",
        val layoutIds: List<String> = emptyList(),
        val enabled: Boolean = true,
        val priority: Int = 0,
        val writeSpecJson: Boolean = false,
    )

    data class ImportResult(
        val outputFile: File,
        val specFile: File?,
        val spec: DictionarySpec,
    )

    fun import(context: Context, req: ImportRequest): ImportResult {
        require(req.dictionaryId.isNotBlank()) { "dictionaryId is blank" }
        require(req.formatId.isNotBlank()) { "formatId is blank" }
        require(req.inputFile.exists()) { "Input not found: ${req.inputFile.absolutePath}" }

        val outDir = File(context.filesDir, "dictionary").apply { mkdirs() }
        val outFile = File(outDir, "${req.dictionaryId}.mybdict")

        ExternalDictionaryConverter.convertToMybdf(
            input = req.inputFile,
            formatId = req.formatId,
            output = outFile,
            dictionaryId = req.dictionaryId,
            name = req.name,
            languages = req.languages,
            dictVersion = SemVer.parse(req.dictionaryVersion),
        )

        val spec = DictionarySpec(
            dictionaryId = req.dictionaryId,
            name = req.name,
            localeTags = req.languages,
            layoutIds = req.layoutIds,
            assetPath = null,
            filePath = outFile.absolutePath,
            dictionaryVersion = req.dictionaryVersion,
            enabled = req.enabled,
            priority = req.priority,
        )

        val specFile = if (req.writeSpecJson) {
            val f = File(outDir, "${req.dictionaryId}.json")
            // Minimal json without forcing kotlinx.serialization dependency in this helper.
            f.writeText(
                buildString {
                    append("{\n")
                    append("  \"dictionaryId\": \"").append(req.dictionaryId).append("\",\n")
                    if (req.name != null) append("  \"name\": \"").append(req.name).append("\",\n")
                    append("  \"localeTags\": ").append(req.languages.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }).append(",\n")
                    append("  \"layoutIds\": ").append(req.layoutIds.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }).append(",\n")
                    append("  \"filePath\": \"").append(outFile.absolutePath).append("\",\n")
                    append("  \"dictionaryVersion\": \"").append(req.dictionaryVersion).append("\",\n")
                    append("  \"enabled\": ").append(req.enabled).append(",\n")
                    append("  \"priority\": ").append(req.priority).append("\n")
                    append("}\n")
                },
            )
            f
        } else {
            null
        }

        return ImportResult(outputFile = outFile, specFile = specFile, spec = spec)
    }
}
