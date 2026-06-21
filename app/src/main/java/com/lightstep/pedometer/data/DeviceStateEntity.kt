package com.lightstep.pedometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_state")
data class DeviceStateEntity(
    @PrimaryKey val id: Int = 0,
    val bootId: String,
    val lastSensorValue: Long,
    val lastSyncTime: Long,
    val sensorAvailable: Boolean,
    val permissionGranted: Boolean
)
