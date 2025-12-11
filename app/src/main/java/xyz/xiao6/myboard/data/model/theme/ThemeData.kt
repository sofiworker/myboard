package xyz.xiao6.myboard.data.model.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class ThemeData(
    val name: String? = null,
    val keyboardBackground: SerializableColor,
    val keyBackground: SerializableColor,
    val keyForeground: SerializableColor,
    val suggestionsBackground: SerializableColor,
    val suggestionsForeground: SerializableColor,
    val backgroundImageUri: String? = null,
    val backgroundAlpha: Float = 1.0f
)

@Serializable(with = SerializableColor.Companion::class)
data class SerializableColor(val color: Color) {
    companion object : KSerializer<SerializableColor> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SerializableColor", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: SerializableColor) {
            encoder.encodeString("#%08X".format(value.color.toArgb()))
        }

        override fun deserialize(decoder: Decoder): SerializableColor {
            val colorString = decoder.decodeString()
            return SerializableColor(Color(android.graphics.Color.parseColor(colorString)))
        }
    }
}
