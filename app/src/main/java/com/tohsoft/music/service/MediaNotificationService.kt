package com.tohsoft.music.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.tohsoft.music.MainActivity
import com.tohsoft.music.R

class MediaNotificationService : MediaSessionService() {

    interface MediaCallbacks {
        fun onPlay()
        fun onPause()
        fun onTogglePlayPause()
        fun onNext()
        fun onPrev()
        fun onCloseApp()
    }

    private var mediaSession: MediaSession? = null
    private var internalFallbackPlayer: ExoPlayer? = null

    companion object {
        const val CHANNEL_ID = "mp3_pro_media_session_channel"
        const val NOTIFICATION_ID = 1002
        const val ACTION_START_FOREGROUND = "com.tohsoft.music.ACTION_START_MEDIA_SESSION"
        const val COMMAND_CLOSE_PLAYER = "com.tohsoft.music.COMMAND_CLOSE_PLAYER"

        @Volatile
        var sharedExoPlayer: ExoPlayer? = null
            private set

        @Volatile
        var mediaCallbacks: MediaCallbacks? = null

        fun attachPlayer(player: ExoPlayer) {
            sharedExoPlayer = player
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL_ID)
            .setNotificationId(NOTIFICATION_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_launcher_foreground)
        setMediaNotificationProvider(notificationProvider)

        val basePlayer: Player = sharedExoPlayer ?: ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { internalFallbackPlayer = it }

        val bridgedPlayer = object : ForwardingPlayer(basePlayer) {
            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands()
                    .buildUpon()
                    .addAll(
                        Player.COMMAND_PLAY_PAUSE,
                        Player.COMMAND_PREPARE,
                        Player.COMMAND_STOP,
                        Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_NEXT,
                        Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                        Player.COMMAND_SEEK_TO_PREVIOUS,
                        Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                        Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                        Player.COMMAND_GET_METADATA
                    )
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_PREPARE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
                    Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_GET_METADATA -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun play() {
                val cb = mediaCallbacks
                if (cb != null) cb.onPlay() else super.play()
            }

            override fun pause() {
                val cb = mediaCallbacks
                if (cb != null) cb.onPause() else super.pause()
            }

            override fun seekToNext() {
                val cb = mediaCallbacks
                if (cb != null) cb.onNext() else super.seekToNext()
            }

            override fun seekToNextMediaItem() {
                val cb = mediaCallbacks
                if (cb != null) cb.onNext() else super.seekToNextMediaItem()
            }

            override fun seekToPrevious() {
                val cb = mediaCallbacks
                if (cb != null) cb.onPrev() else super.seekToPrevious()
            }

            override fun seekToPreviousMediaItem() {
                val cb = mediaCallbacks
                if (cb != null) cb.onPrev() else super.seekToPreviousMediaItem()
            }
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val closeButton = CommandButton.Builder()
            .setDisplayName("Закрыть")
            .setIconResId(android.R.drawable.ic_menu_close_clear_cancel)
            .setSessionCommand(SessionCommand(COMMAND_CLOSE_PLAYER, Bundle.EMPTY))
            .build()

        mediaSession = MediaSession.Builder(this, bridgedPlayer)
            .setSessionActivity(sessionActivityIntent)
            .setCallback(MediaSessionCallback())
            .setCustomLayout(listOf(closeButton))
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Немедленный синхронный перевод службы в Foreground на Android 8–15,
        // чтобы исключить ForegroundServiceDidNotStartInTimeException через ~10-30 секунд после startForegroundService()
        promoteToForegroundImmediately()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun promoteToForegroundImmediately() {
        createNotificationChannel()
        val player = sharedExoPlayer
        val meta = player?.currentMediaItem?.mediaMetadata
        val title = meta?.title?.toString()?.takeIf { it.isNotBlank() } ?: "MP3 Player Pro"
        val artist = meta?.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Фоновое воспроизведение"
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setOngoing(player?.playWhenReady == true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else 0
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, fgsType)
        } catch (_: Exception) {}
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        internalFallbackPlayer?.release()
        internalFallbackPlayer = null
        super.onDestroy()
    }

    private fun shutdownFromNotification() {
        try {
            sharedExoPlayer?.pause()
            sharedExoPlayer?.stop()
        } catch (_: Exception) {}
        try {
            startService(
                Intent(this, DynamicIslandOverlayService::class.java).apply {
                    action = DynamicIslandOverlayService.ACTION_HIDE
                }
            )
        } catch (_: Exception) {}
        mediaCallbacks?.onCloseApp()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MP3 Player Pro (Media3)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Управление воспроизведением, обложка и перемотка для Android 14+"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm?.createNotificationChannel(channel)
        }
    }

    private inner class MediaSessionCallback : MediaSession.Callback {

        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS
                .buildUpon()
                .add(SessionCommand(COMMAND_CLOSE_PLAYER, Bundle.EMPTY))
                .build()
            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {
            if (customCommand.customAction == COMMAND_CLOSE_PLAYER) {
                shutdownFromNotification()
                return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            return super.onCustomCommand(session, controller, customCommand, args)
        }

        override fun onMediaButtonEvent(
            session: MediaSession,
            controllerInfo: MediaSession.ControllerInfo,
            intent: Intent
        ): Boolean {
            if (Intent.ACTION_MEDIA_BUTTON == intent.action) {
                val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT)
                }
                if (keyEvent != null && keyEvent.action == KeyEvent.ACTION_DOWN) {
                    when (keyEvent.keyCode) {
                        KeyEvent.KEYCODE_MEDIA_NEXT -> {
                            mediaCallbacks?.onNext()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                            mediaCallbacks?.onPrev()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                            mediaCallbacks?.onTogglePlayPause()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PLAY -> {
                            mediaCallbacks?.onPlay()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                            mediaCallbacks?.onPause()
                            return true
                        }
                        KeyEvent.KEYCODE_MEDIA_STOP -> {
                            val cb = mediaCallbacks
                            if (cb != null) cb.onPause() else sharedExoPlayer?.pause()
                            return true
                        }
                    }
                }
            }
            return super.onMediaButtonEvent(session, controllerInfo, intent)
        }
    }
}
