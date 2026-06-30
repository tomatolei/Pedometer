package com.lightstep.pedometer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val displayName: String = "轻步用户",
    val avatarUri: String? = null,
    val dailyGoalSteps: Int = 10000,
    val birthDate: String = "1991-01-01",
    val heightCm: Int = 170,
    val weightKg: Int = 65,
    val age: Int = 35,
    val gender: String = GENDER_UNSPECIFIED,
    val strideLengthCm: Int = 83,
    val strideMode: String = STRIDE_MODE_AUTO,
    val themeMode: String = "system",
    val realtimeModeEnabled: Boolean = false
) {
    companion object {
        const val GENDER_UNSPECIFIED = "unspecified"
        const val GENDER_MALE = "male"
        const val GENDER_FEMALE = "female"
        const val STRIDE_MODE_AUTO = "auto"
        const val STRIDE_MODE_MANUAL = "manual"
    }
}
