package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.FileHashRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendationsUseCasesTest {

    private class FakeFileHashRepository(
        private val hashMapping: Map<String, String> = emptyMap()
    ) : FileHashRepository {
        override suspend fun calculateContentHash(uri: String): String? {
            return hashMapping[uri] ?: uri.hashCode().toString()
        }
    }

    private fun createFile(
        id: Long,
        name: String,
        uri: String = "content://media/$id",
        sizeBytes: Long = 1024L,
        extension: String = "jpg",
        category: StorageCategory = StorageCategory.IMAGES,
        dateModifiedEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): StorageFile {
        return StorageFile(
            id = id,
            name = name,
            uri = uri,
            sizeBytes = sizeBytes,
            mimeType = "image/jpeg",
            extension = extension,
            dateModifiedEpochSeconds = dateModifiedEpochSeconds,
            category = category
        )
    }

    @Test
    fun findLargeFilesUseCase_filtersFilesAboveThreshold() {
        val useCase = FindLargeFilesUseCase(defaultThresholdBytes = 50L * 1024L * 1024L)

        val smallFile = createFile(id = 1L, name = "small.jpg", sizeBytes = 10L * 1024L * 1024L)
        val exactFile = createFile(id = 2L, name = "exact.mp4", sizeBytes = 50L * 1024L * 1024L)
        val largeFile = createFile(id = 3L, name = "huge.zip", sizeBytes = 200L * 1024L * 1024L)

        val results = useCase(listOf(smallFile, exactFile, largeFile))

        assertEquals(2, results.size)
        assertEquals(3L, results[0].id) // Sorted descending
        assertEquals(2L, results[1].id)
    }

    @Test
    fun findDuplicateFilesUseCase_groupsFilesWithMatchingHash() = runTest {
        val file1 = createFile(id = 1L, name = "photo1.jpg", uri = "uri_1", sizeBytes = 2048L)
        val file2 = createFile(id = 2L, name = "photo1_copy.jpg", uri = "uri_2", sizeBytes = 2048L)
        val file3 = createFile(id = 3L, name = "photo2.jpg", uri = "uri_3", sizeBytes = 2048L) // Same size, different hash
        val uniqueFile = createFile(id = 4L, name = "other.jpg", uri = "uri_4", sizeBytes = 5000L)

        val hashRepo = FakeFileHashRepository(
            mapOf(
                "uri_1" to "hash_abc",
                "uri_2" to "hash_abc",
                "uri_3" to "hash_xyz",
                "uri_4" to "hash_unique"
            )
        )

        val useCase = FindDuplicateFilesUseCase(hashRepo)
        val duplicateGroups = useCase(listOf(file1, file2, file3, uniqueFile))

        assertEquals(1, duplicateGroups.size)
        val group = duplicateGroups.first()
        assertEquals(2, group.files.size)
        assertEquals(2048L, group.reclaimableSizeBytes) // 1 copy can be reclaimed
        assertEquals(4096L, group.totalSizeBytes)
    }

    @Test
    fun findOldFilesUseCase_identifiesFilesOlderThanCutoff() {
        val now = 1700000000L
        val useCase = FindOldFilesUseCase(defaultThresholdDays = 180, defaultMinSizeBytes = 1000L)

        val newFile = createFile(
            id = 1L,
            name = "recent.jpg",
            sizeBytes = 2000L,
            dateModifiedEpochSeconds = now - (30L * 86400L) // 1 month ago
        )
        val oldFile = createFile(
            id = 2L,
            name = "archived.pdf",
            sizeBytes = 2000L,
            dateModifiedEpochSeconds = now - (200L * 86400L) // ~6.6 months ago
        )

        val results = useCase(listOf(newFile, oldFile), currentEpochSeconds = now)

        assertEquals(1, results.size)
        assertEquals(2L, results.first().id)
    }

    @Test
    fun findTemporaryFilesUseCase_identifiesTempAndCacheExtensions() {
        val useCase = FindTemporaryFilesUseCase()

        val tempFile = createFile(id = 1L, name = "download.tmp", extension = "tmp")
        val logFile = createFile(id = 2L, name = "crash.log", extension = "log")
        val normalFile = createFile(id = 3L, name = "document.pdf", extension = "pdf")

        val results = useCase(listOf(tempFile, logFile, normalFile))

        assertEquals(2, results.size)
        assertTrue(results.any { it.id == 1L })
        assertTrue(results.any { it.id == 2L })
    }

    @Test
    fun generateCleanupRecommendationsUseCase_integratesAllCriteriaAndExcludesDuplicates() = runTest {
        val now = 1700000000L
        val file1 = createFile(id = 1L, name = "dup1.jpg", uri = "u1", sizeBytes = 1000L, dateModifiedEpochSeconds = now)
        val file2 = createFile(id = 2L, name = "dup2.jpg", uri = "u2", sizeBytes = 1000L, dateModifiedEpochSeconds = now)
        val largeFile = createFile(id = 3L, name = "big_video.mp4", uri = "u3", sizeBytes = 100L * 1024L * 1024L, dateModifiedEpochSeconds = now)
        val tempFile = createFile(id = 4L, name = "cache.tmp", uri = "u4", sizeBytes = 500L, extension = "tmp", dateModifiedEpochSeconds = now)
        val oldFile = createFile(id = 5L, name = "old_doc.pdf", uri = "u5", sizeBytes = 2000L, dateModifiedEpochSeconds = now - (300L * 86400L))

        val hashRepo = FakeFileHashRepository(
            mapOf(
                "u1" to "hash_dup",
                "u2" to "hash_dup"
            )
        )

        val useCase = GenerateCleanupRecommendationsUseCase(
            findLargeFilesUseCase = FindLargeFilesUseCase(defaultThresholdBytes = 50L * 1024L * 1024L),
            findDuplicateFilesUseCase = FindDuplicateFilesUseCase(hashRepo),
            findOldFilesUseCase = FindOldFilesUseCase(defaultThresholdDays = 180, defaultMinSizeBytes = 1000L),
            findTemporaryFilesUseCase = FindTemporaryFilesUseCase()
        )

        val summary = useCase(
            files = listOf(file1, file2, largeFile, tempFile, oldFile),
            currentEpochSeconds = now
        )

        assertNotNull(summary)
        // 1 duplicate copy (file2) + 1 large file (file3) + 1 temp file (file4) + 1 old file (file5) = 4 recommendations
        assertEquals(4, summary.totalRecommendationsCount)
        assertEquals(1, summary.duplicateFilesCount)
        assertEquals(1, summary.largeFilesCount)
        assertEquals(1, summary.temporaryFilesCount)
        assertEquals(1, summary.oldFilesCount)

        // Potential reclaimable bytes = 1000 (dup2) + 100MB (large) + 500 (temp) + 2000 (old)
        val expectedReclaimable = 1000L + (100L * 1024L * 1024L) + 500L + 2000L
        assertEquals(expectedReclaimable, summary.potentialReclaimableBytes)

        // Verify priorities and explainable reasons
        val dupRec = summary.recommendations.first { it.type == RecommendationType.DUPLICATE }
        assertEquals(RecommendationPriority.MEDIUM, dupRec.priority)
        assertTrue(dupRec.reason.contains("Arquivo duplicado"))

        val largeRec = summary.recommendations.first { it.type == RecommendationType.LARGE_FILE }
        assertTrue(largeRec.reason.contains("limite configurado"))

        val tempRec = summary.recommendations.first { it.type == RecommendationType.TEMPORARY_FILE }
        assertTrue(tempRec.reason.contains("temporário"))

        val oldRec = summary.recommendations.first { it.type == RecommendationType.OLD_FILE }
        assertTrue(oldRec.reason.contains("sem modificação"))
    }
}
