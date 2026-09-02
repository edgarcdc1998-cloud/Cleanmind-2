package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import kotlinx.coroutines.flow.Flow

class GetLatestAnalysisUseCase(
    private val analysisHistoryRepository: AnalysisHistoryRepository
) {
    operator fun invoke(): Flow<StorageAnalysisResult?> {
        return analysisHistoryRepository.getLatestAnalysis()
    }
}
