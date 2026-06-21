package com.lightstep.pedometer.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import com.lightstep.pedometer.service.WidgetSyncService

class StepCompactWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        WidgetSyncService.startIfNeeded(context)
        StepWidgetUpdater.updateAll(context)
    }
}
