package com.aistudio.cleanmind.app.presentation.screens.dashboard

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.domain.usecase.ClearAnalysisHistoryUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetStorageSavingsDashboardUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SavingsDashboardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
    }

    private class FakeHistoryRepository(
        private val analysesFlow: Flow<List<StorageAnalysisResult>>
    ) : AnalysisHistoryRepository {
        var clearCalled = false

        override fun getLatestAnalysis(): Flow<StorageAnalysisResult?> = flowOf(null)
        override fun getAllAnalyses(): Flow<List<StorageAnalysisResult>> = analysesFlow
        override suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long> = Result.success(1L)
        override fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>> = flowOf(emptyList())
        override suspend fun clearHistory(): Result<Unit> {
            clearCalled = true
            return Result.success(Unit)
        }
    }

    @Test
    fun initialState_withEmptyHistory_transitionsToEmpty() = runTest(testDispatcher) {
        val repo = FakeHistoryRepository(flowOf(emptyList()))
        val getDashboardUseCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)
        val clearUseCase = ClearAnalysisHistoryUseCase(repo)

        val viewModel = SavingsDashboardViewModel(
            application = application,
            getStorageSavingsDashboardUseCase = getDashboardUseCase,
            clearAnalysisHistoryUseCase = clearUseCase,
            ioDispatcher = testDispatcher
        )

        assertTrue(viewModel.uiState.value is SavingsDashboardUiState.Empty)
    }

    @Test
    fun initialState_withData_transitionsToSuccess() = runTest(testDispatcher) {
        val analysis = StorageAnalysisResult(
            id = 1L,
            totalFilesCount = 10,
            totalAnalyzedSizeBytes = 100_000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(1_000_000L, 500_000L, 500_000L),
            timestampEpochMillis = 1700000000000L,
            recommendationsSummary = AnalysisRecommendationsSummary(
                totalRecommendationsCount = 3,
                potentialReclaimableBytes = 25_000L,
                largeFilesCount = 1,
                duplicateFilesCount = 2,
                duplicateGroupsCount = 1,
                oldFilesCount = 0,
                temporaryFilesCount = 0,
                recommendations = emptyList(),
                duplicateGroups = emptyList()
            )
        )
        val repo = FakeHistoryRepository(flowOf(listOf(analysis)))
        val getDashboardUseCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)
        val clearUseCase = ClearAnalysisHistoryUseCase(repo)

        val viewModel = SavingsDashboardViewModel(
            application = application,
            getStorageSavingsDashboardUseCase = getDashboardUseCase,
            clearAnalysisHistoryUseCase = clearUseCase,
            ioDispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertTrue(state is SavingsDashboardUiState.Success)
        val success = state as SavingsDashboardUiState.Success
        assertEquals(1, success.data.totalAnalysesCount)
        assertEquals(PeriodViewType.OBSERVATIONS, success.selectedPeriodType)
    }

    @Test
    fun onSelectPeriodType_updatesSelectedPeriodInSuccessState() = runTest(testDispatcher) {
        val analysis = StorageAnalysisResult(
            id = 1L,
            totalFilesCount = 5,
            totalAnalyzedSizeBytes = 20_000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(100_000L, 50_000L, 50_000L),
            timestampEpochMillis = 1700000000000L
        )
        val repo = FakeHistoryRepository(flowOf(listOf(analysis)))
        val getDashboardUseCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val viewModel = SavingsDashboardViewModel(
            application = application,
            getStorageSavingsDashboardUseCase = getDashboardUseCase,
            ioDispatcher = testDispatcher
        )

        viewModel.onSelectPeriodType(PeriodViewType.WEEKLY)
        val state = viewModel.uiState.value as SavingsDashboardUiState.Success
        assertEquals(PeriodViewType.WEEKLY, state.selectedPeriodType)
    }

    @Test
    fun clearHistory_callsClearUseCase() = runTest(testDispatcher) {
        val repo = FakeHistoryRepository(flowOf(emptyList()))
        val getDashboardUseCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)
        val clearUseCase = ClearAnalysisHistoryUseCase(repo)

        val viewModel = SavingsDashboardViewModel(
            application = application,
            getStorageSavingsDashboardUseCase = getDashboardUseCase,
            clearAnalysisHistoryUseCase = clearUseCase,
            ioDispatcher = testDispatcher
        )

        viewModel.clearHistory()
        assertTrue(repo.clearCalled)
    }

    @Test
    fun errorInFlow_transitionsToErrorState() = runTest(testDispatcher) {
        val failingFlow = flow<List<StorageAnalysisResult>> {
            throw RuntimeException("Database read error")
        }
        val repo = FakeHistoryRepository(failingFlow)
        val getDashboardUseCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val viewModel = SavingsDashboardViewModel(
            application = application,
            getStorageSavingsDashboardUseCase = getDashboardUseCase,
            ioDispatcher = testDispatcher
        )

        val state = viewModel.uiState.value
        assertTrue(state is SavingsDashboardUiState.Error)
        assertEquals("Database read error", (state as SavingsDashboardUiState.Error).message)
    }
}
