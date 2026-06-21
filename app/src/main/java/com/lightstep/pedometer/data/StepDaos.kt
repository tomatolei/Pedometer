package com.lightstep.pedometer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Query("SELECT * FROM daily_steps WHERE date = :date")
    fun observeByDate(date: String): Flow<DailyStepsEntity?>

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun observeRange(startDate: String, endDate: String): Flow<List<DailyStepsEntity>>

    @Query("SELECT * FROM daily_steps WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    suspend fun getRange(startDate: String, endDate: String): List<DailyStepsEntity>

    @Query("SELECT * FROM daily_steps WHERE date = :date")
    suspend fun getByDate(date: String): DailyStepsEntity?

    @Query("SELECT * FROM daily_steps ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): DailyStepsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyStepsEntity)
}

@Dao
interface SensorSampleDao {
    @Query("SELECT * FROM sensor_samples WHERE date = :date ORDER BY timestamp ASC")
    fun observeByDate(date: String): Flow<List<SensorSampleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SensorSampleEntity)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 0")
    fun observe(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 0")
    suspend fun get(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UserSettingsEntity)
}

@Dao
interface DeviceStateDao {
    @Query("SELECT * FROM device_state WHERE id = 0")
    suspend fun get(): DeviceStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DeviceStateEntity)
}
