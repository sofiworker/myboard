package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

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
