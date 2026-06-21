package com.lightstep.pedometer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.lightstep.pedometer.MainActivity
import com.lightstep.pedometer.R
import com.lightstep.pedometer.data.DailyStepsEntity
import com.lightstep.pedometer.data.StepDatabase
import com.lightstep.pedometer.data.StepRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

object StepWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateAll(context: Context) {
        scope.launch {
            updateAllNow(context.applicationContext)
        }
    }

    suspend fun updateAllNow(context: Context) {
        val appContext = context.applicationContext
        val repository = StepRepository(StepDatabase.get(appContext))
        val today = repository.latestTodayForWidget()
        val week = repository.latestSevenDaysForWidget()
        val manager = AppWidgetManager.getInstance(appContext)
        val snapshot = WidgetSnapshot(today, buildWeekItems(week, today.goalSteps))

        updateCompact(appContext, manager, StepCompactWidgetProvider::class.java, snapshot)
        updateProgress(appContext, manager, StepProgressWidgetProvider::class.java, snapshot)
        updateStepSquare(appContext, manager, StepSquareWidgetProvider::class.java, snapshot)
        updateStepWide(appContext, manager, StepWideWidgetProvider::class.java, snapshot)
        updateStepWeek(appContext, manager, StepWeekWidgetProvider::class.java, snapshot)
        updateActivity(appContext, manager, ActivityCompactWidgetProvider::class.java, R.layout.widget_activity_compact, snapshot)
        updateActivity(appContext, manager, ActivitySquareWidgetProvider::class.java, R.layout.widget_activity_square, snapshot)
        updateActivity(appContext, manager, ActivityWideWidgetProvider::class.java, R.layout.widget_activity_wide, snapshot)
        updateActivityWeek(appContext, manager, ActivityWeekWidgetProvider::class.java, snapshot)
    }

    fun hasAnyWidget(context: Context): Boolean {
        val appContext = context.applicationContext
        val manager = AppWidgetManager.getInstance(appContext)
        return widgetProviderClasses.any { provider ->
            manager.getAppWidgetIds(ComponentName(appContext, provider)).isNotEmpty()
        }
    }

    private fun updateCompact(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_steps_compact)
            views.setTextViewText(R.id.widgetSteps, numberFormatter.format(snapshot.steps))
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            views.setOnClickPendingIntent(R.id.widgetSteps, refreshIntent(context))
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateProgress(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_steps_progress)
            fillStepProgress(views, snapshot, compactGoal = false, showGoal = false, showPercent = true)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            views.setOnClickPendingIntent(R.id.widgetTitle, refreshIntent(context))
            setStepRefreshTargets(views, context, hasGoal = false)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateStepSquare(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_steps_square)
            fillStepProgress(views, snapshot, compactGoal = true, showGoal = true, showPercent = false)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            setStepRefreshTargets(views, context, hasGoal = true)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateStepWide(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_steps_wide)
            fillStepProgress(views, snapshot, compactGoal = false, showGoal = true, showPercent = false)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            setStepRefreshTargets(views, context, hasGoal = true)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateStepWeek(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_steps_week)
            fillStepProgress(views, snapshot, compactGoal = true, showGoal = true, showPercent = false)
            fillWeekLabels(views, snapshot.week, stepWeekIds)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            setStepRefreshTargets(views, context, hasGoal = true)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateActivity(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        layoutId: Int,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, layoutId)
            fillActivityMetrics(views, snapshot)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            setActivityRefreshTargets(views, context)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun updateActivityWeek(
        context: Context,
        manager: AppWidgetManager,
        provider: Class<*>,
        snapshot: WidgetSnapshot
    ) {
        widgetIds(context, manager, provider).forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_activity_week)
            fillActivityMetrics(views, snapshot)
            fillWeekLabels(views, snapshot.week, activityWeekIds)
            views.setOnClickPendingIntent(R.id.widgetRoot, openAppIntent(context))
            setActivityRefreshTargets(views, context)
            manager.updateAppWidget(widgetId, views)
        }
    }

    private fun fillStepProgress(
        views: RemoteViews,
        snapshot: WidgetSnapshot,
        compactGoal: Boolean,
        showGoal: Boolean,
        showPercent: Boolean
    ) {
        val percent = if (snapshot.goalSteps > 0) ((snapshot.steps * 100.0) / snapshot.goalSteps).roundToInt().coerceIn(0, 999) else 0
        views.setTextViewText(R.id.widgetSteps, numberFormatter.format(snapshot.steps))
        if (showGoal) {
            views.setTextViewText(
                R.id.widgetGoal,
                if (compactGoal) "/${numberFormatter.format(snapshot.goalSteps)}" else "目标 ${numberFormatter.format(snapshot.goalSteps)}"
            )
        }
        views.setTextViewText(R.id.widgetDistance, snapshot.distanceText)
        views.setProgressBar(R.id.widgetProgress, 100, percent.coerceAtMost(100), false)
        if (showPercent) {
            views.setTextViewText(R.id.widgetPercent, "$percent%")
        }
    }

    private fun fillActivityMetrics(views: RemoteViews, snapshot: WidgetSnapshot) {
        views.setTextViewText(R.id.widgetActivitySteps, "步  ${numberFormatter.format(snapshot.steps)}/${numberFormatter.format(snapshot.goalSteps)}")
        views.setTextViewText(R.id.widgetActivityDistance, "距  ${snapshot.distanceText}")
        views.setTextViewText(R.id.widgetActivityMinutes, "时  ${snapshot.activeMinutes}/90 分钟")
        views.setTextViewText(R.id.widgetActivityCalories, "卡  ${snapshot.calories}/500 千卡")
    }

    private fun setStepRefreshTargets(views: RemoteViews, context: Context, hasGoal: Boolean) {
        val refresh = refreshIntent(context)
        views.setOnClickPendingIntent(R.id.widgetSteps, refresh)
        if (hasGoal) views.setOnClickPendingIntent(R.id.widgetGoal, refresh)
        views.setOnClickPendingIntent(R.id.widgetDistance, refresh)
        views.setOnClickPendingIntent(R.id.widgetProgress, refresh)
    }

    private fun setActivityRefreshTargets(views: RemoteViews, context: Context) {
        val refresh = refreshIntent(context)
        views.setOnClickPendingIntent(R.id.widgetActivitySteps, refresh)
        views.setOnClickPendingIntent(R.id.widgetActivityDistance, refresh)
        views.setOnClickPendingIntent(R.id.widgetActivityMinutes, refresh)
        views.setOnClickPendingIntent(R.id.widgetActivityCalories, refresh)
    }

    private fun fillWeekLabels(views: RemoteViews, week: List<WidgetDay>, ids: IntArray) {
        week.take(ids.size).forEachIndexed { index, day ->
            val marker = if (day.steps >= day.goal && day.goal > 0) "●" else "·"
            views.setTextViewText(ids[index], "${day.label}\n$marker")
            views.setTextColor(ids[index], if (index == ids.lastIndex) orange else muted)
        }
    }

    private fun widgetIds(context: Context, manager: AppWidgetManager, provider: Class<*>): IntArray =
        manager.getAppWidgetIds(ComponentName(context, provider))

    private fun openAppIntent(context: Context): PendingIntent {
        return PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun refreshIntent(context: Context): PendingIntent {
        val intent = Intent(context, StepProgressWidgetProvider::class.java)
            .setAction(ACTION_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            11,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildWeekItems(records: List<DailyStepsEntity>, defaultGoal: Int): List<WidgetDay> {
        val byDate = records.associateBy { it.date }
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFormat = SimpleDateFormat("d", Locale.getDefault())
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        return (0 until 7).map {
            val date = dateFormat.format(calendar.time)
            val record = byDate[date]
            val item = WidgetDay(
                label = labelFormat.format(calendar.time),
                steps = record?.dailySteps ?: 0,
                goal = record?.goalSteps ?: defaultGoal
            )
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            item
        }
    }

    const val ACTION_REFRESH = "com.lightstep.pedometer.widget.REFRESH"

    private val numberFormatter: NumberFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
    private val muted = Color.rgb(111, 118, 114)
    private val orange = Color.rgb(245, 164, 11)
    private val stepWeekIds = intArrayOf(
        R.id.widgetDay1,
        R.id.widgetDay2,
        R.id.widgetDay3,
        R.id.widgetDay4,
        R.id.widgetDay5,
        R.id.widgetDay6,
        R.id.widgetDay7
    )
    private val activityWeekIds = intArrayOf(
        R.id.widgetActivityDay1,
        R.id.widgetActivityDay2,
        R.id.widgetActivityDay3,
        R.id.widgetActivityDay4,
        R.id.widgetActivityDay5,
        R.id.widgetActivityDay6,
        R.id.widgetActivityDay7
    )
    private val widgetProviderClasses = listOf(
        StepCompactWidgetProvider::class.java,
        StepProgressWidgetProvider::class.java,
        StepSquareWidgetProvider::class.java,
        StepWideWidgetProvider::class.java,
        StepWeekWidgetProvider::class.java,
        ActivityCompactWidgetProvider::class.java,
        ActivitySquareWidgetProvider::class.java,
        ActivityWideWidgetProvider::class.java,
        ActivityWeekWidgetProvider::class.java
    )
}

private data class WidgetSnapshot(
    val today: DailyStepsEntity,
    val week: List<WidgetDay>
) {
    val steps: Long = today.dailySteps
    val goalSteps: Int = today.goalSteps
    val activeMinutes: Int = today.activeMinutes
    val calories: Int = today.calories.roundToInt()
    val distanceText: String = String.format(Locale.getDefault(), "%.2f km", today.distanceMeters / 1000.0)
}

private data class WidgetDay(
    val label: String,
    val steps: Long,
    val goal: Int
)
