package com.aistudio.cleanmind.app.data.repository

import com.aistudio.cleanmind.app.data.local.dao.AnalysisDao
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
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
        return analysisDao.getLatestAnalysisWithCategories()
            .map { withCategories ->
                withCategories?.let { item ->
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

                    StorageAnalysisResult(
                        id = summary.id,
                        totalFilesCount = summary.totalFilesCount,
                        totalAnalyzedSizeBytes = summary.totalAnalyzedSizeBytes,
                        categorySummaries = categories,
                        files = emptyList(),
                        deviceStorageStats = stats,
                        timestampEpochMillis = summary.timestampEpochMillis
                    )
                }
            }
            .flowOn(ioDispatcher)
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

            val id = analysisDao.insertFullAnalysis(
                summary = summaryEntity,
                categories = categoryEntities,
                files = fileEntities
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
