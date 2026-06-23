package xyz.xiao6.myboard.core.androidbridge

import android.view.inputmethod.ExtractedText
import android.view.KeyEvent

/**
 * InputConnection 网关。
 * 所有 InputConnection 调用的唯一出口与失败处理。
 * 阶段 01 只定义接口，阶段 06 实现真实逻辑。
 */
interface InputConnectionGateway {
    fun commitText(text: String): Boolean
    fun setComposingText(text: String): Boolean
    fun finishComposingText(): Boolean
    fun deleteSurroundingText(before: Int, after: Int): Boolean
    fun setSelection(start: Int, end: Int): Boolean
    fun performEditorAction(action: Int): Boolean
    fun getExtractedText(): ExtractedText?
    fun sendKeyEvent(keyEvent: KeyEvent): Boolean
    fun finishAndCommit(commit: String): Boolean
}