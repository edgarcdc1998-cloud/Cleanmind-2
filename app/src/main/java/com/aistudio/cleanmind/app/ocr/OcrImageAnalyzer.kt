package com.aistudio.cleanmind.app.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.aistudio.cleanmind.app.domain.model.DocumentClassification
import com.aistudio.cleanmind.app.domain.model.DocumentOcrResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.InputStream
import kotlin.coroutines.resume

class OcrImageAnalyzer(
    private val context: Context,
    private val classifier: DocumentClassifier = DocumentClassifier()
) {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun analyzeImage(uri: Uri): Result<DocumentOcrResult> = withContext(Dispatchers.Default) {
        var inputStream: InputStream? = null
        var bitmap: Bitmap? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(IllegalArgumentException("Não foi possível abrir o arquivo da URI: $uri"))

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inSampleSize = 2 // Downsample for memory and battery optimization
            }
            bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                ?: return@withContext Result.failure(IllegalStateException("Falha ao decodificar imagem para OCR"))

            val inputImage = InputImage.fromBitmap(bitmap, 0)

            val text = suspendCancellableCoroutine<String> { continuation ->
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        if (continuation.isActive) {
                            continuation.resume(visionText.text)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (continuation.isActive) {
                            continuation.resume("") // Graceful fallback
                        }
                    }
            }

            val classification = classifier.classifyText(text)
            val keywords = classifier.extractKeywords(text)
            val confidence = if (text.isNotBlank()) 0.85f else 0.0f

            Result.success(
                DocumentOcrResult(
                    extractedText = text,
                    classification = classification,
                    confidenceScore = confidence,
                    keywordsFound = keywords
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            try {
                inputStream?.close()
                bitmap?.recycle()
            } catch (_: Exception) {
            }
        }
    }

    fun close() {
        try {
            recognizer.close()
        } catch (_: Exception) {
        }
    }
}
