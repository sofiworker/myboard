package xyz.xiao6.myboard.data

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.common.RecognitionResult
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import java.util.Locale

class HandwritingRecognizer {

    private var recognizer: DigitalInkRecognizer? = null
    private var currentLanguageTag: String? = null

    fun downloadModel(language: String, onDownloaded: (Boolean) -> Unit) {
        // Try the requested language tag, then the user's locale, and finally fall back to en-US.
        val candidateTags = sequenceOf(
            language,
            Locale.getDefault().toLanguageTag(),
            "en-US"
        ).map { it.replace('_', '-') } // BCP-47 prefers hyphens
            .distinct()

        var resolvedTag: String? = null
        val modelIdentifier: DigitalInkRecognitionModelIdentifier? =
            candidateTags.firstNotNullOfOrNull { tag ->
                try {
                    val id = DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag)
                    resolvedTag = tag
                    id
                } catch (e: Exception) {
                    Log.w("HandwritingRecognizer", "Invalid language tag for model: $tag", e)
                    null
                }
            }

        if (modelIdentifier == null || resolvedTag == null) {
            Log.e(
                "HandwritingRecognizer",
                "No supported Digital Ink model for language candidates."
            )
            onDownloaded(false)
            return
        }

        // If we already have the correct model, return immediately.
        if (recognizer != null && currentLanguageTag == resolvedTag) {
            onDownloaded(true)
            return
        }

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val remoteModelManager = RemoteModelManager.getInstance()

        remoteModelManager.isModelDownloaded(model).addOnSuccessListener { isDownloaded ->
            if (isDownloaded) {
                Log.i("HandwritingRecognizer", "Model already downloaded")
                recognizer = DigitalInkRecognition.getClient(
                    DigitalInkRecognizerOptions.builder(model).build()
                )
                currentLanguageTag = resolvedTag
                onDownloaded(true)
            } else {
                Log.i("HandwritingRecognizer", "Downloading model...")
                remoteModelManager.download(model, DownloadConditions.Builder().build())
                    .addOnSuccessListener {
                        Log.i("HandwritingRecognizer", "Model downloaded")
                        recognizer = DigitalInkRecognition.getClient(
                            DigitalInkRecognizerOptions.builder(model).build()
                        )
                        currentLanguageTag = resolvedTag
                        onDownloaded(true)
                    }
                    .addOnFailureListener { e ->
                        Log.e("HandwritingRecognizer", "Error while downloading model", e)
                        onDownloaded(false)
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("HandwritingRecognizer", "Failed to check model download state", e)
            onDownloaded(false)
        }
    }

    fun recognize(ink: Ink, onResult: (List<String>) -> Unit) {
        val client = recognizer
        if (client == null) {
            Log.w("HandwritingRecognizer", "Recognizer not ready. Call downloadModel() first.")
            return
        }

        client.recognize(ink)
            .addOnSuccessListener { result: RecognitionResult ->
                onResult(result.candidates.map { it.text })
            }
            .addOnFailureListener { e ->
                Log.e("HandwritingRecognizer", "Error while recognizing", e)
            }
    }
}
