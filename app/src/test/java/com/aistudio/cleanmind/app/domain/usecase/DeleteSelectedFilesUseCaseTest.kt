package com.aistudio.cleanmind.app.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DeleteSelectedFilesUseCaseTest {

    private lateinit var context: Context
    private lateinit var useCase: DeleteSelectedFilesUseCase
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        useCase = DeleteSelectedFilesUseCase(context, testDispatcher)
    }

    private fun createRecommendation(
        id: Long,
        file: StorageFile,
        type: RecommendationType = RecommendationType.TEMPORARY_FILE,
        reclaimableBytes: Long = file.sizeBytes
    ): CleanupRecommendation {
        return CleanupRecommendation(
            id = id,
            file = file,
            type = type,
            priority = RecommendationPriority.HIGH,
            score = 90,
            reason = "Test recommendation",
            reclaimableSizeBytes = reclaimableBytes
        )
    }

    @Test
    fun invoke_emptyList_returnsZeroCounts() = runTest(testDispatcher) {
        val result = useCase(emptyList())
        assertTrue(result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(0, summary.deletedCount)
        assertEquals(0L, summary.reclaimedBytes)
        assertTrue(summary.failedFileNames.isEmpty())
    }

    @Test
    fun invoke_physicalFileExists_deletesRealFileAndReturnsSuccess() = runTest(testDispatcher) {
        val tempFile = File(context.cacheDir, "real_delete_test.tmp")
        tempFile.writeText("sample data to delete")
        assertTrue(tempFile.exists())

        val storageFile = StorageFile(
            id = 1L,
            name = "real_delete_test.tmp",
            uri = tempFile.absolutePath,
            sizeBytes = tempFile.length(),
            mimeType = "application/octet-stream",
            extension = "tmp",
            dateModifiedEpochSeconds = System.currentTimeMillis() / 1000L,
            category = StorageCategory.OTHERS
        )
        val rec = createRecommendation(101L, storageFile)

        val result = useCase(listOf(rec))
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        assertEquals(1, summary.deletedCount)
        assertEquals(storageFile.sizeBytes, summary.reclaimedBytes)
        assertTrue(summary.failedFileNames.isEmpty())

        // Confirm real disk state: file MUST NOT exist after deletion
        assertFalse("O arquivo deve ter sido fisicamente excluído do disco", tempFile.exists())
    }

    @Test
    fun invoke_nonExistentOrInaccessibleFile_doesNotDeclareFakeSuccess() = runTest(testDispatcher) {
        val missingPath = File(context.cacheDir, "non_existent_file.tmp").absolutePath

        val storageFile = StorageFile(
            id = 2L,
            name = "non_existent_file.tmp",
            uri = missingPath,
            sizeBytes = 2048L,
            mimeType = "application/octet-stream",
            extension = "tmp",
            dateModifiedEpochSeconds = System.currentTimeMillis() / 1000L,
            category = StorageCategory.OTHERS
        )
        val rec = createRecommendation(102L, storageFile)

        val result = useCase(listOf(rec))
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        // Must NEVER claim success for non-existent/un-deleted files
        assertEquals(0, summary.deletedCount)
        assertEquals(0L, summary.reclaimedBytes)
        assertEquals(1, summary.failedFileNames.size)
        assertEquals("non_existent_file.tmp", summary.failedFileNames.first())
    }

    @Test
    fun invoke_partialSuccess_reportsAccurateCountsAndFailedList() = runTest(testDispatcher) {
        val realFile = File(context.cacheDir, "success_file.tmp")
        realFile.writeText("to be deleted")
        assertTrue(realFile.exists())

        val recSuccess = createRecommendation(
            id = 201L,
            file = StorageFile(
                id = 10L,
                name = "success_file.tmp",
                uri = realFile.absolutePath,
                sizeBytes = 1000L,
                mimeType = "application/octet-stream",
                extension = "tmp",
                dateModifiedEpochSeconds = 123456L,
                category = StorageCategory.OTHERS
            ),
            reclaimableBytes = 1000L
        )

        val recFail = createRecommendation(
            id = 202L,
            file = StorageFile(
                id = 11L,
                name = "inaccessible_file.mp4",
                uri = "content://media/external/video/media/99999",
                sizeBytes = 5000L,
                mimeType = "video/mp4",
                extension = "mp4",
                dateModifiedEpochSeconds = 123456L,
                category = StorageCategory.VIDEOS
            ),
            reclaimableBytes = 5000L
        )

        val result = useCase(listOf(recSuccess, recFail))
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        assertEquals(1, summary.deletedCount)
        assertEquals(1000L, summary.reclaimedBytes)
        assertEquals(1, summary.failedFileNames.size)
        assertEquals("inaccessible_file.mp4", summary.failedFileNames.first())

        assertFalse("Arquivo real deve ser removido", realFile.exists())
    }

    @Test
    fun invoke_fileUriScheme_deletesSuccessfullyWhenPhysicalFileExists() = runTest(testDispatcher) {
        val testFile = File(context.cacheDir, "scheme_test.tmp")
        testFile.writeText("test file uri")
        assertTrue(testFile.exists())

        val rec = createRecommendation(
            id = 301L,
            file = StorageFile(
                id = 20L,
                name = "scheme_test.tmp",
                uri = "file://${testFile.absolutePath}",
                sizeBytes = 500L,
                mimeType = "application/octet-stream",
                extension = "tmp",
                dateModifiedEpochSeconds = 123456L,
                category = StorageCategory.OTHERS
            ),
            reclaimableBytes = 500L
        )

        val result = useCase(listOf(rec))
        assertTrue(result.isSuccess)

        val summary = result.getOrThrow()
        assertEquals(1, summary.deletedCount)
        assertEquals(500L, summary.reclaimedBytes)
        assertTrue(summary.failedFileNames.isEmpty())
        assertFalse(testFile.exists())
    }
}
