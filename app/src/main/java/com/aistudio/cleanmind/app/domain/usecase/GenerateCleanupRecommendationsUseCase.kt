package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.AnalysisRecommendationsSummary
import com.aistudio.cleanmind.app.domain.model.CleanupRecommendation
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageFile
import com.aistudio.cleanmind.app.util.StorageFormatter

class GenerateCleanupRecommendationsUseCase(
    private val findLargeFilesUseCase: FindLargeFilesUseCase = FindLargeFilesUseCase(),
    private val findDuplicateFilesUseCase: FindDuplicateFilesUseCase,
    private val findOldFilesUseCase: FindOldFilesUseCase = FindOldFilesUseCase(),
    private val findTemporaryFilesUseCase: FindTemporaryFilesUseCase = FindTemporaryFilesUseCase()
) {

    suspend operator fun invoke(
        files: List<StorageFile>,
        currentEpochSeconds: Long = System.currentTimeMillis() / 1000L
    ): AnalysisRecommendationsSummary {
        if (files.isEmpty()) {
            return AnalysisRecommendationsSummary.EMPTY
        }

        val duplicateGroups = findDuplicateFilesUseCase(files)
        val largeFiles = findLargeFilesUseCase(files)
        val oldFiles = findOldFilesUseCase(files, currentEpochSeconds = currentEpochSeconds)
        val temporaryFiles = findTemporaryFilesUseCase(files)

        val recommendations = mutableListOf<CleanupRecommendation>()
        val processedFileIds = mutableSetOf<Long>()

        // 1. Processar Duplicados
        // Cada grupo mantém o primeiro arquivo (mais antigo) como original. Os demais viram recomendações.
        for (group in duplicateGroups) {
            val duplicateCopies = group.files.drop(1)
            for (file in duplicateCopies) {
                val isAlsoLarge = file.sizeBytes >= FindLargeFilesUseCase.DEFAULT_THRESHOLD_BYTES
                val baseScore = if (file.sizeBytes > 50L * 1024L * 1024L) {
                    95
                } else if (file.sizeBytes > 10L * 1024L * 1024L) {
                    90
                } else {
                    85
                }

                val reason = if (isAlsoLarge) {
                    "Arquivo duplicado idêntico a outro arquivo no dispositivo com grande volume (${StorageFormatter.formatBytes(file.sizeBytes)}). Manter apenas o original libera espaço com segurança."
                } else {
                    "Arquivo duplicado idêntico a outro arquivo existente (${StorageFormatter.formatBytes(file.sizeBytes)}). Manter um único exemplar elimina redundância."
                }

                val priority = if (file.sizeBytes > 10L * 1024L * 1024L) {
                    RecommendationPriority.HIGH
                } else {
                    RecommendationPriority.MEDIUM
                }

                recommendations.add(
                    CleanupRecommendation(
                        id = 0L,
                        file = file,
                        type = RecommendationType.DUPLICATE,
                        priority = priority,
                        score = baseScore,
                        reason = reason,
                        reclaimableSizeBytes = file.sizeBytes,
                        duplicateGroupId = group.groupId
                    )
                )
                processedFileIds.add(file.id)
            }
        }

        // 2. Processar Arquivos Grandes
        for (file in largeFiles) {
            if (file.id !in processedFileIds) {
                val score = when {
                    file.sizeBytes >= 500L * 1024L * 1024L -> 85
                    file.sizeBytes >= 200L * 1024L * 1024L -> 75
                    else -> 65
                }

                val priority = if (file.sizeBytes >= 200L * 1024L * 1024L) {
                    RecommendationPriority.HIGH
                } else {
                    RecommendationPriority.MEDIUM
                }

                val reason = "Arquivo da categoria ${file.category.displayName} ocupando volume substancial de ${StorageFormatter.formatBytes(file.sizeBytes)} (acima do limite configurado de 50 MB)."

                recommendations.add(
                    CleanupRecommendation(
                        id = 0L,
                        file = file,
                        type = RecommendationType.LARGE_FILE,
                        priority = priority,
                        score = score,
                        reason = reason,
                        reclaimableSizeBytes = file.sizeBytes
                    )
                )
                processedFileIds.add(file.id)
            }
        }

        // 3. Processar Arquivos Temporários
        for (file in temporaryFiles) {
            if (file.id !in processedFileIds) {
                val score = if (file.sizeBytes > 5L * 1024L * 1024L) 80 else 70
                val reason = "Arquivo temporário ou residual (.${file.extension}) que costuma ser descartável com segurança (${StorageFormatter.formatBytes(file.sizeBytes)})."

                recommendations.add(
                    CleanupRecommendation(
                        id = 0L,
                        file = file,
                        type = RecommendationType.TEMPORARY_FILE,
                        priority = RecommendationPriority.MEDIUM,
                        score = score,
                        reason = reason,
                        reclaimableSizeBytes = file.sizeBytes
                    )
                )
                processedFileIds.add(file.id)
            }
        }

        // 4. Processar Arquivos Antigos
        for (file in oldFiles) {
            if (file.id !in processedFileIds) {
                val ageSeconds = (currentEpochSeconds - file.dateModifiedEpochSeconds).coerceAtLeast(0L)
                val months = (ageSeconds / (30L * 86400L)).coerceAtLeast(6L)
                val score = if (file.sizeBytes >= 20L * 1024L * 1024L) 60 else 45
                val priority = if (file.sizeBytes >= 20L * 1024L * 1024L) {
                    RecommendationPriority.MEDIUM
                } else {
                    RecommendationPriority.LOW
                }

                val reason = "Arquivo da categoria ${file.category.displayName} sem modificação há mais de $months meses (${StorageFormatter.formatBytes(file.sizeBytes)})."

                recommendations.add(
                    CleanupRecommendation(
                        id = 0L,
                        file = file,
                        type = RecommendationType.OLD_FILE,
                        priority = priority,
                        score = score,
                        reason = reason,
                        reclaimableSizeBytes = file.sizeBytes
                    )
                )
                processedFileIds.add(file.id)
            }
        }

        // Ordenar recomendações por relevância explicável (score decrescente, tamanho decrescente)
        val sortedRecommendations = recommendations.sortedWith(
            compareByDescending<CleanupRecommendation> { it.score }
                .thenByDescending { it.reclaimableSizeBytes }
        )

        val totalReclaimable = sortedRecommendations.sumOf { it.reclaimableSizeBytes }
        val duplicateCount = sortedRecommendations.count { it.type == RecommendationType.DUPLICATE }
        val largeCount = sortedRecommendations.count { it.type == RecommendationType.LARGE_FILE }
        val oldCount = sortedRecommendations.count { it.type == RecommendationType.OLD_FILE }
        val tempCount = sortedRecommendations.count { it.type == RecommendationType.TEMPORARY_FILE }

        return AnalysisRecommendationsSummary(
            totalRecommendationsCount = sortedRecommendations.size,
            potentialReclaimableBytes = totalReclaimable,
            largeFilesCount = largeCount,
            duplicateFilesCount = duplicateCount,
            duplicateGroupsCount = duplicateGroups.size,
            oldFilesCount = oldCount,
            temporaryFilesCount = tempCount,
            recommendations = sortedRecommendations,
            duplicateGroups = duplicateGroups
        )
    }
}
