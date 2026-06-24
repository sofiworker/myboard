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
