# P3: 词典系统 (2 周)

## 1. 目标

实现 Trie 词典与联想引擎，支持英文前缀补全、用户词典、词频排序。

## 2. 里程碑验收标准

- [x] Trie 词典可加载英文词库
- [x] 输入前缀可联想单词
- [x] 用户输入新词可自动加入用户词典
- [x] 候选栏可展示联想结果
- [x] 点击候选可插入文本
- [x] 词典导入导出可用

## 3. 详细设计

### 3.1 Trie 词典

```kotlin
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
```

### 3.2 用户词典

```kotlin
@Dao
interface UserDictDao {
    @Query("SELECT * FROM user_words WHERE word LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun searchByPrefix(prefix: String, limit: Int = 20): List<UserWordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(word: UserWordEntity)

    @Query("UPDATE user_words SET frequency = frequency + 1, lastUsedAt = :now WHERE word = :word")
    suspend fun incrementFrequency(word: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM user_words ORDER BY lastUsedAt DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 50): List<UserWordEntity>
}

@Entity(tableName = "user_words")
data class UserWordEntity(
    @PrimaryKey val word: String,
    val frequency: Long = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis()
)
```

### 3.3 联想引擎

```kotlin
class SuggestionEngine @Inject constructor(
    private val primaryDict: TrieDict,
    private val userDict: UserDictDao,
    private val frequencyDict: FrequencyDict
) {
    suspend fun suggest(
        prefix: String,
        context: InputContext,
        maxResults: Int = 10
    ): List<Suggestion> = withContext(Dispatchers.Default) {
        val results = mutableListOf<Suggestion>()

        // 前缀匹配
        val prefixMatches = primaryDict.prefixSearch(prefix)
            .map { Suggestion(it.word, SuggestionType.WORD, it.frequency.toFloat(), DictSource.SYSTEM) }
        results.addAll(prefixMatches)

        // 用户词典
        val userMatches = userDict.searchByPrefix(prefix)
            .map { Suggestion(it.word, SuggestionType.WORD, it.frequency.toFloat() + 1000, DictSource.USER) }
        results.addAll(userMatches)

        // 上下文预测
        if (context.lastWord.isNotEmpty()) {
            val predictions = frequencyDict.getBigrams(context.lastWord)
                .filter { it.startsWith(prefix) }
                .map { Suggestion(it, SuggestionType.PREDICTION, 500f, DictSource.USER) }
            results.addAll(predictions)
        }

        results.distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    suspend fun recordWord(word: String) {
        val existing = userDict.searchByPrefix(word, 1).firstOrNull { it.word == word }
        if (existing != null) {
            userDict.incrementFrequency(word)
        } else {
            userDict.insert(UserWordEntity(word))
        }
    }
}

data class Suggestion(
    val text: String,
    val type: SuggestionType,
    val score: Float,
    val source: DictSource
)

enum class SuggestionType { WORD, PREFIX, PREDICTION, CORRECTION, LLM }
enum class DictSource { SYSTEM, USER, HISTORY, LLM }
```

### 3.4 词典导入导出

```kotlin
class DictionaryImporter @Inject constructor(
    private val primaryDict: TrieDict,
    private val userDict: UserDictDao
) {
    suspend fun importFromJson(context: Context, uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
        val entries = Json.decodeFromString<List<DictImportEntry>>(text)
        for (entry in entries) {
            primaryDict.insert(entry.word, entry.frequency)
        }
    }

    suspend fun exportToJson(context: Context, uri: Uri) {
        val entries = userDict.getRecent(10000).map {
            DictImportEntry(it.word, it.frequency)
        }
        val text = Json.encodeToString(entries)
        context.contentResolver.openOutputStream(uri)?.write(text.toByteArray())
    }
}

@Serializable
data class DictImportEntry(
    val word: String,
    val frequency: Long = 1
)
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `dictionary/trie/TrieDict.kt` | Trie 词典 |
| `dictionary/user/UserDictDao.kt` | 用户词典 DAO |
| `dictionary/user/UserWordEntity.kt` | 用户词典实体 |
| `dictionary/frequency/FrequencyDict.kt` | 词频词典 |
| `dictionary/suggestion/SuggestionEngine.kt` | 联想引擎 |
| `dictionary/import/DictionaryImporter.kt` | 词典导入导出 |
| `assets/dictionaries/en_primary.dict` | 英文词典 |
| `assets/dictionaries/en_frequency.dict` | 英文词频 |
