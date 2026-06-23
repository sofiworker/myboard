package xyz.xiao6.myboard.core.panel

import xyz.xiao6.myboard.core.contract.*

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
