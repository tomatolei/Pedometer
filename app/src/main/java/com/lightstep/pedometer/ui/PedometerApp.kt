package com.lightstep.pedometer.ui

import android.content.Context
import android.content.Intent
import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightstep.pedometer.PedometerUiState
import com.lightstep.pedometer.data.DailyStepsEntity
import com.lightstep.pedometer.data.SensorSampleEntity
import com.lightstep.pedometer.data.UserSettingsEntity
import com.lightstep.pedometer.domain.StepMetrics
import com.lightstep.pedometer.ui.theme.StepBlue
import com.lightstep.pedometer.ui.theme.StepGreen
import com.lightstep.pedometer.ui.theme.StepMuted
import com.lightstep.pedometer.ui.theme.StepOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PedometerApp(
    state: PedometerUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onUpdateProfile: (String, String?, Int, Int, String, String, String, Int) -> Unit,
    onCalibrateStride: (Double) -> Unit,
    onSetTheme: (String) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenBackgroundActivity: () -> Unit,
    onClearMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }
    var profileEditorOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val text = state.message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            onClearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.permissionGranted && state.sensorAvailable && !profileEditorOpen) {
                BottomTabs(selectedTab) { selectedTab = it }
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                !state.permissionGranted -> PermissionScreen(onRequestPermission)
                !state.sensorAvailable -> NoSensorScreen(onRefresh)
                profileEditorOpen -> ProfileEditScreen(
                    state = state,
                    onDone = { profileEditorOpen = false },
                    onUpdateProfile = onUpdateProfile,
                    onCalibrateStride = onCalibrateStride
                )
                selectedTab == MainTab.Today -> TodayScreen(state, onRefresh, onToggleRealtime)
                selectedTab == MainTab.Stats -> StatsScreen(state)
                selectedTab == MainTab.Widget -> WidgetScreen(state, onRefresh)
                selectedTab == MainTab.Me -> SettingsScreen(
                    state = state,
                    onSetGoal = onSetGoal,
                    onEditProfile = { profileEditorOpen = true },
                    onSetTheme = onSetTheme,
                    onToggleRealtime = onToggleRealtime,
                    onOpenBatteryOptimization = onOpenBatteryOptimization,
                    onOpenBackgroundActivity = onOpenBackgroundActivity
                )
            }
        }
    }
}

@Composable
private fun BottomTabs(selected: MainTab, onSelected: (MainTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        MainTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selected == tab,
                onClick = { onSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.label) },
                label = { Text(tab.label) }
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(20.dp))
        Box(modifier = Modifier.size(190.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(StepGreen.copy(alpha = 0.18f), radius = size.minDimension * 0.48f)
                drawCircle(StepBlue.copy(alpha = 0.18f), radius = size.minDimension * 0.34f, center = Offset(size.width * 0.82f, size.height * 0.34f))
            }
            Box(
                modifier = Modifier
                    .size(86.dp)
                    .clip(CircleShape)
                    .background(StepGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.DirectionsWalk, null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("轻步", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text("独立本地计步器", color = StepMuted, modifier = Modifier.padding(top = 6.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureLine(Icons.Outlined.CheckCircle, "不登录", "本地保存，不需要账号")
            FeatureLine(Icons.Outlined.CheckCircle, "不依赖三星健康", "Knox 熔断也能使用")
            FeatureLine(Icons.Outlined.CheckCircle, "支持小组件", "桌面直接看今日步数")
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StepGreen)
            ) {
                Text("开始使用")
            }
            Text("仅记录步数，不上传健康数据", color = StepMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 18.dp))
        }
    }
}

@Composable
private fun NoSensorScreen(onRefresh: () -> Unit) {
    ScreenColumn {
        Header(title = "设备状态", subtitle = "计步能力检测")
        ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(StepOrange.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Warning, null, tint = StepOrange, modifier = Modifier.size(48.dp))
                }
                Text("未检测到硬件计步传感器", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp))
                Text("当前手机可能不支持本地硬件计步", color = StepMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            }
        }
        SettingsRow(Icons.Outlined.Refresh, "重新检测", "再次读取手机传感器状态", onRefresh)
        SettingsRow(Icons.Outlined.Shield, "后台刷新", "已保留后续接入 Health Connect 的扩展位置", null)
    }
}

@Composable
private fun TodayScreen(
    state: PedometerUiState,
    onRefresh: () -> Unit,
    onToggleRealtime: (Boolean) -> Unit
) {
    val today = state.today
    val steps = today?.dailySteps ?: 0
    val goal = today?.goalSteps ?: state.settings.dailyGoalSteps
    val progress = (steps.toFloat() / goal.coerceAtLeast(1)).coerceIn(0f, 1f)

    ScreenColumn(spacing = 10.dp, horizontalPadding = 14.dp, verticalPadding = 10.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Header(title = "今日", subtitle = "${todayLabel()} · 手机传感器")
            RealtimeChip(state.settings.realtimeModeEnabled)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onRefresh,
                enabled = !state.refreshing,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = StepGreen)
            ) {
                Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (state.refreshing) "刷新中" else "刷新")
            }
            FilledTonalButton(
                onClick = { onToggleRealtime(!state.settings.realtimeModeEnabled) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(if (state.settings.realtimeModeEnabled) Icons.Outlined.Pause else Icons.Outlined.PlayArrow, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(5.dp))
                Text(if (state.settings.realtimeModeEnabled) "停止实时" else "实时计步")
            }
        }

        ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("今日步数", fontWeight = FontWeight.SemiBold)
                    StatusPill("目标 ${number(goal)}")
                }
                StepRing(steps = steps, progress = progress, modifier = Modifier.padding(top = 4.dp), height = 136.dp, ringSize = 124.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(Icons.Outlined.Straighten, "距离", "${oneDecimal((today?.distanceMeters ?: 0.0) / 1000.0)} km", StepBlue, Modifier.weight(1f), compact = true)
                    MetricCard(Icons.Outlined.LocalFireDepartment, "热量", "${(today?.calories ?: 0.0).roundToInt()} kcal", StepOrange, Modifier.weight(1f), compact = true)
                    MetricCard(Icons.Outlined.Timer, "活跃", "${today?.activeMinutes ?: 0} 分钟", StepGreen, Modifier.weight(1f), compact = true)
                }
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("今日时段分布", fontWeight = FontWeight.SemiBold)
                    Text("更新 ${timeLabel(today?.updatedAt)}", color = StepMuted, fontSize = 12.sp)
                }
                HourChart(samples = state.samples, totalSteps = steps, modifier = Modifier.padding(top = 6.dp), height = 96.dp)
            }
        }
        if (today?.isEstimated == true) {
            NoticeCard("含跨日估算，后续采样会校正。", compact = true)
        } else {
            NoticeCard("普通模式亮屏补刷；实时模式更及时。", compact = true)
        }
    }
}

@Composable
private fun StatsScreen(state: PedometerUiState) {
    var period by rememberSaveable { mutableStateOf(StatsPeriod.Week) }
    val week = buildWeek(state.week, state.settings.dailyGoalSteps)
    val month = buildMonth(state.month, state.settings.dailyGoalSteps)

    ScreenColumn(spacing = 12.dp, verticalPadding = 14.dp) {
        Header(
            title = "统计",
            subtitle = when (period) {
                StatsPeriod.Day -> "今日时段 · 本机步数"
                StatsPeriod.Week -> "最近 7 天 · 本机步数"
                StatsPeriod.Month -> "最近 30 天 · 本机步数"
            }
        )
        SegmentedHeader(selected = period, onSelected = { period = it })
        when (period) {
            StatsPeriod.Day -> DayStatsContent(state)
            StatsPeriod.Week -> WeekStatsContent(week)
            StatsPeriod.Month -> MonthStatsContent(month)
        }
        NoticeCard("第一版只统计手机传感器步数，手机没带在身上时不会记录人体实际步数。", compact = true)
    }
}

@Composable
private fun DayStatsContent(state: PedometerUiState) {
    val today = state.today
    StatChartCard(
        title = "今日时段",
        subtitle = "总步数 ${number(today?.dailySteps ?: 0)} · 更新 ${timeLabel(today?.updatedAt)}"
    ) {
        HourChart(samples = state.samples, totalSteps = today?.dailySteps ?: 0, height = 138.dp)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("距离", "${oneDecimal((today?.distanceMeters ?: 0.0) / 1000.0)} km", "估算", StepBlue, Modifier.weight(1f), compact = true)
        SummaryCard("热量", "${(today?.calories ?: 0.0).roundToInt()} kcal", "估算", StepOrange, Modifier.weight(1f), compact = true)
        SummaryCard("活跃", "${today?.activeMinutes ?: 0} 分钟", "今日", StepGreen, Modifier.weight(1f), compact = true)
    }
}

@Composable
private fun WeekStatsContent(week: List<WeekDay>) {
    val total = week.sumOf { it.steps }
    val average = if (week.isNotEmpty()) total / week.size else 0
    val best = week.maxOfOrNull { it.steps } ?: 0
    val reached = week.count { it.steps >= it.goal }
    StatChartCard(
        title = "本周趋势",
        subtitle = "总步数 ${number(total)} · 平均 ${number(average)}"
    ) {
        LabeledBarChart(
            bars = week.map { it.steps.toFloat() },
            labels = week.map { it.label },
            highlightIndex = week.lastIndex,
            modifier = Modifier.height(190.dp)
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("达标天数", "$reached 天", "手机随身统计", StepGreen, Modifier.weight(1f), compact = true)
        SummaryCard("最高纪录", number(best), "近 7 天", StepOrange, Modifier.weight(1f), compact = true)
        SummaryCard("连续记录", "${week.count { it.steps > 0 }} 天", "近 7 天", StepBlue, Modifier.weight(1f), compact = true)
    }
}

@Composable
private fun MonthStatsContent(month: List<MonthDay>) {
    val total = month.sumOf { it.steps }
    val average = if (month.isNotEmpty()) total / month.size else 0
    val best = month.maxOfOrNull { it.steps } ?: 0
    val reached = month.count { it.steps >= it.goal }
    StatChartCard(
        title = "30 天趋势",
        subtitle = "总步数 ${number(total)} · 平均 ${number(average)}"
    ) {
        LabeledBarChart(
            bars = month.map { it.steps.toFloat() },
            labels = month.mapIndexed { index, day -> if (index % 5 == 0 || index == month.lastIndex) day.label else "" },
            highlightIndex = month.lastIndex,
            modifier = Modifier.height(190.dp),
            labelEveryBar = false
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("达标天数", "$reached 天", "最近 30 天", StepGreen, Modifier.weight(1f), compact = true)
        SummaryCard("最高纪录", number(best), "最近 30 天", StepOrange, Modifier.weight(1f), compact = true)
        SummaryCard("平均步数", number(average), "每日", StepBlue, Modifier.weight(1f), compact = true)
    }
}

@Composable
private fun WidgetScreen(state: PedometerUiState, onRefresh: () -> Unit) {
    val steps = state.today?.dailySteps ?: 0
    val goal = state.today?.goalSteps ?: state.settings.dailyGoalSteps
    val percent = ((steps * 100.0) / goal.coerceAtLeast(1)).roundToInt()

    ScreenColumn(spacing = 7.dp, verticalPadding = 10.dp) {
        Header(title = "小组件", subtitle = "桌面快速查看步数")
        CompactWidgetRow(iconLabel = "1×1", title = "极简步数", subtitle = "今日") {
            CompactWidgetSmallPreview(steps)
        }
        CompactWidgetRow(iconLabel = "2×1", title = "步数进度", subtitle = "横向进度") {
            CompactWidgetProgressPreview(steps, percent)
        }
        CompactWidgetRow(iconLabel = "2×2", title = "步数卡片", subtitle = "步数 / 目标") {
            CompactWidgetSquarePreview(steps, percent)
        }
        CompactWidgetRow(iconLabel = "4×1", title = "步数横条", subtitle = "桌面横幅") {
            CompactWidgetWidePreview(steps, goal, percent)
        }
        CompactWidgetRow(iconLabel = "4×2", title = "周趋势", subtitle = "近 7 天") {
            CompactWidgetWeekPreview(buildWeek(state.week, goal))
        }
        CompactWidgetRow(iconLabel = "4×2", title = "活动总览", subtitle = "数据汇总", tall = true) {
            CompactActivityPreview(
                steps = steps,
                distanceKm = (state.today?.distanceMeters ?: 0.0) / 1000.0,
                minutes = state.today?.activeMinutes ?: 0,
                calories = (state.today?.calories ?: 0.0).roundToInt()
            )
        }
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = StepGreen)
        ) {
            Icon(Icons.Outlined.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("刷新小组件数据")
        }
    }
}

@Composable
private fun SettingsScreen(
    state: PedometerUiState,
    onSetGoal: (Int) -> Unit,
    onEditProfile: () -> Unit,
    onSetTheme: (String) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenBackgroundActivity: () -> Unit
) {
    var goalValue by remember(state.settings.dailyGoalSteps) { mutableFloatStateOf(state.settings.dailyGoalSteps.toFloat()) }
    val settings = state.settings
    val effectiveStride = StepMetrics.effectiveStrideCm(settings)
    val todayDistanceKm = (state.today?.distanceMeters ?: 0.0) / 1000.0

    ScreenColumn {
        Header(title = "我的", subtitle = "本地数据 · 无账号")
        ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .clip(CircleShape)
                            .background(StepGreen.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarImage(settings.avatarUri, size = 66.dp, cornerRadius = 33.dp)
                    }
                    Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                        Text(settings.displayName, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "${genderLabel(settings.gender)} · ${settings.heightCm} cm · ${settings.weightKg} kg · ${settings.age} 岁",
                            color = StepMuted,
                            fontSize = 12.sp
                        )
                        Text(
                            "步幅 $effectiveStride cm · ${strideModeLabel(settings.strideMode)}",
                            color = StepMuted,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = onEditProfile) {
                        Text("编辑")
                    }
                }
                Divider(color = MaterialTheme.colorScheme.background)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ProfileSummaryMetric("今日", "${number(state.today?.dailySteps ?: 0)} 步", Modifier.weight(1f))
                    ProfileSummaryMetric("距离", "${twoDecimal(todayDistanceKm)} km", Modifier.weight(1f))
                    ProfileSummaryMetric("状态", if (state.sensorAvailable) "正常" else "异常", Modifier.weight(1f))
                }
            }
        }

        SettingsSection("目标设置") {
            Text("每日目标 ${number(goalValue.roundToInt())} 步", fontWeight = FontWeight.SemiBold)
            Slider(
                value = goalValue,
                onValueChange = { goalValue = it },
                valueRange = 1000f..30000f,
                steps = 28,
                onValueChangeFinished = { onSetGoal(goalValue.roundToInt()) }
            )
        }

        SettingsSection("实时计步") {
            SettingSwitchRow(
                icon = Icons.Outlined.Notifications,
                title = "前台服务",
                subtitle = "开启后锁屏和小组件刷新更及时",
                checked = state.settings.realtimeModeEnabled,
                onCheckedChange = onToggleRealtime
            )
        }

        SettingsSection("后台保护") {
            ProtectionLine(Icons.Outlined.CheckCircle, "计步权限", "已开启", good = state.permissionGranted)
            ProtectionLine(Icons.Outlined.CheckCircle, "硬件传感器", if (state.sensorAvailable) "已检测" else "未检测", good = state.sensorAvailable)
            ProtectionActionLine(Icons.Outlined.Warning, "忽略电池优化", "减少系统限制后台刷新", "设置", onOpenBatteryOptimization)
            ProtectionActionLine(Icons.Outlined.Warning, "后台活动", "打开系统应用详情后进入电池/后台选项", "设置", onOpenBackgroundActivity)
        }

        SettingsSection("外观") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ThemeButton("系统", Icons.Outlined.Settings, state.settings.themeMode == "system", Modifier.weight(1f)) { onSetTheme("system") }
                ThemeButton("浅色", Icons.Outlined.LightMode, state.settings.themeMode == "light", Modifier.weight(1f)) { onSetTheme("light") }
                ThemeButton("深色", Icons.Outlined.DarkMode, state.settings.themeMode == "dark", Modifier.weight(1f)) { onSetTheme("dark") }
            }
        }

        SettingsSection("关于") {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AboutRow("作者", "累累")
                AboutRow("邮箱", "823921@qq.com")
                AboutRow("GitHub", "tomatolei/Pedometer")
                AboutRow("主页", "tomatolei.cn")
            }
        }
    }
}

@Composable
private fun ProfileEditScreen(
    state: PedometerUiState,
    onDone: () -> Unit,
    onUpdateProfile: (String, String?, Int, Int, String, String, String, Int) -> Unit,
    onCalibrateStride: (Double) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember(state.settings.displayName) { mutableStateOf(state.settings.displayName) }
    var avatarUri by remember(state.settings.avatarUri) { mutableStateOf(state.settings.avatarUri) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            avatarUri = uri.toString()
        }
    }
    var heightValue by remember(state.settings.heightCm) { mutableFloatStateOf(state.settings.heightCm.toFloat()) }
    var weightValue by remember(state.settings.weightKg) { mutableFloatStateOf(state.settings.weightKg.toFloat()) }
    var birthDate by remember(state.settings.birthDate) { mutableStateOf(state.settings.birthDate) }
    var nicknameEditorOpen by remember { mutableStateOf(false) }
    var nicknameDraft by remember(displayName) { mutableStateOf(displayName) }
    var strideValue by remember(state.settings.strideLengthCm) { mutableFloatStateOf(state.settings.strideLengthCm.toFloat()) }
    var selectedGender by remember(state.settings.gender) { mutableStateOf(state.settings.gender) }
    var selectedStrideMode by remember(state.settings.strideMode) { mutableStateOf(state.settings.strideMode) }
    val initialCalibrationKm = ((state.today?.distanceMeters ?: 1450.0) / 1000.0).coerceIn(0.5, 10.0).toFloat()
    var calibrationKm by remember(state.today?.dailySteps) { mutableFloatStateOf(initialCalibrationKm) }
    val autoStride = StepMetrics.estimatedStrideCm(
        heightValue.roundToInt(),
        selectedGender,
        ageFromBirthDateForUi(birthDate)
    )
    val effectiveStride = if (selectedStrideMode == UserSettingsEntity.STRIDE_MODE_MANUAL) {
        strideValue.roundToInt()
    } else {
        autoStride
    }
    val currentSteps = state.today?.dailySteps ?: 0L
    val previewDistanceKm = currentSteps * effectiveStride / 100000.0
    val saveProfile = {
        onUpdateProfile(
            displayName,
            avatarUri,
            heightValue.roundToInt(),
            weightValue.roundToInt(),
            birthDate,
            selectedGender,
            selectedStrideMode,
            strideValue.roundToInt()
        )
    }

    if (nicknameEditorOpen) {
        AlertDialog(
            onDismissRequest = { nicknameEditorOpen = false },
            title = { Text("修改昵称") },
            text = {
                OutlinedTextField(
                    value = nicknameDraft,
                    onValueChange = { nicknameDraft = it.take(20) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        displayName = nicknameDraft.trim().ifBlank { UserSettingsEntity().displayName }
                        nicknameEditorOpen = false
                    }
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { nicknameEditorOpen = false }) {
                    Text("取消")
                }
            }
        )
    }

    ScreenColumn(spacing = 10.dp, horizontalPadding = 18.dp, verticalPadding = 8.dp, scrollResetKey = "profile-editor") {
        ProfileTopBar(
            title = "个人资料",
            onBack = onDone,
            onSave = {
                saveProfile()
                onDone()
            }
        )

        ProfileHeroCard(
            displayName = displayName.ifBlank { UserSettingsEntity().displayName },
            avatarUri = avatarUri,
            onPickAvatar = { avatarPicker.launch(arrayOf("image/*")) }
        )

        ProfileSectionTitle("个人信息")
        ProfileInfoCard {
            ProfileInfoRow(
                icon = Icons.Outlined.Person,
                label = "头像",
                onClick = { avatarPicker.launch(arrayOf("image/*")) }
            ) {
                AvatarImage(avatarUri, size = 44.dp, cornerRadius = 22.dp)
                Text("›", color = StepMuted, fontSize = 28.sp)
            }
            Divider(color = MaterialTheme.colorScheme.background)
            ProfileInfoRow(
                icon = Icons.Outlined.Edit,
                label = "昵称",
                onClick = {
                    nicknameDraft = displayName
                    nicknameEditorOpen = true
                }
            ) {
                Text(displayName.ifBlank { UserSettingsEntity().displayName }, color = StepMuted, fontSize = 16.sp)
                Text("›", color = StepMuted, fontSize = 28.sp)
            }
            Divider(color = MaterialTheme.colorScheme.background)
            ProfileInfoRow(icon = Icons.Outlined.Person, label = "性别") {
                ProfileGenderControl(selectedGender) { selectedGender = it }
            }
            Divider(color = MaterialTheme.colorScheme.background)
            ProfileInfoRow(
                icon = Icons.Outlined.Timer,
                label = "生日",
                onClick = {
                    showBirthDatePicker(context, birthDate) { picked ->
                        birthDate = picked
                    }
                }
            ) {
                Text(birthDate.ifBlank { "1991-01-01" }, color = StepMuted, fontSize = 16.sp)
                Text("›", color = StepMuted, fontSize = 28.sp)
            }
        }

        ProfileSectionTitle("身体数据")
        BodyDataCard {
            BodyDataSliderRow(
                icon = Icons.Outlined.Straighten,
                label = "身高",
                value = heightValue,
                valueText = "${heightValue.roundToInt()} cm",
                range = 120f..220f,
                steps = 99,
                minText = "120",
                maxText = "220",
                onValueChange = { heightValue = it }
            )
            Divider(color = MaterialTheme.colorScheme.background)
            BodyDataSliderRow(
                icon = Icons.Outlined.Settings,
                label = "体重",
                value = weightValue,
                valueText = "${weightValue.roundToInt()} kg",
                range = 30f..200f,
                steps = 169,
                minText = "30",
                maxText = "200",
                onValueChange = { weightValue = it }
            )
        }

        ProfileNotice("数据仅用于健康管理，不会用于其他用途")

        ProfileSectionTitle("步幅与距离")
        StrideDistanceCard(
            selectedStrideMode = selectedStrideMode,
            onStrideModeChange = { selectedStrideMode = it },
            effectiveStride = effectiveStride,
            autoStride = autoStride,
            previewDistanceKm = previewDistanceKm,
            strideValue = strideValue,
            onStrideValueChange = {
                strideValue = it
                selectedStrideMode = UserSettingsEntity.STRIDE_MODE_MANUAL
            },
            calibrationKm = calibrationKm,
            onCalibrationChange = { calibrationKm = it },
            currentSteps = currentSteps,
            onCalibrateStride = onCalibrateStride
        )
    }
}

@Composable
private fun ProfileTopBar(title: String, onBack: () -> Unit, onSave: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBack) {
            Text("‹ 返回", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        }
        Text(
            title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onSave) {
            Text("保存", color = StepGreen, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ProfileHeroCard(displayName: String, avatarUri: String?, onPickAvatar: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(126.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            StepGreen.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface,
                            StepGreen.copy(alpha = 0.10f)
                        )
                    )
                )
                .padding(horizontal = 22.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val leafColor = StepGreen.copy(alpha = 0.055f)
                drawCircle(leafColor, radius = 48.dp.toPx(), center = Offset(size.width * 0.92f, size.height * 0.74f))
                drawCircle(leafColor.copy(alpha = 0.72f), radius = 30.dp.toPx(), center = Offset(size.width * 0.84f, size.height * 0.88f))
                drawCircle(leafColor.copy(alpha = 0.58f), radius = 24.dp.toPx(), center = Offset(size.width * 0.98f, size.height * 0.48f))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(90.dp), contentAlignment = Alignment.Center) {
                    AvatarImage(avatarUri, size = 86.dp, cornerRadius = 43.dp)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clickable { onPickAvatar() },
                        shape = CircleShape,
                        color = StepGreen,
                        contentColor = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 21.sp)
                        Icon(Icons.Outlined.Edit, null, tint = StepGreen, modifier = Modifier.padding(start = 7.dp).size(18.dp))
                    }
                    Text("点击修改头像和昵称", color = StepMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
}

@Composable
private fun ProfileInfoCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), content = content)
    }
}

@Composable
private fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileIconBox(icon)
        Text(label, fontSize = 16.sp, modifier = Modifier.padding(start = 14.dp))
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            trailing()
        }
    }
}

@Composable
private fun ProfileIconBox(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(StepGreen.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = StepGreen, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun ProfileGenderControl(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .width(192.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProfileGenderSegment("男", UserSettingsEntity.GENDER_MALE, selected, Modifier.weight(1f), onSelected)
        ProfileGenderSegment("女", UserSettingsEntity.GENDER_FEMALE, selected, Modifier.weight(1f), onSelected)
        ProfileGenderSegment("未设置", UserSettingsEntity.GENDER_UNSPECIFIED, selected, Modifier.weight(1f), onSelected)
    }
}

@Composable
private fun ProfileGenderSegment(
    label: String,
    value: String,
    selected: String,
    modifier: Modifier,
    onSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight().clickable { onSelected(value) },
        color = if (selected == value) StepGreen else Color.Transparent,
        contentColor = if (selected == value) Color.White else StepMuted
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp)
        }
    }
}

@Composable
private fun BodyDataCard(content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), content = content)
    }
}

@Composable
private fun BodyDataSliderRow(
    icon: ImageVector,
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    minText: String,
    maxText: String,
    onValueChange: (Float) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        ProfileIconBox(icon)
        Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Text(valueText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Surface(
                    modifier = Modifier.padding(start = 8.dp).size(30.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = StepMuted
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(minText, color = StepMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp))
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = range,
                    steps = steps,
                    modifier = Modifier.weight(1f)
                )
                Text(maxText, color = StepMuted, fontSize = 11.sp, textAlign = TextAlign.End, modifier = Modifier.width(34.dp))
            }
        }
    }
}

@Composable
private fun ProfileNotice(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StepGreen.copy(alpha = 0.08f),
        contentColor = StepMuted
    ) {
        Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Shield, null, tint = StepGreen, modifier = Modifier.size(18.dp))
            Text(text, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
        }
    }
}

@Composable
private fun StrideDistanceCard(
    selectedStrideMode: String,
    onStrideModeChange: (String) -> Unit,
    effectiveStride: Int,
    autoStride: Int,
    previewDistanceKm: Double,
    strideValue: Float,
    onStrideValueChange: (Float) -> Unit,
    calibrationKm: Float,
    onCalibrationChange: (Float) -> Unit,
    currentSteps: Long,
    onCalibrateStride: (Double) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StrideModeControl(selectedStrideMode, onStrideModeChange)
            Text(
                "当前使用 $effectiveStride cm · 自动估算 $autoStride cm · 今日约 ${twoDecimal(previewDistanceKm)} km",
                color = StepMuted,
                fontSize = 12.sp
            )
            ProfileSlider(
                label = "手动步幅",
                value = strideValue,
                valueText = "${strideValue.roundToInt()} cm",
                range = 45f..120f,
                steps = 74,
                onValueChange = onStrideValueChange,
                onFinished = {}
            )
            ProfileSlider(
                label = "三星健康距离",
                value = calibrationKm,
                valueText = "${twoDecimal(calibrationKm.toDouble())} km",
                range = 0.5f..10f,
                steps = 94,
                onValueChange = onCalibrationChange,
                onFinished = {}
            )
            Button(
                onClick = { onCalibrateStride(calibrationKm.toDouble()) },
                enabled = currentSteps > 0,
                colors = ButtonDefaults.buttonColors(containerColor = StepGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("按当前步数校准步幅")
            }
        }
    }
}

@Composable
private fun StrideModeControl(selected: String, onSelected: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.background),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StrideModeSegment("自动", UserSettingsEntity.STRIDE_MODE_AUTO, selected, Modifier.weight(1f), onSelected)
        StrideModeSegment("手动", UserSettingsEntity.STRIDE_MODE_MANUAL, selected, Modifier.weight(1f), onSelected)
    }
}

@Composable
private fun StrideModeSegment(
    label: String,
    value: String,
    selected: String,
    modifier: Modifier,
    onSelected: (String) -> Unit
) {
    Surface(
        modifier = modifier.fillMaxHeight().clickable { onSelected(value) },
        color = if (selected == value) StepGreen else Color.Transparent,
        contentColor = if (selected == value) Color.White else StepMuted
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, fontSize = 16.sp)
        }
    }
}

@Composable
private fun AvatarImage(uriString: String?, size: Dp, cornerRadius: Dp) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            loadImageBitmap(context, uriString)
        }
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(StepGreen.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Outlined.Person, null, tint = StepGreen, modifier = Modifier.size(size * 0.52f))
        }
    }
}

private fun loadImageBitmap(context: Context, uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            context.contentResolver.openInputStream(uri).use { input ->
                BitmapFactory.decodeStream(input)
            }
        }
        bitmap?.asImageBitmap()
    }.getOrNull()
}

private fun showBirthDatePicker(context: Context, current: String, onPicked: (String) -> Unit) {
    val parts = current.split("-").mapNotNull { it.toIntOrNull() }
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    val year = parts.getOrNull(0)?.coerceIn(currentYear - 100, currentYear - 10) ?: currentYear - 35
    val month = parts.getOrNull(1)?.coerceIn(1, 12) ?: 1
    val day = parts.getOrNull(2)?.coerceIn(1, 31) ?: 1
    DatePickerDialog(
        context,
        { _, pickedYear, pickedMonth, pickedDay ->
            onPicked(String.format(Locale.US, "%04d-%02d-%02d", pickedYear, pickedMonth + 1, pickedDay))
        },
        year,
        month - 1,
        day
    ).show()
}

private fun ageFromBirthDateForUi(birthDate: String): Int {
    val parts = birthDate.split("-").mapNotNull { it.toIntOrNull() }
    if (parts.size != 3) return 35
    val today = Calendar.getInstance()
    var age = today.get(Calendar.YEAR) - parts[0]
    val currentMonth = today.get(Calendar.MONTH) + 1
    val currentDay = today.get(Calendar.DAY_OF_MONTH)
    if (currentMonth < parts[1] || (currentMonth == parts[1] && currentDay < parts[2])) {
        age -= 1
    }
    return age.coerceIn(10, 100)
}

@Composable
private fun ScreenColumn(
    spacing: androidx.compose.ui.unit.Dp = 14.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    scrollResetKey: Any? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val scrollState = rememberScrollState()
    LaunchedEffect(scrollResetKey) {
        if (scrollResetKey != null) {
            scrollState.scrollTo(0)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content
    )
}

@Composable
private fun Header(title: String, subtitle: String) {
    Column {
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = StepMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun RealtimeChip(enabled: Boolean) {
    Surface(
        shape = CircleShape,
        color = if (enabled) StepGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        contentColor = if (enabled) StepGreen else StepMuted
    ) {
        Text(if (enabled) "实时中" else "普通", fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun StatusPill(text: String) {
    Surface(shape = CircleShape, color = StepGreen.copy(alpha = 0.12f), contentColor = StepGreen) {
        Text(text, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
    }
}

@Composable
private fun ProfileSummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        Text(label, color = StepMuted, fontSize = 11.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
private fun StepRing(
    steps: Long,
    progress: Float,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 190.dp,
    ringSize: androidx.compose.ui.unit.Dp = 168.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(ringSize)) {
            val stroke = 16.dp.toPx()
            drawArc(
                color = StepGreen.copy(alpha = 0.14f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = StepGreen,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(number(steps), fontSize = if (height < 160.dp) 34.sp else 38.sp, fontWeight = FontWeight.Bold)
            Text("步", color = StepMuted)
        }
    }
}

@Composable
private fun MetricCard(icon: ImageVector, label: String, value: String, tint: Color, modifier: Modifier = Modifier, compact: Boolean = false) {
    Card(
        modifier = modifier.heightIn(min = if (compact) 62.dp else 78.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(if (compact) 9.dp else 12.dp)) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(if (compact) 16.dp else 18.dp))
            Text(label, color = StepMuted, fontSize = 11.sp, modifier = Modifier.padding(top = if (compact) 3.dp else 6.dp))
            Text(value, fontWeight = FontWeight.SemiBold, fontSize = if (compact) 12.sp else 13.sp)
        }
    }
}

@Composable
private fun HourChart(samples: List<SensorSampleEntity>, totalSteps: Long, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 150.dp) {
    val bars = remember(samples, totalSteps) { hourlyBars(samples, totalSteps) }
    var selectedHour by remember(samples, totalSteps) { mutableStateOf<Int?>(null) }
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
    Column(modifier = modifier) {
        BarRow(
            bars = bars,
            highlightLast = false,
            highlightIndex = currentHour,
            selectedIndex = selectedHour,
            onBarClick = { hour -> selectedHour = if (selectedHour == hour) null else hour },
            modifier = Modifier.height(height)
        )
        val selected = selectedHour
        Text(
            text = if (selected != null) {
                "${hourRangeLabel(selected)} · ${number(bars[selected].roundToInt())} 步"
            } else {
                "点击柱子查看时间段步数"
            },
            color = StepMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun WeekChart(days: List<WeekDay>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        BarRow(bars = days.map { it.steps.toFloat() }, highlightLast = true, modifier = Modifier.height(170.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            days.forEach { day ->
                Text(day.label, color = StepMuted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BarRow(
    bars: List<Float>,
    highlightLast: Boolean,
    modifier: Modifier = Modifier,
    highlightIndex: Int? = if (highlightLast) bars.lastIndex else null,
    selectedIndex: Int? = null,
    onBarClick: ((Int) -> Unit)? = null
) {
    val maxValue = max(1f, bars.maxOrNull() ?: 0f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bars.forEachIndexed { index, value ->
            val fraction = (value / maxValue).coerceIn(0f, 1f)
            val color = when (index) {
                selectedIndex -> StepOrange
                highlightIndex -> StepOrange
                else -> StepGreen
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(enabled = onBarClick != null) { onBarClick?.invoke(index) },
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight((0.12f + 0.88f * fraction).coerceIn(0.08f, 1f))
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun NoticeCard(text: String, compact: Boolean = false) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = StepGreen.copy(alpha = 0.08f))) {
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, modifier = Modifier.padding(if (compact) 10.dp else 14.dp))
    }
}

@Composable
private fun SegmentedHeader(selected: StatsPeriod, onSelected: (StatsPeriod) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        StatsPeriod.entries.forEach { period ->
            Surface(
                shape = CircleShape,
                color = if (period == selected) MaterialTheme.colorScheme.background else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelected(period) }
            ) {
                Text(
                    period.label,
                    color = if (period == selected) StepGreen else StepMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 7.dp)
                )
            }
        }
    }
}

@Composable
private fun StatChartCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(subtitle, color = StepMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StepGreen.copy(alpha = 0.10f),
                    contentColor = StepGreen,
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.BarChart, null, modifier = Modifier.size(24.dp))
                    }
                }
            }
            content()
        }
    }
}

@Composable
private fun LabeledBarChart(
    bars: List<Float>,
    labels: List<String>,
    highlightIndex: Int,
    modifier: Modifier = Modifier,
    labelEveryBar: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(Modifier.fillMaxSize()) {
                val gridColor = StepMuted.copy(alpha = 0.18f)
                repeat(4) { index ->
                    val y = size.height * index / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
            }
            BarRow(
                bars = bars,
                highlightLast = false,
                highlightIndex = highlightIndex,
                modifier = Modifier.fillMaxSize().padding(top = 6.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    label,
                    color = StepMuted,
                    fontSize = if (labelEveryBar) 11.sp else 9.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, value: String, subtitle: String, tint: Color, modifier: Modifier = Modifier, compact: Boolean = false) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(if (compact) 12.dp else 14.dp)) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = if (compact) 18.sp else 20.sp)
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            Text(subtitle, color = StepMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun CompactWidgetRow(
    iconLabel: String,
    title: String,
    subtitle: String,
    tall: Boolean = false,
    preview: @Composable () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (tall) 98.dp else 64.dp)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WidgetTypeBadge(iconLabel)
            Column(modifier = Modifier.padding(start = 12.dp).width(104.dp), verticalArrangement = Arrangement.Center) {
                Text(iconLabel, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(top = 1.dp))
                if (tall) {
                    Text(subtitle, color = StepMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 1.dp))
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                preview()
            }
        }
    }
}

@Composable
private fun WidgetTypeBadge(label: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(StepGreen.copy(alpha = 0.10f)),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = StepGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CompactWidgetSmallPreview(steps: Long) {
    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.width(82.dp).height(48.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("今日", color = StepMuted, fontSize = 11.sp)
            Text(number(steps), color = StepGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

@Composable
private fun CompactWidgetProgressPreview(steps: Long, percent: Int) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.width(12.dp))
            LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.weight(1f), color = StepGreen)
            Spacer(Modifier.width(8.dp))
            Text("${percent.coerceIn(0, 999)}%", color = StepGreen, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CompactWidgetSquarePreview(steps: Long, percent: Int) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.width(122.dp).height(50.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text("步数", color = StepMuted, fontSize = 10.sp)
            Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = StepGreen)
        }
    }
}

@Composable
private fun CompactWidgetWidePreview(steps: Long, goal: Int, percent: Int) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(30.dp).clip(CircleShape).background(StepGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.DirectionsWalk, null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
            Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(start = 8.dp))
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(116.dp)) {
                Text("目标 ${number(goal)}", color = StepMuted, fontSize = 9.sp, maxLines = 1)
                LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth(), color = StepGreen)
            }
        }
    }
}

@Composable
private fun CompactWidgetWeekPreview(days: List<WeekDay>) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(58.dp)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val maxSteps = max(1L, days.maxOfOrNull { it.steps } ?: 0L)
            days.forEachIndexed { index, day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height((12 + 28 * (day.steps.toFloat() / maxSteps)).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (index == days.lastIndex) StepOrange else StepGreen)
                    )
                    Text(day.label.takeLast(1), color = StepMuted, fontSize = 9.sp)
                }
            }
        }
    }
}

@Composable
private fun CompactActivityPreview(steps: Long, distanceKm: Double, minutes: Int, calories: Int) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxWidth().height(78.dp)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Canvas(Modifier.size(46.dp)) {
                drawCircle(StepGreen.copy(alpha = 0.18f))
                drawCircle(StepBlue.copy(alpha = 0.18f), radius = size.minDimension * 0.34f)
                drawCircle(Color(0xFF9D6AE8).copy(alpha = 0.30f), radius = size.minDimension * 0.22f)
            }
            Column(modifier = Modifier.padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text("步  ${number(steps)}/10k", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
                Text("距  ${oneDecimal(distanceKm)} km", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
                Text("时  $minutes/90 分", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
                Text("卡  $calories/500", fontWeight = FontWeight.SemiBold, fontSize = 10.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun WidgetPreviewSmall(steps: Long) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("1×1 极简步数", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.background) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                    Text("今日", color = StepMuted, fontSize = 11.sp)
                    Text(number(steps), color = StepGreen, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewProgress(steps: Long, percent: Int) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("2×1 步数进度", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 16.dp)) {
                Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Spacer(Modifier.width(14.dp))
                LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.weight(1f), color = StepGreen)
                Spacer(Modifier.width(12.dp))
                Text("$percent%", color = StepGreen, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun WidgetPreviewSquare(steps: Long, goal: Int, percent: Int) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("2×2 步数卡片", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.Start) {
                    Text("步数", fontWeight = FontWeight.Bold)
                    Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    Text("/${number(goal)}", color = StepMuted)
                    LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.width(120.dp), color = StepGreen)
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewWide(steps: Long, goal: Int, percent: Int) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("4×1 步数横条", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(StepGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DirectionsWalk, null, tint = Color.White)
                }
                Text(number(steps), fontWeight = FontWeight.Bold, fontSize = 24.sp, modifier = Modifier.padding(start = 12.dp))
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text("目标 ${number(goal)}", color = StepMuted, fontSize = 12.sp)
                    LinearProgressIndicator(progress = (percent / 100f).coerceIn(0f, 1f), modifier = Modifier.width(150.dp), color = StepGreen)
                }
            }
        }
    }
}

@Composable
private fun WidgetPreviewWeek(days: List<WeekDay>) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("4×2 周趋势", fontWeight = FontWeight.SemiBold)
            WeekChart(days, Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun ActivityWidgetPreview(steps: Long, distanceKm: Double, minutes: Int, calories: Int) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("每日活动量 2×1 / 2×2 / 4×1 / 4×2", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 14.dp)) {
                Canvas(Modifier.size(72.dp)) {
                    drawCircle(StepGreen.copy(alpha = 0.18f))
                    drawCircle(StepBlue.copy(alpha = 0.18f), radius = size.minDimension * 0.36f)
                    drawCircle(Color(0xFFFF5EA3).copy(alpha = 0.26f), radius = size.minDimension * 0.25f)
                }
                Column(modifier = Modifier.padding(start = 18.dp)) {
                    Text("步  ${number(steps)}/10,000", fontWeight = FontWeight.Bold)
                    Text("距  ${oneDecimal(distanceKm)} km", fontWeight = FontWeight.Bold)
                    Text("时  $minutes/90 分钟", fontWeight = FontWeight.Bold)
                    Text("卡  $calories/500 千卡", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Divider(color = MaterialTheme.colorScheme.background)
            content()
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = StepMuted, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun ProfileSlider(
    label: String,
    value: Float,
    valueText: String,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChange: (Float) -> Unit,
    onFinished: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.SemiBold)
        Text(valueText, color = StepMuted, fontSize = 12.sp)
    }
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = range,
        steps = steps,
        onValueChangeFinished = onFinished
    )
}

@Composable
private fun FeatureLine(icon: ImageVector, title: String, subtitle: String) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = StepGreen)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = StepMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, title: String, subtitle: String, onClick: (() -> Unit)?) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = StepGreen)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = StepMuted, fontSize = 12.sp)
            }
            if (onClick != null) TextButton(onClick = onClick) { Text("检测") }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StepGreen)
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = StepMuted, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ProtectionLine(icon: ImageVector, title: String, value: String, good: Boolean) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = if (good) StepGreen else StepOrange, modifier = Modifier.size(22.dp))
        Text(title, modifier = Modifier.padding(start = 10.dp).weight(1f))
        StatusPill(value)
    }
}

@Composable
private fun ProtectionActionLine(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onClick: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = StepOrange, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = StepMuted, fontSize = 12.sp)
        }
        TextButton(onClick = onClick) {
            Text(action)
        }
    }
}

@Composable
private fun ThemeButton(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (selected) StepGreen.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background,
            contentColor = if (selected) StepGreen else MaterialTheme.colorScheme.onSurface
        )
    ) {
        Icon(icon, null, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(4.dp))
        Text(label)
    }
}

private enum class MainTab(val label: String, val icon: ImageVector) {
    Today("今日", Icons.Outlined.DirectionsWalk),
    Stats("统计", Icons.Outlined.BarChart),
    Widget("小组件", Icons.Outlined.Widgets),
    Me("我的", Icons.Outlined.Person)
}

private enum class StatsPeriod(val label: String) {
    Day("日"),
    Week("周"),
    Month("月")
}

private data class WeekDay(
    val date: String,
    val label: String,
    val steps: Long,
    val goal: Int
)

private data class MonthDay(
    val date: String,
    val label: String,
    val steps: Long,
    val goal: Int
)

private fun buildWeek(records: List<DailyStepsEntity>, defaultGoal: Int): List<WeekDay> {
    val byDate = records.associateBy { it.date }
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormat = SimpleDateFormat("E", Locale.CHINA)
    calendar.add(Calendar.DAY_OF_YEAR, -6)
    return (0 until 7).map {
        val date = dateFormat.format(calendar.time)
        val record = byDate[date]
        val day = WeekDay(
            date = date,
            label = labelFormat.format(calendar.time),
            steps = record?.dailySteps ?: 0,
            goal = record?.goalSteps ?: defaultGoal
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        day
    }
}

private fun buildMonth(records: List<DailyStepsEntity>, defaultGoal: Int): List<MonthDay> {
    val byDate = records.associateBy { it.date }
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val labelFormat = SimpleDateFormat("M/d", Locale.CHINA)
    calendar.add(Calendar.DAY_OF_YEAR, -29)
    return (0 until 30).map {
        val date = dateFormat.format(calendar.time)
        val record = byDate[date]
        val day = MonthDay(
            date = date,
            label = labelFormat.format(calendar.time),
            steps = record?.dailySteps ?: 0,
            goal = record?.goalSteps ?: defaultGoal
        )
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        day
    }
}

private fun hourlyBars(samples: List<SensorSampleEntity>, totalSteps: Long): List<Float> {
    val bars = MutableList(24) { 0f }
    val calendar = Calendar.getInstance()
    samples.forEach { sample ->
        calendar.timeInMillis = sample.timestamp
        val hour = calendar.get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        bars[hour] += sample.deltaSteps.toFloat()
    }
    if (bars.all { it == 0f } && totalSteps > 0) {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY).coerceIn(0, 23)
        bars[hour] = totalSteps.toFloat()
    }
    return bars
}

private fun hourRangeLabel(hour: Int): String =
    String.format(Locale.getDefault(), "%02d:00-%02d:00", hour.coerceIn(0, 23), (hour + 1).coerceAtMost(24))

private fun genderLabel(gender: String): String = when (gender) {
    UserSettingsEntity.GENDER_MALE -> "男性"
    UserSettingsEntity.GENDER_FEMALE -> "女性"
    else -> "未设置"
}

private fun strideModeLabel(mode: String): String = when (mode) {
    UserSettingsEntity.STRIDE_MODE_MANUAL -> "手动校准"
    else -> "自动估算"
}

private fun number(value: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

private fun number(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

private fun oneDecimal(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

private fun twoDecimal(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

private fun todayLabel(): String = SimpleDateFormat("M月d日 E", Locale.CHINA).format(Date())

private fun timeLabel(timestamp: Long?): String =
    if (timestamp == null || timestamp == 0L) "--:--" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
