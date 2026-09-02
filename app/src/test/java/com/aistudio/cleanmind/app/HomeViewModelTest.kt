package com.aistudio.cleanmind.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.presentation.home.AnalysisStatus
import com.aistudio.cleanmind.app.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application

    private class FakeRepository(
        var stats: DeviceStorageStats = DeviceStorageStats(100L * 1024 * 1024 * 1024, 60L * 1024 * 1024 * 1024, 40L * 1024 * 1024 * 1024),
        var analysisResult: Result<StorageAnalysisResult> = Result.success(
            StorageAnalysisResult(
                totalFilesCount = 10,
                totalAnalyzedSizeBytes = 50L * 1024 * 1024,
                categorySummaries = listOf(
                    CategorySummary(StorageCategory.IMAGES, 10, 50L * 1024 * 1024)
                ),
                files = emptyList(),
                deviceStorageStats = DeviceStorageStats(100L * 1024 * 1024 * 1024, 60L * 1024 * 1024 * 1024, 40L * 1024 * 1024 * 1024)
            )
        )
    ) : StorageRepository {
        override suspend fun getDeviceStorageStats(): DeviceStorageStats = stats
        override suspend fun analyzeStorage(): Result<StorageAnalysisResult> = analysisResult
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_hasIdleStatusAndLoadsDeviceStats() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val getStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo)
        val analyzeUseCase = AnalyzeStorageUseCase(fakeRepo)

        val viewModel = HomeViewModel(application, getStatsUseCase, analyzeUseCase, testDispatcher)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalysisStatus.Idle, state.status)
        assertEquals(0, state.totalFilesAnalyzed)
        assertNull(state.totalAnalyzedSpaceFormatted)
        assertTrue(state.categories.isEmpty())
        assertFalse(state.showSettingsDialog)

        // Verifies real device storage stats loaded from StatFs
        assertNotNull(state.deviceTotalSpaceFormatted)
        assertNotNull(state.deviceUsedSpaceFormatted)
        assertNotNull(state.deviceFreeSpaceFormatted)
        assertEquals(0.4f, state.usedPercentage ?: 0f, 0.01f)
    }

    @Test
    fun onAnalyzeRequested_setsRequestingPermissionStatus() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        viewModel.onAnalyzeRequested()
        assertEquals(AnalysisStatus.RequestingPermission, viewModel.uiState.value.status)
    }

    @Test
    fun onPermissionDenied_setsPermissionDeniedStatus() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        viewModel.onPermissionDenied()
        val status = viewModel.uiState.value.status
        assertTrue(status is AnalysisStatus.PermissionDenied)
    }

    @Test
    fun onPermissionGranted_success_populatesAnalyzedData() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        viewModel.onPermissionGranted()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalysisStatus.Success, state.status)
        assertEquals(10, state.totalFilesAnalyzed)
        assertEquals(1, state.categories.size)
        assertEquals(StorageCategory.IMAGES, state.categories.first().category)
        assertEquals(10, state.categories.first().fileCount)
    }

    @Test
    fun onPermissionGranted_failure_setsErrorStatus() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository(
            analysisResult = Result.failure(IllegalStateException("Simulated read error"))
        )
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        viewModel.onPermissionGranted()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val status = state.status
        assertTrue(status is AnalysisStatus.Error)
        assertEquals("Simulated read error", (status as AnalysisStatus.Error).errorMessage)
    }

    @Test
    fun settingsDialog_toggleBehavior() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showSettingsDialog)

        viewModel.onSettingsClicked()
        assertTrue(viewModel.uiState.value.showSettingsDialog)

        viewModel.onDismissSettings()
        assertFalse(viewModel.uiState.value.showSettingsDialog)
    }
}
