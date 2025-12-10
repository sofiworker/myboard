package xyz.xiao6.myboard.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class KeyAction {
    @Serializable
    data class InsertText(val text: String) : KeyAction()

    @Serializable
    object Delete : KeyAction()

    @Serializable
    object Space : KeyAction()

    @Serializable
    object Shift : KeyAction()

    @Serializable
    data class SwitchToLayout(val layout: String) : KeyAction()

    @Serializable
    data class CommitSuggestion(val suggestion: String) : KeyAction()

    @Serializable
    object ShowSettings : KeyAction()
}
