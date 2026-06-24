# MyBoard IME 扩展实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 补齐中文(T9/双拼)、英文(多布局)、日语(假名)布局；实现Room词典系统；接入面板和Toolbar核心功能。

**Architecture:** 在现有正交状态管理架构上渐进扩展：新增布局JSONC文件通过LayoutAssetsLoader统一加载；词典系统使用Room存储，实现PinyinDictionary接口；面板通过PanelType扩展接入IME视图；Toolbar增加布局切换、夜间模式、设置跳转功能。

**Tech Stack:** Kotlin, Jetpack Compose, Room, kotlinx.serialization, JSONC

---

## 文件结构总览

### 新建文件
- `core/layout/LayoutAssetsLoader.kt` — 从assets加载JSONC布局
- `core/engine/builtin/T9Decoder.kt` — T9九键解码器
- `core/engine/builtin/ShuangpinEncoder.kt` — 自然码双拼编码器
- `core/engine/builtin/ShuangpinMapping.kt` — 双拼映射数据
- `core/dictionary/DictionaryModule.kt` — Room数据库模块
- `core/dictionary/DictionaryDao.kt` — DAO接口
- `core/dictionary/UserDictionaryDao.kt` — 用户词典DAO
- `core/dictionary/PhraseEntity.kt` — 词条实体
- `core/dictionary/UserPhraseEntity.kt` — 用户词条实体
- `core/dictionary/DictionaryDatabase.kt` — Room数据库
- `core/dictionary/PinyinDictionary.kt` — 拼音词典实现
- `core/dictionary/UserDictionary.kt` — 用户词典实现
- `core/dictionary/AdaptiveDictionary.kt` — 词频自适应
- `core/dictionary/HotWordCalculator.kt` — 热词计算
- `core/dictionary/DictionaryUpdater.kt` — 词典更新接口
- `core/toolbar/LayoutSwitcher.kt` — 布局切换逻辑
- `core/toolbar/ThemeToggler.kt` — 夜间模式切换
- `ui/panels/EmojiPanel.kt` — Emoji面板
- `ui/panels/ClipboardPanel.kt` — 剪贴板面板
- `ui/panels/KaomojiPanel.kt` — 颜文字面板
- `ui/panels/PlaceholderPanel.kt` — 占位面板
- `assets/layouts/shuangpin_ziran.jsonc` — 自然码双拼布局
- `assets/layouts/t9_chinese.jsonc` — T9九键布局
- `assets/layouts/hiragana.jsonc` — 日语假名布局
- `assets/layouts/qwerty_dvorak.jsonc` — Dvorak布局
- `assets/layouts/qwerty_colemak.jsonc` — Colemak布局
- `assets/layouts/qwerty_abc.jsonc` — ABC布局
- `assets/layouts/phone_dial.jsonc` — 拨号键盘布局
- `assets/engines/ziran_map.json` — 自然码映射
- `assets/engines/t9_keymap.json` — T9按键映射
- `docs/superpowers/changelogs/2026-06-23-ime-expansion.md` — 留痕记录

### 修改文件
- `core/contract/Schema.kt` — 新增T9_PINYIN、SHUANGPIN_ZIRAN常量
- `core/contract/PanelType.kt` — 新增KAOMOJI、TEXT_EXPANSION
- `core/state/BuiltInManifests.kt` — 新增Schema、修复layoutId引用
- `core/layout/BuiltInLayouts.kt` — 改为调用LayoutAssetsLoader
- `core/layout/ActionDispatcher.kt` — 扩展parsePanel
- `core/engine/InputPipelineImpl.kt` — 实现recreateSession
- `core/settings/SettingsManager.kt` — 新增toggleTheme函数
- `ime/MyBoardImeService.kt` — 接入面板视图、增强Toolbar
- `app/build.gradle.kts` — 已有Room依赖，无需修改

---

## Task 1: 扩展 Schema 和 PanelType 常量

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/contract/Schema.kt:14-22`
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/contract/PanelType.kt:1-13`

- [ ] **Step 1: 新增 Schema 常量**

在 `BuiltInSchemas` 中添加 T9_PINYIN 和 SHUANGPIN_ZIRAN：

```kotlin
// Schema.kt - 在 BuiltInSchemas object 中追加
val T9_PINYIN = Schema("T9_PINYIN")
val SHUANGPIN_ZIRAN = Schema("SHUANGPIN_ZIRAN")
```

- [ ] **Step 2: 新增 PanelType**

```kotlin
// PanelType.kt - 完整替换
package xyz.xiao6.myboard.core.contract

enum class PanelType {
    NONE,
    EMOJI,
    SYMBOL,
    CLIPBOARD,
    LLM,
    STT,
    KAOMOJI,
    TEXT_EXPANSION
}
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/contract/Schema.kt app/src/main/java/xyz/xiao6/myboard/core/contract/PanelType.kt
git commit -m "feat: add T9_PINYIN, SHUANGPIN_ZIRAN schemas and KAOMOJI panel type"
```

---

## Task 2: 修复 BuiltInManifests — 新增 T9/双拼 Schema 并修正 layoutId

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/state/BuiltInManifests.kt`

- [ ] **Step 1: 修改 zhCN Manifest — 添加 T9 和自然码双拼 Schema，修正双拼 layoutId**

完整替换 `BuiltInManifests.kt`：

```kotlin
package xyz.xiao6.myboard.core.state

import xyz.xiao6.myboard.core.contract.*

object BuiltInManifests {

    val zhCN = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.zh-CN",
        packageVersion = "1.0.0",
        locale = LocaleTag("zh-CN"),
        displayName = mapOf(
            "zh-CN" to "中文",
            "en-US" to "Chinese"
        ),
        defaults = LocaleDefaults(
            script = Script.HANI,
            schema = BuiltInSchemas.PINYIN,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.HANI to ScriptManifest(
                defaultSchema = BuiltInSchemas.PINYIN,
                schemas = mapOf(
                    BuiltInSchemas.PINYIN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "qwerty",
                        supportsShift = false,
                        encoderId = "identity",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_query",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_pinyin")
                    ),
                    BuiltInSchemas.SHUANGPIN_ZIRAN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "shuangpin_ziran",
                        supportsShift = false,
                        encoderId = "shuangpin_ziran",
                        encoderConfig = "engines/ziran_map.json",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_query",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_shuangpin")
                    ),
                    BuiltInSchemas.T9_PINYIN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "t9_chinese",
                        supportsShift = false,
                        encoderId = "t9",
                        encoderConfig = "engines/t9_keymap.json",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_query",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_t9")
                    ),
                    BuiltInSchemas.DOUBLE_PINYIN to SchemaCapability(
                        engineId = "table_composing",
                        layoutId = "shuangpin_ziran",
                        supportsShift = false,
                        encoderId = "shuangpin_ziran",
                        encoderConfig = "engines/ziran_map.json",
                        dictionary = "dicts/pinyin_main.dict",
                        candidatePolicy = "chinese_default",
                        displayPolicy = "show_query",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_double_pinyin")
                    )
                )
            ),
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_zh_latin")
                    )
                )
            )
        )
    )

    val enUS = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.en-US",
        packageVersion = "1.0.0",
        locale = LocaleTag("en-US"),
        displayName = mapOf(
            "zh-CN" to "英语",
            "en-US" to "English"
        ),
        defaults = LocaleDefaults(
            script = Script.LATN,
            schema = BuiltInSchemas.LATIN_DIRECT,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_en_direct")
                    )
                )
            )
        )
    )

    val jaJP = LanguageManifest(
        manifestVersion = 1,
        packageId = "language.ja-JP",
        packageVersion = "1.0.0",
        locale = LocaleTag("ja-JP"),
        displayName = mapOf(
            "zh-CN" to "日语",
            "en-US" to "Japanese",
            "ja-JP" to "日本語"
        ),
        defaults = LocaleDefaults(
            script = Script.HIRA,
            schema = BuiltInSchemas.ROMAJI,
            layoutId = "qwerty"
        ),
        scripts = mapOf(
            Script.HIRA to ScriptManifest(
                defaultSchema = BuiltInSchemas.ROMAJI,
                schemas = mapOf(
                    BuiltInSchemas.ROMAJI to SchemaCapability(
                        engineId = "transliteration",
                        layoutId = "qwerty",
                        supportsShift = false,
                        fsm = "rules/romaji_hira.fsm.json",
                        candidatePolicy = "japanese_kana_default",
                        displayPolicy = "show_composing",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_romaji_hira")
                    )
                )
            ),
            Script.KATA to ScriptManifest(
                defaultSchema = BuiltInSchemas.ROMAJI,
                schemas = mapOf(
                    BuiltInSchemas.ROMAJI to SchemaCapability(
                        engineId = "transliteration",
                        layoutId = "qwerty",
                        supportsShift = false,
                        fsm = "rules/romaji_kata.fsm.json",
                        candidatePolicy = "japanese_kana_default",
                        displayPolicy = "show_composing",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_romaji_kata")
                    )
                )
            ),
            Script.LATN to ScriptManifest(
                defaultSchema = BuiltInSchemas.LATIN_DIRECT,
                schemas = mapOf(
                    BuiltInSchemas.LATIN_DIRECT to SchemaCapability(
                        engineId = "direct",
                        layoutId = "qwerty",
                        supportsShift = true,
                        mapping = "maps/latin_qwerty.json",
                        candidatePolicy = "direct_default",
                        displayPolicy = "hidden",
                        subtype = SubtypeInfo(labelKey = "subtype_ja_latin")
                    )
                )
            )
        )
    )

    val all: List<LanguageManifest> = listOf(zhCN, enUS, jaJP)
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/state/BuiltInManifests.kt
git commit -m "feat: add T9_PINYIN/SHUANGPIN_ZIRAN schemas, fix DOUBLE_PINYIN layoutId"
```

---

## Task 3: 扩展 ActionDispatcher — 支持新 PanelType

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/layout/ActionDispatcher.kt:121-131`

- [ ] **Step 1: 修改 parsePanel 方法**

```kotlin
// ActionDispatcher.kt - 替换 parsePanel 方法
private fun parsePanel(name: String): PanelType {
    return when (name.uppercase()) {
        "NONE" -> PanelType.NONE
        "EMOJI" -> PanelType.EMOJI
        "SYMBOL" -> PanelType.SYMBOL
        "CLIPBOARD" -> PanelType.CLIPBOARD
        "LLM" -> PanelType.LLM
        "STT" -> PanelType.STT
        "KAOMOJI" -> PanelType.KAOMOJI
        "TEXT_EXPANSION" -> PanelType.TEXT_EXPANSION
        else -> PanelType.NONE
    }
}
```

- [ ] **Step 2: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/layout/ActionDispatcher.kt
git commit -m "feat: extend ActionDispatcher parsePanel for KAOMOJI and TEXT_EXPANSION"
```

---

## Task 4: 创建自然码双拼映射和 T9 按键映射文件

**Files:**
- Create: `app/src/main/assets/engines/ziran_map.json`
- Create: `app/src/main/assets/engines/t9_keymap.json`

- [ ] **Step 1: 创建自然码双拼映射**

```json
// app/src/main/assets/engines/ziran_map.json
{
  "initials": {
    "q": "q", "w": "w", "e": "e", "r": "r", "t": "t",
    "y": "y", "u": "sh", "i": "ch", "o": "uo", "p": "p",
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
  },
  "zeroInitialFinals": {
    "a": "啊", "o": "哦", "e": "额", "ai": "爱", "ei": "诶",
    "ao": "奥", "ou": "欧", "an": "安", "en": "恩", "ang": "昂",
    "er": "儿", "yi": "一", "wu": "五", "yu": "鱼"
  }
}
```

- [ ] **Step 2: 创建 T9 按键映射**

```json
// app/src/main/assets/engines/t9_keymap.json
{
  "keyMap": {
    "2": ["a", "b", "c"],
    "3": ["d", "e", "f"],
    "4": ["g", "h", "i"],
    "5": ["j", "k", "l"],
    "6": ["m", "n", "o"],
    "7": ["p", "q", "r", "s"],
    "8": ["t", "u", "v"],
    "9": ["w", "x", "y", "z"]
  },
  "numberKeys": ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"]
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/engines/ziran_map.json app/src/main/assets/engines/t9_keymap.json
git commit -m "feat: add shuangpin_ziran mapping and T9 keymap assets"
```

---

## Task 5: 实现 LayoutAssetsLoader — 从 assets 加载 JSONC 布局

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/core/layout/LayoutAssetsLoader.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/layout/BuiltInLayouts.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ime/MyBoardImeService.kt:155-157`

- [ ] **Step 1: 创建 LayoutAssetsLoader**

```kotlin
package xyz.xiao6.myboard.core.layout

import android.content.Context
import android.util.Log
import xyz.xiao6.myboard.core.contract.LayoutDoc

/**
 * 从 assets/layouts/ 加载 JSONC 布局文件。
 * 优先从 assets 加载，失败时回退到 BuiltInLayouts 中的硬编码布局。
 */
class LayoutAssetsLoader(private val context: Context) {

    private val cache = mutableMapOf<String, LayoutDoc>()

    fun load(layoutId: String): LayoutDoc? {
        cache[layoutId]?.let { return it }

        // 尝试从 assets 加载 JSONC
        val fromAssets = loadFromAssets(layoutId)
        if (fromAssets != null) {
            cache[layoutId] = fromAssets
            return fromAssets
        }

        // 回退到硬编码布局
        val fromBuiltIn = BuiltInLayouts.byId(layoutId)
        if (fromBuiltIn != null) {
            cache[layoutId] = fromBuiltIn
        }
        return fromBuiltIn
    }

    private fun loadFromAssets(layoutId: String): LayoutDoc? {
        return try {
            val jsonc = context.assets.open("layouts/$layoutId.jsonc")
                .bufferedReader().use { it.readText() }
            val doc = LayoutDocParser.parse(jsonc)
            Log.d("LayoutAssetsLoader", "Loaded layout '$layoutId' from assets")
            doc
        } catch (e: Exception) {
            // 文件不存在或解析失败，静默返回 null
            null
        }
    }

    fun invalidateCache(layoutId: String? = null) {
        if (layoutId != null) {
            cache.remove(layoutId)
        } else {
            cache.clear()
        }
    }
}
```

- [ ] **Step 2: 修改 MyBoardImeService — 初始化 LayoutAssetsLoader 并注册布局**

修改 `MyBoardImeService.kt` 中的组件声明和 `registerBuiltIns()` 方法：

```kotlin
// MyBoardImeService.kt - 在组件声明区域添加
private lateinit var layoutAssetsLoader: LayoutAssetsLoader

// MyBoardImeService.kt - 在 initCoreComponents() 末尾添加
layoutAssetsLoader = LayoutAssetsLoader(this)

// MyBoardImeService.kt - 替换 registerBuiltIns() 中的布局注册部分
// 旧代码:
// val qwertyDoc = BuiltInLayouts.qwerty
// layoutRegistry.register(qwertyDoc, LayoutSource.BUILT_IN)

// 新代码:
val layoutIds = listOf("qwerty", "shuangpin_ziran", "t9_chinese", "hiragana",
    "qwerty_dvorak", "qwerty_colemak", "qwerty_abc", "phone_dial")
layoutIds.forEach { id ->
    val doc = layoutAssetsLoader.load(id)
    if (doc != null) {
        layoutRegistry.register(doc, LayoutSource.BUILT_IN)
    }
}

// MyBoardImeService.kt - 替换 onCreateInputView() 中的布局获取
// 旧代码:
// val layoutDoc = layoutRegistry.get(context.layoutId)

// 新代码:
val layoutDoc = layoutAssetsLoader.load(context.layoutId)
    ?: layoutRegistry.get(context.layoutId)
```

- [ ] **Step 3: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/layout/LayoutAssetsLoader.kt app/src/main/java/xyz/xiao6/myboard/ime/MyBoardImeService.kt
git commit -m "feat: add LayoutAssetsLoader for JSONC layout loading from assets"
```

---

## Task 6: 创建自然码双拼布局 JSONC

**Files:**
- Create: `app/src/main/assets/layouts/shuangpin_ziran.jsonc`

- [ ] **Step 1: 创建 shuangpin_ziran.jsonc**

```jsonc
// app/src/main/assets/layouts/shuangpin_ziran.jsonc
{
  "schemaVersion": 2,
  "id": "shuangpin_ziran",
  "meta": {
    "name": "自然码双拼",
    "description": "Ziran Shuangpin - Natural Code Double Pinyin"
  },
  "env": {
    "supportedSchemas": ["SHUANGPIN_ZIRAN", "DOUBLE_PINYIN"]
  },
  "root": {
    "id": "root",
    "type": "composite",
    "orientation": "VERTICAL",
    "regions": [
      {
        "id": "candidate_region",
        "role": "CANDIDATE",
        "container": {
          "id": "candidate_bar",
          "type": "linear",
          "orientation": "HORIZONTAL",
          "children": [],
          "height": { "dp": 40 }
        }
      },
      {
        "id": "keyboard_region",
        "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid",
          "type": "grid",
          "columns": 10,
          "cells": [
            // Row 1: Q W E R T Y U I O P
            { "key": "q", "col": 0, "row": 0, "content": { "label": "Q", "hint": { "TOP_CENTER": "iu" } } },
            { "key": "w", "col": 1, "row": 0, "content": { "label": "W", "hint": { "TOP_CENTER": "ei" } } },
            { "key": "e", "col": 2, "row": 0, "content": { "label": "E", "hint": { "TOP_CENTER": "e" } } },
            { "key": "r", "col": 3, "row": 0, "content": { "label": "R", "hint": { "TOP_CENTER": "uan" } } },
            { "key": "t", "col": 4, "row": 0, "content": { "label": "T", "hint": { "TOP_CENTER": "ue" } } },
            { "key": "y", "col": 5, "row": 0, "content": { "label": "Y", "hint": { "TOP_CENTER": "un" } } },
            { "key": "u", "col": 6, "row": 0, "content": { "label": "U", "hint": { "TOP_CENTER": "sh" } } },
            { "key": "i", "col": 7, "row": 0, "content": { "label": "I", "hint": { "TOP_CENTER": "ch" } } },
            { "key": "o", "col": 8, "row": 0, "content": { "label": "O", "hint": { "TOP_CENTER": "uo" } } },
            { "key": "p", "col": 9, "row": 0, "content": { "label": "P", "hint": { "TOP_CENTER": "ie" } } },
            // Row 2: A S D F G H J K L
            { "key": "a", "col": 0, "row": 1, "content": { "label": "A", "hint": { "TOP_CENTER": "a" } } },
            { "key": "s", "col": 1, "row": 1, "content": { "label": "S", "hint": { "TOP_CENTER": "ong" } } },
            { "key": "d", "col": 2, "row": 1, "content": { "label": "D", "hint": { "TOP_CENTER": "iang" } } },
            { "key": "f", "col": 3, "row": 1, "content": { "label": "F", "hint": { "TOP_CENTER": "en" } } },
            { "key": "g", "col": 4, "row": 1, "content": { "label": "G", "hint": { "TOP_CENTER": "eng" } } },
            { "key": "h", "col": 5, "row": 1, "content": { "label": "H", "hint": { "TOP_CENTER": "ang" } } },
            { "key": "j", "col": 6, "row": 1, "content": { "label": "J", "hint": { "TOP_CENTER": "an" } } },
            { "key": "k", "col": 7, "row": 1, "content": { "label": "K", "hint": { "TOP_CENTER": "ao" } } },
            { "key": "l", "col": 8, "row": 1, "content": { "label": "L", "hint": { "TOP_CENTER": "ai" } } },
            { "key": "enter", "col": 9, "row": 1, "styleRef": "key_action", "content": { "label": "Enter" }, "actions": { "gestures": { "TAP": { "actionType": "performEditorAction" } } } },
            // Row 3: Shift Z X C V B N M Backspace
            { "key": "shift", "col": 0, "row": 2, "colSpan": 1, "styleRef": "key_function", "content": { "icon": "shift" }, "actions": { "gestures": { "TAP": { "actionType": "cycleLayer", "payload": { "layers": ["NORMAL", "SHIFTED"] } } } } },
            { "key": "z", "col": 1, "row": 2, "content": { "label": "Z", "hint": { "TOP_CENTER": "ou" } } },
            { "key": "x", "col": 2, "row": 2, "content": { "label": "X", "hint": { "TOP_CENTER": "ia" } } },
            { "key": "c", "col": 3, "row": 2, "content": { "label": "C", "hint": { "TOP_CENTER": "iao" } } },
            { "key": "v", "col": 4, "row": 2, "content": { "label": "V", "hint": { "TOP_CENTER": "zh" } } },
            { "key": "b", "col": 5, "row": 2, "content": { "label": "B", "hint": { "TOP_CENTER": "in" } } },
            { "key": "n", "col": 6, "row": 2, "content": { "label": "N", "hint": { "TOP_CENTER": "iao" } } },
            { "key": "m", "col": 7, "row": 2, "content": { "label": "M", "hint": { "TOP_CENTER": "ian" } } },
            { "key": "backspace", "col": 8, "row": 2, "colSpan": 2, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } }, "LONG_PRESS": { "actionType": "delete", "payload": { "count": 10 } } } } },
            // Row 4: ?123 , Space . ?
            { "key": "num", "col": 0, "row": 3, "styleRef": "key_function", "content": { "label": "?123" }, "actions": { "gestures": { "TAP": { "actionType": "openPanel", "payload": { "panel": "SYMBOL" } } } } },
            { "key": "comma", "col": 1, "row": 3, "styleRef": "key_function", "content": { "label": "," }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "," } } } } },
            { "key": "space", "col": 2, "row": 3, "colSpan": 5, "styleRef": "key_space", "content": { "label": "Space" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } } },
            { "key": "period", "col": 7, "row": 3, "styleRef": "key_function", "content": { "label": "." }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "." } } } } },
            { "key": "sym", "col": 8, "row": 3, "colSpan": 2, "styleRef": "key_function", "content": { "label": "?123" }, "actions": { "gestures": { "TAP": { "actionType": "openPanel", "payload": { "panel": "SYMBOL" } } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/layouts/shuangpin_ziran.jsonc
git commit -m "feat: add shuangpin_ziran.jsonc layout with dual-pinyin hints"
```

---

## Task 7: 创建 T9 九键布局 JSONC

**Files:**
- Create: `app/src/main/assets/layouts/t9_chinese.jsonc`

- [ ] **Step 1: 创建 t9_chinese.jsonc**

```jsonc
// app/src/main/assets/layouts/t9_chinese.jsonc
{
  "schemaVersion": 2,
  "id": "t9_chinese",
  "meta": {
    "name": "T9九键",
    "description": "T9 nine-key Chinese input layout"
  },
  "env": {
    "supportedSchemas": ["T9_PINYIN"]
  },
  "root": {
    "id": "root",
    "type": "composite",
    "orientation": "VERTICAL",
    "regions": [
      {
        "id": "candidate_region",
        "role": "CANDIDATE",
        "container": {
          "id": "candidate_bar",
          "type": "linear",
          "orientation": "HORIZONTAL",
          "children": [],
          "height": { "dp": 40 }
        }
      },
      {
        "id": "keyboard_region",
        "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid",
          "type": "grid",
          "columns": 3,
          "cells": [
            { "key": "key_1", "col": 0, "row": 0, "content": { "label": "1", "hint": { "BOTTOM_CENTER": ",@" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "1" } } } } },
            { "key": "key_2", "col": 1, "row": 0, "content": { "label": "2", "hint": { "BOTTOM_CENTER": "ABC" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "2" } } } } },
            { "key": "key_3", "col": 2, "row": 0, "content": { "label": "3", "hint": { "BOTTOM_CENTER": "DEF" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "3" } } } } },
            { "key": "key_4", "col": 0, "row": 1, "content": { "label": "4", "hint": { "BOTTOM_CENTER": "GHI" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "4" } } } } },
            { "key": "key_5", "col": 1, "row": 1, "content": { "label": "5", "hint": { "BOTTOM_CENTER": "JKL" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "5" } } } } },
            { "key": "key_6", "col": 2, "row": 1, "content": { "label": "6", "hint": { "BOTTOM_CENTER": "MNO" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "6" } } } } },
            { "key": "key_7", "col": 0, "row": 2, "content": { "label": "7", "hint": { "BOTTOM_CENTER": "PQRS" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "7" } } } } },
            { "key": "key_8", "col": 1, "row": 2, "content": { "label": "8", "hint": { "BOTTOM_CENTER": "TUV" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "8" } } } } },
            { "key": "key_9", "col": 2, "row": 2, "content": { "label": "9", "hint": { "BOTTOM_CENTER": "WXYZ" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "9" } } } } },
            { "key": "key_star", "col": 0, "row": 3, "styleRef": "key_function", "content": { "label": "*" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "*" } } } } },
            { "key": "key_0", "col": 1, "row": 3, "content": { "label": "0", "hint": { "BOTTOM_CENTER": "Space" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } } },
            { "key": "key_hash", "col": 2, "row": 3, "styleRef": "key_function", "content": { "label": "#" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "#" } } } } },
            { "key": "backspace", "col": 0, "row": 4, "colSpan": 1, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } }, "LONG_PRESS": { "actionType": "delete", "payload": { "count": 10 } } } } },
            { "key": "sym", "col": 1, "row": 4, "colSpan": 1, "styleRef": "key_function", "content": { "label": "?123" }, "actions": { "gestures": { "TAP": { "actionType": "openPanel", "payload": { "panel": "SYMBOL" } } } } },
            { "key": "enter", "col": 2, "row": 4, "colSpan": 1, "styleRef": "key_action", "content": { "label": "Enter" }, "actions": { "gestures": { "TAP": { "actionType": "performEditorAction" } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/assets/layouts/t9_chinese.jsonc
git commit -m "feat: add t9_chinese.jsonc nine-key layout"
```

---

## Task 8: 创建英文多布局 JSONC 文件

**Files:**
- Create: `app/src/main/assets/layouts/qwerty_abc.jsonc`
- Create: `app/src/main/assets/layouts/qwerty_dvorak.jsonc`
- Create: `app/src/main/assets/layouts/qwerty_colemak.jsonc`

- [ ] **Step 1: 创建 ABC 布局**

```jsonc
// app/src/main/assets/layouts/qwerty_abc.jsonc
{
  "schemaVersion": 2,
  "id": "qwerty_abc",
  "meta": { "name": "ABC", "description": "Alphabetical ABC layout" },
  "root": {
    "id": "root", "type": "composite", "orientation": "VERTICAL",
    "regions": [
      { "id": "candidate_region", "role": "CANDIDATE", "container": { "id": "candidate_bar", "type": "linear", "orientation": "HORIZONTAL", "children": [], "height": { "dp": 40 } } },
      {
        "id": "keyboard_region", "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid", "type": "grid", "columns": 5,
          "cells": [
            { "key": "a", "col": 0, "row": 0, "content": { "label": "A" } },
            { "key": "b", "col": 1, "row": 0, "content": { "label": "B" } },
            { "key": "c", "col": 2, "row": 0, "content": { "label": "C" } },
            { "key": "d", "col": 3, "row": 0, "content": { "label": "D" } },
            { "key": "e", "col": 4, "row": 0, "content": { "label": "E" } },
            { "key": "f", "col": 0, "row": 1, "content": { "label": "F" } },
            { "key": "g", "col": 1, "row": 1, "content": { "label": "G" } },
            { "key": "h", "col": 2, "row": 1, "content": { "label": "H" } },
            { "key": "i", "col": 3, "row": 1, "content": { "label": "I" } },
            { "key": "j", "col": 4, "row": 1, "content": { "label": "J" } },
            { "key": "k", "col": 0, "row": 2, "content": { "label": "K" } },
            { "key": "l", "col": 1, "row": 2, "content": { "label": "L" } },
            { "key": "m", "col": 2, "row": 2, "content": { "label": "M" } },
            { "key": "n", "col": 3, "row": 2, "content": { "label": "N" } },
            { "key": "o", "col": 4, "row": 2, "content": { "label": "O" } },
            { "key": "p", "col": 0, "row": 3, "content": { "label": "P" } },
            { "key": "q", "col": 1, "row": 3, "content": { "label": "Q" } },
            { "key": "r", "col": 2, "row": 3, "content": { "label": "R" } },
            { "key": "s", "col": 3, "row": 3, "content": { "label": "S" } },
            { "key": "t", "col": 4, "row": 3, "content": { "label": "T" } },
            { "key": "u", "col": 0, "row": 4, "content": { "label": "U" } },
            { "key": "v", "col": 1, "row": 4, "content": { "label": "V" } },
            { "key": "w", "col": 2, "row": 4, "content": { "label": "W" } },
            { "key": "x", "col": 3, "row": 4, "content": { "label": "X" } },
            { "key": "y", "col": 4, "row": 4, "content": { "label": "Y" } },
            { "key": "shift", "col": 0, "row": 5, "styleRef": "key_function", "content": { "icon": "shift" }, "actions": { "gestures": { "TAP": { "actionType": "cycleLayer", "payload": { "layers": ["NORMAL", "SHIFTED"] } } } } },
            { "key": "z", "col": 1, "row": 5, "content": { "label": "Z" } },
            { "key": "space", "col": 2, "row": 5, "colSpan": 1, "styleRef": "key_space", "content": { "label": " " }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } } },
            { "key": "backspace", "col": 3, "row": 5, "colSpan": 2, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 2: 创建 Dvorak 布局**

```jsonc
// app/src/main/assets/layouts/qwerty_dvorak.jsonc
{
  "schemaVersion": 2,
  "id": "qwerty_dvorak",
  "meta": { "name": "Dvorak", "description": "Dvorak optimized layout" },
  "root": {
    "id": "root", "type": "composite", "orientation": "VERTICAL",
    "regions": [
      { "id": "candidate_region", "role": "CANDIDATE", "container": { "id": "candidate_bar", "type": "linear", "orientation": "HORIZONTAL", "children": [], "height": { "dp": 40 } } },
      {
        "id": "keyboard_region", "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid", "type": "grid", "columns": 10,
          "cells": [
            { "key": "backtick", "col": 0, "row": 0, "content": { "label": "'" } },
            { "key": "comma", "col": 1, "row": 0, "content": { "label": "," } },
            { "key": "period", "col": 2, "row": 0, "content": { "label": "." } },
            { "key": "p", "col": 3, "row": 0, "content": { "label": "P" } },
            { "key": "y", "col": 4, "row": 0, "content": { "label": "Y" } },
            { "key": "f", "col": 5, "row": 0, "content": { "label": "F" } },
            { "key": "g", "col": 6, "row": 0, "content": { "label": "G" } },
            { "key": "c", "col": 7, "row": 0, "content": { "label": "C" } },
            { "key": "r", "col": 8, "row": 0, "content": { "label": "R" } },
            { "key": "l", "col": 9, "row": 0, "content": { "label": "L" } },
            { "key": "a", "col": 0, "row": 1, "content": { "label": "A" } },
            { "key": "o", "col": 1, "row": 1, "content": { "label": "O" } },
            { "key": "e", "col": 2, "row": 1, "content": { "label": "E" } },
            { "key": "u", "col": 3, "row": 1, "content": { "label": "U" } },
            { "key": "i", "col": 4, "row": 1, "content": { "label": "I" } },
            { "key": "d", "col": 5, "row": 1, "content": { "label": "D" } },
            { "key": "h", "col": 6, "row": 1, "content": { "label": "H" } },
            { "key": "t", "col": 7, "row": 1, "content": { "label": "T" } },
            { "key": "n", "col": 8, "row": 1, "content": { "label": "N" } },
            { "key": "s", "col": 9, "row": 1, "content": { "label": "S" } },
            { "key": "semicolon", "col": 0, "row": 2, "content": { "label": ";" } },
            { "key": "q", "col": 1, "row": 2, "content": { "label": "Q" } },
            { "key": "j", "col": 2, "row": 2, "content": { "label": "J" } },
            { "key": "k", "col": 3, "row": 2, "content": { "label": "K" } },
            { "key": "x", "col": 4, "row": 2, "content": { "label": "X" } },
            { "key": "b", "col": 5, "row": 2, "content": { "label": "B" } },
            { "key": "m", "col": 6, "row": 2, "content": { "label": "M" } },
            { "key": "w", "col": 7, "row": 2, "content": { "label": "W" } },
            { "key": "v", "col": 8, "row": 2, "content": { "label": "V" } },
            { "key": "z", "col": 9, "row": 2, "content": { "label": "Z" } },
            { "key": "shift", "col": 0, "row": 3, "colSpan": 1, "styleRef": "key_function", "content": { "icon": "shift" }, "actions": { "gestures": { "TAP": { "actionType": "cycleLayer", "payload": { "layers": ["NORMAL", "SHIFTED"] } } } } },
            { "key": "comma2", "col": 1, "row": 3, "content": { "label": "," } },
            { "key": "space", "col": 2, "row": 3, "colSpan": 5, "styleRef": "key_space", "content": { "label": "Space" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } } },
            { "key": "period2", "col": 7, "row": 3, "content": { "label": "." } },
            { "key": "backspace", "col": 8, "row": 3, "colSpan": 2, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 3: 创建 Colemak 布局**

```jsonc
// app/src/main/assets/layouts/qwerty_colemak.jsonc
{
  "schemaVersion": 2,
  "id": "qwerty_colemak",
  "meta": { "name": "Colemak", "description": "Colemak layout" },
  "root": {
    "id": "root", "type": "composite", "orientation": "VERTICAL",
    "regions": [
      { "id": "candidate_region", "role": "CANDIDATE", "container": { "id": "candidate_bar", "type": "linear", "orientation": "HORIZONTAL", "children": [], "height": { "dp": 40 } } },
      {
        "id": "keyboard_region", "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid", "type": "grid", "columns": 10,
          "cells": [
            { "key": "q", "col": 0, "row": 0, "content": { "label": "Q" } },
            { "key": "w", "col": 1, "row": 0, "content": { "label": "W" } },
            { "key": "f", "col": 2, "row": 0, "content": { "label": "F" } },
            { "key": "p", "col": 3, "row": 0, "content": { "label": "P" } },
            { "key": "g", "col": 4, "row": 0, "content": { "label": "G" } },
            { "key": "j", "col": 5, "row": 0, "content": { "label": "J" } },
            { "key": "l", "col": 6, "row": 0, "content": { "label": "L" } },
            { "key": "u", "col": 7, "row": 0, "content": { "label": "U" } },
            { "key": "y", "col": 8, "row": 0, "content": { "label": "Y" } },
            { "key": "semicolon", "col": 9, "row": 0, "content": { "label": ";" } },
            { "key": "a", "col": 0, "row": 1, "content": { "label": "A" } },
            { "key": "r", "col": 1, "row": 1, "content": { "label": "R" } },
            { "key": "s", "col": 2, "row": 1, "content": { "label": "S" } },
            { "key": "t", "col": 3, "row": 1, "content": { "label": "T" } },
            { "key": "d", "col": 4, "row": 1, "content": { "label": "D" } },
            { "key": "h", "col": 5, "row": 1, "content": { "label": "H" } },
            { "key": "n", "col": 6, "row": 1, "content": { "label": "N" } },
            { "key": "e", "col": 7, "row": 1, "content": { "label": "E" } },
            { "key": "i", "col": 8, "row": 1, "content": { "label": "I" } },
            { "key": "o", "col": 9, "row": 1, "content": { "label": "O" } },
            { "key": "shift", "col": 0, "row": 2, "colSpan": 1, "styleRef": "key_function", "content": { "icon": "shift" }, "actions": { "gestures": { "TAP": { "actionType": "cycleLayer", "payload": { "layers": ["NORMAL", "SHIFTED"] } } } } },
            { "key": "z", "col": 1, "row": 2, "content": { "label": "Z" } },
            { "key": "x", "col": 2, "row": 2, "content": { "label": "X" } },
            { "key": "c", "col": 3, "row": 2, "content": { "label": "C" } },
            { "key": "v", "col": 4, "row": 2, "content": { "label": "V" } },
            { "key": "b", "col": 5, "row": 2, "content": { "label": "B" } },
            { "key": "k", "col": 6, "row": 2, "content": { "label": "K" } },
            { "key": "m", "col": 7, "row": 2, "content": { "label": "M" } },
            { "key": "backspace", "col": 8, "row": 2, "colSpan": 2, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } } } } },
            { "key": "num", "col": 0, "row": 3, "styleRef": "key_function", "content": { "label": "?123" }, "actions": { "gestures": { "TAP": { "actionType": "openPanel", "payload": { "panel": "SYMBOL" } } } } },
            { "key": "comma", "col": 1, "row": 3, "content": { "label": "," } },
            { "key": "space", "col": 2, "row": 3, "colSpan": 5, "styleRef": "key_space", "content": { "label": "Space" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } } },
            { "key": "period", "col": 7, "row": 3, "content": { "label": "." } },
            { "key": "backspace2", "col": 8, "row": 3, "colSpan": 2, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/assets/layouts/qwerty_abc.jsonc app/src/main/assets/layouts/qwerty_dvorak.jsonc app/src/main/assets/layouts/qwerty_colemak.jsonc
git commit -m "feat: add ABC, Dvorak, Colemak English keyboard layouts"
```

---

## Task 9: 创建日语假名和拨号键盘布局

**Files:**
- Create: `app/src/main/assets/layouts/hiragana.jsonc`
- Create: `app/src/main/assets/layouts/phone_dial.jsonc`

- [ ] **Step 1: 创建平假名布局**

```jsonc
// app/src/main/assets/layouts/hiragana.jsonc
{
  "schemaVersion": 2,
  "id": "hiragana",
  "meta": { "name": "ひらがな", "description": "Hiragana 50-on keyboard" },
  "root": {
    "id": "root", "type": "composite", "orientation": "VERTICAL",
    "regions": [
      { "id": "candidate_region", "role": "CANDIDATE", "container": { "id": "candidate_bar", "type": "linear", "orientation": "HORIZONTAL", "children": [], "height": { "dp": 40 } } },
      {
        "id": "keyboard_region", "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid", "type": "grid", "columns": 5,
          "cells": [
            { "key": "a", "col": 0, "row": 0, "content": { "label": "あ" } },
            { "key": "i", "col": 1, "row": 0, "content": { "label": "い" } },
            { "key": "u", "col": 2, "row": 0, "content": { "label": "う" } },
            { "key": "e", "col": 3, "row": 0, "content": { "label": "え" } },
            { "key": "o", "col": 4, "row": 0, "content": { "label": "お" } },
            { "key": "ka", "col": 0, "row": 1, "content": { "label": "か" } },
            { "key": "ki", "col": 1, "row": 1, "content": { "label": "き" } },
            { "key": "ku", "col": 2, "row": 1, "content": { "label": "く" } },
            { "key": "ke", "col": 3, "row": 1, "content": { "label": "け" } },
            { "key": "ko", "col": 4, "row": 1, "content": { "label": "こ" } },
            { "key": "sa", "col": 0, "row": 2, "content": { "label": "さ" } },
            { "key": "si", "col": 1, "row": 2, "content": { "label": "し" } },
            { "key": "su", "col": 2, "row": 2, "content": { "label": "す" } },
            { "key": "se", "col": 3, "row": 2, "content": { "label": "せ" } },
            { "key": "so", "col": 4, "row": 2, "content": { "label": "そ" } },
            { "key": "ta", "col": 0, "row": 3, "content": { "label": "た" } },
            { "key": "ti", "col": 1, "row": 3, "content": { "label": "ち" } },
            { "key": "tu", "col": 2, "row": 3, "content": { "label": "つ" } },
            { "key": "te", "col": 3, "row": 3, "content": { "label": "て" } },
            { "key": "to", "col": 4, "row": 3, "content": { "label": "と" } },
            { "key": "na", "col": 0, "row": 4, "content": { "label": "な" } },
            { "key": "ni", "col": 1, "row": 4, "content": { "label": "に" } },
            { "key": "nu", "col": 2, "row": 4, "content": { "label": "ぬ" } },
            { "key": "ne", "col": 3, "row": 4, "content": { "label": "ね" } },
            { "key": "no", "col": 4, "row": 4, "content": { "label": "の" } },
            { "key": "ha", "col": 0, "row": 5, "content": { "label": "は" } },
            { "key": "hi", "col": 1, "row": 5, "content": { "label": "ひ" } },
            { "key": "hu", "col": 2, "row": 5, "content": { "label": "ふ" } },
            { "key": "he", "col": 3, "row": 5, "content": { "label": "へ" } },
            { "key": "ho", "col": 4, "row": 5, "content": { "label": "ほ" } },
            { "key": "ma", "col": 0, "row": 6, "content": { "label": "ま" } },
            { "key": "mi", "col": 1, "row": 6, "content": { "label": "み" } },
            { "key": "mu", "col": 2, "row": 6, "content": { "label": "む" } },
            { "key": "me", "col": 3, "row": 6, "content": { "label": "め" } },
            { "key": "mo", "col": 4, "row": 6, "content": { "label": "も" } },
            { "key": "ya", "col": 0, "row": 7, "content": { "label": "や" } },
            { "key": "yu", "col": 2, "row": 7, "content": { "label": "ゆ" } },
            { "key": "yo", "col": 4, "row": 7, "content": { "label": "よ" } },
            { "key": "ra", "col": 0, "row": 8, "content": { "label": "ら" } },
            { "key": "ri", "col": 1, "row": 8, "content": { "label": "り" } },
            { "key": "ru", "col": 2, "row": 8, "content": { "label": "る" } },
            { "key": "re", "col": 3, "row": 8, "content": { "label": "れ" } },
            { "key": "ro", "col": 4, "row": 8, "content": { "label": "ろ" } },
            { "key": "wa", "col": 0, "row": 9, "content": { "label": "わ" } },
            { "key": "wo", "col": 2, "row": 9, "content": { "label": "を" } },
            { "key": "n", "col": 4, "row": 9, "content": { "label": "ん" } },
            { "key": "backspace", "col": 0, "row": 10, "colSpan": 1, "styleRef": "key_function", "content": { "icon": "backspace" }, "repeatable": true, "actions": { "gestures": { "TAP": { "actionType": "delete", "payload": { "count": 1 } } } } },
            { "key": "enter", "col": 4, "row": 10, "colSpan": 1, "styleRef": "key_action", "content": { "label": "Enter" }, "actions": { "gestures": { "TAP": { "actionType": "performEditorAction" } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 2: 创建拨号键盘布局**

```jsonc
// app/src/main/assets/layouts/phone_dial.jsonc
{
  "schemaVersion": 2,
  "id": "phone_dial",
  "meta": { "name": "Phone Dial", "description": "Phone dial pad layout" },
  "root": {
    "id": "root", "type": "composite", "orientation": "VERTICAL",
    "regions": [
      {
        "id": "keyboard_region", "role": "KEYBOARD",
        "container": {
          "id": "keyboard_grid", "type": "grid", "columns": 3,
          "cells": [
            { "key": "dial_1", "col": 0, "row": 0, "content": { "label": "1" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "1" } } } } },
            { "key": "dial_2", "col": 1, "row": 0, "content": { "label": "2", "hint": { "BOTTOM_CENTER": "ABC" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "2" } } } } },
            { "key": "dial_3", "col": 2, "row": 0, "content": { "label": "3", "hint": { "BOTTOM_CENTER": "DEF" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "3" } } } } },
            { "key": "dial_4", "col": 0, "row": 1, "content": { "label": "4", "hint": { "BOTTOM_CENTER": "GHI" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "4" } } } } },
            { "key": "dial_5", "col": 1, "row": 1, "content": { "label": "5", "hint": { "BOTTOM_CENTER": "JKL" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "5" } } } } },
            { "key": "dial_6", "col": 2, "row": 1, "content": { "label": "6", "hint": { "BOTTOM_CENTER": "MNO" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "6" } } } } },
            { "key": "dial_7", "col": 0, "row": 2, "content": { "label": "7", "hint": { "BOTTOM_CENTER": "PQRS" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "7" } } } } },
            { "key": "dial_8", "col": 1, "row": 2, "content": { "label": "8", "hint": { "BOTTOM_CENTER": "TUV" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "8" } } } } },
            { "key": "dial_9", "col": 2, "row": 2, "content": { "label": "9", "hint": { "BOTTOM_CENTER": "WXYZ" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "9" } } } } },
            { "key": "dial_star", "col": 0, "row": 3, "styleRef": "key_function", "content": { "label": "*" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "*" } } } } },
            { "key": "dial_0", "col": 1, "row": 3, "content": { "label": "0", "hint": { "BOTTOM_CENTER": "+" } }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "0" } } } } },
            { "key": "dial_hash", "col": 2, "row": 3, "styleRef": "key_function", "content": { "label": "#" }, "actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": "#" } } } } }
          ]
        }
      }
    ]
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/layouts/hiragana.jsonc app/src/main/assets/layouts/phone_dial.jsonc
git commit -m "feat: add hiragana and phone_dial keyboard layouts"
```

---

## Task 10: 实现 T9 解码器和自然码双拼编码器

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/T9Decoder.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/ShuangpinMapping.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/ShuangpinEncoder.kt`

- [ ] **Step 1: 创建 T9Decoder**

```kotlin
package xyz.xiao6.myboard.core.engine.builtin

import kotlinx.serialization.json.*

/**
 * T9 九键解码器。
 * 将数字序列转换为拼音候选列表，配合词典进行查找。
 */
class T9Decoder(private val keyMap: Map<String, List<String>>) {

    companion object {
        fun fromJson(jsonString: String): T9Decoder {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val keyMapObj = json["keyMap"]?.jsonObject ?: return T9Decoder(emptyMap())
            val map = keyMapObj.mapValues { (_, value) ->
                value.jsonArray.map { it.jsonPrimitive.content }
            }
            return T9Decoder(map)
        }
    }

    /**
     * 将数字序列的所有拼音组合解码出来。
     * 例如："22" → ["aa", "ab", "ac", "ba", "bb", "bc", "ca", "cb", "cc"]
     */
    fun decode(sequence: String): List<String> {
        if (sequence.isEmpty()) return emptyList()
        val validSequence = sequence.filter { it.isDigit() && keyMap.containsKey(it.toString()) }
        if (validSequence.isEmpty()) return emptyList()

        val charsList = validSequence.map { keyMap[it.toString()] ?: emptyList() }
        return charsList.fold(listOf("")) { acc, list ->
            acc.flatMap { prefix -> list.map { suffix -> prefix + suffix } }
        }
    }

    /**
     * 获取单个按键对应的候选字符。
     */
    fun getCandidatesForKey(key: String): List<String> {
        return keyMap[key] ?: emptyList()
    }
}
```

- [ ] **Step 2: 创建 ShuangpinMapping**

```kotlin
package xyz.xiao6.myboard.core.engine.builtin

import kotlinx.serialization.json.*

/**
 * 自然码双拼映射数据。
 */
data class ShuangpinMapping(
    val initialMap: Map<String, String>,
    val finalMap: Map<String, String>
) {
    companion object {
        fun fromJson(jsonString: String): ShuangpinMapping {
            val json = Json.parseToJsonElement(jsonString).jsonObject
            val initials = json["initials"]?.jsonObject?.mapValues { (_, v) ->
                v.jsonPrimitive.content
            } ?: emptyMap()
            val finals = json["finals"]?.jsonObject?.mapValues { (_, v) ->
                v.jsonPrimitive.content
            } ?: emptyMap()
            return ShuangpinMapping(initials, finals)
        }
    }

    /**
     * 将双拼的声母键转换为全拼声母。
     * 例如：'v' → "zh", 'u' → "sh"
     */
    fun resolveInitial(key: String): String {
        return initialMap[key] ?: key
    }

    /**
     * 将双拼的韵母键转换为全拼韵母。
     * 例如：'q' → "iu", 'w' → "ei"
     */
    fun resolveFinal(key: String): String {
        return finalMap[key] ?: key
    }
}
```

- [ ] **Step 3: 创建 ShuangpinEncoder**

```kotlin
package xyz.xiao6.myboard.core.engine.builtin

/**
 * 自然码双拼编码器。
 * 将双拼输入转换为全拼序列。
 *
 * 双拼规则：
 * - 每个汉字由恰好两个键输入（声母+韵母）
 * - 零声母音节（如"啊"）使用特殊处理
 * - 翘舌音 zh/ch/sh 用 v/i/u 表示
 */
class ShuangpinEncoder(private val mapping: ShuangpinMapping) {

    /**
     * 将双拼对转换为全拼。
     * 例如："vs" → "zh" + "ong" → "zhong"
     */
    fun decodePair(initialKey: String, finalKey: String): String {
        val initial = mapping.resolveInitial(initialKey)
        val final_ = mapping.resolveFinal(finalKey)
        return initial + final_
    }

    /**
     * 将完整的双拼字符串解码为全拼序列。
     * 双拼每两个字符为一组（声母+韵母）。
     *
     * 例如："vsgo" → ["zhong", "guo"]
     */
    fun decodeAll(input: String): List<String> {
        if (input.length < 2) return listOf(input)

        val results = mutableListOf<String>()
        var i = 0
        while (i + 1 < input.length) {
            val initialKey = input[i].toString()
            val finalKey = input[i + 1].toString()
            val fullPinyin = decodePair(initialKey, finalKey)
            results.add(fullPinyin)
            i += 2
        }
        // 如果有奇数个字符，最后一个直接作为声母
        if (i < input.length) {
            results.add(mapping.resolveInitial(input[i].toString()))
        }
        return results
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/T9Decoder.kt app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/ShuangpinMapping.kt app/src/main/java/xyz/xiao6/myboard/core/engine/builtin/ShuangpinEncoder.kt
git commit -m "feat: add T9Decoder and ShuangpinEncoder for T9 and double-pinyin input"
```

---

## Task 11: 实现 Room 词典系统

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/PhraseEntity.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/UserPhraseEntity.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/DictionaryDao.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/UserDictionaryDao.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/DictionaryDatabase.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/DictionaryModule.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/PinyinDictionary.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/UserDictionary.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/AdaptiveDictionary.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/HotWordCalculator.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/dictionary/DictionaryUpdater.kt`

- [ ] **Step 1: 创建 PhraseEntity**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 系统词典词条实体。
 * 存储拼音到词组的映射及词频信息。
 */
@Entity(tableName = "phrases")
data class PhraseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pinyin") val pinyin: String,
    @ColumnInfo(name = "phrase") val phrase: String,
    @ColumnInfo(name = "frequency") val frequency: Int = 0,
    @ColumnInfo(name = "type") val type: Int = 0, // 0=系统 1=用户导入 2=热词
    @ColumnInfo(name = "created_at") val createdAt: Long = 0,
    @ColumnInfo(name = "last_used_at") val lastUsedAt: Long = 0
)
```

- [ ] **Step 2: 创建 UserPhraseEntity**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户自定义词条实体。
 * 独立于系统词典，支持导出和迁移。
 */
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

- [ ] **Step 3: 创建 DictionaryDao**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import androidx.room.*

/**
 * 系统词典 DAO 接口。
 */
@Dao
interface DictionaryDao {
    @Query("SELECT * FROM phrases WHERE pinyin = :pinyin ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPinyin(pinyin: String, limit: Int = 50): List<PhraseEntity>

    @Query("SELECT * FROM phrases WHERE pinyin LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun lookupByPrefix(prefix: String, limit: Int = 50): List<PhraseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(phrase: PhraseEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(phrases: List<PhraseEntity>)

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

    @Query("SELECT COUNT(*) FROM phrases")
    suspend fun count(): Int
}
```

- [ ] **Step 4: 创建 UserDictionaryDao**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import androidx.room.*

/**
 * 用户词典 DAO 接口。
 */
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

    @Query("SELECT COUNT(*) FROM user_phrases")
    suspend fun count(): Int
}
```

- [ ] **Step 5: 创建 DictionaryDatabase**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room 词典数据库。
 * 系统词典和用户词典分别存储在不同的表中。
 */
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

- [ ] **Step 6: 创建 DictionaryModule**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import android.content.Context

/**
 * 词典依赖模块。
 * 统一创建和管理词典相关组件的生命周期。
 */
class DictionaryModule(context: Context) {

    private val database = DictionaryDatabase.getInstance(context)

    val pinyinDictionary: PinyinDictionary by lazy {
        PinyinDictionary(database.dictionaryDao(), database.userDictionaryDao())
    }

    val userDictionary: UserDictionary by lazy {
        UserDictionary(database.userDictionaryDao())
    }

    val adaptiveDictionary: AdaptiveDictionary by lazy {
        AdaptiveDictionary(pinyinDictionary, HotWordCalculator(database.dictionaryDao()))
    }

    val hotWordCalculator: HotWordCalculator by lazy {
        HotWordCalculator(database.dictionaryDao())
    }

    val dictionaryUpdater: DictionaryUpdater by lazy {
        DictionaryUpdater(database)
    }
}
```

- [ ] **Step 7: 创建 PinyinDictionary**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import xyz.xiao6.myboard.core.contract.Candidate
import xyz.xiao6.myboard.core.contract.CandidateSource
import xyz.xiao6.myboard.core.contract.CandidateType

/**
 * 拼音词典实现。
 * 结合系统词典和用户词典，提供统一的查找接口。
 */
class PinyinDictionary(
    private val dictionaryDao: DictionaryDao,
    private val userDao: UserDictionaryDao
) {

    /**
     * 根据拼音查找候选词。
     * 用户词典结果权重高于系统词典。
     */
    suspend fun lookup(query: String, limit: Int = 50): List<Candidate> {
        // 1. 先查用户词典（权重 1.5x）
        val userResults = userDao.lookupByPinyin(query, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.WORD,
                score = entity.frequency * 1.5,
                source = CandidateSource.USER
            )
        }

        // 2. 再查系统词典
        val systemResults = dictionaryDao.lookupByPinyin(query, limit).map { entity ->
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

    /**
     * 按前缀查找（用于联想输入）。
     */
    suspend fun lookupByPrefix(prefix: String, limit: Int = 20): List<Candidate> {
        return dictionaryDao.lookupByPrefix(prefix, limit).map { entity ->
            Candidate(
                text = entity.phrase,
                type = CandidateType.PREDICTION,
                score = entity.frequency.toDouble(),
                source = CandidateSource.SYSTEM
            )
        }
    }

    /**
     * 添加用户自定义词条。
     */
    suspend fun addPhrase(pinyin: String, phrase: String, frequency: Int = 1) {
        userDao.insert(
            UserPhraseEntity(
                pinyin = pinyin,
                phrase = phrase,
                frequency = frequency
            )
        )
    }

    /**
     * 更新词条词频。
     */
    suspend fun updateFrequency(phrase: String, delta: Int = 1) {
        val now = System.currentTimeMillis()
        userDao.incrementFrequency(phrase, delta, now)
        dictionaryDao.incrementFrequency(phrase, delta)
    }

    /**
     * 删除用户词条。
     */
    suspend fun removeUserPhrase(phrase: String) {
        val all = userDao.getAll()
        val target = all.find { it.phrase == phrase }
        if (target != null) {
            userDao.delete(target)
        }
    }
}
```

- [ ] **Step 8: 创建 UserDictionary**

```kotlin
package xyz.xiao6.myboard.core.dictionary

/**
 * 用户词典管理。
 * 提供用户自定义词条的增删查和导出功能。
 */
class UserDictionary(private val userDao: UserDictionaryDao) {

    suspend fun add(pinyin: String, phrase: String, frequency: Int = 1) {
        userDao.insert(
            UserPhraseEntity(
                pinyin = pinyin,
                phrase = phrase,
                frequency = frequency
            )
        )
    }

    suspend fun remove(phrase: String) {
        val all = userDao.getAll()
        val target = all.find { it.phrase == phrase }
        if (target != null) {
            userDao.delete(target)
        }
    }

    suspend fun lookup(pinyin: String, limit: Int = 50): List<UserPhraseEntity> {
        return userDao.lookupByPinyin(pinyin, limit)
    }

    suspend fun getAll(): List<UserPhraseEntity> {
        return userDao.getAll()
    }

    suspend fun count(): Int {
        return userDao.count()
    }

    /**
     * 导出用户词典为文本格式。
     * 格式：每行 "拼音 词组 词频"
     */
    suspend fun exportToText(): String {
        val all = userDao.getAll()
        return all.joinToString("\n") { "${it.pinyin} ${it.phrase} ${it.frequency}" }
    }
}
```

- [ ] **Step 9: 创建 AdaptiveDictionary**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import xyz.xiao6.myboard.core.contract.Candidate

/**
 * 自适应词频词典。
 * 根据用户使用习惯自动调整词频排序。
 */
class AdaptiveDictionary(
    private val pinyinDictionary: PinyinDictionary,
    private val hotWordCalculator: HotWordCalculator
) {
    /**
     * 用户选择候选后调用，自动调整词频。
     */
    suspend fun onCandidateSelected(candidate: Candidate) {
        pinyinDictionary.updateFrequency(candidate.text, 1)
    }

    /**
     * 获取热词列表（带时间衰减）。
     */
    suspend fun getHotWords(limit: Int = 20): List<Candidate> {
        return hotWordCalculator.calculateHotWords(limit)
    }

    /**
     * 查找候选词。
     */
    suspend fun lookup(query: String, limit: Int = 50): List<Candidate> {
        // 先查热词，再查词典
        val hotWords = hotWordCalculator.calculateHotWords(limit / 4)
        val dictWords = pinyinDictionary.lookup(query, limit)

        // 合并去重
        return (hotWords + dictWords)
            .distinctBy { it.text }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
```

- [ ] **Step 10: 创建 HotWordCalculator**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import xyz.xiao6.myboard.core.contract.Candidate
import xyz.xiao6.myboard.core.contract.CandidateSource
import xyz.xiao6.myboard.core.contract.CandidateType
import kotlin.math.exp

/**
 * 热词计算器。
 * 基于时间衰减算法，最近使用过的词权重更高。
 */
class HotWordCalculator(private val dictionaryDao: DictionaryDao) {

    companion object {
        private const val ONE_WEEK_MS = 7 * 24 * 60 * 60 * 1000L
        private const val DECAY_HALF_LIFE_HOURS = 168.0 // 一周
    }

    /**
     * 计算热词列表。
     * 使用指数时间衰减：score = frequency * exp(-hoursSinceUse / halfLife)
     */
    suspend fun calculateHotWords(limit: Int = 20): List<Candidate> {
        val now = System.currentTimeMillis()
        val recentWords = dictionaryDao.getRecentWords(now - ONE_WEEK_MS, limit * 2)

        return recentWords
            .map { entity ->
                val hoursSinceUse = (now - entity.lastUsedAt) / 3_600_000.0
                val timeDecay = exp(-hoursSinceUse / DECAY_HALF_LIFE_HOURS)
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
}
```

- [ ] **Step 11: 创建 DictionaryUpdater**

```kotlin
package xyz.xiao6.myboard.core.dictionary

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 词典更新接口。
 * 支持从文件导入词库、检查更新、导出用户词典。
 */
class DictionaryUpdater(private val database: DictionaryDatabase) {

    companion object {
        private const val TAG = "DictionaryUpdater"
    }

    /**
     * 从文本文件导入词库。
     * 文件格式：每行 "拼音 词组 [词频]"
     */
    suspend fun importFromTextFile(file: File): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val lines = file.readLines()
            val entities = lines.mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) {
                    PhraseEntity(
                        pinyin = parts[0],
                        phrase = parts[1],
                        frequency = parts.getOrNull(2)?.toIntOrNull() ?: 1,
                        type = 0
                    )
                } else null
            }
            database.dictionaryDao().insertAll(entities)
            Log.d(TAG, "Imported ${entities.size} phrases from ${file.name}")
            Result.success(entities.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import dictionary", e)
            Result.failure(e)
        }
    }

    /**
     * 导出用户词典到文本文件。
     */
    suspend fun exportUserDictionary(file: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val entities = database.userDictionaryDao().getAll()
            val text = entities.joinToString("\n") { "${it.pinyin} ${it.phrase} ${it.frequency}" }
            file.writeText(text)
            Log.d(TAG, "Exported ${entities.size} user phrases to ${file.name}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export user dictionary", e)
            Result.failure(e)
        }
    }

    /**
     * 清理过期系统词条。
     */
    suspend fun cleanupOldEntries(olderThanDays: Int = 90) {
        val cutoff = System.currentTimeMillis() - olderThanDays * 24 * 60 * 60 * 1000L
        database.dictionaryDao().cleanupOldSystemWords(cutoff)
    }
}
```

- [ ] **Step 12: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 13: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/dictionary/
git commit -m "feat: implement Room dictionary system with pinyin dictionary, user dictionary, hot word calculator"
```

---

## Task 12: 创建面板 Composable — Emoji、剪贴板、颜文字、占位

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/panels/EmojiPanel.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/panels/ClipboardPanel.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/panels/KaomojiPanel.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/ui/panels/PlaceholderPanel.kt`

- [ ] **Step 1: 创建 EmojiPanel**

```kotlin
package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class EmojiCategory(val name: String, val emojis: List<EmojiItem>)
data class EmojiItem(val char: String, val name: String)

/**
 * Emoji 面板。
 * 分类展示 Emoji，支持搜索。
 */
@Composable
fun EmojiPanel(
    categories: List<EmojiCategory>,
    onEmojiClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Emoji", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp))
            }
        }

        // 搜索栏
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp)
                .height(36.dp),
            placeholder = { Text("搜索", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall
        )

        // 分类标签
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth(),
            edgePadding = 4.dp
        ) {
            categories.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = { Text(category.name, fontSize = 11.sp) }
                )
            }
        }

        // Emoji 网格
        val displayEmojis = if (searchQuery.isNotEmpty()) {
            categories.flatMap { it.emojis }.filter {
                it.name.contains(searchQuery, ignoreCase = true)
            }
        } else {
            selectedCategory?.emojis ?: emptyList()
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(8),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(4.dp)
        ) {
            items(displayEmojis) { emoji ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onEmojiClick(emoji.char) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji.char, fontSize = 20.sp)
                }
            }
        }
    }
}
```

- [ ] **Step 2: 创建 ClipboardPanel**

```kotlin
package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ClipboardItem(
    val id: String,
    val text: String,
    val timestamp: Long
)

/**
 * 剪贴板面板。
 * 显示剪贴板历史记录，支持点击粘贴和删除。
 */
@Composable
fun ClipboardPanel(
    items: List<ClipboardItem>,
    onItemClick: (ClipboardItem) -> Unit,
    onDelete: (ClipboardItem) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("剪贴板", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp))
            }
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无剪贴板内容", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clickable { onItemClick(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.text,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { onDelete(item) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete, "删除",
                                    modifier = Modifier.size(14.dp),
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: 创建 KaomojiPanel**

```kotlin
package xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class KaomojiCategory(val name: String, val kaomojis: List<String>)

/**
 * 颜文字面板。
 */
@Composable
fun KaomojiPanel(
    categories: List<KaomojiCategory>,
    onKaomojiClick: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("颜文字", style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp))
            }
        }

        // 分类标签
        ScrollableTabRow(
            selectedTabIndex = categories.indexOf(selectedCategory).coerceAtLeast(0),
            modifier = Modifier.fillMaxWidth()
        ) {
            categories.forEach { category ->
                Tab(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    text = { Text(category.name, fontSize = 11.sp) }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(selectedCategory?.kaomojis ?: emptyList()) { kaomoji ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .clickable { onKaomojiClick(kaomoji) }
                ) {
                    Text(
                        kaomoji,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 4: 创建 PlaceholderPanel**

```kotlin
package.xyz.xiao6.myboard.ui.panels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.xiao6.myboard.core.contract.PanelType

/**
 * 占位面板。
 * 用于尚未实现的面板类型。
 */
@Composable
fun PlaceholderPanel(
    panelType: PanelType,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(panelType.name, style = MaterialTheme.typography.titleSmall)
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, "关闭", modifier = Modifier.size(18.dp))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${panelType.name} 面板（开发中）",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/panels/EmojiPanel.kt app/src/main/java/xyz/xiao6/myboard/ui/panels/ClipboardPanel.kt app/src/main/java/xyz/xiao6/myboard/ui/panels/KaomojiPanel.kt app/src/main/java/xyz/xiao6/myboard/ui/panels/PlaceholderPanel.kt
git commit -m "feat: add EmojiPanel, ClipboardPanel, KaomojiPanel and PlaceholderPanel composables"
```

---

## Task 13: 实现 ThemeToggler 和 LayoutSwitcher

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/core/toolbar/ThemeToggler.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/core/toolbar/LayoutSwitcher.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/core/settings/SettingsManager.kt:52-57`

- [ ] **Step 1: 添加 SettingsManager.toggleTheme()**

```kotlin
// SettingsManager.kt - 在 themeMode 属性后面添加
fun toggleTheme() {
    themeMode = if (themeMode == "dark") "light" else "dark"
}
```

- [ ] **Step 2: 创建 ThemeToggler**

```kotlin
package xyz.xiao6.myboard.core.toolbar

import xyz.xiao6.myboard.core.settings.SettingsManager
import xyz.xiao6.myboard.core.theme.ThemeResolverImpl

/**
 * 夜间模式切换器。
 * 协调 SettingsManager 和 ThemeResolver 的状态同步。
 */
class ThemeToggler(
    private val settingsManager: SettingsManager,
    private val themeResolver: ThemeResolverImpl
) {
    fun toggle() {
        settingsManager.toggleTheme()
        themeResolver.setActiveTheme(settingsManager.themeMode)
    }

    fun isDarkMode(): Boolean {
        return settingsManager.themeMode == "dark"
    }
}
```

- [ ] **Step 3: 创建 LayoutSwitcher**

```kotlin
package xyz.xiao6.myboard.core.toolbar

import xyz.xiao6.myboard.core.contract.*
import xyz.xiao6.myboard.core.state.KeyboardContextManager
import xyz.xiao6.myboard.core.state.OrthogonalRegistry

/**
 * 布局切换器。
 * 在当前语言的可用 Schema 之间循环切换。
 */
class LayoutSwitcher(
    private val contextManager: KeyboardContextManager,
    private val orthogonalRegistry: OrthogonalRegistry
) {
    /**
     * 循环切换当前语言的 Schema。
     */
    fun cycleLayout() {
        val current = contextManager.context.value
        val manifest = orthogonalRegistry.getManifest(current.orthogonal.locale) ?: return
        val scriptManifest = manifest.scripts[current.orthogonal.script] ?: return
        val schemas = scriptManifest.schemas.keys.toList()
        if (schemas.size <= 1) return

        val currentIndex = schemas.indexOf(current.orthogonal.schema)
        val nextIndex = (currentIndex + 1) % schemas.size
        contextManager.switchSchema(schemas[nextIndex])
    }

    /**
     * 获取当前 Schema 的显示名称。
     */
    fun getCurrentSchemaName(): String {
        val current = contextManager.context.value
        return when (current.orthogonal.schema) {
            BuiltInSchemas.PINYIN -> "拼音"
            BuiltInSchemas.SHUANGPIN_ZIRAN -> "双拼"
            BuiltInSchemas.T9_PINYIN -> "T9"
            BuiltInSchemas.DOUBLE_PINYIN -> "双拼"
            BuiltInSchemas.LATIN_DIRECT -> "英文"
            BuiltInSchemas.ROMAJI -> "假名"
            else -> current.orthogonal.schema.value
        }
    }

    /**
     * 获取当前语言的可用 Schema 列表。
     */
    fun getAvailableSchemas(): List<Schema> {
        val current = contextManager.context.value
        val manifest = orthogonalRegistry.getManifest(current.orthogonal.locale) ?: return emptyList()
        val scriptManifest = manifest.scripts[current.orthogonal.script] ?: return emptyList()
        return scriptManifest.schemas.keys.toList()
    }
}
```

- [ ] **Step 4: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/core/toolbar/ThemeToggler.kt app/src/main/java/xyz/xiao6/myboard/core/toolbar/LayoutSwitcher.kt app/src/main/java/xyz/xiao6/myboard/core/settings/SettingsManager.kt
git commit -m "feat: add ThemeToggler and LayoutSwitcher toolbar components"
```

---

## Task 14: 集成面板和增强 Toolbar — 修改 MyBoardImeService

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/ime/MyBoardImeService.kt`

- [ ] **Step 1: 添加 import 和组件声明**

在 `MyBoardImeService.kt` 中添加 import：

```kotlin
import xyz.xiao6.myboard.core.toolbar.ThemeToggler
import xyz.xiao6.myboard.core.toolbar.LayoutSwitcher
import xyz.xiao6.myboard.core.dictionary.DictionaryModule
import xyz.xiao6.myboard.ui.panels.*
```

在组件声明区域添加：

```kotlin
// toolbar 组件
private lateinit var themeToggler: ThemeToggler
private lateinit var layoutSwitcher: LayoutSwitcher
private lateinit var dictionaryModule: DictionaryModule
```

- [ ] **Step 2: 在 initCoreComponents() 中初始化新组件**

```kotlin
// 在 initCoreComponents() 末尾添加
themeToggler = ThemeToggler(SettingsManager(this), themeResolver)
layoutSwitcher = LayoutSwitcher(keyboardContextManager, orthogonalRegistry)
dictionaryModule = DictionaryModule(this)
```

- [ ] **Step 3: 替换 Toolbar Composable — 添加夜间模式和设置按钮**

完整替换 `MyBoardImeService.kt` 中的 `Toolbar` composable：

```kotlin
@Composable
private fun Toolbar(
    context: KeyboardContext,
    onLocaleSwitch: (LocaleTag) -> Unit,
    onPanelOpen: (PanelType) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .background(Color(0xFFF1F3F4)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 语言切换
        IconButton(
            onClick = {
                val currentLocale = context.orthogonal.locale
                val nextLocale = when (currentLocale.value) {
                    "en-US" -> LocaleTag("zh-CN")
                    "zh-CN" -> LocaleTag("ja-JP")
                    else -> LocaleTag("en-US")
                }
                onLocaleSwitch(nextLocale)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Language,
                "语言",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // 夜间模式
        IconButton(
            onClick = { themeToggler.toggle() },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                if (themeToggler.isDarkMode()) Icons.Default.LightMode else Icons.Default.DarkMode,
                "主题",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // Emoji 面板
        IconButton(
            onClick = { onPanelOpen(PanelType.EMOJI) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.EmojiEmotions,
                "Emoji",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // 符号面板
        IconButton(
            onClick = { onPanelOpen(PanelType.SYMBOL) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Star,
                "符号",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // 剪贴板面板
        IconButton(
            onClick = { onPanelOpen(PanelType.CLIPBOARD) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.ContentPaste,
                "剪贴板",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // STT 面板
        IconButton(
            onClick = { onPanelOpen(PanelType.STT) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Mic,
                "语音",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }

        // 设置
        IconButton(
            onClick = {
                val intent = android.content.Intent(this@MyBoardImeService, xyz.xiao6.myboard.ui.SettingsActivity::class.java)
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Settings,
                "设置",
                tint = Color(0xFF5F6368),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
```

- [ ] **Step 4: 在 onCreateInputView() 中添加面板视图切换**

修改 `onCreateInputView()` 中的 `Column` 内容，在工具栏/候选栏切换之前添加面板检查：

```kotlin
Column(modifier = Modifier.fillMaxWidth()) {
    // 面板视图
    when (context.activePanel) {
        PanelType.SYMBOL -> {
            // 使用现有的 SymbolPanel（已存在）
            xyz.xiao6.myboard.ui.panels.SymbolPanel(
                categories = listOf(
                    "标点" to listOf("，", "。", "！", "？", "；", "：", "、", "…", "—", "·"),
                    "数学" to listOf("＋", "－", "×", "÷", "＝", "≠", "≤", "≥", "≈", "∞"),
                    "货币" to listOf("￥", "＄", "€", "£", "¥", "￠", "₠", "₣", "₤", "₥")
                ),
                onSymbolClick = { symbol ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.PushToken(symbol))
                        updateInputView()
                    }
                },
                onBack = {
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                }
            )
        }
        PanelType.EMOJI -> {
            EmojiPanel(
                categories = listOf(
                    EmojiCategory("表情", listOf(
                        EmojiItem("😀", "笑脸"), EmojiItem("😂", "笑哭"), EmojiItem("😍", "爱心眼"),
                        EmojiItem("🤔", "思考"), EmojiItem("👍", "点赞"), EmojiItem("❤️", "红心"),
                        EmojiItem("🎉", "庆祝"), EmojiItem("🔥", "火"), EmojiItem("✨", "闪光"),
                        EmojiItem("😭", "哭"), EmojiItem("🤣", "笑"), EmojiItem("😊", "害羞")
                    )),
                    EmojiCategory("手势", listOf(
                        EmojiItem("👋", "挥手"), EmojiItem("🤝", "握手"), EmojiItem("✌️", "胜利"),
                        EmojiItem("👌", "OK"), EmojiItem("👏", "鼓掌"), EmojiItem("🙏", "祈祷")
                    ))
                ),
                onEmojiClick = { emoji ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.PushToken(emoji))
                        updateInputView()
                    }
                },
                onClose = {
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                }
            )
        }
        PanelType.CLIPBOARD -> {
            ClipboardPanel(
                items = emptyList(),
                onItemClick = { item ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.PushToken(item.text))
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                },
                onDelete = { },
                onClose = {
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                }
            )
        }
        PanelType.KAOMOJI -> {
            KaomojiPanel(
                categories = listOf(
                    KaomojiCategory("开心", listOf("(≧▽≦)", "(◕‿◕)", "٩(◕‿◕)۶", "(ﾉ◕ヮ◕)ﾉ*:・ﾟ✧")),
                    KaomojiCategory("难过", listOf("(╥_╥)", "(T_T)", "(；д；)", "ಥ_ಥ")),
                    KaomojiCategory("愤怒", listOf("(╬￣皿￣)", "щ(｀Д´щ)", "(°ロ°)!", "(ᗒᗣᗕ)՞")),
                    KaomojiCategory("惊讶", listOf("(⊙_⊙)", "(°o°)", "Σ( ° △ °|||)", "(ﾟДﾟ)"))
                ),
                onKaomojiClick = { kaomoji ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.PushToken(kaomoji))
                        updateInputView()
                    }
                },
                onClose = {
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                }
            )
        }
        PanelType.LLM, PanelType.STT -> {
            PlaceholderPanel(
                panelType = context.activePanel,
                onClose = {
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.ClosePanel)
                        updateInputView()
                    }
                }
            )
        }
        PanelType.NONE -> { /* 不显示面板 */ }
        PanelType.TEXT_EXPANSION -> { /* placeholder */ }
    }

    // 工具栏/候选栏切换（仅在无面板时显示）
    if (context.activePanel == PanelType.NONE) {
        if (context.hasCandidates || context.isComposing) {
            CandidateBar(
                candidates = context.candidates,
                selectedIndex = context.selectedCandidateIndex,
                onCandidateClick = { index ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.CommitCandidate(index))
                        updateInputView()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Toolbar(
                context = context,
                onLocaleSwitch = { targetLocale ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.SwitchLocale(targetLocale))
                        updateInputView()
                    }
                },
                onPanelOpen = { panelType ->
                    serviceScope.launch {
                        inputPipeline.handle(InputAction.OpenPanel(panelType))
                        updateInputView()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // 主键盘
    if (currentMeasured != null && context.activePanel == PanelType.NONE) {
        LayoutRenderer(
            measuredLayout = currentMeasured,
            context = context,
            themeResolver = themeResolver,
            onAction = { action ->
                serviceScope.launch {
                    inputPipeline.handle(action)
                    feedbackPlayer.playHaptic(
                        HapticToken(id = "key_tap", durationMs = 10, amplitude = 50)
                    )
                    updateInputView()
                }
            },
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
    }
}
```

- [ ] **Step 5: 编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ime/MyBoardImeService.kt
git commit -m "feat: integrate panels into IME view and enhance Toolbar with theme toggle and settings"
```

---

## Task 15: 创建留痕记录文件

**Files:**
- Create: `docs/superpowers/changelogs/2026-06-23-ime-expansion.md`

- [ ] **Step 1: 创建留痕记录**

```markdown
# IME扩展实现记录

## 日期：2026-06-23

### 1. 布局系统

**做了什么：**
- 创建 `LayoutAssetsLoader`，从 `assets/layouts/` 加载 JSONC 布局文件，带缓存和 fallback 到硬编码布局
- 创建 7 个新布局 JSONC 文件：shuangpin_ziran、t9_chinese、hiragana、qwerty_dvorak、qwerty_colemak、qwerty_abc、phone_dial
- 修改 `MyBoardImeService` 使所有布局通过 LayoutAssetsLoader 加载

**为什么：**
- 统一布局定义为 JSONC 声明式格式，便于扩展和维护
- LayoutAssetsLoader 提供缓存机制避免重复解析
- Fallback 机制保证即使 JSONC 加载失败也能使用硬编码布局

**文件：** core/layout/LayoutAssetsLoader.kt, assets/layouts/*.jsonc, ime/MyBoardImeService.kt

### 2. 引擎绑定修复

**做了什么：**
- 在 `BuiltInManifests` 中新增 `T9_PINYIN` 和 `SHUANGPIN_ZIRAN` Schema 常量
- 修正 `DOUBLE_PINYIN` 的 layoutId 从 "shuangpin" 改为 "shuangpin_ziran"
- 新增 `T9_PINYIN` SchemaCapability，使用 table_composing 引擎

**为什么：**
- 原代码中 DOUBLE_PINYIN 引用的 "shuangpin" 布局不存在，会导致运行时错误
- T9 和双拼需要正确的引擎绑定才能正常工作

**文件：** core/contract/Schema.kt, core/state/BuiltInManifests.kt

### 3. 编码器实现

**做了什么：**
- 实现 `T9Decoder`：将数字序列转换为拼音候选组合
- 实现 `ShuangpinMapping`：自然码双拼的声母/韵母映射数据
- 实现 `ShuangpinEncoder`：将双拼输入转换为全拼序列
- 创建 `ziran_map.json` 和 `t9_keymap.json` 映射文件

**为什么：**
- T9 九键输入需要将数字序列解码为拼音组合
- 自然码双拼需要声母/韵母映射来还原全拼
- 映射数据放在 assets 中便于后续更新

**文件：** core/engine/builtin/T9Decoder.kt, core/engine/builtin/ShuangpinMapping.kt, core/engine/builtin/ShuangpinEncoder.kt, assets/engines/*.json

### 4. 词典系统（Room）

**做了什么：**
- 实现 Room 数据库：PhraseEntity（系统词典）和 UserPhraseEntity（用户词典）
- 实现 DictionaryDao 和 UserDictionaryDao
- 实现 PinyinDictionary：结合系统词典和用户词典的查找
- 实现 UserDictionary：用户自定义词条管理
- 实现 AdaptiveDictionary：词频自适应
- 实现 HotWordCalculator：基于时间衰减的热词推荐
- 实现 DictionaryUpdater：词库导入导出接口

**为什么：**
- Room 提供类型安全的数据库操作和协程支持
- 系统词典和用户词典分离，便于管理和迁移
- 词频自适应和热词推荐提升输入效率

**文件：** core/dictionary/*.kt

### 5. 面板系统

**做了什么：**
- 创建 EmojiPanel：分类展示 Emoji，支持搜索
- 创建 ClipboardPanel：剪贴板历史管理
- 创建 KaomojiPanel：颜文字分类展示
- 创建 PlaceholderPanel：未实现面板的占位
- 扩展 PanelType 枚举，新增 KAOMOJI 和 TEXT_EXPANSION
- 在 MyBoardImeService 中接入面板视图切换

**为什么：**
- 面板 Composable 已存在但未被 IME 视图渲染
- 需要根据 activePanel 状态切换显示内容
- PlaceholderPanel 为后续扩展提供统一占位

**文件：** ui/panels/EmojiPanel.kt, ui/panels/ClipboardPanel.kt, ui/panels/KaomojiPanel.kt, ui/panels/PlaceholderPanel.kt, core/contract/PanelType.kt, ime/MyBoardImeService.kt

### 6. Toolbar 增强

**做了什么：**
- 实现 ThemeToggler：夜间模式切换，协调 SettingsManager 和 ThemeResolver
- 实现 LayoutSwitcher：当前语言内 Schema 循环切换
- 在 Toolbar 中添加夜间模式按钮和设置跳转
- 修改 SettingsManager 添加 toggleTheme() 方法

**为什么：**
- 布局切换和夜间模式是高频操作，应放在 Toolbar 中
- 设置跳转需要启动 SettingsActivity
- ThemeToggler 封装了状态同步逻辑

**文件：** core/toolbar/ThemeToggler.kt, core/toolbar/LayoutSwitcher.kt, core/settings/SettingsManager.kt, ime/MyBoardImeService.kt
```

- [ ] **Step 2: Commit**

```bash
git add docs/superpowers/changelogs/2026-06-23-ime-expansion.md
git commit -m "docs: add implementation changelog for IME expansion"
```

---

## Task 16: 最终编译验证和 APK 构建

**Files:**
- None (verification only)

- [ ] **Step 1: 全量编译验证**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL, APK at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2: 验证所有布局 JSONC 可加载**

检查 `LayoutAssetsLoader` 能正确加载所有 8 个布局文件：
- qwerty（已有）
- shuangpin_ziran
- t9_chinese
- hiragana
- qwerty_dvorak
- qwerty_colemak
- qwerty_abc
- phone_dial

- [ ] **Step 3: 验证正交状态转移**

确认所有 SchemaCapability 引用的 engineId、layoutId 在注册表中存在：
- zh-CN: PINYIN → table_composing + qwerty ✓
- zh-CN: SHUANGPIN_ZIRAN → table_composing + shuangpin_ziran ✓
- zh-CN: T9_PINYIN → table_composing + t9_chinese ✓
- en-US: LATIN_DIRECT → direct + qwerty ✓
- ja-JP: ROMAJI → transliteration + qwerty ✓

- [ ] **Step 4: 最终 Commit**

```bash
git add -A
git commit -m "feat: complete IME expansion with layouts, dictionary, panels and toolbar"
```
