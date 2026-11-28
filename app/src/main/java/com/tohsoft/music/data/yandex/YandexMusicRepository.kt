package com.tohsoft.music.data.yandex

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
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
import java.util.concurrent.TimeUnit

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
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        private const val BASE_API_URL = "https://api.music.yandex.net"
        private const val LOGIN_INFO_URL = "https://login.yandex.ru/info?format=json"
        private const val YANDEX_CLIENT_HEADER = "YandexMusicAndroid/24023621"
        private const val YANDEX_USER_AGENT = "Yandex-Music-API"
        // Секретная соль Яндекса для генерации проверочного MD5 хеша прямой MP3-ссылки
        private const val SIGN_SALT = "XGRlBW9FXlekgb0rphzgbt"
    }

    fun sanitizeToken(rawToken: String): String {
        val extracted = if (rawToken.contains("access_token=")) {
            rawToken.substringAfter("access_token=").substringBefore("&").substringBefore("#")
        } else {
            rawToken
        }
        return extracted
            .removePrefix("OAuth ")
            .removePrefix("Bearer ")
            .trim()
    }

    private fun authedRequestBuilder(url: String, token: String): Request.Builder {
        val clean = sanitizeToken(token)
        return Request.Builder()
            .url(url)
            .header("Authorization", "OAuth $clean")
            .header("X-Yandex-Music-Client", YANDEX_CLIENT_HEADER)
            .header("User-Agent", YANDEX_USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "ru")
    }

    /**
     * Получить числовой UID авторизованного пользователя Яндекс Музыки:
     * 1) через /account/status (Mobile API),
     * 2) резервный вариант — через https://login.yandex.ru/info?format=json.
     */
    suspend fun fetchAccountUidResult(token: String): Result<String> = withContext(Dispatchers.IO) {
        val clean = sanitizeToken(token)
        if (clean.isBlank()) return@withContext Result.failure(Exception("Пустой OAuth-токен"))

        var lastError = "Неизвестная ошибка"

        // 1. Основной способ: api.music.yandex.net/account/status
        try {
            val req = authedRequestBuilder("$BASE_API_URL/account/status", clean).build()
            client.newCall(req).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val accountObj = JSONObject(body)
                        .optJSONObject("result")
                        ?.optJSONObject("account")
                    val uidNum = accountObj?.optLong("uid", 0L) ?: 0L
                    val uidStr = if (uidNum > 0L) uidNum.toString() else accountObj?.optString("uid", "").orEmpty()
                    if (uidStr.isNotBlank() && uidStr != "null" && uidStr != "0") {
                        return@withContext Result.success(uidStr)
                    }
                    lastError = "Ответ /account/status не содержит uid"
                } else {
                    lastError = "Music API HTTP ${response.code}"
                }
            }
        } catch (e: Exception) {
            lastError = e.message ?: "Сетевая ошибка /account/status"
        }

        // 2. Резервный способ: login.yandex.ru/info?format=json (всегда отдаёт числовой id для валидного OAuth-токена)
        try {
            val loginReq = Request.Builder()
                .url(LOGIN_INFO_URL)
                .header("Authorization", "OAuth $clean")
                .header("Accept", "application/json")
                .build()
            client.newCall(loginReq).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful && body.isNotBlank()) {
                    val uidStr = JSONObject(body).optString("id", "").trim()
                    if (uidStr.isNotBlank() && uidStr != "null" && uidStr != "0") {
                        return@withContext Result.success(uidStr)
                    }
                } else {
                    lastError = "$lastError; Passport HTTP ${response.code}"
                }
            }
        } catch (e: Exception) {
            lastError = "$lastError; ${e.message ?: "сеть"}"
        }

        Result.failure(Exception(lastError))
    }

    /**
     * Получить UID авторизованного пользователя Яндекс Музыки (совместимость со старым вызовом)
     */
    suspend fun fetchAccountUid(token: String): String {
        return fetchAccountUidResult(token).getOrDefault("me")
    }

    /**
     * Получить список идентификаторов всех сохраненных (лайкнутых) треков пользователя
     */
    suspend fun getLikedTrackIds(token: String, userId: String = ""): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val clean = sanitizeToken(token)
            val resolvedUid = if (userId.isBlank() || userId == "me") {
                fetchAccountUidResult(clean).getOrElse { return@withContext Result.failure(it) }
            } else {
                userId
            }

            val request = authedRequestBuilder("$BASE_API_URL/users/$resolvedUid/likes/tracks", clean).build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP ${response.code}: Ошибка доступа к библиотеке (UID=$resolvedUid)"))
                }
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Пустой ответ от сервера"))
                val json = JSONObject(bodyStr)
                val tracksArray = json.optJSONObject("result")
                    ?.optJSONObject("library")
                    ?.optJSONArray("tracks")
                    ?: return@withContext Result.success(emptyList())

                val trackIds = mutableListOf<String>()
                for (i in 0 until tracksArray.length()) {
                    val item = tracksArray.optJSONObject(i) ?: continue
                    val id = item.optString("id", "").trim()
                    if (id.isNotBlank()) trackIds.add(id)
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
            if (trackIds.isEmpty()) return@withContext Result.success(emptyList())
            val formBody = okhttp3.FormBody.Builder()
                .add("track-ids", trackIds.joinToString(","))
                .build()

            val request = authedRequestBuilder("$BASE_API_URL/tracks", token)
                .post(formBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Метаданные HTTP ${response.code}"))
                val bodyStr = response.body?.string() ?: ""
                val json = JSONObject(bodyStr)
                val resultArray = json.optJSONArray("result") ?: return@withContext Result.success(emptyList())

                val resultList = mutableListOf<YandexTrack>()
                for (i in 0 until resultArray.length()) {
                    val trackObj = resultArray.optJSONObject(i) ?: continue
                    val id = trackObj.optString("id", "").trim()
                    val title = trackObj.optString("title", "").trim()
                    if (id.isBlank() || title.isBlank()) continue

                    val artistsArray = trackObj.optJSONArray("artists")
                    val artistName = if (artistsArray != null && artistsArray.length() > 0) {
                        artistsArray.optJSONObject(0)?.optString("name").orEmpty().ifBlank { "Неизвестный исполнитель" }
                    } else "Неизвестный исполнитель"

                    val albumsArray = trackObj.optJSONArray("albums")
                    val albumObj = if (albumsArray != null && albumsArray.length() > 0) albumsArray.optJSONObject(0) else null
                    val albumTitle = albumObj?.optString("title").orEmpty().ifBlank { "Сингл" }
                    val year = albumObj?.optInt("year", 0)?.takeIf { it > 0 }
                    val durationMs = trackObj.optLong("durationMs", 0L)
                    val coverUri = trackObj.optString("coverUri", "").takeIf { it.isNotBlank() && it != "null" }

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
            val requestInfo = authedRequestBuilder("$BASE_API_URL/tracks/$trackId/download-info", token).build()

            val infoUrl = client.newCall(requestInfo).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("download-info HTTP ${resp.code}"))
                val body = resp.body?.string() ?: ""
                val json = JSONObject(body)
                val results = json.optJSONArray("result") ?: return@withContext Result.failure(Exception("Нет данных download-info"))
                // Ищем наивысший битрейт MP3 (320 или 192 kbps), fallback на любой формат
                var bestMp3Url = ""
                var maxMp3Bitrate = 0
                var fallbackUrl = ""
                var maxAnyBitrate = 0
                for (i in 0 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    val codec = item.optString("codec", "").lowercase()
                    val bitrate = item.optInt("bitrateInKbps", 0)
                    val url = item.optString("downloadInfoUrl", "")
                    if (url.isBlank()) continue
                    if (codec == "mp3" && bitrate >= maxMp3Bitrate) {
                        maxMp3Bitrate = bitrate
                        bestMp3Url = url
                    }
                    if (bitrate >= maxAnyBitrate) {
                        maxAnyBitrate = bitrate
                        fallbackUrl = url
                    }
                }
                bestMp3Url.ifBlank { fallbackUrl }
            }

            if (infoUrl.isEmpty()) return@withContext Result.failure(Exception("Параметры скачивания не найдены"))

            // 2. Получение XML данных хранилища
            val requestXml = authedRequestBuilder(infoUrl, token).build()
            client.newCall(requestXml).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext Result.failure(Exception("XML download-info HTTP ${resp.code}"))
                val xml = resp.body?.string() ?: ""
                val host = extractXmlTag(xml, "host")
                val path = extractXmlTag(xml, "path")
                val ts = extractXmlTag(xml, "ts")
                val s = extractXmlTag(xml, "s")
                if (host.isBlank() || path.length < 2 || ts.isBlank() || s.isBlank()) {
                    return@withContext Result.failure(Exception("Неполные параметры хранилища MP3"))
                }

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
     * Скачивание файла в /Music/YandexMusic/Мне нравится
     * На Android 10+ — через MediaStore Scoped Storage;
     * на Android 5–9 (включая ГУ Changan UNI-S) — прямой записью на диск + MediaScannerConnection.scanFile,
     * чтобы папка гарантированно находилась при сканировании папок.
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

            val request = Request.Builder().url(directUrl).build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.TITLE, track.title)
                    put(MediaStore.Audio.Media.ARTIST, track.artist)
                    put(MediaStore.Audio.Media.ALBUM, "Мне нравится • ${track.album}")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/YandexMusic/Мне нравится")
                    put(MediaStore.Audio.Media.IS_PENDING, 1 as Int)
                }

                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Не удалось создать запись в MediaStore"))

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
                                    onProgress(((downloaded * 100) / totalLength).toInt())
                                }
                            }
                            output.flush()
                        }
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0 as Int)
                resolver.update(uri, contentValues, null, null)
                return@withContext Result.success(uri)
            } else {
                // Android 5.0–9.0 (API 21–28, включая автомобильные ГУ Changan):
                // пишем физически в /Music/YandexMusic/Мне нравится и сразу индексируем MediaScannerConnection
                val musicRoot = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                val targetDir = File(musicRoot, "YandexMusic/Мне нравится")
                if (!targetDir.exists()) targetDir.mkdirs()

                val targetFile = File(targetDir, fileName)
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("Ошибка сети при скачивании MP3"))
                    }
                    val body = response.body ?: throw Exception("Тело ответа пустое")
                    val totalLength = body.contentLength()

                    FileOutputStream(targetFile).use { output ->
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            var downloaded = 0L

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloaded += bytesRead
                                if (totalLength > 0) {
                                    onProgress(((downloaded * 100) / totalLength).toInt())
                                }
                            }
                            output.flush()
                        }
                    }
                }

                val legacyValues = ContentValues().apply {
                    put(MediaStore.Audio.Media.DATA, targetFile.absolutePath)
                    put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.TITLE, track.title)
                    put(MediaStore.Audio.Media.ARTIST, track.artist)
                    put(MediaStore.Audio.Media.ALBUM, "Мне нравится • ${track.album}")
                    put(MediaStore.Audio.Media.IS_MUSIC, 1)
                }
                val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, legacyValues)
                    ?: Uri.fromFile(targetFile)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("audio/mpeg"),
                    null
                )
                return@withContext Result.success(uri)
            }
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
