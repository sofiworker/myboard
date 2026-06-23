# P8: STT 集成 (1 周)

## 1. 目标

实现系统级 STT 接口，端侧模型待调研后接入。

## 2. 里程碑验收标准

- [x] Android 系统 STT 可用
- [x] 语音输入可插入文本
- [x] 支持多语言语音识别
- [x] 端侧 STT 接口预留

## 3. 详细设计

### 3.1 Provider 接口

```kotlin
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
    val maxDuration: Duration = 30.seconds,
    val model: String? = null
)

sealed interface STTEvent {
    data object Listening : STTEvent
    data class PartialResult(val text: String, val confidence: Float) : STTEvent
    data class FinalResult(val text: String, val segments: List<Segment>) : STTEvent
    data class Error(val cause: Throwable) : STTEvent
}
```

### 3.2 Android 系统 STT

```kotlin
class AndroidSTTProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : STTProvider {
    override val id = "android"
    override val name = "Android SpeechRecognizer"

    private var recognizer: SpeechRecognizer? = null

    override fun startListening(config: STTConfig): Flow<STTEvent> = callbackFlow {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    trySend(STTEvent.FinalResult(text, emptyList()))
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    trySend(STTEvent.PartialResult(text, 0.8f))
                }
                override fun onReadyForSpeech(params: Bundle?) { trySend(STTEvent.Listening) }
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onError(error: Int) {
                    trySend(STTEvent.Error(RuntimeException("STT error: $error")))
                }
                override fun onEndOfSpeech() {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer?.startListening(intent)
        awaitClose { recognizer?.cancel() }
    }

    override fun stopListening() { recognizer?.stopListening() }
    override fun cancel() { recognizer?.cancel() }
    override fun isAvailable() = SpeechRecognizer.isRecognitionAvailable(context)
    override fun getSupportedLanguages() = listOf("en", "zh", "ja", "ko", "fr", "de", "es", "ar")
}
```

### 3.3 端侧 STT（待接入）

```kotlin
/**
 * 端侧 STT Provider 接口。
 * 待用户调研后接入具体模型。
 * 当前保留接口定义，实现返回 isAvailable = false。
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
    override fun getSupportedLanguages() = emptyList()
}
```

### 3.4 STT UI

```kotlin
@Composable
fun STTPanel(
    isListening: Boolean,
    partialText: String,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1F1F1F))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isListening) {
            // 波形动画
            WaveformAnimation(modifier = Modifier.height(48.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(partialText, color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            IconButton(onClick = onStop) {
                Icon(Icons.Default.Stop, "Stop", tint = Color.Red)
            }
        } else {
            IconButton(onClick = onStart) {
                Icon(Icons.Default.Mic, "Start", tint = Color.White)
            }
        }
    }
}
```

## 4. 文件清单

| 文件 | 说明 |
|------|------|
| `ai/stt/STTProvider.kt` | Provider 接口 |
| `ai/stt/android/AndroidSTTProvider.kt` | 系统 STT |
| `ai/stt/ondevice/OnDeviceSTTProvider.kt` | 端侧 STT（待接入） |
| `ui/panel/STTPanel.kt` | STT UI |
