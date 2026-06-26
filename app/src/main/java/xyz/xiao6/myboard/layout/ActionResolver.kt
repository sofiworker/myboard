package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.state.*

/**
 * 动作解析器。
 * 根据 KeyModel + KeyType 自动推断默认 TAP 动作。
 * longPress / swipeActions 优先级高于默认 TAP 动作。
 */
class ActionResolver {

    /**
     * 根据按键和手势解析出 InputAction。
     * @param key 按键模型
     * @param gesture 用户手势类型
     * @param context 当前键盘上下文
     */
    fun resolve(key: KeyModel, gesture: GestureType, context: KeyboardContext): InputAction {
        // 非 TAP 手势：检查 swipeActions / longPress
        if (gesture != GestureType.TAP) {
            val action = when (gesture) {
                GestureType.SWIPE_UP -> key.swipeActions?.get(Direction.UP)
                GestureType.SWIPE_DOWN -> key.swipeActions?.get(Direction.DOWN)
                GestureType.SWIPE_LEFT -> key.swipeActions?.get(Direction.LEFT)
                GestureType.SWIPE_RIGHT -> key.swipeActions?.get(Direction.RIGHT)
                GestureType.LONG_PRESS -> {
                    // 长按：如果有 longPress 列表则取第一个
                    key.longPress?.firstOrNull()
                }
                else -> null
            }
            if (action != null) {
                return resolveAction(action, context)
            }
        }

        // TAP 动作自动推断
        if (gesture == GestureType.TAP) {
            val action = resolveTapAction(key, context)
            if (action != InputAction.Noop) return action
        }

        return InputAction.Noop
    }

    private fun resolveTapAction(key: KeyModel, context: KeyboardContext): InputAction {
        return when (key.type) {
            KeyType.CHAR -> {
                val token = key.output ?: key.id
                val transformed = if (token.length == 1 && token[0].isLetter()) {
                    applyLayerTransform(token, context.layer)
                } else token
                InputAction.PushToken(transformed)
            }
            KeyType.SPACE -> InputAction.Space
            KeyType.ENTER -> InputAction.Enter
            KeyType.BACKSPACE -> InputAction.Delete
            KeyType.SHIFT -> {
                val layers = listOf("NORMAL", "SHIFTED", "CAPS_LOCK")
                val currentIdx = layers.indexOf(context.layer.name)
                val nextIdx = (currentIdx + 1) % layers.size
                InputAction.SwitchLayer(parseLayer(layers[nextIdx]))
            }
            KeyType.SYMBOL_SWITCH -> InputAction.OpenPanel(PanelType.SYMBOL)
            KeyType.EMOJI_SWITCH -> InputAction.OpenPanel(PanelType.EMOJI)
            KeyType.FUNCTION -> {
                // FUNCTION 类型需要根据 id 映射
                resolveFunctionById(key.id, context)
            }
            KeyType.EMPTY, KeyType.PLACEHOLDER -> InputAction.Noop
        }
    }

    private fun resolveFunctionById(id: String, context: KeyboardContext): InputAction {
        return when (id) {
            "lang" -> InputAction.SwitchLocale(context.orthogonal.locale)
            "emoji" -> InputAction.OpenPanel(PanelType.EMOJI)
            "clipboard" -> InputAction.OpenPanel(PanelType.CLIPBOARD)
            "settings" -> InputAction.OpenPanel(PanelType.SETTINGS)
            else -> InputAction.Noop
        }
    }

    fun resolveAction(action: KeyAction, context: KeyboardContext): InputAction {
        return when (action.action) {
            "commitToken" -> {
                val token = action.payload["token"] ?: ""
                InputAction.PushToken(token)
            }
            "commitText" -> {
                val text = action.payload["text"] ?: ""
                InputAction.PushToken(text)
            }
            "delete" -> InputAction.Delete
            "space" -> InputAction.Space
            "enter" -> InputAction.Enter
            "switchLayer" -> {
                val layer = action.payload["layer"] ?: "NORMAL"
                InputAction.SwitchLayer(parseLayer(layer))
            }
            "cycleLayer" -> {
                val layersStr = action.payload["layers"] ?: "NORMAL,SHIFTED,CAPS_LOCK"
                val layers = layersStr.split(",")
                val currentIdx = layers.indexOf(context.layer.name)
                val nextIdx = (currentIdx + 1) % layers.size
                InputAction.SwitchLayer(parseLayer(layers[nextIdx]))
            }
            "switchLocale" -> {
                val locale = action.payload["locale"]
                if (locale != null) InputAction.SwitchLocale(LocaleTag(locale))
                else InputAction.SwitchLocale(context.orthogonal.locale)
            }
            "switchSchema" -> {
                val schema = action.payload["schema"] ?: return InputAction.Noop
                InputAction.SwitchSchema(Schema(schema))
            }
            "openPanel" -> {
                val panel = action.payload["panel"] ?: return InputAction.Noop
                InputAction.OpenPanel(parsePanel(panel))
            }
            "closePanel" -> InputAction.ClosePanel
            "selectCandidate" -> {
                val index = action.payload["index"]?.toIntOrNull() ?: -1
                InputAction.CommitCandidate(index)
            }
            "pageCandidate" -> {
                val direction = action.payload["direction"] ?: "next"
                InputAction.PageCandidate(if (direction == "next") 1 else -1)
            }
            "noop" -> InputAction.Noop
            else -> InputAction.Noop
        }
    }

    private fun applyLayerTransform(token: String, layer: LayoutLayer): String {
        if (token.length != 1 || !token[0].isLetter()) return token
        return when (layer) {
            LayoutLayer.SHIFTED, LayoutLayer.CAPS_LOCK -> token.uppercase()
            else -> token.lowercase()
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
            "EMOJI" -> PanelType.EMOJI
            "SYMBOL", "SYMBOL_PANEL" -> PanelType.SYMBOL
            "CLIPBOARD" -> PanelType.CLIPBOARD
            "LLM" -> PanelType.LLM
            "STT" -> PanelType.STT
            "KAOMOJI" -> PanelType.KAOMOJI
            "TEXT_EXPANSION" -> PanelType.TEXT_EXPANSION
            else -> PanelType.NONE
        }
    }
}
