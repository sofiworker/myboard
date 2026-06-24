package xyz.xiao6.myboard.androidbridge

import xyz.xiao6.myboard.contract.input.*
import xyz.xiao6.myboard.contract.layout.*
import xyz.xiao6.myboard.contract.manifest.*
import xyz.xiao6.myboard.contract.theme.*
import xyz.xiao6.myboard.contract.engine.*
import xyz.xiao6.myboard.contract.bridge.*
import xyz.xiao6.myboard.contract.registry.*
import xyz.xiao6.myboard.contract.panel.*
import xyz.xiao6.myboard.contract.language.*
import xyz.xiao6.myboard.contract.state.*

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
