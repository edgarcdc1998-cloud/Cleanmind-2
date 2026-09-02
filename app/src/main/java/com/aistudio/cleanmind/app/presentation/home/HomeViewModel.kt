package com.aistudio.cleanmind.app.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.aistudio.cleanmind.app.CleanMindApp
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.domain.model.BackgroundWorkStatus
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.repository.SettingsRepository
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetLatestAnalysisUseCase
import com.aistudio.cleanmind.app.util.StorageFormatter
import com.aistudio.cleanmind.app.worker.WorkManagerScheduler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val getDeviceStorageStatsUseCase: GetDeviceStorageStatsUseCase,
    private val analyzeStorageUseCase: AnalyzeStorageUseCase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val getLatestAnalysisUseCase: GetLatestAnalysisUseCase? = null,
    private val settingsRepository: SettingsRepository? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStorageStats()
        observeSavedAnalysis()
        observeBackgroundWork()
        observeSettings()
    }

    private fun observeSavedAnalysis() {
        val useCase = getLatestAnalysisUseCase ?: return
        viewModelScope.launch(ioDispatcher) {
            useCase().collect { savedAnalysis ->
                if (savedAnalysis != null) {
                    val categoryUis = mapCategorySummaries(savedAnalysis.categorySummaries)
                    val formattedDate = StorageFormatter.formatTimestamp(savedAnalysis.timestampEpochMillis)
                    _uiState.update { state ->
                        state.copy(
                            status = if (state.status is AnalysisStatus.Idle) {
                                AnalysisStatus.Saved(formattedDate)
                            } else if (state.status is AnalysisStatus.Analyzing && state.backgroundWorkStatus == BackgroundWorkStatus.SUCCEEDED) {
                                AnalysisStatus.Success
                            } else {
                                state.status
                            },
                            hasSavedAnalysis = true,
                            lastAnalyzedEpochMillis = savedAnalysis.timestampEpochMillis,
                            lastAnalyzedDateFormatted = formattedDate,
                            totalFilesAnalyzed = savedAnalysis.totalFilesCount,
                            totalAnalyzedSpaceFormatted = StorageFormatter.formatBytes(savedAnalysis.totalAnalyzedSizeBytes),
                            categories = categoryUis,
                            recommendationsSummary = savedAnalysis.recommendationsSummary
                        )
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            hasSavedAnalysis = false,
                            lastAnalyzedEpochMillis = null,
                            lastAnalyzedDateFormatted = null,
                            recommendationsSummary = null
                        )
                    }
                }
            }
        }
    }

    private fun observeBackgroundWork() {
        viewModelScope.launch(ioDispatcher) {
            try {
                WorkManagerScheduler.getManualAnalysisWorkInfoFlow(getApplication()).collect { workInfoList ->
                    val latestWork = workInfoList.firstOrNull()
                    val backgroundStatus = when (latestWork?.state) {
                        WorkInfo.State.ENQUEUED -> BackgroundWorkStatus.ENQUEUED
                        WorkInfo.State.RUNNING -> BackgroundWorkStatus.RUNNING
                        WorkInfo.State.SUCCEEDED -> BackgroundWorkStatus.SUCCEEDED
                        WorkInfo.State.FAILED -> BackgroundWorkStatus.FAILED
                        WorkInfo.State.CANCELLED -> BackgroundWorkStatus.CANCELLED
                        WorkInfo.State.BLOCKED -> BackgroundWorkStatus.ENQUEUED
                        null -> BackgroundWorkStatus.IDLE
                    }

                    _uiState.update { state ->
                        val updatedStatus = when (backgroundStatus) {
                            BackgroundWorkStatus.ENQUEUED,
                            BackgroundWorkStatus.RUNNING -> AnalysisStatus.Analyzing
                            BackgroundWorkStatus.SUCCEEDED -> {
                                if (state.status is AnalysisStatus.Analyzing) AnalysisStatus.Success else state.status
                            }
                            BackgroundWorkStatus.FAILED -> {
                                val errorMsg = latestWork?.outputData?.getString(com.aistudio.cleanmind.app.worker.StorageAnalysisWorker.KEY_ERROR_MESSAGE)
                                AnalysisStatus.Error(errorMsg ?: "Falha na análise em segundo plano.")
                            }
                            BackgroundWorkStatus.CANCELLED -> {
                                if (state.status is AnalysisStatus.Analyzing) {
                                    if (state.hasSavedAnalysis && state.lastAnalyzedDateFormatted != null) {
                                        AnalysisStatus.Saved(state.lastAnalyzedDateFormatted)
                                    } else {
                                        AnalysisStatus.Idle
                                    }
                                } else {
                                    state.status
                                }
                            }
                            BackgroundWorkStatus.IDLE -> state.status
                        }

                        state.copy(
                            backgroundWorkStatus = backgroundStatus,
                            status = updatedStatus
                        )
                    }

                    // Se a tarefa concluiu com sucesso, recarrega estatísticas do disco
                    if (latestWork?.state == WorkInfo.State.SUCCEEDED) {
                        loadDeviceStorageStats()
                    }
                }
            } catch (_: Exception) {
                // WorkManager may not be initialized in some test environments without test helper
            }
        }
    }

    private fun observeSettings() {
        val repo = settingsRepository ?: return
        viewModelScope.launch(ioDispatcher) {
            repo.isAutoAnalysisEnabled().collect { enabled ->
                _uiState.update { it.copy(isAutoAnalysisEnabled = enabled) }
            }
        }
        viewModelScope.launch(ioDispatcher) {
            repo.getAutoAnalysisIntervalHours().collect { hours ->
                _uiState.update { it.copy(autoAnalysisIntervalHours = hours) }
            }
        }
    }

    fun loadDeviceStorageStats() {
        viewModelScope.launch(ioDispatcher) {
            val stats = getDeviceStorageStatsUseCase()
            if (stats.totalBytes > 0) {
                _uiState.update { state ->
                    state.copy(
                        deviceTotalSpaceFormatted = StorageFormatter.formatBytes(stats.totalBytes),
                        deviceUsedSpaceFormatted = StorageFormatter.formatBytes(stats.usedBytes),
                        deviceFreeSpaceFormatted = StorageFormatter.formatBytes(stats.freeBytes),
                        usedPercentage = stats.usedPercentage
                    )
                }
            }
        }
    }

    fun onAnalyzeRequested() {
        _uiState.update { it.copy(status = AnalysisStatus.RequestingPermission) }
    }

    fun onPermissionGranted() {
        startStorageAnalysis()
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                status = AnalysisStatus.PermissionDenied(
                    message = "Permissão de leitura não concedida. Não foi possível acessar as mídias."
                )
            )
        }
    }

    fun onPersistenceError(message: String) {
        _uiState.update { it.copy(status = AnalysisStatus.PersistenceError(message)) }
    }

    fun onFilterSelected(filter: RecommendationFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    /**
     * Inicia a análise delegando ao WorkManager para execução robusta e desacoplada em segundo plano.
     */
    fun startStorageAnalysis() {
        _uiState.update {
            it.copy(
                status = AnalysisStatus.Analyzing,
                backgroundWorkStatus = BackgroundWorkStatus.ENQUEUED
            )
        }
        try {
            WorkManagerScheduler.scheduleManualAnalysis(getApplication())
        } catch (_: Exception) {
            // Fallback direto via Coroutine caso WorkManager não esteja disponível no ambiente
            runDirectAnalysisFallback()
        }
    }

    fun cancelAnalysis() {
        try {
            WorkManagerScheduler.cancelManualAnalysis(getApplication())
        } catch (_: Exception) {
            // Safe fallback
        }
        _uiState.update { state ->
            val fallbackStatus = if (state.hasSavedAnalysis && state.lastAnalyzedDateFormatted != null) {
                AnalysisStatus.Saved(state.lastAnalyzedDateFormatted)
            } else {
                AnalysisStatus.Idle
            }
            state.copy(
                status = fallbackStatus,
                backgroundWorkStatus = BackgroundWorkStatus.CANCELLED
            )
        }
    }

    fun onToggleAutoAnalysis(enabled: Boolean) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository?.setAutoAnalysisEnabled(enabled)
            val hours = _uiState.value.autoAnalysisIntervalHours
            if (enabled) {
                WorkManagerScheduler.schedulePeriodicAnalysis(getApplication(), hours.toLong())
            } else {
                WorkManagerScheduler.cancelPeriodicAnalysis(getApplication())
            }
        }
    }

    fun onSetAutoAnalysisInterval(hours: Int) {
        viewModelScope.launch(ioDispatcher) {
            settingsRepository?.setAutoAnalysisIntervalHours(hours)
            if (_uiState.value.isAutoAnalysisEnabled) {
                WorkManagerScheduler.schedulePeriodicAnalysis(getApplication(), hours.toLong())
            }
        }
    }

    private fun runDirectAnalysisFallback() {
        viewModelScope.launch(ioDispatcher) {
            val result = analyzeStorageUseCase()
            result.onSuccess { analysisResult ->
                val categoryUis = mapCategorySummaries(analysisResult.categorySummaries)
                val formattedDate = StorageFormatter.formatTimestamp(analysisResult.timestampEpochMillis)
                _uiState.update { state ->
                    state.copy(
                        status = AnalysisStatus.Success,
                        backgroundWorkStatus = BackgroundWorkStatus.SUCCEEDED,
                        hasSavedAnalysis = true,
                        lastAnalyzedEpochMillis = analysisResult.timestampEpochMillis,
                        lastAnalyzedDateFormatted = formattedDate,
                        totalFilesAnalyzed = analysisResult.totalFilesCount,
                        totalAnalyzedSpaceFormatted = StorageFormatter.formatBytes(analysisResult.totalAnalyzedSizeBytes),
                        categories = categoryUis,
                        deviceTotalSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.totalBytes),
                        deviceUsedSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.usedBytes),
                        deviceFreeSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.freeBytes),
                        usedPercentage = analysisResult.deviceStorageStats.usedPercentage,
                        recommendationsSummary = analysisResult.recommendationsSummary
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        status = AnalysisStatus.Error(
                            errorMessage = throwable.localizedMessage ?: "Erro desconhecido ao analisar armazenamento."
                        ),
                        backgroundWorkStatus = BackgroundWorkStatus.FAILED
                    )
                }
            }
        }
    }

    private fun mapCategorySummaries(summaries: List<CategorySummary>): List<CategorySummaryUi> {
        return summaries.map { summary ->
            val titleRes = when (summary.category) {
                StorageCategory.IMAGES -> R.string.category_images
                StorageCategory.VIDEOS -> R.string.category_videos
                StorageCategory.AUDIOS -> R.string.category_audios
                StorageCategory.DOCUMENTS -> R.string.category_documents
                StorageCategory.LARGE_FILES -> R.string.category_large_files
                StorageCategory.OTHERS -> R.string.category_others
            }
            CategorySummaryUi(
                category = summary.category,
                titleResId = titleRes,
                fileCount = summary.fileCount,
                formattedSize = StorageFormatter.formatBytes(summary.totalSizeBytes)
            )
        }
    }

    fun onSettingsClicked() {
        _uiState.update { it.copy(showSettingsDialog = true) }
    }

    fun onDismissSettings() {
        _uiState.update { it.copy(showSettingsDialog = false) }
    }

    fun onShowPermissionRationale() {
        _uiState.update { it.copy(showPermissionRationaleDialog = true) }
    }

    fun onDismissPermissionRationale() {
        _uiState.update { it.copy(showPermissionRationaleDialog = false) }
    }

    fun onToggleRecommendationSelection(recommendationId: Long) {
        _uiState.update { state ->
            val updated = state.selectedRecommendationIds.toMutableSet()
            if (updated.contains(recommendationId)) {
                updated.remove(recommendationId)
            } else {
                updated.add(recommendationId)
            }
            state.copy(selectedRecommendationIds = updated)
        }
    }

    fun onSelectAllRecommendations() {
        _uiState.update { state ->
            val allIds = state.filteredRecommendations.map { it.id }.toSet()
            state.copy(selectedRecommendationIds = allIds)
        }
    }

    fun onClearRecommendationSelection() {
        _uiState.update { it.copy(selectedRecommendationIds = emptySet()) }
    }

    fun onShowReviewConfirmation() {
        _uiState.update { it.copy(showReviewConfirmationDialog = true) }
    }

    fun onDismissReviewConfirmation() {
        _uiState.update { it.copy(showReviewConfirmationDialog = false) }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = CleanMindApp.getAppContainer(application)

                    return HomeViewModel(
                        application = application,
                        getDeviceStorageStatsUseCase = container.getDeviceStorageStatsUseCase,
                        analyzeStorageUseCase = container.analyzeStorageUseCase,
                        getLatestAnalysisUseCase = container.getLatestAnalysisUseCase,
                        settingsRepository = container.settingsRepository
                    ) as T
                }
            }
    }
}

