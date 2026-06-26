# 键盘布局 JSONC 规范化修复设计

## 概述

修复所有布局 JSONC 文件，使其符合 `docs/layout.md` 规范。包括：actionType 命名统一为 UPPER_SNAKE_CASE、Panel 名称统一、Space 动作类型修复、GridCell 字段类型从 Int 改为 Float（支持半列偏移）。

## 规范依据

`docs/layout.md` §9.3 `ActionDispatcher` 定义的合法 actionType 为 UPPER_SNAKE_CASE：
`PUSH_TOKEN`, `DELETE`, `SPACE`, `ENTER`, `SWITCH_LOCALE`, `SWITCH_SCRIPT`, `SWITCH_SCHEMA`, `SWITCH_LAYER`, `COMMIT_CANDIDATE`, `OPEN_PANEL`, `CLOSE_PANEL`, `RESTORE_PREVIOUS_SCHEMA`, `PAGE_NEXT`, `PAGE_PREV`

## 问题清单

### 问题 1：actionType 命名不规范

所有布局使用小写 actionType，规范要求 UPPER_SNAKE_CASE：

| 当前值 | 规范值 | 影响文件 |
|--------|--------|----------|
| `commitToken` | `PUSH_TOKEN` | 全部 8 个布局 |
| `delete` | `DELETE` | qwerty, shuangpin_ziran, t9_chinese, qwerty_colemak, qwerty_dvorak, qwerty_abc, hiragana |
| `cycleLayer` | `SWITCH_LAYER` | qwerty, shuangpin_ziran, qwerty_colemak, qwerty_dvorak, qwerty_abc |
| `switchLocale` | `SWITCH_LOCALE` | qwerty |
| `openPanel` | `OPEN_PANEL` | qwerty, shuangpin_ziran, t9_chinese, qwerty_colemak |
| `performEditorAction` | `ENTER` | qwerty, shuangpin_ziran, t9_chinese, qwerty_colemak, qwerty_dvorak, qwerty_abc, hiragana |

### 问题 2：Space 动作类型不正确

所有布局的 space 键使用 `commitToken` + `{ "token": " " }`，应改为专用的 `SPACE` 动作：

| 文件 | 行号 | 当前 | 应改为 |
|------|------|------|--------|
| `qwerty.jsonc` | 71 | `commitToken` + token | `SPACE`（无 payload） |
| `shuangpin_ziran.jsonc` | 70 | `commitToken` + token | `SPACE`（无 payload） |
| `t9_chinese.jsonc` | 43 | `commitToken` + token | `SPACE`（无 payload） |
| `qwerty_colemak.jsonc` | 71 | `commitToken` + token | `SPACE`（无 payload） |
| `qwerty_dvorak.jsonc` | 72 | `commitToken` + token | `SPACE`（无 payload） |
| `qwerty_abc.jsonc` | 71 | `commitToken` + token | `SPACE`（无 payload） |

### 问题 3：Panel 名称不匹配

`qwerty.jsonc` 和 `shuangpin_ziran.jsonc` 使用 `"SYMBOL_PANEL"`，`PanelType.kt` 枚举值为 `"SYMBOL"`：

| 文件 | 当前值 | 应改为 |
|------|--------|--------|
| `shuangpin_ziran.jsonc:72` | `"SYMBOL_PANEL"` | `"SYMBOL"` |
| `qwerty.jsonc:73` | `"SYMBOL_PANEL"` | `"SYMBOL"` |

### 问题 4：GridCell 字段类型为 Int，不支持半列偏移

`layout.md` §3.4 定义 `GridLayout.GridCell` 的 `col`/`row`/`colSpan`/`rowSpan` 为 `Int`，但当前布局大量使用 Float 值实现半列偏移（如 qwerty.jsonc 的 shift 键 `col: 0, colSpan: 1.5`，下一个键 `col: 1.5`）。需要修改规范为 `Float` 以支持这种布局模式。

## 修复方案

### Fix 1: actionType 命名统一

所有布局中将小写 actionType 改为 UPPER_SNAKE_CASE：
- `commitToken` → `PUSH_TOKEN`
- `delete` → `DELETE`
- `cycleLayer` → `SWITCH_LAYER`
- `switchLocale` → `SWITCH_LOCALE`
- `openPanel` → `OPEN_PANEL`
- `performEditorAction` → `ENTER`

### Fix 2: Space 动作修复

将所有 space 键的 actions 从：
```json
"actions": { "gestures": { "TAP": { "actionType": "commitToken", "payload": { "token": " " } } } }
```
改为：
```json
"actions": { "gestures": { "TAP": { "actionType": "SPACE" } } }
```

### Fix 3: Panel 名称统一

所有布局中 `"panel": "SYMBOL_PANEL"` → `"panel": "SYMBOL"`

### Fix 4: 规范修改 — GridCell 字段类型

修改 `docs/layout.md` §3.4 中 `GridLayout.GridCell` 的定义：

```kotlin
@Serializable
data class GridCell(
    val key: KeyDef,
    val col: Float, val row: Float,       // 支持半列偏移（如 1.5）
    val colSpan: Float = 1f, val rowSpan: Float = 1f
)
```

同时更新 §4.2 `measureGrid` 中的计算逻辑，将 `colWidth` 乘法从整数运算改为浮点运算。

## 涉及文件

1. `app/src/main/assets/layouts/qwerty.jsonc` — Fix 1, Fix 2, Fix 3
2. `app/src/main/assets/layouts/shuangpin_ziran.jsonc` — Fix 1, Fix 2, Fix 3
3. `app/src/main/assets/layouts/t9_chinese.jsonc` — Fix 1, Fix 2
4. `app/src/main/assets/layouts/qwerty_colemak.jsonc` — Fix 1, Fix 2
5. `app/src/main/assets/layouts/qwerty_dvorak.jsonc` — Fix 1, Fix 2
6. `app/src/main/assets/layouts/qwerty_abc.jsonc` — Fix 1, Fix 2
7. `app/src/main/assets/layouts/hiragana.jsonc` — Fix 1
8. `app/src/main/assets/layouts/phone_dial.jsonc` — Fix 1
