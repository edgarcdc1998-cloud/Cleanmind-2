package com.aistudio.cleanmind.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.aistudio.cleanmind.app.data.local.dao.AnalysisDao
import com.aistudio.cleanmind.app.data.local.entity.AnalysisSummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.CategorySummaryEntity
import com.aistudio.cleanmind.app.data.local.entity.ScannedFileEntity

@Database(
    entities = [
        AnalysisSummaryEntity::class,
        CategorySummaryEntity::class,
        ScannedFileEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StorageTypeConverters::class)
abstract class CleanMindDatabase : RoomDatabase() {

    abstract fun analysisDao(): AnalysisDao

    companion object {
        const val DATABASE_NAME = "cleanmind_database"

        @Volatile
        private var INSTANCE: CleanMindDatabase? = null

        fun getInstance(context: Context): CleanMindDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CleanMindDatabase::class.java,
                    DATABASE_NAME
                )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
