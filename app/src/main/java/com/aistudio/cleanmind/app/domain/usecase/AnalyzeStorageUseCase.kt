package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.StorageRepository

class AnalyzeStorageUseCase(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(): Result<StorageAnalysisResult> {
        return repository.analyzeStorage()
    }
}
