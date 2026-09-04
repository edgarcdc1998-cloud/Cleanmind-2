package com.aistudio.cleanmind.app.domain.usecase

import com.aistudio.cleanmind.app.domain.model.AnalysisTimePoint
import com.aistudio.cleanmind.app.domain.model.PeriodSavingsPoint
import com.aistudio.cleanmind.app.domain.model.SavingsDashboardData
import com.aistudio.cleanmind.app.domain.model.SavingsTrendSummary
import com.aistudio.cleanmind.app.domain.model.StorageAnalysisResult
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class GetStorageSavingsDashboardUseCase(
    private val analysisHistoryRepository: AnalysisHistoryRepository,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default
) {

    operator fun invoke(): Flow<SavingsDashboardData> {
        return analysisHistoryRepository.getAllAnalyses()
            .map { analyses -> calculateDashboardData(analyses) }
            .flowOn(defaultDispatcher)
    }

    private fun calculateDashboardData(analyses: List<StorageAnalysisResult>): SavingsDashboardData {
        if (analyses.isEmpty()) {
            return SavingsDashboardData(
                totalAnalysesCount = 0,
                latestAnalysisEpochMillis = null,
                latestAnalyzedSizeBytes = 0L,
                totalVolumeAnalyzedBytes = 0L,
                latestPotentialReclaimableBytes = 0L,
                totalPotentialReclaimableBytes = 0L,
                totalRecommendationsCount = 0,
                duplicatesCount = 0,
                largeFilesCount = 0,
                oldFilesCount = 0,
                tempFilesCount = 0,
                timePoints = emptyList(),
                weeklyAggregations = emptyList(),
                monthlyAggregations = emptyList(),
                trendSummary = null,
                executedSavingsTracked = false
            )
        }

        val sortedDesc = analyses.sortedByDescending { it.timestampEpochMillis }
        val latest = sortedDesc.first()

        val sortedChronological = analyses.sortedBy { it.timestampEpochMillis }

        val timePoints = sortedChronological.map { item ->
            AnalysisTimePoint(
                analysisId = item.id,
                timestampEpochMillis = item.timestampEpochMillis,
                analyzedSizeBytes = item.totalAnalyzedSizeBytes,
                potentialReclaimableBytes = item.recommendationsSummary?.potentialReclaimableBytes ?: 0L,
                filesCount = item.totalFilesCount,
                recommendationsCount = item.recommendationsSummary?.totalRecommendationsCount ?: 0
            )
        }

        val trendSummary = if (sortedChronological.size >= 2) {
            val earliest = sortedChronological.first()
            val last = sortedChronological.last()
            val earliestReclaimable = earliest.recommendationsSummary?.potentialReclaimableBytes ?: 0L
            val lastReclaimable = last.recommendationsSummary?.potentialReclaimableBytes ?: 0L

            SavingsTrendSummary(
                analyzedDeltaBytes = last.totalAnalyzedSizeBytes - earliest.totalAnalyzedSizeBytes,
                reclaimableDeltaBytes = lastReclaimable - earliestReclaimable,
                filesCountDelta = last.totalFilesCount - earliest.totalFilesCount,
                observationsCount = sortedChronological.size
            )
        } else {
            null
        }

        val weeklyAggregations = aggregateWeekly(sortedChronological)
        val monthlyAggregations = aggregateMonthly(sortedChronological)

        val latestRecs = latest.recommendationsSummary

        return SavingsDashboardData(
            totalAnalysesCount = analyses.size,
            latestAnalysisEpochMillis = latest.timestampEpochMillis,
            latestAnalyzedSizeBytes = latest.totalAnalyzedSizeBytes,
            totalVolumeAnalyzedBytes = analyses.sumOf { it.totalAnalyzedSizeBytes },
            latestPotentialReclaimableBytes = latestRecs?.potentialReclaimableBytes ?: 0L,
            totalPotentialReclaimableBytes = analyses.sumOf { it.recommendationsSummary?.potentialReclaimableBytes ?: 0L },
            totalRecommendationsCount = latestRecs?.totalRecommendationsCount ?: 0,
            duplicatesCount = latestRecs?.duplicateFilesCount ?: 0,
            largeFilesCount = latestRecs?.largeFilesCount ?: 0,
            oldFilesCount = latestRecs?.oldFilesCount ?: 0,
            tempFilesCount = latestRecs?.temporaryFilesCount ?: 0,
            timePoints = timePoints,
            weeklyAggregations = weeklyAggregations,
            monthlyAggregations = monthlyAggregations,
            trendSummary = trendSummary,
            executedSavingsTracked = false
        )
    }

    private fun aggregateWeekly(analyses: List<StorageAnalysisResult>): List<PeriodSavingsPoint> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        return analyses
            .groupBy { item ->
                calendar.timeInMillis = item.timestampEpochMillis
                val year = calendar.get(Calendar.YEAR)
                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                "$year-W${String.format(Locale.ROOT, "%02d", week)}"
            }
            .map { (key, group) ->
                val firstEpoch = group.first().timestampEpochMillis
                calendar.timeInMillis = firstEpoch
                val week = calendar.get(Calendar.WEEK_OF_YEAR)
                val year = calendar.get(Calendar.YEAR)
                PeriodSavingsPoint(
                    periodKey = key,
                    periodLabel = "Semana $week, $year",
                    analysesCount = group.size,
                    totalAnalyzedSizeBytes = group.map { it.totalAnalyzedSizeBytes }.average().toLong(),
                    potentialReclaimableBytes = group.map { it.recommendationsSummary?.potentialReclaimableBytes ?: 0L }.average().toLong()
                )
            }
            .sortedBy { it.periodKey }
    }

    private fun aggregateMonthly(analyses: List<StorageAnalysisResult>): List<PeriodSavingsPoint> {
        val calendar = Calendar.getInstance(Locale.getDefault())
        val monthFormat = SimpleDateFormat("MMMM/yyyy", Locale.getDefault())
        return analyses
            .groupBy { item ->
                calendar.timeInMillis = item.timestampEpochMillis
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH) + 1
                "$year-${String.format(Locale.ROOT, "%02d", month)}"
            }
            .map { (key, group) ->
                val firstEpoch = group.first().timestampEpochMillis
                val label = monthFormat.format(Date(firstEpoch))
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                PeriodSavingsPoint(
                    periodKey = key,
                    periodLabel = label,
                    analysesCount = group.size,
                    totalAnalyzedSizeBytes = group.map { it.totalAnalyzedSizeBytes }.average().toLong(),
                    potentialReclaimableBytes = group.map { it.recommendationsSummary?.potentialReclaimableBytes ?: 0L }.average().toLong()
                )
            }
            .sortedBy { it.periodKey }
    }
}
