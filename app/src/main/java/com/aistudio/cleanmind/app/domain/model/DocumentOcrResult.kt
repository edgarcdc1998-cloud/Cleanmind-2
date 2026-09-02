package com.aistudio.cleanmind.app.domain.model

data class DocumentOcrResult(
    val extractedText: String,
    val classification: DocumentClassification,
    val confidenceScore: Float,
    val keywordsFound: List<String> = emptyList()
) {
    val hasText: Boolean get() = extractedText.isNotBlank()
}
