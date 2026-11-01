package com.tohsoft.music.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.tohsoft.music.R
import com.tohsoft.music.data.yandex.YandexMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Фоновый воркер WorkManager для надёжной фоновой загрузки фонотеки Яндекс Музыки
 * Отображает статус-бар уведомление с прогрессом скачивания и сохраняет треки
 * напрямую в память устройства с 320 kbps качеством.
 */
class YandexSyncWorker(
    private val context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val repository = YandexMusicRepository()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val CHANNEL_ID = "yandex_music_sync_channel"
        const val NOTIFICATION_ID = 2024
        const val KEY_OAUTH_TOKEN = "oauth_token"
        const val KEY_USER_ID = "user_id"
    }

    override suspend fun doWork(): ListenableWorker.Result = withContext(Dispatchers.IO) {
        val token = inputData.getString(KEY_OAUTH_TOKEN) ?: return@withContext ListenableWorker.Result.failure()
        val userId = inputData.getString(KEY_USER_ID) ?: "me"

        createNotificationChannel()
        setForeground(createForegroundInfo(0, "Инициализация синхронизации..."))

        try {
            // 1. Получаем список ID лайков
            val likedIdsResult = repository.getLikedTrackIds(token, userId)
            val trackIds = likedIdsResult.getOrNull() ?: return@withContext ListenableWorker.Result.retry()

            if (trackIds.isEmpty()) {
                return@withContext ListenableWorker.Result.success()
            }

            // 2. Получаем метаданные треков пачками по 50 штук
            val tracks = repository.getTracksMetadata(token, trackIds.take(100)).getOrNull() ?: emptyList()

            var downloadedCount = 0
            val total = tracks.size

            for (track in tracks) {
                val progressPercent = if (total > 0) ((downloadedCount * 100) / total) else 0
                setForeground(createForegroundInfo(progressPercent, "Загрузка: ${track.artist} - ${track.title} (${downloadedCount + 1}/$total)"))

                // Получаем прямую MP3 320 kbps ссылку
                val downloadUrlResult = repository.getDownloadUrl(token, track.id)
                val downloadUrl = downloadUrlResult.getOrNull()

                if (downloadUrl != null) {
                    repository.downloadTrackToLocalStorage(context, track, downloadUrl)
                }

                downloadedCount++
            }

            // Завершено
            notificationManager.cancel(NOTIFICATION_ID)
            ListenableWorker.Result.success()
        } catch (e: Exception) {
            ListenableWorker.Result.retry()
        }
    }

    private fun createForegroundInfo(progress: Int, statusText: String): ForegroundInfo {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Яндекс Музыка: Синхронизация")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Синхронизация Яндекс Музыки",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Фоновое скачивание треков в память устройства"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
}
