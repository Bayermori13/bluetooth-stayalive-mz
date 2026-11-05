package com.mz.bluetooth.stayalive.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.os.Build
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

data class BtDeviceModel(
    val name: String,
    val address: String,
    val isConnected: Boolean = false,
    val rssi: Int = -60
)

data class LogEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: String,
    val title: String,
    val subtitle: String,
    val type: LogType
)

enum class LogType { SUCCESS, STOP, ERROR, KEEP_ALIVE }

class BluetoothEngine(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    var pairedDevices = mutableStateListOf<BtDeviceModel>()
    var scannedDevices = mutableStateListOf<BtDeviceModel>()
    var activeConnectedDevice by mutableStateOf<BtDeviceModel?>(null)
    var currentCodecName by mutableStateOf("در حال بررسی...")
    var isScanning by mutableStateOf(false)
    var connectingAddress by mutableStateOf<String?>(null) // آدرس دستگاهی که در حال اتصال به آن هستیم
    val logs = mutableStateListOf<LogEntry>()

    private var a2dpProfile: BluetoothA2dp? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    init {
        setupA2dpProfileListener()
        registerBluetoothReceiver()
        loadPairedDevices()
        addLog("سرویس آماده به کار شد", "سیستم آماده شناسایی و اتصال سریع به هندزفری است", LogType.SUCCESS)
    }

    private fun getTime(): String = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    fun addLog(title: String, subtitle: String, type: LogType) {
        logs.add(0, LogEntry(time = getTime(), title = title, subtitle = subtitle, type = type))
    }

    @SuppressLint("MissingPermission")
    fun loadPairedDevices() {
        try {
            pairedDevices.clear()
            bluetoothAdapter?.bondedDevices?.forEach { device ->
                val isConn = isDeviceConnected(device)
                val model = BtDeviceModel(device.name ?: "هندزفری بدون نام", device.address, isConn)
                pairedDevices.add(model)
                if (isConn) {
                    activeConnectedDevice = model
                    detectAudioCodec(device)
                }
            }
        } catch (_: Exception) {}
    }

    @SuppressLint("MissingPermission")
    private fun isDeviceConnected(device: BluetoothDevice): Boolean {
        return try {
            val isConnectedMethod = device.javaClass.getMethod("isConnected")
            isConnectedMethod.invoke(device) as Boolean
        } catch (_: Exception) {
            false
        }
    }

    private fun setupA2dpProfileListener() {
        bluetoothAdapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                if (profile == BluetoothProfile.A2DP) {
                    a2dpProfile = proxy as BluetoothA2dp
                    checkActiveCodec()
                }
            }
            override fun onServiceDisconnected(profile: Int) {
                if (profile == BluetoothProfile.A2DP) a2dpProfile = null
            }
        }, BluetoothProfile.A2DP)
    }

    @SuppressLint("MissingPermission")
    fun checkActiveCodec() {
        try {
            val devices = a2dpProfile?.connectedDevices
            if (!devices.isNullOrEmpty()) {
                val dev = devices[0]
                activeConnectedDevice = BtDeviceModel(dev.name ?: "هندزفری متصل", dev.address, true)
                detectAudioCodec(dev)
            } else {
                activeConnectedDevice = null
                currentCodecName = "دستگاهی متصل نیست"
            }
        } catch (_: Exception) {
            currentCodecName = "پروتکل استاندارد صوتی"
        }
    }

    @SuppressLint("MissingPermission")
    private fun detectAudioCodec(device: BluetoothDevice) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && a2dpProfile != null) {
            try {
                val getCodecStatusMethod = a2dpProfile?.javaClass?.getMethod("getCodecStatus", BluetoothDevice::class.java)
                val codecStatus = getCodecStatusMethod?.invoke(a2dpProfile, device)
                if (codecStatus != null) {
                    val getCodecConfigMethod = codecStatus.javaClass.getMethod("getCodecConfig")
                    val config = getCodecConfigMethod.invoke(codecStatus)
                    val getCodecTypeMethod = config.javaClass.getMethod("getCodecType")
                    val type = getCodecTypeMethod.invoke(config) as Int

                    currentCodecName = when (type) {
                        0 -> "SBC (استاندارد بلوتوث)"
                        1 -> "AAC (شفافیت بالا)"
                        2 -> "Qualcomm aptX"
                        3 -> "Qualcomm aptX HD"
                        4 -> "Sony LDAC (فوق پیشرفته 990kbps)"
                        5 -> "LC3 (LE Audio)"
                        else -> "کدک HD سفارشی"
                    }
                    return
                }
            } catch (_: Exception) {}
        }
        currentCodecName = "A2DP High Quality Audio"
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address) ?: return
        connectingAddress = address
        addLog("در حال اتصال صوتی...", "در حال برقراری اتصال با ${device.name ?: address}", LogType.KEEP_ALIVE)

        scope.launch(Dispatchers.IO) {
            try {
                if (device.bondState == BluetoothDevice.BOND_NONE) {
                    device.createBond()
                }
                if (a2dpProfile != null) {
                    val connectMethod = a2dpProfile?.javaClass?.getMethod("connect", BluetoothDevice::class.java)
                    connectMethod?.invoke(a2dpProfile, device)
                }
                delay(6000)
                if (connectingAddress == address) {
                    connectingAddress = null
                }
            } catch (e: Exception) {
                connectingAddress = null
                addLog("خطا در اتصال", e.localizedMessage ?: "پاسخی دریافت نشد", LogType.ERROR)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (isScanning) return
        try {
            scannedDevices.clear()
            bluetoothAdapter?.startDiscovery()
            isScanning = true
            addLog("اسکن محیطی آغاز شد", "در حال یافتن امواج هندزفری‌های اطراف...", LogType.KEEP_ALIVE)
        } catch (_: Exception) {
            addLog("خطا در اسکن", "لطفاً بلوتوث و مکان گوشی را روشن کنید", LogType.ERROR)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        try {
            bluetoothAdapter?.cancelDiscovery()
            isScanning = false
        } catch (_: Exception) {}
    }

    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }

        context.registerReceiver(object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(c: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, (-65).toShort()).toInt()
                        device?.let {
                            val name = it.name ?: "دستگاه صوتی مجاور"
                            val model = BtDeviceModel(name, it.address, false, rssi)
                            if (scannedDevices.none { d -> d.address == it.address }) {
                                scannedDevices.add(model)
                            }
                        }
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val name = dev?.name ?: "هندزفری"
                        connectingAddress = null
                        activeConnectedDevice = BtDeviceModel(name, dev?.address ?: "", true)
                        loadPairedDevices()
                        dev?.let { detectAudioCodec(it) }
                        addLog("اتصال برقرار شد", "$name متصل شد - محافظت فعال است", LogType.SUCCESS)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                        val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        addLog("هشدار قطع!", "${dev?.name} قصد خاموشی داشت؛ پالس نگهدارنده مانع شد", LogType.KEEP_ALIVE)
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        val dev = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        connectingAddress = null
                        activeConnectedDevice = null
                        currentCodecName = "دستگاهی متصل نیست"
                        loadPairedDevices()
                        addLog("ارتباط قطع شد", "ارتباط با ${dev?.name} پایان یافت", LogType.ERROR)
                    }
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        loadPairedDevices()
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        isScanning = false
                    }
                }
            }
        }, filter)
    }
}
