package com.mz.bluetooth.stayalive.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.mz.bluetooth.stayalive.MainActivity
import com.mz.bluetooth.stayalive.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.concurrent.thread
import kotlin.math.sin

class KeepAliveService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESTORE_NOTIFICATION = "ACTION_RESTORE_NOTIFICATION"
        const val EXTRA_MODE = "EXTRA_MODE"

        val isRunningState = MutableStateFlow(false)
        val signalModeState = MutableStateFlow(1)
        val elapsedSecondsState = MutableStateFlow(0L)
    }

    private var audioTrack: AudioTrack? = null
    @Volatile private var isStreaming = false
    private var streamThread: Thread? = null
    private var timerThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val mode = intent?.getIntExtra(EXTRA_MODE, signalModeState.value) ?: 1

        if (action == ACTION_RESTORE_NOTIFICATION) {
            if (isRunningState.value) {
                startForegroundWithStickyNotification()
            }
            return START_STICKY
        }

        if (action == ACTION_STOP) {
            isRunningState.value = false
            stopAudioStream()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        signalModeState.value = mode
        isRunningState.value = true
        startForegroundWithStickyNotification()
        startAudioStream(mode)
        startTimer()

        return START_STICKY
    }

    private fun startTimer() {
        if (timerThread?.isAlive == true) return
        timerThread = thread(start = true) {
            while (isRunningState.value) {
                try {
                    Thread.sleep(1000)
                    elapsedSecondsState.value += 1
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
    }

    private fun startForegroundWithStickyNotification() {
        val channelId = "MZ_BLUETOOTH_KEEP_ALIVE"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "نگه‌دارنده بلوتوث MZ",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "سرویس دائمی نگه‌دارنده اتصال هندزفری بلوتوث"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, KeepAliveService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        // مکانیزم ضد حذف: در صورتی که کاربر نوتیفیکیشن را بکشد، فوراً خودش را بازتولید می‌کند
        val deleteIntent = Intent(this, KeepAliveService::class.java).apply {
            action = ACTION_RESTORE_NOTIFICATION
        }
        val pendingDelete = PendingIntent.getService(
            this, 2, deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val modeLabel = when(signalModeState.value) {
            0 -> "سکوت دیجیتال (PCM 0dB)"
            1 -> "فرکانس نامحسوس (۱۵ هرتز)"
            else -> "پالس هوشمند صوتی"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_mz_logo)
            .setContentTitle("سرویس نگه‌دارنده بلوتوث MZ فعال است")
            .setContentText("سیگنال جاری: $modeLabel")
            .setOngoing(true)
            .setAutoCancel(false)
            .setDeleteIntent(pendingDelete)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "توقف سرویس", pendingStop)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notification.flags = notification.flags or Notification.FLAG_NO_CLEAR or Notification.FLAG_ONGOING_EVENT

        startForeground(1001, notification)
    }

    private fun startAudioStream(mode: Int) {
        stopAudioStream()
        isStreaming = true

        val sampleRate = 44100
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_UNKNOWN)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        try {
            audioTrack?.play()
        } catch (_: Exception) {}

        streamThread = thread(start = true) {
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            val frequency = 15.0
            var pulseTick = 0

            try {
                while (isStreaming && !Thread.currentThread().isInterrupted) {
                    when (mode) {
                        0 -> buffer.fill(0)
                        1 -> {
                            for (i in buffer.indices) {
                                buffer[i] = (sin(phase) * 1200).toInt().toShort()
                                phase += 2.0 * Math.PI * frequency / sampleRate
                                if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                            }
                        }
                        2 -> {
                            pulseTick++
                            if (pulseTick % 100 < 6) {
                                for (i in buffer.indices) {
                                    buffer[i] = (sin(phase) * 800).toInt().toShort()
                                    phase += 2.0 * Math.PI * 18.0 / sampleRate
                                }
                            } else {
                                buffer.fill(0)
                            }
                        }
                    }
                    val track = audioTrack
                    if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                        track.write(buffer, 0, buffer.size)
                    } else {
                        break
                    }
                }
            } catch (_: Exception) {
                // خروج ایمن بدون بسته شدن برنامه
            }
        }
    }

    private fun stopAudioStream() {
        isStreaming = false
        try {
            streamThread?.interrupt()
            streamThread?.join(300) // منتظر اتمام حلقه می‌ماند تا کرش نکند
        } catch (_: Exception) {}
        streamThread = null

        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null

        try {
            timerThread?.interrupt()
        } catch (_: Exception) {}
        timerThread = null
    }

    override fun onDestroy() {
        stopAudioStream()
        isRunningState.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
