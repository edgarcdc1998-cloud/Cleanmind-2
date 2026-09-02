package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GetLatestAnalysisUseCaseTest {

    private class FakeAnalysisHistoryRepository(
        var latest: StorageAnalysisResult? = null
    ) : AnalysisHistoryRepository {
        override fun getLatestAnalysis(): Flow<StorageAnalysisResult?> = flowOf(latest)
        override suspend fun saveAnalysis(result: StorageAnalysisResult): Result<Long> {
            latest = result
            return Result.success(1L)
        }
        override fun getFilesForAnalysis(analysisId: Long): Flow<List<StorageFile>> = flowOf(emptyList())
        override suspend fun clearHistory(): Result<Unit> {
            latest = null
            return Result.success(Unit)
        }
    }

    @Test
    fun invoke_returnsRepositoryLatestAnalysis() = runTest {
        val fakeRepo = FakeAnalysisHistoryRepository(
            latest = StorageAnalysisResult(
                totalFilesCount = 42,
                totalAnalyzedSizeBytes = 1024L,
                categorySummaries = emptyList(),
                files = emptyList(),
                deviceStorageStats = DeviceStorageStats(100L, 50L, 50L),
                timestampEpochMillis = 12345L
            )
        )
        val useCase = GetLatestAnalysisUseCase(fakeRepo)
        val result = useCase().first()

        assertNotNull(result)
        assertEquals(42, result?.totalFilesCount)
        assertEquals(12345L, result?.timestampEpochMillis)
    }

    @Test
    fun invoke_returnsNullWhenEmpty() = runTest {
        val fakeRepo = FakeAnalysisHistoryRepository(latest = null)
        val useCase = GetLatestAnalysisUseCase(fakeRepo)
        val result = useCase().first()

        assertNull(result)
    }
}
