# P6: 主题系统 (2 周)

## 1. 目标

实现内置主题、动效支持、图片/GIF 主题、深色模式自动切换。

## 2. 里程碑验收标准

- [x] 内置 5+ 主题可用
- [x] 按键动效流畅
- [x] 图片主题可用
- [x] GIF 动图主题可用
- [x] 深色模式自动切换

## 3. 详细设计

### 3.1 主题模型

```kotlin
@Serializable
data class KeyboardTheme(
    val id: String,
    val name: String,
    val version: Int = 1,
    val type: String = "static",            // static | animated | image
    val colors: ThemeColors,
    val geometry: ThemeGeometry,
    val typography: ThemeTypography,
    val animations: ThemeAnimations? = null,
    val images: ThemeImages? = null
)

@Serializable
data class ThemeColors(
    val background: String,
    val surface: String,
    val key: KeyColors,
    val action: ActionColors,
    val candidate: CandidateColors,
    val toolbar: ToolbarColors? = null,
    val popup: PopupColors? = null
)

@Serializable
data class KeyColors(
    val normal: String,
    val pressed: String,
    val text: String,
    val hint: String
)

@Serializable
data class ThemeGeometry(
    val key: KeyGeometryConfig
)

@Serializable
data class KeyGeometryConfig(
    val cornerRadius: Float = 8f,
    val heightDp: Float = 46f,
    val gapHDp: Float = 4f,
    val gapVDp: Float = 5f
)

@Serializable
data class ThemeTypography(
    val keyLabel: TextConfig = TextConfig(),
    val keyHint: TextConfig = TextConfig(sizeSp = 10f),
    val candidate: TextConfig = TextConfig(sizeSp = 16f),
    val toolbar: TextConfig = TextConfig(sizeSp = 12f)
)

@Serializable
data class TextConfig(
    val sizeSp: Float = 18f,
    val weight: String = "normal"
)

@Serializable
data class ThemeAnimations(
    val keyPress: AnimationConfig? = null,
    val candidateSwitch: AnimationConfig? = null,
    val panelSwitch: AnimationConfig? = null
)

@Serializable
data class AnimationConfig(
    val type: String,                       // scale | fade | slide
    val from: Float = 1f,
    val to: Float = 0.95f,
    val duration: Int = 100
)

@Serializable
data class ThemeImages(
    val keyboardBackground: String? = null,
    val keyNormal: String? = null,
    val keyPressed: String? = null,
    val toolbarBackground: String? = null
)
```

### 3.2 ThemeResolver

```kotlin
class ThemeResolver(private val theme: KeyboardTheme) {
    fun resolveKeyColor(keyId: String, state: KeyState): Color {
        val colors = when (state) {
            KeyState.NORMAL -> theme.colors.key.normal
            KeyState.PRESSED -> theme.colors.key.pressed
            KeyState.DISABLED -> theme.colors.key.normal.copy(alpha = 0.5f)
        }
        return Color.parseColor(colors)
    }

    fun resolveTextColor(keyId: String): Color {
        return Color.parseColor(theme.colors.key.text)
    }

    fun resolveCornerRadius(): Float {
        return theme.geometry.key.cornerRadius
    }

    fun resolveKeyHeight(): Float {
        return theme.geometry.key.heightDp
    }
}
```

### 3.3 深色模式切换

```kotlin
class ThemeSwitchManager @Inject constructor(
    private val prefs: DataStore<Preferences>
) {
    private val _currentTheme = MutableStateFlow<KeyboardTheme>(DefaultTheme)
    val currentTheme: StateFlow<KeyboardTheme> = _currentTheme

    fun init(context: Context) {
        val config = loadSwitchConfig()
        when (config.mode) {
            "auto" -> observeSystemTheme(context)
            "battery" -> observeBattery(context)
            "scheduled" -> checkSchedule(config)
        }
    }

    private fun observeSystemTheme(context: Context) {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        _currentTheme.value = if (nightMode == Configuration.UI_MODE_NIGHT_YES) {
            loadTheme("dark")
        } else {
            loadTheme("light")
        }
    }

    private fun observeBattery(context: Context) {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        if (level < 20) {
            _currentTheme.value = loadTheme("dark")
        } else if (level > 50) {
            _currentTheme.value = loadTheme("light")
        }
    }

    private fun loadTheme(id: String): KeyboardTheme {
        // 从 assets/themes/ 加载
        return DefaultTheme
    }
}
```

### 3.4 内置主题

| 主题 | ID | 说明 |
|------|-----|------|
| Default | `default` | 浅色默认主题 |
| Dark | `dark` | 深色主题 |
| Dracula | `dracula` | Dracula 配色 |
| Nord | `nord` | Nord 配色 |
| Solarized | `solarized` | Solarized 配色 |

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `ui/theme/KeyboardTheme.kt` | 主题模型 |
| `ui/theme/ThemeResolver.kt` | 主题解析 |
| `ui/theme/ThemeSwitchManager.kt` | 主题切换 |
| `assets/themes/default.json` | 默认主题 |
| `assets/themes/dark.json` | 深色主题 |
| `assets/themes/dracula.json` | Dracula 主题 |
| `assets/themes/nord.json` | Nord 主题 |
| `assets/themes/solarized.json` | Solarized 主题 |
