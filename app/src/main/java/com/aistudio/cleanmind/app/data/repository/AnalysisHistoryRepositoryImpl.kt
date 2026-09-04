package com.aistudio.cleanmind.app.data.repository

import com.aistudio.cleanmind.app.data.local.dao.AnalysisDao
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.RecommendationEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity
import com.aistudio.cleanmind.app.data.local.entity.AnalysisWithDetails
import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.DuplicateGroup
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.util.StorageFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AnalysisHistoryRepositoryImpl(
    private val analysisDao: AnalysisDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AnalysisHistoryRepository {

    override fun getLatestAnalysis(): Flow<StorageAnalysisResult?> {
        return analysisDao.getLatestAnalysisWithDetails()
            .map { withDetails -> withDetails?.let { mapToStorageAnalysisResult(it) } }
            .flowOn(ioDispatcher)
    }

    override fun getAllAnalyses(): Flow<List<StorageAnalysisResult>> {
        return analysisDao.getAllAnalysesWithDetails()
            .map { list -> list.map { mapToStorageAnalysisResult(it) } }
            .flowOn(ioDispatcher)
    }

    private fun mapToStorageAnalysisResult(item: AnalysisWithDetails): StorageAnalysisResult {
        val summary = item.summary
        val stats = DeviceStorageStats(
            totalBytes = summary.deviceTotalBytes,
            freeBytes = summary.deviceFreeBytes,
            usedBytes = summary.deviceUsedBytes
        )
        val categories = item.categories.map { catEntity ->
            CategorySummary(
                category = catEntity.category,
                fileCount = catEntity.fileCount,
                totalSizeBytes = catEntity.totalSizeBytes
            )
        }

        val recommendations = item.recommendations.map { rec ->
            CleanupRecommendation(
                id = rec.id,
                file = StorageFile(
                    id = rec.fileId,
                    name = rec.fileName,
                    uri = rec.fileUri,
                    sizeBytes = rec.fileSizeBytes,
                    mimeType = "",
                    extension = StorageFormatter.extractExtension(rec.fileName),
                    dateModifiedEpochSeconds = 0L,
                    category = rec.category
                ),
                type = rec.type,
                priority = rec.priority,
                score = rec.score,
                reason = rec.reason,
                reclaimableSizeBytes = rec.reclaimableSizeBytes,
                duplicateGroupId = rec.duplicateGroupId
            )
        }

        val recommendationsSummary = if (recommendations.isNotEmpty()) {
            val duplicateGroups = recommendations
                .filter { it.type == RecommendationType.DUPLICATE && it.duplicateGroupId != null }
                .groupBy { it.duplicateGroupId!! }
                .map { (groupId, recs) ->
                    val files = recs.map { it.file }
                    val singleSize = recs.firstOrNull()?.reclaimableSizeBytes ?: 0L
                    DuplicateGroup(
                        groupId = groupId,
                        totalSizeBytes = (files.size + 1) * singleSize,
                        reclaimableSizeBytes = files.size * singleSize,
                        files = files
                    )
                }

            AnalysisRecommendationsSummary(
                totalRecommendationsCount = recommendations.size,
                potentialReclaimableBytes = recommendations.sumOf { it.reclaimableSizeBytes },
                largeFilesCount = recommendations.count { it.type == RecommendationType.LARGE_FILE },
                duplicateFilesCount = recommendations.count { it.type == RecommendationType.DUPLICATE },
                duplicateGroupsCount = duplicateGroups.size,
                oldFilesCount = recommendations.count { it.type == RecommendationType.OLD_FILE },
                temporaryFilesCount = recommendations.count { it.type == RecommendationType.TEMPORARY_FILE },
                recommendations = recommendations,
                duplicateGroups = duplicateGroups
            )
        } else {
            null
        }

        return StorageAnalysisResult(
            id = summary.id,
            totalFilesCount = summary.totalFilesCount,
            totalAnalyzedSizeBytes = summary.totalAnalyzedSizeBytes,
            categorySummaries = categories,
            files = emptyList(),
            deviceStorageStats = stats,
            timestampEpochMillis = summary.timestampEpochMillis,
            recommendationsSummary = recommendationsSummary
        )
    }

    override suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long> = withContext(ioDispatcher) {
        try {
            val summaryEntity = AnalysisSummaryEntity(
                timestampEpochMillis = result.timestampEpochMillis,
                totalFilesCount = result.totalFilesCount,
                totalAnalyzedSizeBytes = result.totalAnalyzedSizeBytes,
                deviceTotalBytes = result.deviceStorageStats.totalBytes,
                deviceUsedBytes = result.deviceStorageStats.usedBytes,
                deviceFreeBytes = result.deviceStorageStats.freeBytes
            )

            val categoryEntities = result.categorySummaries.map { cat ->
                CategorySummaryEntity(
                    analysisId = 0L,
                    category = cat.category,
                    fileCount = cat.fileCount,
                    totalSizeBytes = cat.totalSizeBytes
                )
            }

            val fileEntities = result.files.map { file ->
                ScannedFileEntity(
                    analysisId = 0L,
                    originalFileId = file.id,
                    name = file.name,
                    uri = file.uri,
                    sizeBytes = file.sizeBytes,
                    mimeType = file.mimeType,
                    extension = file.extension,
                    dateModifiedEpochSeconds = file.dateModifiedEpochSeconds,
                    category = file.category
                )
            }

            val recommendationEntities = result.recommendationsSummary?.recommendations?.map { rec ->
                RecommendationEntity(
                    analysisId = 0L,
                    fileId = rec.file.id,
                    fileName = rec.file.name,
                    fileUri = rec.file.uri,
                    fileSizeBytes = rec.file.sizeBytes,
                    category = rec.file.category,
                    type = rec.type,
                    priority = rec.priority,
                    score = rec.score,
                    reason = rec.reason,
                    reclaimableSizeBytes = rec.reclaimableSizeBytes,
                    duplicateGroupId = rec.duplicateGroupId
                )
            } ?: emptyList()

            val id = analysisDao.insertFullAnalysis(
                summary = summaryEntity,
                categories = categoryEntities,
                files = fileEntities,
                recommendations = recommendationEntities
            )
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>> {
        return analysisDao.getScannedFiles(analysisId)
            .map { entities ->
                entities.map { entity ->
                    StorageFile(
                        id = entity.originalFileId,
                        name = entity.name,
                        uri = entity.uri,
                        sizeBytes = entity.sizeBytes,
                        mimeType = entity.mimeType,
                        extension = entity.extension,
                        dateModifiedEpochSeconds = entity.dateModifiedEpochSeconds,
                        category = entity.category
                    )
                }
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun clearHistory(): Result<Unit> = withContext(ioDispatcher) {
        try {
            analysisDao.clearAll()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
