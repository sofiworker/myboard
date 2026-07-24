package xyz.xiao6.myboard.pack

import java.util.concurrent.atomic.AtomicReference
import xyz.xiao6.myboard.contract.state.OrthogonalState

fun interface ActiveOrthogonalStateSource {
    fun currentState(): OrthogonalState?
}

object ProcessActiveOrthogonalStateSource : ActiveOrthogonalStateSource {
    private val current = AtomicReference<OrthogonalState?>(null)

    override fun currentState(): OrthogonalState? = current.get()

    fun update(state: OrthogonalState?) {
        current.set(state)
    }
}
