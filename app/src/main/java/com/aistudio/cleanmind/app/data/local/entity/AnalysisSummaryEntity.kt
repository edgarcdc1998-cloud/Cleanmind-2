package com.aistudio.cleanmind.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "analysis_summaries")
data class AnalysisSummaryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val timestampEpochMillis: Long,
    val totalFilesCount: Int,
    val totalAnalyzedSizeBytes: Long,
    val deviceTotalBytes: Long,
    val deviceUsedBytes: Long,
    val deviceFreeBytes: Long
)
