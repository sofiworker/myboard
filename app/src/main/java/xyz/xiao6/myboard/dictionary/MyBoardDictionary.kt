package xyz.xiao6.myboard.dictionary

import android.content.Context
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryFileV1
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets

/**
 * MyBoard internal dictionary file.
 *
 * Supports:
 * - File (MYBDF v1): magic "MYBDF001" (metadata + checksums + payload)
 * - Payload v1: magic "MYBDICT1"
 */
class MyBoardDictionary private constructor(
    private val buffer: ByteBuffer,
    private val layout: Layout,
) {
    private data class Layout(
        val codeCount: Int,
        val entryCount: Int,
        val codeIndexOffset: Int,
        val entryTableOffset: Int,
        val codeBlobOffset: Int,
        val wordBlobOffset: Int,
    )

    fun candidates(code: String, limit: Int = 50): List<String> {
        if (layout.codeCount == 0) return emptyList()
        if (code.isBlank()) return emptyList()
        if (limit <= 0) return emptyList()

        val rec = findCodeRecord(code) ?: return emptyList()
        val max = minOf(rec.count, limit)
        val out = ArrayList<String>(max)
        for (i in 0 until max) {
            val entryIndex = rec.first + i
            if (entryIndex !in 0 until layout.entryCount) break
            val pos = layout.entryTableOffset + entryIndex * ENTRY_RECORD_SIZE
            val wordOffset = readU32Le(buffer, pos)
            out.add(readCStringUtf8(buffer, layout.wordBlobOffset + wordOffset))
        }
        return out
    }

    /**
     * Prefix search: returns candidates for any codes starting with [prefix].
     *
     * This is used for IME composing where the user input is typically a prefix of the full code.
     * Note: results are collected in code order and are not globally weight-sorted.
     */
    fun candidatesByPrefix(prefix: String, limit: Int = 50): List<String> {
        if (layout.codeCount == 0) return emptyList()
        val p = prefix.trim()
        if (p.isBlank()) return emptyList()
        if (limit <= 0) return emptyList()

        val out = ArrayList<String>(minOf(limit, 64))
        val start = findFirstCodeIndexAtOrAfter(p)
        if (start !in 0 until layout.codeCount) return emptyList()

        val targetBytes = p.toByteArray(StandardCharsets.UTF_8)
        var i = start
        while (i < layout.codeCount && out.size < limit) {
            val idxPos = layout.codeIndexOffset + i * CODE_INDEX_RECORD_SIZE
            val codeOffset = readU32Le(buffer, idxPos)
            val codeStr = readCStringUtf8(buffer, layout.codeBlobOffset + codeOffset)
            if (!codeStr.startsWith(p)) break

            val first = readU32Le(buffer, idxPos + 4)
            val count = readU32Le(buffer, idxPos + 8)
            val max = minOf(count, limit - out.size)
            for (j in 0 until max) {
                val entryIndex = first + j
                if (entryIndex !in 0 until layout.entryCount) break
                val pos = layout.entryTableOffset + entryIndex * ENTRY_RECORD_SIZE
                val wordOffset = readU32Le(buffer, pos)
                out.add(readCStringUtf8(buffer, layout.wordBlobOffset + wordOffset))
                if (out.size >= limit) break
            }

            i++
        }

        return out
    }

    private data class CodeRecord(val first: Int, val count: Int)

    private fun findCodeRecord(target: String): CodeRecord? {
        val targetBytes = target.toByteArray(StandardCharsets.UTF_8)
        var lo = 0
        var hi = layout.codeCount - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val idxPos = layout.codeIndexOffset + mid * CODE_INDEX_RECORD_SIZE
            val codeOffset = readU32Le(buffer, idxPos)
            val cmp = compareCStringUtf8(buffer, layout.codeBlobOffset + codeOffset, targetBytes)
            when {
                cmp == 0 -> {
                    val first = readU32Le(buffer, idxPos + 4)
                    val count = readU32Le(buffer, idxPos + 8)
                    return CodeRecord(first = first, count = count)
                }
                cmp < 0 -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return null
    }

    private fun findFirstCodeIndexAtOrAfter(prefix: String): Int {
        val targetBytes = prefix.toByteArray(StandardCharsets.UTF_8)
        var lo = 0
        var hi = layout.codeCount
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            val idxPos = layout.codeIndexOffset + mid * CODE_INDEX_RECORD_SIZE
            val codeOffset = readU32Le(buffer, idxPos)
            val cmp = compareCStringUtf8(buffer, layout.codeBlobOffset + codeOffset, targetBytes)
            if (cmp < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    companion object {
        private const val MAGIC_FILE_V1 = "MYBDF001"
        private const val MAGIC_PAYLOAD_V1 = "MYBDICT1"

        private const val CODE_INDEX_RECORD_SIZE = 12
        private const val ENTRY_RECORD_SIZE = 8

        fun fromAsset(context: Context, assetPath: String): MyBoardDictionary {
            val bytes = context.assets.open(assetPath).use { it.readBytes() }
            return fromBytes(bytes)
        }

        fun fromFile(path: File): MyBoardDictionary {
            val file = requireNotNull(path) { "path is null" }
            require(file.exists()) { "File not found: ${file.absolutePath}" }
            val mapped = RandomAccessFile(file, "r").use { raf ->
                raf.channel.map(FileChannel.MapMode.READ_ONLY, 0, file.length()).order(ByteOrder.LITTLE_ENDIAN)
            }
            return fromByteBuffer(mapped)
        }

        fun fromBytes(bytes: ByteArray): MyBoardDictionary {
            require(bytes.size >= 8) { "Invalid dictionary file: too small (${bytes.size})" }
            val magic = bytes.decodeToString(0, 8)
            return when (magic) {
                MAGIC_FILE_V1 -> {
                    val decoded = MyBoardDictionaryFileV1.decode(bytes)
                    fromByteBuffer(ByteBuffer.wrap(decoded.payload).order(ByteOrder.LITTLE_ENDIAN))
                }
                MAGIC_PAYLOAD_V1 -> fromByteBuffer(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
                else -> error("Unknown dictionary magic: $magic")
            }
        }

        private fun fromByteBuffer(payload: ByteBuffer): MyBoardDictionary {
            payload.order(ByteOrder.LITTLE_ENDIAN)
            require(payload.remaining() >= 12) { "Invalid payload: too small (${payload.remaining()})" }
            val magic = readMagic(payload, 0)
            return when (magic) {
                MAGIC_PAYLOAD_V1 -> MyBoardDictionary(payload, parseLayoutV1(payload))
                else -> error("Unknown payload magic: $magic")
            }
        }

        private fun parseLayoutV1(buf: ByteBuffer): Layout {
            val version = readU32Le(buf, 8)
            require(version == 1) { "Unsupported payload version: $version" }
            val codeCount = readU32Le(buf, 16)
            val entryCount = readU32Le(buf, 20)
            val codeIndexOffset = readU32Le(buf, 24)
            val entryTableOffset = readU32Le(buf, 28)
            val codeBlobOffset = readU32Le(buf, 32)
            val wordBlobOffset = readU32Le(buf, 36)
            val payloadSize = readU32Le(buf, 40)
            require(buf.limit() == payloadSize) { "payload_size mismatch: header=$payloadSize actual=${buf.limit()}" }

            return Layout(
                codeCount = codeCount,
                entryCount = entryCount,
                codeIndexOffset = codeIndexOffset,
                entryTableOffset = entryTableOffset,
                codeBlobOffset = codeBlobOffset,
                wordBlobOffset = wordBlobOffset,
            )
        }

        private fun readMagic(buf: ByteBuffer, pos: Int): String {
            val tmp = ByteArray(8)
            val dup = buf.duplicate()
            dup.position(pos)
            dup.get(tmp)
            return String(tmp, StandardCharsets.UTF_8)
        }

        private fun readU32Le(b: ByteArray, pos: Int): Int {
            if (pos + 4 > b.size) throw EOFException("readU32Le out of range: pos=$pos size=${b.size}")
            return (b[pos].toInt() and 0xFF) or
                ((b[pos + 1].toInt() and 0xFF) shl 8) or
                ((b[pos + 2].toInt() and 0xFF) shl 16) or
                ((b[pos + 3].toInt() and 0xFF) shl 24)
        }

        private fun readU32Le(buf: ByteBuffer, pos: Int): Int {
            if (pos + 4 > buf.limit()) throw EOFException("readU32Le out of range: pos=$pos size=${buf.limit()}")
            return buf.getInt(pos)
        }

        private fun cStringByteLen(buf: ByteBuffer, start: Int): Int {
            var i = start
            while (i < buf.limit() && buf.get(i) != 0.toByte()) i++
            if (i >= buf.limit()) throw EOFException("CString not terminated: start=$start size=${buf.limit()}")
            return i - start
        }

        private fun readCStringUtf8(buf: ByteBuffer, start: Int): String {
            val len = cStringByteLen(buf, start)
            val bytes = ByteArray(len)
            val dup = buf.duplicate()
            dup.position(start)
            dup.get(bytes)
            return String(bytes, StandardCharsets.UTF_8)
        }

        private fun compareCStringUtf8(buf: ByteBuffer, start: Int, target: ByteArray): Int {
            var i = 0
            var p = start
            while (true) {
                if (p >= buf.limit()) throw EOFException("CString not terminated: start=$start size=${buf.limit()}")
                val b = buf.get(p)
                if (b == 0.toByte()) {
                    return if (i >= target.size) 0 else -1
                }
                if (i >= target.size) return 1
                val bi = b.toInt() and 0xFF
                val ti = target[i].toInt() and 0xFF
                if (bi != ti) return bi - ti
                i++
                p++
            }
        }
    }
}
