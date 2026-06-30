package com.lightstep.pedometer.data

import androidx.room.withTransaction
import com.lightstep.pedometer.domain.StepMetrics
import com.lightstep.pedometer.domain.daysAgoString
import com.lightstep.pedometer.domain.todayString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class StepRepository(private val database: StepDatabase) {
    private val dailyDao = database.dailyStepsDao()
    private val sampleDao = database.sensorSampleDao()
    private val settingsDao = database.userSettingsDao()
    private val deviceStateDao = database.deviceStateDao()

    fun observeToday(): Flow<DailyStepsEntity?> = dailyDao.observeByDate(todayString())

    suspend fun getToday(): DailyStepsEntity? = dailyDao.getByDate(todayString())

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
            var normalized = if (existing.heightCm == 170 && existing.weightKg == 65 && existing.strideLengthCm == 70) {
                existing.copy(
                    strideLengthCm = UserSettingsEntity().strideLengthCm,
                    strideMode = UserSettingsEntity.STRIDE_MODE_AUTO
                )
            } else {
                existing
            }
            if (normalized.birthDate.isBlank()) {
                normalized = normalized.copy(birthDate = birthDateFromAge(normalized.age))
            }
            val computedAge = ageFromBirthDate(normalized.birthDate)
            if (computedAge != normalized.age) {
                normalized = normalized.copy(age = computedAge)
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

    suspend fun updateProfile(
        displayName: String,
        avatarUri: String?,
        heightCm: Int,
        weightKg: Int,
        birthDate: String,
        gender: String,
        strideMode: String,
        strideLengthCm: Int
    ) = database.withTransaction {
        val settings = ensureDefaults()
        val cleanName = displayName.trim().take(20).ifBlank { UserSettingsEntity().displayName }
        val cleanBirthDate = normalizeBirthDate(birthDate)
        val updated = settings.copy(
            displayName = cleanName,
            avatarUri = avatarUri?.trim()?.takeIf { it.isNotBlank() },
            birthDate = cleanBirthDate,
            heightCm = heightCm.coerceIn(120, 220),
            weightKg = weightKg.coerceIn(30, 200),
            age = ageFromBirthDate(cleanBirthDate),
            gender = gender.takeIf {
                it == UserSettingsEntity.GENDER_MALE ||
                    it == UserSettingsEntity.GENDER_FEMALE ||
                    it == UserSettingsEntity.GENDER_UNSPECIFIED
            } ?: UserSettingsEntity.GENDER_UNSPECIFIED,
            strideMode = strideMode.takeIf {
                it == UserSettingsEntity.STRIDE_MODE_AUTO ||
                    it == UserSettingsEntity.STRIDE_MODE_MANUAL
            } ?: UserSettingsEntity.STRIDE_MODE_AUTO,
            strideLengthCm = strideLengthCm.coerceIn(45, 120)
        )
        settingsDao.upsert(updated)
        recalculateTodayMetrics(updated)
    }

    suspend fun calibrateStrideFromDistance(distanceKm: Double): UserSettingsEntity = database.withTransaction {
        val settings = ensureDefaults()
        val today = dailyDao.getByDate(todayString())
        val steps = today?.dailySteps ?: 0
        val calibratedStride = if (steps > 0) {
            ((distanceKm.coerceIn(0.1, 100.0) * 100000.0) / steps).roundToInt().coerceIn(45, 120)
        } else {
            settings.strideLengthCm
        }
        val updated = settings.copy(
            strideMode = UserSettingsEntity.STRIDE_MODE_MANUAL,
            strideLengthCm = calibratedStride
        )
        settingsDao.upsert(updated)
        recalculateTodayMetrics(updated)
        updated
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
                    settings = settings,
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
                    settings = settings,
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
                    settings = settings,
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
                    settings = settings,
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
            settings = settings,
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
            settings = settings,
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
        settings: UserSettingsEntity,
        isEstimated: Boolean,
        source: String,
        timestampMillis: Long
    ): DailyStepsEntity {
        val distance = StepMetrics.distanceMeters(dailySteps, StepMetrics.effectiveStrideCm(settings))
        return DailyStepsEntity(
            date = date,
            baselineSensorSteps = baseline,
            lastSensorSteps = lastSensorSteps,
            dailySteps = dailySteps.coerceAtLeast(0),
            compensatedSteps = compensatedSteps.coerceAtLeast(0),
            distanceMeters = distance,
            calories = StepMetrics.calories(distance, settings.weightKg, settings.gender, settings.age),
            activeMinutes = StepMetrics.activeMinutes(dailySteps),
            goalSteps = goalSteps,
            isEstimated = isEstimated,
            pendingDetectorSteps = pendingDetectorSteps.coerceAtLeast(0),
            dataSource = source,
            updatedAt = timestampMillis
        )
    }

    private suspend fun recalculateTodayMetrics(settings: UserSettingsEntity) {
        val today = dailyDao.getByDate(todayString()) ?: return
        val distance = StepMetrics.distanceMeters(today.dailySteps, StepMetrics.effectiveStrideCm(settings))
        dailyDao.upsert(
            today.copy(
                distanceMeters = distance,
                calories = StepMetrics.calories(distance, settings.weightKg, settings.gender, settings.age),
                activeMinutes = StepMetrics.activeMinutes(today.dailySteps),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun normalizeBirthDate(value: String): String {
        val parts = value.split("-").mapNotNull { it.toIntOrNull() }
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val year = parts.getOrNull(0)?.coerceIn(currentYear - 100, currentYear - 10) ?: (currentYear - 35)
        val month = parts.getOrNull(1)?.coerceIn(1, 12) ?: 1
        val maxDay = daysInMonth(year, month)
        val day = parts.getOrNull(2)?.coerceIn(1, maxDay) ?: 1
        return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
    }

    private fun birthDateFromAge(age: Int): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -age.coerceIn(10, 100))
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun ageFromBirthDate(birthDate: String): Int {
        val parts = normalizeBirthDateParts(birthDate) ?: return 35
        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - parts.year
        val currentMonth = today.get(Calendar.MONTH) + 1
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        if (currentMonth < parts.month || (currentMonth == parts.month && currentDay < parts.day)) {
            age -= 1
        }
        return age.coerceIn(10, 100)
    }

    private fun normalizeBirthDateParts(birthDate: String): BirthDateParts? {
        val parts = birthDate.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3) return null
        val year = parts[0]
        val month = parts[1]
        val day = parts[2]
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year !in currentYear - 100..currentYear - 10 || month !in 1..12 || day !in 1..daysInMonth(year, month)) return null
        return BirthDateParts(year, month, day)
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private data class BirthDateParts(val year: Int, val month: Int, val day: Int)

    companion object {
        const val DATA_SOURCE_PHONE = "PHONE_SENSOR"
    }
}
