package com.tohsoft.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class MP3PlayerApp : Application() {

    companion object {
        const val PLAYBACK_CHANNEL_ID = "music_playback_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java) ?: return
            val legacyChannel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Фоновое воспроизведение музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Элементы управления воспроизведением аудио в шторке"
                setShowBadge(false)
            }
            val media3Channel = NotificationChannel(
                "mp3_pro_media_session_channel",
                "MP3 Player Pro (Media3)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением, обложка и перемотка для Android 14+"
                setShowBadge(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(legacyChannel)
            notificationManager.createNotificationChannel(media3Channel)
        }
    }
}
