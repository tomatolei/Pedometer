package com.lightstep.pedometer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import com.lightstep.pedometer.service.WidgetSyncService
import com.lightstep.pedometer.worker.StepSyncWorker

class StepProgressWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetSyncService.startIfNeeded(context)
        StepWidgetUpdater.updateAll(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == StepWidgetUpdater.ACTION_REFRESH) {
            StepSyncWorker.enqueueOneTime(context)
            StepWidgetUpdater.updateAll(context)
        }
    }
}
