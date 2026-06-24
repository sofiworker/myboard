package xyz.xiao6.myboard.state

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
 * 正交能力注册表。
 * 负责加载、校验、合并 Manifest。
 * 阶段 02 实现真实逻辑。
 */
interface OrthogonalRegistry {
    fun register(manifest: LanguageManifest): RegisterResult
    fun unregister(packageId: String): RegisterResult
    fun getLocale(locale: LocaleTag): LocaleCapability?
    fun isSupported(state: OrthogonalState): Boolean
    fun defaultState(locale: LocaleTag): OrthogonalState?
    fun defaultSchema(locale: LocaleTag, script: Script): Schema?
    fun schemaCapability(state: OrthogonalState): SchemaCapability?
}