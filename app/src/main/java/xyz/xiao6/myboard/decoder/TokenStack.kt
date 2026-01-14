package xyz.xiao6.myboard.decoder

import xyz.xiao6.myboard.model.Token
import xyz.xiao6.myboard.model.WeightedSymbol

/**
 * A stack-based container for storing token sequences.
 * Uses LIFO (Last In, First Out) semantics with push/pop operations.
 */
class TokenStack {
    private val stack: ArrayDeque<Token> = ArrayDeque()

    /**
     * Push a token onto the stack.
     */
    fun push(token: Token) {
        stack.addLast(token)
    }

    /**
     * Remove and return the top token from the stack.
     * @return The removed token, or null if stack is empty.
     */
    fun pop(): Token? {
        return stack.removeLastOrNull()
    }

    /**
     * Peek at the top token without removing it.
     * @return The top token, or null if stack is empty.
     */
    fun peek(): Token? {
        return stack.lastOrNull()
    }

    /**
     * Remove all tokens from the stack.
     */
    fun clear() {
        stack.clear()
    }

    /**
     * Check if the stack is empty.
     */
    fun isEmpty(): Boolean = stack.isEmpty()

    /**
     * Get the number of tokens in the stack.
     */
    fun size(): Int = stack.size

    /**
     * Get an immutable view of the current stack as a list.
     * The list is ordered from bottom (oldest) to top (newest).
     */
    fun toList(): List<Token> = stack.toList()

    /**
     * Get an immutable view of the current stack as a list in reverse order.
     * The list is ordered from top (newest) to bottom (oldest).
     */
    fun toReversedList(): List<Token> = stack.reversed()

    /**
     * Extract letter options from all tokens in the stack.
     * Returns a list where each element represents the possible letters for that position.
     */
    fun extractLetterOptions(): List<List<String>> {
        return stack.map { token ->
            when (token) {
                is Token.Literal -> {
                    // For literal tokens, split into individual letters
                    token.text.lowercase().filter { it.isLetter() }.map { it.toString() }
                }
                is Token.SymbolSet -> {
                    token.symbols.map { it.lowercase() }.distinct()
                }
                is Token.WeightedSet -> {
                    token.symbols.sortedByDescending { it.weight }.map { it.ch.lowercase() }.distinct()
                }
                is Token.Marker -> {
                    listOf(token.marker)
                }
                is Token.Sequence -> {
                    // Flatten sequence tokens and extract letters
                    token.tokens.flatMap { seqToken ->
                        when (seqToken) {
                            is Token.Literal -> seqToken.text.lowercase().filter { it.isLetter() }.map { it.toString() }
                            is Token.SymbolSet -> seqToken.symbols.map { it.lowercase() }
                            is Token.WeightedSet -> seqToken.symbols.map { it.ch.lowercase() }
                            else -> emptyList()
                        }
                    }.distinct()
                }
            }
        }
    }

    override fun toString(): String {
        return "TokenStack(size=${stack.size}, tokens=$stack)"
    }
}
