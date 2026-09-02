package com.aistudio.cleanmind.app.domain.model

data class AnalysisRecommendationsSummary(
    val totalRecommendationsCount: Int,
    val potentialReclaimableBytes: Long,
    val largeFilesCount: Int,
    val duplicateFilesCount: Int,
    val duplicateGroupsCount: Int,
    val oldFilesCount: Int,
    val temporaryFilesCount: Int,
    val recommendations: List<CleanupRecommendation>,
    val duplicateGroups: List<DuplicateGroup>
) {
    companion object {
        val EMPTY = AnalysisRecommendationsSummary(
            totalRecommendationsCount = 0,
            potentialReclaimableBytes = 0L,
            largeFilesCount = 0,
            duplicateFilesCount = 0,
            duplicateGroupsCount = 0,
            oldFilesCount = 0,
            temporaryFilesCount = 0,
            recommendations = emptyList(),
            duplicateGroups = emptyList()
        )
    }
}
