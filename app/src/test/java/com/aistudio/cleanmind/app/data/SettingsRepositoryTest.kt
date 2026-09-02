package com.aistudio.cleanmind.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.cleanmind.app.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("cleanmind_settings", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = SettingsRepositoryImpl(context)
    }

    @Test
    fun defaultSettings_hasAutoAnalysisDisabledAnd24Hours() = runTest {
        assertFalse(repository.isAutoAnalysisEnabled().first())
        assertEquals(24, repository.getAutoAnalysisIntervalHours().first())
    }

    @Test
    fun setAutoAnalysisEnabled_updatesFlow() = runTest {
        repository.setAutoAnalysisEnabled(true)
        assertTrue(repository.isAutoAnalysisEnabled().first())

        repository.setAutoAnalysisEnabled(false)
        assertFalse(repository.isAutoAnalysisEnabled().first())
    }

    @Test
    fun setAutoAnalysisIntervalHours_updatesFlow() = runTest {
        repository.setAutoAnalysisIntervalHours(168)
        assertEquals(168, repository.getAutoAnalysisIntervalHours().first())

        repository.setAutoAnalysisIntervalHours(48)
        assertEquals(48, repository.getAutoAnalysisIntervalHours().first())
    }

    @Test
    fun setAutoAnalysisIntervalHours_enforcesMinimum24Hours() = runTest {
        repository.setAutoAnalysisIntervalHours(0)
        assertEquals(24, repository.getAutoAnalysisIntervalHours().first())
    }
}
