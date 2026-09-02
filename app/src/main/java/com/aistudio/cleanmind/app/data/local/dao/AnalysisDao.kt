package com.aistudio.cleanmind.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.AnalysisWithCategories
import com.aistudio.cleanmind.app.data.local.entity.AnalysisWithDetails
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.RecommendationEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnalysisDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSummary(summary: AnalysisSummaryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategorySummaryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScannedFiles(files: List<ScannedFileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendations(recommendations: List<RecommendationEntity>)

    @Transaction
    suspend fun insertFullAnalysis(
        summary: AnalysisSummaryEntity,
        categories: List<CategorySummaryEntity>,
        files: List<ScannedFileEntity>,
        recommendations: List<RecommendationEntity> = emptyList(),
        keepMaxAnalyses: Int = 10
    ): Long {
        val analysisId = insertSummary(summary)
        if (categories.isNotEmpty()) {
            val categoriesWithId = categories.map { it.copy(analysisId = analysisId) }
            insertCategories(categoriesWithId)
        }
        if (files.isNotEmpty()) {
            val filesWithId = files.map { it.copy(analysisId = analysisId) }
            insertScannedFiles(filesWithId)
        }
        if (recommendations.isNotEmpty()) {
            val recsWithId = recommendations.map { it.copy(analysisId = analysisId) }
            insertRecommendations(recsWithId)
        }
        pruneOldAnalyses(keepMaxAnalyses)
        return analysisId
    }

    @Transaction
    @Query("SELECT * FROM analysis_summaries ORDER BY timestampEpochMillis DESC LIMIT 1")
    fun getLatestAnalysisWithCategories(): Flow<AnalysisWithCategories?>

    @Transaction
    @Query("SELECT * FROM analysis_summaries ORDER BY timestampEpochMillis DESC LIMIT 1")
    fun getLatestAnalysisWithDetails(): Flow<AnalysisWithDetails?>

    @Query("SELECT * FROM recommendations WHERE analysisId = :analysisId ORDER BY score DESC, reclaimableSizeBytes DESC")
    fun getRecommendations(analysisId: Long): Flow<List<RecommendationEntity>>

    @Query("SELECT * FROM recommendations WHERE analysisId = :analysisId ORDER BY score DESC, reclaimableSizeBytes DESC")
    suspend fun getRecommendationsList(analysisId: Long): List<RecommendationEntity>

    @Query("SELECT * FROM analysis_summaries ORDER BY timestampEpochMillis DESC LIMIT 1")
    suspend fun getLatestSummary(): AnalysisSummaryEntity?

    @Query("SELECT * FROM scanned_files WHERE analysisId = :analysisId ORDER BY sizeBytes DESC")
    fun getScannedFiles(analysisId: Long): Flow<List<ScannedFileEntity>>

    @Query("SELECT * FROM scanned_files WHERE analysisId = :analysisId ORDER BY sizeBytes DESC")
    suspend fun getScannedFilesList(analysisId: Long): List<ScannedFileEntity>

    @Query("SELECT * FROM category_summaries WHERE analysisId = :analysisId")
    suspend fun getCategorySummariesForAnalysis(analysisId: Long): List<CategorySummaryEntity>

    @Query("SELECT * FROM analysis_summaries ORDER BY timestampEpochMillis DESC")
    fun getAllSummaries(): Flow<List<AnalysisSummaryEntity>>

    @Query("SELECT COUNT(*) FROM analysis_summaries")
    suspend fun getAnalysesCount(): Int

    @Query("DELETE FROM analysis_summaries WHERE id NOT IN (SELECT id FROM analysis_summaries ORDER BY timestampEpochMillis DESC LIMIT :keepCount)")
    suspend fun pruneOldAnalyses(keepCount: Int)

    @Query("DELETE FROM analysis_summaries")
    suspend fun clearAll()
}
