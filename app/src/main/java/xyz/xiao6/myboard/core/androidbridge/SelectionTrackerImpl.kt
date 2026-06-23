package xyz.xiao6.myboard.core.androidbridge

import xyz.xiao6.myboard.core.contract.*

/**
 * 选择跟踪器真实实现。
 */
class SelectionTrackerImpl : SelectionTracker {
    
    override fun onSelectionChanged(
        oldSel: SelectionSnapshot,
        newSel: SelectionSnapshot,
        composingActive: Boolean
    ): SelectionDecision {
        // 如果光标跳跃式移动 > 1 个字符，认为是外部干预
        val delta = kotlin.math.abs(newSel.start - oldSel.start)
        
        if (composingActive && delta > 1) {
            return SelectionDecision.MustReset(ResetReason.CursorMoved)
        }
        
        return SelectionDecision.Trusted
    }
}
