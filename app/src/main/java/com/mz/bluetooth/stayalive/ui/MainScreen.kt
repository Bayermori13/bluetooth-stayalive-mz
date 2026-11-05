package com.mz.bluetooth.stayalive.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mz.bluetooth.stayalive.bluetooth.BluetoothEngine
import com.mz.bluetooth.stayalive.bluetooth.LogType
import kotlinx.coroutines.delay

@Composable
fun MainScreen(
    engine: BluetoothEngine,
    isServiceRunning: Boolean,
    onToggleService: () -> Unit,
    currentMode: Int,
    onSelectMode: (Int) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var runningSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(isServiceRunning) {
        if (isServiceRunning) {
            while (true) {
                delay(1000)
                runningSeconds++
            }
        } else {
            runningSeconds = 0
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            containerColor = BgDark,
            bottomBar = {
                NavigationBar(containerColor = CardDark) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.VolumeUp, contentDescription = null) },
                        label = { Text("کنترلر", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Headphones, contentDescription = null) },
                        label = { Text("هندزفری", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.History, contentDescription = null) },
                        label = { Text("دستگاه‌ها و اسکن", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = NeonCyan, selectedTextColor = NeonCyan, indicatorColor = CardBorder)
                    )
                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                        label = { Text("لاگ‌ها", fontSize = 11.sp) },
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
                // هدر بالای صفحه با لوگوی MZ
                TopHeaderBar()

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> ControllerTab(isServiceRunning, runningSeconds, onToggleService, currentMode, onSelectMode)
                    1 -> HeadsetTab(engine)
                    2 -> ScannerTab(engine)
                    3 -> LogsTab(engine)
                }
            }
        }
    }
}

@Composable
fun TopHeaderBar() {
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
            Text("نگه‌دارنده بلوتوث MZ", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text("EN 🌐", color = TextMuted, fontSize = 12.sp)
        }
    }
}

// ---------------------- تب اول: کنترلر ----------------------
@Composable
fun ControllerTab(
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // نوار وضعیت سرویس
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
                    if (isServiceRunning) "سرویس فعال است" else "سرویس متوقف است",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Text("مدت کارکرد : $timeFormatted", color = NeonCyan, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // دکمه بزرگ دایره‌ای روشن/خاموش
        Box(
            modifier = Modifier
                .size(170.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            if (isServiceRunning) NeonCyan.copy(alpha = 0.25f) else Color.DarkGray.copy(alpha = 0.2f),
                            CardDark
                        )
                    )
                )
                .border(2.5.dp, if (isServiceRunning) NeonCyan else CardBorder, CircleShape)
                .clickable { onToggle() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (isServiceRunning) NeonCyan else TextMuted,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    if (isServiceRunning) "توقف" else "شروع",
                    color = if (isServiceRunning) NeonCyan else TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // کارت خروجی فرکانس صوتی
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("خروجی فرکانس صوتی", color = TextMuted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isServiceRunning) "در حال ارسال پالس فعال صامت (Keep-Alive Stream)" else "Idle (غیرفعال)",
                    color = if (isServiceRunning) NeonTeal else Color.Gray,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // گزینه‌های ۳ حالت فرکانسی
        Text(
            "نوع سیگنال سکوت",
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        SignalModeItem("سکوت مطلق (PCM 0dB)", "پیش‌فرض", currentMode == 0) { onSelectMode(0) }
        Spacer(modifier = Modifier.height(8.dp))
        SignalModeItem("فرکانس نامحسوس (۱۵ هرتز)", "مطمئن‌ترین (ضد خاموشی)", currentMode == 1) { onSelectMode(1) }
        Spacer(modifier = Modifier.height(8.dp))
        SignalModeItem("پالس هوشمند (Smart Pulse)", "کم‌مصرف", currentMode == 2) { onSelectMode(2) }
    }
}

@Composable
fun SignalModeItem(title: String, badge: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
                    tint = if (isSelected) NeonCyan else TextMuted
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
                Text(badge, color = NeonCyan, fontSize = 10.sp)
            }
        }
    }
}

// ---------------------- تب دوم: هندزفری ----------------------
@Composable
fun HeadsetTab(engine: BluetoothEngine) {
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
                        tint = if (dev != null) NeonCyan else TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = dev?.name ?: "هیچ هندزفری متصل نیست",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (dev != null) "آدرس مک: ${dev.address}" else "هندزفری خود را از طریق بلوتوث گوشی متصل کنید.",
                    color = TextMuted,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // پروتکل صوتی
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF13223E))
                        .border(1.dp, NeonCyan, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("کدک صوتی فعلی: ${engine.currentCodecName}", color = NeonCyan, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { engine.checkActiveCodec() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("بررسی اتصال", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ---------------------- تب سوم: دستگاه‌ها و اسکن ----------------------
@Composable
fun ScannerTab(engine: BluetoothEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CardDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("جستجو و اتصال مستقیم به بلوتوث", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("اتصال مستقیم هندزفری از داخل اپلیکیشن", color = TextMuted, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { if (engine.isScanning) engine.stopScan() else engine.startScan() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (engine.isScanning) "توقف اسکن..." else "اسکن و اتصال دستگاه جدید", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("دستگاه‌های ثبت‌شده", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text("${engine.pairedDevices.size} دستگاه", color = NeonCyan, fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn {
            items(engine.pairedDevices) { dev ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(dev.name, color = TextWhite, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text(dev.address, color = TextMuted, fontSize = 11.sp)
                        }
                        if (dev.isConnected) {
                            Text("متصل", color = NeonTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- تب چهارم: لاگ‌ها ----------------------
@Composable
fun LogsTab(engine: BluetoothEngine) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("لاگ رویدادها", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                Icon(Icons.Default.Delete, contentDescription = null, tint = TextMuted)
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
                            Text(log.subtitle, color = TextMuted, fontSize = 11.sp)
                        }
                        Text(log.time, color = TextMuted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
