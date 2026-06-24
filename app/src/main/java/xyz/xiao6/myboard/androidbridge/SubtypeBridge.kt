package xyz.xiao6.myboard.androidbridge

import android.view.inputmethod.InputMethodSubtype
import xyz.xiao6.myboard.contract.state.OrthogonalState

/**
 * Subtype 桥接器。
 * 阶段 01 只定义接口，阶段 07 实现真实逻辑。
 */
interface SubtypeBridge {
    fun onCurrentSubtypeChanged(subtype: InputMethodSubtype)
    fun syncOutbound(state: OrthogonalState)
    fun switchToNext()
}