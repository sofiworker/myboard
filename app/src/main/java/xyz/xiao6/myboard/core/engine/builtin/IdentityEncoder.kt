package xyz.xiao6.myboard.core.engine.builtin

import xyz.xiao6.myboard.core.contract.*

/**
 * Identity 编码器（全拼）。
 * 直接将 token 拼接到 buffer，不做转换。
 */
class IdentityEncoder : Encoder {
    override val encoderId: String = "identity"
    
    override fun append(state: EncodingState, token: String): EncodingState {
        val newQuery = state.queryBuffer + token
        return state.copy(
            rawBuffer = state.rawBuffer + token,
            queryBuffer = newQuery,
            displayText = newQuery
        )
    }
    
    override fun backspace(state: EncodingState): EncodingState {
        if (state.queryBuffer.isEmpty()) return state
        val newQuery = state.queryBuffer.dropLast(1)
        val newRaw = state.rawBuffer.dropLast(1)
        return state.copy(
            rawBuffer = newRaw,
            queryBuffer = newQuery,
            displayText = newQuery
        )
    }
}
