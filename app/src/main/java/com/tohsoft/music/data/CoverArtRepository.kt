package com.tohsoft.music.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Репозиторий поиска и загрузки обложек треков из интернета (iTunes Search API / Apple Music HD).
 * Не требует API-ключей, работает по безопасному HTTPS и кэширует обложки во внутреннее хранилище.
 */
class CoverArtRepository(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cacheDirectory: File by lazy {
        File(context.cacheDir, "covers").apply { mkdirs() }
    }

    data class OnlineCover(
        val trackId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val hdCoverUrl: String,
        val previewUrl: String,
        val releaseYear: Int?
    )

    /**
     * Поиск обложек онлайн по артисту и названию трека.
     */
    suspend fun searchCoverArt(artist: String, title: String): List<OnlineCover> = withContext(Dispatchers.IO) {
        val query = cleanQuery("$artist $title")
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=10"

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "MP3PlayerPro-Android/6.8")
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val jsonString = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(jsonString)
                val results = json.optJSONArray("results") ?: return@withContext emptyList()

                val list = mutableListOf<OnlineCover>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val rawArtUrl = item.optString("artworkUrl100")
                    // Заменяем 100x100 на максимальное качество 1000x1000 HD
                    val hdArtUrl = rawArtUrl.replace("100x100bb.jpg", "1000x1000bb.jpg")
                    val releaseDate = item.optString("releaseDate")
                    val year = releaseDate.take(4).toIntOrNull()

                    list.add(
                        OnlineCover(
                            trackId = item.optLong("trackId"),
                            title = item.optString("trackName"),
                            artist = item.optString("artistName"),
                            album = item.optString("collectionName"),
                            hdCoverUrl = hdArtUrl,
                            previewUrl = rawArtUrl,
                            releaseYear = year
                        )
                    )
                }
                list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Скачивает обложку в высоком разрешении и сохраняет во внутреннее защищённое хранилище приложения.
     */
    suspend fun downloadAndCacheCover(songId: Long, imageUrl: String): Uri? = withContext(Dispatchers.IO) {
        val file = File(cacheDirectory, "cover_song_$songId.jpg")
        val request = Request.Builder().url(imageUrl).build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bytes = response.body?.bytes() ?: return@withContext null
                FileOutputStream(file).use { it.write(bytes) }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun cleanQuery(text: String): String {
        return text
            .replace(Regex("\\.(mp3|flac|wav|m4a|ogg)$", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\d+[\\s.-]+"), "")
            .replace(Regex("\\(.*?\\)"), "")
            .replace(Regex("\\[.*?\\]"), "")
            .trim()
    }
}
