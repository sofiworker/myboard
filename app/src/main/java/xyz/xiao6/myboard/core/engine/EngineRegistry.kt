package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 引擎注册表。
 * 阶段 01 使用 StubEngineRegistry，阶段 05 替换真实实现。
 */
interface EngineRegistry {
    fun register(engine: InputEngine)
    fun get(engineId: String): InputEngine?
}