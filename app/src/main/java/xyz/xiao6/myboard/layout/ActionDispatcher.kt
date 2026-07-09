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
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

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
            LayoutActionType.PUSH_TOKEN -> {
                val token = actionDef.stringPayload("token") ?: ""
                val text = applyLayerTransform(token, context)
                InputAction.PushToken(text)
            }
            
            LayoutActionType.COMMIT_TEXT -> {
                val text = actionDef.stringPayload("text") ?: ""
                InputAction.PushToken(text)
            }
            
            LayoutActionType.DELETE -> InputAction.Delete
            LayoutActionType.SPACE -> InputAction.Space
            LayoutActionType.ENTER -> InputAction.Enter
            
            LayoutActionType.SWITCH_LAYER -> {
                val layer = actionDef.enumPayload<LayoutLayer>("layer") ?: return InputAction.Noop
                InputAction.SwitchLayer(layer)
            }
            
            LayoutActionType.CYCLE_LAYER -> {
                val layers = actionDef.enumListPayload<LayoutLayer>("layers")
                    .ifEmpty { listOf(LayoutLayer.NORMAL, LayoutLayer.SHIFTED, LayoutLayer.CAPS_LOCK) }
                
                val currentIdx = layers.indexOf(context.layer)
                val nextIdx = (currentIdx + 1) % layers.size
                InputAction.SwitchLayer(layers[nextIdx])
            }
            
            LayoutActionType.SWITCH_LOCALE -> {
                val locale = actionDef.stringPayload("locale")
                if (locale != null) {
                    InputAction.SwitchLocale(LocaleTag(locale))
                } else {
                    InputAction.SwitchLocale(context.orthogonal.locale)
                }
            }
            
            LayoutActionType.SWITCH_SCRIPT -> {
                val script = actionDef.enumPayload<Script>("script") ?: return InputAction.Noop
                InputAction.SwitchScript(script)
            }
            
            LayoutActionType.SWITCH_SCHEMA -> {
                val schema = actionDef.stringPayload("schema") ?: return InputAction.Noop
                InputAction.SwitchSchema(Schema(schema))
            }
            
            LayoutActionType.OPEN_PANEL -> {
                val panel = actionDef.enumPayload<PanelType>("panel") ?: return InputAction.Noop
                InputAction.OpenPanel(panel)
            }
            
            LayoutActionType.CLOSE_PANEL -> InputAction.ClosePanel
            
            LayoutActionType.COMMIT_CANDIDATE -> {
                val index = actionDef.intPayload("index") ?: -1
                InputAction.CommitCandidate(index)
            }
            
            LayoutActionType.PAGE_NEXT -> InputAction.PageCandidate(1)
            LayoutActionType.PAGE_PREV -> InputAction.PageCandidate(-1)
            LayoutActionType.PAGE_CANDIDATE -> {
                val direction = actionDef.stringPayload("direction") ?: "next"
                InputAction.PageCandidate(if (direction == "next") 1 else -1)
            }
            
            LayoutActionType.RESTORE_PREVIOUS_SCHEMA -> InputAction.RestorePreviousSchema
            LayoutActionType.NOOP -> InputAction.Noop
        }
    }
    
    private fun applyLayerTransform(token: String, context: KeyboardContext): String {
        if (token.length != 1 || !token[0].isLetter()) return token
        
        return when (context.layer) {
            LayoutLayer.NORMAL, LayoutLayer.SYMBOL, LayoutLayer.NUMBER -> token.lowercase()
            LayoutLayer.SHIFTED, LayoutLayer.CAPS_LOCK -> token.uppercase()
        }
    }
    
    private fun ActionDef.stringPayload(key: String): String? =
        (payload[key] as? JsonPrimitive)?.content

    private fun ActionDef.intPayload(key: String): Int? =
        stringPayload(key)?.toIntOrNull()

    private inline fun <reified T : Enum<T>> ActionDef.enumPayload(key: String): T? =
        stringPayload(key)?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() }

    private inline fun <reified T : Enum<T>> ActionDef.enumListPayload(key: String): List<T> =
        (payload[key] as? JsonArray)
            ?.mapNotNull { element ->
                (element as? JsonPrimitive)?.content?.let { value ->
                    runCatching { enumValueOf<T>(value) }.getOrNull()
                }
            }
            ?: emptyList()
}
