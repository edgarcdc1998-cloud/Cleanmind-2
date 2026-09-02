package com.aistudio.cleanmind.app.domain.repository

import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult

interface StorageRepository {
    suspend fun getDeviceStorageStats(): DeviceStorageStats
    suspend fun analyzeStorage(): Result<StorageAnalysisResult>
}
