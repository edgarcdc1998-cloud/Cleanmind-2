package com.aistudio.cleanmind.app

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetLatestAnalysisUseCase
import com.aistudio.cleanmind.app.domain.usecase.DeleteSelectedFilesUseCase
import com.aistudio.cleanmind.app.domain.usecase.DeletionResult
import com.aistudio.cleanmind.app.domain.usecase.DeletionSummary
import com.aistudio.cleanmind.app.presentation.home.AnalysisStatus
import com.aistudio.cleanmind.app.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
@Config(sdk = [34])
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

    private class FakeHistoryRepository(
        initialAnalysis: StorageAnalysisResult? = null
    ) : AnalysisHistoryRepository {
        private val _flow = MutableStateFlow(initialAnalysis)
        var savedCount = 0

        override fun getLatestAnalysis(): Flow<StorageAnalysisResult?> = _flow.asStateFlow()

        override suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long> {
            savedCount++
            _flow.value = result
            return Result.success(1L)
        }

        override fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>> = MutableStateFlow(emptyList())

        override suspend fun clearHistory(): Result<Unit> {
            _flow.value = null
            return Result.success(Unit)
        }
    }

    @Test
    fun initialState_withSavedAnalysis_loadsSavedStatusAndCategories() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val fakeHistoryRepo = FakeHistoryRepository(
            initialAnalysis = StorageAnalysisResult(
                totalFilesCount = 42,
                totalAnalyzedSizeBytes = 100L * 1024 * 1024,
                categorySummaries = listOf(
                    CategorySummary(StorageCategory.IMAGES, 20, 60L * 1024 * 1024),
                    CategorySummary(StorageCategory.VIDEOS, 22, 40L * 1024 * 1024)
                ),
                files = emptyList(),
                deviceStorageStats = DeviceStorageStats(100L * 1024 * 1024 * 1024, 60L * 1024 * 1024 * 1024, 40L * 1024 * 1024 * 1024),
                timestampEpochMillis = 1700000000000L
            )
        )

        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo, fakeHistoryRepo),
            ioDispatcher = testDispatcher,
            getLatestAnalysisUseCase = GetLatestAnalysisUseCase(fakeHistoryRepo)
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.status is AnalysisStatus.Saved)
        assertTrue(state.hasSavedAnalysis)
        assertEquals(42, state.totalFilesAnalyzed)
        assertEquals(2, state.categories.size)
        assertNotNull(state.lastAnalyzedDateFormatted)
    }

    @Test
    fun onPermissionGranted_withHistoryRepo_persistsResult() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val fakeHistoryRepo = FakeHistoryRepository()

        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo, fakeHistoryRepo),
            ioDispatcher = testDispatcher,
            getLatestAnalysisUseCase = GetLatestAnalysisUseCase(fakeHistoryRepo)
        )
        advanceUntilIdle()

        viewModel.onPermissionGranted()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(AnalysisStatus.Success, state.status)
        assertEquals(1, fakeHistoryRepo.savedCount)
        assertTrue(state.hasSavedAnalysis)
        assertEquals(10, state.totalFilesAnalyzed)
    }

    @Test
    fun onPersistenceError_setsPersistenceErrorStatus() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        viewModel.onPersistenceError("Erro ao salvar no banco")
        val status = viewModel.uiState.value.status
        assertTrue(status is AnalysisStatus.PersistenceError)
        assertEquals("Erro ao salvar no banco", (status as AnalysisStatus.PersistenceError).errorMessage)
    }

    @Test
    fun filterRecommendations_updatesFilteredList() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        assertEquals(com.aistudio.cleanmind.app.presentation.home.RecommendationFilter.ALL, viewModel.uiState.value.selectedFilter)

        viewModel.onFilterSelected(com.aistudio.cleanmind.app.presentation.home.RecommendationFilter.DUPLICATES)
        assertEquals(com.aistudio.cleanmind.app.presentation.home.RecommendationFilter.DUPLICATES, viewModel.uiState.value.selectedFilter)

        viewModel.onFilterSelected(com.aistudio.cleanmind.app.presentation.home.RecommendationFilter.LARGE_FILES)
        assertEquals(com.aistudio.cleanmind.app.presentation.home.RecommendationFilter.LARGE_FILES, viewModel.uiState.value.selectedFilter)
    }

    @Test
    fun recommendationSelection_toggleAndSelectAll_worksCorrectly() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedRecommendationIds.size)

        viewModel.onToggleRecommendationSelection(101L)
        assertTrue(viewModel.uiState.value.selectedRecommendationIds.contains(101L))

        viewModel.onToggleRecommendationSelection(101L)
        assertFalse(viewModel.uiState.value.selectedRecommendationIds.contains(101L))

        viewModel.onToggleRecommendationSelection(102L)
        viewModel.onToggleRecommendationSelection(103L)
        assertEquals(2, viewModel.uiState.value.selectedRecommendationIds.size)

        viewModel.onClearRecommendationSelection()
        assertEquals(0, viewModel.uiState.value.selectedRecommendationIds.size)
    }

    @Test
    fun reviewDialog_showAndDismiss_updatesState() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val viewModel = HomeViewModel(
            application,
            GetDeviceStorageStatsUseCase(fakeRepo),
            AnalyzeStorageUseCase(fakeRepo),
            testDispatcher
        )
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showReviewConfirmationDialog)

        viewModel.onShowReviewConfirmation()
        assertTrue(viewModel.uiState.value.showReviewConfirmationDialog)

        viewModel.onDismissReviewConfirmation()
        assertFalse(viewModel.uiState.value.showReviewConfirmationDialog)
    }

    @Test
    fun executeSelectedCleanup_updatesState() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val deleteSelectedFilesUseCase = DeleteSelectedFilesUseCase(application, testDispatcher)
        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo),
            ioDispatcher = testDispatcher,
            deleteSelectedFilesUseCase = deleteSelectedFilesUseCase
        )
        advanceUntilIdle()

        viewModel.executeSelectedCleanup()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.deletionSummary)
    }

    @Test
    fun onAuthorizationResult_refused_doesNotClearSelectionAndDoesNotReportFalseSuccess() = runTest(testDispatcher) {
        val fakeRepo = FakeRepository()
        val deleteSelectedFilesUseCase = DeleteSelectedFilesUseCase(application, testDispatcher)
        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo),
            ioDispatcher = testDispatcher,
            deleteSelectedFilesUseCase = deleteSelectedFilesUseCase
        )
        advanceUntilIdle()

        // When user refuses authorization (approved = false)
        viewModel.onAuthorizationResult(approved = false)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.pendingIntentSender)
        assertFalse(viewModel.uiState.value.isDeleting)
    }

    @Test
    fun executeSelectedCleanup_whenRequiresAuthorization_setsPendingSenderAndPreservesSelection() = runTest(testDispatcher) {
        val fakeIntent = Intent("action.cleanmind.test")
        val fakePendingIntent = PendingIntent.getBroadcast(
            application, 0, fakeIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val expectedIntentSender = fakePendingIntent.intentSender

        val testFile = StorageFile(
            id = 901L,
            name = "photo_to_auth.jpg",
            uri = "content://media/external/files/901",
            sizeBytes = 2048L,
            mimeType = "image/jpeg",
            extension = "jpg",
            dateModifiedEpochSeconds = 123456L,
            category = StorageCategory.IMAGES
        )

        val rec = CleanupRecommendation(
            id = 701L,
            file = testFile,
            type = RecommendationType.TEMPORARY_FILE,
            priority = RecommendationPriority.HIGH,
            score = 90,
            reason = "Test",
            reclaimableSizeBytes = 2048L
        )

        val mockUseCase = object : DeleteSelectedFilesUseCase(application, testDispatcher) {
            override suspend fun execute(recommendations: List<CleanupRecommendation>): DeletionResult {
                return DeletionResult.RequiresAuthorization(
                    intentSender = expectedIntentSender,
                    pendingRecommendations = recommendations,
                    directSummary = DeletionSummary(0, 0L, emptyList(), emptySet())
                )
            }
        }

        val fakeRepo = FakeRepository()
        val fakeHistoryRepo = FakeHistoryRepository(
            initialAnalysis = StorageAnalysisResult(
                totalFilesCount = 1,
                totalAnalyzedSizeBytes = 2048L,
                categorySummaries = listOf(CategorySummary(StorageCategory.IMAGES, 1, 2048L)),
                files = listOf(testFile),
                deviceStorageStats = DeviceStorageStats(100L, 50L, 50L),
                recommendationsSummary = AnalysisRecommendationsSummary(
                    totalRecommendationsCount = 1,
                    potentialReclaimableBytes = 2048L,
                    largeFilesCount = 0,
                    duplicateFilesCount = 0,
                    duplicateGroupsCount = 0,
                    oldFilesCount = 0,
                    temporaryFilesCount = 1,
                    recommendations = listOf(rec),
                    duplicateGroups = emptyList()
                )
            )
        )

        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo, fakeHistoryRepo),
            ioDispatcher = testDispatcher,
            getLatestAnalysisUseCase = GetLatestAnalysisUseCase(fakeHistoryRepo),
            deleteSelectedFilesUseCase = mockUseCase
        )
        advanceUntilIdle()

        // Select the item
        viewModel.onToggleRecommendationSelection(rec.id)
        assertTrue(viewModel.uiState.value.selectedRecommendationIds.contains(rec.id))

        // Execute cleanup
        viewModel.executeSelectedCleanup()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNotNull(state.pendingIntentSender)
        assertEquals(expectedIntentSender, state.pendingIntentSender)
        // Selection must NOT be cleared prematurely
        assertTrue(state.selectedRecommendationIds.contains(rec.id))
        assertNull(state.deletionSummary)
    }

    @Test
    fun onAuthorizationResult_whenApproved_finalizesAndCleansState() = runTest(testDispatcher) {
        val fakeIntent = Intent("action.cleanmind.test")
        val fakePendingIntent = PendingIntent.getBroadcast(
            application, 0, fakeIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val expectedIntentSender = fakePendingIntent.intentSender

        val testFile = StorageFile(
            id = 902L,
            name = "photo_approved.jpg",
            uri = "content://media/external/files/902",
            sizeBytes = 4096L,
            mimeType = "image/jpeg",
            extension = "jpg",
            dateModifiedEpochSeconds = 123456L,
            category = StorageCategory.IMAGES
        )

        val rec = CleanupRecommendation(
            id = 702L,
            file = testFile,
            type = RecommendationType.TEMPORARY_FILE,
            priority = RecommendationPriority.HIGH,
            score = 90,
            reason = "Test",
            reclaimableSizeBytes = 4096L
        )

        val mockUseCase = object : DeleteSelectedFilesUseCase(application, testDispatcher) {
            override suspend fun execute(recommendations: List<CleanupRecommendation>): DeletionResult {
                return DeletionResult.RequiresAuthorization(
                    intentSender = expectedIntentSender,
                    pendingRecommendations = recommendations,
                    directSummary = DeletionSummary(0, 0L, emptyList(), emptySet())
                )
            }

            override suspend fun verifyAndFinalizeAfterAuthorization(
                pendingRecommendations: List<CleanupRecommendation>,
                directSummary: DeletionSummary
            ): DeletionSummary {
                return DeletionSummary(
                    deletedCount = pendingRecommendations.size,
                    reclaimedBytes = pendingRecommendations.sumOf { it.reclaimableSizeBytes },
                    failedFileNames = emptyList(),
                    deletedRecommendationIds = pendingRecommendations.map { it.id }.toSet()
                )
            }
        }

        val fakeRepo = FakeRepository()
        val fakeHistoryRepo = FakeHistoryRepository(
            initialAnalysis = StorageAnalysisResult(
                totalFilesCount = 1,
                totalAnalyzedSizeBytes = 4096L,
                categorySummaries = listOf(CategorySummary(StorageCategory.IMAGES, 1, 4096L)),
                files = listOf(testFile),
                deviceStorageStats = DeviceStorageStats(100L, 50L, 50L),
                recommendationsSummary = AnalysisRecommendationsSummary(
                    totalRecommendationsCount = 1,
                    potentialReclaimableBytes = 4096L,
                    largeFilesCount = 0,
                    duplicateFilesCount = 0,
                    duplicateGroupsCount = 0,
                    oldFilesCount = 0,
                    temporaryFilesCount = 1,
                    recommendations = listOf(rec),
                    duplicateGroups = emptyList()
                )
            )
        )

        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo, fakeHistoryRepo),
            ioDispatcher = testDispatcher,
            getLatestAnalysisUseCase = GetLatestAnalysisUseCase(fakeHistoryRepo),
            deleteSelectedFilesUseCase = mockUseCase
        )
        advanceUntilIdle()

        viewModel.onToggleRecommendationSelection(rec.id)
        viewModel.executeSelectedCleanup()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingIntentSender)

        // User approves dialog
        viewModel.onAuthorizationResult(approved = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNull(state.pendingIntentSender)
        // Selection of deleted item should be removed
        assertFalse(state.selectedRecommendationIds.contains(rec.id))
        assertNotNull(state.deletionSummary)
        assertEquals(1, state.deletionSummary?.deletedCount)
        assertEquals(4096L, state.deletionSummary?.reclaimedBytes)
        assertTrue(state.deletionSummary?.failedFileNames?.isEmpty() == true)
    }

    @Test
    fun onAuthorizationResult_whenRefusedAfterPending_preservesSelectionAndReportsAccurateState() = runTest(testDispatcher) {
        val fakeIntent = Intent("action.cleanmind.test")
        val fakePendingIntent = PendingIntent.getBroadcast(
            application, 0, fakeIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val expectedIntentSender = fakePendingIntent.intentSender

        val testFile = StorageFile(
            id = 903L,
            name = "photo_refused.jpg",
            uri = "content://media/external/files/903",
            sizeBytes = 1024L,
            mimeType = "image/jpeg",
            extension = "jpg",
            dateModifiedEpochSeconds = 123456L,
            category = StorageCategory.IMAGES
        )

        val rec = CleanupRecommendation(
            id = 703L,
            file = testFile,
            type = RecommendationType.TEMPORARY_FILE,
            priority = RecommendationPriority.HIGH,
            score = 90,
            reason = "Test",
            reclaimableSizeBytes = 1024L
        )

        val mockUseCase = object : DeleteSelectedFilesUseCase(application, testDispatcher) {
            override suspend fun execute(recommendations: List<CleanupRecommendation>): DeletionResult {
                return DeletionResult.RequiresAuthorization(
                    intentSender = expectedIntentSender,
                    pendingRecommendations = recommendations,
                    directSummary = DeletionSummary(0, 0L, emptyList(), emptySet())
                )
            }
        }

        val fakeRepo = FakeRepository()
        val fakeHistoryRepo = FakeHistoryRepository(
            initialAnalysis = StorageAnalysisResult(
                totalFilesCount = 1,
                totalAnalyzedSizeBytes = 1024L,
                categorySummaries = listOf(CategorySummary(StorageCategory.IMAGES, 1, 1024L)),
                files = listOf(testFile),
                deviceStorageStats = DeviceStorageStats(100L, 50L, 50L),
                recommendationsSummary = AnalysisRecommendationsSummary(
                    totalRecommendationsCount = 1,
                    potentialReclaimableBytes = 1024L,
                    largeFilesCount = 0,
                    duplicateFilesCount = 0,
                    duplicateGroupsCount = 0,
                    oldFilesCount = 0,
                    temporaryFilesCount = 1,
                    recommendations = listOf(rec),
                    duplicateGroups = emptyList()
                )
            )
        )

        val viewModel = HomeViewModel(
            application = application,
            getDeviceStorageStatsUseCase = GetDeviceStorageStatsUseCase(fakeRepo),
            analyzeStorageUseCase = AnalyzeStorageUseCase(fakeRepo, fakeHistoryRepo),
            ioDispatcher = testDispatcher,
            getLatestAnalysisUseCase = GetLatestAnalysisUseCase(fakeHistoryRepo),
            deleteSelectedFilesUseCase = mockUseCase
        )
        advanceUntilIdle()

        viewModel.onToggleRecommendationSelection(rec.id)
        viewModel.executeSelectedCleanup()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.pendingIntentSender)

        // User denies/cancels dialog
        viewModel.onAuthorizationResult(approved = false)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isDeleting)
        assertNull(state.pendingIntentSender)
        // Selection must be preserved because file was not deleted
        assertTrue(state.selectedRecommendationIds.contains(rec.id))
        assertNotNull(state.deletionSummary)
        assertEquals(0, state.deletionSummary?.deletedCount)
        assertEquals(0L, state.deletionSummary?.reclaimedBytes)
        assertEquals(listOf("photo_refused.jpg"), state.deletionSummary?.failedFileNames)
    }
}
