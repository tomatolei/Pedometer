package com.lightstep.pedometer.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.lightstep.pedometer.data.StepDatabase
import com.lightstep.pedometer.data.StepRepository
import com.lightstep.pedometer.domain.SystemBoot
import com.lightstep.pedometer.sensor.StepCounterReader
import com.lightstep.pedometer.sensor.StepReadResult
import com.lightstep.pedometer.widget.StepWidgetUpdater
import java.util.concurrent.TimeUnit

class StepSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val reader = StepCounterReader(applicationContext)
        val repository = StepRepository(StepDatabase.get(applicationContext))

        return when (val result = reader.readCurrentSteps(timeoutMillis = 8000)) {
            is StepReadResult.Success -> {
                repository.syncSensorTotal(result.totalSteps, SystemBoot.currentBootId())
                StepWidgetUpdater.updateAllNow(applicationContext)
                Result.success()
            }

            StepReadResult.NoSensor -> {
                repository.setDeviceStatus(sensorAvailable = false, permissionGranted = true)
                StepWidgetUpdater.updateAllNow(applicationContext)
                Result.success()
            }

            StepReadResult.PermissionDenied -> {
                repository.setDeviceStatus(sensorAvailable = reader.hasStepSensor(), permissionGranted = false)
                StepWidgetUpdater.updateAllNow(applicationContext)
                Result.success()
            }

            StepReadResult.Timeout -> Result.retry()
            is StepReadResult.Error -> Result.retry()
        }
    }

    companion object {
        private const val PERIODIC_WORK_NAME = "light_step_periodic_sync"
        private const val ONE_TIME_WORK_NAME = "light_step_refresh_once"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<StepSyncWorker>(15, TimeUnit.MINUTES)
                .addTag(PERIODIC_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueOneTime(context: Context) {
            val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
                .addTag(ONE_TIME_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueueExpedited(context: Context) {
            val request = OneTimeWorkRequestBuilder<StepSyncWorker>()
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .addTag(ONE_TIME_WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
