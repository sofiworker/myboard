package xyz.xiao6.myboard.layout

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
 * 布局提示解析器真实实现。
 * 根据 LayoutHint 返回布局 ID。
 */
class LayoutHintResolverImpl : LayoutHintResolver {
    
    override fun resolve(hint: LayoutHint, currentLayoutId: String): String {
        return when (hint) {
            LayoutHint.ALPHA -> currentLayoutId
            LayoutHint.NUMBER -> "number"
            LayoutHint.PHONE -> "phone"
            LayoutHint.DATETIME -> "datetime"
            LayoutHint.URL -> currentLayoutId
            LayoutHint.EMAIL -> currentLayoutId
            LayoutHint.PASSWORD -> currentLayoutId
        }
    }
}