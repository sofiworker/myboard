package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 编码器注册表。
 * 阶段 01 使用 StubEncoderRegistry，阶段 05 替换真实实现。
 */
interface EncoderRegistry {
    fun register(encoder: Encoder)
    fun get(encoderId: String): Encoder?
}