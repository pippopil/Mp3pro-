package com.tohsoft.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat as MediaNotificationCompat
import com.tohsoft.music.MainActivity

class MusicPlaybackService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_EXIT_APP, ACTION_CLOSE -> {
                // Полное закрытие приложения по свайпу вправо в шторке или кнопке закрытия
                stopForeground(true)
                stopSelf()
                try {
                    val hideIsland = Intent(this, DynamicIslandOverlayService::class.java).apply {
                        this.action = DynamicIslandOverlayService.ACTION_HIDE
                    }
                    startService(hideIsland)
                } catch (ignored: Exception) {}
                
                val closeAppIntent = Intent(ACTION_APP_SHUTDOWN).apply {
                    setPackage(packageName)
                }
                sendBroadcast(closeAppIntent)
                return START_NOT_STICKY
            }
            ACTION_UPDATE_NOTIFICATION -> {
                val title = intent.getStringExtra("extra_title") ?: "MP3 Player Pro"
                val artist = intent.getStringExtra("extra_artist") ?: "Воспроизведение"
                val isPlaying = intent.getBooleanExtra("extra_is_playing", true)
                showPlaybackNotification(title, artist, isPlaying)
            }
            ACTION_PAUSE_NOTIFICATION -> {
                val title = intent.getStringExtra("extra_title") ?: "MP3 Player Pro"
                val artist = intent.getStringExtra("extra_artist") ?: "На паузе"
                showPlaybackNotification(title, artist, false)
            }
            ACTION_TOGGLE_PLAY, ACTION_NEXT, ACTION_PREV -> {
                // Прямое управление с экрана блокировки без разблокировки телефона
                val cmdIntent = Intent(ACTION_MEDIA_COMMAND).apply {
                    putExtra("command", action)
                    setPackage(packageName)
                }
                sendBroadcast(cmdIntent)
            }
        }

        return START_NOT_STICKY
    }

    private fun showPlaybackNotification(title: String, artist: String, isPlaying: Boolean) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Интент при свайпе вправо (deleteIntent) для полного закрытия приложения
        val deleteIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_EXIT_APP
        }
        val deletePendingIntent = PendingIntent.getService(
            this, 1, deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Кнопка закрытия
        val closeIntent = Intent(this, MusicPlaybackService::class.java).apply {
            action = ACTION_CLOSE
        }
        val closePendingIntent = PendingIntent.getService(
            this, 2, closeIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Кнопки управления на экране блокировки без разблокировки
        val prevPending = PendingIntent.getService(
            this, 3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREV },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val playPausePending = PendingIntent.getService(
            this, 4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE_PLAY },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val nextPending = PendingIntent.getService(
            this, 5,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("MP3 Player Pro • Lock Screen")
            .setContentIntent(openPendingIntent)
            .setDeleteIntent(deletePendingIntent) // Свайп вправо полностью закрывает приложение
            .addAction(android.R.drawable.ic_media_previous, "Пред.", prevPending)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Пауза" else "Играть",
                playPausePending
            )
            .addAction(android.R.drawable.ic_media_next, "След.", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Закрыть", closePendingIntent)
            .setStyle(
                MediaNotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setOngoing(isPlaying)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC) // Полный доступ на экране блокировки без разблокировки
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Управление воспроизведением (Lock Screen)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Уведомление в шторке и на экране блокировки для управления музыкой"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        try {
            val hideIsland = Intent(this, DynamicIslandOverlayService::class.java).apply {
                this.action = DynamicIslandOverlayService.ACTION_HIDE
            }
            startService(hideIsland)
        } catch (ignored: Exception) {}
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "mp3_player_playback_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_UPDATE_NOTIFICATION = "com.tohsoft.music.ACTION_UPDATE_NOTIFICATION"
        const val ACTION_PAUSE_NOTIFICATION = "com.tohsoft.music.ACTION_PAUSE_NOTIFICATION"
        const val ACTION_TOGGLE_PLAY = "com.tohsoft.music.ACTION_TOGGLE_PLAY"
        const val ACTION_PREV = "com.tohsoft.music.ACTION_PREV"
        const val ACTION_NEXT = "com.tohsoft.music.ACTION_NEXT"
        const val ACTION_CLOSE = "com.tohsoft.music.ACTION_CLOSE"
        const val ACTION_EXIT_APP = "com.tohsoft.music.ACTION_EXIT_APP"
        const val ACTION_APP_SHUTDOWN = "com.tohsoft.music.ACTION_APP_SHUTDOWN"
        const val ACTION_MEDIA_COMMAND = "com.tohsoft.music.ACTION_MEDIA_COMMAND"
    }
}
