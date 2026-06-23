# 阶段 00：实施总览

> 目标：提供全局视角，确保所有阶段依赖关系正确、无遗漏。

## 1. 重构原则

- **全新重构**：所有代码从零重写，旧代码直接删除，不迁移、不标记 @Deprecated、不复用旧实现。
- **阶段递进**：每个阶段结束时工程可编译、可生成 APK。
- **契约先行**：阶段 01 定义所有跨层接口，后续阶段只实现不改签名。
- **stub 过渡**：阶段 01-02 使用 stub，阶段 04-05 替换真实实现，阶段 05 删除 stub。

## 2. 阶段依赖链

```
01（契约 + stub）→ 02（状态 + Manifest + 设置）→ 03（主题真实实现）
                                                 → 04（布局真实实现，替换 LayoutRegistry stub）
                                                 → 05（引擎真实实现，替换 7 个 stub）
                                                 → 06（桥接真实实现，替换 fake）
                                                 → 07（subtype/权限/i18n）
                                                 → 08（扩展面板 + 语音/LLM）
                                                 → 09（语言包导入 + 自定义）
                                                 → 10（集成测试 + 性能优化 + 发布）
```

## 3. 各阶段核心产物

| 阶段 | 核心产物 | 旧代码/资源删除 |
|------|---------|----------------|
| 01 | 20 个跨层接口契约 + 7+1 个 stub + fake 组件 + 旧结构删除清单 | — |
| 02 | `KeyboardContextManager` + `TransitionEngine` + `OrthogonalRegistry` + `SettingsManager` 扩展 + 内置 Manifest | `KeyboardState`、`KeyboardStateManager`、`LanguageSwitchManager`、旧 `InputAction.kt`、`InputMethodConfig`、`EngineType` |
| 03 | `ThemeResolver` 真实实现 + 内置浅色/深色主题 + 反馈参数 token 化 | `ThemeColors`、`KeyColors`、`ActionColors`、`BuiltInThemes`、旧 `ThemeResolver`、旧 `ThemeModels.kt` |
| 04 | `LayoutRegistry`/`LayoutMeasurer`/`LayoutHintResolver`/`BindingsEvaluator` 真实实现 + 渲染器 + `ActionDispatcher` + `JsoncParser` | `LayoutModels.kt`、`GridCalculator.kt`、`LayoutRepository.kt`、`PatchApplier.kt`、`LayoutParser.kt`、`KeyBindingModels.kt`、`KeyBindingManager.kt`、旧 `ActionDispatcher.kt`、`mapKeyToAction` 硬编码、`assets/layouts/*.json`、`assets/layouts/v2/`、`assets/subtypes/` |
| 05 | `InputPipeline` + 5 个 Registry 真实实现 + `EngineResourceResolver` 真实实现 + 存在性校验补齐 + stub 删除 | `InputEngine.kt`、`DirectInputEngine.kt`、`CompositionInputEngine.kt`、`TrieDict.kt`、`SuggestionEngine.kt`、`DictionaryImporter.kt` |
| 06 | `InputConnectionGateway`/`FeedbackPlayer`/`EditorInfoResolver`/`SelectionTracker`/`HardwareKeyRouter` 真实实现 + `MyBoardImeService.kt` 改造 | 硬编码颜色、旧 `method.xml` |
| 07 | `SubtypeBridge` + `PermissionGateway` + i18n 补齐 + labelKey 校验 | —（`assets/subtypes/` 已在阶段 04 删除） |
| 08 | 面板系统 + 剪贴板/表情/符号/颜文字/语音/LLM 面板 | 旧面板代码（如有） |
| 09 | `LanguagePackImporter` + 自定义布局/主题导入 + `TextExpansionManager` 接入 | 旧自定义代码（如有） |
| 10 | 集成测试 + 性能优化 + 最终旧代码确认删除 + 发布 | 确认所有旧代码/资源已删除 |

## 4. 旧代码删除策略

- **直接删除**：旧代码在实现对应新代码的阶段中直接删除，不做迁移。
- **不标记 @Deprecated**：因为不需要兼容性，旧代码直接删除。
- **保留数据文件**：字典、表情、符号数据保留，路径和格式按新模型调整。
- **保留工具函数**：`LayoutParser.stripJsonLineComments` 提取为新工具函数 `JsoncParser.stripComments`。
- **保留设置/仓库**：`SettingsManager.kt`、`EmojiRepository.kt`、`SymbolRepository.kt`、`KaomojiRepository.kt`、`ClipboardManager.kt` 在现有基础上扩展。
- **保留接口**：`STTProvider.kt`、`LLMProvider.kt` 接口保留，接入新 Schema/面板。
- **保留逻辑**：`TextExpansionManager.kt` 逻辑保留，作为扩展能力接入。
- **保留 UI**：`SettingsScreen.kt`、`ThemeSettingsScreen.kt` UI 保留，在现有基础上扩展。

## 5. 可保留资源汇总

| 保留项 | 原位置 | 处理方式 |
|--------|--------|---------|
| JSONC 注释剥离 | `LayoutParser.stripJsonLineComments` | 提取为新工具函数 `JsoncParser.stripComments` |
| 布局 JSONC | `assets/layouts/v2/qwerty.jsonc` 等 | 按新模型调整字段，移到 `assets/layouts/` 下，删除 v2 目录 |
| 词典数据 | `assets/dictionary/` | 保留 |
| 表情数据 | `assets/emoji/emoji.json`、`kaomoji.json` | 保留 |
| 符号数据 | `assets/symbols/symbols.json` | 保留 |
| `SettingsManager.kt` | `core/settings/` | 在现有基础上扩展 |
| `EmojiRepository.kt` | 仓库层 | 保留，接入新面板结构 |
| `SymbolRepository.kt` | 仓库层 | 保留，接入新面板结构 |
| `KaomojiRepository.kt` | 仓库层 | 保留，接入新面板结构 |
| `ClipboardManager.kt` | 仓库层 | 保留，接入新面板结构 |
| `STTProvider.kt` | 扩展层 | 保留接口，接入新 Schema |
| `LLMProvider.kt` | 扩展层 | 保留接口，接入新面板 |
| `TextExpansionManager.kt` | 扩展层 | 保留逻辑，接入新系统 |
| `SettingsScreen.kt` | UI 层 | 保留 UI，在现有基础上扩展 |
| `ThemeSettingsScreen.kt` | UI 层 | 保留 UI，在现有基础上扩展 |

## 6. 需删除的旧资源汇总

| 删除项 | 原位置 | 删除阶段 |
|--------|--------|---------|
| v1 布局 JSON | `assets/layouts/*.json`（非 v2 子目录） | 04 |
| v2 子目录 | `assets/layouts/v2/`（含 `proposed_extensible_keyboard.jsonc` 参考提案） | 04 |
| 子类型配置 | `assets/subtypes/` 整个目录 | 04 |

## 7. 阶段验收汇总

每个阶段验收标准：

1. `./gradlew test` 通过。
2. `./gradlew assembleDebug`（或 `assembleRelease`）通过。
3. 该阶段旧代码已直接删除，`rg` 搜索无引用。
4. 该阶段新功能可编译可测。
5. 无硬编码颜色、硬编码路由、硬编码语言判断。
