package com.aistudio.cleanmind.app.data.repository

import android.content.Context
import android.net.Uri
import com.aistudio.cleanmind.app.domain.repository.FileHashRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

class FileHashRepositoryImpl(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FileHashRepository {

    override suspend fun calculateContentHash(uri: String): String? = withContext(ioDispatcher) {
        try {
            val parsedUri = Uri.parse(uri)
            context.contentResolver.openInputStream(parsedUri)?.use { inputStream ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val BUFFER_SIZE = 16384 // 16 KB buffer for streaming low memory consumption
    }
}
