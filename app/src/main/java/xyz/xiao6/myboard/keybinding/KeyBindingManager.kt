package xyz.xiao6.myboard.keybinding

/**
 * 按键绑定管理器。
 */
class KeyBindingManager {
    private var bindings = mutableMapOf<String, MutableMap<Layer, KeyBinding>>()

    fun loadBindings(slotDefinitions: List<KeySlot>) {
        // 初始化空绑定
        for (slot in slotDefinitions) {
            bindings[slot.slotId] = mutableMapOf()
        }
    }

    fun resolve(slotId: String, layer: Layer): KeyOutput? {
        return bindings[slotId]?.get(layer)?.output
    }

    fun setBinding(slotId: String, layer: Layer, output: KeyOutput) {
        bindings.getOrPut(slotId) { mutableMapOf() }[layer] = KeyBinding(slotId, layer, output)
    }

    fun applyRemap(remap: RemapConfig) {
        for (entry in remap.remaps) {
            val layer = try { Layer.valueOf(entry.layer) } catch (_: Exception) { Layer.BASE }
            val output = entry.to?.let { convertOutput(it) } ?: continue
            setBinding(entry.slot, layer, output)
        }
    }

    private fun convertOutput(data: KeyOutputData): KeyOutput {
        return when (data.type) {
            "char" -> KeyOutput.Character(data.char ?: "", data.char)
            "text" -> KeyOutput.Text(data.text ?: "")
            "code" -> KeyOutput.Code(data.keyCode ?: 0)
            "action" -> KeyOutput.Action(data.action ?: "", emptyMap())
            else -> KeyOutput.None
        }
    }

    fun getAllBindings(): Map<String, Map<Layer, KeyBinding>> = bindings.toMap()
}
