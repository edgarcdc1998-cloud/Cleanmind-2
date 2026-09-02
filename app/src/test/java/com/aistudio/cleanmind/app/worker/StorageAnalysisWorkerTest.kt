package com.aistudio.cleanmind.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class StorageAnalysisWorkerTest {

    private lateinit var context: Context

    private class FakeSuccessRepository : StorageRepository {
        override suspend fun getDeviceStorageStats(): DeviceStorageStats =
            DeviceStorageStats(100L, 50L, 50L)

        override suspend fun analyzeStorage(): Result<StorageAnalysisResult> = Result.success(
            StorageAnalysisResult(
                totalFilesCount = 42,
                totalAnalyzedSizeBytes = 1024L * 1024L * 50L,
                categorySummaries = listOf(CategorySummary(StorageCategory.IMAGES, 42, 1024L * 1024L * 50L)),
                files = emptyList(),
                deviceStorageStats = DeviceStorageStats(100L, 50L, 50L)
            )
        )
    }

    private class FakeFailingRepository : StorageRepository {
        override suspend fun getDeviceStorageStats(): DeviceStorageStats =
            DeviceStorageStats(100L, 50L, 50L)

        override suspend fun analyzeStorage(): Result<StorageAnalysisResult> =
            Result.failure(SecurityException("Permission denied for MediaStore query"))
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun doWork_successReturnsSuccessResultWithData() = runTest {
        val worker = TestListenableWorkerBuilder<StorageAnalysisWorker>(context).build()
        worker.customAnalyzeStorageUseCase = AnalyzeStorageUseCase(FakeSuccessRepository())

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertEquals(StorageAnalysisWorker.STATUS_SUCCESS, outputData.getString(StorageAnalysisWorker.KEY_STATUS))
        assertEquals(42, outputData.getInt(StorageAnalysisWorker.KEY_ITEMS_COUNT, 0))
    }

    @Test
    fun doWork_securityExceptionReturnsFailureResult() = runTest {
        val worker = TestListenableWorkerBuilder<StorageAnalysisWorker>(context).build()
        worker.customAnalyzeStorageUseCase = AnalyzeStorageUseCase(FakeFailingRepository())

        val result = worker.doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        val error = outputData.getString(StorageAnalysisWorker.KEY_ERROR_MESSAGE)
        assertTrue(error?.contains("Permission denied") == true)
    }
}
