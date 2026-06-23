package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

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