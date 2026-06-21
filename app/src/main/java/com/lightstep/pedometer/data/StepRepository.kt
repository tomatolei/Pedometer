package com.lightstep.pedometer.data

import androidx.room.withTransaction
import com.lightstep.pedometer.domain.StepMetrics
import com.lightstep.pedometer.domain.daysAgoString
import com.lightstep.pedometer.domain.todayString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class StepRepository(private val database: StepDatabase) {
    private val dailyDao = database.dailyStepsDao()
    private val sampleDao = database.sensorSampleDao()
    private val settingsDao = database.userSettingsDao()
    private val deviceStateDao = database.deviceStateDao()

    fun observeToday(): Flow<DailyStepsEntity?> = dailyDao.observeByDate(todayString())

    fun observeSevenDays(): Flow<List<DailyStepsEntity>> =
        dailyDao.observeRange(daysAgoString(6), todayString())

    fun observeThirtyDays(): Flow<List<DailyStepsEntity>> =
        dailyDao.observeRange(daysAgoString(29), todayString())

    fun observeTodaySamples(): Flow<List<SensorSampleEntity>> =
        sampleDao.observeByDate(todayString())

    fun observeSettings(): Flow<UserSettingsEntity> =
        settingsDao.observe().map { it ?: UserSettingsEntity() }

    suspend fun ensureDefaults(): UserSettingsEntity {
        val existing = settingsDao.get()
        if (existing != null) {
            val normalized = if (existing.heightCm == 170 && existing.weightKg == 65 && existing.strideLengthCm == 70) {
                existing.copy(strideLengthCm = UserSettingsEntity().strideLengthCm)
            } else {
                existing
            }
            if (normalized != existing) settingsDao.upsert(normalized)
            return normalized
        }
        return UserSettingsEntity().also { settingsDao.upsert(it) }
    }

    suspend fun updateGoal(goalSteps: Int) {
        val settings = ensureDefaults()
        settingsDao.upsert(settings.copy(dailyGoalSteps = goalSteps.coerceIn(1000, 50000)))
        dailyDao.getByDate(todayString())?.let { today ->
            dailyDao.upsert(today.copy(goalSteps = goalSteps.coerceIn(1000, 50000)))
        }
    }

    suspend fun updateTheme(themeMode: String) {
        val settings = ensureDefaults()
        settingsDao.upsert(settings.copy(themeMode = themeMode))
    }

    suspend fun setRealtimeEnabled(enabled: Boolean) {
        val settings = ensureDefaults()
        settingsDao.upsert(settings.copy(realtimeModeEnabled = enabled))
    }

    suspend fun setDeviceStatus(sensorAvailable: Boolean, permissionGranted: Boolean) {
        val now = System.currentTimeMillis()
        val state = deviceStateDao.get()
        deviceStateDao.upsert(
            DeviceStateEntity(
                bootId = state?.bootId.orEmpty(),
                lastSensorValue = state?.lastSensorValue ?: 0,
                lastSyncTime = now,
                sensorAvailable = sensorAvailable,
                permissionGranted = permissionGranted
            )
        )
    }

    suspend fun latestTodayForWidget(): DailyStepsEntity {
        val settings = ensureDefaults()
        val today = todayString()
        return dailyDao.getByDate(today) ?: emptyDaily(today, 0, settings)
    }

    suspend fun latestSevenDaysForWidget(): List<DailyStepsEntity> =
        dailyDao.getRange(daysAgoString(6), todayString())

    suspend fun syncSensorTotal(
        sensorTotalSteps: Long,
        bootId: String,
        timestampMillis: Long = System.currentTimeMillis(),
        source: String = DATA_SOURCE_PHONE
    ): DailyStepsEntity = database.withTransaction {
        val settings = ensureDefaults()
        val today = todayString()
        val latest = dailyDao.getLatest()
        val deviceState = deviceStateDao.get()
        val bootChanged = deviceState != null && deviceState.bootId.isNotBlank() && deviceState.bootId != bootId
        val sameBootCounterRollback = latest?.date == today &&
            !bootChanged &&
            sensorTotalSteps < latest.lastSensorSteps

        val updated = when {
            latest == null -> {
                emptyDaily(today, sensorTotalSteps, settings)
            }

            latest.date != today -> {
                val sameBootDelta = if (!bootChanged && sensorTotalSteps >= latest.lastSensorSteps) {
                    sensorTotalSteps - latest.lastSensorSteps
                } else {
                    0
                }
                buildDaily(
                    date = today,
                    baseline = if (sameBootDelta > 0) latest.lastSensorSteps else sensorTotalSteps,
                    lastSensorSteps = sensorTotalSteps,
                    dailySteps = sameBootDelta,
                    compensatedSteps = sameBootDelta,
                    goalSteps = settings.dailyGoalSteps,
                    strideLengthCm = settings.strideLengthCm,
                    weightKg = settings.weightKg,
                    isEstimated = sameBootDelta > 0,
                    source = source,
                    timestampMillis = timestampMillis
                )
            }

            sameBootCounterRollback -> {
                val trustedSteps = (latest.dailySteps - latest.pendingDetectorSteps).coerceAtLeast(0)
                buildDaily(
                    date = today,
                    baseline = latest.baselineSensorSteps,
                    lastSensorSteps = latest.lastSensorSteps,
                    dailySteps = trustedSteps,
                    compensatedSteps = latest.compensatedSteps.coerceAtMost(trustedSteps),
                    pendingDetectorSteps = 0,
                    goalSteps = latest.goalSteps,
                    strideLengthCm = settings.strideLengthCm,
                    weightKg = settings.weightKg,
                    isEstimated = latest.isEstimated,
                    source = source,
                    timestampMillis = timestampMillis
                )
            }

            bootChanged -> {
                val trustedSteps = (latest.dailySteps - latest.pendingDetectorSteps).coerceAtLeast(0)
                buildDaily(
                    date = today,
                    baseline = sensorTotalSteps,
                    lastSensorSteps = sensorTotalSteps,
                    dailySteps = trustedSteps,
                    compensatedSteps = trustedSteps,
                    pendingDetectorSteps = 0,
                    goalSteps = latest.goalSteps,
                    strideLengthCm = settings.strideLengthCm,
                    weightKg = settings.weightKg,
                    isEstimated = latest.isEstimated,
                    source = source,
                    timestampMillis = timestampMillis
                )
            }

            else -> {
                val delta = sensorTotalSteps - latest.lastSensorSteps
                val trustedPreviousSteps = (latest.dailySteps - latest.pendingDetectorSteps).coerceAtLeast(0)
                buildDaily(
                    date = today,
                    baseline = latest.baselineSensorSteps,
                    lastSensorSteps = sensorTotalSteps,
                    dailySteps = trustedPreviousSteps + delta,
                    compensatedSteps = latest.compensatedSteps,
                    pendingDetectorSteps = 0,
                    goalSteps = latest.goalSteps,
                    strideLengthCm = settings.strideLengthCm,
                    weightKg = settings.weightKg,
                    isEstimated = latest.isEstimated,
                    source = source,
                    timestampMillis = timestampMillis
                )
            }
        }

        dailyDao.upsert(updated)
        sampleDao.insert(
            SensorSampleEntity(
                timestamp = timestampMillis,
                sensorTotalSteps = sensorTotalSteps,
                deltaSteps = (updated.dailySteps - (latest?.takeIf { it.date == updated.date }?.dailySteps ?: 0)).coerceAtLeast(0),
                date = updated.date,
                source = source,
                bootId = bootId,
                note = when {
                    latest == null -> "first_install"
                    latest.date != today -> "date_changed"
                    sameBootCounterRollback -> "counter_rollback_ignored"
                    bootChanged -> "sensor_reset"
                    else -> "normal"
                }
            )
        )
        deviceStateDao.upsert(
            DeviceStateEntity(
                bootId = bootId,
                lastSensorValue = sensorTotalSteps,
                lastSyncTime = timestampMillis,
                sensorAvailable = true,
                permissionGranted = true
            )
        )
        updated
    }

    suspend fun addDetectedStep(
        bootId: String,
        timestampMillis: Long = System.currentTimeMillis(),
        source: String = DATA_SOURCE_PHONE
    ): DailyStepsEntity = database.withTransaction {
        val settings = ensureDefaults()
        val today = todayString()
        val latest = dailyDao.getByDate(today)
        val deviceState = deviceStateDao.get()
        val baseline = latest?.baselineSensorSteps ?: deviceState?.lastSensorValue ?: 0
        val lastSensorSteps = latest?.lastSensorSteps ?: baseline
        val updated = buildDaily(
            date = today,
            baseline = baseline,
            lastSensorSteps = lastSensorSteps,
            dailySteps = (latest?.dailySteps ?: 0) + 1,
            compensatedSteps = latest?.compensatedSteps ?: 0,
            pendingDetectorSteps = (latest?.pendingDetectorSteps ?: 0) + 1,
            goalSteps = latest?.goalSteps ?: settings.dailyGoalSteps,
            strideLengthCm = settings.strideLengthCm,
            weightKg = settings.weightKg,
            isEstimated = latest?.isEstimated ?: false,
            source = source,
            timestampMillis = timestampMillis
        )

        dailyDao.upsert(updated)
        sampleDao.insert(
            SensorSampleEntity(
                timestamp = timestampMillis,
                sensorTotalSteps = lastSensorSteps,
                deltaSteps = 1,
                date = today,
                source = source,
                bootId = bootId,
                note = "step_detector"
            )
        )
        deviceStateDao.upsert(
            DeviceStateEntity(
                bootId = bootId,
                lastSensorValue = lastSensorSteps,
                lastSyncTime = timestampMillis,
                sensorAvailable = true,
                permissionGranted = true
            )
        )
        updated
    }

    private fun emptyDaily(date: String, baseline: Long, settings: UserSettingsEntity): DailyStepsEntity =
        buildDaily(
            date = date,
            baseline = baseline,
            lastSensorSteps = baseline,
            dailySteps = 0,
            compensatedSteps = 0,
            pendingDetectorSteps = 0,
            goalSteps = settings.dailyGoalSteps,
            strideLengthCm = settings.strideLengthCm,
            weightKg = settings.weightKg,
            isEstimated = false,
            source = DATA_SOURCE_PHONE,
            timestampMillis = System.currentTimeMillis()
        )

    private fun buildDaily(
        date: String,
        baseline: Long,
        lastSensorSteps: Long,
        dailySteps: Long,
        compensatedSteps: Long,
        pendingDetectorSteps: Long = 0,
        goalSteps: Int,
        strideLengthCm: Int,
        weightKg: Int,
        isEstimated: Boolean,
        source: String,
        timestampMillis: Long
    ): DailyStepsEntity {
        val distance = StepMetrics.distanceMeters(dailySteps, strideLengthCm)
        return DailyStepsEntity(
            date = date,
            baselineSensorSteps = baseline,
            lastSensorSteps = lastSensorSteps,
            dailySteps = dailySteps.coerceAtLeast(0),
            compensatedSteps = compensatedSteps.coerceAtLeast(0),
            distanceMeters = distance,
            calories = StepMetrics.calories(distance, weightKg),
            activeMinutes = StepMetrics.activeMinutes(dailySteps),
            goalSteps = goalSteps,
            isEstimated = isEstimated,
            pendingDetectorSteps = pendingDetectorSteps.coerceAtLeast(0),
            dataSource = source,
            updatedAt = timestampMillis
        )
    }

    companion object {
        const val DATA_SOURCE_PHONE = "PHONE_SENSOR"
    }
}
