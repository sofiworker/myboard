# 高级主题定义：极简赤点（Pure Flat）

日期: 2026-07-14  
参考图: `IM/mmexportab337216fcfc110be420fe1f470dfb28_1783862774029.jpeg`  
依据:

- `docs/superpowers/specs/2026-07-13-theme-package-skin-system-design.md`
- `docs/superpowers/plans/2026-07-13-foundation-theme-runtime.md`

## 1. 主题定位

本主题是设计文档中的 **纯 token 高级皮肤（SkinLayer）**，不是 Foundation 基础色板，也不是贴纸/背景图主题。

| 维度 | 判定 |
|------|------|
| 主题类别 | 纯 token 主题 |
| 分层角色 | `SkinLayer`，叠加在 `FoundationTheme` 之上 |
| `colorPolicy` | `locked`（设计师锁定完整配色，不继承用户基础色板） |
| 是否使用图片 | 否 |
| 是否使用 decorations | 否 |
| 是否使用 layoutBindings | 否 |
| 是否改 Layout / 命中 | 否 |
| 覆盖 surface | primary 键盘、候选栏、工具栏、符号页、Emoji 页 |

视觉一句话：

> 纯白底板 + 浅灰圆角扁平按键 + 近黑高对比文字 + 高饱和红色动作键，零阴影、零贴纸、零纹理。

它对应参考图上下两套布局（T9 / QWERTY）的同一套视觉语言，仅布局几何不同，主题 token 完全一致。

## 2. 参考图拆解

### 2.1 视觉要素

| 元素 | 观察结果 | 主题映射 |
|------|----------|----------|
| 键盘底板 | 纯白 / 近白，无纹理 | `surface.keyboard` solid |
| 字母键 | 浅灰填充，与底板有轻微层次 | `key_default.background` |
| 功能键（123 / 中英 / 删除等） | 与字母键同色系，几乎无额外强调 | `key_function` 与 default 接近 |
| 回车 / 发送 | 高饱和红色，白字图标 | `key_action` |
| 文字 | 近黑、中等字重 | `textColor` / `hintColor` |
| 圆角 | 大圆角，接近 pill，但未完全胶囊 | `cornerRadius` 约 16–18 |
| 阴影 / 描边 / 高光 | 无 | 不声明 shadow / border / highlight |
| 工具栏图标 | 细线黑描边 | chrome toolbar icon tint |
| 候选 / 输入区 | 白底、无边框装饰 | chrome candidate / surface |
| 装饰贴纸 | 无 | `decorations: []` |
| 背景摄影 / 纹理 | 无 | `assets: {}` |

### 2.2 与设计文档分类对照

设计文档 §1 列出的皮肤类型：

1. **纯 token 主题** ← 本主题
2. 质感主题（高光/玻璃/金属）← 否
3. 背景图主题 ← 否
4. 贴纸主题 ← 否
5. 溢出装饰主题 ← 否
6. 结构型主题 ← 否（T9/QWERTY 几何差来自 Layout，不是主题）

因此落地路径走 **§12.1 纯视觉皮肤**，且资源列表为空，不需要 Decoration 与 Layout 绑定。

### 2.3 与 Foundation 的边界

| 能力 | Foundation（Phase 1） | 本高级主题 |
|------|----------------------|------------|
| 预置色板 / 动态色 / 自定义主色 | 是 | 否（locked 覆盖） |
| 按键填充/描边/无边框外观 | 是 | 本主题强制 filled + 无描边 |
| 圆角档位 | compact/rounded/pill | 本主题固定大圆角 token |
| 图片 / 贴纸 / 字体包 | 否 | 否 |
| 动作键强调色 | 由 seed 生成（如蓝） | 锁定为红色 |

启用本皮肤时：

```text
ThemeRuntime = FoundationTheme（仅提供 variant / mode 骨架）
             + SkinLayer(pure_flat, colorPolicy=locked)
```

解析失败时完整回退 Foundation，符合设计文档 §4.1。

## 3. 包元数据

### 3.1 标识

| 字段 | 值 |
|------|-----|
| `packageId` / `theme.id` | `pure_flat` |
| 中文名 | 极简赤点 |
| 英文名 | Pure Flat |
| 作者 | MyBoard |
| schema | `1.0.0` |
| capabilities | `usesImages=false`, `usesDecorations=false`, `usesLayoutBindings=false` |

### 3.2 包目录

```text
themes/pure_flat/
  manifest.jsonc
  theme.jsonc
  previews/
    preview_light.webp   # 后续由预览渲染器生成
    preview_dark.webp
```

无 `assets/`、`layouts/`、`layoutPatches/`。

导出包名：`pure_flat.mybskin`。

## 4. manifest.jsonc

```jsonc
{
  "schemaVersion": "1.0.0",
  "packageId": "pure_flat",
  "displayName": {
    "zh-CN": "极简赤点",
    "en-US": "Pure Flat"
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
    "usesImages": false,
    "usesDecorations": false,
    "usesLayoutBindings": false
  },
  "layoutBindings": []
}
```

## 5. theme.jsonc

说明：

- 使用设计文档目标 schema（`variants` / `PaintSpec` / `chromeStyles`），不是当前 Phase 1 的旧 `ThemeDoc` 扁平字段。
- token / style id 与 `KeyStyleRole`、`FeedbackTokenId` 对齐，禁止业务侧再写裸字符串。
- light 严格对齐参考图；dark 保持同一设计语言（扁平 + 赤色动作键），仅反转明度。

```jsonc
{
  "schemaVersion": "1.0.0",
  "id": "pure_flat",
  "name": {
    "zh-CN": "极简赤点",
    "en-US": "Pure Flat"
  },
  "colorPolicy": "locked",
  "variants": {
    "light": {
      "colors": {
        "surface.keyboard": "#FFFFFF",
        "surface.chrome": "#FFFFFF",
        "surface.panel": "#FFFFFF",
        "text.primary": "#1A1A1A",
        "text.secondary": "#8A8A8A",
        "text.onAction": "#FFFFFF",
        "key.default": "#F0F0F0",
        "key.defaultPressed": "#E2E2E2",
        "key.function": "#F0F0F0",
        "key.functionPressed": "#E2E2E2",
        "key.action": "#FF3B30",
        "key.actionPressed": "#E0352B",
        "candidate.background": "#FFFFFF",
        "candidate.text": "#1A1A1A",
        "candidate.selected": "#FF3B30",
        "toolbar.icon": "#1A1A1A",
        "toolbar.background": "#FFFFFF",
        "panel.emoji": "#FFFFFF",
        "panel.symbol": "#FFFFFF"
      },
      "keyStyles": {
        "key_default": {
          "background": { "type": "solid", "color": "#F0F0F0" },
          "pressedBackground": { "type": "solid", "color": "#E2E2E2" },
          "textColor": "#1A1A1A",
          "pressedTextColor": "#1A1A1A",
          "hintColor": "#8A8A8A",
          "iconTint": "#1A1A1A",
          "fontSize": 18,
          "fontWeight": 500,
          "cornerRadius": 16
        },
        "key_function": {
          "background": { "type": "solid", "color": "#F0F0F0" },
          "pressedBackground": { "type": "solid", "color": "#E2E2E2" },
          "textColor": "#1A1A1A",
          "pressedTextColor": "#1A1A1A",
          "hintColor": "#8A8A8A",
          "iconTint": "#1A1A1A",
          "fontSize": 14,
          "fontWeight": 500,
          "cornerRadius": 16
        },
        "key_action": {
          "background": { "type": "solid", "color": "#FF3B30" },
          "pressedBackground": { "type": "solid", "color": "#E0352B" },
          "textColor": "#FFFFFF",
          "pressedTextColor": "#FFFFFF",
          "iconTint": "#FFFFFF",
          "fontSize": 16,
          "fontWeight": 600,
          "cornerRadius": 16
        },
        "key_space": {
          "background": { "type": "solid", "color": "#F0F0F0" },
          "pressedBackground": { "type": "solid", "color": "#E2E2E2" },
          "textColor": "#1A1A1A",
          "pressedTextColor": "#1A1A1A",
          "hintColor": "#8A8A8A",
          "iconTint": "#1A1A1A",
          "fontSize": 14,
          "fontWeight": 500,
          "cornerRadius": 18
        },
        "key_candidate": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "pressedBackground": { "type": "solid", "color": "#F0F0F0" },
          "textColor": "#1A1A1A",
          "pressedTextColor": "#FF3B30",
          "fontSize": 16,
          "fontWeight": 500,
          "cornerRadius": 8
        }
      },
      "chromeStyles": {
        "toolbar": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "iconTint": "#1A1A1A",
          "cornerRadius": 0
        },
        "candidateBar": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "textColor": "#1A1A1A",
          "highlightColor": "#FF3B30",
          "cornerRadius": 0
        },
        "panel": {
          "background": { "type": "solid", "color": "#FFFFFF" },
          "cornerRadius": 0
        }
      }
    },
    "dark": {
      "colors": {
        "surface.keyboard": "#121212",
        "surface.chrome": "#121212",
        "surface.panel": "#121212",
        "text.primary": "#F5F5F5",
        "text.secondary": "#9A9A9A",
        "text.onAction": "#FFFFFF",
        "key.default": "#2A2A2A",
        "key.defaultPressed": "#3A3A3A",
        "key.function": "#2A2A2A",
        "key.functionPressed": "#3A3A3A",
        "key.action": "#FF3B30",
        "key.actionPressed": "#E0352B",
        "candidate.background": "#121212",
        "candidate.text": "#F5F5F5",
        "candidate.selected": "#FF3B30",
        "toolbar.icon": "#F5F5F5",
        "toolbar.background": "#121212",
        "panel.emoji": "#121212",
        "panel.symbol": "#121212"
      },
      "keyStyles": {
        "key_default": {
          "background": { "type": "solid", "color": "#2A2A2A" },
          "pressedBackground": { "type": "solid", "color": "#3A3A3A" },
          "textColor": "#F5F5F5",
          "pressedTextColor": "#F5F5F5",
          "hintColor": "#9A9A9A",
          "iconTint": "#F5F5F5",
          "fontSize": 18,
          "fontWeight": 500,
          "cornerRadius": 16
        },
        "key_function": {
          "background": { "type": "solid", "color": "#2A2A2A" },
          "pressedBackground": { "type": "solid", "color": "#3A3A3A" },
          "textColor": "#F5F5F5",
          "pressedTextColor": "#F5F5F5",
          "hintColor": "#9A9A9A",
          "iconTint": "#F5F5F5",
          "fontSize": 14,
          "fontWeight": 500,
          "cornerRadius": 16
        },
        "key_action": {
          "background": { "type": "solid", "color": "#FF3B30" },
          "pressedBackground": { "type": "solid", "color": "#E0352B" },
          "textColor": "#FFFFFF",
          "pressedTextColor": "#FFFFFF",
          "iconTint": "#FFFFFF",
          "fontSize": 16,
          "fontWeight": 600,
          "cornerRadius": 16
        },
        "key_space": {
          "background": { "type": "solid", "color": "#2A2A2A" },
          "pressedBackground": { "type": "solid", "color": "#3A3A3A" },
          "textColor": "#F5F5F5",
          "pressedTextColor": "#F5F5F5",
          "hintColor": "#9A9A9A",
          "iconTint": "#F5F5F5",
          "fontSize": 14,
          "fontWeight": 500,
          "cornerRadius": 18
        },
        "key_candidate": {
          "background": { "type": "solid", "color": "#121212" },
          "pressedBackground": { "type": "solid", "color": "#2A2A2A" },
          "textColor": "#F5F5F5",
          "pressedTextColor": "#FF3B30",
          "fontSize": 16,
          "fontWeight": 500,
          "cornerRadius": 8
        }
      },
      "chromeStyles": {
        "toolbar": {
          "background": { "type": "solid", "color": "#121212" },
          "iconTint": "#F5F5F5",
          "cornerRadius": 0
        },
        "candidateBar": {
          "background": { "type": "solid", "color": "#121212" },
          "textColor": "#F5F5F5",
          "highlightColor": "#FF3B30",
          "cornerRadius": 0
        },
        "panel": {
          "background": { "type": "solid", "color": "#121212" },
          "cornerRadius": 0
        }
      }
    }
  },
  "assets": {},
  "typography": {},
  "effects": {},
  "decorations": [],
  "styleOverrides": [],
  "layoutBindings": [],
  "feedback": {
    "haptic": {
      "key_tap": { "durationMs": 10, "amplitude": 48, "fallbackVibration": true },
      "key_long_press": { "durationMs": 28, "amplitude": 96, "fallbackVibration": true },
      "key_action": { "durationMs": 14, "amplitude": 80, "fallbackVibration": true }
    },
    "sound": {
      "key_tap": { "soundResName": "key_tap", "volume": 0.25 },
      "key_action": { "soundResName": "key_action", "volume": 0.4 },
      "key_space": { "soundResName": "key_space", "volume": 0.18 }
    }
  }
}
```

## 6. 设计规则（作者约束）

实现或扩展同类主题时遵守：

1. **扁平优先**  
   禁止 shadow、innerShadow、highlight、texture、gradient（除非另开质感主题系列）。

2. **功能键不抢戏**  
   `key_function` 与 `key_default` 同色或极近；唯一强调色留给 `key_action`。

3. **动作键固定赤色**  
   light/dark 都使用同一红色家族（`#FF3B30` / pressed `#E0352B`），dark 不改成霓虹粉。

4. **大圆角一致**  
   默认键 16、空格 18；不得在 selector 里把个别字母键改成直角。

5. **无资源依赖**  
   `assets` 与 `decorations` 必须为空；校验器应允许零资源纯 token 包。

6. **不碰 Layout**  
   参考图里的 T9 / QWERTY 差异完全由现有 `LayoutDoc` 表达；主题只解析 `styleRef`。

7. **colorPolicy = locked**  
   用户切换基础色板时，本皮肤配色不变；取消皮肤后才回到 Foundation 生成色。

8. **i18n**  
   `displayName` / `name` 至少含 `zh-CN` 与 `en-US`。

## 7. 运行时解析预期

```text
AppearanceSettings.skinThemeId = "pure_flat"
  -> ThemeCatalog.load("pure_flat")
  -> ThemeRuntimeProvider:
       foundation tokens 被 skin locked colors 覆盖
       keyStyles / chromeStyles 来自 pure_flat.variants[light|dark]
       decorations = empty
       assetResolver = no-op
  -> LayoutRenderer / Toolbar / CandidateBar / Panel 只消费 ThemeRuntime
```

`theme_mode` / `appearanceMode` 仍只选择 light/dark variant，不改变布局。

## 8. 与 Phase 计划的关系

| 阶段 | 本主题需要的能力 | 状态 |
|------|------------------|------|
| Phase 1 Foundation | AppearanceSettings、variant、Chrome 消费 | 进行中 / 已规划 |
| Phase 2 SkinLayer | `colorPolicy`、variants PaintSpec、合成 Runtime | 本定义依赖 |
| Phase 3 decorations | 不需要 | N/A |
| Phase 4 包导入 | `.mybskin` 导入校验 | 需要，但本包零资源，校验最简 |
| Phase 5 layoutBindings | 不需要 | N/A |

本文件是 **Phase 2 的首个参考内置高级皮肤定义**，用于：

- 验证 locked 纯 token 皮肤路径；
- 作为主题定义器「高级模式」的最小可导出样例；
- 对照参考图做视觉回归。

当前 Phase 1 **不实现** 本包加载；在 SkinLayer 落地前，可将 light 色值临时对照 Foundation 预览，但不得把 `pure_flat` 硬编码进 `BuiltInThemes` 作为第二套状态源。

## 9. 验收标准

- [ ] 以 `pure_flat` 为 `skinThemeId` 时，主键盘视觉接近参考图：白底、浅灰键、大红回车、无阴影。
- [ ] T9 与 QWERTY 使用同一 ThemeRuntime，仅 Layout 不同。
- [ ] 候选栏、工具栏、Emoji/符号页使用同一 locked 配色。
- [ ] 切换系统深浅色只切换 variant，不改键位几何。
- [ ] 卸载/解析失败回退 Foundation，IME 不崩溃。
- [ ] 包内无图片资源时校验通过。
- [ ] 设置入口仅在 `SettingsActivity` 主题页；`SettingsRepository.appearanceSettings` 为唯一来源。
- [ ] 显示名称支持中英文。

## 10. 后续可选变体

同系列可再定义，但应是独立 `packageId`，不要改写 `pure_flat`：

| packageId | 差异 |
|-----------|------|
| `pure_flat_blue` | 动作键改为系统蓝，其余不变 |
| `pure_flat_mono` | 动作键也不强调，全灰阶 |
| `pure_flat_outlined` | 白键 + 细描边（进入质感/描边子类） |

首版只交付 `pure_flat` 一个参考实现即可。
