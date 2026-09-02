package com.aistudio.cleanmind.app.util

import com.aistudio.cleanmind.app.domain.model.StorageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StorageFormatterTest {

    @Test
    fun extractExtension_returnsCorrectLowercasedExtension() {
        assertEquals("jpg", StorageFormatter.extractExtension("photo.jpg"))
        assertEquals("png", StorageFormatter.extractExtension("IMAGE.PNG"))
        assertEquals("pdf", StorageFormatter.extractExtension("Document.V1.Final.PDF"))
        assertEquals("mp4", StorageFormatter.extractExtension("/storage/emulated/0/Movies/clip.MP4"))
    }

    @Test
    fun extractExtension_returnsEmptyWhenNoExtension() {
        assertEquals("", StorageFormatter.extractExtension("README"))
        assertEquals("", StorageFormatter.extractExtension(".nomedia"))
        assertEquals("", StorageFormatter.extractExtension(""))
    }

    @Test
    fun categorize_images() {
        assertEquals(StorageCategory.IMAGES, StorageFormatter.categorize("image/jpeg", "vacation.jpg"))
        assertEquals(StorageCategory.IMAGES, StorageFormatter.categorize("application/octet-stream", "picture.png"))
        assertEquals(StorageCategory.IMAGES, StorageFormatter.categorize("image/webp", "icon.webp"))
    }

    @Test
    fun categorize_videos() {
        assertEquals(StorageCategory.VIDEOS, StorageFormatter.categorize("video/mp4", "clip.mp4"))
        assertEquals(StorageCategory.VIDEOS, StorageFormatter.categorize("application/octet-stream", "movie.mkv"))
        assertEquals(StorageCategory.VIDEOS, StorageFormatter.categorize("video/quicktime", "record.mov"))
    }

    @Test
    fun categorize_audios() {
        assertEquals(StorageCategory.AUDIOS, StorageFormatter.categorize("audio/mpeg", "song.mp3"))
        assertEquals(StorageCategory.AUDIOS, StorageFormatter.categorize("application/octet-stream", "track.flac"))
        assertEquals(StorageCategory.AUDIOS, StorageFormatter.categorize("audio/ogg", "voice.ogg"))
    }

    @Test
    fun categorize_documents() {
        assertEquals(StorageCategory.DOCUMENTS, StorageFormatter.categorize("application/pdf", "receipt.pdf"))
        assertEquals(StorageCategory.DOCUMENTS, StorageFormatter.categorize("text/plain", "notes.txt"))
        assertEquals(StorageCategory.DOCUMENTS, StorageFormatter.categorize("application/vnd.ms-excel", "sheet.xlsx"))
        assertEquals(StorageCategory.DOCUMENTS, StorageFormatter.categorize("application/zip", "archive.zip"))
    }

    @Test
    fun categorize_others_whenUnknownMimeAndExtension() {
        assertEquals(
            StorageCategory.OTHERS,
            StorageFormatter.categorize("application/octet-stream", "data.unknown_bin")
        )
    }

    @Test
    fun formatBytes_formatsProperly() {
        assertEquals("0 B", StorageFormatter.formatBytes(0L))
        assertEquals("500 B", StorageFormatter.formatBytes(500L))
        assertTrue(StorageFormatter.formatBytes(1024L).contains("KB"))
        assertTrue(StorageFormatter.formatBytes((1.5 * 1024 * 1024).toLong()).contains("MB"))
        assertTrue(StorageFormatter.formatBytes((2.0 * 1024 * 1024 * 1024).toLong()).contains("GB"))
    }
}
