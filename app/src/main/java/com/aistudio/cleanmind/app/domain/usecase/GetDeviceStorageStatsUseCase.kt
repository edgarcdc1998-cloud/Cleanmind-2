package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.repository.StorageRepository

class GetDeviceStorageStatsUseCase(
    private val repository: StorageRepository
) {
    suspend operator fun invoke(): DeviceStorageStats {
        return repository.getDeviceStorageStats()
    }
}
