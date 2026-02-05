package xyz.xiao6.myboard.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

/**
 * Manages offline speech recognition using Vosk.
 * Handles model loading, service lifecycle, and recognition callbacks.
 */
class VoiceInputManager(private val context: Context) {
    private val logTag = "VoiceInputManager"
    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false
    private var isInitializing = false
    
    // Callbacks
    var onPartialResult: ((String) -> Unit)? = null
    var onResult: ((String) -> Unit)? = null
    var onError: ((Exception) -> Unit)? = null
    var onStateChanged: ((Boolean) -> Unit)? = null

    private val recognitionListener = object : RecognitionListener {
        override fun onPartialResult(hypothesis: String?) {
            hypothesis?.let {
                val text = parseVoskResult(it, "partial")
                if (text.isNotEmpty()) {
                    onPartialResult?.invoke(text)
                }
            }
        }

        override fun onResult(hypothesis: String?) {
            hypothesis?.let {
                val text = parseVoskResult(it, "text")
                if (text.isNotEmpty()) {
                    onResult?.invoke(text)
                }
            }
        }

        override fun onFinalResult(hypothesis: String?) {
            hypothesis?.let {
                val text = parseVoskResult(it, "text")
                if (text.isNotEmpty()) {
                    onResult?.invoke(text)
                }
            }
            stopListening()
        }

        override fun onError(exception: Exception?) {
            exception?.let {
                Log.e(logTag, "Recognition error", it)
                onError?.invoke(it)
            }
            stopListening()
        }

        override fun onTimeout() {
            stopListening()
        }
    }

    /**
     * Initializes the Vosk model asynchronously.
     */
    fun initialize(scope: CoroutineScope, onInitialized: (Boolean) -> Unit) {
        if (model != null) {
            onInitialized(true)
            return
        }
        if (isInitializing) return
        isInitializing = true

        Log.d(logTag, "Starting Vosk model initialization...")
        
        // Ensure assets exist and are not compressed (handled in build.gradle)
        // StorageService.unpack checks if files are already synced based on a version file.
        
        try {
            StorageService.unpack(context, "model-small-cn", "model",
                { m ->
                    model = m
                    isInitializing = false
                    Log.d(logTag, "Vosk model loaded successfully")
                    scope.launch(Dispatchers.Main) {
                        onInitialized(true)
                    }
                },
                { exception ->
                    isInitializing = false
                    Log.e(logTag, "Vosk failed to unpack model: ${exception.message}", exception)
                    scope.launch(Dispatchers.Main) {
                        onInitialized(false)
                    }
                }
            )
        } catch (e: Exception) {
            isInitializing = false
            Log.e(logTag, "Vosk fatal initialization error", e)
            onInitialized(false)
        }
    }

    fun isModelLoaded(): Boolean = model != null

    fun startListening() {
        if (isListening) return
        val m = model ?: run {
            Log.e(logTag, "Model not loaded yet")
            onError?.invoke(Exception("模型尚未就绪，请稍后再试"))
            return
        }

        try {
            val rec = Recognizer(m, 16000.0f)
            speechService = SpeechService(rec, 16000.0f)
            speechService?.startListening(recognitionListener)
            isListening = true
            onStateChanged?.invoke(true)
            Log.d(logTag, "Started listening")
        } catch (e: IOException) {
            Log.e(logTag, "Failed to start speech service", e)
            onError?.invoke(e)
        }
    }

    fun stopListening() {
        if (!isListening) return
        speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isListening = false
        onStateChanged?.invoke(false)
        Log.d(logTag, "Stopped listening")
    }

    fun cleanup() {
        stopListening()
        model?.close()
        model = null
    }

    private fun parseVoskResult(json: String, key: String): String {
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.optString(key, "")
        } catch (e: Exception) {
            ""
        }
    }
}