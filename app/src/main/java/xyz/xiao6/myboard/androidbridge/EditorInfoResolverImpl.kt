package xyz.xiao6.myboard.androidbridge

import android.view.inputmethod.EditorInfo
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
 * EditorInfo 解析器真实实现。
 */
class EditorInfoResolverImpl : EditorInfoResolver {
    
    override fun resolve(editorInfo: EditorInfo?, currentLocale: LocaleTag): EditorProfile {
        val inputType = editorInfo?.inputType ?: 0
        val imeOptions = editorInfo?.imeOptions ?: 0
        
        val layoutHint = when (inputType and android.text.InputType.TYPE_MASK_CLASS) {
            android.text.InputType.TYPE_CLASS_NUMBER -> LayoutHint.NUMBER
            android.text.InputType.TYPE_CLASS_PHONE -> LayoutHint.PHONE
            android.text.InputType.TYPE_CLASS_DATETIME -> LayoutHint.DATETIME
            else -> when (inputType and android.text.InputType.TYPE_MASK_VARIATION) {
                android.text.InputType.TYPE_TEXT_VARIATION_URI -> LayoutHint.URL
                android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS -> LayoutHint.EMAIL
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD -> LayoutHint.PASSWORD
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS -> LayoutHint.EMAIL
                android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD -> LayoutHint.PASSWORD
                else -> LayoutHint.ALPHA
            }
        }
        
        val enterAction = when (imeOptions and android.view.inputmethod.EditorInfo.IME_MASK_ACTION) {
            android.view.inputmethod.EditorInfo.IME_ACTION_DONE -> EnterAction.DONE
            android.view.inputmethod.EditorInfo.IME_ACTION_GO -> EnterAction.GO
            android.view.inputmethod.EditorInfo.IME_ACTION_NEXT -> EnterAction.NEXT
            android.view.inputmethod.EditorInfo.IME_ACTION_PREVIOUS -> EnterAction.PREVIOUS
            android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH -> EnterAction.SEARCH
            android.view.inputmethod.EditorInfo.IME_ACTION_SEND -> EnterAction.SEND
            android.view.inputmethod.EditorInfo.IME_ACTION_NONE -> EnterAction.NONE
            else -> EnterAction.UNSPECIFIED
        }
        
        val enterLabel = editorInfo?.actionLabel?.toString()
        val candidateDisabled = (inputType and android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
        val composingDisabled = candidateDisabled
        
        return EditorProfile(
            layoutHint = layoutHint,
            enterAction = enterAction,
            enterLabel = enterLabel,
            candidateDisabled = candidateDisabled,
            composingDisabled = composingDisabled,
            rawInputType = inputType,
            rawImeOptions = imeOptions
        )
    }
}
