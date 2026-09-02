package com.aistudio.cleanmind.app.util

import com.aistudio.cleanmind.app.domain.model.StorageCategory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object StorageFormatter {

    private val formatOneDecimal = DecimalFormat("#,##0.0", java.text.DecimalFormatSymbols(Locale.getDefault()))
    private val formatTwoDecimals = DecimalFormat("#,##0.00", java.text.DecimalFormatSymbols(Locale.getDefault()))

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val kilo = 1024.0
        val mega = kilo * 1024
        val giga = mega * 1024
        val tera = giga * 1024

        val doubleBytes = bytes.toDouble()

        return when {
            doubleBytes >= tera -> "${formatTwoDecimals.format(doubleBytes / tera)} TB"
            doubleBytes >= giga -> "${formatTwoDecimals.format(doubleBytes / giga)} GB"
            doubleBytes >= mega -> "${formatOneDecimal.format(doubleBytes / mega)} MB"
            doubleBytes >= kilo -> "${formatOneDecimal.format(doubleBytes / kilo)} KB"
            else -> "$bytes B"
        }
    }

    fun extractExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex > 0 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }
    }

    fun categorize(mimeType: String, fileName: String): StorageCategory {
        val lowerMime = mimeType.lowercase(Locale.ROOT)
        val ext = extractExtension(fileName)

        return when {
            lowerMime.startsWith("image/") || ext in IMAGE_EXTENSIONS -> StorageCategory.IMAGES
            lowerMime.startsWith("video/") || ext in VIDEO_EXTENSIONS -> StorageCategory.VIDEOS
            lowerMime.startsWith("audio/") || ext in AUDIO_EXTENSIONS -> StorageCategory.AUDIOS
            lowerMime.startsWith("text/") ||
                lowerMime.contains("pdf") ||
                lowerMime.contains("document") ||
                lowerMime.contains("spreadsheet") ||
                lowerMime.contains("presentation") ||
                ext in DOCUMENT_EXTENSIONS -> StorageCategory.DOCUMENTS
            else -> StorageCategory.OTHERS
        }
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return formatter.format(Date(epochMillis))
    }

    private val IMAGE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "svg", "raw", "dng"
    )

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "mkv", "webm", "avi", "mov", "3gp", "ts", "flv", "wmv", "m4v"
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "wav", "ogg", "m4a", "flac", "aac", "wma", "opus", "mid", "amr"
    )

    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "json", "xml", "epub",
        "zip", "rar", "tar", "gz", "7z"
    )
}
