package com.lightstep.pedometer.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lightstep.pedometer.worker.StepSyncWorker

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            StepSyncWorker.enqueuePeriodic(context)
            StepSyncWorker.enqueueOneTime(context)
        }
    }
}
