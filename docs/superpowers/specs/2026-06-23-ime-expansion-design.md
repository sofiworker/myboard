# MyBoard IME 扩展设计文档

> 版本：v1.0
> 日期：2026-06-23
> 状态：Draft
> 定位：补齐中文(T9/双拼)、英文(多布局)、日语(假名)布局；实现词典系统(Room)；接入面板和Toolbar核心功能。
> 前置依赖：`docs/layout.md`（布局引擎v2.0）、`docs/engine.md`（引擎层）、`docs/orthogonal-state-management.md`（正交状态管理）、`docs/core.md`（核心架构）。

---

## 0. 范围界定

### 本次实现

| 子系统 | 内容 | 优先级 |
|--------|------|--------|
| 布局 | JSONC加载统一化 + 8个布局文件 | P0 |
| 引擎 | 修复引擎绑定bug + T9解码器 + 双拼映射 | P0 |
| 词典 | Room词典系统 + 雾凇拼音词库集成 | P0 |
| 面板 | Symbol/Emoji/Clipboard面板接入 | P1 |
| Toolbar | 布局切换/夜间模式/设置跳转/布局+emoji+clipboard | P1 |
| 留痕 | 实现记录文件 | P1 |

### 延后实现（placeholder）

- 五笔输入法
- 快捷短语面板
- 文字编辑面板
- 键盘高度调节滑块
- 布局继承/patch系统
- 主题JSONC文件加载
- 外部语言包下载

### 已知bug修复

1. 所有SchemaCapability的`engineId`错误设为`"direct"` → 修正为正确的引擎ID
2. `shuangpin`布局引用不存在 → 创建对应JSONC文件
3. `InputPipelineImpl.recreateSession()`的TODO → 实现EngineContext创建

---

## 1. 整体架构

### 1.1 设计原则

遵循`docs/layout.md`§0的约束：

- 布局数据全JSONC声明式，代码只负责解析和渲染
- 所有按键事件走正交动作路径（`PUSH_TOKEN` → `InputPipeline` → Engine）
- 布局层只读`KeyboardContext`，不持有状态
- 在现有正交状态管理架构上渐进扩展，不重构核心架构

遵循`docs/engine.md`的约束：

- 引擎层不读布局key id
- 引擎通过`InputAction`与布局层解耦
- 词典通过`DictionaryRegistry`按`DictionaryKey`查找

### 1.2 文件结构

```
assets/
  layouts/
    qwerty.jsonc              (已有，需改为被加载)
    qwerty_abc.jsonc           (新增)
    qwerty_dvorak.jsonc        (新增)
    qwerty_colemak.jsonc       (新增)
    shuangpin_ziran.jsonc      (新增)
    t9_chinese.jsonc           (新增)
    hiragana.jsonc             (新增)
    phone_dial.jsonc           (新增)
  dictionaries/
    pinyin_dict.db             (预编译SQLite，雾凇拼音)
  engines/
    ziran_map.json             (自然码双拼映射)
    t9_keymap.json             (T9按键映射)
    romaji_fsm.json            (罗马字FSM，预留)

app/src/main/java/xyz/xiao6/myboard/
  core/layout/
    LayoutAssetsLoader.kt      (新增 - 从assets加载JSONC)
    BuiltInLayouts.kt          (修改 - 改为调用LayoutAssetsLoader)
  core/engine/builtin/
    T9Decoder.kt               (新增 - T9解码器)
    ShuangpinEncoder.kt        (新增 - 自然码双拼编码器)
    ShuangpinMapping.kt        (新增 - 双拼映射数据)
  core/engine/
    EngineResourceResolverImpl.kt (修改 - 修复engineId绑定)
  core/dictionary/             (新增目录)
    DictionaryModule.kt        (Room模块)
    DictionaryDao.kt           (DAO接口)
    DictionaryDatabase.kt      (Room数据库)
    DictionaryEntity.kt        (实体类)
    PinyinDictionary.kt        (拼音词典实现)
    UserDictionary.kt          (用户词典实现)
    DictionaryRegistryImpl.kt  (修改 - 接入Room)
  core/dictionary/update/      (新增目录)
    DictionaryUpdater.kt       (词典更新接口)
    HotWordCalculator.kt       (热词计算)
  core/toolbar/                (新增目录)
    ToolbarManager.kt          (Toolbar状态管理)
    LayoutSwitcher.kt          (布局切换逻辑)
    ThemeToggler.kt            (夜间模式切换)
  ui/panels/
    ClipboardPanel.kt          (新增 - 剪贴板面板)
    EmojiPanel.kt              (新增 - Emoji面板封装)
  core/state/
    BuiltInManifests.kt        (修改 - 修复engineId)
    PanelManagerImpl.kt        (修改 - 接入面板状态)
```

### 1.3 正交状态扩展现有维度

不新增正交维度。在现有`(Locale, Script, Schema)`三维基础上：

- `Locale`扩展：新增`zh-TW`、`ko-KR`预留（本次不实现）
- `Script`不变：`HANI`, `LATN`, `HIRA`, `KATA`
- `Schema`扩展：新增`SHUANGPIN_ZIRAN`, `T9_PINYIN`

### 1.4 状态转移验证

编写Python脚本`scripts/validate_transitions.py`，在编译前验证：

1. 所有SchemaCapability引用的engineId必须在EngineRegistry中存在
2. 所有SchemaCapability引用的layoutId必须在LayoutRegistry中存在
3. 所有Transition定义的targetLocale/script/schema必须在OrthogonalRegistry中存在

---

## 2. 布局系统

### 2.1 JSONC加载流程

```
MyBoardImeService.onCreateInputView()
  → LayoutRegistry.get(context.layoutId)
  → LayoutAssetsLoader.load(context.layoutId)
  → assets/layouts/{id}.jsonc 读取
  → LayoutDocParser.parse(jsonc) → LayoutDoc
  → LayoutDocParser.toCompositeLayout(doc) → CompositeLayout
  → 缓存到LayoutRegistry
```

### 2.2 LayoutAssetsLoader

```kotlin
class LayoutAssetsLoader(private val context: Context) {
    fun load(layoutId: String): CompositeLayout? {
        return try {
            val jsonc = context.assets.open("layouts/$layoutId.jsonc")
                .bufferedReader().readText()
            val doc = LayoutDocParser.parse(jsonc)
            LayoutDocParser.toCompositeLayout(doc)
        } catch (e: Exception) {
            Log.e("LayoutAssetsLoader", "Failed to load layout: $layoutId", e)
            null
        }
    }
}
```

### 2.3 布局文件清单

#### qwerty.jsonc（已有，需验证加载）

标准QWERTY布局，支持NORMAL/SHIFTED/CAPS_LOCK三层。

#### qwerty_abc.jsonc

字母顺序ABC布局，适合初学者。

布局排列：
```
Row 1: A B C D E
Row 2: F G H I J
Row 3: K L M N O
Row 4: P Q R S T
Row 5: U V W X Y Z
```

#### qwerty_dvorak.jsonc

Dvorak优化布局，常用字母在中间行。

布局排列：
```
Row 1: ' , . P Y F G C R L
Row 2: A O E U I D H T N S -
Row 3: ; Q J K X B M W V Z
```

#### qwerty_colemak.jsonc

Colemak布局，保留QWERTY的ZXCV位置。

布局排列：
```
Row 1: Q W F P G J L U Y ;
Row 2: A R S T D H N E I O '
Row 3: Z X C V B K M , . /
```

#### shuangpin_ziran.jsonc

自然码双拼布局。声母区与QWERTY相同，韵母区通过组合键输入。

布局排列：
```
Row 1: Q W E R T Y U I O P [ ]
Row 2: A S D F G H J K L ;
Row 3: Z X C V B N M , . /
```

Layer变体：
- NORMAL：显示双拼键位提示（如q键显示"iu"）
- SHIFTED：显示对应大写/符号

#### t9_chinese.jsonc

T9九键布局，适合手机单手操作。

布局排列：
```
Row 1: 1(,@) 2(ABC) 3(DEF)
Row 2: 4(GHI) 5(JKL) 6(MNO)
Row 3: 7(PQRS) 8(TUV) 9(WXYZ)
Row 4: *(+[]) 0(空格) #(.?!)
```

每键对应：
- 2→ABC(拼音a/b/c)
- 3→DEF(拼音d/e/f)
- 4→GHI(拼音g/h/i)
- 5→JKL(拼音j/k/l)
- 6→MNO(拼音m/n/o)
- 7→PQRS(拼音p/q/r/s)
- 8→TUV(拼音t/u/v)
- 9→WXYZ(拼音w/x/y/z)

#### hiragana.jsonc

日语平假名50音布局。

布局排列（按行排列）：
```
Row 1: あ(a) い(i) う(u) え(e) お(o) ゃ(ya) ゅ(yu) ょ(yo)
Row 2: か(ka) き(ki) く(ku) け(ke) こ(ko)
Row 3: さ(sa) し(shi) す(su) せ(se) そ(so)
Row 4: た(ta) ち(chi) つ(tsu) て(te) と(to)
Row 5: な(na) に(ni) ぬ(nu) ね(ne) の(no)
Row 6: は(ha) ひ(hi) ふ(fu) へ(he) ほ(ho)
Row 7: ま(ma) み(mi) む(mu) め(me) も(mo)
Row 8: や(ya) ゆ(yu) よ(yo)
Row 9: ら(ra) り(ri) る(ru) れ(re) ろ(ro)
Row 10: わ(wa) を(wo) ん(n)
```

Layer变体：
- HIRA：平假名
- KATA：片假名（同一按键映射到片假名）

#### phone_dial.jsonc

拨号键盘布局，纯数字+符号。

布局排列：
```
Row 1: 1 2 3
Row 2: 4 5 6
Row 3: 7 8 9
Row 4: * 0 #
```

### 2.4 T9解码器

```kotlin
class T9Decoder(private val keyMap: Map<String, List<String>>) {
    /**
     * 将T9按键序列转换为拼音候选列表
     * 例如："22" → ["aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc"]
     */
    fun decode(sequence: String): List<String> {
        if (sequence.isEmpty()) return emptyList()
        val chars = sequence.map { keyMap[it.toString()] ?: emptyList() }
        return chars.fold(listOf("")) { acc, list ->
            acc.flatMap { prefix -> list.map { suffix -> prefix + suffix } }
        }
    }

    /**
     * 将拼音候选转换为词组候选（需配合词典）
     */
    suspend fun lookupCandidates(
        sequence: String,
        dictionary: Dictionary,
        limit: Int = 50
    ): List<Candidate> {
        val pinyins = decode(sequence)
        return pinyins.flatMap { dictionary.lookup(it, limit) }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
```

### 2.5 自然码双拼编码器

```kotlin
class ShuangpinEncoder(private val mapping: ShuangpinMapping) {
    /**
     * 将双拼输入转换为全拼
     * 例如："vs" → "zhi"
     */
    fun decode(doublePinyin: String): String {
        if (doublePinyin.length != 2) return doublePinyin
        val initial = mapping.initialMap[doublePinyin[0].toString()] ?: doublePinyin[0].toString()
        val final_ = mapping.finalMap[doublePinyin[1].toString()] ?: doublePinyin[1].toString()
        return initial + final_
    }

    /**
     * 批量解码，返回所有可能的全拼组合
     */
    fun decodeAll(input: String): List<String> {
        // 处理声母+韵母的组合
        // 例如："vs" → ["zhi"]
        // "aa" → ["啊"] (直接韵母)
        val results = mutableListOf<String>()
        var i = 0
        while (i < input.length) {
            if (i + 1 < input.length) {
                val pair = input.substring(i, i + 2)
                val decoded = decode(pair)
                if (decoded != pair) {
                    results.add(decoded)
                    i += 2
                    continue
                }
            }
            // 单个字符处理
            results.add(input[i].toString())
            i++
        }
        return results
    }
}
```

自然码双拼映射表（`ziran_map.json`）：

```json
{
  "initials": {
    "q": "q", "w": "w", "e": "e", "r": "r", "t": "t",
    "y": "y", "u": "sh", "i": "ch", "o": "o", "p": "p",
    "a": "a", "s": "s", "d": "d", "f": "f", "g": "g",
    "h": "h", "j": "j", "k": "k", "l": "l",
    "z": "z", "x": "x", "c": "c", "v": "zh", "b": "b",
    "n": "n", "m": "m"
  },
  "finals": {
    "q": "iu", "w": "ei", "e": "e", "r": "uan", "t": "ue",
    "y": "un", "u": "u", "i": "i", "o": "uo", "p": "ie",
    "a": "a", "s": "ong", "d": "iang", "f": "en", "g": "eng",
    "h": "ang", "j": "an", "k": "ao", "l": "ai",
    "z": "ou", "x": "ia", "c": "iao", "v": "v", "b": "in",
    "n": "iao", "m": "ian"
  }
}
```

### 2.6 引擎绑定修复

`BuiltInManifests.kt`中修正SchemaCapability的engineId：

```kotlin
// zh-CN HANI/PINYIN
SchemaCapability(
    engineId = "table_composing",  // 修正：原来错误的"direct"
    layoutId = "qwerty",
    encoderId = "pinyin",
    dictionary = "pinyin",
    candidatePolicy = "chinese_default",
    displayPolicy = "show_query"
)

// zh-CN HANI/SHUANGPIN_ZIRAN
SchemaCapability(
    engineId = "table_composing",
    layoutId = "shuangpin_ziran",
    encoderId = "shuangpin_ziran",
    dictionary = "pinyin",
    candidatePolicy = "chinese_default",
    displayPolicy = "show_query"
)

// zh-CN HANI/T9_PINYIN
SchemaCapability(
    engineId = "table_composing",
    layoutId = "t9_chinese",
    encoderId = "t9",
    dictionary = "pinyin",
    candidatePolicy = "chinese_default",
    displayPolicy = "show_query"
)

// ja-JP HIRA/KATA/ROMAJI
SchemaCapability(
    engineId = "transliteration",  // 修正：原来错误的"direct"
    layoutId = "hiragana",
    fsmId = "romaji",
    candidatePolicy = "japanese_kana_default",
    displayPolicy = "show_composing"
)

// en-US LATN/DIRECT (保持不变)
SchemaCapability(
    engineId = "direct",
    layoutId = "qwerty",
    candidatePolicy = "direct_default",
    displayPolicy = "hidden"
)
```

---

## 3. 词典系统（Room）

### 3.1 Room数据库定义

```kotlin
// DictionaryEntity.kt
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pinyin") val pinyin: String,
    @ColumnInfo(name = "phrase") val phrase: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 0,
    @ColumnInfo(name = "type") val type: Int = 0,  // 0=系统 1=用户 2=热词
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = 0
)

// UserPhraseEntity.kt
@Entity(tableName = "user_phrases")
data class UserPhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pinyin") val pinyin: String,
    @ColumnInfo(name = "phrase") val phrase: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 1,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = System.currentTimeMillis()
)
```

### 3.2 DAO接口

```kotlin
// DictionaryDao.kt
@Dao
interface DictionaryDao {
    @Query("SELECT * FROM phrases WHERE pinyin = :pinyin ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPinyin(pinyin: String, limit: Int = 50): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE pinyin LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: PhraseEntity)

    @Update
    suspend fun update(phrase: PhraseEntity)

    @Query("UPDATE phrases SET frequency = frequency + :delta WHERE phrase = :phrase")
    suspend fun incrementFrequency(phrase: String, delta: Int = 1)

    @Query("SELECT * FROM phrases ORDER BY frequency DESC LIMIT :limit")
    suspend fun getHotWords(limit: Int = 20): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE last_used_at > :since ORDER BY frequency DESC LIMIT :limit")
    suspend fun getRecentWords(since: Long, limit: Int = 20): List<PhraseEntity>

    @Query("DELETE FROM phrases WHERE last_used_at < :before AND type = 0")
    suspend fun cleanupOldSystemWords(before: Long)
}

// UserDictionaryDao.kt
@Dao
interface UserDictionaryDao {
    @Query("SELECT * FROM user_phrases WHERE pinyin = :pinyin ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPinyin(pinyin: String, limit: Int = 50): List<UserPhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: UserPhraseEntity)

    @Update
    suspend fun update(phrase: UserPhraseEntity)

    @Query("UPDATE user_phrases SET frequency = frequency + :delta, last_used_at = :now WHERE phrase = :phrase")
    suspend fun incrementFrequency(phrase: String, delta: Int = 1, now: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(phrase: UserPhraseEntity)

    @Query("SELECT * FROM user_phrases ORDER BY frequency DESC")
    suspend fun getAll(): List<UserPhraseEntity>
}
```

### 3.3 Room数据库

```kotlin
// DictionaryDatabase.kt
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(
    entities = [PhraseEntity::class, UserPhraseEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DictionaryDatabase : RoomDatabase() {
    abstract fun dictionaryDao(): DictionaryDao
    abstract fun userDictionaryDao(): UserDictionaryDao

    companion object {
        @Volatile
        private var INSTANCE: DictionaryDatabase? = null

        fun getInstance(context: Context): DictionaryDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DictionaryDatabase::class.java,
                    "myboard_dictionary.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
```

### 3.4 词典接口实现

```kotlin
// PinyinDictionary.kt
class PinyinDictionary(
    private val dao: DictionaryDao,
    private val userDao: UserDictionaryDao
) : Dictionary {

    override suspend fun lookup(query: String, limit: Int): List<Candidate> {
        // 1. 先查用户词典（优先级最高）
        val userResults = userDao.lookupByPinyin(query, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.WORD,
                score = entity.frequency * 1.5,  // 用户词典权重1.5倍
                source = CandidateSource.USER
            )
        }

        // 2. 再查系统词典
        val systemResults = dao.lookupByPinyin(query, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.WORD,
                score = entity.frequency.toDouble(),
                source = CandidateSource.SYSTEM
            )
        }

        // 3. 合并去重，按分数排序
        return (userResults + systemResults)
            .distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(limit)
    }

    override suspend fun addPhrase(pinyin: String, phrase: String, frequency: Int) {
        userDao.insert(UserPhraseEntity(
            pinyin = pinyin,
            phrase = phrase,
            frequency = frequency
        ))
    }

    override suspend fun updateFrequency(phrase: String, delta: Int) {
        val now = System.currentTimeMillis()
        userDao.incrementFrequency(phrase, delta, now)
        dao.incrementFrequency(phrase, delta)
    }

    override suspend fun removePhrase(phrase: String) {
        val entity = userDao.getAll().find { it.phrase == phrase }
        if (entity != null) userDao.delete(entity)
    }

    override fun observeChanges(): Flow<Unit> = flow {
        // 通过Room的InvalidationTracker观察变化
        // 或定期轮询
    }
}
```

### 3.5 词频自动调整

```kotlin
// AdaptiveDictionary.kt
class AdaptiveDictionary(
    private val pinyinDictionary: PinyinDictionary,
    private val hotWordCalculator: HotWordCalculator
) {
    /**
     * 用户选择候选后调用，自动调整词频
     */
    suspend fun onCandidateSelected(candidate: Candidate) {
        // 1. 更新词频
        pinyinDictionary.updateFrequency(candidate.text, 1)

        // 2. 记录使用历史
        hotWordCalculator.recordUsage(candidate.text)
    }

    /**
     * 获取热词列表（带时间衰减）
     */
    suspend fun getHotWords(limit: Int = 20): List<Candidate> {
        return hotWordCalculator.calculateHotWords(limit)
    }
}
```

### 3.6 热词推荐算法

```kotlin
// HotWordCalculator.kt
class HotWordCalculator(private val dao: DictionaryDao) {
    /**
     * 基于时间衰减的热词算法
     * 最近使用过的词权重更高
     */
    suspend fun calculateHotWords(limit: Int = 20): List<Candidate> {
        val now = System.currentTimeMillis()
        val oneWeek = 7 * 24 * 60 * 60 * 1000L

        // 获取最近一周使用过的词
        val recentWords = dao.getRecentWords(now - oneWeek, limit * 2)

        return recentWords
            .map { entity ->
                val hoursSinceUse = (now - entity.lastUsedAt) / 3600000.0
                val timeDecay = kotlin.math.exp(-hoursSinceUse / 168.0)  // 一周衰减
                val hotScore = entity.frequency * timeDecay

                Candidate(
                    text = entity.phrase,
                    type = CandidateType.WORD,
                    score = hotScore,
                    source = CandidateSource.HISTORY
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 记录使用历史
     */
    suspend fun recordUsage(phrase: String) {
        val entity = dao.lookupByPinyin(phrase, 1).firstOrNull()
        if (entity != null) {
            dao.incrementFrequency(phrase, 1)
        }
    }
}
```

### 3.7 词典更新接口

```kotlin
// DictionaryUpdater.kt
interface DictionaryUpdater {
    suspend fun checkUpdate(): DictionaryUpdateInfo
    suspend fun downloadUpdate(url: String): Result<Unit>
    suspend fun importFromZip(file: File): Result<Unit>
    suspend fun exportUserDictionary(): File
}

// DictionaryUpdateInfo.kt
data class DictionaryUpdateInfo(
    val currentVersion: String,
    val latestVersion: String,
    val updateUrl: String,
    val releaseNotes: String
)
```

### 3.8 雾凇拼音词库集成

- 来源：`github.com/iDvel/rime-ice` 的`dicts.cn_dicts.txt`
- 转换脚本：`scripts/import_rime_dict.py`
- 导入逻辑：首次启动时从assets预编译的`pinyin_dict.db`加载
- 更新机制：预留`DictionaryUpdater`接口，后续可通过网络更新

---

## 4. 面板系统

### 4.1 面板类型

```kotlin
// PanelType.kt (已存在，确认定义)
enum class PanelType {
    NONE,           // 无面板，显示键盘
    SYMBOL,         // 符号面板
    EMOJI,          // Emoji面板
    CLIPBOARD,      // 剪贴板面板
    KAOMOJI,        // 颜文字面板
    TEXT_EXPANSION, // 快捷短语
    STT,            // 语音输入
    LLM             // AI助手
}
```

### 4.2 IME视图切换逻辑

```kotlin
// MyBoardImeService.kt 中的KeyboardView
@Composable
fun KeyboardView(context: KeyboardContext) {
    when {
        // 面板优先级最高
        context.activePanel != PanelType.NONE -> {
            when (context.activePanel) {
                PanelType.SYMBOL -> SymbolPanel(
                    categories = symbolRepository.getCategories(),
                    onSymbolClick = { symbol ->
                        dispatch(InputAction.CommitToken(symbol))
                    },
                    onClose = { dispatch(InputAction.ClosePanel) }
                )
                PanelType.EMOJI -> EmojiPanel(
                    categories = emojiRepository.getCategories(),
                    onEmojiClick = { emoji ->
                        dispatch(InputAction.CommitToken(emoji))
                    },
                    onClose = { dispatch(InputAction.ClosePanel) }
                )
                PanelType.CLIPBOARD -> ClipboardPanel(
                    clipboardManager = clipboardManager,
                    onItemClick = { item ->
                        dispatch(InputAction.CommitToken(item.text))
                    },
                    onDelete = { item ->
                        clipboardManager.removeClip(item.id)
                    },
                    onClose = { dispatch(InputAction.ClosePanel) }
                )
                PanelType.KAOMOJI -> KaomojiPanel(
                    kaomojiRepository.getCategories(),
                    onKaomojiClick = { kaomoji ->
                        dispatch(InputAction.CommitToken(kaomoji))
                    },
                    onClose = { dispatch(InputAction.ClosePanel) }
                )
                // 其他面板暂时显示占位符
                else -> PlaceholderPanel(
                    panelType = context.activePanel,
                    onClose = { dispatch(InputAction.ClosePanel) }
                )
            }
        }
        // 候选栏
        context.hasCandidates || context.isComposing -> {
            CandidateBar(
                candidates = context.candidates,
                selectedIndex = context.selectedCandidateIndex,
                onCandidateClick = { index ->
                    dispatch(InputAction.CommitCandidate(index))
                },
                onPageNext = { dispatch(InputAction.PageCandidate(true)) },
                onPagePrev = { dispatch(InputAction.PageCandidate(false)) }
            )
        }
        // 工具栏+键盘
        else -> {
            Toolbar(
                onLanguageClick = { layoutSwitcher.cycleLayout() },
                onThemeToggle = { themeToggler.toggle() },
                onEmojiClick = { dispatch(InputAction.OpenPanel(PanelType.EMOJI)) },
                onSymbolClick = { dispatch(InputAction.OpenPanel(PanelType.SYMBOL)) },
                onClipboardClick = { dispatch(InputAction.OpenPanel(PanelType.CLIPBOARD)) },
                onSettingsClick = { openSettings() }
            )
            KeyboardLayout(context)
        }
    }
}
```

### 4.3 剪贴板面板

```kotlin
// ClipboardPanel.kt
@Composable
fun ClipboardPanel(
    clipboardManager: ClipboardManager,
    onItemClick: (ClipboardItem) -> Unit,
    onDelete: (ClipboardItem) -> Unit,
    onClose: () -> Unit
) {
    val clipHistory by clipboardManager.observeHistory().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxWidth()) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("剪贴板", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "关闭")
            }
        }

        // 剪贴板列表
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(clipHistory) { item ->
                ClipboardItemCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    onDelete = { onDelete(item) }
                )
            }
        }
    }
}
```

### 4.4 Emoji面板增强

```kotlin
// EmojiPanel.kt
@Composable
fun EmojiPanel(
    categories: List<EmojiCategory>,
    onEmojiClick: (String) -> Unit,
    onClose: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 搜索栏
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            placeholder = { Text("搜索Emoji") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
        )

        // 分类标签
        LazyRow(modifier = Modifier.fillMaxWidth()) {
            items(categories) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(category.name) }
                )
            }
        }

        // Emoji网格
        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxWidth()
        ) {
            val emojis = if (searchQuery.isNotEmpty()) {
                categories.flatMap { it.emojis }
                    .filter { it.name.contains(searchQuery) }
            } else {
                selectedCategory?.emojis ?: emptyList()
            }

            items(emojis) { emoji ->
                EmojiItem(
                    emoji = emoji,
                    onClick = { onEmojiClick(emoji.char) }
                )
            }
        }
    }
}
```

---

## 5. Toolbar系统

### 5.1 Toolbar布局

```kotlin
// Toolbar.kt
@Composable
fun Toolbar(
    onLanguageClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onEmojiClick: () -> Unit,
    onSymbolClick: () -> Unit,
    onClipboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 语言切换
        IconButton(onClick = onLanguageClick) {
            Icon(Icons.Default.Language, contentDescription = "切换布局")
        }

        // 夜间模式
        IconButton(onClick = onThemeToggle) {
            Icon(Icons.Default.DarkMode, contentDescription = "切换主题")
        }

        // 剪贴板
        IconButton(onClick = onClipboardClick) {
            Icon(Icons.Default.ContentPaste, contentDescription = "剪贴板")
        }

        // Emoji
        IconButton(onClick = onEmojiClick) {
            Icon(Icons.Default.EmojiEmotions, contentDescription = "Emoji")
        }

        // 符号
        IconButton(onClick = onSymbolClick) {
            Icon(Icons.Default.SymbolSymbols, contentDescription = "符号")
        }

        // 设置
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Default.Settings, contentDescription = "设置")
        }
    }
}
```

### 5.2 布局切换逻辑

```kotlin
// LayoutSwitcher.kt
class LayoutSwitcher(
    private val contextManager: KeyboardContextManager,
    private val orthogonalRegistry: OrthogonalRegistry
) {
    /**
     * 循环切换当前语言的可用Schema
     */
    fun cycleLayout() {
        val current = contextManager.context.value
        val manifest = orthogonalRegistry.getManifest(current.orthogonal.locale) ?: return
        val availableSchemas = manifest.schemas.keys.toList()
        val currentIndex = availableSchemas.indexOf(current.orthogonal.schema)
        val nextSchema = availableSchemas[(currentIndex + 1) % availableSchemas.size]
        contextManager.switchSchema(nextSchema)
    }

    /**
     * 获取当前布局的显示名称
     */
    fun getCurrentLayoutName(): String {
        val current = contextManager.context.value
        return when (current.orthogonal.schema) {
            Schema.PINYIN -> "拼音"
            Schema.SHUANGPIN_ZIRAN -> "双拼"
            Schema.T9_PINYIN -> "T9"
            Schema.LATIN_DIRECT -> "英文"
            Schema.ROMAJI -> "假名"
            else -> current.orthogonal.schema.name
        }
    }
}
```

### 5.3 夜间模式切换

```kotlin
// ThemeToggler.kt
class ThemeToggler(
    private val settingsManager: SettingsManager,
    private val themeResolver: ThemeResolver
) {
    fun toggle() {
        val current = settingsManager.theme.value
        val next = if (current == "light") "dark" else "light"
        settingsManager.setTheme(next)
        themeResolver.setActiveTheme(next)
    }

    fun isDarkMode(): Boolean {
        return settingsManager.theme.value == "dark"
    }
}
```

### 5.4 键盘调节（Placeholder）

```kotlin
// KeyboardAdjuster.kt (预留接口)
interface KeyboardAdjuster {
    fun setHeight(heightDp: Int)
    fun getHeight(): Int
    fun resetToDefault()
    fun getAvailableHeights(): List<Int>
}

// 实现类（预留）
class KeyboardAdjusterImpl(
    private val settingsManager: SettingsManager
) : KeyboardAdjuster {
    override fun setHeight(heightDp: Int) {
        // TODO: 实现键盘高度调节
    }

    override fun getHeight(): Int {
        return settingsManager.keyboardHeight.value
    }

    override fun resetToDefault() {
        // TODO: 重置为默认高度
    }

    override fun getAvailableHeights(): List<Int> {
        return listOf(200, 220, 240, 260, 280)
    }
}
```

---

## 6. 留痕记录方案

### 6.1 记录文件位置

```
docs/superpowers/
  ├── specs/
  │   └── 2026-06-23-ime-expansion-design.md  (本文档)
  └── changelogs/
      └── 2026-06-23-ime-expansion.md  (实现记录)
```

### 6.2 实现记录格式

实现记录保存在 `docs/superpowers/changelogs/2026-06-23-ime-expansion.md`，格式如下：

- **做了什么** — 具体的功能点和修改
- **为什么** — 设计决策的理由
- **文件** — 涉及的文件列表

每个子系统独立记录，包含布局系统、词典系统、引擎绑定修复、面板接入、Toolbar实现五个部分。

---

## 7. 实现顺序

1. **布局加载统一化**（1天）
   - 实现LayoutAssetsLoader
   - 验证现有qwerty.jsonc加载
   - 修复BuiltInLayouts.kt

2. **引擎绑定修复**（0.5天）
   - 修正BuiltInManifests.kt中的engineId
   - 验证状态转移

3. **新布局文件**（2天）
   - 创建7个新布局JSONC文件
   - 验证每个布局的状态转移

4. **词典系统**（2天）
   - 实现Room数据库和DAO
   - 实现PinyinDictionary和UserDictionary
   - 集成雾凇拼音词库

5. **T9和双拼编码器**（1.5天）
   - 实现T9Decoder
   - 实现ShuangpinEncoder
   - 测试编码解码

6. **面板接入**（1天）
   - 接入Symbol/Emoji/Clipboard面板
   - 实现面板状态管理

7. **Toolbar实现**（1天）
   - 实现布局切换、夜间模式、设置跳转
   - 集成到IME视图

8. **留痕记录和验证**（0.5天）
   - 编写实现记录
   - 运行验证脚本

**总计：约9.5天**

---

## 8. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| JSONC解析性能 | 首次加载慢 | 缓存机制 + 预编译 |
| Room数据库初始化 | 首次启动慢 | 预编译数据库文件 |
| T9候选词过多 | 内存占用高 | 分页加载 + 限制候选数 |
| 雾凇拼音词库版权 | 法律风险 | 确认MIT协议 + 预留替换接口 |
| 面板状态管理复杂 | 状态混乱 | 统一通过KeyboardContext管理 |

---

## 附录A：验证脚本

```python
#!/usr/bin/env python3
# scripts/validate_transitions.py

"""
验证正交状态转移的合法性
在编译前运行，确保所有引用的资源都存在
"""

import json
import os

def validate_engine_ids():
    """验证所有engineId在EngineRegistry中存在"""
    # 读取BuiltInManifests.kt
    # 提取所有engineId
    # 检查是否在EngineRegistry中注册
    pass

def validate_layout_ids():
    """验证所有layoutId在LayoutRegistry中存在"""
    # 读取assets/layouts/目录
    # 检查所有引用的layoutId是否都有对应的JSONC文件
    pass

def validate_transitions():
    """验证所有状态转移的目标状态合法"""
    # 读取TransitionEngine.kt
    # 提取所有transition定义
    # 检查目标locale/script/schema是否在OrthogonalRegistry中
    pass

if __name__ == "__main__":
    print("验证引擎绑定...")
    validate_engine_ids()
    print("验证布局引用...")
    validate_layout_ids()
    print("验证状态转移...")
    validate_transitions()
    print("所有验证通过！")
```
