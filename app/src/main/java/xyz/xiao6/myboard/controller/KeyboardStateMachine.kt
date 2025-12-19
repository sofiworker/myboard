package xyz.xiao6.myboard.controller

import xyz.xiao6.myboard.model.Action
import xyz.xiao6.myboard.model.ActionType
import xyz.xiao6.myboard.model.CommandType
import xyz.xiao6.myboard.model.GestureType
import xyz.xiao6.myboard.model.InputEngine
import xyz.xiao6.myboard.model.Key
import xyz.xiao6.myboard.model.KeyAction
import xyz.xiao6.myboard.model.KeyboardLayer
import xyz.xiao6.myboard.model.ModifierKey
import xyz.xiao6.myboard.model.Token
import xyz.xiao6.myboard.model.KeyActionDefault
import java.util.Locale

/**
 * Reduces key trigger events into high-level effects (commit text, push token, switch layer, etc.).
 *
 * NOTE: Key model follows `assets/layouts/a.kt` and is intentionally NOT backward compatible.
 */
object KeyboardStateMachine {

    sealed interface Event {
        data class Triggered(val key: Key, val trigger: GestureType) : Event
        data class ActionEvent(val action: KeyAction, val key: Key? = null) : Event
    }

    sealed interface Effect {
        data class CommitText(val text: String) : Effect
        data class PushToken(val token: Token) : Effect
        data object CommitComposing : Effect
        data object Back : Effect
        data object ToggleLocale : Effect
        data class SwitchLayout(val layoutId: String) : Effect
        data class SwitchLayer(val layer: KeyboardLayer) : Effect
        data class SwitchEngine(val engine: InputEngine) : Effect
        data class ToggleModifier(val modifier: ModifierKey) : Effect
        data object ClearComposition : Effect
        data object NoOp : Effect
    }

    data class Model(
        val currentLayoutId: String,
        val state: LayoutState = LayoutState(),
        val keys: List<Key> = emptyList(),
    )

    fun reduce(model: Model, event: Event): Pair<Model, List<Effect>> {
        val actions: List<Action> =
            when (event) {
                is Event.Triggered -> resolveTriggeredActions(event.key, event.trigger, model.state)
                is Event.ActionEvent -> resolveInlineKeyAction(event.action, event.key, model.state)
            }

        if (actions.isEmpty()) return model to listOf(Effect.NoOp)

        val effects = actions.flatMap { actionToEffects(it) }
        return model to effects.ifEmpty { listOf(Effect.NoOp) }
    }

    private fun resolveTriggeredActions(key: Key, trigger: GestureType, state: LayoutState): List<Action> {
        val action = key.actions[trigger] ?: return defaultActionsFor(key, trigger)
        if (action.cases.isNotEmpty()) {
            val matched = action.cases.firstOrNull { matches(it.whenCondition, state) }
            if (matched != null) return matched.doActions
        }

        if (action.defaultActions.isNotEmpty()) return action.defaultActions

        return defaultActionsForNoMatchedCase(key, trigger, action).ifEmpty {
            if (action.cases.isEmpty()) defaultActionsForActionType(key, trigger, action.actionType) else emptyList()
        }
    }

    private fun resolveInlineKeyAction(action: KeyAction, key: Key?, state: LayoutState): List<Action> {
        if (action.cases.isNotEmpty()) {
            val matched = action.cases.firstOrNull { matches(it.whenCondition, state) }
            if (matched != null) return matched.doActions
        }

        if (action.defaultActions.isNotEmpty()) return action.defaultActions

        val k = key ?: return emptyList()
        return defaultActionsForNoMatchedCase(k, GestureType.TAP, action).ifEmpty {
            if (action.cases.isEmpty()) defaultActionsForActionType(k, GestureType.TAP, action.actionType) else emptyList()
        }
    }

    private fun defaultActionsForNoMatchedCase(key: Key, trigger: GestureType, action: KeyAction): List<Action> {
        if (trigger != GestureType.TAP) return emptyList()
        return when (action.fallback) {
            KeyActionDefault.PRIMARY_CODE_AS_TOKEN -> {
                val code = key.primaryCode
                if (code < 0 || !Character.isValidCodePoint(code)) return emptyList()
                val text = String(Character.toChars(code))
                if (text.isBlank()) emptyList() else listOf(Action.PushToken(Token.Literal(text)))
            }
            null -> emptyList()
        }
    }

    private fun defaultActionsFor(key: Key, trigger: GestureType): List<Action> {
        if (trigger != GestureType.TAP) return emptyList()
        val text = key.ui.label ?: key.label ?: ""
        if (text.isBlank()) return emptyList()
        return listOf(Action.CommitText(text))
    }

    private fun defaultActionsForActionType(key: Key?, trigger: GestureType, actionType: ActionType): List<Action> {
        if (trigger != GestureType.TAP) return emptyList()
        val text = key?.ui?.label ?: key?.label ?: ""

        return when (actionType) {
            ActionType.COMMIT_TEXT -> text.takeIf { it.isNotBlank() }?.let { listOf(Action.CommitText(it)) }.orEmpty()
            ActionType.COMMIT_COMPOSING -> listOf(Action.CommitComposing)
            ActionType.PUSH_TOKEN -> text.takeIf { it.isNotBlank() }?.let { listOf(Action.PushToken(Token.Literal(it))) }.orEmpty()
            ActionType.CLEAR_COMPOSITION -> listOf(Action.ClearComposition)
            ActionType.REPLACE_TOKENS -> listOf(Action.NoOp)
            ActionType.COMMAND -> {
                val cmd =
                    when (key?.keyId) {
                        xyz.xiao6.myboard.model.KeyIds.BACKSPACE -> CommandType.BACKSPACE
                        xyz.xiao6.myboard.model.KeyIds.SPACE -> CommandType.SPACE
                        xyz.xiao6.myboard.model.KeyIds.ENTER -> CommandType.ENTER
                        else -> null
                    }
                cmd?.let { listOf(Action.Command(it)) }.orEmpty()
            }
            ActionType.BACK -> listOf(Action.Back)
            ActionType.TOGGLE_LOCALE -> listOf(Action.ToggleLocale)
            ActionType.SWITCH_LAYOUT -> emptyList()
            ActionType.SWITCH_LAYER, ActionType.SWITCH_ENGINE, ActionType.TOGGLE_MODIFIER, ActionType.OPEN_POPUP, ActionType.NO_OP ->
                emptyList()
        }
    }

    private fun matches(cond: xyz.xiao6.myboard.model.WhenCondition?, state: LayoutState): Boolean {
        if (cond == null) return true
        cond.engine?.let {
            val current = state.engine?.trim().orEmpty()
            if (!it.name.equals(current, ignoreCase = true)) return false
        }
        cond.layer?.let {
            if (!it.name.equals(state.layer.name, ignoreCase = true)) return false
        }
        // Modifiers/composing/locale are not wired into LayoutState yet; treat them as "not match" only when explicitly required.
        if (cond.modifiers.isNotEmpty()) return false
        if (cond.composing != null) return false
        if (!cond.locale.isNullOrBlank()) {
            val current = normalizeLocaleTag(state.localeTag.orEmpty())
            val required = normalizeLocaleTag(cond.locale)
            if (required.isBlank()) return false
            if (!localeMatches(required = required, current = current)) return false
        }
        return true
    }

    private fun localeMatches(required: String, current: String): Boolean {
        if (current.isBlank()) return false
        if (required.contains('-')) return required.equals(current, ignoreCase = true)
        // Language-only: "zh" matches "zh-CN" / "zh-HK" etc.
        return current.startsWith(required, ignoreCase = true)
    }

    private fun normalizeLocaleTag(tag: String): String {
        val t = tag.trim().replace('_', '-')
        val parts = t.split('-').filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""
        val language = parts[0].lowercase(Locale.ROOT)
        val region = parts.getOrNull(1)?.uppercase(Locale.ROOT)
        return if (region.isNullOrBlank()) language else "$language-$region"
    }

    private fun actionToEffects(action: Action): List<Effect> {
        return when (action) {
            is Action.CommitText -> listOf(Effect.CommitText(action.text))
            Action.CommitComposing -> listOf(Effect.CommitComposing)
            is Action.PushToken -> listOf(Effect.PushToken(action.token))
            Action.ClearComposition -> listOf(Effect.ClearComposition)
            Action.Back -> listOf(Effect.Back)
            Action.ToggleLocale -> listOf(Effect.ToggleLocale)
            is Action.Command -> {
                val text =
                    when (action.commandType) {
                        CommandType.BACKSPACE -> "\b"
                        CommandType.ENTER -> "\n"
                        CommandType.SPACE -> " "
                        else -> ""
                    }
                if (text.isEmpty()) listOf(Effect.NoOp) else listOf(Effect.CommitText(text))
            }
            is Action.SwitchLayer -> listOf(Effect.SwitchLayer(action.layer))
            is Action.SwitchLayout -> listOf(Effect.SwitchLayout(action.layoutId))
            is Action.SwitchEngine -> listOf(Effect.SwitchEngine(action.engine))
            is Action.ToggleModifier -> listOf(Effect.ToggleModifier(action.modifier))
            is Action.OpenPopup -> listOf(Effect.NoOp)
            Action.NoOp -> listOf(Effect.NoOp)
        }
    }
}
