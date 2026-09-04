package com.aistudio.cleanmind.app.presentation.screens.dashboard

import com.aistudio.cleanmind.app.domain.model.SavingsDashboardData

sealed interface SavingsDashboardUiState {
    data object Loading : SavingsDashboardUiState
    data object Empty : SavingsDashboardUiState
    data class Success(
        val data: SavingsDashboardData,
        val formattedLatestDate: String,
        val formattedLatestAnalyzedSize: String,
        val formattedTotalVolumeAnalyzed: String,
        val formattedLatestPotentialReclaimable: String,
        val formattedTotalPotentialReclaimable: String,
        val selectedPeriodType: PeriodViewType = PeriodViewType.OBSERVATIONS
    ) : SavingsDashboardUiState
    data class Error(val message: String) : SavingsDashboardUiState
}

enum class PeriodViewType {
    OBSERVATIONS,
    WEEKLY,
    MONTHLY
}
