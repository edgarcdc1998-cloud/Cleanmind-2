package com.aistudio.cleanmind.app.domain.model

data class CategorySummary(
    val category: StorageCategory,
    val fileCount: Int,
    val totalSizeBytes: Long
)
