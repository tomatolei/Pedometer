package com.lightstep.pedometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_samples")
data class SensorSampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val sensorTotalSteps: Long,
    val deltaSteps: Long,
    val date: String,
    val source: String,
    val bootId: String,
    val note: String
)
