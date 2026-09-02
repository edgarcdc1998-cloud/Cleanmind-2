package com.aistudio.cleanmind.app.data.local.database

import androidx.room.TypeConverter
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
}
