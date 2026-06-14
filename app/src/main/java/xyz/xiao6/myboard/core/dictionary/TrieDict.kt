package xyz.xiao6.myboard.core.dictionary

/**
 * Trie 词典：高效前缀查询。
 */
class TrieDict {
    private class Node {
        var frequency: Long = 0
        val children = HashMap<Char, Node>()
        var isWord: Boolean = false
    }

    private val root = Node()

    fun insert(word: String, frequency: Long = 1) {
        var node = root
        for (ch in word.lowercase()) {
            node = node.children.getOrPut(ch) { Node() }
        }
        node.isWord = true
        node.frequency = maxOf(node.frequency, frequency)
    }

    fun prefixSearch(prefix: String, maxResults: Int = 20): List<DictEntry> {
        var node = root
        for (ch in prefix.lowercase()) {
            node = node.children[ch] ?: return emptyList()
        }
        return collectWords(node, prefix.lowercase(), maxResults)
    }

    fun fuzzySearch(word: String, maxDistance: Int = 2): List<FuzzyMatch> {
        val results = mutableListOf<FuzzyMatch>()
        val prevRow = IntArray(word.length + 1) { it }
        fuzzySearchHelper(root, word, 0, prevRow, maxDistance, "", results)
        return results.sortedBy { it.distance }
    }

    private fun collectWords(node: Node, prefix: String, max: Int): List<DictEntry> {
        val results = mutableListOf<DictEntry>()
        val stack = ArrayDeque<Pair<Node, String>>()
        stack.addLast(node to prefix)

        while (stack.isNotEmpty() && results.size < max) {
            val (current, word) = stack.removeLast()
            if (current.isWord) {
                results.add(DictEntry(word, current.frequency))
            }
            for ((ch, child) in current.children) {
                stack.addLast(child to word + ch)
            }
        }

        return results.sortedByDescending { it.frequency }
    }

    private fun fuzzySearchHelper(
        node: Node, target: String, targetIdx: Int,
        prevRow: IntArray, maxDist: Int, currentWord: String,
        results: MutableList<FuzzyMatch>
    ) {
        if (targetIdx > target.length) return

        val currentRow = IntArray(target.length + 1)
        currentRow[0] = targetIdx

        for (i in 1..target.length) {
            currentRow[i] = minOf(
                prevRow[i] + 1,
                currentRow[i - 1] + 1,
                prevRow[i - 1] + if (target[i - 1] == currentWord.lastOrNull()) 0 else 1
            )
        }

        if (targetIdx == target.length && node.isWord && currentRow[target.length] <= maxDist) {
            results.add(FuzzyMatch(currentWord, node.frequency, currentRow[target.length]))
        }

        for ((ch, child) in node.children) {
            fuzzySearchHelper(child, target, targetIdx + 1, currentRow, maxDist, currentWord + ch, results)
        }
    }
}

data class DictEntry(val word: String, val frequency: Long)
data class FuzzyMatch(val word: String, val frequency: Long, val distance: Int)
