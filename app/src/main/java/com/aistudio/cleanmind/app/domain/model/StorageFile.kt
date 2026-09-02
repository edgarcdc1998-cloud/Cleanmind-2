package com.aistudio.cleanmind.app.domain.model

data class StorageFile(
    val id: Long,
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extension: String,
    val dateModifiedEpochSeconds: Long,
    val category: StorageCategory,
    val relativePath: String? = null,
    val isReadable: Boolean = true
) {
    val isLargeFile: Boolean
        get() = sizeBytes >= LARGE_FILE_THRESHOLD_BYTES

    companion object {
        const val LARGE_FILE_THRESHOLD_BYTES = 50L * 1024L * 1024L // 50 MB
    }
}
