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
