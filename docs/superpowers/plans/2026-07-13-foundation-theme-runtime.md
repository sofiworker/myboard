# Foundation Theme Runtime Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 交付第一期基础主题主流程：用户可在设置页选择类似 Gboard 的基础色板、系统动态色或自定义主色，并让主键盘、候选栏、Toolbar、Emoji/Symbol full-surface 和其它面板统一消费同一份主题运行时。

**Architecture:** 第一阶段只实现 `FoundationThemeSelection -> ThemeRuntimeProvider -> ThemeDoc -> ThemeResolverImpl`，不引入图片资源、Decoration、SkinLayer 导入或 Layout binding。`SettingsRepository` 保存唯一的 `AppearanceSettings` JSON，`SettingsActivity.kt` 暴露全部主题设置入口，IME Compose 主流程观察该设置并解析当前 light/dark variant。

**Tech Stack:** Kotlin、kotlinx.serialization、Room Flow、Jetpack Compose、Material 3 dynamic color、JUnit4、Gradle Android plugin。

## Global Constraints

- 当前 app 未发布，不需要兼容旧版本；可以收敛旧 `theme_mode` / `current_theme` 使用点。
- 所有主题设置必须通过 `SettingsActivity.kt` 可达，且 `SettingsRepository` 是唯一状态来源。
- 基础主题不携带资源、不改 Layout、不改变输入动作或命中区域。
- `key_default`、`key_function`、`key_action`、`key_tap` 这类 token/style id 只能集中定义在一个协议文件中；业务代码、测试和生成器必须通过枚举或常量引用，不能散落裸字符串。
- 第一阶段不实现高级 SkinLayer、Decoration、`.mybskin` 导入、主题定义器、Layout binding。
- 数据与视图分离：主题模型、颜色生成、运行时解析放在 `theme/foundation`，Compose 只消费 ViewModel 状态。
- i18n 必须覆盖所有新增设置页可见字符串。
- 不申请外部存储或其它新增权限；系统动态色只读系统可用颜色。
- 每次代码修改后必须能运行 `./gradlew.bat test` 和 `./gradlew.bat assembleDebug`。

---

## Scope

本计划是设计文档的 Phase 1。它修复当前主题主流程短板：

- `current_theme` / `theme_mode` 不再作为分散状态被 IME、Toolbar、设置页分别解释。
- `BuiltInThemes` 不再作为设置页主题列表来源；基础主题由 `FoundationPalette` 和 `ThemeColorGenerator` 生成。
- `ThemeToggler` 只修改 `AppearanceSettings.foundation.appearanceMode`，不直接写 `ThemeResolverImpl`。
- `LayoutRenderer` 仍只根据 `styleRef` 解析视觉样式，不改 `LayoutDoc`。
- Emoji/Symbol full-surface 继续通过 `PanelLayoutResolver` 和 `LayoutRenderer` 渲染，因此自动获得基础主题。
- Clipboard、Kaomoji、Locale/Layout switch、Placeholder 等非 layout-backed 面板改为消费 `ChromeColors`。

## File Structure

- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeModels.kt`
  基础主题设置模型、枚举、序列化默认值。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeTokenIds.kt`
  集中定义按键样式、反馈 token、声音资源名等协议 id，禁止业务代码直接写裸字符串。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/FoundationPalette.kt`
  内置基础色板定义，不依赖 Android 资源，方便单测。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeColorUtils.kt`
  纯 Kotlin ARGB 解析、混色、明暗调整、hex 输出。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeColorGenerator.kt`
  将 `FoundationThemeSelection + ThemeVariant` 生成现有 `ThemeDoc`。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProvider.kt`
  根据 `AppearanceSettings`、系统深浅色和动态色 seed 解析不可变 `ThemeRuntime`。
- Create `app/src/main/java/xyz/xiao6/myboard/theme/foundation/DynamicThemeSeed.kt`
  Compose/Android 侧读取 Material 3 dynamic color 主色，API < 31 返回 null。
- Modify `app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt`
  新增 `KEY_APPEARANCE_SETTINGS`、`appearanceSettings` Flow、更新方法和默认值。
- Modify `app/src/main/java/xyz/xiao6/myboard/ui/settings/SettingsViewModel.kt`
  暴露 `appearanceSettings` 与类型安全更新入口。
- Modify `app/src/main/java/xyz/xiao6/myboard/ui/settings/ThemeSettingsScreen.kt`
  从“内置主题列表”改为“基础色板 + 动态色 + 自定义色 + 明暗模式 + 按键外观”。
- Modify `app/src/main/java/xyz/xiao6/myboard/activity/SettingsActivity.kt`
  继续从主题路由进入新设置页，保持单入口。
- Modify `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`
  初始化 `ThemeRuntimeProvider`，在 Compose 主流程中观察 `AppearanceSettings`，统一更新 `ThemeResolverImpl`。
- Modify `app/src/main/java/xyz/xiao6/myboard/toolbar/ThemeToggler.kt`
  只通过 `SettingsRepository.updateAppearanceMode()` 切换 light/dark。
- Modify `app/src/main/java/xyz/xiao6/myboard/ui/keyboard/Toolbar.kt`
  `isDark` 从运行时 variant 传入；按钮集合和排序仍来自 Toolbar 设置。
- Modify `app/src/main/java/xyz/xiao6/myboard/ui/keyboard/CandidateBar.kt`
  保持候选策略不变，只消费基础主题生成的 `ChromeColors`。
- Modify `app/src/main/java/xyz/xiao6/myboard/ui/panels/*.kt`
  给非 layout-backed 面板增加 `chrome: ChromeColors` 参数，移除硬编码浅色背景。
- Modify `app/src/main/res/values/strings.xml`
  新增英文主题设置字符串。
- Modify `app/src/main/res/values-zh-rCN/strings.xml`
  新增中文主题设置字符串。
- Add tests under `app/src/test/java/xyz/xiao6/myboard/theme/foundation/`
  覆盖模型序列化、颜色生成、variant 选择、设置解析。

## Task 1: Foundation Theme Models

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeModels.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeTokenIds.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeModelsTest.kt`

**Interfaces:**
- Produces: `AppearanceSettings.default(): AppearanceSettings`
- Produces: `FoundationThemeSelection`
- Produces: `AppearanceMode`, `PaletteSource`, `FoundationPaletteId`, `KeyTreatment`, `KeyContrast`, `CornerStyle`, `ThemeVariant`
- Produces: `KeyStyleRole`, `FeedbackTokenId`
- Consumed by: Tasks 2, 3, 4, 5, 6, 7.

- [ ] **Step 1: Write failing model serialization tests**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FoundationThemeModelsTest {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Test
    fun `default appearance settings encode and decode`() {
        val encoded = json.encodeToString(AppearanceSettings.default())
        val decoded = json.decodeFromString<AppearanceSettings>(encoded)

        assertEquals(PaletteSource.PRESET, decoded.foundation.paletteSource)
        assertEquals(FoundationPaletteId.GBOARD_BLUE, decoded.foundation.paletteId)
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, decoded.foundation.appearanceMode)
        assertEquals(KeyTreatment.FILLED, decoded.foundation.keyTreatment)
        assertNull(decoded.skinThemeId)
    }

    @Test
    fun `custom seed source keeps selected color`() {
        val settings = AppearanceSettings(
            foundation = FoundationThemeSelection(
                paletteSource = PaletteSource.CUSTOM_SEED,
                customSeedColor = "#00A86B",
                keyTreatment = KeyTreatment.OUTLINED,
                cornerStyle = CornerStyle.PILL
            )
        )

        val decoded = json.decodeFromString<AppearanceSettings>(json.encodeToString(settings))

        assertEquals(PaletteSource.CUSTOM_SEED, decoded.foundation.paletteSource)
        assertEquals("#00A86B", decoded.foundation.customSeedColor)
        assertEquals(KeyTreatment.OUTLINED, decoded.foundation.keyTreatment)
        assertEquals(CornerStyle.PILL, decoded.foundation.cornerStyle)
    }

    @Test
    fun `style and feedback ids are referenced through protocol enums`() {
        assertEquals(KeyStyleRole.DEFAULT, KeyStyleRole.fromRef(KeyStyleRole.DEFAULT.ref))
        assertEquals(KeyStyleRole.ACTION, KeyStyleRole.fromRef(KeyStyleRole.ACTION.ref))
        assertEquals(FeedbackTokenId.KEY_TAP.ref, FeedbackTokenId.KEY_TAP.soundResName)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.FoundationThemeModelsTest"`

Expected: FAIL because `AppearanceSettings` and related enums do not exist.

- [ ] **Step 3: Add model file**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import kotlinx.serialization.Serializable

@Serializable
data class AppearanceSettings(
    val foundation: FoundationThemeSelection = FoundationThemeSelection(),
    val skinThemeId: String? = null
) {
    companion object {
        fun default(): AppearanceSettings = AppearanceSettings()
    }
}

@Serializable
data class FoundationThemeSelection(
    val paletteSource: PaletteSource = PaletteSource.PRESET,
    val paletteId: FoundationPaletteId = FoundationPaletteId.GBOARD_BLUE,
    val customSeedColor: String? = null,
    val keyTreatment: KeyTreatment = KeyTreatment.FILLED,
    val keyContrast: KeyContrast = KeyContrast.NORMAL,
    val cornerStyle: CornerStyle = CornerStyle.ROUNDED,
    val appearanceMode: AppearanceMode = AppearanceMode.FOLLOW_SYSTEM
)

@Serializable
enum class PaletteSource {
    PRESET,
    SYSTEM_DYNAMIC,
    CUSTOM_SEED
}

@Serializable
enum class FoundationPaletteId {
    GBOARD_BLUE,
    MINT,
    ROSE,
    VIOLET,
    GRAPHITE
}

@Serializable
enum class KeyTreatment {
    FILLED,
    OUTLINED,
    BORDERLESS
}

@Serializable
enum class KeyContrast {
    NORMAL,
    HIGH
}

@Serializable
enum class CornerStyle {
    COMPACT,
    ROUNDED,
    PILL
}

@Serializable
enum class AppearanceMode {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK
}

enum class ThemeVariant {
    LIGHT,
    DARK
}
```

- [ ] **Step 4: Add token id protocol file**

```kotlin
package xyz.xiao6.myboard.theme.foundation

enum class KeyStyleRole(val ref: String) {
    DEFAULT("key_default"),
    FUNCTION("key_function"),
    ACTION("key_action"),
    SPACE("key_space"),
    CANDIDATE("key_candidate");

    companion object {
        val fallbackRef: String
            get() = DEFAULT.ref

        fun fromRef(ref: String): KeyStyleRole? =
            values().firstOrNull { it.ref == ref }
    }
}

enum class FeedbackTokenId(
    val ref: String,
    val soundResName: String = ref
) {
    KEY_TAP("key_tap"),
    KEY_LONG_PRESS("key_long_press"),
    KEY_ACTION("key_action"),
    KEY_SPACE("key_space")
}
```

Only this protocol file may contain the wire strings. Later tasks must use `KeyStyleRole.DEFAULT.ref`, `KeyStyleRole.FUNCTION.ref`, `FeedbackTokenId.KEY_TAP.ref`, and equivalent enum references.

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.FoundationThemeModelsTest"`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeModels.kt app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeTokenIds.kt app/src/test/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeModelsTest.kt
git commit -m "feat: add foundation theme settings model"
```

## Task 2: Palette And Color Generator

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/FoundationPalette.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeColorUtils.kt`
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeColorGenerator.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/theme/foundation/ThemeColorGeneratorTest.kt`

**Interfaces:**
- Consumes: `FoundationThemeSelection`, `ThemeVariant`
- Produces: `FoundationPalette.resolve(selection: FoundationThemeSelection, dynamicSeedColor: String?): FoundationPalette`
- Produces: `ThemeColorGenerator.generate(selection: FoundationThemeSelection, variant: ThemeVariant, dynamicSeedColor: String? = null): ThemeDoc`
- Consumed by: Task 3 runtime provider and Task 5 setting previews.

- [ ] **Step 1: Write failing generator tests**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorGeneratorTest {
    private val generator = ThemeColorGenerator()

    @Test
    fun `default light palette generates readable filled keys`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(),
            variant = ThemeVariant.LIGHT
        )

        assertFalse(doc.dark)
        assertEquals("foundation_gboard_blue_light", doc.id)
        assertEquals("#F1F3F4", doc.colors.background)
        assertEquals("#FFFFFF", doc.keyStyles.getValue(KeyStyleRole.DEFAULT.ref).background)
        assertEquals("#1A73E8", doc.colors.candidateHighlight)
    }

    @Test
    fun `dark variant uses dark surfaces and light text`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(),
            variant = ThemeVariant.DARK
        )

        assertTrue(doc.dark)
        assertEquals("#1E1E1E", doc.colors.background)
        assertEquals("#E8EAED", doc.colors.keyText)
    }

    @Test
    fun `outlined treatment keeps transparent default key and visible border via function colors`() {
        val doc = generator.generate(
            selection = FoundationThemeSelection(keyTreatment = KeyTreatment.OUTLINED),
            variant = ThemeVariant.LIGHT
        )

        assertEquals("#00FFFFFF", doc.keyStyles.getValue(KeyStyleRole.DEFAULT.ref).background)
        assertEquals("#E8EAED", doc.colors.keyFunction)
    }

    @Test
    fun `custom seed changes accent without changing layout related fields`() {
        val blue = generator.generate(FoundationThemeSelection(), ThemeVariant.LIGHT)
        val green = generator.generate(
            FoundationThemeSelection(
                paletteSource = PaletteSource.CUSTOM_SEED,
                customSeedColor = "#00A86B"
            ),
            ThemeVariant.LIGHT
        )

        assertNotEquals(blue.colors.candidateHighlight, green.colors.candidateHighlight)
        assertEquals(blue.keyStyles.keys, green.keyStyles.keys)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.ThemeColorGeneratorTest"`

Expected: FAIL because palette and generator classes do not exist.

- [ ] **Step 3: Add palette definitions**

```kotlin
package xyz.xiao6.myboard.theme.foundation

data class FoundationPalette(
    val id: FoundationPaletteId,
    val seedColor: String,
    val lightAccent: String,
    val darkAccent: String,
    val titleKey: String
) {
    companion object {
        val all: List<FoundationPalette> = listOf(
            FoundationPalette(FoundationPaletteId.GBOARD_BLUE, "#1A73E8", "#1A73E8", "#8AB4F8", "theme_palette_gboard_blue"),
            FoundationPalette(FoundationPaletteId.MINT, "#00A86B", "#00875A", "#65D6A4", "theme_palette_mint"),
            FoundationPalette(FoundationPaletteId.ROSE, "#D9306E", "#C5225E", "#F28BAE", "theme_palette_rose"),
            FoundationPalette(FoundationPaletteId.VIOLET, "#7E57C2", "#6D46B3", "#B69DF8", "theme_palette_violet"),
            FoundationPalette(FoundationPaletteId.GRAPHITE, "#5F6368", "#4E5358", "#BDC1C6", "theme_palette_graphite")
        )

        fun byId(id: FoundationPaletteId): FoundationPalette =
            all.firstOrNull { it.id == id } ?: all.first()

        fun resolve(selection: FoundationThemeSelection, dynamicSeedColor: String?): FoundationPalette {
            return when (selection.paletteSource) {
                PaletteSource.PRESET -> byId(selection.paletteId)
                PaletteSource.SYSTEM_DYNAMIC -> {
                    val seed = dynamicSeedColor?.takeIf { it.isNotBlank() } ?: byId(selection.paletteId).seedColor
                    fromSeed(FoundationPaletteId.GBOARD_BLUE, seed, "theme_palette_system_dynamic")
                }
                PaletteSource.CUSTOM_SEED -> {
                    val seed = selection.customSeedColor?.takeIf { it.isNotBlank() } ?: byId(selection.paletteId).seedColor
                    fromSeed(selection.paletteId, seed, "theme_palette_custom")
                }
            }
        }

        private fun fromSeed(id: FoundationPaletteId, seed: String, titleKey: String): FoundationPalette =
            FoundationPalette(
                id = id,
                seedColor = seed,
                lightAccent = seed,
                darkAccent = ThemeColorUtils.mix(seed, "#FFFFFF", 0.45f),
                titleKey = titleKey
            )
    }
}
```

- [ ] **Step 4: Add color utility**

```kotlin
package xyz.xiao6.myboard.theme.foundation

object ThemeColorUtils {
    fun normalizeHex(input: String, fallback: String = "#1A73E8"): String {
        val raw = input.trim()
        val body = raw.removePrefix("#")
        val normalized = when (body.length) {
            6 -> "FF$body"
            8 -> body
            else -> return fallback
        }
        return if (normalized.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
            "#${normalized.takeLast(6).uppercase()}"
        } else {
            fallback
        }
    }

    fun withAlpha(hex: String, alpha: Int): String {
        val rgb = normalizeHex(hex).removePrefix("#")
        return "#${alpha.coerceIn(0, 255).toString(16).padStart(2, '0').uppercase()}$rgb"
    }

    fun mix(fromHex: String, toHex: String, amount: Float): String {
        val from = parseRgb(normalizeHex(fromHex))
        val to = parseRgb(normalizeHex(toHex))
        val t = amount.coerceIn(0f, 1f)
        val r = (from[0] + (to[0] - from[0]) * t).toInt().coerceIn(0, 255)
        val g = (from[1] + (to[1] - from[1]) * t).toInt().coerceIn(0, 255)
        val b = (from[2] + (to[2] - from[2]) * t).toInt().coerceIn(0, 255)
        return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
    }

    private fun parseRgb(hex: String): IntArray {
        val body = normalizeHex(hex).removePrefix("#")
        return intArrayOf(
            body.substring(0, 2).toInt(16),
            body.substring(2, 4).toInt(16),
            body.substring(4, 6).toInt(16)
        )
    }
}
```

- [ ] **Step 5: Add generator**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import xyz.xiao6.myboard.theme.FeedbackSection
import xyz.xiao6.myboard.theme.HapticTokenDef
import xyz.xiao6.myboard.theme.KeyStyleDef
import xyz.xiao6.myboard.theme.SoundTokenDef
import xyz.xiao6.myboard.theme.ThemeColors
import xyz.xiao6.myboard.theme.ThemeDoc

class ThemeColorGenerator {
    fun generate(
        selection: FoundationThemeSelection,
        variant: ThemeVariant,
        dynamicSeedColor: String? = null
    ): ThemeDoc {
        val palette = FoundationPalette.resolve(selection, dynamicSeedColor)
        val dark = variant == ThemeVariant.DARK
        val accent = if (dark) palette.darkAccent else palette.lightAccent
        val corner = cornerRadius(selection.cornerStyle)
        val highContrast = selection.keyContrast == KeyContrast.HIGH

        val colors = if (dark) {
            ThemeColors(
                background = "#1E1E1E",
                surface = "#2D2D2D",
                keyDefault = keyBackground(selection.keyTreatment, dark),
                keyPressed = "#4A4A4A",
                keyText = "#E8EAED",
                keyHint = if (highContrast) "#BDC1C6" else "#8E8E93",
                keyFunction = "#3C3C3C",
                keyFunctionPressed = "#4A4A4A",
                keyFunctionText = "#E8EAED",
                keyAction = accent,
                keyActionPressed = ThemeColorUtils.mix(accent, "#000000", 0.18f),
                keyActionText = "#1E1E1E",
                candidateBackground = "#2D2D2D",
                candidateText = "#E8EAED",
                candidateHighlight = accent
            )
        } else {
            ThemeColors(
                background = "#F1F3F4",
                surface = "#FFFFFF",
                keyDefault = keyBackground(selection.keyTreatment, dark),
                keyPressed = "#E8EAED",
                keyText = "#202124",
                keyHint = if (highContrast) "#5F6368" else "#8E8E93",
                keyFunction = "#E8EAED",
                keyFunctionPressed = "#DADCE0",
                keyFunctionText = "#202124",
                keyAction = accent,
                keyActionPressed = ThemeColorUtils.mix(accent, "#000000", 0.20f),
                keyActionText = "#FFFFFF",
                candidateBackground = "#FFFFFF",
                candidateText = "#202124",
                candidateHighlight = accent
            )
        }

        return ThemeDoc(
            id = "foundation_${palette.id.name.lowercase()}_${variant.name.lowercase()}",
            name = palette.titleKey,
            dark = dark,
            colors = colors,
            keyStyles = keyStyles(colors, selection.keyTreatment, corner),
            feedback = defaultFeedback()
        )
    }

    private fun keyBackground(treatment: KeyTreatment, dark: Boolean): String {
        return when (treatment) {
            KeyTreatment.FILLED -> if (dark) "#3C3C3C" else "#FFFFFF"
            KeyTreatment.OUTLINED -> if (dark) "#003C3C3C" else "#00FFFFFF"
            KeyTreatment.BORDERLESS -> "#00000000"
        }
    }

    private fun cornerRadius(style: CornerStyle): Float {
        return when (style) {
            CornerStyle.COMPACT -> 6f
            CornerStyle.ROUNDED -> 10f
            CornerStyle.PILL -> 20f
        }
    }

    private fun keyStyles(colors: ThemeColors, treatment: KeyTreatment, corner: Float): Map<String, KeyStyleDef> {
        val defaultCorner = if (treatment == KeyTreatment.BORDERLESS) 6f else corner
        return mapOf(
            KeyStyleRole.DEFAULT.ref to KeyStyleDef(
                background = colors.keyDefault,
                pressedBackground = colors.keyPressed,
                textColor = colors.keyText,
                pressedTextColor = colors.keyText,
                fontSize = 18f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyText
            ),
            KeyStyleRole.FUNCTION.ref to KeyStyleDef(
                background = if (treatment == KeyTreatment.BORDERLESS) "#00000000" else colors.keyFunction,
                pressedBackground = colors.keyFunctionPressed,
                textColor = colors.keyFunctionText,
                pressedTextColor = colors.keyFunctionText,
                fontSize = 14f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyFunctionText
            ),
            KeyStyleRole.ACTION.ref to KeyStyleDef(
                background = colors.keyAction,
                pressedBackground = colors.keyActionPressed,
                textColor = colors.keyActionText,
                pressedTextColor = colors.keyActionText,
                fontSize = 14f,
                cornerRadius = defaultCorner,
                iconTint = colors.keyActionText
            ),
            KeyStyleRole.SPACE.ref to KeyStyleDef(
                background = colors.keyDefault,
                pressedBackground = colors.keyPressed,
                textColor = colors.keyText,
                pressedTextColor = colors.keyText,
                fontSize = 14f,
                cornerRadius = (defaultCorner * 2f).coerceAtMost(24f),
                iconTint = colors.keyText
            ),
            KeyStyleRole.CANDIDATE.ref to KeyStyleDef(
                background = colors.candidateBackground,
                pressedBackground = colors.keyPressed,
                textColor = colors.candidateText,
                pressedTextColor = colors.candidateHighlight,
                fontSize = 16f,
                cornerRadius = 4f,
                iconTint = colors.candidateText
            )
        )
    }

    private fun defaultFeedback(): FeedbackSection =
        FeedbackSection(
            haptic = mapOf(
                FeedbackTokenId.KEY_TAP.ref to HapticTokenDef(durationMs = 10, amplitude = 64),
                FeedbackTokenId.KEY_LONG_PRESS.ref to HapticTokenDef(durationMs = 30, amplitude = 128),
                FeedbackTokenId.KEY_ACTION.ref to HapticTokenDef(durationMs = 15, amplitude = 96)
            ),
            sound = mapOf(
                FeedbackTokenId.KEY_TAP.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_TAP.soundResName, volume = 0.3f),
                FeedbackTokenId.KEY_ACTION.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_ACTION.soundResName, volume = 0.5f),
                FeedbackTokenId.KEY_SPACE.ref to SoundTokenDef(soundResName = FeedbackTokenId.KEY_SPACE.soundResName, volume = 0.2f)
            )
        )
}
```

- [ ] **Step 6: Run generator tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.ThemeColorGeneratorTest"`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/theme/foundation app/src/test/java/xyz/xiao6/myboard/theme/foundation
git commit -m "feat: generate foundation theme colors"
```

## Task 3: Theme Runtime Provider

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProvider.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProviderTest.kt`

**Interfaces:**
- Consumes: `AppearanceSettings`, `ThemeColorGenerator`
- Produces: `ThemeRuntimeProvider.resolve(settings: AppearanceSettings, systemDark: Boolean, dynamicSeedColor: String? = null): ThemeRuntime`
- Produces: `ThemeRuntime.doc: ThemeDoc`, `ThemeRuntime.variant: ThemeVariant`
- Consumed by: Tasks 6 and 7.

- [ ] **Step 1: Write failing runtime provider tests**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRuntimeProviderTest {
    private val provider = ThemeRuntimeProvider()

    @Test
    fun `follow system selects dark when system is dark`() {
        val runtime = provider.resolve(AppearanceSettings.default(), systemDark = true)

        assertEquals(ThemeVariant.DARK, runtime.variant)
        assertTrue(runtime.doc.dark)
        assertNull(runtime.skinThemeId)
    }

    @Test
    fun `explicit light ignores dark system`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(appearanceMode = AppearanceMode.LIGHT)
            ),
            systemDark = true
        )

        assertEquals(ThemeVariant.LIGHT, runtime.variant)
        assertFalse(runtime.doc.dark)
    }

    @Test
    fun `system dynamic falls back to preset when dynamic seed is absent`() {
        val runtime = provider.resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(paletteSource = PaletteSource.SYSTEM_DYNAMIC)
            ),
            systemDark = false,
            dynamicSeedColor = null
        )

        assertEquals("#1A73E8", runtime.doc.colors.candidateHighlight)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.ThemeRuntimeProviderTest"`

Expected: FAIL because `ThemeRuntimeProvider` does not exist.

- [ ] **Step 3: Add runtime provider**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import xyz.xiao6.myboard.theme.ThemeDoc

data class ThemeRuntime(
    val appearanceSettings: AppearanceSettings,
    val variant: ThemeVariant,
    val doc: ThemeDoc,
    val skinThemeId: String? = appearanceSettings.skinThemeId
)

class ThemeRuntimeProvider(
    private val colorGenerator: ThemeColorGenerator = ThemeColorGenerator()
) {
    fun resolve(
        settings: AppearanceSettings,
        systemDark: Boolean,
        dynamicSeedColor: String? = null
    ): ThemeRuntime {
        val variant = when (settings.foundation.appearanceMode) {
            AppearanceMode.FOLLOW_SYSTEM -> if (systemDark) ThemeVariant.DARK else ThemeVariant.LIGHT
            AppearanceMode.LIGHT -> ThemeVariant.LIGHT
            AppearanceMode.DARK -> ThemeVariant.DARK
        }
        val doc = colorGenerator.generate(
            selection = settings.foundation,
            variant = variant,
            dynamicSeedColor = dynamicSeedColor
        )
        return ThemeRuntime(
            appearanceSettings = settings,
            variant = variant,
            doc = doc,
            skinThemeId = settings.skinThemeId
        )
    }
}
```

- [ ] **Step 4: Run runtime tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.ThemeRuntimeProviderTest"`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProvider.kt app/src/test/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProviderTest.kt
git commit -m "feat: resolve foundation theme runtime"
```

## Task 4: SettingsRepository Appearance Source

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/data/repository/SettingsRepositoryAppearanceTest.kt`

**Interfaces:**
- Consumes: `AppearanceSettings`
- Produces: `SettingsRepository.KEY_APPEARANCE_SETTINGS`
- Produces: `SettingsRepository.appearanceSettings: Flow<AppearanceSettings>`
- Produces: `suspend fun getAppearanceSettings(): AppearanceSettings`
- Produces: `suspend fun updateAppearanceSettings(settings: AppearanceSettings)`
- Produces: `suspend fun updateFoundationTheme(transform: (FoundationThemeSelection) -> FoundationThemeSelection)`
- Produces: `suspend fun updateAppearanceMode(mode: AppearanceMode)`
- Consumed by: Tasks 5, 6, 7.

- [ ] **Step 1: Write failing repository tests**

```kotlin
package xyz.xiao6.myboard.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.data.dao.SettingsDao
import xyz.xiao6.myboard.data.entity.SettingsEntity
import xyz.xiao6.myboard.data.entity.ToolbarItemEntity
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.FoundationPaletteId
import xyz.xiao6.myboard.theme.foundation.FoundationThemeSelection

class SettingsRepositoryAppearanceTest {
    @Test
    fun `missing appearance setting returns default`() = runBlocking {
        val repo = SettingsRepository(FakeSettingsDao())

        val settings = repo.getAppearanceSettings()

        assertEquals(FoundationPaletteId.GBOARD_BLUE, settings.foundation.paletteId)
        assertEquals(AppearanceMode.FOLLOW_SYSTEM, settings.foundation.appearanceMode)
    }

    @Test
    fun `update appearance settings persists one json value`() = runBlocking {
        val dao = FakeSettingsDao()
        val repo = SettingsRepository(dao)

        repo.updateAppearanceSettings(
            repo.getAppearanceSettings().copy(
                foundation = FoundationThemeSelection(
                    paletteId = FoundationPaletteId.MINT,
                    appearanceMode = AppearanceMode.DARK
                )
            )
        )

        val stored = dao.getSetting(SettingsRepository.KEY_APPEARANCE_SETTINGS)
        val observed = repo.appearanceSettings.first()
        requireNotNull(stored)
        assertEquals(FoundationPaletteId.MINT, observed.foundation.paletteId)
        assertEquals(AppearanceMode.DARK, observed.foundation.appearanceMode)
    }

    @Test
    fun `update appearance mode only changes mode`() = runBlocking {
        val repo = SettingsRepository(FakeSettingsDao())
        repo.updateAppearanceSettings(
            repo.getAppearanceSettings().copy(
                foundation = FoundationThemeSelection(paletteId = FoundationPaletteId.ROSE)
            )
        )

        repo.updateAppearanceMode(AppearanceMode.LIGHT)

        val settings = repo.getAppearanceSettings()
        assertEquals(FoundationPaletteId.ROSE, settings.foundation.paletteId)
        assertEquals(AppearanceMode.LIGHT, settings.foundation.appearanceMode)
    }

    private class FakeSettingsDao : SettingsDao {
        private val values = MutableStateFlow<Map<String, String>>(emptyMap())
        private val toolbar = MutableStateFlow<List<ToolbarItemEntity>>(emptyList())

        override fun getAllSettings(): Flow<List<SettingsEntity>> =
            values.map { map -> map.map { SettingsEntity(it.key, it.value) } }

        override suspend fun getSetting(key: String): String? = values.value[key]

        override fun observeSetting(key: String): Flow<String?> = values.map { it[key] }

        override suspend fun upsertSetting(entity: SettingsEntity) {
            values.value = values.value + (entity.key to entity.stringValue)
        }

        override suspend fun getSettingCount(): Int = values.value.size

        override fun getToolbarItems(): Flow<List<ToolbarItemEntity>> = toolbar

        override suspend fun upsertToolbarItem(entity: ToolbarItemEntity) {
            toolbar.value = toolbar.value + entity
        }

        override suspend fun deleteToolbarItem(type: String) {
            toolbar.value = toolbar.value.filterNot { it.type == type }
        }

        override suspend fun deleteAllToolbarItems() {
            toolbar.value = emptyList()
        }

        override suspend fun getToolbarItemCount(): Int = toolbar.value.size
    }
}
```

- [ ] **Step 2: Run repository tests to verify failure**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.data.repository.SettingsRepositoryAppearanceTest"`

Expected: FAIL because repository appearance APIs do not exist.

- [ ] **Step 3: Add serialization helpers and Flow**

Modify `SettingsRepository.kt` imports:

```kotlin
import xyz.xiao6.myboard.theme.foundation.AppearanceMode
import xyz.xiao6.myboard.theme.foundation.AppearanceSettings
import xyz.xiao6.myboard.theme.foundation.FoundationThemeSelection
```

Add inside `SettingsRepository`:

```kotlin
private val settingsJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

val appearanceSettings: Flow<AppearanceSettings> =
    observeSetting(KEY_APPEARANCE_SETTINGS).map { raw -> decodeAppearanceSettings(raw) }

suspend fun getAppearanceSettings(): AppearanceSettings =
    decodeAppearanceSettings(getSetting(KEY_APPEARANCE_SETTINGS))

suspend fun updateAppearanceSettings(settings: AppearanceSettings) {
    updateSetting(KEY_APPEARANCE_SETTINGS, settingsJson.encodeToString(settings))
}

suspend fun updateFoundationTheme(transform: (FoundationThemeSelection) -> FoundationThemeSelection) {
    val current = getAppearanceSettings()
    updateAppearanceSettings(current.copy(foundation = transform(current.foundation)))
}

suspend fun updateAppearanceMode(mode: AppearanceMode) {
    updateFoundationTheme { it.copy(appearanceMode = mode) }
}

private fun decodeAppearanceSettings(raw: String?): AppearanceSettings {
    return raw?.takeIf { it.isNotBlank() }?.let { value ->
        runCatching { settingsJson.decodeFromString<AppearanceSettings>(value) }.getOrNull()
    } ?: AppearanceSettings.default()
}
```

Add companion object key and replace default theme keys:

```kotlin
const val KEY_APPEARANCE_SETTINGS = "appearance_settings"
```

In `DEFAULT_SETTINGS`, replace `"theme_mode"` and `"current_theme"` entries with:

```kotlin
KEY_APPEARANCE_SETTINGS to Json { encodeDefaults = true }.encodeToString(AppearanceSettings.default()),
```

- [ ] **Step 4: Run repository tests**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.data.repository.SettingsRepositoryAppearanceTest"`

Expected: PASS.

- [ ] **Step 5: Search old setting usage**

Run: `rg "\"theme_mode\"|\"current_theme\"" app/src/main/java`

Expected: matches remain in files not yet migrated: `ThemeSettingsScreen.kt`, `MyBoardImeService.kt`, `ThemeToggler.kt`. These are handled in Tasks 5-7.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/data/repository/SettingsRepository.kt app/src/test/java/xyz/xiao6/myboard/data/repository/SettingsRepositoryAppearanceTest.kt
git commit -m "feat: store appearance settings as single source"
```

## Task 5: Settings ViewModel And Theme Settings UI

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/SettingsViewModel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/settings/ThemeSettingsScreen.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Interfaces:**
- Consumes: `SettingsRepository.appearanceSettings`
- Produces: `SettingsViewModel.appearanceSettings`
- Produces: `SettingsViewModel.updateAppearanceSettings(settings: AppearanceSettings)`
- Produces: `SettingsViewModel.updateFoundationTheme(transform: (FoundationThemeSelection) -> FoundationThemeSelection)`
- UI still entered from `SettingsActivity.kt` route `theme`.
- Consumed by: User settings flow and Task 6 IME runtime.

- [ ] **Step 1: Add ViewModel tests by compiling against the new methods**

This project currently has no ViewModel unit harness. Use compile verification for this task, then cover persistence in Task 4 and runtime in Task 6.

Run before modification: `.\gradlew.bat compileDebugKotlin`

Expected: PASS before edits.

- [ ] **Step 2: Add ViewModel appearance API**

Modify `SettingsViewModel.kt` imports:

```kotlin
import xyz.xiao6.myboard.theme.foundation.AppearanceSettings
import xyz.xiao6.myboard.theme.foundation.FoundationThemeSelection
```

Add inside `SettingsViewModel`:

```kotlin
val appearanceSettings: StateFlow<AppearanceSettings> = repo.appearanceSettings
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppearanceSettings.default())

fun updateAppearanceSettings(settings: AppearanceSettings) {
    viewModelScope.launch { repo.updateAppearanceSettings(settings) }
}

fun updateFoundationTheme(transform: (FoundationThemeSelection) -> FoundationThemeSelection) {
    viewModelScope.launch { repo.updateFoundationTheme(transform) }
}
```

- [ ] **Step 3: Add i18n strings**

Add to `values/strings.xml`:

```xml
<string name="settings_theme_palette">Color palette</string>
<string name="settings_theme_palette_gboard_blue">Default blue</string>
<string name="settings_theme_palette_mint">Mint</string>
<string name="settings_theme_palette_rose">Rose</string>
<string name="settings_theme_palette_violet">Violet</string>
<string name="settings_theme_palette_graphite">Graphite</string>
<string name="settings_theme_palette_system_dynamic">System colors</string>
<string name="settings_theme_palette_system_dynamic_desc">Use Android dynamic colors when available</string>
<string name="settings_theme_palette_custom">Custom color</string>
<string name="settings_theme_palette_custom_desc">Use a saved custom seed color</string>
<string name="settings_theme_key_treatment">Key style</string>
<string name="settings_theme_key_treatment_filled">Filled</string>
<string name="settings_theme_key_treatment_outlined">Outlined</string>
<string name="settings_theme_key_treatment_borderless">Borderless</string>
<string name="settings_theme_key_contrast">High contrast keys</string>
<string name="settings_theme_corner_style">Corner style</string>
<string name="settings_theme_corner_compact">Compact</string>
<string name="settings_theme_corner_rounded">Rounded</string>
<string name="settings_theme_corner_pill">Pill</string>
```

Add to `values-zh-rCN/strings.xml`:

```xml
<string name="settings_theme_palette">颜色方案</string>
<string name="settings_theme_palette_gboard_blue">默认蓝</string>
<string name="settings_theme_palette_mint">薄荷绿</string>
<string name="settings_theme_palette_rose">玫瑰红</string>
<string name="settings_theme_palette_violet">紫罗兰</string>
<string name="settings_theme_palette_graphite">石墨灰</string>
<string name="settings_theme_palette_system_dynamic">系统颜色</string>
<string name="settings_theme_palette_system_dynamic_desc">可用时使用 Android 动态颜色</string>
<string name="settings_theme_palette_custom">自定义颜色</string>
<string name="settings_theme_palette_custom_desc">使用已保存的自定义种子色</string>
<string name="settings_theme_key_treatment">按键样式</string>
<string name="settings_theme_key_treatment_filled">填充</string>
<string name="settings_theme_key_treatment_outlined">描边</string>
<string name="settings_theme_key_treatment_borderless">无边框</string>
<string name="settings_theme_key_contrast">高对比度按键</string>
<string name="settings_theme_corner_style">圆角样式</string>
<string name="settings_theme_corner_compact">紧凑</string>
<string name="settings_theme_corner_rounded">圆润</string>
<string name="settings_theme_corner_pill">胶囊</string>
```

- [ ] **Step 4: Replace theme list UI with foundation controls**

In `ThemeSettingsScreen.kt`:

- Remove `BuiltInThemes` import and `ThemeDoc`-based list.
- Collect `val appearance by viewModel.appearanceSettings.collectAsState()`.
- Use `appearance.foundation.appearanceMode` for light/dark/follow system.
- Use `FoundationPalette.all` for preset palette swatches.
- Add rows for `SYSTEM_DYNAMIC`, `CUSTOM_SEED`, `KeyTreatment`, `KeyContrast`, `CornerStyle`.
- Keep all updates through `viewModel.updateFoundationTheme { ... }`.

Use these helper signatures in the same file:

```kotlin
@Composable
private fun FoundationPaletteItem(
    label: String,
    seedColor: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false
)

@Composable
private fun FoundationChoiceItem(
    label: String,
    description: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false
)

@Composable
private fun ThemeSeedSwatch(seedColor: String)
```

The appearance mode update code must use enum values:

```kotlin
onClick = {
    viewModel.updateFoundationTheme { it.copy(appearanceMode = AppearanceMode.FOLLOW_SYSTEM) }
}
```

The preset palette update code must use:

```kotlin
onClick = {
    viewModel.updateFoundationTheme {
        it.copy(
            paletteSource = PaletteSource.PRESET,
            paletteId = palette.id
        )
    }
}
```

The system dynamic update code must use:

```kotlin
onClick = {
    viewModel.updateFoundationTheme { it.copy(paletteSource = PaletteSource.SYSTEM_DYNAMIC) }
}
```

The custom color row in this task uses the existing saved `customSeedColor` or `#1A73E8`. A color picker is not part of Phase 1:

```kotlin
onClick = {
    viewModel.updateFoundationTheme {
        it.copy(
            paletteSource = PaletteSource.CUSTOM_SEED,
            customSeedColor = it.customSeedColor ?: "#1A73E8"
        )
    }
}
```

- [ ] **Step 5: Compile UI changes**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: PASS.

- [ ] **Step 6: Verify old theme list is no longer used by settings**

Run: `rg "BuiltInThemes|current_theme|theme_mode" app/src/main/java/xyz/xiao6/myboard/ui/settings`

Expected: no matches in `ThemeSettingsScreen.kt`.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/settings/SettingsViewModel.kt app/src/main/java/xyz/xiao6/myboard/ui/settings/ThemeSettingsScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-zh-rCN/strings.xml
git commit -m "feat: expose foundation theme settings"
```

## Task 6: IME Runtime Integration

**Files:**
- Create: `app/src/main/java/xyz/xiao6/myboard/theme/foundation/DynamicThemeSeed.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProviderTest.kt`

**Interfaces:**
- Consumes: `SettingsRepository.appearanceSettings`
- Consumes: `DynamicThemeSeed.currentSeedColor(): String?`
- Produces: IME runtime updates through `themeResolver.setTheme(runtime.doc)`
- Keeps: `PanelLayoutResolver.layoutIdFor()` unchanged; Emoji/Symbol layout IDs remain `emoji_full_surface` and `symbols_full_surface`.

- [ ] **Step 1: Extend runtime test for dynamic seed**

Append to `ThemeRuntimeProviderTest.kt`:

```kotlin
@Test
fun `system dynamic uses provided dynamic seed`() {
    val runtime = provider.resolve(
        AppearanceSettings(
            foundation = FoundationThemeSelection(paletteSource = PaletteSource.SYSTEM_DYNAMIC)
        ),
        systemDark = false,
        dynamicSeedColor = "#FF5722"
    )

    assertEquals("#FF5722", runtime.doc.colors.candidateHighlight)
}
```

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.ThemeRuntimeProviderTest"`

Expected: PASS if Task 2 fallback and dynamic seed logic are implemented as specified.

- [ ] **Step 2: Add Android dynamic color seed provider**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import android.os.Build
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

object DynamicThemeSeed {
    @Composable
    fun currentSeedColor(): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val scheme = dynamicLightColorScheme(LocalContext.current)
        return "#${scheme.primary.toArgb().and(0x00FFFFFF).toString(16).padStart(6, '0').uppercase()}"
    }
}
```

- [ ] **Step 3: Initialize runtime provider in IME service**

In `MyBoardImeService.kt`, add field:

```kotlin
private lateinit var themeRuntimeProvider: ThemeRuntimeProvider
```

In `initCoreComponents()`, replace the current `runBlocking` theme initialization block with:

```kotlin
themeRuntimeProvider = ThemeRuntimeProvider()
themeResolver = ThemeResolverImpl(themeRuntimeProvider.resolve(AppearanceSettings.default(), systemDark = false).doc)
```

Remove the `savedThemeMode` / `BuiltInThemes.dark` / `BuiltInThemes.light` branch from IME initialization.

- [ ] **Step 4: Resolve runtime inside Compose**

Inside `setContent` in `onCreateInputView()`, after collecting keyboard context, add:

```kotlin
val appearanceSettings by settingsRepository.appearanceSettings.collectAsState(
    initial = AppearanceSettings.default()
)
val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
val dynamicSeedColor = DynamicThemeSeed.currentSeedColor()
val themeRuntime = remember(appearanceSettings, systemDark, dynamicSeedColor) {
    themeRuntimeProvider.resolve(
        settings = appearanceSettings,
        systemDark = systemDark,
        dynamicSeedColor = dynamicSeedColor
    )
}
SideEffect {
    themeResolver.setTheme(themeRuntime.doc)
}
```

Replace:

```kotlin
val isDark = themeResolver.isDark()
```

with:

```kotlin
val isDark = themeRuntime.variant == ThemeVariant.DARK
```

Keep `val chrome = themeResolver.resolveChromeColors()` after the `SideEffect` block in the same composition scope.

- [ ] **Step 5: Ensure existing settings map remains for keyboard height only**

Keep:

```kotlin
val allSettings by settingsRepository.settings.collectAsState(initial = emptyMap())
```

Use it only for `KeyboardHeightPolicy.KEY_HEIGHT` and `KeyboardHeightPolicy.KEY_HORIZONTAL_INSET`. Do not read theme keys from `allSettings`.

- [ ] **Step 6: Compile IME integration**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: PASS.

- [ ] **Step 7: Verify IME no longer reads legacy theme keys**

Run: `rg "\"theme_mode\"|\"current_theme\"|BuiltInThemes" app/src/main/java/xyz/xiao6/myboard/app app/src/main/java/xyz/xiao6/myboard/toolbar`

Expected: matches only in files not yet handled by Task 7, or no matches after Task 7.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/theme/foundation/DynamicThemeSeed.kt app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt app/src/test/java/xyz/xiao6/myboard/theme/foundation/ThemeRuntimeProviderTest.kt
git commit -m "feat: resolve foundation theme in ime runtime"
```

## Task 7: Toolbar Theme Toggle

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/toolbar/ThemeToggler.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`

**Interfaces:**
- Consumes: `SettingsRepository.getAppearanceSettings()`
- Produces: `ThemeToggler.toggle()` updates `AppearanceMode`
- Removes: direct dependency on `ThemeResolverImpl` and `BuiltInThemes`

- [ ] **Step 1: Modify ThemeToggler constructor and logic**

Replace `ThemeToggler.kt` with:

```kotlin
package xyz.xiao6.myboard.toolbar

import xyz.xiao6.myboard.data.repository.SettingsRepository
import xyz.xiao6.myboard.theme.foundation.AppearanceMode

class ThemeToggler(
    private val repo: SettingsRepository
) {
    suspend fun toggle() {
        val current = repo.getAppearanceSettings().foundation.appearanceMode
        val newMode = if (current == AppearanceMode.DARK) {
            AppearanceMode.LIGHT
        } else {
            AppearanceMode.DARK
        }
        repo.updateAppearanceMode(newMode)
    }

    suspend fun isDarkMode(): Boolean {
        return repo.getAppearanceSettings().foundation.appearanceMode == AppearanceMode.DARK
    }
}
```

- [ ] **Step 2: Update IME construction**

In `MyBoardImeService.kt`, replace:

```kotlin
themeToggler = ThemeToggler(settingsRepository, themeResolver)
```

with:

```kotlin
themeToggler = ThemeToggler(settingsRepository)
```

- [ ] **Step 3: Compile toolbar toggle**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: PASS.

- [ ] **Step 4: Verify old direct theme mutations are gone**

Run: `rg "BuiltInThemes|setTheme\\(|\"theme_mode\"|\"current_theme\"" app/src/main/java/xyz/xiao6/myboard/toolbar app/src/main/java/xyz/xiao6/myboard/ui/settings app/src/main/java/xyz/xiao6/myboard/app`

Expected: `setTheme(` appears only in `MyBoardImeService.kt` runtime resolution. No `BuiltInThemes`, `"theme_mode"`, or `"current_theme"` matches in these paths.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/toolbar/ThemeToggler.kt app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt
git commit -m "refactor: route toolbar theme toggle through appearance settings"
```

## Task 8: Non Layout-Backed Panel Chrome

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/ClipboardPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/KaomojiPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/LocaleLayoutSwitchPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/PlaceholderPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/LLMPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/STTPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/ui/panels/TextExpansionPanel.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt`

**Interfaces:**
- Consumes: `ChromeColors`
- Produces: non layout-backed panels that render with `chrome.background`, `chrome.surface`, `chrome.candidateText`, `chrome.candidateHighlight`
- Keeps: panel behavior, candidate strategy, toolbar button ordering, input actions unchanged.

- [ ] **Step 1: Add `chrome` parameter to panel composables**

For each panel composable, add:

```kotlin
import xyz.xiao6.myboard.contract.theme.ChromeColors
```

Change the signature pattern from:

```kotlin
fun PlaceholderPanel(
    panelType: PanelType,
    onBack: () -> Unit,
    onHideKeyboard: () -> Unit
)
```

to:

```kotlin
fun PlaceholderPanel(
    panelType: PanelType,
    chrome: ChromeColors,
    onBack: () -> Unit,
    onHideKeyboard: () -> Unit
)
```

Apply the same `chrome: ChromeColors` parameter to `ClipboardPanel`, `KaomojiPanel`, `LocaleLayoutSwitchPanel`, `LLMPanel`, `STTPanel`, and `TextExpansionPanel`.

- [ ] **Step 2: Replace hardcoded panel backgrounds**

Use these replacements consistently:

```kotlin
.background(Color(0xFFF1F3F4))
```

becomes:

```kotlin
.background(chrome.background)
```

```kotlin
.background(Color.White)
```

becomes:

```kotlin
.background(chrome.surface)
```

Text that used hardcoded dark/black defaults should use:

```kotlin
color = chrome.candidateText
```

Primary selected chip/icon accents should use:

```kotlin
color = chrome.candidateHighlight
```

Keep STT recording red indicators as red because they communicate recording state rather than theme.

- [ ] **Step 3: Pass chrome from IME service**

In `MyBoardImeService.kt`, pass `chrome = chrome` to every affected panel call:

```kotlin
ClipboardPanel(
    entries = clipboardManager.getHistory(),
    chrome = chrome,
    onEntryClick = { entry -> ... },
    onDeleteEntry = { entry -> ... },
    onClearAll = { ... },
    onBack = closePanelAndRefresh,
    onHideKeyboard = hideKeyboard
)
```

Use the same pattern for `KaomojiPanel`, `LocaleLayoutSwitchPanel`, and `PlaceholderPanel`.

- [ ] **Step 4: Compile panel changes**

Run: `.\gradlew.bat compileDebugKotlin`

Expected: PASS.

- [ ] **Step 5: Verify remaining hardcoded panel backgrounds**

Run: `rg "Color\\(0xFFF1F3F4\\)|Color\\.White|0xFF1F1F1F|0xFF2D2D2D" app/src/main/java/xyz/xiao6/myboard/ui/panels`

Expected: no matches for neutral panel surfaces except state-specific STT recording colors.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/ui/panels app/src/main/java/xyz/xiao6/myboard/app/MyBoardImeService.kt
git commit -m "feat: theme non layout backed panels"
```

## Task 9: Theme Resolver And BuiltInThemes Boundary

**Files:**
- Modify: `app/src/main/java/xyz/xiao6/myboard/theme/BuiltInThemes.kt`
- Modify: `app/src/main/java/xyz/xiao6/myboard/theme/ThemeResolverImpl.kt`
- Test: `app/src/test/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeMainFlowTest.kt`

**Interfaces:**
- Consumes: `ThemeColorGenerator`
- Produces: `BuiltInThemes.defaultFallback: ThemeDoc`
- Keeps: `ThemeResolverImpl(initialDoc: ThemeDoc)`, `setTheme(newDoc: ThemeDoc)`, `resolveKeyStyle`, `resolveChromeColors`
- Removes: production use of `BuiltInThemes.all` as a theme catalog.

- [ ] **Step 1: Write flow test**

```kotlin
package xyz.xiao6.myboard.theme.foundation

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.xiao6.myboard.theme.ThemeResolverImpl

class FoundationThemeMainFlowTest {
    @Test
    fun `resolver consumes generated foundation theme`() {
        val runtime = ThemeRuntimeProvider().resolve(
            AppearanceSettings(
                foundation = FoundationThemeSelection(
                    paletteId = FoundationPaletteId.MINT,
                    appearanceMode = AppearanceMode.LIGHT,
                    keyTreatment = KeyTreatment.FILLED
                )
            ),
            systemDark = false
        )
        val resolver = ThemeResolverImpl(runtime.doc)

        val chrome = resolver.resolveChromeColors()
        val action = resolver.resolveKeyStyle(KeyStyleRole.ACTION.ref)

        assertEquals(androidx.compose.ui.graphics.Color(0xFF00875A), chrome.candidateHighlight)
        assertEquals(androidx.compose.ui.graphics.Color.White, action.textColor)
    }

    @Test
    fun `unknown key style falls back to generated key default`() {
        val runtime = ThemeRuntimeProvider().resolve(AppearanceSettings.default(), systemDark = false)
        val resolver = ThemeResolverImpl(runtime.doc)

        val unknown = resolver.resolveKeyStyle("${KeyStyleRole.DEFAULT.ref}_missing")
        val fallback = resolver.resolveKeyStyle(KeyStyleRole.DEFAULT.ref)

        assertEquals(fallback.background, unknown.background)
        assertEquals(fallback.textColor, unknown.textColor)
    }
}
```

- [ ] **Step 2: Run flow test**

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.FoundationThemeMainFlowTest"`

Expected: FAIL if `ThemeResolverImpl` still depends on Android stubbed color parsing in local unit tests.

- [ ] **Step 3: Make ThemeResolverImpl color parsing JVM-testable**

In `ThemeResolverImpl.kt`, remove `android.graphics.Color` and `ColorSpaces` imports. Replace `parseColorToCompose()` with:

```kotlin
private fun parseColorToCompose(colorStr: String): androidx.compose.ui.graphics.Color {
    val raw = colorStr.trim().removePrefix("#")
    val argb = when (raw.length) {
        6 -> "FF$raw"
        8 -> raw
        else -> return androidx.compose.ui.graphics.Color.White
    }
    if (!argb.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return androidx.compose.ui.graphics.Color.White
    }
    return androidx.compose.ui.graphics.Color(argb.toLong(16).toInt())
}
```

Also import `xyz.xiao6.myboard.theme.foundation.KeyStyleRole` and replace the hardcoded fallback style id:

```kotlin
if (styleDef == null) {
    if (styleRef != KeyStyleRole.DEFAULT.ref) {
        return resolveKeyStyle(KeyStyleRole.DEFAULT.ref)
    }
    return defaultStyle
}
```

Run: `.\gradlew.bat testDebugUnitTest --tests "xyz.xiao6.myboard.theme.foundation.FoundationThemeMainFlowTest"`

Expected: PASS.

- [ ] **Step 4: Reduce BuiltInThemes to fallback only**

Keep `BuiltInThemes.kt` if other modules still need a crash-safe fallback, but remove its use as a selectable catalog. Add:

```kotlin
val defaultFallback: ThemeDoc
    get() = light
```

Keep `light` and `dark` until the advanced asset catalog replaces them. Do not reference `BuiltInThemes.all` from Settings or IME.

- [ ] **Step 5: Verify production usage**

Run: `rg "BuiltInThemes\\.all|BuiltInThemes\\.dark|BuiltInThemes\\.light" app/src/main/java`

Expected: no matches outside `BuiltInThemes.kt`. `BuiltInThemes.defaultFallback` may remain only in crash fallback code if introduced deliberately.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/xyz/xiao6/myboard/theme/BuiltInThemes.kt app/src/main/java/xyz/xiao6/myboard/theme/ThemeResolverImpl.kt app/src/test/java/xyz/xiao6/myboard/theme/foundation/FoundationThemeMainFlowTest.kt
git commit -m "test: cover generated foundation theme resolver flow"
```

## Task 10: Final Verification

**Files:**
- No planned source modifications unless verification exposes a compile or test failure.

**Interfaces:**
- Validates: foundation settings are the only theme source.
- Validates: layout files and `PanelLayoutResolver` are unchanged.
- Validates: APK builds.

- [ ] **Step 1: Run all unit tests**

Run: `.\gradlew.bat test`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 2: Build debug APK**

Run: `.\gradlew.bat assembleDebug`

Expected: BUILD SUCCESSFUL and debug APK generated under `app/build/outputs/apk/debug/`.

- [ ] **Step 3: Verify no theme state split remains in main code**

Run: `rg "\"theme_mode\"|\"current_theme\"" app/src/main/java`

Expected: no matches. If a migration helper intentionally references old keys, delete it because app has not shipped.

- [ ] **Step 4: Verify Layout contract remains untouched**

Run: `git diff -- app/src/main/assets/layouts app/src/main/java/xyz/xiao6/myboard/layout/PanelLayoutResolver.kt`

Expected: no diff. Foundation themes must not modify layout data or panel layout selection.

- [ ] **Step 5: Verify SettingsActivity still owns the entry**

Run: `rg "composable\\(\"theme\"\\)|ThemeSettingsScreen" app/src/main/java/xyz/xiao6/myboard/activity app/src/main/java/xyz/xiao6/myboard/ui/settings`

Expected: `SettingsActivity.kt` route `theme` points to `ThemeSettingsScreen`, and no second theme settings Activity exists.

- [ ] **Step 6: Commit verification fixes if any**

If verification required source changes, commit them:

```bash
git add app/src/main
git commit -m "fix: complete foundation theme integration"
```

If no source changes were required after Task 9, do not create an empty commit.

## Manual Review Checklist

- 设置页能选择 Follow system、Light、Dark，且 Toolbar 按钮切换只改变 `AppearanceSettings.foundation.appearanceMode`。
- 设置页能选择预置色板、系统动态色、自定义色入口、Filled/Outlined/Borderless、对比度、圆角。
- 切换语言、输入方案、Shift 层、Emoji、Symbol 时，主题保持一致且不改变 layoutId。
- 候选栏和 Toolbar 颜色随基础主题变化，但候选策略和 Toolbar 按钮排序不变。
- 非 layout-backed panels 不再固定浅色背景。
- 系统动态色在 Android 12+ 使用系统颜色；低版本回退到当前 preset seed，不申请权限。
- `ThemeDoc`、`ThemeResolverImpl` 仍兼容现有 `LayoutRenderer` 的 `styleRef` 解析。

## Self-Review

**Spec coverage:** 本计划覆盖设计文档 Phase 1：`AppearanceSettings` 单一来源、基础色板、系统动态色、自定义主色、light/dark/follow-system、基础按键外观、主键盘/候选栏/Toolbar/Emoji/Symbol/面板接入、`current_theme` 与 `theme_mode` 主流程问题修复。高级 SkinLayer、Decoration、主题包导入、主题定义器、Layout binding 明确不在本计划实现。

**Placeholder scan:** 文档已检查会导致执行歧义的占位表达；高级能力用范围边界表达，不作为未定义任务塞进第一阶段。

**Type consistency:** `AppearanceSettings`、`FoundationThemeSelection`、`AppearanceMode`、`ThemeVariant`、`ThemeRuntimeProvider.resolve()`、`SettingsRepository.updateAppearanceMode()` 在各任务中的名称和签名保持一致。
