package xyz.xiao6.myboard.core.ai.stt

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * STT Provider 接口。
 */
interface STTProvider {
    val id: String
    val name: String

    fun startListening(config: STTConfig): Flow<STTEvent>
    fun stopListening()
    fun cancel()
    fun isAvailable(): Boolean
    fun getSupportedLanguages(): List<String>
}

data class STTConfig(
    val language: String,
    val enablePunctuation: Boolean = true,
    val maxDuration: Duration = 30.seconds
)

sealed interface STTEvent {
    data object Listening : STTEvent
    data class PartialResult(val text: String, val confidence: Float) : STTEvent
    data class FinalResult(val text: String) : STTEvent
    data class Error(val cause: Throwable) : STTEvent
}

data class Segment(
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float
)

/**
 * 端侧 STT Provider（待接入）。
 */
class OnDeviceSTTProvider : STTProvider {
    override val id = "on_device"
    override val name = "On-Device STT"

    override fun startListening(config: STTConfig): Flow<STTEvent> = flow {
        emit(STTEvent.Error(UnsupportedOperationException("On-device STT not yet implemented")))
    }

    override fun stopListening() {}
    override fun cancel() {}
    override fun isAvailable() = false
    override fun getSupportedLanguages(): List<String> = emptyList()
}
