package xyz.xiao6.myboard.core.input

import xyz.xiao6.myboard.core.keyboard.ShiftState

/**
 * 语言切换状态机。
 */
class LanguageSwitchManager(
    private val rules: List<SwitchRule>,
    private val languageRegistry: LanguageRegistry
) {
    private val languageStates = mutableMapOf<String, LanguageState>()
    private val history = ArrayDeque<String>(10)

    fun switch(from: String, to: String): SwitchAction {
        val fromLang = languageRegistry.get(from) ?: return SwitchAction.Noop
        val toLang = languageRegistry.get(to) ?: return SwitchAction.Noop

        val rule = rules.firstOrNull {
            (it.fromType == fromLang.type || it.fromType == "*") &&
            (it.toType == toLang.type || it.toType == "*")
        } ?: rules.last()

        saveState(from)
        val restoredState = if (rule.preservePerLanguageState) languageStates[to] else null
        history.addLast(from)

        return SwitchAction(
            targetLanguage = to,
            clearComposing = rule.clearComposing,
            shiftState = restoredState?.shift ?: ShiftState.OFF,
            arrangement = restoredState?.arrangement ?: toLang.defaultArrangement,
            switchLayoutDirection = rule.switchLayoutDirection
        )
    }

    fun undo(): SwitchAction? {
        val previous = history.removeLastOrNull() ?: return null
        val current = history.lastOrNull() ?: return null
        return switch(current, previous)
    }

    private fun saveState(langId: String) {
        languageStates[langId] = LanguageState(ShiftState.OFF, "alpha")
    }
}

data class LanguageState(
    val shift: ShiftState,
    val arrangement: String,
    val capsLock: Boolean = false
)

data class SwitchAction(
    val targetLanguage: String,
    val clearComposing: Boolean,
    val shiftState: ShiftState,
    val arrangement: String,
    val switchLayoutDirection: Boolean
) {
    companion object {
        val Noop = SwitchAction("", false, ShiftState.OFF, "", false)
    }
}

data class SwitchRule(
    val fromType: String,
    val toType: String,
    val clearComposing: Boolean = true,
    val shiftBehavior: String = "resetToOff",
    val resetArrangement: Boolean = true,
    val switchLayoutDirection: Boolean = false,
    val preservePerLanguageState: Boolean = true
)

/**
 * 语言注册表。
 */
class LanguageRegistry {
    private val languages = mutableMapOf<String, LanguageInfo>()

    fun register(info: LanguageInfo) {
        languages[info.id] = info
    }

    fun get(id: String): LanguageInfo? = languages[id]

    fun listAll(): List<LanguageInfo> = languages.values.toList()
}

data class LanguageInfo(
    val id: String,
    val name: String,
    val type: String,               // DIRECT_LTR, DIRECT_RTL, COMPOSITION, COMPLEX
    val direction: String = "LTR",
    val defaultArrangement: String = "alpha",
    val inputMethodId: String
)
