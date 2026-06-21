package com.lightstep.pedometer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lightstep.pedometer.data.StepDatabase
import com.lightstep.pedometer.data.StepRepository
import com.lightstep.pedometer.domain.SystemBoot
import com.lightstep.pedometer.sensor.StepCounterReader
import com.lightstep.pedometer.sensor.StepReadResult
import com.lightstep.pedometer.widget.StepWidgetUpdater
import com.lightstep.pedometer.worker.StepSyncWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WakeRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_USER_PRESENT && intent.action != Intent.ACTION_SCREEN_ON) return

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        StepSyncWorker.enqueueExpedited(appContext)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = StepCounterReader(appContext)
                val repository = StepRepository(StepDatabase.get(appContext))
                when (val result = reader.readCurrentSteps(timeoutMillis = 8000)) {
                    is StepReadResult.Success -> {
                        repository.syncSensorTotal(result.totalSteps, SystemBoot.currentBootId())
                    }

                    StepReadResult.NoSensor -> {
                        repository.setDeviceStatus(sensorAvailable = false, permissionGranted = true)
                    }

                    StepReadResult.PermissionDenied -> {
                        repository.setDeviceStatus(sensorAvailable = reader.hasStepSensor(), permissionGranted = false)
                    }

                    StepReadResult.Timeout,
                    is StepReadResult.Error -> Unit
                }
                StepWidgetUpdater.updateAllNow(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
