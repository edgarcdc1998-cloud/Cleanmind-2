package com.aistudio.cleanmind.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.aistudio.cleanmind.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class SettingsRepositoryImpl(
    context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : SettingsRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _autoAnalysisEnabled = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_ANALYSIS_ENABLED, DEFAULT_AUTO_ANALYSIS_ENABLED)
    )

    private val _autoAnalysisIntervalHours = MutableStateFlow(
        prefs.getInt(KEY_AUTO_ANALYSIS_INTERVAL_HOURS, DEFAULT_INTERVAL_HOURS)
    )

    override fun isAutoAnalysisEnabled(): Flow<Boolean> = _autoAnalysisEnabled.asStateFlow()

    override suspend fun setAutoAnalysisEnabled(enabled: Boolean) = withContext(ioDispatcher) {
        prefs.edit().putBoolean(KEY_AUTO_ANALYSIS_ENABLED, enabled).apply()
        _autoAnalysisEnabled.value = enabled
    }

    override fun getAutoAnalysisIntervalHours(): Flow<Int> = _autoAnalysisIntervalHours.asStateFlow()

    override suspend fun setAutoAnalysisIntervalHours(hours: Int) = withContext(ioDispatcher) {
        val validHours = if (hours >= 24) hours else 24
        prefs.edit().putInt(KEY_AUTO_ANALYSIS_INTERVAL_HOURS, validHours).apply()
        _autoAnalysisIntervalHours.value = validHours
    }

    companion object {
        private const val PREFS_NAME = "cleanmind_settings_prefs"
        private const val KEY_AUTO_ANALYSIS_ENABLED = "key_auto_analysis_enabled"
        private const val KEY_AUTO_ANALYSIS_INTERVAL_HOURS = "key_auto_analysis_interval_hours"

        const val DEFAULT_AUTO_ANALYSIS_ENABLED = false
        const val DEFAULT_INTERVAL_HOURS = 24 // 24 hours (Daily)
        const val WEEKLY_INTERVAL_HOURS = 168 // 7 days (Weekly)
    }
}
