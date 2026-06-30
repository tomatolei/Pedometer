package com.lightstep.pedometer.data

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyStepsEntity::class,
        SensorSampleEntity::class,
        UserSettingsEntity::class,
        DeviceStateEntity::class
    ],
    version = 5,
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN age INTEGER NOT NULL DEFAULT 35")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN gender TEXT NOT NULL DEFAULT 'unspecified'")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN strideMode TEXT NOT NULL DEFAULT 'auto'")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN displayName TEXT NOT NULL DEFAULT '轻步用户'")
                db.execSQL("ALTER TABLE user_settings ADD COLUMN avatarUri TEXT")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN birthDate TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
