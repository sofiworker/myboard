package xyz.xiao6.myboard.core.androidbridge

import android.view.inputmethod.ExtractedText
import android.view.inputmethod.InputConnection
import android.view.KeyEvent

/**
 * InputConnection 网关真实实现。
 * 阶段 06：替换接口定义的占位实现。
 */
class InputConnectionGatewayImpl : InputConnectionGateway {
    
    private var connection: InputConnection? = null
    
    /** 更新当前 InputConnection */
    fun update(ic: InputConnection?) {
        // 先 finish composing，避免 composing 状态跨连接
        if (connection != null && connection != ic) {
            connection?.finishComposingText()
        }
        connection = ic
    }
    
    override fun commitText(text: String): Boolean {
        return connection?.commitText(text, 1) ?: false
    }
    
    override fun setComposingText(text: String): Boolean {
        return connection?.setComposingText(text, 1) ?: false
    }
    
    override fun finishComposingText(): Boolean {
        return connection?.finishComposingText() ?: false
    }
    
    override fun deleteSurroundingText(before: Int, after: Int): Boolean {
        return connection?.deleteSurroundingText(before, after) ?: false
    }
    
    override fun setSelection(start: Int, end: Int): Boolean {
        return connection?.setSelection(start, end) ?: false
    }
    
    override fun performEditorAction(action: Int): Boolean {
        return connection?.performEditorAction(action) ?: false
    }
    
    override fun getExtractedText(): ExtractedText? {
        return connection?.getExtractedText(null, 0)
    }
    
    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        return connection?.sendKeyEvent(keyEvent) ?: false
    }
    
    override fun finishAndCommit(commit: String): Boolean {
        connection?.finishComposingText()
        return connection?.commitText(commit, 1) ?: false
    }
}
