package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class GetStorageSavingsDashboardUseCaseTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeHistoryRepository(
        private val analyses: List<StorageAnalysisResult> = emptyList()
    ) : AnalysisHistoryRepository {
        override fun getLatestAnalysis(): Flow<StorageAnalysisResult?> = flowOf(analyses.firstOrNull())
        override fun getAllAnalyses(): Flow<List<StorageAnalysisResult>> = flowOf(analyses)
        override suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long> = Result.success(1L)
        override fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>> = flowOf(emptyList())
        override suspend fun clearHistory(): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun emptyHistory_returnsZeroMetricsAndNullTrend() = runTest(testDispatcher) {
        val repo = FakeHistoryRepository(emptyList())
        val useCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val result = useCase().first()

        assertEquals(0, result.totalAnalysesCount)
        assertNull(result.latestAnalysisEpochMillis)
        assertEquals(0L, result.latestAnalyzedSizeBytes)
        assertEquals(0L, result.totalVolumeAnalyzedBytes)
        assertEquals(0L, result.latestPotentialReclaimableBytes)
        assertEquals(0L, result.totalPotentialReclaimableBytes)
        assertEquals(0, result.totalRecommendationsCount)
        assertTrue(result.timePoints.isEmpty())
        assertTrue(result.weeklyAggregations.isEmpty())
        assertTrue(result.monthlyAggregations.isEmpty())
        assertNull(result.trendSummary)
        assertFalse(result.executedSavingsTracked)
    }

    @Test
    fun singleAnalysis_returnsExactValues_andNoTrendExtrapolated() = runTest(testDispatcher) {
        val dummyFile = StorageFile(1L, "big.zip", "uri://1", 1000L, "application/zip", "zip", 0L, StorageCategory.DOCUMENTS)
        val rec = CleanupRecommendation(
            id = 1L,
            file = dummyFile,
            type = RecommendationType.LARGE_FILE,
            priority = RecommendationPriority.MEDIUM,
            score = 80,
            reason = "Grande",
            reclaimableSizeBytes = 1000L
        )
        val single = StorageAnalysisResult(
            id = 1L,
            totalFilesCount = 10,
            totalAnalyzedSizeBytes = 50000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(100000L, 50000L, 50000L),
            timestampEpochMillis = 1700000000000L,
            recommendationsSummary = AnalysisRecommendationsSummary(
                totalRecommendationsCount = 1,
                potentialReclaimableBytes = 1000L,
                largeFilesCount = 1,
                duplicateFilesCount = 0,
                duplicateGroupsCount = 0,
                oldFilesCount = 0,
                temporaryFilesCount = 0,
                recommendations = listOf(rec),
                duplicateGroups = emptyList()
            )
        )

        val repo = FakeHistoryRepository(listOf(single))
        val useCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val result = useCase().first()

        assertEquals(1, result.totalAnalysesCount)
        assertEquals(1700000000000L, result.latestAnalysisEpochMillis)
        assertEquals(50000L, result.latestAnalyzedSizeBytes)
        assertEquals(50000L, result.totalVolumeAnalyzedBytes)
        assertEquals(1000L, result.latestPotentialReclaimableBytes)
        assertEquals(1000L, result.totalPotentialReclaimableBytes)
        assertEquals(1, result.totalRecommendationsCount)
        assertEquals(1, result.largeFilesCount)
        assertEquals(1, result.timePoints.size)
        // CRÍTICO: Não mostrar tendência quando houver apenas uma observação
        assertNull(result.trendSummary)
        assertFalse(result.executedSavingsTracked)
    }

    @Test
    fun multipleAnalyses_calculatesAccurateTrendAndAggregations() = runTest(testDispatcher) {
        val analysis1 = StorageAnalysisResult(
            id = 1L,
            totalFilesCount = 50,
            totalAnalyzedSizeBytes = 100_000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(500_000L, 400_000L, 100_000L),
            timestampEpochMillis = 1000L,
            recommendationsSummary = AnalysisRecommendationsSummary(
                totalRecommendationsCount = 2,
                potentialReclaimableBytes = 20_000L,
                largeFilesCount = 1,
                duplicateFilesCount = 1,
                duplicateGroupsCount = 1,
                oldFilesCount = 0,
                temporaryFilesCount = 0,
                recommendations = emptyList(),
                duplicateGroups = emptyList()
            )
        )

        val analysis2 = StorageAnalysisResult(
            id = 2L,
            totalFilesCount = 65,
            totalAnalyzedSizeBytes = 150_000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(500_000L, 350_000L, 150_000L),
            timestampEpochMillis = 2000L,
            recommendationsSummary = AnalysisRecommendationsSummary(
                totalRecommendationsCount = 4,
                potentialReclaimableBytes = 35_000L,
                largeFilesCount = 2,
                duplicateFilesCount = 1,
                duplicateGroupsCount = 1,
                oldFilesCount = 1,
                temporaryFilesCount = 0,
                recommendations = emptyList(),
                duplicateGroups = emptyList()
            )
        )

        val repo = FakeHistoryRepository(listOf(analysis2, analysis1))
        val useCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val result = useCase().first()

        assertEquals(2, result.totalAnalysesCount)
        assertEquals(2000L, result.latestAnalysisEpochMillis)
        assertEquals(150_000L, result.latestAnalyzedSizeBytes)
        assertEquals(250_000L, result.totalVolumeAnalyzedBytes)
        assertEquals(35_000L, result.latestPotentialReclaimableBytes)
        assertEquals(55_000L, result.totalPotentialReclaimableBytes)
        assertEquals(4, result.totalRecommendationsCount)
        assertEquals(2, result.largeFilesCount)
        assertEquals(1, result.oldFilesCount)
        assertEquals(2, result.timePoints.size)

        // Verificação do Trend
        assertNotNull(result.trendSummary)
        assertEquals(50_000L, result.trendSummary!!.analyzedDeltaBytes) // 150k - 100k
        assertEquals(15_000L, result.trendSummary!!.reclaimableDeltaBytes) // 35k - 20k
        assertEquals(15, result.trendSummary!!.filesCountDelta) // 65 - 50
        assertEquals(2, result.trendSummary!!.observationsCount)
    }

    @Test
    fun zeroValues_handledSafelyWithoutDivisionByZero() = runTest(testDispatcher) {
        val analysis = StorageAnalysisResult(
            id = 1L,
            totalFilesCount = 0,
            totalAnalyzedSizeBytes = 0L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(0L, 0L, 0L),
            timestampEpochMillis = 1000L,
            recommendationsSummary = null
        )

        val repo = FakeHistoryRepository(listOf(analysis))
        val useCase = GetStorageSavingsDashboardUseCase(repo, testDispatcher)

        val result = useCase().first()

        assertEquals(1, result.totalAnalysesCount)
        assertEquals(0L, result.latestAnalyzedSizeBytes)
        assertEquals(0L, result.latestPotentialReclaimableBytes)
        assertEquals(0, result.totalRecommendationsCount)
        assertNull(result.trendSummary)
    }
}
