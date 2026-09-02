package com.aistudio.cleanmind.app.domain.model

data class CleanupRecommendation(
    val id: Long = 0L,
    val file: StorageFile,
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val score: Int,
    val reason: String,
    val reclaimableSizeBytes: Long,
    val duplicateGroupId: String? = null
)
