package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.state.PanelType

object PanelLayoutResolver {
    fun layoutIdFor(panelType: PanelType): String? {
        return when (panelType) {
            PanelType.SYMBOL -> "symbols_full_surface"
            PanelType.EMOJI -> "emoji_full_surface"
            else -> null
        }
    }
}
