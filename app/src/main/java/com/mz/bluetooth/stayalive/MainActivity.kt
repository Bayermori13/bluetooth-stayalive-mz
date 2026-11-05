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
    private var isServiceActive by mutableStateOf(false)
    private var signalMode by mutableStateOf(1)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        bluetoothEngine.loadPairedDevices()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bluetoothEngine = BluetoothEngine(this)
        requestRequiredPermissions()

        setContent {
            MZBluetoothTheme {
                MainScreen(
                    engine = bluetoothEngine,
                    isServiceRunning = isServiceActive,
                    onToggleService = { toggleKeepAliveService() },
                    currentMode = signalMode,
                    onSelectMode = { mode ->
                        signalMode = mode
                        if (isServiceActive) startServiceWithMode(mode)
                    }
                )
            }
        }
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

    private fun toggleKeepAliveService() {
        if (isServiceActive) {
            val stopIntent = Intent(this, KeepAliveService::class.java).apply {
                action = KeepAliveService.ACTION_STOP
            }
            startService(stopIntent)
            isServiceActive = false
            bluetoothEngine.addLog("سرویس متوقف شد", "سیگنال نگه‌دارنده توسط کاربر خاموش شد", LogType.STOP)
        } else {
            startServiceWithMode(signalMode)
            isServiceActive = true
            bluetoothEngine.addLog("سرویس فعال شد", "نگه‌دارنده دائمی با موفقیت استارت خورد", LogType.SUCCESS)
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
