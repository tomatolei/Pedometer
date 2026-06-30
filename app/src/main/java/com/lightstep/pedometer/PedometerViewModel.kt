package com.lightstep.pedometer

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lightstep.pedometer.data.DailyStepsEntity
import com.lightstep.pedometer.data.SensorSampleEntity
import com.lightstep.pedometer.data.StepDatabase
import com.lightstep.pedometer.data.StepRepository
import com.lightstep.pedometer.data.UserSettingsEntity
import com.lightstep.pedometer.domain.SystemBoot
import com.lightstep.pedometer.sensor.StepCounterReader
import com.lightstep.pedometer.sensor.StepReadResult
import com.lightstep.pedometer.service.RealtimeStepService
import com.lightstep.pedometer.service.WidgetSyncService
import com.lightstep.pedometer.widget.StepWidgetUpdater
import com.lightstep.pedometer.worker.StepSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PedometerViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val repository = StepRepository(StepDatabase.get(appContext))
    private val reader = StepCounterReader(appContext)

    private val permissionGranted = MutableStateFlow(reader.hasPermission())
    private val sensorAvailable = MutableStateFlow(reader.hasStepSensor())
    private val refreshing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val dataState = combine(
        repository.observeToday(),
        repository.observeSevenDays(),
        repository.observeThirtyDays(),
        repository.observeSettings(),
        repository.observeTodaySamples()
    ) { today, week, month, settings, samples ->
        PedometerDataState(today, week, month, settings, samples)
    }

    val uiState: StateFlow<PedometerUiState> = combine(
        dataState,
        permissionGranted,
        sensorAvailable,
        refreshing,
        message
    ) { data, hasPermission, hasSensor, isRefreshing, userMessage ->
        PedometerUiState(
            today = data.today,
            week = data.week,
            month = data.month,
            settings = data.settings,
            samples = data.samples,
            permissionGranted = hasPermission,
            sensorAvailable = hasSensor,
            refreshing = isRefreshing,
            message = userMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PedometerUiState()
    )

    init {
        StepSyncWorker.enqueuePeriodic(appContext)
        viewModelScope.launch {
            repository.ensureDefaults()
            repository.setDeviceStatus(reader.hasStepSensor(), reader.hasPermission())
            if (reader.hasPermission()) {
                refreshFromSensor(showMessage = false)
                WidgetSyncService.startIfNeeded(appContext)
            }
        }
    }

    fun onPermissionResult(granted: Boolean) {
        permissionGranted.value = granted || reader.hasPermission()
        sensorAvailable.value = reader.hasStepSensor()
        viewModelScope.launch {
            repository.setDeviceStatus(sensorAvailable.value, permissionGranted.value)
            if (permissionGranted.value) {
                refreshFromSensor(showMessage = true)
                WidgetSyncService.startIfNeeded(appContext)
            } else {
                message.value = "需要计步权限后才能读取手机步数"
            }
        }
    }

    fun refreshFromSensor(showMessage: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            sensorAvailable.value = reader.hasStepSensor()
            permissionGranted.value = reader.hasPermission()
            when (val result = reader.readCurrentSteps(timeoutMillis = if (showMessage) 6500 else 4500)) {
                is StepReadResult.Success -> {
                    repository.syncSensorTotal(result.totalSteps, SystemBoot.currentBootId())
                    repository.setDeviceStatus(sensorAvailable = true, permissionGranted = true)
                    StepWidgetUpdater.updateAllNow(appContext)
                    if (showMessage) message.value = "步数已刷新"
                }

                StepReadResult.NoSensor -> {
                    repository.setDeviceStatus(sensorAvailable = false, permissionGranted = true)
                    if (showMessage) message.value = "当前手机未检测到硬件计步传感器"
                }

                StepReadResult.PermissionDenied -> {
                    repository.setDeviceStatus(sensorAvailable.value, permissionGranted = false)
                    if (showMessage) message.value = "请先授权身体活动权限"
                }

                StepReadResult.Timeout -> {
                    if (showMessage) message.value = "传感器暂未返回数据，稍后会继续同步"
                }

                is StepReadResult.Error -> {
                    if (showMessage) message.value = "传感器读取失败：${result.reason}"
                }
            }
            refreshing.value = false
        }
    }

    fun setGoal(goalSteps: Int) {
        viewModelScope.launch {
            repository.updateGoal(goalSteps)
            StepWidgetUpdater.updateAllNow(appContext)
        }
    }

    fun updateProfile(
        displayName: String,
        avatarUri: String?,
        heightCm: Int,
        weightKg: Int,
        birthDate: String,
        gender: String,
        strideMode: String,
        strideLengthCm: Int
    ) {
        viewModelScope.launch {
            repository.updateProfile(displayName, avatarUri, heightCm, weightKg, birthDate, gender, strideMode, strideLengthCm)
            StepWidgetUpdater.updateAllNow(appContext)
            message.value = "个人资料已更新"
        }
    }

    fun calibrateStride(distanceKm: Double) {
        viewModelScope.launch {
            val settings = repository.calibrateStrideFromDistance(distanceKm)
            StepWidgetUpdater.updateAllNow(appContext)
            message.value = "步幅已校准为 ${settings.strideLengthCm} cm"
        }
    }

    fun setTheme(themeMode: String) {
        viewModelScope.launch {
            repository.updateTheme(themeMode)
        }
    }

    fun setRealtime(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            repository.setRealtimeEnabled(enabled)
            if (enabled) {
                WidgetSyncService.stop(context.applicationContext)
                RealtimeStepService.start(context.applicationContext)
                message.value = "实时计步已开启"
            } else {
                RealtimeStepService.stop(context.applicationContext)
                WidgetSyncService.startIfNeeded(context.applicationContext)
                message.value = "实时计步已关闭"
            }
            StepWidgetUpdater.updateAllNow(appContext)
        }
    }

    fun clearMessage() {
        message.value = null
    }
}

private data class PedometerDataState(
    val today: DailyStepsEntity?,
    val week: List<DailyStepsEntity>,
    val month: List<DailyStepsEntity>,
    val settings: UserSettingsEntity,
    val samples: List<SensorSampleEntity>
)

data class PedometerUiState(
    val today: DailyStepsEntity? = null,
    val week: List<DailyStepsEntity> = emptyList(),
    val month: List<DailyStepsEntity> = emptyList(),
    val settings: UserSettingsEntity = UserSettingsEntity(),
    val samples: List<SensorSampleEntity> = emptyList(),
    val permissionGranted: Boolean = false,
    val sensorAvailable: Boolean = true,
    val refreshing: Boolean = false,
    val message: String? = null
)
