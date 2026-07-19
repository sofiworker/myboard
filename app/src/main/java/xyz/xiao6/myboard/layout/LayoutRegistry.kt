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
 * 布局注册表。
 * 阶段 01 使用 StubLayoutRegistry，阶段 04 替换真实实现。
 */
interface LayoutRegistry {
    fun register(doc: LayoutDoc, source: LayoutSource): RegisterResult
    fun register(key: LayoutKey, doc: LayoutDoc, source: LayoutSource): RegisterResult
    fun unregister(layoutId: String)
    fun unregister(key: LayoutKey)
    fun get(layoutId: String): LayoutDoc?
    fun get(key: LayoutKey): LayoutDoc?
    fun activeVersion(packageId: String, layoutId: String): SemVer?
    fun resolve(packageId: String, layoutId: String, packageVersion: SemVer): LayoutKey
    fun validate(doc: LayoutDoc): List<LayoutIssue>
    fun findBuiltIn(id: String): LayoutDoc?
}
