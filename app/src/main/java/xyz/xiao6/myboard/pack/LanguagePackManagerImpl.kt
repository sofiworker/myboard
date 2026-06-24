package xyz.xiao6.myboard.pack

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
import xyz.xiao6.myboard.engine.EngineRegistry
import xyz.xiao6.myboard.engine.DictionaryRegistry
import xyz.xiao6.myboard.layout.LayoutRegistry

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
