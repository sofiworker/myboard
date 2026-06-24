package xyz.xiao6.myboard.engine

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
 * 字典注册表真实实现。
 * 管理 Dictionary 实例的加载和缓存。
 */
class DictionaryRegistryImpl : DictionaryRegistry {
    
    private val dictionaries = mutableMapOf<DictionaryKey, Dictionary>()
    private val cache = mutableMapOf<DictionaryKey, Dictionary>()
    
    fun register(key: DictionaryKey, dictionary: Dictionary) {
        dictionaries[key] = dictionary
    }
    
    override fun load(key: DictionaryKey): Dictionary? {
        // 先查缓存
        cache[key]?.let { return it }
        
        // 再查注册表
        val dict = dictionaries[key]
        if (dict != null) {
            cache[key] = dict
            return dict
        }
        
        return null
    }
    
    override fun invalidate(key: DictionaryKey) {
        cache.remove(key)
    }
    
    fun clearCache() {
        cache.clear()
    }
}
