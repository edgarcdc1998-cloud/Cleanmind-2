package com.aistudio.cleanmind.app.domain.model

data class DuplicateGroup(
    val groupId: String,
    val totalSizeBytes: Long,
    val reclaimableSizeBytes: Long,
    val files: List<StorageFile>
)
