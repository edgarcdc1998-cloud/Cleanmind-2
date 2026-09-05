package com.aistudio.cleanmind.app.presentation.home

import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.BackgroundWorkStatus
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.util.StorageFormatter

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
    val recommendationsSummary: AnalysisRecommendationsSummary? = null,
    val selectedFilter: RecommendationFilter = RecommendationFilter.ALL,
    val backgroundWorkStatus: BackgroundWorkStatus = BackgroundWorkStatus.IDLE,
    val isAutoAnalysisEnabled: Boolean = false,
    val autoAnalysisIntervalHours: Int = 24,
    val showSettingsDialog: Boolean = false,
    val showPermissionRationaleDialog: Boolean = false,
    val selectedRecommendationIds: Set<Long> = emptySet(),
    val showReviewConfirmationDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deletionSummary: DeletionSummaryUi? = null,
    val pendingIntentSender: android.content.IntentSender? = null
) {
    val isAnalyzed: Boolean
        get() = status is AnalysisStatus.Success || status is AnalysisStatus.Saved || hasSavedAnalysis

    val isAnalyzing: Boolean
        get() = status is AnalysisStatus.Analyzing || backgroundWorkStatus == BackgroundWorkStatus.RUNNING || backgroundWorkStatus == BackgroundWorkStatus.ENQUEUED

    val isBackgroundWorkActive: Boolean
        get() = backgroundWorkStatus == BackgroundWorkStatus.ENQUEUED || backgroundWorkStatus == BackgroundWorkStatus.RUNNING

    val filteredRecommendations: List<CleanupRecommendation>
        get() {
            val list = recommendationsSummary?.recommendations ?: emptyList()
            return when (selectedFilter) {
                RecommendationFilter.ALL -> list
                RecommendationFilter.DUPLICATES -> list.filter { it.type == RecommendationType.DUPLICATE }
                RecommendationFilter.LARGE_FILES -> list.filter { it.type == RecommendationType.LARGE_FILE }
                RecommendationFilter.OLD_FILES -> list.filter { it.type == RecommendationType.OLD_FILE }
                RecommendationFilter.TEMPORARY_FILES -> list.filter { it.type == RecommendationType.TEMPORARY_FILE }
            }
        }

    val selectedRecommendations: List<CleanupRecommendation>
        get() {
            val list = recommendationsSummary?.recommendations ?: emptyList()
            return list.filter { selectedRecommendationIds.contains(it.id) }
        }

    val selectedReclaimableBytes: Long
        get() = selectedRecommendations.sumOf { it.reclaimableSizeBytes }

    val selectedReclaimableSpaceFormatted: String
        get() = StorageFormatter.formatBytes(selectedReclaimableBytes)

    val potentialReclaimableSpaceFormatted: String?
        get() = recommendationsSummary?.potentialReclaimableBytes?.let { StorageFormatter.formatBytes(it) }
}

data class DeletionSummaryUi(
    val deletedCount: Int,
    val reclaimedBytes: Long,
    val reclaimedSpaceFormatted: String,
    val failedFileNames: List<String>
)
