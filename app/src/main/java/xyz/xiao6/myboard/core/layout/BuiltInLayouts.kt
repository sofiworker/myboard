package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 内置布局常量。
 * 提供默认的 QWERTY 布局定义。
 */
object BuiltInLayouts {
    
    private fun letterKey(id: String, label: String, hint: String? = null): KeyDef {
        return KeyDef(
            id = id,
            content = ContentSpec(
                label = label,
                hint = if (hint != null) mapOf(HintPosition.TOP_CENTER to hint) else emptyMap()
            ),
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(
                        actionType = "commitToken",
                        payload = mapOf(
                            "token" to kotlinx.serialization.json.JsonPrimitive(id)
                        )
                    )
                )
            )
        )
    }
    
    private fun actionKey(id: String, label: String, actionType: String, payload: Map<String, kotlinx.serialization.json.JsonElement> = emptyMap()): KeyDef {
        return KeyDef(
            id = id,
            styleRef = "key_function",
            content = ContentSpec(label = label),
            actions = ActionMap(
                gestures = mapOf(
                    GestureType.TAP to ActionDef(actionType = actionType, payload = payload)
                )
            )
        )
    }
    
    val qwerty: LayoutDoc = LayoutDoc(
        schemaVersion = 2,
        id = "qwerty",
        meta = LayoutMeta(name = "QWERTY", description = "Standard QWERTY keyboard"),
        root = CompositeLayout(
            id = "root",
            orientation = Orientation.VERTICAL,
            regions = listOf(
                Region(
                    id = "candidate_region",
                    role = RegionRole.CANDIDATE,
                    container = LinearLayout(
                        id = "candidate_bar",
                        orientation = Orientation.HORIZONTAL,
                        children = emptyList(),
                        height = Dimension.Dp(40f)
                    )
                ),
                Region(
                    id = "keyboard_region",
                    role = RegionRole.KEYBOARD,
                    container = GridLayout(
                        id = "keyboard_grid",
                        columns = 10,
                        cells = buildQwertyCells()
                    )
                )
            )
        ),
        supportedLayers = listOf("NORMAL", "SHIFTED", "CAPS_LOCK")
    )
    
    private fun buildQwertyCells(): List<GridLayout.GridCell> {
        return listOf(
            // Row 1: Q W E R T Y U I O P
            GridLayout.GridCell(letterKey("q", "Q"), 0, 0),
            GridLayout.GridCell(letterKey("w", "W"), 1, 0),
            GridLayout.GridCell(letterKey("e", "E", "3"), 2, 0),
            GridLayout.GridCell(letterKey("r", "R", "4"), 3, 0),
            GridLayout.GridCell(letterKey("t", "T", "5"), 4, 0),
            GridLayout.GridCell(letterKey("y", "Y", "6"), 5, 0),
            GridLayout.GridCell(letterKey("u", "U", "7"), 6, 0),
            GridLayout.GridCell(letterKey("i", "I", "8"), 7, 0),
            GridLayout.GridCell(letterKey("o", "O", "9"), 8, 0),
            GridLayout.GridCell(letterKey("p", "P", "0"), 9, 0),
            
            // Row 2: A S D F G H J K L ENTER
            GridLayout.GridCell(letterKey("a", "A"), 0, 1),
            GridLayout.GridCell(letterKey("s", "S"), 1, 1),
            GridLayout.GridCell(letterKey("d", "D"), 2, 1),
            GridLayout.GridCell(letterKey("f", "F"), 3, 1),
            GridLayout.GridCell(letterKey("g", "G"), 4, 1),
            GridLayout.GridCell(letterKey("h", "H"), 5, 1),
            GridLayout.GridCell(letterKey("j", "J"), 6, 1),
            GridLayout.GridCell(letterKey("k", "K"), 7, 1),
            GridLayout.GridCell(letterKey("l", "L"), 8, 1),
            GridLayout.GridCell(
                KeyDef(
                    id = "enter",
                    styleRef = "key_action",
                    content = ContentSpec(label = "Enter"),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "performEditorAction"))
                    )
                ), 9, 1
            ),
            
            // Row 3: SHIFT Z X C V B N M BACKSPACE
            GridLayout.GridCell(
                KeyDef(
                    id = "shift",
                    styleRef = "key_function",
                    content = ContentSpec(icon = "shift"),
                    width = Dimension.Weight(1.5f),
                    actions = ActionMap(
                        gestures = mapOf(
                            GestureType.TAP to ActionDef(
                                actionType = "cycleLayer",
                                payload = mapOf(
                                    "layers" to kotlinx.serialization.json.JsonArray(
                                        listOf("NORMAL", "SHIFTED", "CAPS_LOCK").map { 
                                            kotlinx.serialization.json.JsonPrimitive(it) 
                                        }
                                    )
                                )
                            )
                        )
                    )
                ), 0, 2, colSpan = 1.5.toInt()
            ),
            GridLayout.GridCell(letterKey("z", "Z"), 1, 2),
            GridLayout.GridCell(letterKey("x", "X"), 2, 2),
            GridLayout.GridCell(letterKey("c", "C"), 3, 2),
            GridLayout.GridCell(letterKey("v", "V"), 4, 2),
            GridLayout.GridCell(letterKey("b", "B"), 5, 2),
            GridLayout.GridCell(letterKey("n", "N"), 6, 2),
            GridLayout.GridCell(letterKey("m", "M"), 7, 2),
            GridLayout.GridCell(
                KeyDef(
                    id = "backspace",
                    styleRef = "key_function",
                    content = ContentSpec(icon = "backspace"),
                    width = Dimension.Weight(1.5f),
                    actions = ActionMap(
                        gestures = mapOf(
                            GestureType.TAP to ActionDef(actionType = "delete", payload = mapOf("count" to kotlinx.serialization.json.JsonPrimitive(1))),
                            GestureType.LONG_PRESS to ActionDef(actionType = "delete", payload = mapOf("count" to kotlinx.serialization.json.JsonPrimitive(10)))
                        )
                    ),
                    repeatable = true
                ), 8, 2, colSpan = 2
            ),
            
            // Row 4: LANG COMMA SPACE PERIOD SYM
            GridLayout.GridCell(
                KeyDef(
                    id = "lang",
                    styleRef = "key_function",
                    content = ContentSpec(icon = "lang"),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "switchLocale"))
                    )
                ), 0, 3
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "comma",
                    styleRef = "key_function",
                    content = ContentSpec(label = ","),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "commitToken", payload = mapOf("token" to kotlinx.serialization.json.JsonPrimitive(","))))
                    )
                ), 1, 3
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "space",
                    styleRef = "key_space",
                    content = ContentSpec(label = "Space"),
                    width = Dimension.Weight(5f),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "commitToken", payload = mapOf("token" to kotlinx.serialization.json.JsonPrimitive(" "))))
                    )
                ), 2, 3, colSpan = 5
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "period",
                    styleRef = "key_function",
                    content = ContentSpec(label = "."),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "commitToken", payload = mapOf("token" to kotlinx.serialization.json.JsonPrimitive("."))))
                    )
                ), 7, 3
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "sym",
                    styleRef = "key_function",
                    content = ContentSpec(label = "?123"),
                    width = Dimension.Weight(2f),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "openPanel", payload = mapOf("panel" to kotlinx.serialization.json.JsonPrimitive("SYMBOL_PANEL"))))
                    )
                ), 8, 3, colSpan = 2
            )
        )
    }
    
    /** 所有内置布局 */
    val all: List<LayoutDoc> = listOf(qwerty)
    
    /** 根据 ID 查找 */
    fun byId(id: String): LayoutDoc? = all.find { it.id == id }
}
