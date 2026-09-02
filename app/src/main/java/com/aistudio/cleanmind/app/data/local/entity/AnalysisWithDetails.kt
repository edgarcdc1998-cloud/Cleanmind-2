package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AnalysisWithDetails(
    @Embedded
    val summary: AnalysisSummaryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "analysisId"
    )
    val categories: List<CategorySummaryEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "analysisId"
    )
    val recommendations: List<RecommendationEntity>
)
