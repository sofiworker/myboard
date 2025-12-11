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
    data class CommitSuggestion(val suggestion: String) : KeyAction()
    @Serializable
    data class Shift(val layout: String?) : KeyAction()
    @Serializable
    data class SwitchToLayout(val layout: String) : KeyAction()
    @Serializable
    object ShowSettings : KeyAction()
    @Serializable
    object SystemEmoji : KeyAction()
    @Serializable
    object SystemClipboard : KeyAction()
    @Serializable
    object SystemVoice : KeyAction()
    @Serializable
    data class MoveCursor(val offset: Int) : KeyAction()
    @Serializable
    data class MoveFloatingWindow(val deltaX: Int, val deltaY: Int) : KeyAction()
}
