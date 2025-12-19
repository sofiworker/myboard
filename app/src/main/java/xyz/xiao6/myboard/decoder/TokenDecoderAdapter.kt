package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.model.Token

/**
 * Adapter for decoders that don't implement [TokenDecoder]:
 * converts [Token] into plain text and forwards to [Decoder.onText].
 *
 * Policy (deterministic, minimal):
 * - Literal -> text
 * - Marker -> marker string
 * - Sequence -> apply each token sequentially
 * - SymbolSet -> first non-blank symbol (if any)
 * - WeightedSet -> highest-weight non-blank symbol (if any)
 */
class TokenDecoderAdapter(
    private val decoder: Decoder,
) : TokenDecoder {

    override fun onToken(token: Token): DecodeUpdate {
        return when (token) {
            is Token.Literal -> decoder.onText(token.text)
            is Token.Marker -> decoder.onText(token.marker)
            is Token.Sequence -> applySequence(token.tokens)
            is Token.SymbolSet -> decoder.onText(firstNonBlank(token.symbols) ?: "")
            is Token.WeightedSet -> decoder.onText(bestWeighted(token.symbols) ?: "")
        }
    }

    private fun applySequence(tokens: List<Token>): DecodeUpdate {
        var last = DecodeUpdate()
        val commits = ArrayList<String>()
        for (t in tokens) {
            last = onToken(t)
            if (last.commitTexts.isNotEmpty()) commits.addAll(last.commitTexts)
        }
        return last.copy(commitTexts = commits)
    }

    private fun firstNonBlank(symbols: List<String>): String? {
        return symbols.firstOrNull { it.isNotBlank() }
    }

    private fun bestWeighted(symbols: List<xyz.xiao6.myboard.model.WeightedSymbol>): String? {
        return symbols
            .filter { it.ch.isNotBlank() }
            .maxByOrNull { it.weight }
            ?.ch
    }
}

