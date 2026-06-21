package com.lightstep.pedometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailyStepsEntity(
    @PrimaryKey val date: String,
    val baselineSensorSteps: Long,
    val lastSensorSteps: Long,
    val dailySteps: Long,
    val compensatedSteps: Long,
    val distanceMeters: Double,
    val calories: Double,
    val activeMinutes: Int,
    val goalSteps: Int,
    val isEstimated: Boolean,
    val pendingDetectorSteps: Long,
    val dataSource: String,
    val updatedAt: Long
)
