package com.aistudio.cleanmind.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.data.local.dao.AnalysisDao
import com.aistudio.cleanmind.app.data.local.database.CleanMindDatabase
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AnalysisDaoTest {

    private lateinit var database: CleanMindDatabase
    private lateinit var analysisDao: AnalysisDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CleanMindDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        analysisDao = database.analysisDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    @Test
    fun insertFullAnalysis_persistsSummaryCategoriesAndFiles() = runBlocking {
        val summary = AnalysisSummaryEntity(
            timestampEpochMillis = 1000L,
            totalFilesCount = 3,
            totalAnalyzedSizeBytes = 15000L,
            deviceTotalBytes = 100000L,
            deviceUsedBytes = 50000L,
            deviceFreeBytes = 50000L
        )

        val categories = listOf(
            CategorySummaryEntity(
                analysisId = 0L,
                category = StorageCategory.IMAGES,
                fileCount = 2,
                totalSizeBytes = 10000L
            ),
            CategorySummaryEntity(
                analysisId = 0L,
                category = StorageCategory.DOCUMENTS,
                fileCount = 1,
                totalSizeBytes = 5000L
            )
        )

        val files = listOf(
            ScannedFileEntity(
                analysisId = 0L,
                originalFileId = 1L,
                name = "photo.jpg",
                uri = "content://media/1",
                sizeBytes = 6000L,
                mimeType = "image/jpeg",
                extension = "jpg",
                dateModifiedEpochSeconds = 500L,
                category = StorageCategory.IMAGES
            ),
            ScannedFileEntity(
                analysisId = 0L,
                originalFileId = 2L,
                name = "image.png",
                uri = "content://media/2",
                sizeBytes = 4000L,
                mimeType = "image/png",
                extension = "png",
                dateModifiedEpochSeconds = 600L,
                category = StorageCategory.IMAGES
            )
        )

        val insertedId = analysisDao.insertFullAnalysis(summary, categories, files)
        assertTrue(insertedId > 0)

        val latest = analysisDao.getLatestAnalysisWithCategories().first()
        assertNotNull(latest)
        assertEquals(insertedId, latest!!.summary.id)
        assertEquals(3, latest.summary.totalFilesCount)
        assertEquals(15000L, latest.summary.totalAnalyzedSizeBytes)
        assertEquals(2, latest.categories.size)

        val scannedFiles = analysisDao.getScannedFilesList(insertedId)
        assertEquals(2, scannedFiles.size)
        assertEquals("photo.jpg", scannedFiles[0].name)
    }

    @Test
    fun getLatestAnalysis_returnsNullWhenEmpty() = runBlocking {
        val latest = analysisDao.getLatestAnalysisWithCategories().first()
        assertNull(latest)
    }

    @Test
    fun pruneOldAnalyses_retainsOnlyKeepCount() = runBlocking {
        for (i in 1..5) {
            val summary = AnalysisSummaryEntity(
                timestampEpochMillis = i * 1000L,
                totalFilesCount = i,
                totalAnalyzedSizeBytes = i * 1000L,
                deviceTotalBytes = 100000L,
                deviceUsedBytes = 50000L,
                deviceFreeBytes = 50000L
            )
            analysisDao.insertSummary(summary)
        }

        assertEquals(5, analysisDao.getAnalysesCount())

        analysisDao.pruneOldAnalyses(keepCount = 2)

        assertEquals(2, analysisDao.getAnalysesCount())
        val latest = analysisDao.getLatestSummary()
        assertNotNull(latest)
        assertEquals(5000L, latest!!.timestampEpochMillis)
    }

    @Test
    fun clearAll_removesAllAnalyses() = runBlocking {
        val summary = AnalysisSummaryEntity(
            timestampEpochMillis = 1000L,
            totalFilesCount = 1,
            totalAnalyzedSizeBytes = 1000L,
            deviceTotalBytes = 100000L,
            deviceUsedBytes = 50000L,
            deviceFreeBytes = 50000L
        )
        analysisDao.insertSummary(summary)
        assertEquals(1, analysisDao.getAnalysesCount())

        analysisDao.clearAll()
        assertEquals(0, analysisDao.getAnalysesCount())
    }
}
