package xyz.xiao6.myboard.decoder

import android.content.Context
import xyz.xiao6.myboard.dictionary.MyBoardDictionary
import xyz.xiao6.myboard.model.DictionarySpec
import xyz.xiao6.myboard.util.MLog
import java.util.concurrent.ConcurrentHashMap

/**
 * DecoderFactory：根据 DictionarySpec 构建对应的 Decoder。
 *
 * 目前实现：
 * - PINYIN_* -> TokenPinyinDecoder(MyBoardDictionary)
 * - 其他 -> PassthroughDecoder（后续接入英文/笔画/手写等）
 */
class DecoderFactory(
    private val context: Context,
) {
    private val logTag = "DecoderFactory"
    private val buildStamp = "2025-12-14-prefix-v1"
    private val dictCache = ConcurrentHashMap<String, MyBoardDictionary>()

    fun candidatesByPrefix(
        specs: List<DictionarySpec>,
        rawKey: String,
        limit: Int = 50,
    ): List<String> {
        if (specs.isEmpty()) return emptyList()
        if (limit <= 0) return emptyList()
        val supported = filterSupportedSpecs(specs)
        if (supported.isEmpty()) return emptyList()

        val key = normalizePinyinKey(rawKey)
        if (key.isBlank()) return emptyList()

        return mergeCandidates(
            specs = supported,
            limit = limit,
        ) { dict, max -> dict.candidatesByPrefix(key, max) }
    }

    fun candidatesByPrefix(
        spec: DictionarySpec?,
        rawKey: String,
        limit: Int = 50,
    ): List<String> {
        return if (spec == null) emptyList() else candidatesByPrefix(listOf(spec), rawKey, limit)
    }

    fun create(specs: List<DictionarySpec>): Decoder {
        val supported = filterSupportedSpecs(specs)
        if (supported.isEmpty()) return PassthroughDecoder

        val dicts = supported.mapNotNull { spec -> loadDictionary(spec) }
        if (dicts.isEmpty()) return PassthroughDecoder

        val lookup = DictionaryLookup { searchKey, limit ->
            mergeCandidates(
                specs = supported,
                limit = limit,
            ) { dict, max -> dict.candidatesByPrefix(searchKey, max) }
        }

        return TokenPinyinDecoder(lookup)
    }

    fun create(spec: DictionarySpec?): Decoder {
        return create(listOfNotNull(spec))
    }

    private fun filterSupportedSpecs(specs: List<DictionarySpec>): List<DictionarySpec> {
        val enabled = specs.filter { it.enabled }
        val supported = enabled.filter { isSupportedScheme(it.codeScheme) }
        if (supported.isEmpty()) return emptyList()
        val scheme = supported.first().codeScheme
        return supported.filter { it.codeScheme == scheme }
    }

    private fun isSupportedScheme(scheme: String?): Boolean {
        val s = scheme?.trim().orEmpty()
        return s == "PINYIN_FULL" || s == "PINYIN_T9"
    }

    private fun loadDictionary(spec: DictionarySpec): MyBoardDictionary? {
        val assetPath = spec.assetPath?.trim().orEmpty()
        if (assetPath.isNotBlank()) {
            return try {
                dictCache.getOrPut("asset:$assetPath") {
                    MLog.d(logTag, "[$buildStamp] Loading MyBoardDictionary from assetPath=$assetPath")
                    MyBoardDictionary.fromAsset(context, assetPath)
                }
            } catch (t: Throwable) {
                MLog.e(logTag, "Failed to load MyBoardDictionary assetPath=$assetPath", t)
                null
            }
        }

        val filePath = spec.filePath?.trim().orEmpty()
        if (filePath.isNotBlank()) {
            return try {
                dictCache.getOrPut("file:$filePath") {
                    MLog.d(logTag, "[$buildStamp] Loading MyBoardDictionary from filePath=$filePath")
                    MyBoardDictionary.fromFile(java.io.File(filePath))
                }
            } catch (t: Throwable) {
                MLog.e(logTag, "Failed to load MyBoardDictionary filePath=$filePath", t)
                null
            }
        }

        MLog.w(logTag, "Dictionary missing assetPath/filePath dictionaryId=${spec.dictionaryId}")
        return null
    }

    private fun mergeCandidates(
        specs: List<DictionarySpec>,
        limit: Int,
        fetch: (MyBoardDictionary, Int) -> List<String>,
    ): List<String> {
        val out = ArrayList<String>(minOf(limit, 64))
        val seen = LinkedHashSet<String>(minOf(limit * 2, 128))
        for (spec in specs) {
            val dict = loadDictionary(spec) ?: continue
            val candidates = fetch(dict, limit)
            for (cand in candidates) {
                if (seen.add(cand)) {
                    out.add(cand)
                    if (out.size >= limit) return out
                }
            }
        }
        return out
    }
}
