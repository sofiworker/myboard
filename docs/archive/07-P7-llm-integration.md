# P7: LLM 集成 (2 周)

## 1. 目标

接入 LLM（本地 ONNX / 云端 API），支持自动补全、翻译、语句美化、智能联想。

## 2. 里程碑验收标准

- [x] 本地 LLM 可推理
- [x] 云端 LLM 可调用
- [x] 自动补全可用
- [x] 翻译功能可用
- [x] 语句美化可用
- [x] 智能联想可在候选栏展示

## 3. 详细设计

### 3.1 Provider 接口

```kotlin
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

data class LLMResult(
    val text: String,
    val tokens: Int,
    val latencyMs: Long
)
```

### 3.2 本地 LLM

```kotlin
class LocalLLMProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LLMProvider {
    override val id = "local"
    override val type = LLMType.LOCAL

    private var session: OrtSession? = null
    private var tokenizer: Tokenizer? = null

    suspend fun loadModel(modelPath: String) = withContext(Dispatchers.IO) {
        val env = OrtEnvironment.getEnvironment()
        session = env.createSession(modelPath)
        tokenizer = Tokenizer.fromFile("$modelPath/tokenizer.json")
    }

    override suspend fun complete(
        request: LLMRequest,
        stream: (String) -> Unit
    ): LLMResult = withContext(Dispatchers.Default) {
        val prompt = buildPrompt(request)
        val tokens = tokenizer!!.encode(prompt)
        val output = StringBuilder()
        // ONNX 推理逻辑
        LLMResult(output.toString(), tokens.size, 0L)
    }

    override fun isAvailable() = session != null
    override fun getCapabilities() = setOf(LLMCapability.AUTOCOMPLETE, LLMCapability.SUGGESTION)
}
```

### 3.3 云端 LLM

```kotlin
class CloudLLMProvider @Inject constructor(
    private val httpClient: OkHttpClient,
    private val apiKeyManager: APIKeyManager
) : LLMProvider {
    override val id = "cloud"
    override val type = LLMType.CLOUD

    override suspend fun complete(
        request: LLMRequest,
        stream: (String) -> Unit
    ): LLMResult = withContext(Dispatchers.IO) {
        val apiKey = apiKeyManager.getKey()
        val body = buildRequestBody(request)

        val httpRequest = Request.Builder()
            .url("https://api.example.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        val response = httpClient.newCall(httpRequest).execute()
        // SSE 流式解析
        LLMResult("", 0, 0L)
    }

    override fun isAvailable() = apiKeyManager.hasKey()
    override fun getCapabilities() = setOf(
        LLMCapability.AUTOCOMPLETE,
        LLMCapability.TRANSLATION,
        LLMCapability.ENHANCEMENT,
        LLMCapability.SUGGESTION
    )
}
```

### 3.4 LLM 功能

| 功能 | 触发方式 | Prompt 模板 |
|------|---------|-------------|
| **自动补全** | 长按空格 | "Complete: ${context}" |
| **中英翻译** | 工具栏按钮 | "Translate to ${target}: ${text}" |
| **语句美化** | 工具栏按钮 | "Enhance: ${text}" |
| **智能联想** | 候选栏 | "Suggest for: ${prefix}" |

### 3.5 LLM 设置页面

```kotlin
@Composable
fun LLMSettingsScreen() {
    val viewModel: LLMSettingsViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            SectionHeader(text = "LLM Provider")
            SingleChoice(
                selected = state.selectedProvider,
                options = listOf("Local", "Cloud", "Disabled"),
                onSelect = { viewModel.setProvider(it) }
            )
        }

        if (state.selectedProvider == "Cloud") {
            item {
                SectionHeader(text = "API Configuration")
                OutlinedTextField(
                    value = state.apiKey,
                    onValueChange = { viewModel.setApiKey(it) },
                    label = { Text("API Key") }
                )
                OutlinedTextField(
                    value = state.apiEndpoint,
                    onValueChange = { viewModel.setApiEndpoint(it) },
                    label = { Text("API Endpoint") }
                )
            }
        }

        item {
            SectionHeader(text = "Features")
            SwitchRow("Auto-complete", state.autoComplete) { viewModel.toggleAutoComplete() }
            SwitchRow("Translation", state.translation) { viewModel.toggleTranslation() }
            SwitchRow("Text Enhancement", state.enhancement) { viewModel.toggleEnhancement() }
        }
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `ai/llm/LLMProvider.kt` | Provider 接口 |
| `ai/llm/local/LocalLLMProvider.kt` | 本地 LLM |
| `ai/llm/cloud/CloudLLMProvider.kt` | 云端 LLM |
| `ai/llm/pipeline/PromptBuilder.kt` | Prompt 构建 |
| `ai/llm/pipeline/ResponseParser.kt` | 响应解析 |
| `ui/settings/LLMSettingsScreen.kt` | LLM 设置页面 |
| `assets/prompts/autocomplete.txt` | 自动补全模板 |
| `assets/prompts/translate.txt` | 翻译模板 |
| `assets/prompts/enhance.txt` | 美化模板 |
