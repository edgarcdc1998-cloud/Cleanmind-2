package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.StorageFile

class FindTemporaryFilesUseCase {

    operator fun invoke(files: List<StorageFile>): List<StorageFile> {
        return files.asSequence()
            .filter { file ->
                val ext = file.extension.lowercase()
                val name = file.name.lowercase()
                ext in TEMPORARY_EXTENSIONS ||
                    name.startsWith("~") ||
                    name.startsWith(".tmp") ||
                    name.endsWith(".tmp") ||
                    ext.contains("cache")
            }
            .sortedByDescending { it.sizeBytes }
            .toList()
    }

    companion object {
        val TEMPORARY_EXTENSIONS = setOf(
            "tmp", "temp", "log", "bak", "cache", "old", "dmp"
        )
    }
}
