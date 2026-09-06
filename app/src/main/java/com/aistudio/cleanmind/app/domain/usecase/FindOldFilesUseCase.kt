package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageFile

class FindOldFilesUseCase(
    private val defaultThresholdDays: Int = DEFAULT_THRESHOLD_DAYS,
    private val defaultMinSizeBytes: Long = DEFAULT_MIN_SIZE_BYTES
) {
    operator fun invoke(
        files: List<StorageFile>,
        thresholdDays: Int = defaultThresholdDays,
        minSizeBytes: Long = defaultMinSizeBytes,
        currentEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): List<StorageFile> {
        if (thresholdDays <= 0) return emptyList()
        val thresholdSeconds = thresholdDays * SECONDS_IN_DAY

        return files.asSequence()
            .filter { file ->
                file.sizeBytes >= minSizeBytes &&
                    file.dateModifiedEpochSeconds > 0L &&
                    (currentEpochSeconds - file.dateModifiedEpochSeconds) > thresholdSeconds
            }
            .sortedByDescending { it.sizeBytes }
            .toList()
    }

    companion object {
        const val OLD_FILE_DAYS = 30
        const val OLD_FILE_THRESHOLD_SECONDS = OLD_FILE_DAYS * 24L * 60L * 60L
        const val DEFAULT_THRESHOLD_DAYS = OLD_FILE_DAYS
        const val DEFAULT_MIN_SIZE_BYTES = 0L
        private const val SECONDS_IN_DAY = 86400L
    }
}
