package com.aistudio.cleanmind.app.domain.repository

interface FileHashRepository {
    suspend fun calculateContentHash(uri: String): String?
}
