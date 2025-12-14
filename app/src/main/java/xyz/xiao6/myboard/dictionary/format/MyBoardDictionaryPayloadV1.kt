package xyz.xiao6.myboard.dictionary.format

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * Compact payload v1 (MYBDICT1): code -> candidate list index.
 *
 * The runtime reader is implemented in [xyz.xiao6.myboard.dictionary.MyBoardDictionary].
 * This encoder is used by the in-app importer (user uploaded dictionaries).
 */
/**
 * MYBDICT v1 payload encoder.
 *
 * This is the actual search index used by the IME runtime (code -> candidates).
 * The reader lives in `xyz.xiao6.myboard.dictionary.MyBoardDictionary`.
 */
object MyBoardDictionaryPayloadV1 {
    /**
     * Payload format v1.
     *
     * Note: This is the *first* shipped payload format in this app (no legacy payloads kept).
     */
    const val MAGIC_V1: String = "MYBDICT1"
    const val VERSION_V1: Int = 1

    private const val HEADER_SIZE = 8 + 4 * 9
    private const val CODE_INDEX_RECORD_SIZE = 12
    private const val ENTRY_RECORD_SIZE = 8

    /**
     * Encodes entries into MYBDICT v1 bytes.
     *
     * The index is sorted by code; candidates for each code are sorted by (-weight, word).
     */
    fun encode(entries: Sequence<DictionaryEntry>): ByteArray {
        val grouped = entries
            .filter { it.code.isNotBlank() && it.word.isNotBlank() }
            .groupBy { it.code }
            .mapValues { (_, list) ->
                list.sortedWith(compareByDescending<DictionaryEntry> { it.weight }.thenBy { it.word })
            }

        val codes = grouped.keys.sorted()

        val codeOffsets = LinkedHashMap<String, Int>(codes.size)
        val codeBlob = ByteArrayOutputStream()
        for (code in codes) {
            codeOffsets[code] = codeBlob.size()
            codeBlob.write(code.toByteArray(StandardCharsets.UTF_8))
            codeBlob.write(0)
        }

        val wordBlob = ByteArrayOutputStream()
        val entryWordOffsets = ArrayList<Int>(grouped.values.sumOf { it.size })
        val entryWeights = ArrayList<Int>(entryWordOffsets.size)
        for (code in codes) {
            val list = grouped.getValue(code)
            for (e in list) {
                entryWordOffsets += wordBlob.size()
                entryWeights += e.weight
                wordBlob.write(e.word.toByteArray(StandardCharsets.UTF_8))
                wordBlob.write(0)
            }
        }

        val codeIndex = ByteArrayOutputStream()
        var firstEntryIndex = 0
        for (code in codes) {
            val count = grouped.getValue(code).size
            codeIndex.write(u32le(codeOffsets.getValue(code)))
            codeIndex.write(u32le(firstEntryIndex))
            codeIndex.write(u32le(count))
            firstEntryIndex += count
        }

        val entryTable = ByteArrayOutputStream()
        for (i in entryWordOffsets.indices) {
            entryTable.write(u32le(entryWordOffsets[i]))
            entryTable.write(i32le(entryWeights[i]))
        }

        val codeCount = codes.size
        val entryCount = entryWordOffsets.size
        val codeIndexOffset = HEADER_SIZE
        val entryTableOffset = codeIndexOffset + codeCount * CODE_INDEX_RECORD_SIZE
        val codeBlobOffset = entryTableOffset + entryCount * ENTRY_RECORD_SIZE
        val wordBlobOffset = codeBlobOffset + codeBlob.size()
        val payloadSize = wordBlobOffset + wordBlob.size()

        val out = ByteArrayOutputStream(payloadSize)
        out.write(MAGIC_V1.toByteArray(StandardCharsets.UTF_8))
        out.write(u32le(VERSION_V1))
        out.write(u32le(0))
        out.write(u32le(codeCount))
        out.write(u32le(entryCount))
        out.write(u32le(codeIndexOffset))
        out.write(u32le(entryTableOffset))
        out.write(u32le(codeBlobOffset))
        out.write(u32le(wordBlobOffset))
        out.write(u32le(payloadSize))
        out.write(codeIndex.toByteArray())
        out.write(entryTable.toByteArray())
        out.write(codeBlob.toByteArray())
        out.write(wordBlob.toByteArray())

        val bytes = out.toByteArray()
        check(bytes.size == payloadSize) { "payload_size mismatch: header=$payloadSize actual=${bytes.size}" }
        return bytes
    }

    private fun u32le(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v ushr 8) and 0xFF).toByte(),
        ((v ushr 16) and 0xFF).toByte(),
        ((v ushr 24) and 0xFF).toByte(),
    )

    private fun i32le(v: Int): ByteArray = u32le(v)
}
