package xyz.xiao6.myboard.core.layout

import xyz.xiao6.myboard.core.contract.*

/**
 * 布局注册表。
 * 阶段 01 使用 StubLayoutRegistry，阶段 04 替换真实实现。
 */
interface LayoutRegistry {
    fun register(doc: LayoutDoc, source: LayoutSource): RegisterResult
    fun unregister(layoutId: String)
    fun get(layoutId: String): LayoutDoc?
    fun validate(doc: LayoutDoc): List<LayoutIssue>
    fun findBuiltIn(id: String): LayoutDoc?
}