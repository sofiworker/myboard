package xyz.xiao6.myboard.data

import android.util.Log
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.Ink
import com.google.mlkit.vision.digitalink.RecognitionResult

class HandwritingRecognizer {

    private var recognizer: DigitalInkRecognizer? = null

    fun downloadModel(language: String, onDownloaded: () -> Unit) {
        val modelIdentifier: DigitalInkRecognitionModelIdentifier? =
            try {
                DigitalInkRecognitionModelIdentifier.fromLanguageTag(language)
            } catch (e: Exception) {
                Log.e("HandwritingRecognizer", "Failed to get model identifier", e)
                null
            }

        if (modelIdentifier == null) {
            Log.e("HandwritingRecognizer", "Model not found for language: $language")
            return
        }

        val model = DigitalInkRecognitionModel.builder(modelIdentifier).build()
        val remoteModelManager = RemoteModelManager.getInstance()

        remoteModelManager.isModelDownloaded(model).addOnSuccessListener { isDownloaded ->
            if (isDownloaded) {
                Log.i("HandwritingRecognizer", "Model already downloaded")
                recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
                onDownloaded()
            } else {
                Log.i("HandwritingRecognizer", "Downloading model...")
                remoteModelManager.download(model, DownloadConditions.Builder().build())
                    .addOnSuccessListener {
                        Log.i("HandwritingRecognizer", "Model downloaded")
                        recognizer = DigitalInkRecognition.getClient(DigitalInkRecognizerOptions.builder(model).build())
                        onDownloaded()
                    }
                    .addOnFailureListener { e ->
                        Log.e("HandwritingRecognizer", "Error while downloading model", e)
                    }
            }
        }
    }

    fun recognize(ink: Ink, onResult: (List<String>) -> Unit) {
        recognizer?.recognize(ink)
            ?.addOnSuccessListener { result: RecognitionResult ->
                onResult(result.candidates.map { it.text })
            }
            ?.addOnFailureListener { e ->
                Log.e("HandwritingRecognizer", "Error while recognizing", e)
            }
    }
}
