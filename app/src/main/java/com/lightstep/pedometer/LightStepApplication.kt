package com.lightstep.pedometer

import android.app.Application
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.lightstep.pedometer.service.WidgetSyncService
import com.lightstep.pedometer.service.WakeRefreshReceiver
import com.lightstep.pedometer.worker.StepSyncWorker

class LightStepApplication : Application() {
    private val wakeRefreshReceiver = WakeRefreshReceiver()

    override fun onCreate() {
        super.onCreate()
        registerWakeRefreshReceiver()
        StepSyncWorker.enqueuePeriodic(this)
        WidgetSyncService.startIfNeeded(this)
    }

    private fun registerWakeRefreshReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            wakeRefreshReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }
}
