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
        best[n] = Seg(emptyList(), segCount = 0, sumSquares = 0)

        for (i in n - 1 downTo 0) {
            var bestHere: Seg? = null
            val maxLen = minOf(MAX_SYLLABLE_LEN, n - i)
            for (len in 1..maxLen) {
                val j = i + len
                val piece = token.substring(i, j)
                if (piece !in syllables) continue
                val tail = best[j] ?: continue
                val cand = Seg(listOf(piece) + tail.segments, segCount = 1 + tail.segCount, sumSquares = len * len + tail.sumSquares)
                if (bestHere == null || cand.isBetterThan(bestHere)) bestHere = cand
            }
            best[i] = bestHere
        }

        val res = best[0]?.segments ?: return emptyList()
        return if (res.size <= 1) emptyList() else res
    }

    private data class Seg(
        val segments: List<String>,
        val segCount: Int,
        val sumSquares: Int,
    ) {
        fun isBetterThan(other: Seg): Boolean {
            return when {
                segCount != other.segCount -> segCount < other.segCount
                sumSquares != other.sumSquares -> sumSquares > other.sumSquares
                else -> segments.size < other.segments.size
            }
        }
    }

    private fun buildSyllableSet(): Set<String> {
        val initials =
            listOf(
                "",
                "b", "p", "m", "f",
                "d", "t", "n", "l",
                "g", "k", "h",
                "j", "q", "x",
                "zh", "ch", "sh", "r",
                "z", "c", "s",
                "y", "w",
            )

        val finals =
            listOf(
                "a", "o", "e",
                "ai", "ei", "ao", "ou",
                "an", "en", "ang", "eng", "er", "ong",
                "i", "ia", "ie", "iao", "iu", "ian", "in", "iang", "ing", "iong",
                "u", "ua", "uo", "uai", "ui", "uan", "un", "uang", "ueng",
                "v", "ve", "van", "vn", "ue",
            )

        val standalone =
            listOf(
                "a", "o", "e", "ai", "ei", "ao", "ou", "an", "en", "ang", "eng", "er",
            )

        val set = LinkedHashSet<String>(600)
        set.addAll(standalone)
        for (initial in initials) {
            for (final in finals) {
                val syl = initial + final
                if (syl.length <= MAX_SYLLABLE_LEN) set += syl
            }
        }
        return set
    }
}

