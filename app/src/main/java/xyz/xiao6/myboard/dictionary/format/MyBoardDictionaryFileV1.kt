package xyz.xiao6.myboard.dictionary.format

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.EOFException
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import java.util.zip.CRC32
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * MYBDF v1 codec (container layer).
 *
 * "MYBDF" = MyBoard Dictionary File.
 * This container wraps the payload bytes (currently: MYBDICT v1) with:
 * - fixed header (magic/version/language profile/sizes/checksums)
 * - JSON metadata (dictionaryId, languages, etc.)
 * - optional compression (zlib)
 *
 * Design goals (aligned with `dictionary/a.cc`):
 * - Fixed magic + format version (fast reject)
 * - Primary language/script/feature flags in header (fast matching)
 * - Self-contained metadata JSON (dictionaryId, applicable languages, etc.)
 * - Payload can be stored raw or zlib-compressed
 * - CRC32 checksums for header+meta and payload integrity
 *
 * Byte order: little-endian.
 *
 * Layout:
 * 1) Header (64 bytes)
 * 2) Metadata JSON (UTF-8, length=meta_size)
 * 3) Payload bytes (length=payload_size_stored)
 */
object MyBoardDictionaryFileV1 {
    const val MAGIC: String = "MYBDF001"
    const val FORMAT_VERSION: Int = 1
    const val HEADER_SIZE: Int = 64

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = false
    }

    enum class Compression(val id: Int) {
        NONE(0),
        ZLIB(1),
    }

    /**
     * Metadata JSON stored right after the fixed header.
     *
     * NOTE: This JSON is part of the `crc32_header_meta` checksum.
     */
    @Serializable
    data class DictionaryFileMeta(
        val dictionaryId: String,
        val name: String? = null,
        /**
         * Applicable languages (BCP-47).
         * Example: ["zh-CN"] / ["en"].
         */
        val languages: List<String> = emptyList(),
        val sourceFormat: String? = null,
        val sourceVersion: String? = null,
        val createdBy: String? = "myboard",
        val createdAtEpochMs: Long? = null,
    )

    /**
     * Parsed header fields.
     *
     * The `languageCode/regionCode/scriptType/featureFlags` here correspond to the "primary" profile,
     * which is meant for fast selection and may be derived from `meta.languages.firstOrNull()`.
     */
    data class Header(
        val dictVersion: SemVer,
        val languageCode: Int,
        val regionCode: Int,
        val scriptType: Int,
        val featureFlags: Int,
        val compression: Compression,
        val metaSize: Int,
        val payloadSizeUncompressed: Int,
        val payloadSizeStored: Int,
        val crc32Payload: Int,
        val crc32HeaderMeta: Int,
    )

    data class Decoded(
        val header: Header,
        val meta: DictionaryFileMeta,
        /**
         * Always returned as *uncompressed* payload bytes.
         */
        val payload: ByteArray,
    )

    /**
     * Reads only (header + meta) from a `.mybdict` file, without loading payload.
     *
     * Useful for listing user dictionaries quickly.
     */
    fun readHeaderAndMeta(file: File): Pair<Header, DictionaryFileMeta> {
        RandomAccessFile(file, "r").use { raf ->
            val headerBytes = ByteArray(HEADER_SIZE)
            raf.readFully(headerBytes)

            val header = decodeHeader(headerBytes)
            val metaBytes = ByteArray(header.metaSize)
            raf.readFully(metaBytes)
            val metaJson = metaBytes.toString(Charsets.UTF_8)
            val meta = json.decodeFromString(DictionaryFileMeta.serializer(), metaJson)

            // Validate header+meta checksum.
            val headerMeta = ByteArray(headerBytes.size + metaBytes.size)
            System.arraycopy(headerBytes, 0, headerMeta, 0, headerBytes.size)
            System.arraycopy(metaBytes, 0, headerMeta, headerBytes.size, metaBytes.size)
            headerMeta[52] = 0
            headerMeta[53] = 0
            headerMeta[54] = 0
            headerMeta[55] = 0
            val crcComputed = crc32(headerMeta)
            require(crcComputed == header.crc32HeaderMeta) {
                "Invalid MYBDF: header/meta CRC32 mismatch (expected=${header.crc32HeaderMeta} actual=$crcComputed)"
            }

            return header to meta
        }
    }

    /**
     * Encodes bytes into a `.mybdict` file content (MYBDF v1).
     *
     * The payload is typically a compact index payload (e.g. MYBDICT1).
     */
    fun encode(
        meta: DictionaryFileMeta,
        dictVersion: SemVer,
        payloadUncompressed: ByteArray,
        compression: Compression = Compression.ZLIB,
        profile: LanguageProfile.HeaderProfile = LanguageProfile.fromPrimaryLanguageTag(meta.languages.firstOrNull()),
    ): ByteArray {
        val metaJson = json.encodeToString(meta).toByteArray(StandardCharsets.UTF_8)
        require(metaJson.size >= 0) { "meta_size invalid: ${metaJson.size}" }

        val payloadStored = when (compression) {
            Compression.NONE -> payloadUncompressed
            Compression.ZLIB -> zlibCompress(payloadUncompressed, level = 9)
        }

        // CRC32 is always computed over the *uncompressed* payload bytes.
        val crcPayload = crc32(payloadUncompressed)

        // Header layout (64 bytes):
        // 0..7   magic[8]
        // 8..11  u32 format_version
        // 12..19 u16 a.b.c + reserved0
        // 20..27 language/profile fields
        // 28..55 sizes + CRC fields
        // 56..63 reserved
        val header = ByteArray(HEADER_SIZE)
        val buf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(MAGIC.toByteArray(StandardCharsets.UTF_8))
        buf.putInt(FORMAT_VERSION)
        buf.putShort(dictVersion.major.toShort())
        buf.putShort(dictVersion.minor.toShort())
        buf.putShort(dictVersion.patch.toShort())
        buf.putShort(0)

        buf.putShort((profile.languageCode and 0xFFFF).toShort())
        buf.put((profile.regionCode.id and 0xFF).toByte())
        buf.put((profile.scriptType.id and 0xFF).toByte())
        buf.putInt(profile.featureFlags)

        buf.putInt(compression.id and 0xF)
        buf.putInt(HEADER_SIZE)
        buf.putInt(metaJson.size)
        buf.putInt(payloadUncompressed.size)
        buf.putInt(payloadStored.size)
        buf.putInt(crcPayload)
        buf.putInt(0) // placeholder: crc32_header_meta
        buf.putLong(0L) // reserved[8]

        val headerMeta = ByteArray(header.size + metaJson.size)
        System.arraycopy(header, 0, headerMeta, 0, header.size)
        System.arraycopy(metaJson, 0, headerMeta, header.size, metaJson.size)

        // Compute crc32_header_meta over [header+meta] with the crc field zeroed.
        val crcHeaderMeta = crc32(headerMeta)
        ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN).putInt(52, crcHeaderMeta)

        val out = ByteArray(header.size + metaJson.size + payloadStored.size)
        System.arraycopy(header, 0, out, 0, header.size)
        System.arraycopy(metaJson, 0, out, header.size, metaJson.size)
        System.arraycopy(payloadStored, 0, out, header.size + metaJson.size, payloadStored.size)
        return out
    }

    /**
     * Decodes a `.mybdict` file content (in-memory bytes).
     */
    fun decode(bytes: ByteArray): Decoded {
        val header = decodeHeader(bytes)
        val metaStart = HEADER_SIZE
        val metaEnd = metaStart + header.metaSize
        if (metaEnd > bytes.size) throw EOFException("Invalid MYBDF: meta out of range (end=$metaEnd size=${bytes.size})")

        val metaJson = bytes.copyOfRange(metaStart, metaEnd).toString(Charsets.UTF_8)
        val meta = json.decodeFromString(DictionaryFileMeta.serializer(), metaJson)

        // Validate header+meta checksum.
        val headerMeta = bytes.copyOfRange(0, metaEnd)
        headerMeta[52] = 0
        headerMeta[53] = 0
        headerMeta[54] = 0
        headerMeta[55] = 0
        val crcComputed = crc32(headerMeta)
        require(crcComputed == header.crc32HeaderMeta) {
            "Invalid MYBDF: header/meta CRC32 mismatch (expected=${header.crc32HeaderMeta} actual=$crcComputed)"
        }

        val payloadStart = metaEnd
        val payloadEnd = payloadStart + header.payloadSizeStored
        if (payloadEnd > bytes.size) throw EOFException("Invalid MYBDF: payload out of range (end=$payloadEnd size=${bytes.size})")
        val payloadStored = bytes.copyOfRange(payloadStart, payloadEnd)
        val payload = when (header.compression) {
            Compression.NONE -> payloadStored
            Compression.ZLIB -> zlibDecompress(payloadStored, header.payloadSizeUncompressed)
        }

        if (payload.size != header.payloadSizeUncompressed) {
            throw EOFException("Invalid MYBDF: payload size mismatch (expected=${header.payloadSizeUncompressed} actual=${payload.size})")
        }

        val crc = crc32(payload)
        require(crc == header.crc32Payload) {
            "Invalid MYBDF: payload CRC32 mismatch (expected=${header.crc32Payload} actual=$crc)"
        }

        return Decoded(header = header, meta = meta, payload = payload)
    }

    /**
     * Decodes the fixed header (and validates magic/version/header_size).
     *
     * This does NOT validate `crc32_header_meta` because it needs the meta bytes.
     */
    fun decodeHeader(bytes: ByteArray): Header {
        if (bytes.size < HEADER_SIZE) throw EOFException("Invalid MYBDF: too small (${bytes.size})")
        val magic = bytes.decodeToString(0, 8)
        require(magic == MAGIC) { "Invalid MYBDF magic: $magic" }
        val buf = ByteBuffer.wrap(bytes, 0, HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        val version = buf.getInt(8)
        require(version == FORMAT_VERSION) { "Unsupported MYBDF format version: $version" }

        val major = buf.getShort(12).toInt() and 0xFFFF
        val minor = buf.getShort(14).toInt() and 0xFFFF
        val patch = buf.getShort(16).toInt() and 0xFFFF

        val languageCode = buf.getShort(20).toInt() and 0xFFFF
        val regionCode = buf.get(22).toInt() and 0xFF
        val scriptType = buf.get(23).toInt() and 0xFF
        val featureFlags = buf.getInt(24)

        val flags = buf.getInt(28)
        val headerSize = buf.getInt(32)
        require(headerSize == HEADER_SIZE) { "Unsupported MYBDF header size: $headerSize" }
        val metaSize = buf.getInt(36)
        val payloadSizeUncompressed = buf.getInt(40)
        val payloadSizeStored = buf.getInt(44)
        val crcPayload = buf.getInt(48)
        val crcHeaderMeta = buf.getInt(52)

        val compressionId = flags and 0xF
        val compression = Compression.entries.firstOrNull { it.id == compressionId }
            ?: error("Unsupported MYBDF compression: $compressionId")

        return Header(
            dictVersion = SemVer(major, minor, patch),
            languageCode = languageCode,
            regionCode = regionCode,
            scriptType = scriptType,
            featureFlags = featureFlags,
            compression = compression,
            metaSize = metaSize,
            payloadSizeUncompressed = payloadSizeUncompressed,
            payloadSizeStored = payloadSizeStored,
            crc32Payload = crcPayload,
            crc32HeaderMeta = crcHeaderMeta,
        )
    }

    private fun crc32(bytes: ByteArray): Int = CRC32().apply { update(bytes) }.value.toInt()

    private fun zlibCompress(bytes: ByteArray, level: Int): ByteArray {
        val deflater = Deflater(level, false)
        return try {
            deflater.setInput(bytes)
            deflater.finish()
            val out = ByteArray(bytes.size.coerceAtLeast(32))
            var buffer = out
            var size = 0
            while (!deflater.finished()) {
                if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
                size += deflater.deflate(buffer, size, buffer.size - size)
            }
            buffer.copyOf(size)
        } finally {
            deflater.end()
        }
    }

    private fun zlibDecompress(compressed: ByteArray, expectedSize: Int): ByteArray {
        val inflater = Inflater()
        return try {
            inflater.setInput(compressed)
            val out = ByteArray(expectedSize)
            var off = 0
            while (!inflater.finished() && off < out.size) {
                val n = inflater.inflate(out, off, out.size - off)
                if (n == 0 && inflater.needsInput()) break
                off += n
            }
            require(inflater.finished() && off == out.size) {
                "Bad zlib stream: produced=$off expected=${out.size} finished=${inflater.finished()}"
            }
            out
        } finally {
            inflater.end()
        }
    }
}
