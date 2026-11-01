package com.tohsoft.music.data.yandex

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Модели данных Яндекс Музыки
 */
data class YandexTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val coverUri: String?,
    val year: Int?
) {
    /**
     * Получить HD обложку в разрешении 1000x1000 пикселей
     */
    val hdCoverUrl: String?
        get() = coverUri?.replace("%%", "1000x1000")?.let { "https://$it" }
}

data class YandexDownloadInfo(
    val host: String,
    val path: String,
    val ts: String,
    val s: String
)

/**
 * Репозиторий интеграции с Яндекс Музыкой (Mobile API)
 * Позволяет авторизоваться по OAuth-токену, получить сохраненные лайкнутые треки
 * и скачать их в максимальном качестве 320 kbps в Scoped Storage устройства.
 */
class YandexMusicRepository(
    private val client: OkHttpClient = OkHttpClient()
) {
    companion object {
        private const val BASE_API_URL = "https://api.music.yandex.net"
        // Секретная соль Яндекса для генерации проверочного MD5 хеша прямой MP3-ссылки
        private const val SIGN_SALT = "XGRlBW9FXlekgb0rphzgbt"
    }

    /**
     * Получить список идентификаторов всех сохраненных (лайкнутых) треков пользователя
     */
    suspend fun getLikedTrackIds(token: String, userId: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_API_URL/users/$userId/likes/tracks")
                .header("Authorization", "OAuth $token")
                .header("Accept", "application/json")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: Ошибка авторизации или доступа"))
                }
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Пустой ответ"))
                val json = JSONObject(bodyStr)
                val resultObj = json.getJSONObject("result")
                val libraryObj = resultObj.getJSONObject("library")
                val tracksArray = libraryObj.getJSONArray("tracks")

                val trackIds = mutableListOf<String>()
                for (i in 0 until tracksArray.length()) {
                    val item = tracksArray.getJSONObject(i)
                    trackIds.add(item.getString("id"))
                }
                Result.success(trackIds)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Загрузить метаданные пачки треков (название, артисты, альбом, обложка)
     */
    suspend fun getTracksMetadata(token: String, trackIds: List<String>): Result<List<YandexTrack>> = withContext(Dispatchers.IO) {
        try {
            val formBody = okhttp3.FormBody.Builder()
                .add("track-ids", trackIds.joinToString(","))
                .build()

            val request = Request.Builder()
                .url("$BASE_API_URL/tracks")
                .header("Authorization", "OAuth $token")
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Не удалось загрузить метаданные"))
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val resultArray = json.getJSONArray("result")

                val resultList = mutableListOf<YandexTrack>()
                for (i in 0 until resultArray.length()) {
                    val trackObj = resultArray.getJSONObject(i)
                    val id = trackObj.getString("id")
                    val title = trackObj.getString("title")
                    
                    val artistsArray = trackObj.optJSONArray("artists")
                    val artistName = if (artistsArray != null && artistsArray.length() > 0) {
                        artistsArray.getJSONObject(0).getString("name")
                    } else "Неизвестный исполнитель"

                    val albumsArray = trackObj.optJSONArray("albums")
                    val albumObj = if (albumsArray != null && albumsArray.length() > 0) albumsArray.getJSONObject(0) else null
                    val albumTitle = albumObj?.optString("title") ?: "Сингл"
                    val year = albumObj?.optInt("year")
                    val durationMs = trackObj.optLong("durationMs", 0L)
                    val coverUri = trackObj.optString("coverUri", null)

                    resultList.add(
                        YandexTrack(
                            id = id,
                            title = title,
                            artist = artistName,
                            album = albumTitle,
                            durationMs = durationMs,
                            coverUri = coverUri,
                            year = year
                        )
                    )
                }
                Result.success(resultList)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Получить прямую ссылку на скачивание аудиофайла 320 kbps MP3
     */
    suspend fun getDownloadUrl(token: String, trackId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // 1. Запрос параметров хранилища
            val requestInfo = Request.Builder()
                .url("$BASE_API_URL/tracks/$trackId/download-info")
                .header("Authorization", "OAuth $token")
                .build()

            val infoUrl = client.newCall(requestInfo).execute().use { resp ->
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val results = json.getJSONArray("result")
                // Ищем наивысший битрейт (320 или 192 kbps)
                var bestUrl = ""
                var maxBitrate = 0
                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val bitrate = item.optInt("bitrateInKbps", 0)
                    if (bitrate >= maxBitrate) {
                        maxBitrate = bitrate
                        bestUrl = item.getString("downloadInfoUrl")
                    }
                }
                bestUrl
            }

            if (infoUrl.isEmpty()) return@withContext Result.failure(Exception("Параметры скачивания не найдены"))

            // 2. Получение XML данных хранилища
            val requestXml = Request.Builder().url(infoUrl).build()
            client.newCall(requestXml).execute().use { resp ->
                val xml = resp.body?.string() ?: ""
                val host = extractXmlTag(xml, "host")
                val path = extractXmlTag(xml, "path")
                val ts = extractXmlTag(xml, "ts")
                val s = extractXmlTag(xml, "s")

                // 3. Вычисление подписи MD5: md5("XGRlBW9FXlekgb0rphzgbt" + path.substring(1) + s)
                val dataToSign = SIGN_SALT + path.substring(1) + s
                val md5Sign = md5(dataToSign)

                // 4. Итоговая прямая ссылка на MP3 файл
                val directUrl = "https://$host/get-mp3/$md5Sign/$ts$path"
                Result.success(directUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Скачивание файла в Scoped Storage (папка /Music/YandexMusic)
     * с регистрацией в MediaStore для мгновенного появления в плеере
     */
    suspend fun downloadTrackToLocalStorage(
        context: Context,
        track: YandexTrack,
        directUrl: String,
        onProgress: (Int) -> Unit = {}
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val rawName = "${track.artist} - ${track.title}"
            val safeName = rawName.replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁ ._ -]"), "_")
            val fileName = "$safeName.mp3"
            val resolver = context.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                put(MediaStore.Audio.Media.TITLE, track.title)
                put(MediaStore.Audio.Media.ARTIST, track.artist)
                put(MediaStore.Audio.Media.ALBUM, track.album)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/YandexMusic")
                    put(MediaStore.Audio.Media.IS_PENDING, 1 as Int)
                }
            }

            val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext Result.failure(Exception("Не удалось создать запись в MediaStore"))

            val request = Request.Builder().url(directUrl).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    resolver.delete(uri, null, null)
                    return@withContext Result.failure(Exception("Ошибка сети при скачивании MP3"))
                }

                val body = response.body ?: throw Exception("Тело ответа пустое")
                val totalLength = body.contentLength()

                resolver.openOutputStream(uri)?.use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var downloaded = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloaded += bytesRead
                            if (totalLength > 0) {
                                val progress = ((downloaded * 100) / totalLength).toInt()
                                onProgress(progress)
                            }
                        }
                        output.flush()
                    }
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0 as Int)
                resolver.update(uri, contentValues, null, null)
            }

            Result.success(uri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val bytes = md.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun extractXmlTag(xml: String, tag: String): String {
        val regex = "<$tag>(.*?)</$tag>".toRegex()
        return regex.find(xml)?.groupValues?.get(1) ?: ""
    }
}
