package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aistudio.cleanmind.app.domain.model.StorageCategory

@Entity(
    tableName = "category_summaries",
    foreignKeys = [
        ForeignKey(
            entity = AnalysisSummaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["analysisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["analysisId"])
    ]
)
data class CategorySummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val analysisId: Long,
    val category: StorageCategory,
    val fileCount: Int,
    val totalSizeBytes: Long
)
