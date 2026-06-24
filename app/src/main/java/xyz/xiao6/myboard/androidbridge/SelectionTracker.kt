package xyz.xiao6.myboard.androidbridge

import xyz.xiao6.myboard.contract.bridge.SelectionSnapshot
import xyz.xiao6.myboard.contract.bridge.SelectionDecision

/**
 * 光标选择追踪器。
 * 阶段 01 只定义接口，阶段 06 实现真实逻辑。
 */
interface SelectionTracker {
    fun onSelectionChanged(
        oldSel: SelectionSnapshot,
        newSel: SelectionSnapshot,
        composingActive: Boolean
    ): SelectionDecision
}