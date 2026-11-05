package com.mz.bluetooth.stayalive.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mz.bluetooth.stayalive.bluetooth.BluetoothEngine
import com.mz.bluetooth.stayalive.bluetooth.BtDeviceModel
import com.mz.bluetooth.stayalive.bluetooth.LogType
import kotlin.math.sin

@Composable
fun MainScreen(
    engine: BluetoothEngine,
    isServiceRunning: Boolean,
    runningSeconds: Long,
    onToggleService: () -> Unit,
    currentMode: Int,
    onSelectMode: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isPersian by remember { mutableStateOf(true) }

    CompositionLocalProvider(LocalLayoutDirection provides (if (isPersian) LayoutDirection.Rtl else LayoutDirection.Ltr)) {
        Scaffold(
            containerColor = BgDark,
            bottomBar = {
                NavigationBar(containerColor = CardDark) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                        label = { Text(if (isPersian) "کنترلر" else "Controller", fontSize = 11.sp, color = TextWhite) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = null) },
                        label = { Text(if (isPersian) "هندزفری" else "Headset", fontSize = 11.sp, color = TextWhite) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text(if (isPersian) "دستگاه‌ها و اسکن" else "Devices & Scan", fontSize = 11.sp, color = TextWhite) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                        label = { Text(if (isPersian) "لاگ‌ها" else "Logs", fontSize = 11.sp, color = TextWhite) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TopHeaderBar(isPersian = isPersian, onToggleLanguage = { isPersian = !isPersian })

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedTab) {
                    0 -> ControllerTab(isPersian, isServiceRunning, runningSeconds, onToggleService, currentMode, onSelectMode)
                    1 -> HeadsetTab(isPersian, engine)
                    2 -> ScannerTab(isPersian, engine)
                    3 -> LogsTab(isPersian, engine)
                }
            }
        }
    }
}

@Composable
fun TopHeaderBar(isPersian: Boolean, onToggleLanguage: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF13223E))
                    .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("MZ", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isPersian) "نگه‌دارنده بلوتوث MZ" else "MZ Bluetooth StayAlive",
                color = TextWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .clickable { onToggleLanguage() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                if (isPersian) "🌐 English" else "🌐 فارسی",
                color = TextWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ---------------------- تب اول: کنترلر و ویژوالایزر امواج ----------------------
@Composable
fun ControllerTab(
    isPersian: Boolean,
    isServiceRunning: Boolean,
    seconds: Long,
    onToggle: () -> Unit,
    currentMode: Int,
    onSelectMode: (Int) -> Unit
) {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val timeFormatted = String.format("%02d:%02d:%02d", hrs, mins, secs)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScale by animateFloatAsState(if (isPressed) 0.92f else 1f, label = "buttonScale")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isServiceRunning) NeonCyan else Color.Gray)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    if (isServiceRunning) (if (isPersian) "سرویس فعال است" else "Service Active")
                    else (if (isPersian) "سرویس متوقف است" else "Service Stopped"),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Text(
                "${if (isPersian) "مدت کارکرد" else "Uptime"}: $timeFormatted",
                color = NeonCyan,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // دکمه بزرگ دایره‌ای با افکت لمسی
        Box(
            modifier = Modifier
                .scale(buttonScale)
                .size(165.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isServiceRunning) NeonCyan.copy(alpha = 0.35f) else Color.DarkGray.copy(alpha = 0.15f),
                            CardDark
                        )
                    )
                )
                .border(2.5.dp, if (isServiceRunning) NeonCyan else CardBorder, CircleShape)
                .clickable(interactionSource = interactionSource, indication = null) { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isServiceRunning) NeonCyan else TextWhite,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isServiceRunning) (if (isPersian) "توقف" else "Stop")
                    else (if (isPersian) "شروع" else "Start"),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ویژوالایزر زنده نمایش فرکانس در حال پخش
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (isPersian) "رادار و اسیلوسکوپ زنده فرکانس" else "Live Frequency Visualizer", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        if (isServiceRunning) (if (isPersian) "سیگنال برخط" else "Active Stream") else "Idle",
                        color = if (isServiceRunning) NeonTeal else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                LiveWaveVisualizer(isRunning = isServiceRunning, mode = currentMode)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            if (isPersian) "نوع سیگنال نگه‌دارنده" else "Keep-Alive Signal Mode",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        SignalModeItem(
            if (isPersian) "سکوت دیجیتال (PCM 0dB)" else "Absolute Silence (0dB)",
            if (isPersian) "پیش‌فرض" else "Default",
            currentMode == 0
        ) { onSelectMode(0) }

        Spacer(modifier = Modifier.height(8.dp))

        SignalModeItem(
            if (isPersian) "فرکانس نامحسوس (۱۵ هرتز)" else "Inaudible Tone (15 Hz)",
            if (isPersian) "تضمینی ضد خاموشی" else "Best Protection",
            currentMode == 1
        ) { onSelectMode(1) }

        Spacer(modifier = Modifier.height(8.dp))

        SignalModeItem(
            if (isPersian) "پالس هوشمند صوتی" else "Smart Pulse Wave",
            if (isPersian) "فوق کم‌مصرف" else "Battery Saver",
            currentMode == 2
        ) { onSelectMode(2) }
    }
}

// کامپوننت رسم موج و نوسان زنده فرکانس صوتی
@Composable
fun LiveWaveVisualizer(isRunning: Boolean, mode: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveAnim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF09101C))
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        if (!isRunning || mode == 0) {
            // خط مستقیم سکوت دیجیتال
            drawLine(
                color = if (isRunning) NeonCyan.copy(alpha = 0.7f) else Color.DarkGray,
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 2.dp.toPx()
            )
        } else if (mode == 1) {
            // موج سینوسی زنده ۱۵ هرتز
            val path = Path()
            val points = 80
            for (i in 0..points) {
                val x = (width / points) * i
                val rad = Math.toRadians((i * 18.0 + phase))
                val y = centerY + sin(rad).toFloat() * 18.dp.toPx()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, color = NeonCyan, style = Stroke(width = 2.5.dp.toPx()))
        } else {
            // پالس ضربانی هوشمند
            val path = Path()
            path.moveTo(0f, centerY)
            val pulseX = (width * ((phase % 100) / 100f))
            path.lineTo((pulseX - 30).coerceAtLeast(0f), centerY)
            path.lineTo(pulseX - 15, centerY - 20.dp.toPx())
            path.lineTo(pulseX, centerY + 20.dp.toPx())
            path.lineTo(pulseX + 15, centerY)
            path.lineTo(width, centerY)
            drawPath(path, color = NeonTeal, style = Stroke(width = 2.5.dp.toPx()))
        }
    }
}

@Composable
fun SignalModeItem(title: String, badge: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "cardScale")

    Card(
        modifier = Modifier
            .scale(scale)
            .fillMaxWidth()
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF13223E) else CardDark),
        border = BorderStroke(1.dp, if (isSelected) NeonCyan else CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) NeonCyan else TextWhite
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CardBorder)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(badge, color = TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---------------------- تب دوم: هندزفری ----------------------
@Composable
fun HeadsetTab(isPersian: Boolean, engine: BluetoothEngine) {
    val dev = engine.activeConnectedDevice

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF15223A)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Headphones,
                        contentDescription = null,
                        tint = if (dev != null) NeonCyan else TextWhite,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = dev?.name ?: (if (isPersian) "هیچ هندزفری متصل نیست" else "No Headset Connected"),
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (dev != null) "MAC: ${dev.address}"
                    else (if (isPersian) "هندزفری خود را از طریق بلوتوث گوشی متصل کنید." else "Connect your bluetooth headset to begin."),
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13223E))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "${if (isPersian) "کدک صوتی اتصال" else "Audio Codec"}: ${engine.currentCodecName}",
                        color = TextWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { engine.checkActiveCodec() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isPersian) "بررسی مجدد اتصال" else "Check Connection", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------- تب سوم: اسکن، تاریخچه و اتصال با لودینگ ----------------------
@Composable
fun ScannerTab(isPersian: Boolean, engine: BluetoothEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    if (isPersian) "جستجو و اتصال مستقیم به بلوتوث" else "Search & Direct Bluetooth Connect",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isPersian) "اتصال مستقیم به هندزفری از داخل خود اپلیکیشن" else "Pair and connect directly from inside the app",
                    color = TextWhite.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { if (engine.isScanning) engine.stopScan() else engine.startScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (engine.isScanning) (if (isPersian) "در حال اسکن امواج اطراف..." else "Scanning nearby...")
                        else (if (isPersian) "اسکن دستگاه جدید" else "Scan New Device"),
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (engine.scannedDevices.isNotEmpty()) {
            Text(
                if (isPersian) "امواج یافت‌شده مجاور" else "Nearby Devices Found",
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(engine.scannedDevices) { dev ->
                    DeviceCardItem(
                        dev = dev,
                        isConnecting = engine.connectingAddress == dev.address,
                        isPersian = isPersian,
                        onConnect = { engine.connectToDevice(dev.address) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(if (isPersian) "دستگاه‌های ذخیره‌شده (تاریخچه)" else "Paired Devices History", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${engine.pairedDevices.size}", color = NeonCyan, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(engine.pairedDevices) { dev ->
                DeviceCardItem(
                    dev = dev,
                    isConnecting = engine.connectingAddress == dev.address,
                    isPersian = isPersian,
                    onConnect = { engine.connectToDevice(dev.address) }
                )
            }
        }
    }
}

@Composable
fun DeviceCardItem(dev: BtDeviceModel, isConnecting: Boolean, isPersian: Boolean, onConnect: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, if (dev.isConnected) NeonCyan else CardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dev.name, color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text("MAC: ${dev.address} | سیگنال: ${dev.rssi}dBm", color = TextWhite.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            if (dev.isConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D2924))
                        .border(1.dp, NeonTeal, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(if (isPersian) "متصل" else "Connected", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            } else if (isConnecting) {
                // لودینگ چرخشی موقع اتصال
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = NeonCyan,
                        strokeWidth = 2.5.dp
                    )
                }
            } else {
                Button(
                    onClick = onConnect,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF13223E)),
                    border = BorderStroke(1.dp, NeonCyan),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(if (isPersian) "اتصال" else "Connect", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------- تب چهارم: لاگ‌ها ----------------------
@Composable
fun LogsTab(isPersian: Boolean, engine: BluetoothEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(if (isPersian) "لاگ رویدادها" else "Event Logs", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13223E))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("${engine.logs.size}", color = NeonCyan, fontSize = 11.sp)
                }
            }
            IconButton(onClick = { engine.logs.clear() }) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = TextWhite)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn {
            items(engine.logs) { log ->
                val borderColor = when (log.type) {
                    LogType.SUCCESS -> NeonCyan
                    LogType.ERROR -> NeonRed
                    LogType.KEEP_ALIVE -> NeonTeal
                    LogType.STOP -> Color.Gray
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, borderColor.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(log.title, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(log.subtitle, color = TextWhite.copy(alpha = 0.75f), fontSize = 11.sp)
                        }
                        Text(log.time, color = TextWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
