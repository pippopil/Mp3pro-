package com.tohsoft.music.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.os.Build
import com.tohsoft.music.service.GlobalEqualizerService

/**
 * Приёмник широковещательных намерений AudioEffect.
 * Автоматически срабатывает, когда на телефоне запускается любой сторонний плеер:
 * Яндекс Музыка, Spotify, VK Музыка, YouTube Music, Deezer и т.д.
 */
class AudioSessionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val audioSessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0)
        val packageName = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME) ?: "Сторонний плеер"

        val serviceIntent = Intent(context, GlobalEqualizerService::class.java).apply {
            putExtra(GlobalEqualizerService.EXTRA_PACKAGE_NAME, packageName)
            putExtra(GlobalEqualizerService.EXTRA_AUDIO_SESSION_ID, audioSessionId)
            when (action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    this.action = GlobalEqualizerService.ACTION_ATTACH_SESSION
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    this.action = GlobalEqualizerService.ACTION_DETACH_SESSION
                }
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
