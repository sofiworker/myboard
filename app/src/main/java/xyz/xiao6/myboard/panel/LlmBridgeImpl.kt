package xyz.xiao6.myboard.panel

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
 * LLM 桥接器真实实现。
 * 阶段 08 简化版：占位实现。
 */
class LlmBridgeImpl : LlmBridge {
    
    override suspend fun complete(prompt: String): String? {
        // TODO: 集成 LLM API
        return null
    }
    
    override suspend fun suggestNextWords(context: String, count: Int): List<String> {
        // TODO: 集成 LLM 预测
        return emptyList()
    }
    
    override suspend fun correctText(text: String): String? {
        // TODO: 集成 LLM 纠错
        return null
    }
}
