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
 * STT（语音转文字）桥接器真实实现。
 * 阶段 08 简化版：占位实现。
 */
class SttBridgeImpl : SttBridge {
    
    private var _isListening = false
    override val isListening: Boolean get() = _isListening
    
    override fun startListening() {
        _isListening = true
        // TODO: 集成 Android SpeechRecognizer
    }
    
    override fun stopListening() {
        _isListening = false
    }
    
    override fun cancel() {
        _isListening = false
    }
}
