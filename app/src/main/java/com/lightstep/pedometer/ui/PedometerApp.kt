package com.lightstep.pedometer.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lightstep.pedometer.PedometerUiState
import com.lightstep.pedometer.data.DailyStepsEntity
import com.lightstep.pedometer.data.SensorSampleEntity
import com.lightstep.pedometer.ui.theme.StepBlue
import com.lightstep.pedometer.ui.theme.StepGreen
import com.lightstep.pedometer.ui.theme.StepMuted
import com.lightstep.pedometer.ui.theme.StepOrange
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun PedometerApp(
    state: PedometerUiState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onSetGoal: (Int) -> Unit,
    onSetTheme: (String) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenBackgroundActivity: () -> Unit,
    onClearMessage: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.Today) }

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
            if (state.permissionGranted && state.sensorAvailable) {
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
                selectedTab == MainTab.Today -> TodayScreen(state, onRefresh, onToggleRealtime)
                selectedTab == MainTab.Stats -> StatsScreen(state)
                selectedTab == MainTab.Widget -> WidgetScreen(state, onRefresh)
                selectedTab == MainTab.Me -> SettingsScreen(
                    state = state,
                    onSetGoal = onSetGoal,
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

    ScreenColumn {
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
        NoticeCard("第一版只统计手机传感器步数，手机没带在身上时不会记录人体实际步数。")
    }
}

@Composable
private fun DayStatsContent(state: PedometerUiState) {
    val today = state.today
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("今日时段", fontWeight = FontWeight.SemiBold)
            Text("总步数 ${number(today?.dailySteps ?: 0)} · 更新 ${timeLabel(today?.updatedAt)}", color = StepMuted, fontSize = 12.sp)
            HourChart(samples = state.samples, totalSteps = today?.dailySteps ?: 0, modifier = Modifier.padding(top = 14.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("距离", "${oneDecimal((today?.distanceMeters ?: 0.0) / 1000.0)} km", "估算", StepBlue, Modifier.weight(1f))
        SummaryCard("热量", "${(today?.calories ?: 0.0).roundToInt()} kcal", "估算", StepOrange, Modifier.weight(1f))
        SummaryCard("活跃", "${today?.activeMinutes ?: 0} 分钟", "今日", StepGreen, Modifier.weight(1f))
    }
}

@Composable
private fun WeekStatsContent(week: List<WeekDay>) {
    val total = week.sumOf { it.steps }
    val average = if (week.isNotEmpty()) total / week.size else 0
    val best = week.maxOfOrNull { it.steps } ?: 0
    val reached = week.count { it.steps >= it.goal }
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("本周趋势", fontWeight = FontWeight.SemiBold)
            Text("总步数 ${number(total)} · 平均 ${number(average)}", color = StepMuted, fontSize = 12.sp)
            WeekChart(week, Modifier.padding(top = 18.dp))
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("达标天数", "$reached 天", "手机随身统计", StepGreen, Modifier.weight(1f))
        SummaryCard("最高纪录", number(best), "近 7 天", StepOrange, Modifier.weight(1f))
        SummaryCard("连续记录", "${week.count { it.steps > 0 }} 天", "近 7 天", StepBlue, Modifier.weight(1f))
    }
}

@Composable
private fun MonthStatsContent(month: List<MonthDay>) {
    val total = month.sumOf { it.steps }
    val average = if (month.isNotEmpty()) total / month.size else 0
    val best = month.maxOfOrNull { it.steps } ?: 0
    val reached = month.count { it.steps >= it.goal }
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text("30 天趋势", fontWeight = FontWeight.SemiBold)
            Text("总步数 ${number(total)} · 平均 ${number(average)}", color = StepMuted, fontSize = 12.sp)
            BarRow(
                bars = month.map { it.steps.toFloat() },
                highlightLast = true,
                modifier = Modifier
                    .padding(top = 18.dp)
                    .height(170.dp)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                month.filterIndexed { index, _ -> index % 5 == 0 || index == month.lastIndex }.forEach {
                    Text(it.label, color = StepMuted, fontSize = 10.sp)
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SummaryCard("达标天数", "$reached 天", "最近 30 天", StepGreen, Modifier.weight(1f))
        SummaryCard("最高纪录", number(best), "最近 30 天", StepOrange, Modifier.weight(1f))
        SummaryCard("平均步数", number(average), "每日", StepBlue, Modifier.weight(1f))
    }
}

@Composable
private fun WidgetScreen(state: PedometerUiState, onRefresh: () -> Unit) {
    val steps = state.today?.dailySteps ?: 0
    val goal = state.today?.goalSteps ?: state.settings.dailyGoalSteps
    val percent = ((steps * 100.0) / goal.coerceAtLeast(1)).roundToInt()

    ScreenColumn {
        Header(title = "小组件", subtitle = "桌面快速查看步数")
        WidgetPreviewSmall(steps)
        WidgetPreviewProgress(steps, percent)
        WidgetPreviewSquare(steps, goal, percent)
        WidgetPreviewWide(steps, goal, percent)
        WidgetPreviewWeek(buildWeek(state.week, goal))
        ActivityWidgetPreview(
            steps = steps,
            distanceKm = (state.today?.distanceMeters ?: 0.0) / 1000.0,
            minutes = state.today?.activeMinutes ?: 0,
            calories = (state.today?.calories ?: 0.0).roundToInt()
        )
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
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
    onSetTheme: (String) -> Unit,
    onToggleRealtime: (Boolean) -> Unit,
    onOpenBatteryOptimization: () -> Unit,
    onOpenBackgroundActivity: () -> Unit
) {
    var goalValue by remember(state.settings.dailyGoalSteps) { mutableFloatStateOf(state.settings.dailyGoalSteps.toFloat()) }

    ScreenColumn {
        Header(title = "我的", subtitle = "本地数据 · 无账号")
        ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(StepGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.DirectionsWalk, null, tint = Color.White)
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text("轻步 Pedometer", fontWeight = FontWeight.Bold)
                    Text("数据保存在本机 · 不上传", color = StepMuted, fontSize = 12.sp)
                }
                Spacer(Modifier.weight(1f))
                StatusPill(if (state.sensorAvailable) "正常" else "异常")
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
    }
}

@Composable
private fun ScreenColumn(
    spacing: androidx.compose.ui.unit.Dp = 14.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    verticalPadding: androidx.compose.ui.unit.Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
    BarRow(bars = bars, highlightLast = true, modifier = modifier.height(height))
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
private fun BarRow(bars: List<Float>, highlightLast: Boolean, modifier: Modifier = Modifier) {
    val maxValue = max(1f, bars.maxOrNull() ?: 0f)
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        bars.forEachIndexed { index, value ->
            val fraction = (value / maxValue).coerceIn(0f, 1f)
            val color = if (highlightLast && index == bars.lastIndex) StepOrange else StepGreen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(138.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((18 + 120 * fraction).dp)
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
private fun SummaryCard(title: String, value: String, subtitle: String, tint: Color, modifier: Modifier = Modifier) {
    ElevatedCard(shape = RoundedCornerShape(8.dp), modifier = modifier) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            Text(subtitle, color = StepMuted, fontSize = 10.sp)
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

private fun number(value: Long): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

private fun number(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

private fun oneDecimal(value: Double): String = String.format(Locale.getDefault(), "%.2f", value)

private fun todayLabel(): String = SimpleDateFormat("M月d日 E", Locale.CHINA).format(Date())

private fun timeLabel(timestamp: Long?): String =
    if (timestamp == null || timestamp == 0L) "--:--" else SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
