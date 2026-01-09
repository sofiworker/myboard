package xyz.xiao6.myboard.decoder

/**
 * Decoder for swype/gesture typing input
 * 滑行输入解码器，将滑过的按键序列转换为候选词
 */
class SwypeDecoder : Decoder {
    private val logTag = "SwypeDecoder"

    private var currentKeySequence = mutableListOf<String>()

    override fun onText(text: String): DecodeUpdate {
        // For swype input, text input is handled via setKeySequence
        // This method is called when text is committed via other means
        return DecodeUpdate(commitTexts = listOf(text))
    }

    override fun onCandidateSelected(text: String): DecodeUpdate {
        currentKeySequence.clear()
        return DecodeUpdate(commitTexts = listOf(text))
    }

    override fun reset(): DecodeUpdate {
        currentKeySequence.clear()
        return DecodeUpdate(composingText = "")
    }

    /**
     * Set the complete key sequence from a swype gesture
     */
    fun setKeySequence(keys: List<String>): DecodeUpdate {
        currentKeySequence.clear()
        currentKeySequence.addAll(keys)

        // Build a pseudo-word from the key sequence
        // In a full implementation, this would query a dictionary with the swype path
        val word = keys.joinToString("").lowercase()

        return DecodeUpdate(
            candidates = listOf(word),
            composingText = word,
        )
    }

    /**
     * Get the current key sequence
     */
    fun getKeySequence(): List<String> = currentKeySequence.toList()

    /**
     * Check if currently composing
     */
    fun isComposing(): Boolean = currentKeySequence.isNotEmpty()
}
