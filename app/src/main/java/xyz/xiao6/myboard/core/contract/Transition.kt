package xyz.xiao6.myboard.core.contract

/**
 * 状态转移事件。
 * 所有状态变更请求通过 TransitionEvent 表达。
 */
sealed interface TransitionEvent {
    data class SwitchLocale(val locale: LocaleTag) : TransitionEvent
    data class SwitchScript(val script: Script) : TransitionEvent
    data class SwitchSchema(val schema: Schema) : TransitionEvent
    data class SwitchLayer(val layer: LayoutLayer) : TransitionEvent
    data class OpenPanel(val panelType: PanelType) : TransitionEvent
    data object ClosePanel : TransitionEvent
    data object ClearComposing : TransitionEvent
    data class SetComposing(val text: String, val candidates: List<Candidate>) : TransitionEvent
}

/**
 * 状态转移结果。
 * 状态转移不能静默失败，必须返回明确结果。
 */
sealed interface TransitionResult {
    data class Applied(val context: KeyboardContext) : TransitionResult
    data class Rejected(val reason: TransitionRejectReason) : TransitionResult
}

/**
 * 转移拒绝原因。
 */
enum class TransitionRejectReason {
    UNSUPPORTED_LOCALE,
    UNSUPPORTED_SCRIPT,
    UNSUPPORTED_SCHEMA,
    ILLEGAL_COMBINATION,
    CAPABILITY_NOT_REGISTERED,
    CAPABILITY_CONFLICT
}