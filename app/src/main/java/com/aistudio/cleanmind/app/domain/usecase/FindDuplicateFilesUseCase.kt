package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.DuplicateGroup
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.domain.repository.FileHashRepository

class FindDuplicateFilesUseCase(
    private val fileHashRepository: FileHashRepository
) {
    suspend operator fun invoke(files: List<StorageFile>): List<DuplicateGroup> {
        if (files.size < 2) return emptyList()

        // 1. Agrupar arquivos pelo tamanho (descartando arquivos vazios e grupos unitários)
        val candidateSizeGroups = files.asSequence()
            .filter { it.sizeBytes > 0L }
            .groupBy { it.sizeBytes }
            .filter { it.value.size >= 2 }

        if (candidateSizeGroups.isEmpty()) return emptyList()

        val duplicateGroups = mutableListOf<DuplicateGroup>()

        // 2. Para arquivos com mesmo tamanho, calcular hash streaming somente quando necessário
        for ((sizeBytes, candidateFiles) in candidateSizeGroups) {
            val hashesMap = mutableMapOf<String, MutableList<StorageFile>>()

            for (file in candidateFiles) {
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val hash = fileHashRepository.calculateContentHash(file.uri)
                if (hash != null) {
                    hashesMap.getOrPut(hash) { mutableListOf() }.add(file)
                }
            }

            // 3. Agrupar arquivos com conteúdo realmente igual (mesmo hash SHA-256)
            for ((hash, matchingFiles) in hashesMap) {
                if (matchingFiles.size >= 2) {
                    // Ordenar por data de modificação: o mais antigo é a referência/original
                    val sortedByAge = matchingFiles.sortedBy { it.dateModifiedEpochSeconds }
                    val reclaimable = (sortedByAge.size - 1) * sizeBytes
                    duplicateGroups.add(
                        DuplicateGroup(
                            groupId = "dup_${sizeBytes}_${hash.take(8)}",
                            totalSizeBytes = sortedByAge.size * sizeBytes,
                            reclaimableSizeBytes = reclaimable,
                            files = sortedByAge
                        )
                    )
                }
            }
        }

        return duplicateGroups.sortedByDescending { it.reclaimableSizeBytes }
    }
}
