package com.aistudio.cleanmind.app.domain.repository

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageFile
import kotlinx.coroutines.flow.Flow

interface AnalysisHistoryRepository {
    fun getLatestAnalysis(): Flow<StorageAnalysisResult?>
    suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long>
    fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>>
    suspend fun clearHistory(): Result<Unit>
}
