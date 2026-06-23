package xyz.xiao6.myboard.core.androidbridge

import android.view.inputmethod.EditorInfo
import xyz.xiao6.myboard.core.contract.EditorProfile
import xyz.xiao6.myboard.core.contract.LocaleTag

/**
 * EditorInfo 解析器。
 * 阶段 01 只定义接口，阶段 06 实现真实逻辑。
 */
interface EditorInfoResolver {
    fun resolve(editorInfo: EditorInfo?, currentLocale: LocaleTag): EditorProfile
}