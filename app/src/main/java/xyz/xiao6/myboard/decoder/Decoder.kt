package xyz.xiao6.myboard.decoder

/**
 * Converts layout outputs (committed key texts) into dictionary search keys and candidates.
 */
interface Decoder {
    fun onText(text: String): DecodeUpdate
    fun onCandidateSelected(text: String): DecodeUpdate
    fun reset(): DecodeUpdate = DecodeUpdate(composingText = "")
}

data class DecodeUpdate(
    val commitTexts: List<String> = emptyList(),
    val candidates: List<String> = emptyList(),
    /**
     * Current composing text (canonical search key).
     * Empty means "not composing"; null means "decoder does not report composing state".
     */
    val composingText: String? = null,
)

fun interface DictionaryLookup {
    fun candidates(searchKey: String, limit: Int): List<String>
}
