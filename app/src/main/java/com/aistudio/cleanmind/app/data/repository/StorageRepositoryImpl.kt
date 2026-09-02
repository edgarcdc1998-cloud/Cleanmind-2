package com.aistudio.cleanmind.app.data.repository

import com.aistudio.cleanmind.app.data.datasource.StorageDataSource
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageRepositoryImpl(
    private val dataSource: StorageDataSource,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageRepository {

    override suspend fun getDeviceStorageStats(): DeviceStorageStats {
        return dataSource.getDeviceStorageStats()
    }

    override suspend fun analyzeStorage(): Result<StorageAnalysisResult> = withContext(ioDispatcher) {
        try {
            val deviceStats = dataSource.getDeviceStorageStats()
            val files = dataSource.queryMediaFiles()

            val categoryMap = mutableMapOf<StorageCategory, MutableList<Long>>()
            StorageCategory.values().forEach { cat ->
                categoryMap[cat] = mutableListOf()
            }

            var totalAnalyzedSize = 0L

            files.forEach { file ->
                totalAnalyzedSize += file.sizeBytes
                categoryMap[file.category]?.add(file.sizeBytes)
                if (file.isLargeFile) {
                    categoryMap[StorageCategory.LARGE_FILES]?.add(file.sizeBytes)
                }
            }

            val categorySummaries = StorageCategory.entries.mapNotNull { cat ->
                val sizes = categoryMap[cat] ?: emptyList()
                if (sizes.isNotEmpty()) {
                    CategorySummary(
                        category = cat,
                        fileCount = sizes.size,
                        totalSizeBytes = sizes.sum()
                    )
                } else null
            }

            val result = StorageAnalysisResult(
                totalFilesCount = files.size,
                totalAnalyzedSizeBytes = totalAnalyzedSize,
                categorySummaries = categorySummaries,
                files = files,
                deviceStorageStats = deviceStats
            )

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
