package xyz.xiao6.myboard.core.layout

import kotlinx.serialization.Serializable

@Serializable
data class KeyboardLayout(
    val schemaVersion: Int = 1,
    val id: String,
    val meta: LayoutMeta? = null,
    val templates: Map<String, KeyTemplate> = emptyMap(),
    val keys: Map<String, KeyData>,
    val rows: List<RowData>,
    val arrangements: Map<String, ArrangementData>,
    val geometry: GeometryConfig = GeometryConfig()
)

@Serializable
data class LayoutMeta(val name: String? = null, val locale: String? = null, val tags: List<String> = emptyList())

@Serializable
data class KeyData(
    val t: String,
    val code: Int = 0,
    val label: String? = null,
    val icon: String? = null,
    val hint: String? = null,
    val weight: Float = 1f,
    val action: String? = null,
    val popup: List<String> = emptyList(),
    val repeatable: Boolean = false
)

@Serializable
data class RowData(val id: String, val keys: List<String>)

@Serializable
data class ArrangementData(val rows: List<String>)

@Serializable
data class KeyTemplate(val role: String? = null, val extends: String? = null)

@Serializable
data class GeometryConfig(
    val height: HeightConfig = HeightConfig(),
    val gap: GapConfig = GapConfig(),
    val padding: PaddingConfig = PaddingConfig()
)

@Serializable
data class HeightConfig(val dp: Int = 260, val min: Int = 190, val max: Int = 360)
@Serializable
data class GapConfig(val h: Float = 6f, val v: Float = 8f)
@Serializable
data class PaddingConfig(val all: Float = 4f)
