package xyz.xiao6.myboard.dictionary

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.dictionary.format.DictionaryEntry
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryFileV1
import xyz.xiao6.myboard.dictionary.format.MyBoardDictionaryPayloadV1
import xyz.xiao6.myboard.dictionary.format.SemVer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

class MyBoardDictionaryTest {
    @Test
    fun candidates_findByCode_mybdfV1_payloadV1() {
        val file = buildMybdfV1()
        val dict = MyBoardDictionary.fromBytes(file)
        assertEquals(listOf("啊", "阿"), dict.candidates("a"))
        assertEquals(emptyList<String>(), dict.candidates("missing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun mybdfV1_rejectsBadPayloadCrc() {
        MyBoardDictionary.fromBytes(tamperMybdfPayloadCrc(buildMybdfV1()))
    }

    private fun buildMybdfV1(): ByteArray {
        val payload = MyBoardDictionaryPayloadV1.encode(
            sequenceOf(
                DictionaryEntry(word = "啊", code = "a", weight = 10),
                DictionaryEntry(word = "阿", code = "a", weight = 5),
            ),
        )
        return MyBoardDictionaryFileV1.encode(
            meta = MyBoardDictionaryFileV1.DictionaryFileMeta(
                dictionaryId = "dict_pinyin",
                name = "Pinyin",
                languages = listOf("zh-CN"),
                sourceFormat = "unit_test",
            ),
            dictVersion = SemVer(1, 0, 0),
            payloadUncompressed = payload,
            compression = MyBoardDictionaryFileV1.Compression.ZLIB,
        )
    }

    private fun tamperMybdfPayloadCrc(src: ByteArray): ByteArray {
        val out = src.clone()
        val headerSize = MyBoardDictionaryFileV1.HEADER_SIZE
        val metaSize = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).getInt(36)
        val metaEnd = headerSize + metaSize

        // Change crc32_payload (offset 40) but keep crc32_header_meta consistent.
        val oldPayloadCrc = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).getInt(48)
        val newPayloadCrc = oldPayloadCrc xor 0x13572468
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).putInt(48, newPayloadCrc)

        // Recompute crc32_header_meta over [header+meta] with crc field zeroed.
        val headerMeta = out.copyOfRange(0, metaEnd)
        headerMeta[52] = 0
        headerMeta[53] = 0
        headerMeta[54] = 0
        headerMeta[55] = 0
        val crc = CRC32().apply { update(headerMeta) }.value.toInt()
        ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN).putInt(52, crc)
        return out
    }

    private fun u32le(v: Int): ByteArray = byteArrayOf(
        (v and 0xFF).toByte(),
        ((v ushr 8) and 0xFF).toByte(),
        ((v ushr 16) and 0xFF).toByte(),
        ((v ushr 24) and 0xFF).toByte(),
    )

    private fun i32le(v: Int): ByteArray = u32le(v)
}
