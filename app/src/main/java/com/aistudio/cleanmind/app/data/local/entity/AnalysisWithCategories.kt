package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class AnalysisWithCategories(
    @Embedded
    val summary: AnalysisSummaryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "analysisId"
    )
    val categories: List<CategorySummaryEntity>
)
