package com.aistudio.cleanmind.app.data.repository

import com.aistudio.cleanmind.app.data.datasource.StorageDataSource
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class StorageRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeStorageDataSource(
        var stats: DeviceStorageStats = DeviceStorageStats(1000L, 600L, 400L),
        var files: List<StorageFile> = emptyList(),
        var shouldThrow: Boolean = false
    ) : StorageDataSource {
        override fun getDeviceStorageStats(): DeviceStorageStats {
            if (shouldThrow) throw IllegalStateException("Storage error")
            return stats
        }

        override suspend fun queryMediaFiles(): List<StorageFile> {
            if (shouldThrow) throw IllegalStateException("MediaStore query failed")
            return files
        }
    }

    @Test
    fun getDeviceStorageStats_returnsCorrectStats() = runTest(testDispatcher) {
        val fakeStats = DeviceStorageStats(100L * 1024, 40L * 1024, 60L * 1024)
        val dataSource = FakeStorageDataSource(stats = fakeStats)
        val repository = StorageRepositoryImpl(dataSource, testDispatcher)

        val result = repository.getDeviceStorageStats()
        assertEquals(fakeStats.totalBytes, result.totalBytes)
        assertEquals(fakeStats.usedBytes, result.usedBytes)
        assertEquals(fakeStats.freeBytes, result.freeBytes)
        assertEquals(0.6f, result.usedPercentage, 0.01f)
    }

    @Test
    fun analyzeStorage_emptyStorage_returnsZeroFilesAndEmptySummaries() = runTest(testDispatcher) {
        val dataSource = FakeStorageDataSource(files = emptyList())
        val repository = StorageRepositoryImpl(dataSource, testDispatcher)

        val result = repository.analyzeStorage()
        assertTrue(result.isSuccess)

        val analysis = result.getOrThrow()
        assertEquals(0, analysis.totalFilesCount)
        assertEquals(0L, analysis.totalAnalyzedSizeBytes)
        assertTrue(analysis.files.isEmpty())
        assertTrue(analysis.categorySummaries.isEmpty())
    }

    @Test
    fun analyzeStorage_withDiverseFiles_correctlyCategorizesAndAggregates() = runTest(testDispatcher) {
        val testFiles = listOf(
            StorageFile(
                id = 1L,
                name = "vacation.jpg",
                uri = "content://media/external/images/media/1",
                sizeBytes = 2 * 1024 * 1024L, // 2MB
                mimeType = "image/jpeg",
                extension = "jpg",
                dateModifiedEpochSeconds = 1700000000L,
                category = StorageCategory.IMAGES
            ),
            StorageFile(
                id = 2L,
                name = "song.mp3",
                uri = "content://media/external/audio/media/2",
                sizeBytes = 5 * 1024 * 1024L, // 5MB
                mimeType = "audio/mpeg",
                extension = "mp3",
                dateModifiedEpochSeconds = 1700000000L,
                category = StorageCategory.AUDIOS
            ),
            StorageFile(
                id = 3L,
                name = "big_movie.mp4",
                uri = "content://media/external/video/media/3",
                sizeBytes = 150 * 1024 * 1024L, // 150MB (> 50MB threshold)
                mimeType = "video/mp4",
                extension = "mp4",
                dateModifiedEpochSeconds = 1700000000L,
                category = StorageCategory.VIDEOS
            )
        )

        val dataSource = FakeStorageDataSource(files = testFiles)
        val repository = StorageRepositoryImpl(dataSource, testDispatcher)

        val result = repository.analyzeStorage()
        assertTrue(result.isSuccess)

        val analysis = result.getOrThrow()
        assertEquals(3, analysis.totalFilesCount)
        assertEquals(157 * 1024 * 1024L, analysis.totalAnalyzedSizeBytes)

        val imageSummary = analysis.categorySummaries.find { it.category == StorageCategory.IMAGES }
        assertNotNull(imageSummary)
        assertEquals(1, imageSummary?.fileCount)
        assertEquals(2 * 1024 * 1024L, imageSummary?.totalSizeBytes)

        val audioSummary = analysis.categorySummaries.find { it.category == StorageCategory.AUDIOS }
        assertNotNull(audioSummary)
        assertEquals(1, audioSummary?.fileCount)
        assertEquals(5 * 1024 * 1024L, audioSummary?.totalSizeBytes)

        val largeSummary = analysis.categorySummaries.find { it.category == StorageCategory.LARGE_FILES }
        assertNotNull(largeSummary)
        assertEquals(1, largeSummary?.fileCount)
        assertEquals(150 * 1024 * 1024L, largeSummary?.totalSizeBytes)
    }

    @Test
    fun analyzeStorage_dataSourceError_returnsFailure() = runTest(testDispatcher) {
        val dataSource = FakeStorageDataSource(shouldThrow = true)
        val repository = StorageRepositoryImpl(dataSource, testDispatcher)

        val result = repository.analyzeStorage()
        assertTrue(result.isFailure)
        assertEquals("Storage error", result.exceptionOrNull()?.message)
    }
}
