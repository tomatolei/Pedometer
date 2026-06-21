package com.lightstep.pedometer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
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
import kotlinx.coroutines.runBlocking

class RealtimeStepService : Service(), SensorEventListener {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var sensorManager: SensorManager
    private lateinit var repository: StepRepository
    private var stepCounterSensor: Sensor? = null
    private var lastNotificationSteps: Long = -1
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
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        startInForeground(0)
        stepCounterSensor?.let(::registerStepSensor)
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) handleCounterEvent(event)
    }

    private fun registerStepSensor(sensor: Sensor) {
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0)
    }

    private fun handleCounterEvent(event: SensorEvent) {
        val totalSteps = event.values.firstOrNull()?.toLong() ?: return
        serviceScope.launch {
            val daily = repository.syncSensorTotal(totalSteps, SystemBoot.currentBootId())
            publishSteps(daily.dailySteps)
        }
    }

    private suspend fun publishSteps(steps: Long) {
        val now = SystemClock.elapsedRealtime()
        val shouldUpdateWidget = lastWidgetUpdateAt == 0L ||
            now - lastWidgetUpdateAt >= WIDGET_UPDATE_MIN_INTERVAL_MS ||
            steps - lastWidgetUpdateSteps >= WIDGET_UPDATE_STEP_DELTA
        if (shouldUpdateWidget) {
            StepWidgetUpdater.updateAllNow(applicationContext)
            lastWidgetUpdateAt = now
            lastWidgetUpdateSteps = steps
        }
        if (steps != lastNotificationSteps) {
            lastNotificationSteps = steps
            startInForeground(steps)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        runBlocking(Dispatchers.IO) {
            repository.setRealtimeEnabled(false)
            StepWidgetUpdater.updateAllNow(applicationContext)
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground(steps: Long) {
        val notification = buildNotification(steps)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(steps: Long): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, RealtimeStepService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = if (steps > 0) "今日 $steps 步" else "正在监听手机计步传感器"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("轻步实时计步")
            .setContentText(content)
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
                "实时计步",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "light_step_realtime"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP = "com.lightstep.pedometer.action.STOP_REALTIME"
        private const val WIDGET_UPDATE_MIN_INTERVAL_MS = 2000L
        private const val WIDGET_UPDATE_STEP_DELTA = 5L

        fun start(context: Context) {
            val intent = Intent(context, RealtimeStepService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, RealtimeStepService::class.java))
        }
    }
}
