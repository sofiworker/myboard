package xyz.xiao6.myboard.data.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class VoiceInputManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    enum class State {
        IDLE,
        LISTENING,
        RECOGNIZING
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State> = _state

    private var onResult: (String) -> Unit = {}

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = State.LISTENING
        }

        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0) ?: ""
            onResult(text)
            _state.value = State.IDLE
        }

        override fun onError(error: Int) {
            _state.value = State.IDLE
        }

        // Other listener methods can be implemented to provide more detailed feedback
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _state.value = State.RECOGNIZING
        }
        override fun onPartialResults(partialResults: Bundle?) {}
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    fun startListening(onResult: (String) -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // In a real app, you would request the permission here.
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            return
        }
        this.onResult = onResult
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(listener)
            startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH))
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _state.value = State.IDLE
    }

    fun cancel() {
        speechRecognizer?.cancel()
        _state.value = State.IDLE
    }
}
