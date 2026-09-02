package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository

class SaveAnalysisResultUseCase(
    private val analysisHistoryRepository: AnalysisHistoryRepository
) {
    suspend operator fun invoke(result: StorageAnalysisResult): Result<Long> {
        return analysisHistoryRepository.saveAnalysis(result)
    }
}
