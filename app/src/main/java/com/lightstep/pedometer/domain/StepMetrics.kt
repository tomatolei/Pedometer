package com.lightstep.pedometer.domain

import kotlin.math.roundToInt

object StepMetrics {
    fun distanceMeters(steps: Long, strideLengthCm: Int): Double =
        steps.coerceAtLeast(0) * strideLengthCm.coerceAtLeast(30) / 100.0

    fun calories(distanceMeters: Double, weightKg: Int): Double =
        (distanceMeters / 1000.0) * weightKg.coerceAtLeast(30) * 0.815

    fun activeMinutes(steps: Long): Int =
        (steps.coerceAtLeast(0) / 100.0).roundToInt()
}
