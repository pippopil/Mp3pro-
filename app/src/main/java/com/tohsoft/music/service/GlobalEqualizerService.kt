package com.tohsoft.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tohsoft.music.MainActivity
import com.tohsoft.music.audio.EqualizerManager

/**
 * Фоновая служба глобального эквалайзера.
 * Обрабатывает аудиопотоки сторонних плееров (Яндекс Музыка, Spotify)
 * или системный микс Session 0 через аппаратный 10-полосный эквалайзер.
 */
class GlobalEqualizerService : Service() {

    companion object {
        const val CHANNEL_ID = "global_equalizer_channel"
        const val NOTIFICATION_ID = 2002

        const val ACTION_START_GLOBAL = "com.tohsoft.music.ACTION_START_GLOBAL_EQ"
        const val ACTION_STOP_GLOBAL = "com.tohsoft.music.ACTION_STOP_GLOBAL_EQ"
        const val ACTION_ATTACH_SESSION = "com.tohsoft.music.ACTION_ATTACH_SESSION"
        const val ACTION_DETACH_SESSION = "com.tohsoft.music.ACTION_DETACH_SESSION"

        const val EXTRA_AUDIO_SESSION_ID = "extra_audio_session_id"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"

        var isServiceRunning = false
            private set
        var currentAttachedPackage: String = "Глобальный аудиомикс"
            private set
        var currentSessionId: Int = 0
            private set
    }

    private var equalizerManager: EqualizerManager? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        equalizerManager = EqualizerManager(0)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_GLOBAL
        val sessionId = intent?.getIntExtra(EXTRA_AUDIO_SESSION_ID, 0) ?: 0
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: "Глобальный аудиомикс"

        when (action) {
            ACTION_STOP_GLOBAL -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                isServiceRunning = false
                return START_NOT_STICKY
            }
            ACTION_ATTACH_SESSION -> {
                currentAttachedPackage = formatPackageName(pkg)
                currentSessionId = sessionId
                equalizerManager?.attachAudioSession(sessionId)
            }
            ACTION_DETACH_SESSION -> {
                currentAttachedPackage = "Глобальный аудиомикс"
                currentSessionId = 0
                equalizerManager?.attachAudioSession(0)
            }
            else -> {
                currentAttachedPackage = formatPackageName(pkg)
                currentSessionId = sessionId
                equalizerManager?.attachAudioSession(sessionId)
            }
        }

        isServiceRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    private fun formatPackageName(pkg: String): String {
        return when {
            pkg.contains("yandex.music") -> "Яндекс Музыка"
            pkg.contains("spotify") -> "Spotify"
            pkg.contains("vkontakte") || pkg.contains("vk.music") -> "VK Музыка"
            pkg.contains("youtube") -> "YouTube Music"
            pkg.contains("sound") -> "Звук (Сбер)"
            pkg.contains("deezer") -> "Deezer"
            else -> pkg
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GlobalEqualizerService::class.java).apply {
            action = ACTION_STOP_GLOBAL
        }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("🎛️ MP3 Pro Эквалайзер активен")
            .setContentText("Обработка звука: ${currentAttachedPackage} (Сессия #${currentSessionId})")
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Выключить", pendingStop)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Глобальный эквалайзер для сторонних плееров",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Показывает статус обработки звука для Яндекс Музыки, Spotify и внешних плееров"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        equalizerManager?.release()
        equalizerManager = null
        isServiceRunning = false
        super.onDestroy()
    }
}
