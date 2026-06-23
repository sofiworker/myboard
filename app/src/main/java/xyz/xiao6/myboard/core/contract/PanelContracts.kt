package xyz.xiao6.myboard.core.contract

/**
 * 面板管理器接口。
 */
interface PanelManager {
    val currentPanel: PanelType
    fun openPanel(panelType: PanelType)
    fun closePanel()
}

/**
 * STT（语音转文字）桥接器接口。
 */
interface SttBridge {
    val isListening: Boolean
    fun startListening()
    fun stopListening()
    fun cancel()
}

/**
 * LLM 桥接器接口。
 */
interface LlmBridge {
    suspend fun complete(prompt: String): String?
    suspend fun suggestNextWords(context: String, count: Int): List<String>
    suspend fun correctText(text: String): String?
}
