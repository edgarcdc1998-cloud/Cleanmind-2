package com.aistudio.cleanmind.app.presentation.screens.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aistudio.cleanmind.app.CleanMindApp
import com.aistudio.cleanmind.app.domain.model.SavingsDashboardData
import com.aistudio.cleanmind.app.domain.usecase.ClearAnalysisHistoryUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetStorageSavingsDashboardUseCase
import com.aistudio.cleanmind.app.util.StorageFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SavingsDashboardViewModel(
    application: Application,
    private val getStorageSavingsDashboardUseCase: GetStorageSavingsDashboardUseCase,
    private val clearAnalysisHistoryUseCase: ClearAnalysisHistoryUseCase? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<SavingsDashboardUiState>(SavingsDashboardUiState.Loading)
    val uiState: StateFlow<SavingsDashboardUiState> = _uiState.asStateFlow()

    private var currentPeriodType: PeriodViewType = PeriodViewType.OBSERVATIONS

    init {
        loadDashboardData()
    }

    fun loadDashboardData() {
        _uiState.value = SavingsDashboardUiState.Loading
        viewModelScope.launch(ioDispatcher) {
            getStorageSavingsDashboardUseCase()
                .catch { exception ->
                    _uiState.value = SavingsDashboardUiState.Error(
                        exception.message ?: "Falha ao carregar métricas de economia de armazenamento."
                    )
                }
                .collect { data ->
                    if (data.totalAnalysesCount == 0) {
                        _uiState.value = SavingsDashboardUiState.Empty
                    } else {
                        _uiState.value = mapSuccessState(data, currentPeriodType)
                    }
                }
        }
    }

    fun onSelectPeriodType(periodType: PeriodViewType) {
        currentPeriodType = periodType
        val current = _uiState.value
        if (current is SavingsDashboardUiState.Success) {
            _uiState.update {
                (it as SavingsDashboardUiState.Success).copy(selectedPeriodType = periodType)
            }
        }
    }

    fun clearHistory() {
        val useCase = clearAnalysisHistoryUseCase ?: return
        viewModelScope.launch(ioDispatcher) {
            useCase()
        }
    }

    private fun mapSuccessState(
        data: SavingsDashboardData,
        periodType: PeriodViewType
    ): SavingsDashboardUiState.Success {
        val dateFormatted = data.latestAnalysisEpochMillis?.let {
            StorageFormatter.formatTimestamp(it)
        } ?: "--"

        return SavingsDashboardUiState.Success(
            data = data,
            formattedLatestDate = dateFormatted,
            formattedLatestAnalyzedSize = StorageFormatter.formatBytes(data.latestAnalyzedSizeBytes),
            formattedTotalVolumeAnalyzed = StorageFormatter.formatBytes(data.totalVolumeAnalyzedBytes),
            formattedLatestPotentialReclaimable = StorageFormatter.formatBytes(data.latestPotentialReclaimableBytes),
            formattedTotalPotentialReclaimable = StorageFormatter.formatBytes(data.totalPotentialReclaimableBytes),
            selectedPeriodType = periodType
        )
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val container = CleanMindApp.getAppContainer(application)
                    return SavingsDashboardViewModel(
                        application = application,
                        getStorageSavingsDashboardUseCase = container.getStorageSavingsDashboardUseCase,
                        clearAnalysisHistoryUseCase = container.clearAnalysisHistoryUseCase
                    ) as T
                }
            }
    }
}
