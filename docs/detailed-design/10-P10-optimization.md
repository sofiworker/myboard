# P10: 优化与发布 (2 周)

## 1. 目标

性能优化、测试、打包发布。

## 2. 里程碑验收标准

- [x] APK < 10MB
- [x] 冷启动 < 500ms
- [x] 触摸响应 < 16ms
- [x] 所有单元测试通过
- [x] APK 可安装使用

## 3. 详细设计

### 3.1 性能优化

#### 3.1.1 启动优化

```kotlin
// 延迟加载词典
class LazyDictLoader {
    private var primaryDict: TrieDict? = null
    private var userDict: UserDictDao? = null

    suspend fun loadPrimary() = withContext(Dispatchers.IO) {
        if (primaryDict == null) {
            primaryDict = TrieDict()
            // 加载词典文件
        }
    }

    suspend fun loadUser() = withContext(Dispatchers.IO) {
        if (userDict == null) {
            // 初始化 Room
        }
    }
}
```

#### 3.1.2 内存优化

```kotlin
// LRU 缓存主题
class ThemeCache(private val maxSize: Int = 5) {
    private val cache = LinkedHashMap<String, KeyboardTheme>(maxSize, 0.75f, true)

    fun get(id: String): KeyboardTheme? = cache[id]

    fun put(id: String, theme: KeyboardTheme) {
        if (cache.size >= maxSize) {
            cache.remove(cache.keys.first())
        }
        cache[id] = theme
    }
}
```

#### 3.1.3 渲染优化

```kotlin
// 减少 invalidate
class KeyboardRenderer {
    private var lastState: KeyboardState? = null

    fun updateState(newState: KeyboardState) {
        val oldState = lastState ?: return
        if (oldState == newState) return

        // 只更新变化的按键
        val changedKeys = findChangedKeys(oldState, newState)
        if (changedKeys.isNotEmpty()) {
            invalidateKeys(changedKeys)
        }

        lastState = newState
    }

    private fun findChangedKeys(old: KeyboardState, new: KeyboardState): Set<String> {
        val changed = mutableSetOf<String>()
        if (old.shiftState != new.shiftState) {
            changed.add("shift")
        }
        // ... 其他状态比较
        return changed
    }
}
```

### 3.2 测试

#### 3.2.1 单元测试

```kotlin
class TrieDictTest {
    @Test
    fun `prefix search returns correct results`() {
        val dict = TrieDict()
        dict.insert("language", 100)
        dict.insert("large", 90)
        dict.insert("last", 80)

        val results = dict.prefixSearch("la")
        assertEquals(3, results.size)
        assertEquals("language", results[0].word)
    }

    @Test
    fun `fuzzy search finds close matches`() {
        val dict = TrieDict()
        dict.insert("hello", 100)
        dict.insert("world", 90)

        val results = dict.fuzzySearch("helo")
        assertTrue(results.isNotEmpty())
        assertEquals("hello", results[0].word)
    }
}

class SuggestionEngineTest {
    @Test
    fun `suggest returns sorted results`() = runTest {
        val engine = SuggestionEngine(mockDict, mockUserDict, mockFreqDict)
        val results = engine.suggest("la", InputContext(), 5)
        assertTrue(results.isNotEmpty())
        assertTrue(results[0].score >= results[1].score)
    }
}

class PatchApplierTest {
    @Test
    fun `replaceKey updates key data`() {
        val base = createTestLayout()
        val patch = LayoutPatch(
            target = "test",
            ops = listOf(PatchOp.ReplaceKey("q", createTestKey("Q")))
        )

        val result = PatchApplier.apply(base, patch)
        assertEquals("Q", result.keys["q"]?.label)
    }
}
```

#### 3.2.2 Compose 测试

```kotlin
class KeyboardCanvasTest {
    @Test
    fun `keyboard renders all keys`() {
        composeTestRule.setContent {
            KeyboardScreen(layout = testLayout)
        }
        composeTestRule.onAllNodesWithTag("key").assertCountEquals(30)
    }

    @Test
    fun `shift toggles key labels`() {
        composeTestRule.setContent {
            KeyboardScreen(layout = testLayout)
        }
        composeTestRule.onNodeWithTag("shift").performClick()
        composeTestRule.onNodeWithTag("key_q").assertTextEquals("Q")
    }
}
```

### 3.3 打包发布

#### 3.3.1 ProGuard 配置

```proguard
# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep custom serializers
-keep class xyz.xiao6.myboard.model.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
```

#### 3.3.2 签名配置

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `core/common/LazyDictLoader.kt` | 延迟加载 |
| `ui/theme/ThemeCache.kt` | 主题缓存 |
| `ui/keyboard/KeyboardRenderer.kt` | 渲染优化 |
| `test/.../TrieDictTest.kt` | 词典测试 |
| `test/.../SuggestionEngineTest.kt` | 联想测试 |
| `test/.../PatchApplierTest.kt` | Patch 测试 |
| `proguard-rules.pro` | ProGuard 配置 |
| `app/build.gradle.kts` | 签名配置 |
