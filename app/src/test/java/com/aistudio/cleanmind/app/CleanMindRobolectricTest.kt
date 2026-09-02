package com.aistudio.cleanmind.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CleanMindRobolectricTest {

    @Test
    fun readAppNameStringResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("CleanMind", appName)
    }

    @Test
    fun readStorageStatusNotAnalyzedResource() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val status = context.getString(R.string.storage_status_not_analyzed)
        assertEquals("Ainda não analisado", status)
    }
}
