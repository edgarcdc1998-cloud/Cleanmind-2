package com.aistudio.cleanmind.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aistudio.cleanmind.app.CleanMindApp
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageAnalysisWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    // Worker factory / test injection hook
    var customAnalyzeStorageUseCase: AnalyzeStorageUseCase? = null

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        if (isStopped) {
            return@withContext Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Worker was cancelled before starting.")
            )
        }

        try {
            val analyzeUseCase = customAnalyzeStorageUseCase
                ?: CleanMindApp.getAppContainer(applicationContext).analyzeStorageUseCase

            val analysisResult = analyzeUseCase().getOrThrow()

            if (isStopped) {
                return@withContext Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to "Worker was stopped during execution.")
                )
            }

            Result.success(
                workDataOf(
                    KEY_STATUS to STATUS_SUCCESS,
                    KEY_ITEMS_COUNT to analysisResult.totalFilesCount,
                    KEY_TIMESTAMP to analysisResult.timestampEpochMillis,
                    KEY_RECLAIMABLE_BYTES to (analysisResult.recommendationsSummary?.potentialReclaimableBytes ?: 0L)
                )
            )
        } catch (e: SecurityException) {
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Storage permission missing or denied: ${e.localizedMessage}")
            )
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS && !isStopped) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to (e.localizedMessage ?: "Unknown error occurred during analysis"))
                )
            }
        }
    }

    companion object {
        const val KEY_STATUS = "key_analysis_status"
        const val KEY_ITEMS_COUNT = "key_items_count"
        const val KEY_TIMESTAMP = "key_timestamp"
        const val KEY_RECLAIMABLE_BYTES = "key_reclaimable_bytes"
        const val KEY_ERROR_MESSAGE = "key_error_message"

        const val STATUS_SUCCESS = "SUCCESS"
        private const val MAX_RETRY_ATTEMPTS = 2
    }
}
