package xyz.xiao6.myboard.core.engine

import xyz.xiao6.myboard.core.contract.*

/**
 * 编码器注册表真实实现。
 */
class EncoderRegistryImpl : EncoderRegistry {
    
    private val encoders = mutableMapOf<String, Encoder>()
    
    override fun register(encoder: Encoder) {
        encoders[encoder.encoderId] = encoder
    }
    
    override fun get(encoderId: String): Encoder? {
        return encoders[encoderId]
    }
}
