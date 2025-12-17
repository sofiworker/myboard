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
 * - dict_pinyin + assetPath -> PinyinDictionaryDecoder(MyBoardDictionary)
 * - 其他 -> PassthroughDecoder（后续接入英文/笔画/手写等）
 */
class DecoderFactory(
    private val context: Context,
) {
    private val logTag = "DecoderFactory"
    private val buildStamp = "2025-12-14-prefix-v1"
    private val dictCache = ConcurrentHashMap<String, MyBoardDictionary>()

    fun create(spec: DictionarySpec?): Decoder {
        if (spec == null) return PassthroughDecoder
        if (!spec.enabled) return PassthroughDecoder

        val scheme = spec.codeScheme?.trim().orEmpty()

        // Minimal mapping for now (drive by canonical scheme, not by external source format).
        if (scheme == "PINYIN_FULL") {
            val assetPath = spec.assetPath?.trim().orEmpty()
            if (assetPath.isBlank()) {
                MLog.w(logTag, "PINYIN_FULL missing assetPath; fallback to PassthroughDecoder")
                return PassthroughDecoder
            }

            val dict = try {
                dictCache.getOrPut(assetPath) {
                    MLog.d(logTag, "[$buildStamp] Loading MyBoardDictionary from assetPath=$assetPath")
                    MyBoardDictionary.fromAsset(context, assetPath)
                }
            } catch (t: Throwable) {
                MLog.e(logTag, "Failed to load MyBoardDictionary assetPath=$assetPath; fallback to passthrough", t)
                return PassthroughDecoder
            }

            val lookup = DictionaryLookup { searchKey, limit ->
                val out = dict.candidatesByPrefix(searchKey, limit)
                MLog.d(logTag, "lookup(prefix) key='$searchKey' limit=$limit -> ${out.size} candidates")
                out
            }

            // Smoke test: verify some known keys can produce candidates (helps diagnose "no candidates" issues).
            runCatching {
                MLog.d(logTag, "[$buildStamp] smoke test begin")
                val keys = listOf("a", "ao", "ni", "nihao", "ni hao", "zhong", "zhongguo", "wo", "women")
                for (k in keys) {
                    val outPrefix = dict.candidatesByPrefix(k, 5)
                    val outExact = dict.candidates(k, 5)
                    MLog.d(logTag, "smoke key='$k' exact=${outExact.take(5)} prefix=${outPrefix.take(5)}")
                }
                MLog.d(logTag, "[$buildStamp] smoke test end")
            }.onFailure { t ->
                MLog.w(logTag, "smoke test failed", t)
            }
            return PinyinDictionaryDecoder(lookup)
        }

        if (scheme == "PINYIN_T9") {
            val assetPath = spec.assetPath?.trim().orEmpty()
            if (assetPath.isBlank()) {
                MLog.w(logTag, "PINYIN_T9 missing assetPath; fallback to PassthroughDecoder")
                return PassthroughDecoder
            }

            val dict = try {
                dictCache.getOrPut(assetPath) {
                    MLog.d(logTag, "[$buildStamp] Loading MyBoardDictionary from assetPath=$assetPath")
                    MyBoardDictionary.fromAsset(context, assetPath)
                }
            } catch (t: Throwable) {
                MLog.e(logTag, "Failed to load MyBoardDictionary assetPath=$assetPath; fallback to passthrough", t)
                return PassthroughDecoder
            }

            val lookup = DictionaryLookup { searchKey, limit ->
                dict.candidatesByPrefix(searchKey, limit)
            }
            return T9PinyinDecoder(lookup)
        }

        MLog.d(logTag, "No decoder mapping for codeScheme=$scheme dictionaryId=${spec.dictionaryId}; fallback to PassthroughDecoder")
        return PassthroughDecoder
    }
}
