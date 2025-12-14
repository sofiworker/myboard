package xyz.xiao6.myboard.controller

import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyTrigger

/**
 * 状态机：把输入事件归约为“布局切换”或“状态切换”或“提交输出”等 effect。
 * State machine: reduces input events into effects (layout switch vs state change vs commit).
 *
 * 关键约束 / Key constraints:
 * - Layout Switch：几何/Key/Row 发生根本变化 -> 必须加载新的 Layout JSON，并触发 requestLayout()
 * - State Change：仅内容/样式/隐藏变化 -> 只更新 LayoutState，并局部 invalidate(Rect)，禁止 requestLayout()
 */
object KeyboardStateMachine {

    sealed interface Event {
        data class Triggered(val key: Key, val trigger: KeyTrigger) : Event
        data class Action(val action: KeyAction) : Event
    }

    sealed interface Effect {
        data class CommitText(val text: String) : Effect
        data class SwitchLayout(val layoutId: String) : Effect
        data class SwitchLocale(val localeTag: String) : Effect
        data object BackLayout : Effect
        data class UpdateState(val newState: LayoutState, val invalidateKeyIds: Set<String>) : Effect
        data object NoOp : Effect
    }

    data class Model(
        val currentLayoutId: String,
        val state: LayoutState = LayoutState(),
        val keys: List<Key> = emptyList(),
    )

    fun reduce(model: Model, event: Event): Pair<Model, List<Effect>> {
        val action = when (event) {
            is Event.Triggered -> event.key.behaviors[event.trigger] ?: defaultActionFor(event.key, event.trigger)
            is Event.Action -> event.action
        } ?: return model to listOf(Effect.NoOp)
        return reduceAction(model, action)
    }

    private fun reduceAction(model: Model, action: KeyAction): Pair<Model, List<Effect>> {
        return when (action.actionType) {
            ActionType.COMMIT -> {
                val text = action.value ?: return model to listOf(Effect.NoOp)
                model to listOf(Effect.CommitText(text))
            }

            ActionType.BACKSPACE -> model to listOf(Effect.CommitText("\b"))
            ActionType.SPACE -> model to listOf(Effect.CommitText(" "))

            ActionType.SWITCH_LAYOUT -> {
                val targetLayoutId = action.value ?: return model to listOf(Effect.NoOp)
                if (targetLayoutId == model.currentLayoutId) model to listOf(Effect.NoOp)
                else model to listOf(Effect.SwitchLayout(targetLayoutId))
            }

            ActionType.SWITCH_LOCALE -> {
                val tag = action.value?.trim().orEmpty()
                if (tag.isBlank()) model to listOf(Effect.NoOp)
                else model to listOf(Effect.SwitchLocale(tag))
            }

            ActionType.BACK_LAYOUT -> model to listOf(Effect.BackLayout)

            ActionType.TOGGLE_SHIFT -> {
                val newShift = when (model.state.shift) {
                    ShiftState.OFF -> ShiftState.ON
                    ShiftState.ON -> ShiftState.OFF
                    ShiftState.CAPS_LOCK -> ShiftState.OFF
                }
                val newState = model.state.copy(shift = newShift)
                val invalidateIds = alphabeticKeyIds(model.keys)
                model.copy(state = newState) to listOf(Effect.UpdateState(newState, invalidateIds))
            }

            ActionType.SET_LAYER -> {
                val layerName = action.value ?: return model to listOf(Effect.NoOp)
                val newLayer = runCatching { Layer.valueOf(layerName) }.getOrNull() ?: return model to listOf(Effect.NoOp)
                if (newLayer == model.state.layer) return model to listOf(Effect.NoOp)
                val newState = model.state.copy(layer = newLayer)
                val invalidateIds = model.keys.mapTo(LinkedHashSet()) { it.keyId }
                model.copy(state = newState) to listOf(Effect.UpdateState(newState, invalidateIds))
            }

            ActionType.TOGGLE_HOTWORD_HIGHLIGHT -> {
                val keyId = action.value ?: return model to listOf(Effect.NoOp)
                val highlighted = model.state.highlightedKeyIds.toMutableSet()
                if (!highlighted.add(keyId)) highlighted.remove(keyId)
                val newState = model.state.copy(highlightedKeyIds = highlighted)
                model.copy(state = newState) to listOf(Effect.UpdateState(newState, setOf(keyId)))
            }

            ActionType.SHOW_POPUP -> model to listOf(Effect.NoOp)
        }
    }

    private fun defaultActionFor(key: Key, trigger: KeyTrigger): KeyAction? {
        if (trigger != KeyTrigger.TAP) return null
        return KeyAction(actionType = ActionType.COMMIT, value = key.label)
    }

    private fun alphabeticKeyIds(keys: List<Key>): Set<String> {
        val out = LinkedHashSet<String>()
        for (key in keys) {
            val label = key.label
            if (label.length == 1 && label[0].isLetter()) out += key.keyId
        }
        return out
    }
}
