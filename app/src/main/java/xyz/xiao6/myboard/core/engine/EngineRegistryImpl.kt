package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 引擎注册表真实实现。
 */
class EngineRegistryImpl : EngineRegistry {
    
    private val engines = mutableMapOf<String, InputEngine>()
    
    override fun register(engine: InputEngine) {
        engines[engine.engineId] = engine
    }
    
    override fun get(engineId: String): InputEngine? {
        return engines[engineId]
    }
}
