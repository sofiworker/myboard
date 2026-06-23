package xyz.xiao6.myboard.core.pack

import xyz.xiao6.myboard.core.contract.*
import xyz.xiao6.myboard.core.engine.EngineRegistry
import xyz.xiao6.myboard.core.engine.DictionaryRegistry
import xyz.xiao6.myboard.core.layout.LayoutRegistry

/**
 * 语言包管理器真实实现。
 */
class LanguagePackManagerImpl(
    private val engineRegistry: EngineRegistry,
    private val dictionaryRegistry: DictionaryRegistry,
    private val layoutRegistry: LayoutRegistry
) : LanguagePackManager {
    
    private val packs = mutableMapOf<String, LanguagePackInfo>()
    
    override fun listInstalled(): List<LanguagePackInfo> {
        return packs.values.toList()
    }
    
    override fun install(pack: LanguagePackInfo): Boolean {
        packs[pack.packageId] = pack
        return true
    }
    
    override fun uninstall(packageId: String): Boolean {
        return packs.remove(packageId) != null
    }
    
    override fun get(packageId: String): LanguagePackInfo? {
        return packs[packageId]
    }
}
