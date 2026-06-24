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
- EmojiPanel、ClipboardPanel、KaomojiPanel 已存在（在之前的阶段实现）
- 创建 PlaceholderPanel：未实现面板的占位
- 扩展 PanelType 枚举，新增 KAOMOJI 和 TEXT_EXPANSION
- 在 MyBoardImeService 中接入面板视图切换

**为什么：**
- 面板 Composable 已存在但未被 IME 视图渲染
- 需要根据 activePanel 状态切换显示内容
- PlaceholderPanel 为后续扩展提供统一占位

**文件：** ui/panels/PlaceholderPanel.kt, core/contract/PanelType.kt, ime/MyBoardImeService.kt

### 6. Toolbar 增强

**做了什么：**
- 实现 ThemeToggler：夜间模式切换，协调 SettingsManager 和 ThemeResolver
- 实现 LayoutSwitcher：当前语言内 Schema 循环切换
- 在 Toolbar 中添加夜间模式按钮和设置跳转
- 修改 SettingsManager 添加 toggleTheme() 方法
- 修复 SettingsActivity 引用路径

**为什么：**
- 布局切换和夜间模式是高频操作，应放在 Toolbar 中
- 设置跳转需要启动 SettingsActivity
- ThemeToggler 封装了状态同步逻辑

**文件：** core/toolbar/ThemeToggler.kt, core/toolbar/LayoutSwitcher.kt, core/settings/SettingsManager.kt, ime/MyBoardImeService.kt

### 7. PanelType 扩展

**做了什么：**
- 新增 KAOMOJI 和 TEXT_EXPANSION 面板类型
- 扩展 ActionDispatcher 的 parsePanel 方法支持新类型

**为什么：**
- 颜文字和快捷短语是常见输入法功能
- 预留扩展点便于后续实现

**文件：** core/contract/PanelType.kt, core/layout/ActionDispatcher.kt
