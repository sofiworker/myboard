package xyz.xiao6.myboard.layout

import xyz.xiao6.myboard.contract.layout.Bindings
import xyz.xiao6.myboard.contract.state.KeyboardContext

/**
 * Evaluates optional container visibility/enabled bindings against KeyboardContext.
 */
interface BindingsEvaluator {
    fun evaluate(bindings: Bindings?, context: KeyboardContext): Pair<Boolean, Boolean>
}
