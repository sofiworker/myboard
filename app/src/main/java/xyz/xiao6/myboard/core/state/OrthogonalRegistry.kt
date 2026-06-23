package xyz.xiao6.myboard.core.state

import xyz.xiao6.myboard.core.contract.*

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