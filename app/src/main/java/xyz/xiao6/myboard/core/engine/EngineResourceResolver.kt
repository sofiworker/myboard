package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 引擎资源解析器。
 * 阶段 01 使用 StubEngineResourceResolver，阶段 05 替换真实实现。
 */
interface EngineResourceResolver {
    fun resolve(capability: SchemaCapability, packageId: String): EngineResources
}