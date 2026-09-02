package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository

class ClearAnalysisHistoryUseCase(
    private val analysisHistoryRepository: AnalysisHistoryRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return analysisHistoryRepository.clearHistory()
    }
}
