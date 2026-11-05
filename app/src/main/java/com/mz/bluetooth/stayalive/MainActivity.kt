package com.mz.bluetooth.stayalive

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.mz.bluetooth.stayalive.bluetooth.BluetoothEngine
import com.mz.bluetooth.stayalive.bluetooth.LogType
import com.mz.bluetooth.stayalive.service.KeepAliveService
import com.mz.bluetooth.stayalive.ui.MZBluetoothTheme
import com.mz.bluetooth.stayalive.ui.MainScreen

class MainActivity : ComponentActivity() {

    private lateinit var bluetoothEngine: BluetoothEngine

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothEngine.loadPairedDevices()
        bluetoothEngine.checkActiveCodec()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothEngine = BluetoothEngine(this)
        requestRequiredPermissions()

        setContent {
            // خواندن همگام و زنده وضعیت سرویس از پس‌زمینه
            val isRunning by KeepAliveService.isRunningState.collectAsState()
            val signalMode by KeepAliveService.signalModeState.collectAsState()
            val elapsedSeconds by KeepAliveService.elapsedSecondsState.collectAsState()

            MZBluetoothTheme {
                MainScreen(
                    engine = bluetoothEngine,
                    isServiceRunning = isRunning,
                    runningSeconds = elapsedSeconds,
                    onToggleService = { toggleKeepAliveService(isRunning, signalMode) },
                    currentMode = signalMode,
                    onSelectMode = { mode ->
                        KeepAliveService.signalModeState.value = mode
                        if (isRunning) startServiceWithMode(mode)
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothEngine.loadPairedDevices()
        bluetoothEngine.checkActiveCodec()
    }

    private fun requestRequiredPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun toggleKeepAliveService(isRunning: Boolean, mode: Int) {
        if (isRunning) {
            val stopIntent = Intent(this, KeepAliveService::class.java).apply {
                action = KeepAliveService.ACTION_STOP
            }
            startService(stopIntent)
            bluetoothEngine.addLog("سرویس خاموش شد", "نگه‌دارنده فرکانسی توسط کاربر متوقف گردید", LogType.STOP)
        } else {
            startServiceWithMode(mode)
            bluetoothEngine.addLog("سرویس روشن شد", "ارسال جریان ممتد برای بیدار نگه داشتن هندزفری آغاز شد", LogType.SUCCESS)
        }
    }

    private fun startServiceWithMode(mode: Int) {
        val startIntent = Intent(this, KeepAliveService::class.java).apply {
            action = KeepAliveService.ACTION_START
            putExtra(KeepAliveService.EXTRA_MODE, mode)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(startIntent)
        } else {
            startService(startIntent)
        }
    }
}
