package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

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
            GridLayout.GridCell(letterKey("q", "Q"), 0f, 0f),
            GridLayout.GridCell(letterKey("w", "W"), 1f, 0f),
            GridLayout.GridCell(letterKey("e", "E", "3"), 2f, 0f),
            GridLayout.GridCell(letterKey("r", "R", "4"), 3f, 0f),
            GridLayout.GridCell(letterKey("t", "T", "5"), 4f, 0f),
            GridLayout.GridCell(letterKey("y", "Y", "6"), 5f, 0f),
            GridLayout.GridCell(letterKey("u", "U", "7"), 6f, 0f),
            GridLayout.GridCell(letterKey("i", "I", "8"), 7f, 0f),
            GridLayout.GridCell(letterKey("o", "O", "9"), 8f, 0f),
            GridLayout.GridCell(letterKey("p", "P", "0"), 9f, 0f),
            
            // Row 2: A S D F G H J K L ENTER
            GridLayout.GridCell(letterKey("a", "A"), 0f, 1f),
            GridLayout.GridCell(letterKey("s", "S"), 1f, 1f),
            GridLayout.GridCell(letterKey("d", "D"), 2f, 1f),
            GridLayout.GridCell(letterKey("f", "F"), 3f, 1f),
            GridLayout.GridCell(letterKey("g", "G"), 4f, 1f),
            GridLayout.GridCell(letterKey("h", "H"), 5f, 1f),
            GridLayout.GridCell(letterKey("j", "J"), 6f, 1f),
            GridLayout.GridCell(letterKey("k", "K"), 7f, 1f),
            GridLayout.GridCell(letterKey("l", "L"), 8f, 1f),
            GridLayout.GridCell(
                KeyDef(
                    id = "enter",
                    styleRef = "key_action",
                    content = ContentSpec(label = "Enter"),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "performEditorAction"))
                    )
                ), 9f, 1f
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
                ), 0f, 2f, colSpan = 1.5f
            ),
            GridLayout.GridCell(letterKey("z", "Z"), 1f, 2f),
            GridLayout.GridCell(letterKey("x", "X"), 2f, 2f),
            GridLayout.GridCell(letterKey("c", "C"), 3f, 2f),
            GridLayout.GridCell(letterKey("v", "V"), 4f, 2f),
            GridLayout.GridCell(letterKey("b", "B"), 5f, 2f),
            GridLayout.GridCell(letterKey("n", "N"), 6f, 2f),
            GridLayout.GridCell(letterKey("m", "M"), 7f, 2f),
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
                ), 8f, 2f, colSpan = 2f
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
                ), 0f, 3f
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "comma",
                    styleRef = "key_function",
                    content = ContentSpec(label = ","),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "commitToken", payload = mapOf("token" to kotlinx.serialization.json.JsonPrimitive(","))))
                    )
                ), 1f, 3f
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
                ), 2f, 3f, colSpan = 5f
            ),
            GridLayout.GridCell(
                KeyDef(
                    id = "period",
                    styleRef = "key_function",
                    content = ContentSpec(label = "."),
                    actions = ActionMap(
                        gestures = mapOf(GestureType.TAP to ActionDef(actionType = "commitToken", payload = mapOf("token" to kotlinx.serialization.json.JsonPrimitive("."))))
                    )
                ), 7f, 3f
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
                ), 8f, 3f, colSpan = 2f
            )
        )
    }
    
    /** 所有内置布局 */
    val all: List<LayoutDoc> = listOf(qwerty)
    
    /** 根据 ID 查找 */
    fun byId(id: String): LayoutDoc? = all.find { it.id == id }
}
