package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageUseCasesTest {

    private class FakeStorageRepository(
        private val stats: DeviceStorageStats = DeviceStorageStats(100L, 60L, 40L),
        private val analysisResult: Result<StorageAnalysisResult> = Result.success(
            StorageAnalysisResult(0, 0L, emptyList(), emptyList(), stats)
        )
    ) : StorageRepository {
        override suspend fun getDeviceStorageStats(): DeviceStorageStats = stats
        override suspend fun analyzeStorage(): Result<StorageAnalysisResult> = analysisResult
    }

    @Test
    fun getDeviceStorageStatsUseCase_invokesRepository() = runTest {
        val stats = DeviceStorageStats(200L, 100L, 100L)
        val repository = FakeStorageRepository(stats = stats)
        val useCase = GetDeviceStorageStatsUseCase(repository)

        val result = useCase()
        assertEquals(stats, result)
    }

    @Test
    fun analyzeStorageUseCase_returnsSuccess() = runTest {
        val stats = DeviceStorageStats(200L, 100L, 100L)
        val expected = StorageAnalysisResult(5, 5000L, emptyList(), emptyList(), stats)
        val repository = FakeStorageRepository(analysisResult = Result.success(expected))
        val useCase = AnalyzeStorageUseCase(repository)

        val result = useCase()
        assertTrue(result.isSuccess)
        assertEquals(expected, result.getOrNull())
    }

    @Test
    fun analyzeStorageUseCase_returnsFailure() = runTest {
        val repository = FakeStorageRepository(analysisResult = Result.failure(RuntimeException("Failed")))
        val useCase = AnalyzeStorageUseCase(repository)

        val result = useCase()
        assertTrue(result.isFailure)
        assertEquals("Failed", result.exceptionOrNull()?.message)
    }
}
