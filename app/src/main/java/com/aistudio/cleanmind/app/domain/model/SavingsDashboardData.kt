package com.aistudio.cleanmind.app.domain.model

data class SavingsDashboardData(
    val totalAnalysesCount: Int,
    val latestAnalysisEpochMillis: Long?,
    val latestAnalyzedSizeBytes: Long,
    val totalVolumeAnalyzedBytes: Long,
    val latestPotentialReclaimableBytes: Long,
    val totalPotentialReclaimableBytes: Long,
    val totalRecommendationsCount: Int,
    val duplicatesCount: Int,
    val largeFilesCount: Int,
    val oldFilesCount: Int,
    val tempFilesCount: Int,
    val timePoints: List<AnalysisTimePoint>,
    val weeklyAggregations: List<PeriodSavingsPoint> = emptyList(),
    val monthlyAggregations: List<PeriodSavingsPoint> = emptyList(),
    val trendSummary: SavingsTrendSummary? = null,
    val executedSavingsTracked: Boolean = false
)

data class AnalysisTimePoint(
    val analysisId: Long,
    val timestampEpochMillis: Long,
    val analyzedSizeBytes: Long,
    val potentialReclaimableBytes: Long,
    val filesCount: Int,
    val recommendationsCount: Int
)

data class PeriodSavingsPoint(
    val periodKey: String,
    val periodLabel: String,
    val analysesCount: Int,
    val totalAnalyzedSizeBytes: Long,
    val potentialReclaimableBytes: Long
)

data class SavingsTrendSummary(
    val analyzedDeltaBytes: Long,
    val reclaimableDeltaBytes: Long,
    val filesCountDelta: Int,
    val observationsCount: Int
)
