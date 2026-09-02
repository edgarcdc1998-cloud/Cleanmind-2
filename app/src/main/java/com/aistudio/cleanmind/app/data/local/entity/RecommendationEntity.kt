package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageCategory

@Entity(
    tableName = "recommendations",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisSummaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["analysisId"]),
        Index(value = ["analysisId", "type"])
    ]
)
data class RecommendationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val analysisId: Long,
    val fileId: Long,
    val fileName: String,
    val fileUri: String,
    val fileSizeBytes: Long,
    val category: StorageCategory,
    val type: RecommendationType,
    val priority: RecommendationPriority,
    val score: Int,
    val reason: String,
    val reclaimableSizeBytes: Long,
    val duplicateGroupId: String? = null
)
