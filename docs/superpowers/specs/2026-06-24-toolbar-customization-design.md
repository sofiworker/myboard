# Toolbar 自定义 + 设置架构迁移 设计文档

日期: 2026-06-24

## 1. 背景与目标

### 1.1 现状问题

| 问题 | 说明 |
|------|------|
| Toolbar 不可自定义 | 6 个按钮硬编码在 `MyBoardImeService.kt`，用户无法调整顺序或增减 |
| 设置入口位置错误 | Settings icon 在右侧，不符合主流输入法习惯（左侧固定） |
| 缺少收起键盘按钮 | 右侧无 ↓ 收起键盘按钮 |
| 缺少 Layout 切换入口 | `LayoutSwitcher` 已实现，但 Toolbar 无入口 |
| 设置层使用 SharedPreferences | 违反架构约束，需迁移到 Room + ViewModel |

### 1.2 目标

1. Toolbar 支持完全自定义：按钮增减、顺序调整、布局模式选择
2. 对标主流输入法：左侧固定设置入口、右侧固定收起键盘、中间可滚动/固定
3. 设置层全面迁移到 Room + ViewModel 架构
4. 保持 i18n 支持

## 2. 架构设计

### 2.1 分层架构

```
┌─────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                     │
│  SettingsScreen / Toolbar  ← collectAsState()            │
│                         ↑                                │
│              SettingsViewModel (StateFlow<UiState>)       │
└─────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────┐
│              SettingsRepository (单一数据源)               │
│  Flow<AllSettings>  /  suspend fun update*(...)          │
└─────────────────────────────────────────────────────────┘
                              ↑
┌─────────────────────────────────────────────────────────┐
│              SettingsDao (Room @Dao)                      │
│  settings 表  /  toolbar_items 表                         │
└─────────────────────────────────────────────────────────┘
```

### 2.2 目录结构变更

```
xyz.xiao6.myboard/
  data/                              # 新增 data 层
    db/
      AppDatabase.kt                 # Room Database（合并现有 DictionaryDatabase）
      dao/
        SettingsDao.kt
        PhraseDao.kt                 # 从 DictionaryDao 迁移
        UserPhraseDao.kt             # 从 UserDictionaryDao 迁移
      entity/
        SettingsEntity.kt
        ToolbarItemEntity.kt
        PhraseEntity.kt              # 从 dictionary/ 迁移
        UserPhraseEntity.kt          # 从 dictionary/ 迁移
    repository/
      SettingsRepository.kt
  ui/
    settings/
      SettingsViewModel.kt           # 新增
      SettingsScreen.kt              # 重构：使用 ViewModel
      ToolbarSettingsScreen.kt       # 新增：工具栏设置页面
      ToolbarSettingsViewModel.kt    # 新增
  toolbar/
    Toolbar.kt                       # 重构：从 MyBoardImeService 提取为独立 Composable
    LayoutSwitcher.kt                # 保留
    ThemeToggler.kt                  # 保留
  settings/
    SettingsManager.kt               # 删除（迁移完成后）
```

## 3. 数据模型

### 3.1 Room Entity

```kotlin
// ===== settings 表：键值对配置 =====
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val stringValue: String = "",
    val intValue: Int = 0,
    val booleanValue: Boolean = false
)

// ===== toolbar_items 表：工具栏按钮配置 =====
@Entity(
    tableName = "toolbar_items",
    indices = [Index("sortOrder")]
)
data class ToolbarItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // ToolbarItemType 枚举的 name
    val enabled: Boolean = true,
    val sortOrder: Int = 0      // 显示顺序，从 0 开始
)
```

### 3.2 枚举定义

```kotlin
// ===== Toolbar 中间区可用按钮类型 =====
enum class ToolbarItemType {
    LOCALE_SWITCH,   // 语言循环切换
    THEME_TOGGLE,    // 主题切换
    EMOJI,           // Emoji 面板
    SYMBOL,          // 符号面板
    CLIPBOARD,       // 剪贴板面板
    LAYOUT_SWITCH,   // Layout/Schema 循环切换
    VOICE_INPUT      // 语音输入（预留）
}

// ===== Toolbar 布局模式 =====
enum class ToolbarLayoutMode {
    SCROLLABLE,      // 横向滚动（默认，主流输入法风格）
    FIXED            // 固定均分屏幕宽度
}

// ===== Toolbar 固定位置按钮类型（不可配置） =====
enum class ToolbarFixedPosition {
    LEFT,            // 左侧：Settings icon
    RIGHT            // 右侧：↓ 收起键盘
}
```

### 3.3 数据库设计

**独立数据库**：设置使用新的 `SettingsDatabase`，与现有 `DictionaryDatabase` 互不影响，避免合并数据库带来的迁移风险。

```
SettingsDatabase (version = 1)       DictionaryDatabase (version = 1)
├── settings 表                      ├── phrases 表（不变）
└── toolbar_items 表                 └── user_phrases 表（不变）
```

```kotlin
@Database(
    entities = [SettingsEntity::class, ToolbarItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SettingsDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
}
```

### 3.4 默认值

```kotlin
object DefaultToolbarItems {
    val items = listOf(
        ToolbarItemEntity(type = "LOCALE_SWITCH", enabled = true, sortOrder = 0),
        ToolbarItemEntity(type = "THEME_TOGGLE",  enabled = true, sortOrder = 1),
        ToolbarItemEntity(type = "EMOJI",         enabled = true, sortOrder = 2),
        ToolbarItemEntity(type = "SYMBOL",        enabled = true, sortOrder = 3),
        ToolbarItemEntity(type = "CLIPBOARD",     enabled = true, sortOrder = 4),
        ToolbarItemEntity(type = "LAYOUT_SWITCH", enabled = true, sortOrder = 5)
    )
}

object DefaultSettings {
    val items = mapOf(
        "current_locale" to "en-US",
        "enabled_locales" to "en-US,zh-CN",
        "theme_mode" to "auto",
        "current_theme" to "default",
        "keyboard_height" to "260",
        "key_font_size" to "18",
        "haptic_feedback" to "true",
        "sound_feedback" to "false",
        "double_space_period" to "true",
        "auto_capitalize" to "true",
        "double_pinyin_enabled" to "false",
        "voice_input_enabled" to "false",
        "handwriting_enabled" to "false",
        "llm_provider" to "disabled",
        "llm_api_key" to "",
        "llm_endpoint" to "",
        "stt_provider" to "system",
        "onboarding_completed" to "false",
        "toolbar_layout_mode" to "SCROLLABLE"
    )
}
```

## 4. Repository

```kotlin
class SettingsRepository(private val dao: SettingsDao) {

    // ===== 通用设置 =====
    val settings: Flow<Map<String, String>> = dao.getAllSettings()
        .map { entities -> entities.associate { it.key to it.stringValue } }

    suspend fun updateSetting(key: String, value: String) {
        dao.upsertSetting(SettingsEntity(key = key, stringValue = value))
    }

    suspend fun getSetting(key: String): String? = dao.getSetting(key)

    // ===== Toolbar 配置 =====
    val toolbarItems: Flow<List<ToolbarItemEntity>> = dao.getToolbarItems()

    val toolbarLayoutMode: Flow<ToolbarLayoutMode> = dao.getSetting("toolbar_layout_mode")
        .map { ToolbarLayoutMode.valueOf(it ?: "SCROLLABLE") }

    suspend fun updateToolbarItems(items: List<ToolbarItemEntity>) {
        dao.deleteAllToolbarItems()
        items.forEach { dao.upsertToolbarItem(it) }
    }

    suspend fun updateToolbarLayoutMode(mode: ToolbarLayoutMode) {
        updateSetting("toolbar_layout_mode", mode.name)
    }

    // ===== 初始化（首次启动或数据库升级） =====
    suspend fun initializeDefaults() {
        if (dao.getSettingCount() == 0) {
            DefaultSettings.items.forEach { (key, value) ->
                dao.upsertSetting(SettingsEntity(key = key, stringValue = value))
            }
        }
        if (dao.getToolbarItemCount() == 0) {
            DefaultToolbarItems.items.forEach { dao.upsertToolbarItem(it) }
        }
    }
}
```

## 5. ViewModel

### 5.1 SettingsViewModel（全局设置）

```kotlin
class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    data class UiState(
        val settings: Map<String, String> = emptyMap(),
        val isLoading: Boolean = true
    )

    val uiState: StateFlow<UiState> = repo.settings
        .map { UiState(settings = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun updateSetting(key: String, value: String) {
        viewModelScope.launch { repo.updateSetting(key, value) }
    }
}
```

### 5.2 ToolbarSettingsViewModel（工具栏设置）

```kotlin
class ToolbarSettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    data class UiState(
        val items: List<ToolbarItemDisplay> = emptyList(),
        val layoutMode: ToolbarLayoutMode = ToolbarLayoutMode.SCROLLABLE,
        val availableToAdd: List<ToolbarItemType> = emptyList()
    )

    data class ToolbarItemDisplay(
        val type: ToolbarItemType,
        val enabled: Boolean,
        val sortOrder: Int,
        val isFixed: Boolean = false   // true = 左右固定按钮，不可操作
    )

    val uiState: StateFlow<UiState> = combine(
        repo.toolbarItems,
        repo.toolbarLayoutMode
    ) { items, layoutMode ->
        val enabledTypes = items.filter { it.enabled }.map { it.type }.toSet()
        val available = ToolbarItemType.entries.filter { it !in enabledTypes }
        UiState(
            items = items.sortedBy { it.sortOrder }.map {
                ToolbarItemDisplay(
                    type = ToolbarItemType.valueOf(it.type),
                    enabled = it.enabled,
                    sortOrder = it.sortOrder
                )
            },
            layoutMode = layoutMode,
            availableToAdd = available
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun setLayoutMode(mode: ToolbarLayoutMode) {
        viewModelScope.launch { repo.updateToolbarLayoutMode(mode) }
    }

    fun toggleItem(type: ToolbarItemType, enabled: Boolean) {
        viewModelScope.launch {
            val current = repo.toolbarItems.first()
            if (enabled) {
                // 添加：追加到末尾
                val maxOrder = current.maxOfOrNull { it.sortOrder } ?: -1
                val newEntity = ToolbarItemEntity(
                    type = type.name,
                    enabled = true,
                    sortOrder = maxOrder + 1
                )
                repo.updateToolbarItems(current + newEntity)
            } else {
                // 移除
                repo.updateToolbarItems(current.filter { it.type != type.name })
            }
        }
    }

    fun reorderItems(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val current = repo.toolbarItems.first().sortedBy { it.sortOrder }.toMutableList()
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            val reordered = current.mapIndexed { index, entity ->
                entity.copy(sortOrder = index)
            }
            repo.updateToolbarItems(reordered)
        }
    }
}
```

## 6. UI 设计

### 6.1 Toolbar 三区布局

```
┌──────────────────────────────────────────────────────────┐
│ [⚙]  [🔄] [😀] [📋] [⭐] [🌙] [🎤] [🌐] ...    [↓] │
│  ↑      ←── 中间区（LazyRow 横向滚动）──→          ↑    │
│ 固定                                                固定  │
└──────────────────────────────────────────────────────────┘
```

**固定区**：
- 左侧：Settings icon (`Icons.Default.Settings`)，点击跳转 SettingsActivity
- 右侧：↓ 收起键盘 icon (`Icons.Default.KeyboardHide`)，点击调用 `inputMethodService.hideSoftInputFromWindow()`

**中间区**：
- 使用 `LazyRow` 实现横向滚动
- 当 `layoutMode == FIXED` 时，使用 `Row` + `Modifier.weight(1f)` 等分宽度
- 按钮顺序和显隐由 `toolbarItems` 配置驱动

### 6.2 Toolbar Composable 签名

```kotlin
@Composable
fun Toolbar(
    items: List<ToolbarItemEntity>,
    layoutMode: ToolbarLayoutMode,
    context: KeyboardContext,
    themeToggler: ThemeToggler,
    layoutSwitcher: LayoutSwitcher,
    onLocaleSwitch: (LocaleTag) -> Unit,
    onPanelOpen: (PanelType) -> Unit,
    onSettingsClick: () -> Unit,
    onHideKeyboard: () -> Unit,
    modifier: Modifier = Modifier
)
```

### 6.3 工具栏设置页面

```
┌─────────────────────────────────┐
│  ← 工具栏设置                    │
├─────────────────────────────────┤
│  布局模式                        │
│  ○ 横向滚动（推荐）              │
│  ○ 固定均分                      │
├─────────────────────────────────┤
│  当前按钮（拖拽调整顺序）         │
│  ┌─ ⚙ 设置 ──────── [固定] ─┐   │
│  │  🔄 Layout切换  ✓    ≡    │   │
│  │  😀 Emoji       ✓    ≡    │   │
│  │  📋 剪贴板      ✓    ≡    │   │
│  │  ⭐ 符号        ✓    ≡    │   │
│  │  🌙 主题        ✓    ≡    │   │
│  │  🌐 语言        ✓    ≡    │   │
│  │  🎤 语音        ✓    ≡    │   │
│  └─ ↓ 收起键盘 ──── [固定] ─┘   │
├─────────────────────────────────┤
│  可添加的按钮                    │
│  [🎤 语音输入] [🌐 语言切换]     │
└─────────────────────────────────┘
```

**交互细节**：
- 固定按钮（⚙ 和 ↓）显示为灰色，不可拖拽、不可删除
- 中间区按钮：左侧开关控制显隐，右侧 ≡ 手柄支持拖拽排序
- "可添加的按钮" 区域显示已隐藏的按钮，点击添加到中间区

### 6.4 收起键盘实现

```kotlin
// 在 MyBoardImeService 中
onHideKeyboard = {
    requestHideSelf(0)  // InputMethodService 方法
}
```

## 7. SettingsManager 迁移映射

| 旧 Key (SettingsManager) | 新 Key (Room) | 类型 |
|---------------------------|----------------|------|
| `current_locale` | `current_locale` | String |
| `enabled_locales` | `enabled_locales` | String (逗号分隔) |
| `default_script_per_locale` | `default_script_per_locale` | String (JSON) |
| `default_schema_per_locale_script` | `default_schema_per_locale_script` | String (JSON) |
| `double_pinyin_enabled` | `double_pinyin_enabled` | Boolean |
| `voice_input_enabled` | `voice_input_enabled` | Boolean |
| `handwriting_enabled` | `handwriting_enabled` | Boolean |
| `theme_mode` | `theme_mode` | String |
| `current_theme` | `current_theme` | String |
| `haptic_feedback` | `haptic_feedback` | Boolean |
| `sound_feedback` | `sound_feedback` | Boolean |
| `keyboard_height` | `keyboard_height` | Int |
| `key_font_size` | `key_font_size` | Float |
| `double_space_period` | `double_space_period` | Boolean |
| `auto_capitalize` | `auto_capitalize` | Boolean |
| `llm_provider` | `llm_provider` | String |
| `llm_api_key` | `llm_api_key` | String |
| `llm_endpoint` | `llm_endpoint` | String |
| `stt_provider` | `stt_provider` | String |
| `onboarding_completed` | `onboarding_completed` | Boolean |
| — | `toolbar_layout_mode` | String (新增) |

## 8. i18n

新增字符串资源：

```xml
<!-- Toolbar 按钮名称 -->
<string name="toolbar_settings">设置</string>
<string name="toolbar_hide_keyboard">收起键盘</string>
<string name="toolbar_locale_switch">语言切换</string>
<string name="toolbar_theme_toggle">主题切换</string>
<string name="toolbar_emoji">Emoji</string>
<string name="toolbar_symbol">符号</string>
<string name="toolbar_clipboard">剪贴板</string>
<string name="toolbar_layout_switch">键盘布局</string>
<string name="toolbar_voice_input">语音输入</string>

<!-- 工具栏设置页面 -->
<string name="toolbar_settings_title">工具栏设置</string>
<string name="toolbar_layout_mode">布局模式</string>
<string name="toolbar_layout_scrollable">横向滚动</string>
<string name="toolbar_layout_fixed">固定均分</string>
<string name="toolbar_manage_buttons">按钮管理</string>
<string name="toolbar_available_buttons">可添加的按钮</string>
<string name="toolbar_add_button">添加</string>
<string name="toolbar_remove_button">移除</string>
<string name="toolbar_fixed_button">固定</string>
```

## 9. 实施阶段

### Phase 1: 数据层基础
1. 创建 Room Entity（SettingsEntity, ToolbarItemEntity）
2. 创建 SettingsDao
3. 升级 Database (version 1 → 2)，合并现有字典表
4. 创建 SettingsRepository
5. 数据迁移：从 SharedPreferences 读取旧数据写入 Room

### Phase 2: ViewModel 层
1. 创建 SettingsViewModel
2. 创建 ToolbarSettingsViewModel
3. 重构 ThemeToggler 读取设置方式

### Phase 3: Toolbar 重构
1. 从 MyBoardImeService 提取 Toolbar 为独立 Composable
2. 实现三区布局（固定左-中间滚动/固定-固定右）
3. 新增 ↓ 收起键盘按钮
4. 新增 🔄 Layout 切换按钮
5. Toolbar 读取 toolbarItems 配置动态渲染

### Phase 4: UI 设置页面
1. 创建 ToolbarSettingsScreen（拖拽排序 + 显隐开关）
2. 重构 SettingsScreen 使用 SettingsViewModel
3. 添加工具栏设置入口到 SettingsScreen

### Phase 5: 清理与验证
1. 删除 SettingsManager.kt
2. 更新所有引用点
3. 确保编译通过，生成 APK

## 10. 验收标准

- [ ] Toolbar 左侧固定显示 Settings icon，点击跳转 SettingsActivity
- [ ] Toolbar 右侧固定显示 ↓ 收起键盘按钮，点击收起键盘
- [ ] Toolbar 中间区按钮顺序和显隐可由用户自定义
- [ ] 支持横向滚动和固定均分两种布局模式
- [ ] Layout 切换按钮存在于 Toolbar 中，点击可循环切换 Schema
- [ ] 工具栏设置页面支持拖拽排序和按钮增减
- [ ] 所有设置存储在 Room，不使用 SharedPreferences
- [ ] 所有设置通过 ViewModel 访问
- [ ] 设置页面和 Toolbar 按钮名称支持 i18n
- [ ] 编译通过，可生成 APK
