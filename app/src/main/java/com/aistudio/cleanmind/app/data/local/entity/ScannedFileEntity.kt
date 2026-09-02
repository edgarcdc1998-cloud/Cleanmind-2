package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aistudio.cleanmind.app.domain.model.StorageCategory

@Entity(
    tableName = "scanned_files",
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
        Index(value = ["analysisId", "category"])
    ]
)
data class ScannedFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val analysisId: Long,
    val originalFileId: Long,
    val name: String,
    val uri: String,
    val sizeBytes: Long,
    val mimeType: String,
    val extension: String,
    val dateModifiedEpochSeconds: Long,
    val category: StorageCategory
)
