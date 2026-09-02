package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.domain.repository.StorageRepository

class AnalyzeStorageUseCase(
    private val repository: StorageRepository,
    private val historyRepository: AnalysisHistoryRepository? = null,
    private val generateCleanupRecommendationsUseCase: GenerateCleanupRecommendationsUseCase? = null
) {
    suspend operator fun invoke(): Result<StorageAnalysisResult> {
        val result = repository.analyzeStorage()
        return result.mapCatching { analysis ->
            val enrichedAnalysis = if (generateCleanupRecommendationsUseCase != null && analysis.files.isNotEmpty()) {
                val recs = generateCleanupRecommendationsUseCase(analysis.files)
                analysis.copy(recommendationsSummary = recs)
            } else {
                analysis
            }
            historyRepository?.saveAnalysis(enrichedAnalysis)
            enrichedAnalysis
        }
    }
}
