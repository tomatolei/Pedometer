package com.lightstep.pedometer

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lightstep.pedometer.ui.PedometerApp
import com.lightstep.pedometer.ui.theme.LightStepTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PedometerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val darkBars = when (uiState.settings.themeMode) {
                "dark" -> true
                "light" -> false
                else -> systemDark
            }
            var pendingRealtimeEnable by remember { mutableStateOf(false) }

            SideEffect {
                configureSystemBars(darkBars)
            }

            val activityPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                viewModel.onPermissionResult(granted)
            }

            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (granted && pendingRealtimeEnable) {
                    viewModel.setRealtime(context, true)
                }
                pendingRealtimeEnable = false
            }

            LightStepTheme(themeMode = uiState.settings.themeMode) {
                PedometerApp(
                    state = uiState,
                    onRequestPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            viewModel.onPermissionResult(true)
                        }
                    },
                    onRefresh = { viewModel.refreshFromSensor(showMessage = true) },
                    onSetGoal = viewModel::setGoal,
                    onUpdateProfile = viewModel::updateProfile,
                    onCalibrateStride = viewModel::calibrateStride,
                    onSetTheme = viewModel::setTheme,
                    onToggleRealtime = { enabled ->
                        if (enabled &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingRealtimeEnable = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.setRealtime(context, enabled)
                        }
                    },
                    onOpenBatteryOptimization = ::openBatteryOptimizationSettings,
                    onOpenBackgroundActivity = ::openAppDetailsSettings,
                    onClearMessage = viewModel::clearMessage
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (viewModel.uiState.value.permissionGranted) {
            viewModel.refreshFromSensor(showMessage = false)
        }
    }

    private fun configureSystemBars(darkBars: Boolean) {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkBars
            isAppearanceLightNavigationBars = !darkBars
        }
    }

    private fun openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            openAppDetailsSettings()
            return
        }

        val powerManager = getSystemService(PowerManager::class.java)
        val packageUri = Uri.parse("package:$packageName")
        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri)
        val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        try {
            if (powerManager?.isIgnoringBatteryOptimizations(packageName) == true) {
                startActivity(fallbackIntent)
            } else {
                startActivity(requestIntent)
            }
        } catch (_: ActivityNotFoundException) {
            openAppDetailsSettings()
        } catch (_: SecurityException) {
            openAppDetailsSettings()
        }
    }

    private fun openAppDetailsSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }
}
