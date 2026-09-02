package com.aistudio.cleanmind.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WorkManagerSchedulerTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
    }

    @Test
    fun scheduleManualAnalysis_enqueuesWork() = runTest {
        val operation = WorkManagerScheduler.scheduleManualAnalysis(context)
        assertNotNull(operation)

        val workInfoList = WorkManagerScheduler.getManualAnalysisWorkInfoFlow(context).first()
        assertTrue(workInfoList.isNotEmpty())
        val workInfo = workInfoList.first()
        assertTrue(
            workInfo.state == WorkInfo.State.ENQUEUED ||
            workInfo.state == WorkInfo.State.RUNNING ||
            workInfo.state == WorkInfo.State.SUCCEEDED
        )
    }

    @Test
    fun cancelManualAnalysis_cancelsExistingWork() = runTest {
        WorkManagerScheduler.scheduleManualAnalysis(context)
        WorkManagerScheduler.cancelManualAnalysis(context)

        val workManager = WorkManager.getInstance(context)
        val statuses = workManager.getWorkInfosForUniqueWork(WorkManagerScheduler.MANUAL_WORK_NAME).get()
        assertTrue(statuses.isNotEmpty())
        val state = statuses.first().state
        assertTrue(state == WorkInfo.State.CANCELLED || state == WorkInfo.State.SUCCEEDED)
    }

    @Test
    fun schedulePeriodicAnalysis_enqueuesPeriodicWork() = runTest {
        WorkManagerScheduler.schedulePeriodicAnalysis(context = context, intervalHours = 24)

        val periodicWorkList = WorkManagerScheduler.getPeriodicAnalysisWorkInfoFlow(context).first()
        assertTrue(periodicWorkList.isNotEmpty())
    }

    @Test
    fun cancelPeriodicAnalysis_cancelsPeriodicWork() = runTest {
        WorkManagerScheduler.schedulePeriodicAnalysis(context = context, intervalHours = 24)
        WorkManagerScheduler.cancelPeriodicAnalysis(context)

        val workManager = WorkManager.getInstance(context)
        val statuses = workManager.getWorkInfosForUniqueWork(WorkManagerScheduler.PERIODIC_WORK_NAME).get()
        assertTrue(statuses.isNotEmpty())
        val state = statuses.first().state
        assertTrue(state == WorkInfo.State.CANCELLED)
    }
}
