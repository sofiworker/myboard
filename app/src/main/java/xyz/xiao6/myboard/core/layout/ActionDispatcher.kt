package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 动作分发器。
 */
class ActionDispatcher {
    
    fun dispatch(key: KeyDef, gesture: GestureType, context: KeyboardContext): InputAction {
        val actionDef = key.actions.gestures[gesture]
            ?: return InputAction.Noop
        
        return resolveAction(actionDef, context)
    }
    
    private fun resolveAction(actionDef: ActionDef, context: KeyboardContext): InputAction {
        return when (actionDef.actionType) {
            "commitToken" -> {
                val token = actionDef.payload["token"]?.let { 
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content 
                } ?: ""
                val text = applyLayerTransform(token, context)
                InputAction.PushToken(text)
            }
            
            "commitText" -> {
                val text = actionDef.payload["text"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: ""
                InputAction.PushToken(text)
            }
            
            "delete" -> InputAction.Delete
            "space" -> InputAction.Space
            "enter" -> InputAction.Enter
            
            "switchLayer" -> {
                val layer = actionDef.payload["layer"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: "NORMAL"
                InputAction.SwitchLayer(parseLayer(layer))
            }
            
            "cycleLayer" -> {
                val layers = actionDef.payload["layers"]?.let { element ->
                    (element as? kotlinx.serialization.json.JsonArray)?.mapNotNull {
                        (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                    }
                } ?: listOf("NORMAL", "SHIFTED", "CAPS_LOCK")
                
                val currentIdx = layers.indexOf(context.layer.name)
                val nextIdx = (currentIdx + 1) % layers.size
                InputAction.SwitchLayer(parseLayer(layers[nextIdx]))
            }
            
            "switchLocale" -> {
                val locale = actionDef.payload["locale"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                }
                if (locale != null) {
                    InputAction.SwitchLocale(LocaleTag(locale))
                } else {
                    InputAction.SwitchLocale(context.orthogonal.locale)
                }
            }
            
            "switchSchema" -> {
                val schema = actionDef.payload["schema"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: return InputAction.Noop
                InputAction.SwitchSchema(Schema(schema))
            }
            
            "openPanel" -> {
                val panel = actionDef.payload["panel"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: return InputAction.Noop
                InputAction.OpenPanel(parsePanel(panel))
            }
            
            "closePanel" -> InputAction.ClosePanel
            
            "selectCandidate" -> {
                val index = actionDef.payload["index"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()
                } ?: -1
                InputAction.CommitCandidate(index)
            }
            
            "pageCandidate" -> {
                val direction = actionDef.payload["direction"]?.let {
                    (it as? kotlinx.serialization.json.JsonPrimitive)?.content
                } ?: "next"
                InputAction.PageCandidate(if (direction == "next") 1 else -1)
            }
            
            "noop" -> InputAction.Noop
            else -> InputAction.Noop
        }
    }
    
    private fun applyLayerTransform(token: String, context: KeyboardContext): String {
        if (token.length != 1 || !token[0].isLetter()) return token
        
        return when (context.layer) {
            LayoutLayer.NORMAL, LayoutLayer.SYMBOL, LayoutLayer.NUMBER -> token.lowercase()
            LayoutLayer.SHIFTED, LayoutLayer.CAPS_LOCK -> token.uppercase()
        }
    }
    
    private fun parseLayer(name: String): LayoutLayer {
        return when (name.uppercase()) {
            "NORMAL" -> LayoutLayer.NORMAL
            "SHIFTED" -> LayoutLayer.SHIFTED
            "CAPS_LOCK" -> LayoutLayer.CAPS_LOCK
            else -> LayoutLayer.NORMAL
        }
    }
    
    private fun parsePanel(name: String): PanelType {
        return when (name.uppercase()) {
            "NONE" -> PanelType.NONE
            "EMOJI" -> PanelType.EMOJI
            "SYMBOL" -> PanelType.SYMBOL
            "CLIPBOARD" -> PanelType.CLIPBOARD
            "LLM" -> PanelType.LLM
            "STT" -> PanelType.STT
            else -> PanelType.NONE
        }
    }
}