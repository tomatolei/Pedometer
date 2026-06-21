package com.lightstep.pedometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val dailyGoalSteps: Int = 10000,
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val strideLengthCm: Int = 83,
    val themeMode: String = "system",
    val realtimeModeEnabled: Boolean = false
)
