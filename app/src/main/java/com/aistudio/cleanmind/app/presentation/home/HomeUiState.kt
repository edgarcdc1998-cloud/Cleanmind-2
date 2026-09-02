package com.aistudio.cleanmind.app.presentation.home

import com.aistudio.cleanmind.app.domain.model.StorageCategory

sealed interface AnalysisStatus {
    data object Idle : AnalysisStatus
    data class Saved(val lastAnalyzedDateFormatted: String) : AnalysisStatus
    data object RequestingPermission : AnalysisStatus
    data object Analyzing : AnalysisStatus
    data object Success : AnalysisStatus
    data class PermissionDenied(val message: String? = null) : AnalysisStatus
    data class PersistenceError(val errorMessage: String) : AnalysisStatus
    data class Error(val errorMessage: String) : AnalysisStatus
}

data class CategorySummaryUi(
    val category: StorageCategory,
    val titleResId: Int,
    val fileCount: Int,
    val formattedSize: String
)

data class HomeUiState(
    val status: AnalysisStatus = AnalysisStatus.Idle,
    val hasSavedAnalysis: Boolean = false,
    val lastAnalyzedEpochMillis: Long? = null,
    val lastAnalyzedDateFormatted: String? = null,
    val deviceTotalSpaceFormatted: String? = null,
    val deviceUsedSpaceFormatted: String? = null,
    val deviceFreeSpaceFormatted: String? = null,
    val usedPercentage: Float? = null,
    val totalFilesAnalyzed: Int = 0,
    val totalAnalyzedSpaceFormatted: String? = null,
    val categories: List<CategorySummaryUi> = emptyList(),
    val showSettingsDialog: Boolean = false,
    val showPermissionRationaleDialog: Boolean = false
) {
    val isAnalyzed: Boolean
        get() = status is AnalysisStatus.Success || status is AnalysisStatus.Saved || hasSavedAnalysis

    val isAnalyzing: Boolean
        get() = status is AnalysisStatus.Analyzing
}
