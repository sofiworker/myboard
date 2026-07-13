# MyBoard 主题包与皮肤系统设计文档

日期: 2026-07-13

## 1. 背景与目标

`IM/` 目录中的参考图覆盖了几类输入法皮肤：

- 纯 token 主题：只改变颜色、圆角、阴影、字体、功能键配色。
- 质感主题：按键有高光、玻璃、金属、阴影、描边、渐变。
- 背景图主题：键盘底板带图片、纹理、渐变或半透明遮罩。
- 贴纸主题：小图标、角色、水印、装饰条覆盖在键盘表面。
- 溢出装饰主题：装饰图从一个按键延伸到相邻按键或整块键盘区域。
- 结构型主题：视觉设计要求某些键位尺寸、位置或行列结构不同。

当前 MyBoard 已经具备主题系统雏形：`ThemeDoc`、`ThemeResolverImpl`、`assets/themes/*.jsonc`、`ThemeSettingsScreen`，以及布局文件中的 `styleRef`。但它仍然偏“颜色表”，还不能表达图片皮肤、装饰层、主题资源包、用户导入和主题内可选布局变体。

本文档目标：

- 定义一套正式的主题包格式，让设计师和开发者能把视觉想法落地为可导入皮肤包。
- 明确主题与 Layout 的边界：主题默认不改输入行为和几何，确有必要时通过显式 Layout 绑定处理。
- 设计主题解析、资源加载、渲染分层、校验和设置接入方案。
- 设计主题定义器，用于创建、预览、校验和导出自定义主题。
- 说明当前代码如何接入，不破坏正交状态、输入引擎和布局层主流程。

## 2. 当前系统现状

### 2.1 已有能力

当前代码中相关入口：

- `app/src/main/assets/themes/light.jsonc`
- `app/src/main/assets/themes/dark.jsonc`
- `xyz.xiao6.myboard.theme.ThemeDoc`
- `xyz.xiao6.myboard.theme.ThemeResolverImpl`
- `xyz.xiao6.myboard.theme.BuiltInThemes`
- `xyz.xiao6.myboard.ui.settings.ThemeSettingsScreen`
- `xyz.xiao6.myboard.app.MyBoardImeService`
- `xyz.xiao6.myboard.layout.LayoutRenderer`
- `xyz.xiao6.myboard.ui.keyboard.Toolbar`
- `xyz.xiao6.myboard.ui.keyboard.CandidateBar`
- `SettingsRepository` 中的 `theme_mode`、`current_theme`

当前布局层已经遵守一个正确方向：`LayoutDoc` 只放 `styleRef`，颜色和视觉由主题层解析。这是皮肤系统继续扩展的基础。

### 2.2 当前短板

- `current_theme` 在设置页可写入，但 IME 初始化主题时主要看 `theme_mode`，没有完整消费 `current_theme`。
- `BuiltInThemes.kt` 与 `assets/themes/*.jsonc` 存在两套内置主题来源，容易状态不一致。
- `ThemeDoc` 只覆盖少量颜色、按键样式和反馈 token，无法表达图片、贴纸、纹理、渐变、阴影层级、装饰层。
- `LayoutRenderer` 目前按“按键背景 -> 文字/角标”单 pass 绘制，无法插入 Image #1 这种跨按键装饰。
- `Toolbar` 和 `CandidateBar` 只消费 `ChromeColors`，还不能表达主题化圆角、背景图、边框、阴影。
- 没有主题包导入、资源沙箱、预览图、主题定义器和自定义主题校验流程。

## 3. 行业做法抽象

输入法、桌面 UI、Web 设计系统和游戏 UI 的主题包通常遵循同一类思路：

1. **资源包化**：主题不是单个颜色配置，而是 manifest、样式 token、图片资源、预览图和可选扩展文件的集合。
2. **主题与布局分离**：默认情况下主题只改变视觉，布局只负责几何和交互。需要结构变化时使用独立布局或布局 patch。
3. **token 层抽象**：设计师定义语义 token，如 `key.default.background`、`key.action.text`、`surface.keyboard`，渲染层不直接依赖具体颜色。
4. **资源引用间接化**：JSON 中只写 `assetId`，解析器负责把它映射到安全路径、密度版本和可绘制对象。
5. **预览优先**：主题包附带 preview，编辑器提供实时预览，多布局、多状态、多深浅色同时检查。
6. **严格校验**：导入包要验证 schema、路径、图片尺寸、文件大小、引用完整性、版本兼容、权限安全。
7. **降级策略**：主题某部分不可用时跳过局部效果或回退默认主题，不能让输入法崩溃。

MyBoard 应采用这种资源包模型，而不是把每张参考图硬编码成特殊布局或特殊渲染逻辑。

## 4. 核心设计决策

### 4.1 主题默认不修改 Layout

首版皮肤系统不让主题隐式改变键位几何、按键动作或输入语义。

理由：

- 当前 `docs/layout.md` 已明确布局层负责几何与动作，主题层负责视觉。
- 输入法的稳定性依赖按键命中区域和动作映射可预测。
- 设计师常见的“看起来跨键”“贴纸压住键”“角色站在键上”可以通过装饰层实现，不需要改 Layout。
- 如果主题可以随意改 Layout，会让 `KeyboardContext.layoutId`、Manifest、设置页和输入行为变得难以追踪。

### 4.2 需要修改 Layout 时走显式绑定

有些皮肤确实需要改变结构，例如：

- 每行按键数量不同。
- 空格、回车、退格尺寸变化。
- T9 与 QWERTY 都使用特殊排布。
- 主题设计要求大面积异形键位，而不是单纯装饰覆盖。

这种情况使用“主题包附带布局”处理：

- 主题包可以携带 `layouts/*.jsonc` 或 `layoutPatches/*.jsonc`。
- 主题 manifest 显式声明 `layoutBindings`，把基础布局映射到皮肤布局。
- 导入时布局文件必须通过 `LayoutRegistry.validate`。
- 应用主题时，设置页必须让用户知道该主题会切换布局。
- 主题不能偷偷改变当前 Schema 的动作；布局动作仍然来自 `LayoutDoc`，并经过同一套注册校验。

首版推荐只做“视觉主题 + 装饰层”，不立即修改现有 Layout 数据模型。Layout 绑定作为主题包协议预留，等布局继承和 patch 能力稳定后再开放给用户主题。

### 4.3 溢出装饰不参与命中测试

Image #1 中信封、小猫、星星、便签这类元素会覆盖多个按键。它们应作为 `DecorationDef` 绘制在键盘表面：

- 可以锚定某个按键、某组按键、某个区域或整个键盘。
- 可以超出锚定按键边界。
- 默认不参与 hit test。
- 触摸仍然按 `MeasuredLayout.keys` 的几何区域命中。
- 装饰图的 zIndex 和 drawLayer 控制它在按键背景、文字、角标之间的层级。

这能复刻“图标溢出到其它按键”的视觉，同时不破坏输入。

## 5. 概念模型

### 5.1 ThemePackage

主题包是磁盘上的完整资源集合。内置主题位于 assets，用户主题位于 app 私有目录。

```text
theme.mybskin
  manifest.jsonc
  theme.jsonc
  assets/
    background.webp
    mail_cat.png
    star.png
    key_texture.webp
  layouts/
    qwerty_mail_cat.jsonc
  layoutPatches/
    qwerty_mail_cat_patch.jsonc
  previews/
    preview_light.webp
    preview_dark.webp
```

`.mybskin` 本质是 zip。导入后解压到：

```text
files/themes/{themeId}/
```

内置主题建议迁移到：

```text
app/src/main/assets/themes/{themeId}/manifest.jsonc
app/src/main/assets/themes/{themeId}/theme.jsonc
```

### 5.2 ThemeManifest

`manifest.jsonc` 描述包元数据、入口文件、兼容版本、预览图和可选布局绑定。

```jsonc
{
  "schemaVersion": "1.0.0",
  "packageId": "mail_cat",
  "displayName": {
    "en-US": "Mail Cat",
    "zh-CN": "信件小猫"
  },
  "author": "MyBoard",
  "entry": "theme.jsonc",
  "previews": {
    "light": "previews/preview_light.webp",
    "dark": "previews/preview_dark.webp"
  },
  "compatibility": {
    "minAppThemeSchema": "1.0.0",
    "requiresLayoutSchema": "1.0.0"
  },
  "capabilities": {
    "usesImages": true,
    "usesDecorations": true,
    "usesLayoutBindings": false
  },
  "layoutBindings": []
}
```

### 5.3 ThemeDoc

`theme.jsonc` 描述视觉 token、资源引用、装饰层、反馈和可选布局绑定。

```kotlin
data class ThemeDoc(
    val schemaVersion: String,
    val id: String,
    val name: LocalizedText,
    val dark: Boolean? = null,
    val variants: Map<ThemeVariant, ThemeVariantDoc>,
    val assets: Map<String, ThemeAssetDef>,
    val colors: Map<String, ColorToken>,
    val typography: Map<String, TypographyToken>,
    val effects: Map<String, EffectToken>,
    val keyStyles: Map<String, KeyStyleDef>,
    val chromeStyles: ChromeStyleSection,
    val decorations: List<DecorationDef>,
    val feedback: FeedbackSection,
    val layoutBindings: List<ThemeLayoutBinding>
)
```

`dark` 只保留兼容旧主题。新主题应使用 `variants.light` / `variants.dark`。

### 5.4 ThemeRuntime

`ThemeRuntime` 是解析后的不可变运行时对象：

```kotlin
data class ThemeRuntime(
    val id: String,
    val variant: ThemeVariant,
    val keyStyles: Map<String, ResolvedKeyStyle>,
    val chrome: ResolvedChromeStyle,
    val decorations: List<ResolvedDecoration>,
    val feedback: FeedbackPolicy,
    val assetResolver: ThemeAssetResolver
)
```

渲染层只消费 `ThemeRuntime` 或 `ThemeResolver` 暴露出的稳定接口，不直接读磁盘 JSON。

## 6. PaintSpec 与样式 token

当前 `KeyStyleDef.background` 是字符串颜色，不够。应升级为 paint 描述。

```kotlin
sealed class PaintSpec {
    data class Solid(val color: String) : PaintSpec()
    data class LinearGradient(
        val colors: List<String>,
        val angle: Float = 90f
    ) : PaintSpec()
    data class RadialGradient(
        val colors: List<String>,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f
    ) : PaintSpec()
    data class Image(
        val assetId: String,
        val scaleType: ScaleType = ScaleType.Crop,
        val alpha: Float = 1f,
        val tint: String? = null
    ) : PaintSpec()
}
```

按键样式建议扩展为：

```kotlin
data class KeyStyleDef(
    val background: PaintSpec,
    val pressedBackground: PaintSpec? = null,
    val textColor: String,
    val pressedTextColor: String? = null,
    val hintColor: String? = null,
    val iconTint: String? = null,
    val fontRef: String? = null,
    val fontSize: Float,
    val fontWeight: Int = 500,
    val cornerRadius: Float,
    val border: BorderSpec? = null,
    val shadow: ShadowSpec? = null,
    val innerShadow: ShadowSpec? = null,
    val highlight: HighlightSpec? = null,
    val texture: PaintSpec? = null
)
```

这样可以覆盖：

- 圆润白色按键。
- 深色霓虹按键。
- 玻璃高光按键。
- 黑白极简按键。
- 带纹理的空格键或功能键。

## 7. DecorationDef：溢出图标和贴纸系统

### 7.1 设计目标

`DecorationDef` 负责表达所有“不是按键，但画在键盘上”的视觉元素：

- 背景水印。
- 角色贴纸。
- 星星、气泡、便签。
- 横跨多个按键的信封图。
- 按键边缘的发光条。
- 主题品牌角标。

### 7.2 数据结构

```kotlin
data class DecorationDef(
    val id: String,
    val assetId: String,
    val anchor: DecorationAnchor,
    val size: DecorationSize,
    val offset: DpOffset = DpOffset.Zero,
    val alpha: Float = 1f,
    val rotation: Float = 0f,
    val scaleType: ScaleType = ScaleType.Fit,
    val drawLayer: DecorationLayer,
    val zIndex: Int = 0,
    val clip: DecorationClip = DecorationClip.Keyboard,
    val hitTest: DecorationHitTest = DecorationHitTest.Ignore,
    val visibleWhen: DecorationCondition? = null
)
```

锚点：

```kotlin
sealed class DecorationAnchor {
    data class Keyboard(val x: Float, val y: Float) : DecorationAnchor()
    data class Key(val keyId: String, val x: Float, val y: Float) : DecorationAnchor()
    data class KeyGroup(val keyIds: List<String>, val x: Float, val y: Float) : DecorationAnchor()
    data class Region(val regionId: String, val x: Float, val y: Float) : DecorationAnchor()
}
```

绘制层：

```kotlin
enum class DecorationLayer {
    Background,
    AboveSurface,
    BelowKeys,
    AboveKeysBelowLabels,
    AboveLabels,
    Foreground
}
```

裁剪策略：

```kotlin
enum class DecorationClip {
    None,
    Keyboard,
    ContentArea,
    AnchorBounds
}
```

默认使用 `Keyboard`：装饰可以溢出按键，但不会画出整个键盘区域。

### 7.3 Image #1 示例

信封贴纸跨过 `s`、`d`、`z`、`x` 等按键时，不应该改这些按键的定义。示例：

```jsonc
{
  "id": "mail_envelope",
  "assetId": "mail_envelope",
  "anchor": {
    "type": "keyGroup",
    "keyIds": ["s", "d", "z", "x"],
    "x": 0.5,
    "y": 0.45
  },
  "size": { "widthDp": 98, "heightDp": 62 },
  "offset": { "xDp": 0, "yDp": -8 },
  "drawLayer": "AboveKeysBelowLabels",
  "zIndex": 20,
  "clip": "Keyboard",
  "hitTest": "Ignore"
}
```

右下角小兔子覆盖回车键附近：

```jsonc
{
  "id": "enter_bunny",
  "assetId": "bunny_sticker",
  "anchor": {
    "type": "key",
    "keyId": "enter",
    "x": 0.65,
    "y": 0.55
  },
  "size": { "widthDp": 58, "heightDp": 78 },
  "offset": { "xDp": -4, "yDp": -10 },
  "drawLayer": "AboveLabels",
  "zIndex": 40,
  "clip": "Keyboard",
  "hitTest": "Ignore"
}
```

重点：视觉可以覆盖回车键，但点击仍然命中回车键。

## 8. 渲染分层

当前 `LayoutRenderer` 应升级为分层渲染。推荐结构：

```text
KeyboardSurface
  1. keyboard background / background image
  2. chrome background: toolbar/candidate/panel surface
  3. DecorationLayer.Background
  4. key shadows
  5. key backgrounds / textures / borders
  6. DecorationLayer.AboveKeysBelowLabels
  7. key labels / hints / icons
  8. DecorationLayer.AboveLabels
  9. foreground watermark / overlay
  10. gesture hit layer
```

手势层必须独立于装饰层：

```text
PointerInput
  -> findHitKey(position, measuredLayout)
  -> ActionDispatcher.dispatch(key, gesture, context)
  -> InputPipeline
```

装饰层只画图，不影响命中，除非未来明确支持可点击装饰。首版禁止可点击装饰，避免装饰绕过 `InputPipeline`。

## 9. 与当前 Layout 的接入

### 9.1 不改变 LayoutDoc 的基础契约

已有布局文件中的 `styleRef` 继续有效：

```jsonc
{
  "key": {
    "id": "space",
    "styleRef": "key_space",
    "content": { "label": "@string/keyboard_key_space" }
  }
}
```

主题只解析 `key_space` 的视觉，不改变 `space` 的动作、位置或大小。

### 9.2 按 keyId 做视觉选择器

为了支持“某些具体键带贴纸/颜色”的皮肤，又不改 Layout，可以引入 selector 覆盖：

```jsonc
{
  "styleOverrides": [
    {
      "selector": { "keyId": "enter" },
      "styleRef": "key_enter_special"
    },
    {
      "selector": { "keyIds": ["shift", "backspace"] },
      "styleRef": "key_corner_decoration"
    }
  ]
}
```

限制：

- selector 只能覆盖视觉 style。
- 不能覆盖 `actions`。
- 不能覆盖几何。
- selector 未命中时自动跳过。

这比要求设计师复制整个 `qwerty.jsonc` 更安全。

### 9.3 主题包内布局绑定

确实需要改布局时，使用 `layoutBindings`：

```jsonc
{
  "layoutBindings": [
    {
      "baseLayoutId": "qwerty",
      "replacementLayoutId": "mail_cat_qwerty",
      "mode": "optIn",
      "reason": {
        "zh-CN": "此皮肤包含专用 QWERTY 排列，以适配信件贴纸和大回车键。",
        "en-US": "This skin includes a dedicated QWERTY layout for its sticker composition."
      }
    }
  ]
}
```

`mode`：

- `none`：不绑定布局。
- `optIn`：用户应用主题时可选择是否启用专用布局。
- `required`：该主题必须启用专用布局，否则只显示为不可用或降级为默认视觉主题。

首版只支持 `none` 和 `optIn`。不建议开放 `required`，因为输入法布局变化对用户肌肉记忆影响较大。

### 9.4 当前是否要动 Layout

结论：首版不动现有 Layout 数据模型，不把参考图转成新布局。

首版应做：

- 扩展 `ThemeDoc`。
- 扩展 `ThemeResolver`。
- 扩展 `LayoutRenderer` 绘制层。
- 让 `current_theme` 真正参与主流程。
- 增加主题包导入、预览和校验。
- 支持基于 `keyId` / `styleRef` 的视觉 selector。
- 支持 decoration 溢出。

暂缓：

- 布局 patch 编辑器。
- 主题自动修改布局。
- 主题内 required layout。
- 可点击装饰。

这样能覆盖大部分参考图，风险最低。

## 10. 主题解析主流程

### 10.1 新组件

建议新增：

- `ThemeCatalog`：列出内置主题和用户主题。
- `ThemePackageImporter`：导入 `.mybskin`。
- `ThemePackageValidator`：校验包结构、JSON、资源、layoutBindings。
- `ThemeRuntimeProvider`：根据设置和系统深浅色解析当前 `ThemeRuntime`。
- `ThemeAssetResolver`：把 assetId 解析成 `ImageBitmap` / `Painter` / 字体资源。
- `ThemePreviewRenderer`：给设置页和主题定义器渲染预览。

### 10.2 设置流

设置仍然以 `SettingsRepository` 为唯一来源：

```text
SettingsActivity
  -> SettingsRepository.updateSetting("current_theme", themeId)
  -> SettingsRepository.updateSetting("theme_mode", auto/light/dark)
  -> MyBoardImeService collect settings
  -> ThemeRuntimeProvider.resolve(current_theme, theme_mode, systemDark)
  -> ThemeResolver.setTheme(runtime)
  -> LayoutRenderer / Toolbar / CandidateBar 重组
```

`SettingsActivity.kt` 必须保留所有主题设置入口，不能在其它地方维护第二份主题状态。

### 10.3 系统深浅色

`theme_mode`：

- `auto`：使用系统深浅色，选择 `variants.light` 或 `variants.dark`。
- `light`：强制 light variant。
- `dark`：强制 dark variant。

如果主题没有对应 variant：

- 没有 dark 时，使用 light 并应用可选 dim overlay。
- 没有 light 时，使用 dark。
- 两者都没有，主题无效，回退默认主题。

### 10.4 BuiltInThemes 去重

最终应删除或弱化 `BuiltInThemes.kt` 中的完整主题常量。内置主题也从 assets 解析，避免 Kotlin 常量与 JSONC 不一致。

过渡期可以保留 `BuiltInThemes` 作为 fallback，但 `ThemeCatalog` 的主来源应是：

```text
assets/themes/*/manifest.jsonc
files/themes/*/manifest.jsonc
```

## 11. 主题定义器

### 11.1 定位

主题定义器是设计师/开发者创建皮肤包的工具。它属于设置体系，入口必须在 `SettingsActivity.kt` 里的主题页面下。

目标不是首版做完整 Figma，而是提供可靠的落地通道：

- 创建主题。
- 导入图片。
- 编辑 token。
- 添加装饰。
- 多布局预览。
- 校验和导出 `.mybskin`。

### 11.2 模式

基础模式：

- 选择基础模板。
- 设置键盘背景色/背景图。
- 设置默认键、功能键、动作键、空格键、候选栏、工具栏颜色。
- 设置圆角、阴影、描边、字体大小。
- 保存为用户主题。

高级模式：

- JSONC 编辑器。
- 装饰层可视化定位。
- selector 配置。
- 资源列表管理。
- 主题包校验面板。
- 可选布局绑定。

### 11.3 预览状态

至少要预览：

- QWERTY 正常态。
- T9 或双拼布局。
- 候选栏显示态。
- 工具栏显示态。
- 符号/Emoji full-surface 页面。
- light/dark variant。
- 不同键盘高度和左右 inset。

预览使用模拟 `KeyboardContext` 和现有 `LayoutMeasurer/LayoutRenderer`，不能维护一套独立预览渲染逻辑。

### 11.4 权限

导入图片和主题包使用系统文件选择器：

- `ACTION_OPEN_DOCUMENT`
- 复制到 app 私有目录
- 不申请外部存储权限

这符合最小权限原则。

## 12. 设计师/开发者落地流程

### 12.1 纯视觉皮肤

1. 选择基础布局：例如 `qwerty`、`t9_chinese`、`shuangpin_ziran`。
2. 定义主题 token：背景、键、功能键、候选栏、工具栏。
3. 导入图片资源：背景图、贴纸、纹理。
4. 定义 decorations：锚定 keyId 或 keyboard 坐标。
5. 用主题定义器预览多种布局。
6. 校验主题包。
7. 导出 `.mybskin`。

这种方式不改 Layout，是默认推荐路径。

### 12.2 带溢出贴纸的皮肤

1. 保持 Layout 不变。
2. 通过 `DecorationAnchor.Key` 或 `DecorationAnchor.KeyGroup` 锚定贴纸。
3. 设置 `clip = Keyboard`。
4. 设置 `hitTest = Ignore`。
5. 用 `drawLayer` 控制是否覆盖文字。

Image #1 属于这种类型。

### 12.3 需要局部改变按键视觉的皮肤

使用 selector：

```jsonc
{
  "selector": { "keyId": "space" },
  "styleRef": "key_space_mail_cat"
}
```

不要复制布局文件。

### 12.4 需要改变几何的皮肤

只有当视觉要求无法通过 decorations 和 selector 实现时才修改 Layout：

1. 复制或继承基础布局。
2. 创建 `layouts/{themeId}_qwerty.jsonc` 或 `layoutPatches/qwerty_patch.jsonc`。
3. 在 `manifest.jsonc` 声明 `layoutBindings`。
4. 导入时通过布局校验。
5. 用户应用主题时显示“此主题包含专用布局”。

几何变更不能隐藏在 `theme.jsonc` 的视觉字段里。

## 13. 示例主题定义

```jsonc
{
  "schemaVersion": "1.0.0",
  "id": "mail_cat",
  "name": {
    "zh-CN": "信件小猫",
    "en-US": "Mail Cat"
  },
  "variants": {
    "light": {
      "colors": {
        "surface.keyboard": "#F4F4F4",
        "surface.chrome": "#F7F7F7",
        "text.primary": "#111111",
        "text.secondary": "#8E8E8E",
        "accent.blue": "#DFF6FF"
      },
      "keyStyles": {
        "key_default": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "pressedBackground": { "type": "solid", "color": "#ECECEC" },
          "textColor": "#111111",
          "hintColor": "#9A9A9A",
          "fontSize": 18,
          "cornerRadius": 18,
          "border": { "color": "#D6D6D6", "widthDp": 1 },
          "shadow": { "color": "#22000000", "blurDp": 4, "offsetYDp": 2 }
        },
        "key_space": {
          "background": {
            "type": "linearGradient",
            "colors": ["#F9F9F9", "#E7E7E7"],
            "angle": 90
          },
          "textColor": "#111111",
          "fontSize": 16,
          "cornerRadius": 20
        }
      },
      "chromeStyles": {
        "toolbar": {
          "background": { "type": "solid", "color": "#F7F7F7" },
          "cornerRadius": 22
        },
        "candidateBar": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "cornerRadius": 16
        }
      }
    }
  },
  "assets": {
    "mail_envelope": { "path": "assets/mail_envelope.png" },
    "bunny_sticker": { "path": "assets/bunny_sticker.png" }
  },
  "decorations": [
    {
      "id": "mail_envelope",
      "assetId": "mail_envelope",
      "anchor": {
        "type": "keyGroup",
        "keyIds": ["s", "d", "z", "x"],
        "x": 0.5,
        "y": 0.45
      },
      "size": { "widthDp": 98, "heightDp": 62 },
      "offset": { "xDp": 0, "yDp": -8 },
      "drawLayer": "AboveKeysBelowLabels",
      "zIndex": 20,
      "clip": "Keyboard",
      "hitTest": "Ignore"
    }
  ],
  "feedback": {
    "haptic": {
      "key_tap": { "durationMs": 10, "amplitude": 64, "fallbackVibration": true }
    },
    "sound": {}
  }
}
```

## 14. 导入、校验与安全

主题包导入必须校验：

- zip 不能包含 `../` 或绝对路径。
- 只允许 `.jsonc`、`.png`、`.webp`、`.jpg`、`.jpeg`、`.ttf`、`.otf` 等白名单资源。
- 单包大小限制，例如 20 MB。
- 单张图片尺寸限制，例如最大 4096x4096。
- JSON 大小限制，例如 1 MB。
- `schemaVersion` 主版本兼容。
- `theme.id`、`manifest.packageId` 一致。
- 所有 `assetId` 引用必须存在。
- 所有 `styleRef` 至少能回退到 `key_default`。
- 所有 decoration anchor 引用的 keyId 在目标布局缺失时必须可跳过，不允许崩溃。
- layoutBindings 中的布局必须通过 `LayoutRegistry.validate`。
- 字体资源只在 app 内使用，不执行任何外部代码。

失败策略：

- manifest 或 theme JSON 无效：整包拒绝导入。
- 个别 decoration 资源缺失：导入失败，因为设计不完整。
- 应用时某布局缺少 anchor key：跳过该 decoration，并在预览校验中提示。
- layoutBinding 无效：禁用该 binding，但主题视觉仍可用。
- 当前主题运行时异常：回退默认主题并记录错误。

## 15. 性能设计

- 主题 JSON 只在导入、启动或切换主题时解析。
- `ThemeRuntime` 不可变，适合 Compose 重组。
- 图片资源用 `ThemeAssetResolver` 缓存，禁止在 `DrawScope` 中解码图片。
- 大背景图导入时生成预览缩略图，运行时按实际绘制尺寸 downsample。
- decoration 按 `drawLayer` 和 `zIndex` 预排序。
- `resolveKeyStyle(styleRef)` 必须是 Map 查询，不做运行时 JSON 解析。
- 主题切换触发重组，不触发布局重测，除非用户启用了 layoutBinding。

## 16. 与 i18n 的关系

主题包中所有用户可见名称必须支持本地化：

```jsonc
{
  "displayName": {
    "zh-CN": "信件小猫",
    "en-US": "Mail Cat"
  }
}
```

设置页展示时按系统语言选择，缺失时回退 `en-US`，再回退任意可用名称。

键盘按键 label 仍优先来自 LayoutDoc 的 `@string/...`，主题不替换输入语义文案。主题可以修改字体、颜色和装饰，但不能把 `Enter` 的语义变成其它动作。

## 17. 测试计划

### 17.1 单元测试

- ThemeManifest JSONC 解析。
- ThemeDoc variants 解析。
- PaintSpec 多态解析。
- DecorationDef key/keyGroup/keyboard anchor 解析。
- ThemePackageValidator 拒绝路径逃逸。
- ThemePackageValidator 拒绝缺失 asset。
- ThemeRuntimeProvider 根据 `theme_mode` 选择 variant。
- `current_theme` 缺失时回退默认主题。
- layoutBinding 无效时禁用绑定。

### 17.2 渲染测试

- key style 正确回退到 `key_default`。
- decoration 超出 anchor key 但不超出 keyboard clip。
- decoration 不影响 hit test。
- zIndex 顺序稳定。
- Toolbar/CandidateBar 使用 chrome style。

### 17.3 集成测试

- SettingsActivity 选择主题后，IME 重建/重组使用新主题。
- 自动深浅色切换能选择正确 variant。
- 导入用户主题后可在主题列表显示。
- 删除当前主题后回退默认主题。
- 启用 optIn layoutBinding 后 `KeyboardContext.layoutId` 由状态层显式更新。

### 17.4 构建验收

实现完成后必须运行：

```bash
./gradlew test
./gradlew assembleDebug
```

本文档只新增设计说明，不改变 app 编译产物逻辑。

## 18. 分阶段落地

### Phase 1：主题选择主流程修正

- `current_theme` 接入 `MyBoardImeService`。
- `ThemeCatalog` 列出内置主题。
- 主题设置页从 `ThemeCatalog` 读取主题，不直接依赖 `BuiltInThemes.all`。
- `ThemeToggler` 只改 `theme_mode`，不直接写死 light/dark 主题对象。

### Phase 2：ThemeDoc 扩展

- 增加 variants、PaintSpec、ChromeStyle、EffectToken。
- 保持旧 light/dark JSONC 兼容。
- 引入 ThemeRuntime。

### Phase 3：渲染分层和 decorations

- `LayoutRenderer` 拆分绘制层。
- 支持 decoration anchor、clip、zIndex。
- 保持 hit test 只基于 MeasuredLayout。
- `Toolbar` / `CandidateBar` 消费 chrome style。

### Phase 4：主题包导入和定义器

- `.mybskin` 导入。
- 主题包校验。
- 设置页主题定义器。
- 预览、保存、导出。

### Phase 5：可选布局绑定

- 支持主题包附带 LayoutDoc。
- 支持 `layoutBindings optIn`。
- 支持布局 patch 后再开放高级设计能力。

## 19. 验收标准

- 设计师能用主题定义器创建纯视觉主题并预览。
- 开发者能手写 `.mybskin`，通过校验后导入。
- Image #1 这种跨按键装饰能通过 decorations 表达，不需要改 Layout。
- 主题切换只改视觉，不影响输入行为。
- 需要几何变化的主题必须显式声明 layoutBinding，并经过用户确认和 LayoutRegistry 校验。
- 所有主题设置入口都在 SettingsActivity 可达。
- 所有用户可见字符串支持 i18n。
- 不申请不必要权限。
- 主题解析失败不会导致 IME 崩溃。

## 20. 关键结论

MyBoard 的皮肤系统应采用“主题包 + token + 资源 + 装饰层 + 可选布局绑定”的设计。

首版不要动 Layout。当前 Layout 已经承担几何和动作职责，主题系统应先补齐视觉表达能力。大多数参考图，包括图标溢出到其它按键上的效果，都可以通过 decoration 层完成。

当主题确实需要改变键位几何时，不把这种能力塞进 ThemeDoc 的视觉字段，而是让主题包显式携带 LayoutDoc 或 LayoutPatch，并通过 `layoutBindings` 让用户选择启用。这样既能支持高级皮肤，也能保持输入法主流程稳定、可测试、可扩展。
