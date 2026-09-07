package com.aistudio.cleanmind.app.domain.usecase

import android.annotation.SuppressLint
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val sdkVersion: Int = Build.VERSION.SDK_INT
) {
    companion object {
        const val MAX_BATCH_SIZE = 2000
    }

    open suspend fun execute(recommendations: List<CleanupRecommendation>): DeletionResult = withContext(ioDispatcher) {
        var directDeletedCount = 0
        var directReclaimedBytes = 0L
        val directFailedFiles = mutableListOf<String>()
        val directDeletedIds = mutableSetOf<Long>()
        val authRequiredRecs = mutableListOf<CleanupRecommendation>()

        for (rec in recommendations) {
            val uriString = rec.file.uri
            if (uriString.startsWith("content://")) {
                if (sdkVersion >= Build.VERSION_CODES.R) {
                    val uri = Uri.parse(uriString)
                    if (!doesContentUriExist(uri)) {
                        // File already does not exist in MediaStore
                        directFailedFiles.add(rec.file.name)
                        continue
                    }

                    var deleted = false
                    var needsAuth = false
                    try {
                        val deletedRows = deleteContentUri(uri)
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
                    // Android 29 fallback: delegated directly to requestAuthorization
                    // to prevent duplicate delete attempts on the same files.
                    authRequiredRecs.add(rec)
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

            val authResult = requestAuthorization(batchRecommendations)
            val updatedDirectDeletedCount = directSummary.deletedCount + authResult.directlyDeleted.size
            val updatedDirectReclaimedBytes = directSummary.reclaimedBytes + authResult.directlyDeleted.sumOf { it.reclaimableSizeBytes }
            val updatedDirectDeletedIds = directSummary.deletedRecommendationIds + authResult.directlyDeleted.map { it.id }
            val updatedFailedFiles = directSummary.failedFileNames + authResult.failedRecommendations.map { it.file.name }
            val updatedDirectSummary = directSummary.copy(
                deletedCount = updatedDirectDeletedCount,
                reclaimedBytes = updatedDirectReclaimedBytes,
                failedFileNames = updatedFailedFiles,
                deletedRecommendationIds = updatedDirectDeletedIds
            )

            if (authResult.intentSender != null) {
                return@withContext DeletionResult.RequiresAuthorization(
                    intentSender = authResult.intentSender,
                    pendingRecommendations = authResult.pendingRecommendations,
                    directSummary = updatedDirectSummary
                )
            } else {
                val unhandledBatchRemainder = authRequiredRecs.drop(batchRecommendations.size)
                val remainingFailures = authResult.pendingRecommendations.map { it.file.name } +
                    unhandledBatchRemainder.map { it.file.name }
                return@withContext DeletionResult.Completed(
                    updatedDirectSummary.copy(
                        failedFileNames = updatedDirectSummary.failedFileNames + remainingFailures
                    )
                )
            }
        }

        DeletionResult.Completed(directSummary)
    }

    internal open fun deleteContentUri(uri: Uri): Int {
        return context.contentResolver.delete(uri, null, null)
    }

    @SuppressLint("NewApi")
    internal open fun requestAuthorization(
        recommendations: List<CleanupRecommendation>
    ): AuthorizationRequestResult {
        if (sdkVersion >= Build.VERSION_CODES.R) {
            val intentSender = createIntentSenderForAuthorization(recommendations)
            return AuthorizationRequestResult(
                intentSender = intentSender,
                directlyDeleted = emptyList(),
                pendingRecommendations = recommendations,
                failedRecommendations = emptyList()
            )
        } else if (sdkVersion >= Build.VERSION_CODES.Q) {
            val directlyDeleted = mutableListOf<CleanupRecommendation>()
            val failed = mutableListOf<CleanupRecommendation>()
            for ((index, rec) in recommendations.withIndex()) {
                val uri = Uri.parse(rec.file.uri)
                if (!doesContentUriExist(uri)) {
                    failed.add(rec)
                    continue
                }

                try {
                    val deletedRows = deleteContentUri(uri)
                    if (deletedRows > 0 && !doesContentUriExist(uri)) {
                        directlyDeleted.add(rec)
                    } else {
                        failed.add(rec)
                    }
                } catch (e: RecoverableSecurityException) {
                    val intentSender = e.userAction.actionIntent.intentSender
                    val pending = listOf(rec) + recommendations.subList(index + 1, recommendations.size)
                    return AuthorizationRequestResult(
                        intentSender = intentSender,
                        directlyDeleted = directlyDeleted,
                        pendingRecommendations = pending,
                        failedRecommendations = failed
                    )
                } catch (_: Exception) {
                    failed.add(rec)
                }
            }
            return AuthorizationRequestResult(
                intentSender = null,
                directlyDeleted = directlyDeleted,
                pendingRecommendations = emptyList(),
                failedRecommendations = failed
            )
        } else {
            return AuthorizationRequestResult(
                intentSender = null,
                directlyDeleted = emptyList(),
                pendingRecommendations = emptyList(),
                failedRecommendations = recommendations
            )
        }
    }

    open suspend fun verifyAndFinalizeAfterAuthorization(
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

    @SuppressLint("NewApi")
    internal open fun createIntentSenderForAuthorization(recommendations: List<CleanupRecommendation>): IntentSender? {
        return try {
            if (sdkVersion >= Build.VERSION_CODES.R) {
                val uris = recommendations.take(MAX_BATCH_SIZE).map { Uri.parse(it.file.uri) }
                val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, uris)
                pendingIntent.intentSender
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}

data class AuthorizationRequestResult(
    val intentSender: IntentSender?,
    val directlyDeleted: List<CleanupRecommendation> = emptyList(),
    val pendingRecommendations: List<CleanupRecommendation> = emptyList(),
    val failedRecommendations: List<CleanupRecommendation> = emptyList()
)

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
