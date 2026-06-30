package com.lightstep.pedometer.domain

import com.lightstep.pedometer.data.UserSettingsEntity
import kotlin.math.roundToInt

object StepMetrics {
    fun effectiveStrideCm(settings: UserSettingsEntity): Int {
        return if (settings.strideMode == UserSettingsEntity.STRIDE_MODE_MANUAL) {
            settings.strideLengthCm.coerceIn(30, 150)
        } else {
            estimatedStrideCm(settings.heightCm, settings.gender, settings.age)
        }
    }

    fun estimatedStrideCm(heightCm: Int, gender: String, age: Int): Int {
        val baseRatio = when (gender) {
            UserSettingsEntity.GENDER_MALE -> 0.49
            UserSettingsEntity.GENDER_FEMALE -> 0.47
            else -> 0.49
        }
        val ageFactor = when {
            age >= 70 -> 0.94
            age >= 60 -> 0.96
            age <= 16 -> 0.96
            else -> 1.0
        }
        return (heightCm.coerceIn(120, 220) * baseRatio * ageFactor).roundToInt().coerceIn(45, 120)
    }

    fun distanceMeters(steps: Long, strideLengthCm: Int): Double =
        steps.coerceAtLeast(0) * strideLengthCm.coerceAtLeast(30) / 100.0

    fun calories(distanceMeters: Double, weightKg: Int, gender: String, age: Int): Double {
        val genderFactor = when (gender) {
            UserSettingsEntity.GENDER_FEMALE -> 0.96
            UserSettingsEntity.GENDER_MALE -> 1.02
            else -> 1.0
        }
        val ageFactor = when {
            age >= 60 -> 0.96
            age <= 16 -> 0.94
            else -> 1.0
        }
        return (distanceMeters / 1000.0) * weightKg.coerceAtLeast(30) * 0.815 * genderFactor * ageFactor
    }

    fun activeMinutes(steps: Long): Int =
        (steps.coerceAtLeast(0) / 100.0).roundToInt()
}
