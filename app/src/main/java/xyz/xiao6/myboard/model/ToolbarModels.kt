package xyz.xiao6.myboard.model

import kotlinx.serialization.Serializable

/**
 * Toolbar schema (JSON) used by IME runtime to define toolbar structure (items).
 *
 * Example asset: assets/toolbars/toolbar_default.json
 */
@Serializable
data class ToolbarSpec(
    val version: Int = 1,
    val toolbarId: String,
    val name: String? = null,
    val items: List<ToolbarItemSpec> = emptyList(),
)

@Serializable
data class ToolbarItemSpec(
    /**
     * Stable identifier used for click dispatch (e.g. "layout", "settings").
     */
    val itemId: String,
    /**
     * Human-readable name (also used as contentDescription for accessibility).
     */
    val name: String,
    /**
     * Icon key that maps to a drawable resource (e.g. "layout", "voice").
     */
    val icon: String,
    /**
     * Optional: visibility and ordering.
     */
    val enabled: Boolean = true,
    val priority: Int = 0,
)

