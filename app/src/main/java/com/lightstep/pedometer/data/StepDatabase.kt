package com.lightstep.pedometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        DailyStepsEntity::class,
        SensorSampleEntity::class,
        UserSettingsEntity::class,
        DeviceStateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class StepDatabase : RoomDatabase() {
    abstract fun dailyStepsDao(): DailyStepsDao
    abstract fun sensorSampleDao(): SensorSampleDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun deviceStateDao(): DeviceStateDao

    companion object {
        @Volatile
        private var instance: StepDatabase? = null

        fun get(context: Context): StepDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    StepDatabase::class.java,
                    "light_step.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}
