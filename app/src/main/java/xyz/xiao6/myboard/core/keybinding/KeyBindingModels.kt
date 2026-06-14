package xyz.xiao6.myboard.core.keybinding

import kotlinx.serialization.Serializable

/**
 * Code 键盘：物理按键槽位。
 */
data class KeySlot(
    val slotId: String,
    val keyCode: Int,
    val row: Int,
    val col: Int,
    val width: Float = 1f
)

/**
 * 按键绑定：Slot → Output 的映射。
 */
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

data class GestureMap(
    val tap: KeyOutput? = null,
    val longPress: KeyOutput? = null,
    val doubleTap: KeyOutput? = null,
    val flickUp: KeyOutput? = null,
    val flickDown: KeyOutput? = null,
    val flickLeft: KeyOutput? = null,
    val flickRight: KeyOutput? = null
)

/**
 * 按键映射配置。
 */
@Serializable
data class RemapConfig(
    val target: String,
    val remaps: List<RemapEntry>
)

@Serializable
data class RemapEntry(
    val slot: String,
    val layer: String = "BASE",
    val gesture: String? = null,
    val from: String? = null,
    val to: KeyOutputData? = null
)

@Serializable
data class KeyOutputData(
    val type: String,
    val char: String? = null,
    val text: String? = null,
    val keyCode: Int? = null,
    val action: String? = null
)
