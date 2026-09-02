package com.aistudio.cleanmind.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.data.local.database.CleanMindDatabase
import com.aistudio.cleanmind.app.domain.model.CategorySummary
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
class AnalysisHistoryRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var database: CleanMindDatabase
    private lateinit var repository: AnalysisHistoryRepositoryImpl

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CleanMindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AnalysisHistoryRepositoryImpl(
            analysisDao = database.analysisDao(),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getLatestAnalysis_returnsNullWhenNoDataSaved() = runTest(testDispatcher) {
        val latest = repository.getLatestAnalysis().first()
        assertNull(latest)
    }

    @Test
    fun saveAnalysis_andRetrieveLatest_succeeds() = runTest(testDispatcher) {
        val result = StorageAnalysisResult(
            totalFilesCount = 5,
            totalAnalyzedSizeBytes = 20480L,
            categorySummaries = listOf(
                CategorySummary(StorageCategory.IMAGES, 3, 15000L),
                CategorySummary(StorageCategory.DOCUMENTS, 2, 5480L)
            ),
            files = listOf(
                StorageFile(
                    id = 1L,
                    name = "doc.pdf",
                    uri = "content://media/1",
                    sizeBytes = 5480L,
                    mimeType = "application/pdf",
                    extension = "pdf",
                    dateModifiedEpochSeconds = 123456L,
                    category = StorageCategory.DOCUMENTS
                )
            ),
            deviceStorageStats = DeviceStorageStats(100000L, 60000L, 40000L),
            timestampEpochMillis = 99999L
        )

        val saveResult = repository.saveAnalysis(result)
        assertTrue(saveResult.isSuccess)
        val id = saveResult.getOrThrow()
        assertTrue(id > 0)

        val latest = repository.getLatestAnalysis().first()
        assertNotNull(latest)
        assertEquals(5, latest!!.totalFilesCount)
        assertEquals(20480L, latest.totalAnalyzedSizeBytes)
        assertEquals(2, latest.categorySummaries.size)
        assertEquals(99999L, latest.timestampEpochMillis)

        val files = repository.getFilesForAnalysis(id).first()
        assertEquals(1, files.size)
        assertEquals("doc.pdf", files[0].name)
    }

    @Test
    fun clearHistory_removesSavedAnalyses() = runTest(testDispatcher) {
        val result = StorageAnalysisResult(
            totalFilesCount = 1,
            totalAnalyzedSizeBytes = 1000L,
            categorySummaries = emptyList(),
            files = emptyList(),
            deviceStorageStats = DeviceStorageStats(100000L, 60000L, 40000L),
            timestampEpochMillis = 1000L
        )
        repository.saveAnalysis(result)
        assertNotNull(repository.getLatestAnalysis().first())

        val clearResult = repository.clearHistory()
        assertTrue(clearResult.isSuccess)
        assertNull(repository.getLatestAnalysis().first())
    }
}
