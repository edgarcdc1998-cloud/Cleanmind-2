package com.aistudio.cleanmind.app.domain.model

data class StorageAnalysisResult(
    val totalFilesCount: Int,
    val totalAnalyzedSizeBytes: Long,
    val categorySummaries: List<CategorySummary>,
    val files: List<StorageFile>,
    val deviceStorageStats: DeviceStorageStats,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val id: Long = 0L,
    val recommendationsSummary: AnalysisRecommendationsSummary? = null
)
