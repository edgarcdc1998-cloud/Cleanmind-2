package com.aistudio.cleanmind.app.domain.usecase

import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

open class DeleteSelectedFilesUseCase(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    companion object {
        const val MAX_BATCH_SIZE = 2000
    }

    suspend fun execute(recommendations: List<CleanupRecommendation>): DeletionResult = withContext(ioDispatcher) {
        var directDeletedCount = 0
        var directReclaimedBytes = 0L
        val directFailedFiles = mutableListOf<String>()
        val directDeletedIds = mutableSetOf<Long>()
        val authRequiredRecs = mutableListOf<CleanupRecommendation>()

        for (rec in recommendations) {
            val uriString = rec.file.uri
            if (uriString.startsWith("content://")) {
                val uri = Uri.parse(uriString)
                if (!doesContentUriExist(uri)) {
                    // File already does not exist in MediaStore
                    directFailedFiles.add(rec.file.name)
                    continue
                }

                var deleted = false
                var needsAuth = false
                try {
                    val deletedRows = context.contentResolver.delete(uri, null, null)
                    if (deletedRows > 0 && !doesContentUriExist(uri)) {
                        deleted = true
                    } else {
                        needsAuth = true
                    }
                } catch (_: SecurityException) {
                    needsAuth = true
                } catch (_: Exception) {
                    needsAuth = false
                }

                if (deleted) {
                    directDeletedCount++
                    directReclaimedBytes += rec.reclaimableSizeBytes
                    directDeletedIds.add(rec.id)
                } else if (needsAuth) {
                    authRequiredRecs.add(rec)
                } else {
                    directFailedFiles.add(rec.file.name)
                }
            } else {
                // Physical file path or file:// URI
                val path = uriString.removePrefix("file://")
                val file = File(path)
                if (file.exists()) {
                    val deleted = file.delete()
                    if (deleted && !file.exists()) {
                        directDeletedCount++
                        directReclaimedBytes += rec.reclaimableSizeBytes
                        directDeletedIds.add(rec.id)
                    } else {
                        directFailedFiles.add(rec.file.name)
                    }
                } else {
                    directFailedFiles.add(rec.file.name)
                }
            }
        }

        val directSummary = DeletionSummary(
            deletedCount = directDeletedCount,
            reclaimedBytes = directReclaimedBytes,
            failedFileNames = directFailedFiles,
            deletedRecommendationIds = directDeletedIds
        )

        if (authRequiredRecs.isNotEmpty()) {
            val batchRecommendations = if (authRequiredRecs.size > MAX_BATCH_SIZE) {
                authRequiredRecs.take(MAX_BATCH_SIZE)
            } else {
                authRequiredRecs
            }

            val intentSender = createIntentSenderForAuthorization(batchRecommendations)
            if (intentSender != null) {
                return@withContext DeletionResult.RequiresAuthorization(
                    intentSender = intentSender,
                    pendingRecommendations = batchRecommendations,
                    directSummary = directSummary
                )
            } else {
                return@withContext DeletionResult.Completed(
                    directSummary.copy(
                        failedFileNames = directSummary.failedFileNames + authRequiredRecs.map { it.file.name }
                    )
                )
            }
        }

        DeletionResult.Completed(directSummary)
    }

    suspend fun verifyAndFinalizeAfterAuthorization(
        pendingRecommendations: List<CleanupRecommendation>,
        directSummary: DeletionSummary
    ): DeletionSummary = withContext(ioDispatcher) {
        var authDeletedCount = 0
        var authReclaimedBytes = 0L
        val authFailedFileNames = mutableListOf<String>()
        val actuallyDeletedIds = mutableSetOf<Long>()

        for (rec in pendingRecommendations) {
            val uriString = rec.file.uri
            val stillExists = if (uriString.startsWith("content://")) {
                doesContentUriExist(Uri.parse(uriString))
            } else {
                val path = uriString.removePrefix("file://")
                File(path).exists()
            }

            if (!stillExists) {
                authDeletedCount++
                authReclaimedBytes += rec.reclaimableSizeBytes
                actuallyDeletedIds.add(rec.id)
            } else {
                authFailedFileNames.add(rec.file.name)
            }
        }

        DeletionSummary(
            deletedCount = directSummary.deletedCount + authDeletedCount,
            reclaimedBytes = directSummary.reclaimedBytes + authReclaimedBytes,
            failedFileNames = directSummary.failedFileNames + authFailedFileNames,
            deletedRecommendationIds = directSummary.deletedRecommendationIds + actuallyDeletedIds
        )
    }

    suspend operator fun invoke(recommendations: List<CleanupRecommendation>): Result<DeletionSummary> {
        return when (val result = execute(recommendations)) {
            is DeletionResult.Completed -> Result.success(result.summary)
            is DeletionResult.RequiresAuthorization -> {
                Result.failure(PendingAuthorizationException(result))
            }
        }
    }

    internal open fun doesContentUriExist(uri: Uri): Boolean {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.MediaColumns._ID),
                null,
                null,
                null
            )?.use { cursor ->
                cursor.moveToFirst() && cursor.count > 0
            } ?: false
        } catch (_: Exception) {
            false
        }
    }

    internal open fun createIntentSenderForAuthorization(recommendations: List<CleanupRecommendation>): IntentSender? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val uris = recommendations.take(MAX_BATCH_SIZE).map { Uri.parse(it.file.uri) }
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                pendingIntent.intentSender
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                for (rec in recommendations) {
                    try {
                        context.contentResolver.delete(Uri.parse(rec.file.uri), null, null)
                    } catch (e: RecoverableSecurityException) {
                        return e.userAction.actionIntent.intentSender
                    } catch (_: Exception) {}
                }
                null
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

class PendingAuthorizationException(
    val requiresAuthorization: DeletionResult.RequiresAuthorization
) : IllegalStateException("Deletion requires user authorization via system dialog and cannot be completed directly")

sealed class DeletionResult {
    data class Completed(val summary: DeletionSummary) : DeletionResult()
    data class RequiresAuthorization(
        val intentSender: IntentSender,
        val pendingRecommendations: List<CleanupRecommendation>,
        val directSummary: DeletionSummary
    ) : DeletionResult()
}

data class DeletionSummary(
    val deletedCount: Int,
    val reclaimedBytes: Long,
    val failedFileNames: List<String>,
    val deletedRecommendationIds: Set<Long> = emptySet()
)
