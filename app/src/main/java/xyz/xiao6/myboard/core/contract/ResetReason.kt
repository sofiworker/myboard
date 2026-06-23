package xyz.xiao6.myboard.core.contract

/**
 * 必须触发 session reset 或 close 的原因。
 * 发送方必须是 Android 平台桥接层。InputPipeline 只负责接收并执行 reset。
 */
enum class ResetReason {
    LocaleChanged,
    ScriptChanged,
    SchemaChanged,
    InputStarted,
    InputFinished,
    UserCleared,
    DictionaryUpdated,
    CursorMoved,
    InputConnectionInvalid,
    ResourceFailed
}