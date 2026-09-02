package com.aistudio.cleanmind.app.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.data.datasource.MediaStoreDataSourceImpl
import com.aistudio.cleanmind.app.data.local.database.CleanMindDatabase
import com.aistudio.cleanmind.app.data.repository.AnalysisHistoryRepositoryImpl
import com.aistudio.cleanmind.app.data.repository.StorageRepositoryImpl
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetLatestAnalysisUseCase
import com.aistudio.cleanmind.app.util.StorageFormatter
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
    private val getLatestAnalysisUseCase: GetLatestAnalysisUseCase? = null
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStorageStats()
        observeSavedAnalysis()
    }

    private fun observeSavedAnalysis() {
        val useCase = getLatestAnalysisUseCase ?: return
        viewModelScope.launch(ioDispatcher) {
            useCase().collect { savedAnalysis ->
                if (savedAnalysis != null) {
                    val categoryUis = mapCategorySummaries(savedAnalysis.categorySummaries)
                    val formattedDate = StorageFormatter.formatTimestamp(savedAnalysis.timestampEpochMillis)
                    _uiState.update { state ->
                        if (state.status !is AnalysisStatus.Analyzing) {
                            state.copy(
                                status = if (state.status is AnalysisStatus.Idle) {
                                    AnalysisStatus.Saved(formattedDate)
                                } else {
                                    state.status
                                },
                                hasSavedAnalysis = true,
                                lastAnalyzedEpochMillis = savedAnalysis.timestampEpochMillis,
                                lastAnalyzedDateFormatted = formattedDate,
                                totalFilesAnalyzed = savedAnalysis.totalFilesCount,
                                totalAnalyzedSpaceFormatted = StorageFormatter.formatBytes(savedAnalysis.totalAnalyzedSizeBytes),
                                categories = categoryUis
                            )
                        } else {
                            state
                        }
                    }
                } else {
                    _uiState.update { state ->
                        state.copy(
                            hasSavedAnalysis = false,
                            lastAnalyzedEpochMillis = null,
                            lastAnalyzedDateFormatted = null
                        )
                    }
                }
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

    fun startStorageAnalysis() {
        viewModelScope.launch(ioDispatcher) {
            _uiState.update { it.copy(status = AnalysisStatus.Analyzing) }

            val result = analyzeStorageUseCase()
            result.onSuccess { analysisResult ->
                val categoryUis = mapCategorySummaries(analysisResult.categorySummaries)
                val formattedDate = StorageFormatter.formatTimestamp(analysisResult.timestampEpochMillis)
                _uiState.update { state ->
                    state.copy(
                        status = AnalysisStatus.Success,
                        hasSavedAnalysis = true,
                        lastAnalyzedEpochMillis = analysisResult.timestampEpochMillis,
                        lastAnalyzedDateFormatted = formattedDate,
                        totalFilesAnalyzed = analysisResult.totalFilesCount,
                        totalAnalyzedSpaceFormatted = StorageFormatter.formatBytes(analysisResult.totalAnalyzedSizeBytes),
                        categories = categoryUis,
                        deviceTotalSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.totalBytes),
                        deviceUsedSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.usedBytes),
                        deviceFreeSpaceFormatted = StorageFormatter.formatBytes(analysisResult.deviceStorageStats.freeBytes),
                        usedPercentage = analysisResult.deviceStorageStats.usedPercentage
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { state ->
                    state.copy(
                        status = AnalysisStatus.Error(
                            errorMessage = throwable.localizedMessage ?: "Erro desconhecido ao analisar armazenamento."
                        )
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

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = CleanMindDatabase.getInstance(application.applicationContext)
                    val analysisDao = db.analysisDao()
                    val historyRepository = AnalysisHistoryRepositoryImpl(analysisDao)
                    val dataSource = MediaStoreDataSourceImpl(application.applicationContext)
                    val storageRepository = StorageRepositoryImpl(dataSource)
                    val getStatsUseCase = GetDeviceStorageStatsUseCase(storageRepository)
                    val analyzeUseCase = AnalyzeStorageUseCase(storageRepository, historyRepository)
                    val getLatestAnalysisUseCase = GetLatestAnalysisUseCase(historyRepository)
                    return HomeViewModel(
                        application = application,
                        getDeviceStorageStatsUseCase = getStatsUseCase,
                        analyzeStorageUseCase = analyzeUseCase,
                        getLatestAnalysisUseCase = getLatestAnalysisUseCase
                    ) as T
                }
            }
    }
}
