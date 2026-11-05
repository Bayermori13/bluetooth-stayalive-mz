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
        const val EXTRA_MODE = "EXTRA_MODE"

        // وضعیت‌های زنده و مشترک بین برنامه و پس‌زمینه
        val isRunningState = MutableStateFlow(false)
        val signalModeState = MutableStateFlow(1)
        val elapsedSecondsState = MutableStateFlow(0L)
    }

    private var audioTrack: AudioTrack? = null
    private var isStreaming = false
    private var streamThread: Thread? = null
    private var timerThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        val mode = intent?.getIntExtra(EXTRA_MODE, signalModeState.value) ?: 1

        if (action == ACTION_STOP) {
            stopAudioStream()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            isRunningState.value = false
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
                Thread.sleep(1000)
                elapsedSecondsState.value += 1
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

        val modeLabel = when(signalModeState.value) {
            0 -> "سکوت مطلق (PCM 0dB)"
            1 -> "فرکانس نامحسوس (۱۵ هرتز)"
            else -> "پالس هوشمند صوتی"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_mz_logo)
            .setContentTitle("سرویس نگه‌دارنده MZ فعال است")
            .setContentText("سیگنال: $modeLabel")
            .setOngoing(true) // کاملاً ثابت و غیرقابل کشیدن با دست
            .setAutoCancel(false)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "خاموش کردن", pendingStop)
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

        audioTrack?.play()

        streamThread = thread(start = true) {
            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            val frequency = 15.0
            var pulseTick = 0

            while (isStreaming) {
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
                audioTrack?.write(buffer, 0, buffer.size)
            }
        }
    }

    private fun stopAudioStream() {
        isStreaming = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            streamThread?.interrupt()
            streamThread = null
            timerThread?.interrupt()
            timerThread = null
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        stopAudioStream()
        isRunningState.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
