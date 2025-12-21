package xyz.xiao6.myboard.dictionary

import android.content.Context
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import xyz.xiao6.myboard.dictionary.format.DictionaryEntry
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryFileV1
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryPayloadV1
import xyz.xiao6.myboard.dictionary.format.SemVer
import xyz.xiao6.myboard.model.DictionaryParser
import xyz.xiao6.myboard.model.DictionarySpec

@OptIn(ExperimentalSerializationApi::class)
class UserDictionaryStore(
    private val context: Context,
) {
    private val userDir = File(context.filesDir, "dictionary")
    private val sourceDir = File(userDir, "sources")
    private val json = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
    }

    fun hasSourceFile(dictionaryId: String): Boolean = sourceFile(dictionaryId).exists()

    fun isUserDictionary(spec: DictionarySpec): Boolean {
        val path = spec.filePath?.trim().orEmpty()
        return path.isNotBlank() && path.startsWith(userDir.absolutePath)
    }

    fun createCustomDictionary(
        spec: DictionarySpec,
        sourceFormat: String = SOURCE_FORMAT_CUSTOM,
    ): DictionarySpec {
        val dictionaryId = spec.dictionaryId.trim()
        require(dictionaryId.isNotBlank()) { "dictionaryId is blank" }

        userDir.mkdirs()
        sourceDir.mkdirs()

        val dictFile = dictFile(dictionaryId)
        val sourceFile = sourceFile(dictionaryId)
        if (!sourceFile.exists()) {
            sourceFile.writeText("")
        }

        val fileSpec = spec.copy(
            assetPath = null,
            filePath = dictFile.absolutePath,
        )
        writeSpec(fileSpec)
        rebuildDictionaryFromSource(dictionaryId, fileSpec, sourceFormat = sourceFormat)
        return fileSpec
    }

    fun appendEntry(
        dictionaryId: String,
        entry: DictionaryEntry,
    ) {
        val id = dictionaryId.trim()
        require(id.isNotBlank()) { "dictionaryId is blank" }
        val sourceFile = sourceFile(id)
        require(sourceFile.exists()) { "Source not found: ${sourceFile.absolutePath}" }
        sourceFile.appendText("${entry.word}\t${entry.code}\t${entry.weight}\n")
        val spec = readSpec(id) ?: error("Spec not found for $id")
        rebuildDictionaryFromSource(id, spec, sourceFormat = SOURCE_FORMAT_CUSTOM)
    }

    fun deleteDictionary(dictionaryId: String) {
        val id = dictionaryId.trim()
        if (id.isBlank()) return
        dictFile(id).delete()
        specFile(id).delete()
        sourceFile(id).delete()
    }

    fun importDictionary(
        inputFile: File,
        formatId: String,
        spec: DictionarySpec,
    ): DictionarySpec {
        val dictionaryId = spec.dictionaryId.trim()
        require(dictionaryId.isNotBlank()) { "dictionaryId is blank" }

        userDir.mkdirs()

        val dictVersion = SemVer.parse(spec.dictionaryVersion ?: DEFAULT_DICT_VERSION)
        val result = DictionaryImporter.import(
            context = context,
            req = DictionaryImporter.ImportRequest(
                inputFile = inputFile,
                formatId = formatId,
                dictionaryId = dictionaryId,
                name = spec.name,
                languages = spec.localeTags,
                dictionaryVersion = dictVersion.toString(),
                layoutIds = spec.layoutIds,
                enabled = spec.enabled,
                priority = spec.priority,
                writeSpecJson = false,
            ),
        )

        val fileSpec = spec.copy(
            assetPath = null,
            filePath = result.outputFile.absolutePath,
        )
        writeSpec(fileSpec)
        return fileSpec
    }

    fun readSpec(dictionaryId: String): DictionarySpec? {
        val file = specFile(dictionaryId)
        if (!file.exists()) return null
        return DictionaryParser.parseSpec(file.readText())
    }

    private fun rebuildDictionaryFromSource(
        dictionaryId: String,
        spec: DictionarySpec,
        sourceFormat: String,
    ) {
        val sourceFile = sourceFile(dictionaryId)
        val entries = readEntries(sourceFile)
        val payload = MyBoardDictionaryPayloadV1.encode(entries)
        val meta = MyBoardDictionaryFileV1.DictionaryFileMeta(
            dictionaryId = dictionaryId,
            name = spec.name,
            languages = spec.localeTags,
            sourceFormat = sourceFormat,
            sourceVersion = null,
            createdBy = "myboard_app",
            createdAtEpochMs = System.currentTimeMillis(),
        )
        val dictVersion = SemVer.parse(spec.dictionaryVersion ?: DEFAULT_DICT_VERSION)
        val bytes = MyBoardDictionaryFileV1.encode(
            meta = meta,
            dictVersion = dictVersion,
            payloadUncompressed = payload,
            compression = MyBoardDictionaryFileV1.Compression.ZLIB,
        )
        dictFile(dictionaryId).writeBytes(bytes)
    }

    private fun readEntries(file: File): Sequence<DictionaryEntry> = sequence {
        if (!file.exists()) return@sequence
        file.bufferedReader().use { reader ->
            var raw = reader.readLine()
            while (raw != null) {
                val line = raw.trim()
                if (line.isNotBlank() && !line.startsWith("#")) {
                    val parts = line.split('\t')
                    if (parts.size >= 2) {
                        val word = parts[0].trim()
                        val code = parts[1].trim()
                        if (word.isNotBlank() && code.isNotBlank()) {
                            val weight = parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0
                            yield(DictionaryEntry(word = word, code = code, weight = weight))
                        }
                    }
                }
                raw = reader.readLine()
            }
        }
    }

    private fun writeSpec(spec: DictionarySpec) {
        val file = specFile(spec.dictionaryId)
        file.writeText(json.encodeToString(DictionarySpec.serializer(), spec))
    }

    private fun dictFile(dictionaryId: String): File = File(userDir, "$dictionaryId.mybdict")
    private fun specFile(dictionaryId: String): File = File(userDir, "$dictionaryId.json")
    private fun sourceFile(dictionaryId: String): File = File(sourceDir, "$dictionaryId.tsv")

    companion object {
        private const val DEFAULT_DICT_VERSION = "1.0.0"
        private const val SOURCE_FORMAT_CUSTOM = "custom_inline"
    }
}
