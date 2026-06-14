# P2: 布局系统 (2 周)

## 1. 目标

实现 JSON 驱动的布局系统，支持声明式布局定义、模板继承、跨行跨列。

## 2. 里程碑验收标准

- [x] JSON 布局文件可解析
- [x] `qwerty.json` 可正常渲染
- [x] 支持模板继承（`extends`）
- [x] 支持 arrangements 切换
- [x] 支持 `colSpan` / `rowSpan`
- [x] 支持 popups 弹出
- [x] 支持用户 Patch

## 3. 详细设计

### 3.1 数据模型

#### 3.1.1 KeyboardLayout

```kotlin
@Serializable
data class KeyboardLayout(
    val schemaVersion: Int = 1,
    val id: String,
    val meta: LayoutMeta? = null,
    val script: ScriptConfig? = null,
    val inputMethod: InputMethodConfig? = null,
    val grid: GridConfig = GridConfig(columns = 10),
    val templates: Map<String, KeyTemplate> = emptyMap(),
    val keys: Map<String, KeyData>,
    val rows: List<RowData>,
    val arrangements: Map<String, ArrangementData>,
    val geometry: GeometryConfig = GeometryConfig(),
    val popups: Map<String, PopupData> = emptyMap(),
    val dictionary: DictionaryConfig? = null,
    val shift: ShiftConfig? = null,
    val enter: EnterConfig? = null,
    val space: SpaceConfig? = null,
    val backspace: BackspaceConfig? = null
)

@Serializable
data class GridConfig(
    val columns: Int = 10,
    val rowHeight: String = "1fr"
)

@Serializable
data class LayoutMeta(
    val name: String? = null,
    val locale: String? = null,
    val tags: List<String> = emptyList(),
    val description: String? = null,
    val author: String? = null
)
```

#### 3.1.2 KeyData

```kotlin
@Serializable
data class KeyData(
    val t: String,                           // 模板引用
    val code: Int = 0,
    val label: String? = null,
    val icon: String? = null,
    val output: String? = null,
    val action: String? = null,
    val actionParams: Map<String, String> = emptyMap(),
    val popup: List<String> = emptyList(),
    val popupRef: String? = null,
    val repeatable: Boolean = false,
    val colSpan: Int? = null,               // 跨列数
    val rowSpan: Int? = null,               // 跨行数
    val dynamicLabel: Map<String, String>? = null,
    val themeOverride: Map<String, Any>? = null
)
```

#### 3.1.3 RowData

```kotlin
@Serializable
data class RowData(
    val id: String,
    val keys: List<String>,                  // 按键 ID 列表
    val pad: Float = 0f,                     // 行内 padding
    val heightRatio: Float? = null           // 行高比例
)
```

#### 3.1.4 ArrangementData

```kotlin
@Serializable
data class ArrangementData(
    val rows: List<String>,                  // 行 ID 列表
    val type: String = "weightedRows"
)
```

#### 3.1.5 KeyTemplate

```kotlin
@Serializable
data class KeyTemplate(
    val role: String? = null,
    val extends: String? = null,             // 继承自哪个模板
    val gestures: Map<String, List<ActionDef>>? = null,
    val repeat: RepeatConfig? = null
)

@Serializable
data class RepeatConfig(
    val delay: Int = 400,
    val interval: Int = 50,
    val accelerate: Boolean = false
)
```

#### 3.1.6 GeometryConfig

```kotlin
@Serializable
data class GeometryConfig(
    val height: HeightConfig = HeightConfig(),
    val gap: GapConfig = GapConfig(),
    val padding: PaddingConfig = PaddingConfig()
)

@Serializable
data class HeightConfig(
    val dp: Int = 260,
    val min: Int = 190,
    val max: Int = 360
)

@Serializable
data class GapConfig(
    val h: Float = 4f,
    val v: Float = 5f
)

@Serializable
data class PaddingConfig(
    val all: Float = 6f
)
```

### 3.2 布局解析器

#### 3.2.1 LayoutParser

```kotlin
object LayoutParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        coerceInputValues = true
    }

    fun parse(text: String): KeyboardLayout {
        val normalized = stripJsonLineComments(text)
        return json.decodeFromString(KeyboardLayout.serializer(), normalized)
    }

    fun parseFromAssets(context: Context, path: String): KeyboardLayout {
        val text = context.assets.open(path).bufferedReader().readText()
        return parse(text)
    }

    private fun stripJsonLineComments(text: String): String {
        val out = StringBuilder(text.length)
        var inString = false
        var escaped = false

        var i = 0
        while (i < text.length) {
            val c = text[i]

            if (!inString && c == '/' && i + 1 < text.length && text[i + 1] == '/') {
                while (i < text.length && text[i] != '\n') i++
                continue
            }

            out.append(c)

            if (inString) {
                if (escaped) {
                    escaped = false
                } else if (c == '\\') {
                    escaped = true
                } else if (c == '"') {
                    inString = false
                }
            } else if (c == '"') {
                inString = true
            }

            i++
        }

        return out.toString()
    }
}
```

### 3.3 几何计算器

#### 3.3.1 GridCalculator

```kotlin
class GridCalculator {
    fun calculate(
        layout: KeyboardLayout,
        containerWidth: Float,
        containerHeight: Float
    ): Map<String, KeyGeometry> {
        val arrangement = layout.arrangements[layout.arrangements.keys.firstOrNull()] ?: return emptyMap()
        val rows = arrangement.rows.mapNotNull { rowId ->
            layout.rows.find { it.id == rowId }
        }

        val spanCount = layout.grid.columns
        val hGap = layout.geometry.gap.h
        val vGap = layout.geometry.gap.v
        val padding = layout.geometry.padding.all

        val availableWidth = containerWidth - padding * 2
        val availableHeight = containerHeight - padding * 2
        val cellWidth = (availableWidth - hGap * (spanCount - 1)) / spanCount
        val rowHeight = availableHeight / rows.size

        val result = mutableMapOf<String, KeyGeometry>()
        val occupied = Array(rows.size) { BooleanArray(spanCount) }

        for ((rowIndex, row) in rows.withIndex()) {
            var col = 0
            for (keyId in row.keys) {
                val key = layout.keys[keyId] ?: continue
                val cs = key.colSpan ?: 1
                val rs = key.rowSpan ?: 1

                // 找到下一个未占用的列
                while (col < spanCount && occupied[rowIndex][col]) col++
                if (col >= spanCount) break

                val leftPx = padding + col * (cellWidth + hGap)
                val topPx = padding + rowIndex * (rowHeight + vGap)
                val widthPx = cs * cellWidth + (cs - 1) * hGap
                val heightPx = rs * rowHeight + (rs - 1) * vGap

                result[keyId] = KeyGeometry(
                    keyId = keyId,
                    col = col, row = rowIndex,
                    colSpan = cs, rowSpan = rs,
                    widthPx = widthPx, heightPx = heightPx,
                    leftPx = leftPx, topPx = topPx
                )

                for (r in 0 until rs) {
                    for (c in 0 until cs) {
                        if (rowIndex + r < rows.size && col + c < spanCount) {
                            occupied[rowIndex + r][col + c] = true
                        }
                    }
                }

                col += cs
            }
        }

        return result
    }
}

data class KeyGeometry(
    val keyId: String,
    val col: Int,
    val row: Int,
    val colSpan: Int,
    val rowSpan: Int,
    val widthPx: Float,
    val heightPx: Float,
    val leftPx: Float,
    val topPx: Float
)
```

### 3.4 Patch 系统

#### 3.4.1 LayoutPatch

```kotlin
@Serializable
data class LayoutPatch(
    val target: String,
    val ops: List<PatchOp>
)

sealed interface PatchOp {
    @Serializable data class ReplaceKey(val key: String, val data: KeyData) : PatchOp
    @Serializable data class InsertKeyAfter(val after: String, val data: KeyData) : PatchOp
    @Serializable data class InsertKeyBefore(val before: String, val data: KeyData) : PatchOp
    @Serializable data class RemoveKey(val key: String) : PatchOp
    @Serializable data class ReplaceRow(val row: String, val keys: List<String>) : PatchOp
    @Serializable data class ReplaceGesture(val key: String, val gesture: String, val action: ActionDef) : PatchOp
}
```

#### 3.4.2 PatchApplier

```kotlin
object PatchApplier {
    fun apply(base: KeyboardLayout, patch: LayoutPatch): KeyboardLayout {
        var result = base.copy(
            keys = base.keys.toMutableMap(),
            rows = base.rows.toMutableList()
        )

        for (op in patch.ops) {
            result = when (op) {
                is PatchOp.ReplaceKey -> result.copy(
                    keys = result.keys.toMutableMap().apply { put(op.key, op.data) }
                )
                is PatchOp.InsertKeyAfter -> {
                    val newKeys = result.keys.toMutableMap().apply { put(op.data.keyId, op.data) }
                    val newRows = result.rows.map { row ->
                        val idx = row.keys.indexOf(op.after)
                        if (idx >= 0) {
                            val newKeysList = row.keys.toMutableList().apply { add(idx + 1, op.data.keyId) }
                            row.copy(keys = newKeysList)
                        } else row
                    }
                    result.copy(keys = newKeys, rows = newRows)
                }
                is PatchOp.RemoveKey -> {
                    val newRows = result.rows.map { row ->
                        row.copy(keys = row.keys.filter { it != op.key })
                    }
                    result.copy(rows = newRows)
                }
                is PatchOp.ReplaceRow -> {
                    val newRows = result.rows.map { row ->
                        if (row.id == op.row) row.copy(keys = op.keys) else row
                    }
                    result.copy(rows = newRows)
                }
                else -> result
            }
        }

        return result
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `core/layout/LayoutModels.kt` | 数据模型 |
| `core/layout/LayoutParser.kt` | JSON 解析 |
| `core/layout/GridCalculator.kt` | 几何计算 |
| `core/layout/PatchApplier.kt` | Patch 应用 |
| `core/layout/LayoutRepository.kt` | 布局仓库 |
| `assets/layouts/qwerty.json` | 英文布局 |
| `assets/layouts/zh_pinyin.json` | 中文拼音布局 |
| `assets/layouts/ja_romaji.json` | 日文罗马字布局 |
| `assets/layouts/ko_hangul.json` | 韩文谚文布局 |
