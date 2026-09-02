package com.aistudio.cleanmind.app.presentation.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.cleanmind.app.R
import com.aistudio.cleanmind.app.data.datasource.MediaStoreDataSourceImpl
import com.aistudio.cleanmind.app.data.repository.StorageRepositoryImpl
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.util.StorageFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val getDeviceStorageStatsUseCase: GetDeviceStorageStatsUseCase,
    private val analyzeStorageUseCase: AnalyzeStorageUseCase
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDeviceStorageStats()
    }

    fun loadDeviceStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
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

    fun startStorageAnalysis() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(status = AnalysisStatus.Analyzing) }

            val result = analyzeStorageUseCase()
            result.onSuccess { analysisResult ->
                val categoryUis = mapCategorySummaries(analysisResult.categorySummaries)
                _uiState.update { state ->
                    state.copy(
                        status = AnalysisStatus.Success,
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
                    val dataSource = MediaStoreDataSourceImpl(application.applicationContext)
                    val repository = StorageRepositoryImpl(dataSource)
                    val getStatsUseCase = GetDeviceStorageStatsUseCase(repository)
                    val analyzeUseCase = AnalyzeStorageUseCase(repository)
                    return HomeViewModel(
                        application = application,
                        getDeviceStorageStatsUseCase = getStatsUseCase,
                        analyzeStorageUseCase = analyzeUseCase
                    ) as T
                }
            }
    }
}
