package com.aistudio.cleanmind.app.data.local.database

import androidx.room.TypeConverter
import com.aistudio.cleanmind.app.domain.model.RecommendationPriority
import com.aistudio.cleanmind.app.domain.model.RecommendationType
import com.aistudio.cleanmind.app.domain.model.StorageCategory

class StorageTypeConverters {

    @TypeConverter
    fun fromStorageCategory(category: StorageCategory?): String {
        return category?.name ?: StorageCategory.OTHERS.name
    }

    @TypeConverter
    fun toStorageCategory(value: String?): StorageCategory {
        if (value == null) return StorageCategory.OTHERS
        return try {
            StorageCategory.valueOf(value)
        } catch (_: IllegalArgumentException) {
            StorageCategory.OTHERS
        }
    }

    @TypeConverter
    fun fromRecommendationType(type: RecommendationType?): String {
        return type?.name ?: RecommendationType.LARGE_FILE.name
    }

    @TypeConverter
    fun toRecommendationType(value: String?): RecommendationType {
        if (value == null) return RecommendationType.LARGE_FILE
        return try {
            RecommendationType.valueOf(value)
        } catch (_: IllegalArgumentException) {
            RecommendationType.LARGE_FILE
        }
    }

    @TypeConverter
    fun fromRecommendationPriority(priority: RecommendationPriority?): String {
        return priority?.name ?: RecommendationPriority.MEDIUM.name
    }

    @TypeConverter
    fun toRecommendationPriority(value: String?): RecommendationPriority {
        if (value == null) return RecommendationPriority.MEDIUM
        return try {
            RecommendationPriority.valueOf(value)
        } catch (_: IllegalArgumentException) {
            RecommendationPriority.MEDIUM
        }
    }
}
