package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.domain.repository.StorageRepository

class AnalyzeStorageUseCase(
    private val repository: StorageRepository,
    private val historyRepository: AnalysisHistoryRepository? = null
) {
    suspend operator fun invoke(): Result<StorageAnalysisResult> {
        val result = repository.analyzeStorage()
        result.onSuccess { analysis ->
            historyRepository?.saveAnalysis(analysis)
        }
        return result
    }
}
