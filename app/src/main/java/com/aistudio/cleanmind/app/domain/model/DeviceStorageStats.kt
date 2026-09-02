package com.aistudio.cleanmind.app.domain.model

data class DeviceStorageStats(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long
) {
    val usedPercentage: Float
        get() = if (totalBytes > 0) {
            (usedBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
}
