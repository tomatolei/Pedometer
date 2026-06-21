package com.lightstep.pedometer.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class StepCounterReader(private val context: Context) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepSensor: Sensor?
        get() = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    fun hasPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasStepSensor(): Boolean = stepSensor != null

    suspend fun readCurrentSteps(timeoutMillis: Long = 3500): StepReadResult {
        if (!hasPermission()) return StepReadResult.PermissionDenied
        val sensor = stepSensor ?: return StepReadResult.NoSensor

        val value = withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine<Long> { continuation ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val total = event.values.firstOrNull()?.toLong()
                        if (total != null && continuation.isActive) {
                            sensorManager.unregisterListener(this)
                            continuation.resume(total)
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }

                val registered = sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_UI,
                    0
                )
                if (!registered && continuation.isActive) {
                    continuation.resume(-1)
                }

                continuation.invokeOnCancellation {
                    sensorManager.unregisterListener(listener)
                }
            }
        } ?: return StepReadResult.Timeout

        return if (value >= 0) StepReadResult.Success(value) else StepReadResult.Error("register_failed")
    }
}

sealed class StepReadResult {
    data class Success(val totalSteps: Long) : StepReadResult()
    object PermissionDenied : StepReadResult()
    object NoSensor : StepReadResult()
    object Timeout : StepReadResult()
    data class Error(val reason: String) : StepReadResult()
}
