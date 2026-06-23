# 阶段 04：布局模型、测量与渲染

> 顺序：04  
> 目标：实现全新布局系统，包括模型解析、校验、继承与 patch、测量与渲染。  
> 依据：`docs/layout.md`、`docs/core.md` 第 4 节

## 1. 预期目标

本阶段结束时：

- `LayoutDoc` 模型可从 JSONC 解析，校验、继承、patch、测量、渲染全链路可用。
- `LayoutRegistry`、`LayoutMeasurer`、`LayoutHintResolver`、`BindingsEvaluator` 真实实现，替换阶段 01 的 stub。
- 渲染器可依赖 `ThemeResolver`，按 `KeyStyle` 渲染按键，硬编码颜色全部删除。
- `ActionDispatcher` 可创建 `InputAction` 并传递给 `InputPipeline`（阶段 05 实现 pipeline 逻辑）。
- 旧布局模型（`LayoutModels.kt`）、旧布局计算（`GridCalculator.kt`）、旧布局仓库（`LayoutRepository.kt`）、旧 Patch（`PatchApplier.kt`）已直接删除。
- 旧 v1 布局文件已删除，`v2` 子目录已消除，布局文件统一放在 `assets/layouts/` 下。

**依赖说明**：本阶段使用阶段 01 定义的 stub `InputPipeline`（不真实处理动作，只记录调用）和阶段 03 的真实 `ThemeResolver`。

## 2. 前置依赖

- 阶段 01 的 `LayoutRegistry`、`LayoutMeasurer`、`LayoutHintResolver`、`BindingsEvaluator`、`InputAction`、`LayoutDoc`、`KeyDef` 等契约已定义。
- 阶段 02 的 `KeyboardContextManager` 可提供 `StateFlow<KeyboardContext>`。
- 阶段 03 的 `ThemeResolver` 可解析 `KeyStyle`。

## 3. 实施步骤

### 4.1 布局数据模型实现

做什么：

- 实现阶段 01 定义的 `LayoutDoc`、`LayoutContainer` sealed class（`rows`/`columns`/`grid`/`scroll`/`fixed`）、`KeyDef`、`ContentSpec`、`VariantPatch`、`Dimension` sealed class、`ActionDef`、`ActionMap`、`GestureType`、`HitShape` sealed class、`Region`、`RegionRole`、`LayoutMeta`、`LayoutEnv`、`Bindings`、`ScrollSpec`、`BoxSpacing`、`Orientation`、`Gravity`、`HintPosition` 等模型。
- 全部 `@Serializable`，支持 JSONC 解析（使用新工具函数 `JsoncParser.stripComments`）。
- 实现继承和 patch：`LayoutDoc.extends` 和 `LayoutDoc.patches` 字段，解析期 resolve。
- **直接删除**旧 `LayoutModels.kt`（`KeyboardLayout`/`KeyData`/`RowData`/`ArrangementData`/`GeometryConfig`）。
- **直接删除**旧 `LayoutParser.kt`（含旧 `stripJsonLineComments`），用新 `JsoncParser` 替代。

测试：

- 解析布局文件成功。
- 继承和 patch 解析正确，子布局可引用父布局。
- 缺 `rows` 或 `columns` 或 `grid` 定义时解析失败。
- 旧 `LayoutModels.kt`、`LayoutParser.kt` 引用已全部清除。

预期目标：

- 布局模型完全符合设计文档，支持继承和 patch。

性能：

- 单个布局文件解析目标小于 50 ms。

### 4.2 `LayoutRegistry` 真实实现

做什么：

- 实现阶段 01 定义的 `LayoutRegistry` 接口，替换阶段 01 的 `StubLayoutRegistry`。
- `register(doc: LayoutDoc, source: LayoutSource): RegisterResult`：注册新布局。
- `get(layoutId: String): LayoutDoc?`：获取布局，支持继承 resolve。
- `validate(doc: LayoutDoc): List<LayoutIssue>`：校验布局。
- `findBuiltIn(id: String): LayoutDoc?`：查找内置布局。
- 注册期校验：`rows`/`columns`/`grid` 有且仅有一个，`ActionDef.action` 合法，`styleRef` 非空，`dimension` 合法。
- **直接删除**旧 `LayoutRepository.kt`。

测试：

- 合法布局注册成功。
- 非法布局注册失败，返回 `LayoutIssue`。
- 继承解析正确，子布局合并父布局内容。
- 旧 `LayoutRepository.kt` 引用已全部清除。

预期目标：

- 布局注册和查询可用。

性能：

- `get` 使用 map 缓存，目标 O(1)。

### 4.3 `LayoutMeasurer` 实现

做什么：

- 实现阶段 01 定义的 `LayoutMeasurer` 接口。
- `measure(doc: LayoutDoc, layer: LayoutLayer, w: Int, h: Int): MeasuredLayout`：测量布局。
- `invalidate(layoutId: String? = null)`：清除缓存。
- 输出 `MeasuredLayout`、`MeasuredRegion`、`MeasuredKey`。
- **直接删除**旧 `GridCalculator.kt`。

测试：

- 给定布局和尺寸，输出测量结果。
- 测量结果可用于渲染。
- 旧 `GridCalculator.kt` 引用已全部清除。

预期目标：

- 布局测量可用，渲染器可消费 `MeasuredLayout`。

性能：

- 单次测量目标小于 5 ms。
- 缓存机制避免重复测量。

### 4.4 `LayoutHintResolver` 实现

做什么：

- 实现阶段 01 定义的 `LayoutHintResolver` 接口。
- `resolve(hint: LayoutHint, currentLayoutId: String): String`：根据 `EditorInfo.inputType` 提示返回布局 ID。
- 映射规则：`text` → 当前布局，`number` → `builtin:number`，`phone` → `builtin:phone`，`uri` → 当前布局，`email` → 当前布局，`password` → 当前布局。
- 未知 hint 返回当前布局。

测试：

- `resolve(LAYOUT_HINT_TEXT, "zh:qwerty")` 返回 `"zh:qwerty"`。
- `resolve(LAYOUT_HINT_NUMBER, "zh:qwerty")` 返回 `"builtin:number"`。

预期目标：

- 阶段 06 `EditorInfoResolver` 可调用此接口决定初始布局。

性能：

- 单次 resolve 小于 0.5 ms。

### 4.5 `BindingsEvaluator` 实现

做什么：

- 实现阶段 01 定义的 `BindingsEvaluator` 接口。
- `evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean>`：评估 region 的 visible 和 enabled。
- 评估规则：`visibleWhen` 和 `enabledWhen` 表达式求值。

测试：

- `visibleWhen = "composingActive"` 在 composing 时返回 `true`。
- `enabledWhen = "hasCandidates"` 在有候选时返回 `true`。

预期目标：

- 布局 region 可根据状态动态显隐。

性能：

- 单次 evaluate 小于 0.5 ms。

### 4.6 渲染器实现

做什么：

- 实现全新渲染器，消费 `MeasuredLayout` 和 `ThemeResolver`。
- 按 `KeyStyle` 渲染按键，无硬编码颜色。
- 支持手势：点击、长按、滑动。
- **直接删除**旧 `ComposeInputView.kt` 中的 `mapKeyToAction` 硬编码映射。
- **直接删除**旧 `KeyBindingModels.kt`、`KeyBindingManager.kt`（新手势处理在 `ActionDispatcher` 中）。

测试：

- 渲染输出符合 `ThemeDoc` token。
- 手势触发正确的 `InputAction`。
- 硬编码颜色已全部删除。
- 旧 `KeyBindingModels.kt`/`KeyBindingManager.kt` 引用已全部清除。

预期目标：

- 渲染器完全依赖主题 token，支持手势。

性能：

- 渲染帧率目标 60 fps。
- 避免不必要的 recomposition。

### 4.7 `ActionDispatcher` 实现

做什么：

- 实现全新 `ActionDispatcher`，消费 `MeasuredKey` 和手势事件。
- 根据 `KeyDef` 和手势类型创建 `InputAction`。
- 将 `InputAction` 传递给 `InputPipeline.handle()`（阶段 05 实现真实处理）。
- 本阶段使用 stub `InputPipeline`（只记录调用）。
- **直接删除**旧 `ActionDispatcher.kt`（死代码）。

测试：

- 点击按键触发 `PushToken` 或对应 `InputAction`。
- 长按触发 `LongPress` 动作。
- stub `InputPipeline` 记录调用。

预期目标：

- 手势到 `InputAction` 的转换可用。

性能：

- 单次转换小于 1 ms。

### 4.8 内置布局文件与 assets 清理

做什么：

- **删除所有旧布局文件**：
  - `assets/layouts/*.json`（v1 格式）：`candidate.json`、`dialer.json`、`handwriting.json`、`numeric.json`、`qwerty.json`、`qwertyv2.json`、`scheme.json`、`t9.json`、`test_qwerty.json`、`zh_cn_cluster.json`。
  - `assets/layouts/v2/` 整个子目录（含 `proposed_extensible_keyboard.jsonc` 参考提案）。
  - `assets/layouts/v2/` 中的 `qwerty.jsonc`、`shuangpin.jsonc`、`t9.jsonc`、`candidate.jsonc` 按新模型调整字段后，放到 `assets/layouts/` 下。
- **在 `assets/layouts/` 下创建内置布局**（全部 JSONC 格式）：
  - `qwerty.jsonc`（标准 QWERTY）
  - `shuangpin.jsonc`（双拼）
  - `t9.jsonc`（T9 九宫格）
  - `candidate.jsonc`（候选栏）
  - `number.jsonc`（数字键盘）
  - `phone.jsonc`（电话拨号盘）
  - `symbols.jsonc`（符号面板）
- 最终 `assets/layouts/` 目录下只有 `.jsonc` 文件，没有子目录。

测试：

- 所有内置布局注册成功。
- 布局校验通过。
- `assets/layouts/` 下无 `.json` 文件，无 `v2` 子目录。

预期目标：

- 阶段 05 引擎有布局可用。
- assets 目录干净，无旧格式遗留。

性能：

- 内置布局总解析目标小于 200 ms。

### 4.9 删除旧子类型配置

做什么：

- **直接删除** `assets/subtypes/` 整个目录（含 `generated.json`）。
- 后续由阶段 07 `SubtypeBridge` 动态生成 subtype。

测试：

- `assets/subtypes/` 目录不存在。

预期目标：

- subtype 不再有静态配置文件。

## 4. 阶段验收

运行：

```bash
./gradlew test
./gradlew assembleDebug
```

静态检查：

```bash
rg "LayoutModels|GridCalculator|LayoutRepository|PatchApplier|KeyBindingModels|KeyBindingManager|mapKeyToAction|LayoutParser" app/src/main/java
```

```bash
find app/src/main/assets/layouts -name "*.json" -o -name "v2"  # 应无结果
```

```bash
ls app/src/main/assets/subtypes 2>/dev/null  # 应不存在
```

验收标准：

- `LayoutDoc` 模型可解析、校验、继承、patch。
- `LayoutRegistry`、`LayoutMeasurer`、`LayoutHintResolver`、`BindingsEvaluator` 真实实现，替换 stub。
- 渲染器依赖 `ThemeResolver`，无硬编码颜色。
- `ActionDispatcher` 可创建 `InputAction` 并传递给 stub `InputPipeline`。
- 旧布局模型、计算器、仓库、Patch、按键绑定、硬编码映射、旧 `LayoutParser` 已直接删除。
- `assets/layouts/` 下只有 `.jsonc` 文件，无 `.json` 文件，无 `v2` 子目录。
- `assets/subtypes/` 目录已删除。
- 内置布局文件可加载。
