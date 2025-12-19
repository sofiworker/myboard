package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.model.Token

/**
 * Optional decoder capability: accept structured [Token]s from layout actions.
 *
 * Decoders that don't implement this will still receive plain text via [Decoder.onText].
 */
interface TokenDecoder {
    fun onToken(token: Token): DecodeUpdate
}
