package xyz.xiao6.myboard.ai.llm

/**
 * LLM Provider 接口。
 */
interface LLMProvider {
    val id: String
    val type: LLMType

    suspend fun complete(request: LLMRequest, stream: (String) -> Unit): LLMResult
    fun isAvailable(): Boolean
    fun getCapabilities(): Set<LLMCapability>
}

enum class LLMType { LOCAL, CLOUD, MOCK }
enum class LLMCapability { AUTOCOMPLETE, TRANSLATION, ENHANCEMENT, SUGGESTION }

data class LLMRequest(
    val systemPrompt: String,
    val messages: List<Message>,
    val maxTokens: Int = 256,
    val temperature: Float = 0.7f,
    val capability: LLMCapability
)

data class Message(
    val role: String,
    val content: String
)

data class LLMResult(
    val text: String,
    val tokens: Int,
    val latencyMs: Long
)

/**
 * Mock LLM Provider。
 */
class MockLLMProvider : LLMProvider {
    override val id = "mock"
    override val type = LLMType.MOCK

    override suspend fun complete(request: LLMRequest, stream: (String) -> Unit): LLMResult {
        val response = when (request.capability) {
            LLMCapability.AUTOCOMPLETE -> "suggestion"
            LLMCapability.TRANSLATION -> "translated text"
            LLMCapability.ENHANCEMENT -> "enhanced text"
            LLMCapability.SUGGESTION -> "idea"
        }
        stream(response)
        return LLMResult(response, 0, 0)
    }

    override fun isAvailable() = true
    override fun getCapabilities() = LLMCapability.entries.toSet()
}
