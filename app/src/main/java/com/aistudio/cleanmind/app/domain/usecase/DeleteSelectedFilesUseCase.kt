package com.aistudio.cleanmind.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class DeleteSelectedFilesUseCase(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(recommendations: List<CleanupRecommendation>): Result<DeletionSummary> = withContext(ioDispatcher) {
        var successCount = 0
        var successBytes = 0L
        val failedFiles = mutableListOf<String>()

        recommendations.forEach { rec ->
            val uriString = rec.file.uri
            var deleted = false
            try {
                if (uriString.startsWith("content://")) {
                    val uri = Uri.parse(uriString)
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    deleted = deletedRows > 0
                } else {
                    val path = uriString.removePrefix("file://")
                    val file = File(path)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                }
            } catch (e: Exception) {
                // Handle deletion errors gracefully
            }

            // Fallback for relative path physical files if ContentResolver delete is non-functional or permissions block
            if (!deleted && rec.file.relativePath != null) {
                try {
                    val file = File(rec.file.relativePath)
                    if (file.exists()) {
                        deleted = file.delete()
                    }
                } catch (_: Exception) {}
            }

            // Exclusão estrita sem simulação: apenas marcar sucesso se a operação física foi efetivamente confirmada
            if (deleted) {
                successCount++
                successBytes += rec.reclaimableSizeBytes
            } else {
                failedFiles.add(rec.file.name)
            }
        }

        Result.success(
            DeletionSummary(
                deletedCount = successCount,
                reclaimedBytes = successBytes,
                failedFileNames = failedFiles
            )
        )
    }
}

data class DeletionSummary(
    val deletedCount: Int,
    val reclaimedBytes: Long,
    val failedFileNames: List<String>
)
