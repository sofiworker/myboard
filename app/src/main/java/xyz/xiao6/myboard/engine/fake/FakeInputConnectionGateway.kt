package xyz.xiao6.myboard.engine.fake

import android.view.inputmethod.ExtractedText
import android.view.KeyEvent
import xyz.xiao6.myboard.androidbridge.InputConnectionGateway

/**
 * Fake InputConnectionGateway - 保留供测试使用
 * 
 * 记录调用，可配置返回值，用于阶段 05 测试。
 */
class FakeInputConnectionGateway : InputConnectionGateway {
    data class CallRecord(
        val method: String,
        val args: Map<String, Any?>
    )
    
    val calls = mutableListOf<CallRecord>()
    var commitTextResult: Boolean = true
    var setComposingTextResult: Boolean = true
    var finishComposingTextResult: Boolean = true
    var deleteSurroundingTextResult: Boolean = true
    var setSelectionResult: Boolean = true
    var performEditorActionResult: Boolean = true
    var extractedTextResult: ExtractedText? = null
    var sendKeyEventResult: Boolean = true
    
    override fun commitText(text: String): Boolean {
        calls.add(CallRecord("commitText", mapOf("text" to text)))
        return commitTextResult
    }
    
    override fun setComposingText(text: String): Boolean {
        calls.add(CallRecord("setComposingText", mapOf("text" to text)))
        return setComposingTextResult
    }
    
    override fun finishComposingText(): Boolean {
        calls.add(CallRecord("finishComposingText", emptyMap()))
        return finishComposingTextResult
    }
    
    override fun deleteSurroundingText(before: Int, after: Int): Boolean {
        calls.add(CallRecord("deleteSurroundingText", mapOf("before" to before, "after" to after)))
        return deleteSurroundingTextResult
    }
    
    override fun setSelection(start: Int, end: Int): Boolean {
        calls.add(CallRecord("setSelection", mapOf("start" to start, "end" to end)))
        return setSelectionResult
    }
    
    override fun performEditorAction(action: Int): Boolean {
        calls.add(CallRecord("performEditorAction", mapOf("action" to action)))
        return performEditorActionResult
    }
    
    override fun getExtractedText(): ExtractedText? {
        calls.add(CallRecord("getExtractedText", emptyMap()))
        return extractedTextResult
    }
    
    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        calls.add(CallRecord("sendKeyEvent", mapOf("keyCode" to keyEvent.keyCode)))
        return sendKeyEventResult
    }
    
    override fun finishAndCommit(commit: String): Boolean {
        calls.add(CallRecord("finishAndCommit", mapOf("commit" to commit)))
        if (!finishComposingTextResult) return false
        return commitTextResult
    }
    
    fun clear() {
        calls.clear()
    }
}
