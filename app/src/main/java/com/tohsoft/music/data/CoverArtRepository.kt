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

    data class OnlineMusicTrack(
        val trackId: Long,
        val title: String,
        val artist: String,
        val album: String,
        val genre: String,
        val audioUrl: String,
        val coverUrl: String,
        val durationMs: Long
    )

    /**
     * Живой глобальный поиск музыки в сети по названию, артисту или жанру через открытый API.
     */
    suspend fun searchOnlineMusic(query: String): List<OnlineMusicTrack> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()
        val encodedQuery = URLEncoder.encode(trimmed, "UTF-8")
        val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=30"

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

                val list = mutableListOf<OnlineMusicTrack>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val previewUrl = item.optString("previewUrl")
                    if (previewUrl.isNullOrBlank()) continue

                    val rawArtUrl = item.optString("artworkUrl100")
                    val hdArtUrl = rawArtUrl.replace("100x100bb.jpg", "600x600bb.jpg")

                    list.add(
                        OnlineMusicTrack(
                            trackId = item.optLong("trackId"),
                            title = item.optString("trackName", "Без названия"),
                            artist = item.optString("artistName", "Неизвестный исполнитель"),
                            album = item.optString("collectionName", "Сингл"),
                            genre = item.optString("primaryGenreName", "Музыка"),
                            audioUrl = previewUrl,
                            coverUrl = hdArtUrl,
                            durationMs = item.optLong("trackTimeMillis", 30000L)
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
     * Поиск обложек онлайн по артисту и названию трека с каскадным fallback.
     */
    suspend fun searchCoverArt(artist: String, title: String): List<OnlineCover> = withContext(Dispatchers.IO) {
        val q1 = cleanQuery(artist + " " + title)
        var results = executeQuery(q1)
        if (results.isEmpty()) {
            val q2 = cleanQuery(title)
            if (q2.isNotBlank() && q2 != q1) {
                results = executeQuery(q2)
            }
        }
        if (results.isEmpty() && artist.isNotBlank() && !artist.contains("unknown", ignoreCase = true)) {
            val q3 = cleanQuery(artist)
            results = executeQuery(q3)
        }
        results
    }

    private fun executeQuery(query: String): List<OnlineCover> {
        if (query.isBlank()) return emptyList()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=" + encodedQuery + "&entity=song&limit=10"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MP3PlayerPro-Android/6.8")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val jsonString = response.body?.string() ?: return emptyList()
                val json = JSONObject(jsonString)
                val results = json.optJSONArray("results") ?: return emptyList()

                val list = mutableListOf<OnlineCover>()
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val rawArtUrl = item.optString("artworkUrl100")
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
                return list
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }

    /**
     * Создает авторскую процедурную обложку для трека,
     * если в сети или метаданных ID3 обложка не найдена.
     * Ни один трек в плеере никогда не остаётся без обложки!
     */
    fun generateProceduralCover(songId: Long, title: String, artist: String): Uri {
        val file = File(cacheDirectory, "gen_art_" + Math.abs(songId) + ".jpg")
        if (file.exists() && file.length() > 500) {
            return Uri.fromFile(file)
        }

        try {
            val size = 512
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)

            val palettes = listOf(
                intArrayOf(0xFF0A0F1D.toInt(), 0xFF0D9488.toInt(), 0xFF06B6D4.toInt()),
                intArrayOf(0xFF0F172A.toInt(), 0xFF6366F1.toInt(), 0xFFA855F7.toInt()),
                intArrayOf(0xFF070B19.toInt(), 0xFF059669.toInt(), 0xFF10B981.toInt()),
                intArrayOf(0xFF180828.toInt(), 0xFFE11D48.toInt(), 0xFFFB7185.toInt()),
                intArrayOf(0xFF090D1E.toInt(), 0xFF2563EB.toInt(), 0xFF38BDF8.toInt()),
                intArrayOf(0xFF1E1B4B.toInt(), 0xFFD97706.toInt(), 0xFFFBBF24.toInt())
            )
            val hash = Math.abs((title + artist).hashCode())
            val colors = palettes[hash % palettes.size]

            val bgPaint = android.graphics.Paint().apply {
                shader = android.graphics.LinearGradient(
                    0f, 0f, size.toFloat(), size.toFloat(),
                    colors[0], colors[1],
                    android.graphics.Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

            val circlePaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 3f
                color = colors[2]
                alpha = 80
            }
            canvas.drawCircle(size / 2f, size / 2f, 180f, circlePaint)
            canvas.drawCircle(size / 2f, size / 2f, 130f, circlePaint)
            canvas.drawCircle(size / 2f, size / 2f, 80f, circlePaint)

            val centerPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                style = android.graphics.Paint.Style.FILL
                color = colors[2]
                alpha = 200
            }
            canvas.drawCircle(size / 2f, size / 2f, 55f, centerPaint)

            val textPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 44f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(8f, 0f, 4f, 0xFF000000.toInt())
            }
            val initials = ((artist.firstOrNull()?.toString() ?: "M") + 
                            (title.firstOrNull()?.toString() ?: "P")).uppercase()
            canvas.drawText(initials, size / 2f, (size / 2f) + 16f, textPaint)

            val subPaint = android.graphics.Paint().apply {
                isAntiAlias = true
                color = android.graphics.Color.WHITE
                textSize = 22f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                alpha = 230
            }
            val cleanTitle = if (title.length > 22) title.take(20) + "..." else title
            canvas.drawText(cleanTitle, size / 2f, size - 45f, subPaint)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            bitmap.recycle()
            return Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            return Uri.EMPTY
        }
    }

    /**
     * Скачивает обложку в высоком разрешении и сохраняет во внутреннее защищённое хранилище приложения.
     */
    suspend fun downloadAndCacheCover(songId: Long, imageUrl: String): Uri? = withContext(Dispatchers.IO) {
        val file = File(cacheDirectory, "cover_song_" + songId + ".jpg")
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
