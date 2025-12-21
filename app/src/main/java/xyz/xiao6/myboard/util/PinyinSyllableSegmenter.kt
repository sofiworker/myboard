package xyz.xiao6.myboard.util

/**
 * Heuristic pinyin syllable segmenter for display only.
 *
 * Input is expected to be plain a-z (e.g. "nihao"), output is a list of syllables (e.g. ["ni","hao"]).
 * If segmentation fails (e.g. incomplete syllable), returns a single-item list with the original token.
 */
object PinyinSyllableSegmenter {
    private val syllables: Set<String> = buildSyllableSet()
    private const val MAX_SYLLABLE_LEN = 6

    fun segmentToList(text: String): List<String> {
        val raw = text.trim()
        if (raw.isBlank()) return emptyList()

        val parts = raw.split('\'', ' ').map { it.trim() }.filter { it.isNotBlank() }
        if (parts.isEmpty()) return emptyList()

        val out = ArrayList<String>(parts.size * 2)
        for (part in parts) {
            val token = part.lowercase().filter { it in 'a'..'z' }
            if (token.isBlank()) continue
            val segmented = segmentToken(token)
            if (segmented.isEmpty()) out.add(token) else out.addAll(segmented)
        }
        return out
    }

    fun segmentForDisplay(text: String): String {
        val segments = segmentToList(text)
        return if (segments.isEmpty()) "" else segments.joinToString("'")
    }

    private fun segmentToken(token: String): List<String> {
        if (token.length <= 1) return emptyList()
        val n = token.length
        val best = arrayOfNulls<Seg>(n + 1)
        best[n] = Seg(emptyList(), segCount = 0, sumSquares = 0, penalty = 0)

        for (i in n - 1 downTo 0) {
            var bestHere: Seg? = null
            val maxLen = minOf(MAX_SYLLABLE_LEN, n - i)
            for (len in 1..maxLen) {
                val j = i + len
                val piece = token.substring(i, j)
                if (piece !in syllables) continue
                val tail = best[j] ?: continue
                val cand =
                    Seg(
                        segments = listOf(piece) + tail.segments,
                        segCount = 1 + tail.segCount,
                        sumSquares = len * len + tail.sumSquares,
                        penalty = tail.penalty,
                    )
                if (bestHere == null || cand.isBetterThan(bestHere)) bestHere = cand
            }
            val tail = best[i + 1] ?: continue
            val fallback =
                Seg(
                    segments = listOf(token.substring(i, i + 1)) + tail.segments,
                    segCount = 1 + tail.segCount,
                    sumSquares = 1 + tail.sumSquares,
                    penalty = tail.penalty + 5,
                )
            if (bestHere == null || fallback.isBetterThan(bestHere)) bestHere = fallback
            best[i] = bestHere
        }

        val res = best[0]?.segments ?: return emptyList()
        return if (res.size <= 1) emptyList() else res
    }

    private data class Seg(
        val segments: List<String>,
        val segCount: Int,
        val sumSquares: Int,
        val penalty: Int,
    ) {
        fun isBetterThan(other: Seg): Boolean {
            return when {
                penalty != other.penalty -> penalty < other.penalty
                segCount != other.segCount -> segCount < other.segCount
                sumSquares != other.sumSquares -> sumSquares > other.sumSquares
                else -> segments.size < other.segments.size
            }
        }
    }

    private fun buildSyllableSet(): Set<String> {
        val base = LinkedHashSet<String>(520)
        base += listOf(
            "a", "o", "e", "ai", "ei", "ao", "ou",
            "an", "en", "ang", "eng", "er",
        )

        base += listOf(
            "ba", "bo", "bai", "bei", "bao", "ban", "ben", "bang", "beng", "bi", "bie", "biao", "bian", "bin", "bing", "bu",
            "pa", "po", "pai", "pei", "pao", "pou", "pan", "pen", "pang", "peng", "pi", "pie", "piao", "pian", "pin", "ping", "pu",
            "ma", "mo", "me", "mai", "mei", "mao", "mou", "man", "men", "mang", "meng", "mi", "mie", "miao", "mian", "min", "ming", "mu",
            "fa", "fo", "fei", "fou", "fan", "fen", "fang", "feng", "fu",
            "da", "de", "dai", "dei", "dao", "dou", "dan", "den", "dang", "deng", "dong", "di", "die", "diao", "dian", "ding", "du", "duo", "dui", "duan", "dun",
            "ta", "te", "tai", "tao", "tou", "tan", "tang", "teng", "tong", "ti", "tie", "tiao", "tian", "ting", "tu", "tuo", "tui", "tuan", "tun",
            "na", "ne", "nai", "nei", "nao", "nou", "nan", "nen", "nang", "neng", "nong", "ni", "nie", "niao", "nian", "nin", "ning", "nu", "nuo", "nuan", "nun", "nv", "nve",
            "la", "le", "lai", "lei", "lao", "lou", "lan", "lang", "leng", "long", "li", "lie", "liao", "lian", "lin", "ling", "lu", "luo", "luan", "lun", "lv", "lve",
            "ga", "ge", "gai", "gei", "gao", "gou", "gan", "gen", "gang", "geng", "gong", "gu", "gua", "guo", "guai", "gui", "guan", "gun", "guang",
            "ka", "ke", "kai", "kao", "kou", "kan", "ken", "kang", "keng", "kong", "ku", "kua", "kuo", "kuai", "kui", "kuan", "kun", "kuang",
            "ha", "he", "hai", "hei", "hao", "hou", "han", "hen", "hang", "heng", "hong", "hu", "hua", "huo", "huai", "hui", "huan", "hun", "huang",
            "ji", "jia", "jie", "jiao", "jiu", "jian", "jin", "jiang", "jing", "jiong", "ju", "jue", "juan", "jun",
            "qi", "qia", "qie", "qiao", "qiu", "qian", "qin", "qiang", "qing", "qiong", "qu", "que", "quan", "qun",
            "xi", "xia", "xie", "xiao", "xiu", "xian", "xin", "xiang", "xing", "xiong", "xu", "xue", "xuan", "xun",
            "zha", "zhe", "zhai", "zhei", "zhao", "zhou", "zhan", "zhen", "zhang", "zheng", "zhong", "zhi", "zhu", "zhua", "zhuo", "zhuai", "zhui", "zhuan", "zhun", "zhuang",
            "cha", "che", "chai", "chao", "chou", "chan", "chen", "chang", "cheng", "chong", "chi", "chu", "chua", "chuo", "chuai", "chui", "chuan", "chun", "chuang",
            "sha", "she", "shai", "shei", "shao", "shou", "shan", "shen", "shang", "sheng", "shi", "shu", "shua", "shuo", "shuai", "shui", "shuan", "shun", "shuang",
            "ra", "re", "rao", "rou", "ran", "ren", "rang", "reng", "rong", "ri", "ru", "rua", "ruo", "rui", "ruan", "run",
            "za", "ze", "zai", "zei", "zao", "zou", "zan", "zen", "zang", "zeng", "zong", "zi", "zu", "zuo", "zui", "zuan", "zun",
            "ca", "ce", "cai", "cao", "cou", "can", "cen", "cang", "ceng", "cong", "ci", "cu", "cuo", "cui", "cuan", "cun",
            "sa", "se", "sai", "sao", "sou", "san", "sen", "sang", "seng", "song", "si", "su", "suo", "sui", "suan", "sun",
        )

        base += listOf(
            "ya", "yo", "yao", "ye", "you", "yan", "yin", "yang", "ying", "yong",
            "yi", "yu", "yue", "yuan", "yun", "yao", "yong",
            "wa", "wo", "wai", "wei", "wan", "wen", "wang", "weng",
        )

        base += listOf(
            "nv", "nve", "lv", "lve", "ju", "jue", "juan", "jun",
            "qu", "que", "quan", "qun", "xu", "xue", "xuan", "xun",
            "yu", "yue", "yuan", "yun",
        )

        return base.filter { it.length <= MAX_SYLLABLE_LEN }.toSet()
    }
}
