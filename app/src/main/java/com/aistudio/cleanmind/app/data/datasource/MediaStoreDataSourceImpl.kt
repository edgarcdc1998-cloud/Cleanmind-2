package com.aistudio.cleanmind.app.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import com.aistudio.cleanmind.app.domain.model.DeviceStorageStats
import com.aistudio.cleanmind.app.domain.model.StorageCategory
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.util.StorageFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MediaStoreDataSourceImpl(
    private val context: Context
) : StorageDataSource {

    override fun getDeviceStorageStats(): DeviceStorageStats {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong

            val totalBytes = totalBlocks * blockSize
            val freeBytes = availableBlocks * blockSize
            val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

            DeviceStorageStats(
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = usedBytes
            )
        } catch (e: Exception) {
            DeviceStorageStats(
                totalBytes = 0L,
                freeBytes = 0L,
                usedBytes = 0L
            )
        }
    }

    override suspend fun queryMediaFiles(): List<StorageFile> = withContext(Dispatchers.IO) {
        val filesList = mutableListOf<StorageFile>()
        val contentResolver = context.contentResolver

        // Query Images
        queryCollection(
            contentResolver = contentResolver,
            collectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            defaultCategory = StorageCategory.IMAGES,
            destination = filesList
        )

        // Query Videos
        queryCollection(
            contentResolver = contentResolver,
            collectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            defaultCategory = StorageCategory.VIDEOS,
            destination = filesList
        )

        // Query Audio
        queryCollection(
            contentResolver = contentResolver,
            collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            defaultCategory = StorageCategory.AUDIOS,
            destination = filesList
        )

        // Query Non-Media / Documents via Files table if accessible
        queryFilesCollection(
            contentResolver = contentResolver,
            destination = filesList
        )

        filesList
    }

    private fun queryCollection(
        contentResolver: ContentResolver,
        collectionUri: Uri,
        defaultCategory: StorageCategory,
        destination: MutableList<StorageFile>
    ) {
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        try {
            contentResolver.query(
                collectionUri,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateModCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "unnamed" else "unnamed"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) else 0L
                    val relPath = if (relativePathCol >= 0) cursor.getString(relativePathCol) else null

                    val itemUri = ContentUris.withAppendedId(collectionUri, id)
                    val ext = StorageFormatter.extractExtension(name)
                    val category = if (defaultCategory == StorageCategory.OTHERS) {
                        StorageFormatter.categorize(mime, name)
                    } else {
                        defaultCategory
                    }

                    destination.add(
                        StorageFile(
                            id = id,
                            uri = itemUri.toString(),
                            name = name,
                            sizeBytes = size,
                            mimeType = mime,
                            extension = ext,
                            dateModifiedEpochSeconds = dateMod,
                            category = category,
                            relativePath = relPath,
                            isReadable = true
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or partially granted; safe degradation
        } catch (_: Exception) {
            // Handle query exceptions gracefully without crashing
        }
    }

    private fun queryFilesCollection(
        contentResolver: ContentResolver,
        destination: MutableList<StorageFile>
    ) {
        val filesUri = MediaStore.Files.getContentUri("external")
        val projection = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        ).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.MediaColumns.RELATIVE_PATH)
            }
        }.toTypedArray()

        // Filter out images, video, audio to avoid duplicates from previous queries
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_NONE}"

        try {
            contentResolver.query(
                filesUri,
                projection,
                selection,
                null,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
                val dateModCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
                val relativePathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    val id = if (idCol >= 0) cursor.getLong(idCol) else 0L
                    val name = if (nameCol >= 0) cursor.getString(nameCol) ?: "unnamed" else "unnamed"
                    val size = if (sizeCol >= 0) cursor.getLong(sizeCol).coerceAtLeast(0L) else 0L
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) ?: "application/octet-stream" else "application/octet-stream"
                    val dateMod = if (dateModCol >= 0) cursor.getLong(dateModCol) else 0L
                    val relPath = if (relativePathCol >= 0) cursor.getString(relativePathCol) else null

                    val itemUri = ContentUris.withAppendedId(filesUri, id)
                    val ext = StorageFormatter.extractExtension(name)
                    val category = StorageFormatter.categorize(mime, name)

                    destination.add(
                        StorageFile(
                            id = id,
                            uri = itemUri.toString(),
                            name = name,
                            sizeBytes = size,
                            mimeType = mime,
                            extension = ext,
                            dateModifiedEpochSeconds = dateMod,
                            category = category,
                            relativePath = relPath,
                            isReadable = true
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Permission not granted or partially granted
        } catch (_: Exception) {
            // Handle query exceptions gracefully
        }
    }
}
