package xyz.xiao6.myboard.decoder

/**
 * No-op decoder: directly commits incoming text and never provides candidates.
 */
object PassthroughDecoder : Decoder {
    override fun onText(text: String): DecodeUpdate = DecodeUpdate(commitTexts = listOf(text), composingText = "")
    override fun onCandidateSelected(text: String): DecodeUpdate = DecodeUpdate(commitTexts = listOf(text), composingText = "")
}
