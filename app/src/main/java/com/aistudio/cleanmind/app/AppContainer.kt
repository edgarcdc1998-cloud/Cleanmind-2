package com.aistudio.cleanmind.app

import android.content.Context
import com.aistudio.cleanmind.app.data.datasource.MediaStoreDataSourceImpl
import com.aistudio.cleanmind.app.data.datasource.StorageDataSource
import com.aistudio.cleanmind.app.data.local.database.CleanMindDatabase
import com.aistudio.cleanmind.app.data.repository.AnalysisHistoryRepositoryImpl
import com.aistudio.cleanmind.app.data.repository.FileHashRepositoryImpl
import com.aistudio.cleanmind.app.data.repository.SettingsRepositoryImpl
import com.aistudio.cleanmind.app.data.repository.StorageRepositoryImpl
import com.aistudio.cleanmind.app.domain.repository.AnalysisHistoryRepository
import com.aistudio.cleanmind.app.domain.repository.FileHashRepository
import com.aistudio.cleanmind.app.domain.repository.SettingsRepository
import com.aistudio.cleanmind.app.domain.repository.StorageRepository
import com.aistudio.cleanmind.app.domain.usecase.AnalyzeStorageUseCase
import com.aistudio.cleanmind.app.domain.usecase.FindDuplicateFilesUseCase
import com.aistudio.cleanmind.app.domain.usecase.FindLargeFilesUseCase
import com.aistudio.cleanmind.app.domain.usecase.FindOldFilesUseCase
import com.aistudio.cleanmind.app.domain.usecase.FindTemporaryFilesUseCase
import com.aistudio.cleanmind.app.domain.usecase.GenerateCleanupRecommendationsUseCase
import com.aistudio.cleanmind.app.domain.usecase.ClearAnalysisHistoryUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetDeviceStorageStatsUseCase
import com.aistudio.cleanmind.app.domain.usecase.GetLatestAnalysisUseCase
import com.aistudio.cleanmind.app.domain.usecase.DeleteSelectedFilesUseCase

interface AppContainer {
    val database: CleanMindDatabase
    val storageDataSource: StorageDataSource
    val storageRepository: StorageRepository
    val historyRepository: AnalysisHistoryRepository
    val fileHashRepository: FileHashRepository
    val settingsRepository: SettingsRepository

    val findLargeFilesUseCase: FindLargeFilesUseCase
    val findDuplicateFilesUseCase: FindDuplicateFilesUseCase
    val findOldFilesUseCase: FindOldFilesUseCase
    val findTemporaryFilesUseCase: FindTemporaryFilesUseCase
    val generateCleanupRecommendationsUseCase: GenerateCleanupRecommendationsUseCase
    val getDeviceStorageStatsUseCase: GetDeviceStorageStatsUseCase
    val analyzeStorageUseCase: AnalyzeStorageUseCase
    val getLatestAnalysisUseCase: GetLatestAnalysisUseCase
    val clearAnalysisHistoryUseCase: ClearAnalysisHistoryUseCase
    val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase
}

class DefaultAppContainer(private val context: Context) : AppContainer {
    override val database: CleanMindDatabase by lazy {
        CleanMindDatabase.getInstance(context)
    }

    override val storageDataSource: StorageDataSource by lazy {
        MediaStoreDataSourceImpl(context)
    }

    override val storageRepository: StorageRepository by lazy {
        StorageRepositoryImpl(storageDataSource)
    }

    override val historyRepository: AnalysisHistoryRepository by lazy {
        AnalysisHistoryRepositoryImpl(database.analysisDao())
    }

    override val fileHashRepository: FileHashRepository by lazy {
        FileHashRepositoryImpl(context)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(context)
    }

    override val findLargeFilesUseCase: FindLargeFilesUseCase by lazy {
        FindLargeFilesUseCase()
    }

    override val findDuplicateFilesUseCase: FindDuplicateFilesUseCase by lazy {
        FindDuplicateFilesUseCase(fileHashRepository)
    }

    override val findOldFilesUseCase: FindOldFilesUseCase by lazy {
        FindOldFilesUseCase()
    }

    override val findTemporaryFilesUseCase: FindTemporaryFilesUseCase by lazy {
        FindTemporaryFilesUseCase()
    }

    override val generateCleanupRecommendationsUseCase: GenerateCleanupRecommendationsUseCase by lazy {
        GenerateCleanupRecommendationsUseCase(
            findLargeFilesUseCase = findLargeFilesUseCase,
            findDuplicateFilesUseCase = findDuplicateFilesUseCase,
            findOldFilesUseCase = findOldFilesUseCase,
            findTemporaryFilesUseCase = findTemporaryFilesUseCase
        )
    }

    override val getDeviceStorageStatsUseCase: GetDeviceStorageStatsUseCase by lazy {
        GetDeviceStorageStatsUseCase(storageRepository)
    }

    override val analyzeStorageUseCase: AnalyzeStorageUseCase by lazy {
        AnalyzeStorageUseCase(
            repository = storageRepository,
            historyRepository = historyRepository,
            generateCleanupRecommendationsUseCase = generateCleanupRecommendationsUseCase
        )
    }

    override val getLatestAnalysisUseCase: GetLatestAnalysisUseCase by lazy {
        GetLatestAnalysisUseCase(historyRepository)
    }

    override val clearAnalysisHistoryUseCase: ClearAnalysisHistoryUseCase by lazy {
        ClearAnalysisHistoryUseCase(historyRepository)
    }

    override val deleteSelectedFilesUseCase: DeleteSelectedFilesUseCase by lazy {
        DeleteSelectedFilesUseCase(context)
    }
}
