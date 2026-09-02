package com.aistudio.cleanmind.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun isAutoAnalysisEnabled(): Flow<Boolean>
    suspend fun setAutoAnalysisEnabled(enabled: Boolean)

    fun getAutoAnalysisIntervalHours(): Flow<Int>
    suspend fun setAutoAnalysisIntervalHours(hours: Int)
}
