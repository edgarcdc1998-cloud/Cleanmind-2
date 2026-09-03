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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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

    @Test
    fun invoke_withSelectedRecommendations_returnsCorrectDeletionSummary() = runTest(testDispatcher) {
        val file1 = StorageFile(
            id = 1L,
            name = "file1.png",
            uri = "content://media/external/images/media/1",
            sizeBytes = 1024L * 1024L,
            mimeType = "image/png",
            extension = "png",
            dateModifiedEpochSeconds = 123456789L,
            category = StorageCategory.IMAGES
        )
        val rec1 = CleanupRecommendation(
            id = 101L,
            file = file1,
            type = RecommendationType.DUPLICATE,
            priority = RecommendationPriority.MEDIUM,
            score = 80,
            reason = "Duplicado de file2.png",
            reclaimableSizeBytes = 1024L * 1024L
        )

        val file2 = StorageFile(
            id = 2L,
            name = "large_temp.tmp",
            uri = "content://media/external/files/2",
            sizeBytes = 50 * 1024L * 1024L,
            mimeType = "application/octet-stream",
            extension = "tmp",
            dateModifiedEpochSeconds = 123456789L,
            category = StorageCategory.OTHERS
        )
        val rec2 = CleanupRecommendation(
            id = 102L,
            file = file2,
            type = RecommendationType.TEMPORARY_FILE,
            priority = RecommendationPriority.HIGH,
            score = 95,
            reason = "Arquivo temporário grande",
            reclaimableSizeBytes = 50 * 1024L * 1024L
        )

        val list = listOf(rec1, rec2)
        val result = useCase(list)

        assertTrue(result.isSuccess)
        val summary = result.getOrNull()
        assertTrue(summary != null)
        assertEquals(2, summary?.deletedCount)
        assertEquals(51 * 1024L * 1024L, summary?.reclaimedBytes)
    }
}
