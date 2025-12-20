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
        spec: DictionarySpec?,
        rawKey: String,
        limit: Int = 50,
    ): List<String> {
        if (spec == null) return emptyList()
        if (!spec.enabled) return emptyList()
        if (limit <= 0) return emptyList()

        val scheme = spec.codeScheme?.trim().orEmpty()
        if (scheme != "PINYIN_FULL" && scheme != "PINYIN_T9") return emptyList()

        val key = normalizePinyinKey(rawKey)
        if (key.isBlank()) return emptyList()

        val assetPath = spec.assetPath?.trim().orEmpty()
        if (assetPath.isBlank()) return emptyList()

        val dict = try {
            dictCache.getOrPut(assetPath) {
                MLog.d(logTag, "Loading MyBoardDictionary from assetPath=$assetPath (lookup-only)")
                MyBoardDictionary.fromAsset(context, assetPath)
            }
        } catch (t: Throwable) {
            MLog.e(logTag, "Failed to load MyBoardDictionary assetPath=$assetPath (lookup-only)", t)
            return emptyList()
        }

        return dict.candidatesByPrefix(key, limit)
    }

    fun create(spec: DictionarySpec?): Decoder {
        if (spec == null) return PassthroughDecoder
        if (!spec.enabled) return PassthroughDecoder

        val scheme = spec.codeScheme?.trim().orEmpty()

        // Unified pinyin decoder: drive by token input (qwerty letters + T9 symbol sets).
        if (scheme == "PINYIN_FULL" || scheme == "PINYIN_T9") {
            val assetPath = spec.assetPath?.trim().orEmpty()
            if (assetPath.isBlank()) {
                MLog.w(logTag, "$scheme missing assetPath; fallback to PassthroughDecoder")
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

            runCatching {
//                MLog.d(logTag, "[$buildStamp] smoke test begin scheme=$scheme")
//                val keys = listOf("a", "ao", "ni", "nihao", "ni hao", "zhong", "zhongguo", "wo", "women")
//                for (k in keys) {
//                    val outPrefix = dict.candidatesByPrefix(k, 5)
//                    val outExact = dict.candidates(k, 5)
//                    MLog.d(logTag, "smoke key='$k' exact=${outExact.take(5)} prefix=${outPrefix.take(5)}")
//                }
//                MLog.d(logTag, "[$buildStamp] smoke test end")
            }.onFailure { t ->
                MLog.w(logTag, "smoke test failed", t)
            }

            return TokenPinyinDecoder(lookup)
        }

        MLog.d(logTag, "No decoder mapping for codeScheme=$scheme dictionaryId=${spec.dictionaryId}; fallback to PassthroughDecoder")
        return PassthroughDecoder
    }
}
