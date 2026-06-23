package xyz.xiao6.myboard.core.androidbridge

import xyz.xiao6.myboard.core.contract.SelectionSnapshot
import xyz.xiao6.myboard.core.contract.SelectionDecision

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