package com.aistudio.cleanmind.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.aistudio.cleanmind.app.data.local.dao.AnalysisDao
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.RecommendationEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity

@Database(
    entities = [
        AnalysisSummaryEntity::class,
        CategorySummaryEntity::class,
        ScannedFileEntity::class,
        RecommendationEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(StorageTypeConverters::class)
abstract class CleanMindDatabase : RoomDatabase() {

    abstract fun analysisDao(): AnalysisDao

    companion object {
        const val DATABASE_NAME = "cleanmind_database"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `recommendations` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `analysisId` INTEGER NOT NULL,
                        `fileId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileUri` TEXT NOT NULL,
                        `fileSizeBytes` INTEGER NOT NULL,
                        `category` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `priority` TEXT NOT NULL,
                        `score` INTEGER NOT NULL,
                        `reason` TEXT NOT NULL,
                        `reclaimableSizeBytes` INTEGER NOT NULL,
                        `duplicateGroupId` TEXT,
                        FOREIGN KEY(`analysisId`) REFERENCES `analysis_summaries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendations_analysisId` ON `recommendations` (`analysisId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_recommendations_analysisId_type` ON `recommendations` (`analysisId`, `type`)")
            }
        }

        @Volatile
        private var INSTANCE: CleanMindDatabase? = null

        fun getInstance(context: Context): CleanMindDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CleanMindDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
