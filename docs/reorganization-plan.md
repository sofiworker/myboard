# 包结构重组计划

## 目标
重新组织 `xyz.xiao6.myboard` 的包结构，解决以下问题：
- `core/` 前缀冗余，增加无意义层级
- `core/contract/` 22 个文件扁平堆积，按领域拆分
- `core/androidbridge/` 命名不清晰
- `core/panel/` 只有 Impl 没有接口
- `core/pack/` 和 `core/extension/` 功能重叠
- `ime/` 混合 Service 和 Activities
- `ui/panels/` 混合键盘面板和设置页面
- 单文件包过多（clipboard, expansion, common 等）

## 当前结构 → 目标结构

### 第一层：移除 `core/` 前缀
所有 `core/X/` → `X/`，减少一层无意义嵌套。

### 第二层：合约层按领域拆分

**当前** `core/contract/`（22 个文件扁平）→ **目标** `contract/` 按领域：

```
contract/
├── input/           ← InputAction, InputEvent, EngineResult, InputSessionState, ResetReason
├── layout/          ← LayoutContracts (Dimension, KeyDef, LayoutContainer, Region, LayoutDoc...)
├── manifest/        ← ManifestContracts (LanguageManifest, SchemaCapability, LocaleCapability...)
├── theme/           ← ThemeContracts (KeyStyle, FeedbackPolicy, ThemeResolver)
├── engine/          ← EngineContracts (Encoder, Dictionary, CandidatePolicy, DisplayPolicy...)
├── bridge/          ← BridgeContracts (EditorProfile, SelectionSnapshot, SelectionDecision)
├── registry/        ← RegistryContracts (RegisterResult, LayoutIssue, DictionaryKey...)
├── panel/           ← PanelContracts (PanelManager, SttBridge, LlmBridge)
├── language/        ← LanguagePackContracts (LanguagePackManager, LanguagePackInfo)
└── state/           ← LocaleTag, Script, Schema, KeyboardContext, OrthogonalState, LayoutLayer,
                       PanelType, Candidate, Transition (TransitionEvent, TransitionResult)
```

### 第三层：IME 入口分离

**当前** `ime/`（4 个文件混合 Service + Activity）→ 拆分：

```
app/                  ← Android 组件入口
├── MyBoardImeService.kt
└── ...

activity/             ← Activity 组件
├── MainActivity.kt
├── SettingsActivity.kt
└── OnboardingActivity.kt
```

### 第四层：UI 层整理

**当前** `ui/` 混乱 → **目标**：

```
ui/
├── keyboard/         ← 键盘 UI 组件
│   ├── KeyboardView.kt  (从 MyBoardImeService 提取的键盘主视图)
│   ├── Toolbar.kt       (从 ui/keyboard/ 移入)
│   └── CandidateBar.kt  (从 ui/candidate/ 移入)
├── panels/           ← 键盘功能面板（纯键盘面板）
│   ├── EmojiPanel.kt
│   ├── SymbolPanel.kt
│   ├── KaomojiPanel.kt
│   ├── ClipboardPanel.kt
│   ├── TextExpansionPanel.kt
│   ├── PlaceholderPanel.kt
│   ├── LLMPanel.kt
│   └── STTPanel.kt
└── settings/         ← 设置页面
    ├── SettingsScreen.kt
    ├── ThemeSettingsScreen.kt
    ├── LLMSettingsScreen.kt
    ├── STTSettingsScreen.kt
    └── OnboardingScreen.kt
```

### 第五层：合并小包

| 当前 | 合并到 | 原因 |
|------|--------|------|
| `core/panel/` (3 Impl 文件) | `panel/` | 保持原名但移到顶层 |
| `core/pack/` (1 Impl) + `core/extension/` (1 接口) | `pack/` | 功能重叠，合并 |
| `core/symbol/` (3 文件) | `dictionary/` | 符号数据本质上是静态字典 |
| `core/clipboard/` (1 文件) | `clipboard/` | 保持独立但移到顶层 |

## 完整目标包结构

```
xyz.xiao6.myboard/
├── app/                          # Android 入口点
│   ├── MyBoardImeService.kt
│   └── ...
│
├── activity/                     # Activity 组件
│   ├── MainActivity.kt
│   ├── SettingsActivity.kt
│   └── OnboardingActivity.kt
│
├── contract/                     # 数据类型 & 接口（按领域分包）
│   ├── input/
│   ├── layout/
│   ├── manifest/
│   ├── theme/
│   ├── engine/
│   ├── bridge/
│   ├── registry/
│   ├── panel/
│   ├── language/
│   └── state/
│
├── engine/                       # 输入引擎层
│   ├── InputPipeline.kt
│   ├── InputPipelineImpl.kt
│   ├── EngineRegistry.kt / Impl
│   ├── EngineResourceResolver.kt / Impl
│   ├── EncoderRegistry.kt / Impl
│   ├── DictionaryRegistry.kt / Impl
│   ├── CandidatePolicyRegistry.kt / Impl
│   ├── DisplayPolicyRegistry.kt / Impl
│   ├── builtin/                  # 内置引擎实现
│   │   ├── DirectEngine.kt
│   │   ├── TableComposingEngine.kt
│   │   ├── TransliterationEngine.kt
│   │   ├── IdentityEncoder.kt
│   │   ├── ShuangpinEncoder.kt
│   │   ├── ShuangpinMapping.kt
│   │   ├── T9Decoder.kt
│   │   ├── ChineseDefaultPolicy.kt
│   │   ├── DirectDefaultPolicy.kt
│   │   ├── JapaneseKanaDefaultPolicy.kt
│   │   ├── HiddenDisplayPolicy.kt
│   │   ├── ShowComposingPolicy.kt
│   │   └── ShowQueryPolicy.kt
│   └── fake/                     # 测试替身
│       ├── FakeFeedbackPlayer.kt
│       └── FakeInputConnectionGateway.kt
│
├── layout/                       # 布局引擎层
│   ├── LayoutEngine.kt
│   ├── LayoutMeasurer.kt / Impl
│   ├── LayoutRegistry.kt / Impl
│   ├── LayoutRenderer.kt
│   ├── ActionDispatcher.kt
│   ├── LayoutHintResolver.kt / Impl
│   ├── BindingsEvaluator.kt / Impl
│   ├── BuiltInLayouts.kt
│   ├── LayoutAssetsLoader.kt
│   ├── LayoutDocParser.kt
│   └── JsoncParser.kt
│
├── state/                        # 状态管理层
│   ├── KeyboardContextManager.kt / Impl
│   ├── TransitionEngine.kt / Impl
│   ├── OrthogonalRegistry.kt / Impl
│   └── BuiltInManifests.kt
│
├── androidbridge/                # Android 平台桥接层
│   ├── InputConnectionGateway.kt / Impl
│   ├── EditorInfoResolver.kt / Impl
│   ├── SelectionTracker.kt / Impl
│   ├── HardwareKeyRouter.kt / Impl
│   ├── SubtypeBridge.kt / Impl
│   ├── PermissionGateway.kt / Impl
│   └── FeedbackPlayer.kt / Impl
│
├── theme/                        # 主题系统
│   ├── ThemeDoc.kt
│   ├── ThemeResolverImpl.kt
│   └── BuiltInThemes.kt
│
├── dictionary/                   # 字典、候选、符号数据
│   ├── DictionaryModule.kt
│   ├── PinyinDictionary.kt
│   ├── UserDictionary.kt
│   ├── AdaptiveDictionary.kt
│   ├── HotWordCalculator.kt
│   ├── DictionaryUpdater.kt
│   ├── DictionaryDao.kt
│   ├── DictionaryDatabase.kt
│   ├── UserDictionaryDao.kt
│   ├── PhraseEntity.kt
│   ├── UserPhraseEntity.kt
│   ├── EmojiRepository.kt      ← 从 core/symbol/ 合并
│   ├── KaomojiRepository.kt    ← 从 core/symbol/ 合并
│   └── SymbolRepository.kt     ← 从 core/symbol/ 合并
│
├── settings/                     # 设置管理（单一来源）
│   └── SettingsManager.kt
│
├── toolbar/                      # 工具栏工具
│   ├── LayoutSwitcher.kt
│   └── ThemeToggler.kt
│
├── panel/                        # 面板管理
│   ├── PanelManagerImpl.kt
│   ├── LlmBridgeImpl.kt
│   └── SttBridgeImpl.kt
│
├── clipboard/                    # 剪贴板管理
│   └── ClipboardManager.kt
│
├── expansion/                    # 文本扩展
│   └── TextExpansionManager.kt
│
├── keybinding/                   # 按键绑定
│   ├── KeyBindingManager.kt
│   └── KeyBindingModels.kt
│
├── pack/                         # 语言包管理（合并 extension）
│   ├── LanguagePackManagerImpl.kt
│   └── LanguagePackImporter.kt
│
├── ai/                           # AI 服务
│   ├── llm/LLMProvider.kt
│   └── stt/STTProvider.kt
│
├── common/                       # 共享工具
│   └── SchemaVersion.kt
│
└── ui/                           # Compose UI
    ├── keyboard/
    │   ├── Toolbar.kt
    │   └── CandidateBar.kt
    ├── panels/
    │   ├── EmojiPanel.kt
    │   ├── SymbolPanel.kt
    │   ├── KaomojiPanel.kt
    │   ├── ClipboardPanel.kt
    │   ├── TextExpansionPanel.kt
    │   ├── PlaceholderPanel.kt
    │   ├── LLMPanel.kt
    │   └── STTPanel.kt
    └── settings/
        ├── SettingsScreen.kt
        ├── ThemeSettingsScreen.kt
        ├── LLMSettingsScreen.kt
        ├── STTSettingsScreen.kt
        └── OnboardingScreen.kt
```

## 执行步骤

### 阶段 1：合约层拆分（contract/ 按领域）
- 创建 `contract/input/`, `contract/layout/`, `contract/manifest/`, `contract/theme/`, `contract/engine/`, `contract/bridge/`, `contract/registry/`, `contract/panel/`, `contract/language/`, `contract/state/`
- 移动文件并更新 package 声明
- 更新所有 import

### 阶段 2：移除 core/ 前缀
- 将 `core/engine/` → `engine/`
- 将 `core/layout/` → `layout/`
- 将 `core/state/` → `state/`
- 将 `core/androidbridge/` → `androidbridge/`
- 将 `core/theme/` → `theme/`
- 将 `core/dictionary/` → `dictionary/`
- 将 `core/settings/` → `settings/`
- 将 `core/toolbar/` → `toolbar/`
- 将 `core/panel/` → `panel/`
- 将 `core/clipboard/` → `clipboard/`
- 将 `core/expansion/` → `expansion/`
- 将 `core/keybinding/` → `keybinding/`
- 将 `core/pack/` + `core/extension/` → `pack/`
- 将 `core/ai/` → `ai/`
- 将 `core/common/` → `common/`
- 将 `core/symbol/` → `dictionary/`（合并）

### 阶段 3：入口点分离
- 将 `ime/MyBoardImeService.kt` → `app/MyBoardImeService.kt`
- 将 `ime/MainActivity.kt` → `activity/MainActivity.kt`
- 将 `ime/SettingsActivity.kt` → `activity/SettingsActivity.kt`
- 将 `ime/OnboardingActivity.kt` → `activity/OnboardingActivity.kt`

### 阶段 4：UI 层整理
- 将 `ui/keyboard/Toolbar.kt` → `ui/keyboard/Toolbar.kt`（保持）
- 将 `ui/candidate/ComposeCandidateBar.kt` → `ui/keyboard/CandidateBar.kt`
- 将 `ui/panels/SettingsScreen.kt` → `ui/settings/SettingsScreen.kt`
- 将 `ui/panels/ThemeSettingsScreen.kt` → `ui/settings/ThemeSettingsScreen.kt`
- 将 `ui/panels/LLMSettingsScreen.kt` → `ui/settings/LLMSettingsScreen.kt`
- 将 `ui/panels/STTSettingsScreen.kt` → `ui/settings/STTSettingsScreen.kt`
- 将 `ui/panels/OnboardingScreen.kt` → `ui/settings/OnboardingScreen.kt`

### 阶段 5：更新全局 import
- 批量更新所有文件的 package 声明和 import
- 编译验证

## 验证
```bash
./gradlew assembleDebug
```
编译通过并生成 APK。
