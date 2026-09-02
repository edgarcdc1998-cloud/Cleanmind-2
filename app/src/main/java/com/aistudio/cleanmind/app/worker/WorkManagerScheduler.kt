package com.aistudio.cleanmind.app.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    const val MANUAL_WORK_NAME = "cleanmind_manual_storage_analysis"
    const val PERIODIC_WORK_NAME = "cleanmind_periodic_storage_analysis"

    const val TAG_MANUAL_ANALYSIS = "tag_cleanmind_manual_analysis"
    const val TAG_PERIODIC_ANALYSIS = "tag_cleanmind_periodic_analysis"

    /**
     * Dispara uma análise manual em segundo plano.
     * Política: [ExistingWorkPolicy.KEEP] evita que o usuário ou o sistema crie múltiplas análises
     * simultâneas caso uma já esteja na fila (ENQUEUED) ou em execução (RUNNING).
     */
    fun scheduleManualAnalysis(context: Context): Operation {
        val constraints = Constraints.Builder()
            .setRequiresStorageNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<StorageAnalysisWorker>()
            .setConstraints(constraints)
            .addTag(TAG_MANUAL_ANALYSIS)
            .build()

        return WorkManager.getInstance(context).enqueueUniqueWork(
            MANUAL_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Cancela a análise manual em andamento.
     */
    fun cancelManualAnalysis(context: Context): Operation {
        return WorkManager.getInstance(context).cancelUniqueWork(MANUAL_WORK_NAME)
    }

    /**
     * Agenda a análise periódica em segundo plano.
     * Restrições aplicadas:
     * - Bateria não baixa ([Constraints.Builder.setRequiresBatteryNotLow]): evita drenar a bateria do usuário.
     * - Armazenamento não baixo ([Constraints.Builder.setRequiresStorageNotLow]): garante integridade do sistema.
     * - 100% Local / Offline: nenhuma restrição de rede necessária.
     *
     * Política: [ExistingPeriodicWorkPolicy.UPDATE] atualiza os parâmetros/intervalo preservando a periodicidade.
     */
    fun schedulePeriodicAnalysis(
        context: Context,
        intervalHours: Long = 24
    ): Operation {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresStorageNotLow(true)
            .build()

        val safeInterval = intervalHours.coerceAtLeast(1) // WorkManager mínimo é 15 min, mas usamos horas
        val request = PeriodicWorkRequestBuilder<StorageAnalysisWorker>(
            safeInterval,
            TimeUnit.HOURS,
            1, // flexInterval: 1 hora de flex window
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(TAG_PERIODIC_ANALYSIS)
            .build()

        return WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Cancela o agendamento da análise periódica.
     */
    fun cancelPeriodicAnalysis(context: Context): Operation {
        return WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
    }

    /**
     * Observa o estado da análise manual via Flow do WorkManager.
     */
    fun getManualAnalysisWorkInfoFlow(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(MANUAL_WORK_NAME)
    }

    /**
     * Observa o estado da análise periódica via Flow do WorkManager.
     */
    fun getPeriodicAnalysisWorkInfoFlow(context: Context): Flow<List<WorkInfo>> {
        return WorkManager.getInstance(context).getWorkInfosForUniqueWorkFlow(PERIODIC_WORK_NAME)
    }
}
