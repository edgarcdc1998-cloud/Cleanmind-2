package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageFile

class FindLargeFilesUseCase(
    private val defaultThresholdBytes: Long = DEFAULT_THRESHOLD_BYTES
) {
    operator fun invoke(
        files: List<StorageFile>,
        thresholdBytes: Long = defaultThresholdBytes
    ): List<StorageFile> {
        if (thresholdBytes <= 0L) return emptyList()
        return files.asSequence()
            .filter { it.sizeBytes >= thresholdBytes }
            .sortedByDescending { it.sizeBytes }
            .toList()
    }

    companion object {
        const val DEFAULT_THRESHOLD_BYTES: Long = 50L * 1024L * 1024L // 50 MB
    }
}
