package com.lightstep.pedometer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.lightstep.pedometer.MainActivity
import com.lightstep.pedometer.R
import com.lightstep.pedometer.data.StepDatabase
import com.lightstep.pedometer.data.StepRepository
import com.lightstep.pedometer.domain.SystemBoot
import com.lightstep.pedometer.widget.StepWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WidgetSyncService : Service(), SensorEventListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    private var stepCounterSensor: Sensor? = null
    private var lastWidgetUpdateAt: Long = 0
    private var lastWidgetUpdateSteps: Long = -1

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        repository = StepRepository(StepDatabase.get(this))
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP || !canRun(this) || !StepWidgetUpdater.hasAnyWidget(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground()
        stepCounterSensor?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL, 0)
        } ?: stopSelf()
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        val totalSteps = event.values.firstOrNull()?.toLong() ?: return
        serviceScope.launch {
            val daily = repository.syncSensorTotal(totalSteps, SystemBoot.currentBootId())
            val now = SystemClock.elapsedRealtime()
            val shouldUpdateWidget = lastWidgetUpdateAt == 0L ||
                now - lastWidgetUpdateAt >= WIDGET_UPDATE_MIN_INTERVAL_MS ||
                daily.dailySteps - lastWidgetUpdateSteps >= WIDGET_UPDATE_STEP_DELTA
            if (shouldUpdateWidget) {
                StepWidgetUpdater.updateAllNow(applicationContext)
                lastWidgetUpdateAt = now
                lastWidgetUpdateSteps = daily.dailySteps
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            21,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            22,
            Intent(this, WidgetSyncService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("轻步小组件同步")
            .setContentText("普通模式下保持桌面步数更新")
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "停止", stopIntent)
            .build()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "小组件同步",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "light_step_widget_sync"
        private const val NOTIFICATION_ID = 1002
        private const val ACTION_STOP = "com.lightstep.pedometer.action.STOP_WIDGET_SYNC"
        private const val WIDGET_UPDATE_MIN_INTERVAL_MS = 5000L
        private const val WIDGET_UPDATE_STEP_DELTA = 5L

        fun startIfNeeded(context: Context) {
            val appContext = context.applicationContext
            if (!canRun(appContext) || !StepWidgetUpdater.hasAnyWidget(appContext)) return
            val intent = Intent(appContext, WidgetSyncService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(intent)
            } else {
                appContext.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.applicationContext.stopService(Intent(context.applicationContext, WidgetSyncService::class.java))
        }

        private fun canRun(context: Context): Boolean {
            val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            val hasSensor = (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager)
                .getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
            return hasPermission && hasSensor
        }
    }
}
