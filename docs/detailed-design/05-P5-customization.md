# P5: 高度自定义 (2 周)

## 1. 目标

实现 Code 键盘系统、按键映射、候选栏自定义、工具栏自定义、可视化编辑器。

## 2. 里程碑验收标准

- [x] Code 键盘可正常工作
- [x] 用户映射可生效
- [x] 候选栏可自定义布局
- [x] 工具栏可自定义
- [x] 可视化编辑器可用

## 3. 详细设计

### 3.1 Code 键盘系统

#### 3.1.1 KeySlot 模型

```kotlin
data class KeySlot(
    val slotId: String,
    val keyCode: Int,
    val row: Int,
    val col: Int,
    val width: Float = 1f
)

data class KeyBinding(
    val slotId: String,
    val layer: Layer,
    val output: KeyOutput,
    val gesture: GestureMap = GestureMap()
)

sealed interface KeyOutput {
    data class Character(val char: String, val label: String? = null) : KeyOutput
    data class Text(val text: String) : KeyOutput
    data class Code(val keyCode: Int) : KeyOutput
    data class Action(val action: String, val params: Map<String, String> = emptyMap()) : KeyOutput
    data object None : KeyOutput
}

enum class Layer { BASE, SHIFT, SYMBOL, SYMBOL_SHIFT, CUSTOM_1, CUSTOM_2, CUSTOM_3 }
```

#### 3.1.2 KeyBindingManager

```kotlin
class KeyBindingManager @Inject constructor(
    private val userPrefs: UserPreferences
) {
    private var bindings: Map<String, Map<Layer, KeyBinding>> = emptyMap()

    fun loadBindings(layout: KeyboardLayout) {
        bindings = layout.bindings.groupBy { it.slotId }
            .mapValues { (_, v) -> v.associateBy { it.layer } }
    }

    fun resolve(slotId: String, layer: Layer): KeyBinding? {
        return bindings[slotId]?.get(layer)
    }

    fun applyRemap(remap: RemapConfig) {
        for (r in remap.remaps) {
            val existing = bindings[r.slotId]?.get(r.layer)
            if (existing != null && r.to != null) {
                bindings[r.slotId]?.put(r.layer, existing.copy(output = r.to))
            }
        }
    }
}
```

### 3.2 按键映射

```kotlin
@Serializable
data class RemapConfig(
    val target: String,
    val remaps: List<RemapEntry>
)

@Serializable
data class RemapEntry(
    val slot: String,
    val layer: Layer = Layer.BASE,
    val gesture: String? = null,
    val from: String? = null,
    val to: KeyOutput? = null
)
```

### 3.3 候选栏自定义

```kotlin
@Serializable
data class CandidateBarConfig(
    val layout: String = "scroll",          // scroll | pager | grid
    val itemWidth: String = "wrap",
    val itemHeight: Int = 40,
    val maxVisible: Int = 5,
    val showSource: Boolean = true,
    val sections: List<CandidateSection> = emptyList()
)

@Serializable
data class CandidateSection(
    val id: String,
    val title: String,
    val source: String,                      // dictionary | llm | history
    val maxItems: Int = 20
)
```

### 3.4 工具栏自定义

```kotlin
@Serializable
data class ToolbarConfig(
    val items: List<ToolbarItem>,
    val maxVisible: Int = 6,
    val showLabels: Boolean = false
)

@Serializable
data class ToolbarItem(
    val id: String,
    val name: String,
    val icon: String,
    val action: String,
    val enabled: Boolean = true,
    val priority: Int = 100
)
```

### 3.5 可视化编辑器

```kotlin
@Composable
fun LayoutEditorScreen(
    layout: KeyboardLayout,
    onSave: (KeyboardLayout) -> Unit,
    onCancel: () -> Unit
) {
    var editingLayout by remember { mutableStateOf(layout) }
    var selectedKey by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        // 预览区
        KeyboardPreview(
            layout = editingLayout,
            onKeyClick = { selectedKey = it }
        )

        // 编辑区
        selectedKey?.let { keyId ->
            KeyEditor(
                key = editingLayout.keys[keyId]!!,
                onUpdate = { newKey ->
                    editingLayout = editingLayout.copy(
                        keys = editingLayout.keys.toMutableMap().apply { put(keyId, newKey) }
                    )
                }
            )
        }

        // 工具栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            TextButton(onClick = { /* 恢复默认 */ }) { Text("恢复默认") }
            TextButton(onClick = { onSave(editingLayout) }) { Text("保存") }
        }
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `core/keybinding/KeySlot.kt` | 按键槽位模型 |
| `core/keybinding/KeyBindingManager.kt` | 绑定管理 |
| `core/keybinding/RemapConfig.kt` | 映射配置 |
| `ui/candidate/CandidateBarConfig.kt` | 候选栏配置 |
| `ui/toolbar/ToolbarConfig.kt` | 工具栏配置 |
| `ui/settings/LayoutEditorScreen.kt` | 布局编辑器 |
| `ui/settings/ThemeEditorScreen.kt` | 主题编辑器 |
