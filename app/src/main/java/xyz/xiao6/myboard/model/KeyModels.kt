package xyz.xiao6.myboard.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object KeyIds {
    const val BACKSPACE = "key_backspace"
    const val SPACE = "key_space"
    const val ENTER = "key_enter"
    const val SEGMENT = "key_segment"
    const val LAYER_TOGGLE = "key_layer_toggle"
    const val BACK = "key_back"
}

/**
 * Well-known key primary codes (keep them centralized; avoid magic numbers).
 *
 * Note: these are layout-level codes; command handling still uses the emitted text ("\b", "\n", " ").
 */
object KeyPrimaryCodes {
    /** Shift modifier key (UI/state only). */
    const val SHIFT: Int = -1

    /** Switch layout key (e.g. QWERTY <-> T9). */
    const val MODE_SWITCH: Int = -2

    /** Android backspace/delete semantics in this project (mapped to "\b" at runtime). */
    const val BACKSPACE: Int = -5

    /** Locale toggle key (UI/state only). */
    const val LOCALE_TOGGLE: Int = -99

    /** Manual segment key (commit composing as raw text). */
    const val SEGMENT: Int = -98

    /** Symbols panel key (UI action). */
    const val SYMBOLS_PANEL: Int = -100

    /** \"ABC\" back key for numeric/dialer layouts. */
    const val BACK_TO_QWERTY: Int = -101

    /** Generic back key: go back to previous layout (layout history). */
    const val BACK: Int = -102

    /** Retype / re-input key used by candidates UI. */
    const val REINPUT: Int = -200

    /** ASCII/Unicode space. */
    const val SPACE: Int = 32

    /** ASCII/Unicode LF (newline). */
    const val ENTER: Int = 10
}

@Serializable
enum class ActionType {
    COMMIT_TEXT,
    COMMIT_COMPOSING,
    PUSH_TOKEN,
    REPLACE_TOKENS,
    CLEAR_COMPOSITION,
    COMMAND,
    BACK,
    TOGGLE_LOCALE,
    SWITCH_LAYER,
    SWITCH_ENGINE,

    SWITCH_LAYOUT,

    TOGGLE_MODIFIER,
    OPEN_POPUP,
    NO_OP,
}

@Serializable
enum class TokenType {
    LITERAL,
    SYMBOL_SET,
    WEIGHTED_SET,
    SEQUENCE,
    MARKER,
}

@Serializable
enum class GestureType {
    TAP,
    DOUBLE_TAP,
    LONG_PRESS,
    FLICK_UP,
    FLICK_DOWN,
    FLICK_LEFT,
    FLICK_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    SWIPE_LEFT,
    SWIPE_RIGHT,
}

@Serializable
enum class ModifierKey {
    SHIFT,
    CAPS_LOCK,
    ALT,
}

@Serializable
enum class KeyboardLayer {
    ALPHA,
    NUM,
    SYMBOL,
    EMOJI,
    CUSTOM,
}

@Serializable
enum class InputEngine {
    EN_DIRECT,
    ZH_PINYIN,
    ZH_WUBI,
    JA_ROMAJI,
    JA_KANA,
}

@Serializable
enum class CommandType {
    BACKSPACE,
    ENTER,
    SPACE,
    TAB,
    MOVE_CURSOR_LEFT,
    MOVE_CURSOR_RIGHT,
    MOVE_CURSOR_UP,
    MOVE_CURSOR_DOWN,
    MOVE_CURSOR_START,
    MOVE_CURSOR_END,
    SELECT_CANDIDATE,
    NEXT_PAGE,
    PREV_PAGE,
    CUT,
    COPY,
    PASTE,
    SELECT_ALL,
    UNDO,
    REDO,
}

@Serializable
data class Key(
    val keyId: String,
    val primaryCode: Int,
    val label: String? = null,
    val hints: Map<String, String> = emptyMap(),
    val ui: KeyUI,
    val actions: Map<GestureType, KeyAction> = emptyMap(),
)

@Serializable
data class KeyUI(
    val label: String? = null,
    val styleId: String,
    val gridPosition: GridPosition,
    val icons: List<String> = emptyList(),
    val accessibilityLabel: String? = null,
    /**
     * Width weight inside its row when using weight-based layout.
     *
     * When all keys in a row use the default (1.0), geometry falls back to gridPosition-based layout.
     */
    val widthWeight: Float = 1f,
)

@Serializable
data class GridPosition(
    val startCol: Int,
    val startRow: Int,
    val spanCols: Int = 1,
)

@Serializable
data class KeyAction(
    val actionType: ActionType,
    val cases: List<KeyActionCase> = emptyList(),
    /**
     * Default branch actions:
     * - Used when [cases] is empty
     * - Or when no case matches
     */
    @SerialName("default")
    val defaultActions: List<Action> = emptyList(),
    /**
     * Built-in fallback when defaultActions is empty.
     */
    val fallback: KeyActionDefault? = null,
)

@Serializable
enum class KeyActionDefault {
    /**
     * Emit [Key.primaryCode] as a literal token (code point -> string) via PUSH_TOKEN.
     * Invalid/negative code points are ignored (no-op).
     */
    PRIMARY_CODE_AS_TOKEN,
}

@Serializable
data class KeyActionCase(
    val whenCondition: WhenCondition? = null,
    val doActions: List<Action> = emptyList(),
)

@Serializable
data class WhenCondition(
    val layer: KeyboardLayer? = null,
    val engine: InputEngine? = null,
    val modifiers: Set<ModifierKey> = emptySet(),
    val composing: Boolean? = null,
    val locale: String? = null,
)

@Serializable(with = ActionSerializer::class)
sealed interface Action {
    val type: ActionType

    @Serializable
    data class CommitText(val text: String) : Action {
        override val type: ActionType = ActionType.COMMIT_TEXT
    }

    @Serializable
    data object CommitComposing : Action {
        override val type: ActionType = ActionType.COMMIT_COMPOSING
    }

    @Serializable
    data class PushToken(val token: Token) : Action {
        override val type: ActionType = ActionType.PUSH_TOKEN
    }

    @Serializable
    data object ClearComposition : Action {
        override val type: ActionType = ActionType.CLEAR_COMPOSITION
    }

    @Serializable
    data object Back : Action {
        override val type: ActionType = ActionType.BACK
    }

    @Serializable
    data object ToggleLocale : Action {
        override val type: ActionType = ActionType.TOGGLE_LOCALE
    }

    @Serializable
    data class Command(val commandType: CommandType) : Action {
        override val type: ActionType = ActionType.COMMAND
    }

    @Serializable
    data class SwitchLayer(val layer: KeyboardLayer) : Action {
        override val type: ActionType = ActionType.SWITCH_LAYER
    }

    @Serializable
    data class SwitchLayout(val layoutId: String) : Action {
        override val type: ActionType = ActionType.SWITCH_LAYOUT
    }

    @Serializable
    data class SwitchEngine(val engine: InputEngine) : Action {
        override val type: ActionType = ActionType.SWITCH_ENGINE
    }

    @Serializable
    data class ToggleModifier(val modifier: ModifierKey) : Action {
        override val type: ActionType = ActionType.TOGGLE_MODIFIER
    }

    @Serializable
    data class OpenPopup(val popupId: String) : Action {
        override val type: ActionType = ActionType.OPEN_POPUP
    }

    @Serializable
    data object NoOp : Action {
        override val type: ActionType = ActionType.NO_OP
    }
}

@Serializable(with = TokenSerializer::class)
sealed interface Token {
    val type: TokenType

    @Serializable
    data class Literal(val text: String) : Token {
        override val type: TokenType = TokenType.LITERAL
    }

    @Serializable
    data class SymbolSet(val symbols: List<String>) : Token {
        override val type: TokenType = TokenType.SYMBOL_SET
    }

    @Serializable
    data class WeightedSet(val symbols: List<WeightedSymbol>) : Token {
        override val type: TokenType = TokenType.WEIGHTED_SET
    }

    @Serializable
    data class Sequence(val tokens: List<Token>) : Token {
        override val type: TokenType = TokenType.SEQUENCE
    }

    @Serializable
    data class Marker(val marker: String) : Token {
        override val type: TokenType = TokenType.MARKER
    }
}

@Serializable
data class WeightedSymbol(
    val ch: String,
    val weight: Double,
)

private object TokenSerializer : KSerializer<Token> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Token {
        val jsonDecoder = decoder as? JsonDecoder ?: error("TokenSerializer requires JsonDecoder")
        val element = jsonDecoder.decodeJsonElement()
        return decodeToken(element) ?: Token.Literal("")
    }

    override fun serialize(encoder: Encoder, value: Token) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("TokenSerializer requires JsonEncoder")
        jsonEncoder.encodeJsonElement(toJsonElement(value))
    }

    private fun decodeToken(element: JsonElement): Token? {
        return when (element) {
            is JsonPrimitive -> element.contentOrNull?.let { Token.Literal(it) }
            is JsonArray -> decodeTokenArray(element)
            is JsonObject -> decodeTokenObject(element)
            else -> null
        }
    }

    private fun decodeTokenArray(arr: JsonArray): Token? {
        if (arr.isEmpty()) return null

        val allStrings = arr.all { it is JsonPrimitive && it.isString }
        if (allStrings) {
            val symbols =
                arr.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.takeIf(String::isNotBlank) }
            return Token.SymbolSet(symbols)
        }

        val allWeighted =
            arr.all {
                val o = (it as? JsonObject) ?: return@all false
                o["ch"] is JsonPrimitive && o["weight"] is JsonPrimitive
            }
        if (allWeighted) {
            val symbols =
                arr.mapNotNull {
                    val o = it as? JsonObject ?: return@mapNotNull null
                    val ch = o["ch"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val weight = o["weight"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                        ?: return@mapNotNull null
                    WeightedSymbol(ch = ch, weight = weight)
                }
            return Token.WeightedSet(symbols)
        }

        return null
    }

    private fun decodeTokenObject(obj: JsonObject): Token? {
        val type = obj["type"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        return when (type) {
            TokenType.LITERAL.name -> obj["text"]?.jsonPrimitive?.contentOrNull?.let(Token::Literal)
            TokenType.SYMBOL_SET.name ->
                obj["symbols"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?.let(Token::SymbolSet)

            TokenType.WEIGHTED_SET.name -> {
                val list =
                    obj["symbols"]?.jsonArray?.mapNotNull { e ->
                        val o = e as? JsonObject ?: return@mapNotNull null
                        val ch = o["ch"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                        val weight = o["weight"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()
                            ?: return@mapNotNull null
                        WeightedSymbol(ch = ch, weight = weight)
                    }.orEmpty()
                Token.WeightedSet(list)
            }

            TokenType.SEQUENCE.name -> obj["tokens"]?.jsonArray?.mapNotNull(::decodeToken)
                ?.let(Token::Sequence)

            TokenType.MARKER.name -> obj["marker"]?.jsonPrimitive?.contentOrNull?.let(Token::Marker)
            else -> null
        }
    }

    fun toJsonElement(token: Token): JsonElement {
        return when (token) {
            is Token.Literal ->
                buildJsonObject {
                    put("type", JsonPrimitive(TokenType.LITERAL.name))
                    put("text", JsonPrimitive(token.text))
                }

            is Token.SymbolSet ->
                buildJsonObject {
                    put("type", JsonPrimitive(TokenType.SYMBOL_SET.name))
                    put(
                        "symbols",
                        buildJsonArray { token.symbols.forEach { add(JsonPrimitive(it)) } })
                }

            is Token.WeightedSet ->
                buildJsonObject {
                    put("type", JsonPrimitive(TokenType.WEIGHTED_SET.name))
                    put(
                        "symbols",
                        buildJsonArray {
                            token.symbols.forEach { sym ->
                                add(buildJsonObject {
                                    put("ch", JsonPrimitive(sym.ch))
                                    put("weight", JsonPrimitive(sym.weight))
                                })
                            }
                        },
                    )
                }

            is Token.Sequence ->
                buildJsonObject {
                    put("type", JsonPrimitive(TokenType.SEQUENCE.name))
                    put(
                        "tokens",
                        buildJsonArray { token.tokens.forEach { add(toJsonElement(it)) } })
                }

            is Token.Marker ->
                buildJsonObject {
                    put("type", JsonPrimitive(TokenType.MARKER.name))
                    put("marker", JsonPrimitive(token.marker))
                }
        }
    }
}

private object ActionSerializer : KSerializer<Action> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun deserialize(decoder: Decoder): Action {
        val jsonDecoder = decoder as? JsonDecoder ?: error("ActionSerializer requires JsonDecoder")
        val element = jsonDecoder.decodeJsonElement()
        val obj: JsonObject = element as? JsonObject ?: element.jsonObject

        val typeRaw = obj["type"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val type =
            runCatching { ActionType.valueOf(typeRaw) }.getOrNull()
                ?: error("Unknown action type=$typeRaw")

        return when (type) {
            ActionType.COMMIT_TEXT -> Action.CommitText(
                obj["text"]?.jsonPrimitive?.contentOrNull ?: ""
            )

            ActionType.COMMIT_COMPOSING -> Action.CommitComposing

            ActionType.PUSH_TOKEN -> {
                val tokenEl = obj["token"] ?: error("PUSH_TOKEN requires token")
                Action.PushToken(TokenSerializer.deserialize(JsonDecoderStub(jsonDecoder, tokenEl)))
            }

            ActionType.CLEAR_COMPOSITION -> Action.ClearComposition

            ActionType.BACK -> Action.Back
            ActionType.TOGGLE_LOCALE -> Action.ToggleLocale

            ActionType.COMMAND -> {
                val cmdRaw = obj["commandType"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val cmd = runCatching { CommandType.valueOf(cmdRaw) }.getOrNull()
                    ?: error("Unknown commandType=$cmdRaw")
                Action.Command(cmd)
            }

            ActionType.SWITCH_LAYER -> {
                val layerRaw = obj["layer"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val layer = runCatching { KeyboardLayer.valueOf(layerRaw) }.getOrNull()
                    ?: error("Unknown layer=$layerRaw")
                Action.SwitchLayer(layer)
            }

            ActionType.SWITCH_LAYOUT -> {
                val layoutId = obj["layoutId"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                if (layoutId.isBlank()) error("SWITCH_LAYOUT requires layoutId")
                Action.SwitchLayout(layoutId)
            }

            ActionType.SWITCH_ENGINE -> {
                val engineRaw = obj["engine"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val engine = runCatching { InputEngine.valueOf(engineRaw) }.getOrNull()
                    ?: error("Unknown engine=$engineRaw")
                Action.SwitchEngine(engine)
            }

            ActionType.TOGGLE_MODIFIER -> {
                val modRaw = obj["modifier"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
                val mod = runCatching { ModifierKey.valueOf(modRaw) }.getOrNull()
                    ?: error("Unknown modifier=$modRaw")
                Action.ToggleModifier(mod)
            }

            ActionType.OPEN_POPUP -> Action.OpenPopup(
                obj["popupId"]?.jsonPrimitive?.contentOrNull ?: ""
            )

            ActionType.REPLACE_TOKENS, ActionType.NO_OP -> Action.NoOp
        }
    }

    override fun serialize(encoder: Encoder, value: Action) {
        val jsonEncoder = encoder as? JsonEncoder ?: error("ActionSerializer requires JsonEncoder")
        val out =
            when (value) {
                is Action.CommitText ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.COMMIT_TEXT.name))
                        put("text", JsonPrimitive(value.text))
                    }

                Action.CommitComposing ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.COMMIT_COMPOSING.name))
                    }

                is Action.PushToken ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.PUSH_TOKEN.name))
                        put("token", TokenSerializer.toJsonElement(value.token))
                    }

                Action.ClearComposition ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.CLEAR_COMPOSITION.name))
                    }

                Action.Back ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.BACK.name))
                    }

                Action.ToggleLocale ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.TOGGLE_LOCALE.name))
                    }

                is Action.Command ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.COMMAND.name))
                        put("commandType", JsonPrimitive(value.commandType.name))
                    }

                is Action.SwitchLayer ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.SWITCH_LAYER.name))
                        put("layer", JsonPrimitive(value.layer.name))
                    }

                is Action.SwitchLayout ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.SWITCH_LAYOUT.name))
                        put("layoutId", JsonPrimitive(value.layoutId))
                    }

                is Action.SwitchEngine ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.SWITCH_ENGINE.name))
                        put("engine", JsonPrimitive(value.engine.name))
                    }

                is Action.ToggleModifier ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.TOGGLE_MODIFIER.name))
                        put("modifier", JsonPrimitive(value.modifier.name))
                    }

                is Action.OpenPopup ->
                    buildJsonObject {
                        put("type", JsonPrimitive(ActionType.OPEN_POPUP.name))
                        put("popupId", JsonPrimitive(value.popupId))
                    }

                Action.NoOp -> buildJsonObject { put("type", JsonPrimitive(ActionType.NO_OP.name)) }
            }
        jsonEncoder.encodeJsonElement(out)
    }
}

/**
 * Minimal JsonDecoder stub to reuse token deserialization against a nested element.
 */
private class JsonDecoderStub(
    private val parent: JsonDecoder,
    private val element: JsonElement,
) : JsonDecoder by parent {
    override fun decodeJsonElement(): JsonElement = element
}
