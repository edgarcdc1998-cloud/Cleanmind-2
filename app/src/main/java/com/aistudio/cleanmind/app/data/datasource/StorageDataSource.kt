package com.aistudio.cleanmind.app.data.datasource

import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageFile

interface StorageDataSource {
    fun getDeviceStorageStats(): DeviceStorageStats
    suspend fun queryMediaFiles(): List<StorageFile>
}
