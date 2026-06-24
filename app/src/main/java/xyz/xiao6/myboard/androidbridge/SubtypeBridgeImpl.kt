package xyz.xiao6.myboard.androidbridge

import android.view.inputmethod.InputMethodSubtype
import xyz.xiao6.myboard.contract.state.OrthogonalState

/**
 * Subtype 桥接器真实实现。
 */
class SubtypeBridgeImpl : SubtypeBridge {
    
    private var currentState: OrthogonalState? = null
    
    override fun onCurrentSubtypeChanged(subtype: InputMethodSubtype) {
        // Android 平台 subtype 变化时回调
        // 将 InputMethodSubtype 转为内部 OrthogonalState 变更
    }
    
    override fun syncOutbound(state: OrthogonalState) {
        currentState = state
    }
    
    override fun switchToNext() {
        // TODO: 通知 InputMethodManager 切换到下一个 subtype
    }
}
