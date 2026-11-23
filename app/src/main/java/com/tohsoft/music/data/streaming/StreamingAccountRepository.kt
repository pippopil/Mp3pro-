package com.tohsoft.music.data.streaming

import android.content.Context
import android.net.Uri
import com.tohsoft.music.data.model.Song
import com.tohsoft.music.data.yandex.YandexMusicRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

enum class StreamingProvider(
    val id: String,
    val displayName: String,
    val isFreeNoKey: Boolean,
    val hint: String
) {
    AUDIUS("audius", "Audius (Бесплатно)", true, "Публичный @handle или пусто для Топ-треков (без API-ключей)"),
    SPOTIFY("spotify", "Spotify (Ссылка / Токен)", true, "Вставьте публичную ссылку open.spotify.com/playlist/... или OAuth-токен"),
    YANDEX("yandex", "Яндекс Музыка", false, "Вставьте OAuth-токен Яндекс Музыки (y0_AgAAAA...)"),
    SOUNDCLOUD("soundcloud", "SoundCloud", false, "Вставьте OAuth-токен SoundCloud"),
    VK("vk", "VK Музыка", false, "Вставьте токен VK Audio")
}

data class StreamingAccount(
    val id: String,
    val providerId: String,
    val displayName: String,
    val credential: String,
    val addedAt: Long,
    val tracks: List<Song>
)

class StreamingAccountRepository(private val context: Context) {

    // Зашифрованное хранилище OAuth-токенов (EncryptedSharedPreferences / AES-256)
    private val prefs = com.tohsoft.music.data.security.SecureTokenStorage.getEncryptedPrefs(
        context,
        encryptedName = "mp3pro_streaming_accounts_secure",
        legacyPlainName = "mp3pro_streaming_accounts"
    )
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    fun getActiveAccountId(): String? = prefs.getString("active_account_id", null)

    fun setActiveAccountId(accountId: String?) {
        prefs.edit().putString("active_account_id", accountId).apply()
    }

    fun loadSavedAccounts(): List<StreamingAccount> {
        val raw = prefs.getString("accounts_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            val list = mutableListOf<StreamingAccount>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val tracksArr = obj.optJSONArray("tracks") ?: JSONArray()
                val songs = mutableListOf<Song>()
                for (j in 0 until tracksArr.length()) {
                    val t = tracksArr.optJSONObject(j) ?: continue
                    songs.add(
                        Song(
                            id = t.optLong("id"),
                            title = t.optString("title", "Трек"),
                            artist = t.optString("artist", "Исполнитель"),
                            album = t.optString("album", "Стриминг"),
                            durationMs = t.optLong("durationMs", 180000L),
                            contentUri = Uri.parse(t.optString("uri", "")),
                            coverUrl = t.optString("coverUrl").takeIf { it.isNotBlank() },
                            genre = t.optString("genre", "Streaming"),
                            folder = t.optString("folder", "Streaming"),
                            isBuiltIn = false,
                            streamingProvider = obj.optString("providerId")
                        )
                    )
                }
                list.add(
                    StreamingAccount(
                        id = obj.optString("id"),
                        providerId = obj.optString("providerId"),
                        displayName = obj.optString("displayName"),
                        credential = obj.optString("credential"),
                        addedAt = obj.optLong("addedAt"),
                        tracks = songs
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveAccounts(accounts: List<StreamingAccount>) {
        val arr = JSONArray()
        accounts.forEach { acc ->
            val tracksArr = JSONArray()
            acc.tracks.forEach { s ->
                tracksArr.put(
                    JSONObject().apply {
                        put("id", s.id)
                        put("title", s.title)
                        put("artist", s.artist)
                        put("album", s.album)
                        put("durationMs", s.durationMs)
                        put("uri", s.contentUri.toString())
                        put("coverUrl", s.coverUrl ?: "")
                        put("genre", s.genre)
                        put("folder", s.folder)
                    }
                )
            }
            arr.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("providerId", acc.providerId)
                    put("displayName", acc.displayName)
                    put("credential", acc.credential)
                    put("addedAt", acc.addedAt)
                    put("tracks", tracksArr)
                }
            )
        }
        prefs.edit().putString("accounts_json", arr.toString()).apply()
    }

    fun removeAccount(accountId: String) {
        val remaining = loadSavedAccounts().filterNot { it.id == accountId }
        saveAccounts(remaining)
        if (getActiveAccountId() == accountId) {
            setActiveAccountId(remaining.firstOrNull()?.id)
        }
    }

    suspend fun connectAndSync(
        provider: StreamingProvider,
        rawCredential: String,
        customLabel: String
    ): Result<StreamingAccount> = withContext(Dispatchers.IO) {
        try {
            val cleanCred = rawCredential.trim()
            val (accountLabel, tracks) = when (provider) {
                StreamingProvider.AUDIUS -> syncAudius(cleanCred, customLabel)
                StreamingProvider.SPOTIFY -> syncSpotify(cleanCred, customLabel)
                StreamingProvider.YANDEX -> syncYandex(cleanCred, customLabel)
                StreamingProvider.SOUNDCLOUD -> syncSoundCloud(cleanCred, customLabel)
                StreamingProvider.VK -> syncVk(cleanCred, customLabel)
            }
            if (tracks.isEmpty()) {
                return@withContext Result.failure(Exception("Не удалось загрузить треки. Проверьте ссылку, @handle или токен."))
            }
            val accountId = provider.id + "-" + Math.abs((accountLabel + cleanCred).hashCode())
            val account = StreamingAccount(
                id = accountId,
                providerId = provider.id,
                displayName = accountLabel,
                credential = cleanCred,
                addedAt = System.currentTimeMillis(),
                tracks = tracks
            )
            val updated = listOf(account) + loadSavedAccounts().filterNot { it.id == accountId }
            saveAccounts(updated)
            Result.success(account)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun syncAudius(handleInput: String, customLabel: String): Pair<String, List<Song>> {
        val handle = handleInput.removePrefix("@").trim()
        var resolvedUserName = if (handle.isNotBlank()) "@$handle" else "Audius Trending"
        val itemsArray: JSONArray? = if (handle.isNotBlank()) {
            val userReq = Request.Builder()
                .url("https://discoveryprovider.audius.co/v1/users/handle/" + URLEncoder.encode(handle, "UTF-8") + "?app_name=MP3PlayerPro")
                .build()
            var userId = ""
            httpClient.newCall(userReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val userJson = JSONObject(resp.body?.string() ?: "{}").optJSONObject("data")
                    userId = userJson?.optString("id") ?: ""
                    userJson?.optString("name")?.takeIf { it.isNotBlank() }?.let { resolvedUserName = it }
                }
            }
            if (userId.isNotBlank()) {
                val tracksReq = Request.Builder()
                    .url("https://discoveryprovider.audius.co/v1/users/" + userId + "/tracks?limit=30&app_name=MP3PlayerPro")
                    .build()
                httpClient.newCall(tracksReq).execute().use { resp ->
                    if (resp.isSuccessful) JSONObject(resp.body?.string() ?: "{}").optJSONArray("data") else null
                }
            } else {
                val searchReq = Request.Builder()
                    .url("https://discoveryprovider.audius.co/v1/tracks/search?query=" + URLEncoder.encode(handle, "UTF-8") + "&limit=20&app_name=MP3PlayerPro")
                    .build()
                httpClient.newCall(searchReq).execute().use { resp ->
                    if (resp.isSuccessful) JSONObject(resp.body?.string() ?: "{}").optJSONArray("data") else null
                }
            }
        } else {
            val trendReq = Request.Builder()
                .url("https://discoveryprovider.audius.co/v1/tracks/trending?limit=25&app_name=MP3PlayerPro")
                .build()
            httpClient.newCall(trendReq).execute().use { resp ->
                if (resp.isSuccessful) JSONObject(resp.body?.string() ?: "{}").optJSONArray("data") else null
            }
        }

        val songs = mutableListOf<Song>()
        if (itemsArray != null) {
            for (i in 0 until itemsArray.length()) {
                val item = itemsArray.optJSONObject(i) ?: continue
                val idStr = item.optString("id")
                if (idStr.isBlank()) continue
                val durSec = item.optLong("duration", 180L).coerceAtLeast(30L)
                val userObj = item.optJSONObject("user")
                val artObj = item.optJSONObject("artwork")
                val cover = artObj?.optString("1000x1000")?.takeIf { it.isNotBlank() }
                    ?: artObj?.optString("480x480")?.takeIf { it.isNotBlank() }
                songs.add(
                    Song(
                        id = -Math.abs(("audius_" + idStr).hashCode().toLong()) - 20_000L,
                        title = item.optString("title", "Audius Track"),
                        artist = userObj?.optString("name", resolvedUserName) ?: resolvedUserName,
                        album = "Audius • $resolvedUserName",
                        durationMs = durSec * 1000L,
                        contentUri = Uri.parse("https://discoveryprovider.audius.co/v1/tracks/" + idStr + "/stream?app_name=MP3PlayerPro"),
                        coverUrl = cover,
                        genre = item.optString("genre", "Audius HQ"),
                        bitrate = "320 kbps MP3",
                        folder = "Streaming/Audius",
                        streamingProvider = "audius"
                    )
                )
            }
        }
        val label = customLabel.ifBlank { "Audius: $resolvedUserName" }
        return label to songs
    }

    private fun resolveFullStreamByQuery(query: String): Pair<String, Long>? {
        return try {
            val req = Request.Builder()
                .url("https://discoveryprovider.audius.co/v1/tracks/search?query=" + URLEncoder.encode(query, "UTF-8") + "&limit=3&app_name=MP3PlayerPro")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val arr = JSONObject(resp.body?.string() ?: "{}").optJSONArray("data") ?: return@use null
                for (i in 0 until arr.length()) {
                    val t = arr.optJSONObject(i) ?: continue
                    val tid = t.optString("id")
                    val dur = t.optLong("duration", 0L)
                    if (tid.isNotBlank() && dur >= 45L) {
                        return@use ("https://discoveryprovider.audius.co/v1/tracks/" + tid + "/stream?app_name=MP3PlayerPro") to (dur * 1000L)
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun syncSpotify(credential: String, customLabel: String): Pair<String, List<Song>> {
        val songs = mutableListOf<Song>()
        val isPublicLink = credential.contains("open.spotify.com", true) || credential.startsWith("spotify:", true)
        if (isPublicLink) {
            val regex = Regex("(playlist|album|artist|track)[/:]([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
            val match = regex.find(credential)
                ?: throw Exception("Неверный формат ссылки Spotify. Пример: https://open.spotify.com/playlist/...")
            val type = match.groupValues[1].lowercase()
            val id = match.groupValues[2]
            var collectionTitle = "Spotify " + type.replaceFirstChar { it.uppercase() }

            try {
                val oembedReq = Request.Builder()
                    .url("https://open.spotify.com/oembed?url=" + URLEncoder.encode("https://open.spotify.com/" + type + "/" + id, "UTF-8"))
                    .build()
                httpClient.newCall(oembedReq).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = JSONObject(resp.body?.string() ?: "{}")
                        json.optString("title").takeIf { it.isNotBlank() }?.let { collectionTitle = it }
                    }
                }
            } catch (_: Exception) {}

            val embedReq = Request.Builder()
                .url("https://open.spotify.com/embed/" + type + "/" + id)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val html = httpClient.newCall(embedReq).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.string() ?: "" else ""
            }
            val nextDataRegex = Regex("""<script id=["']__NEXT_DATA__["'][^>]*>(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
            val nextMatch = nextDataRegex.find(html)
            if (nextMatch != null) {
                val nextJson = JSONObject(nextMatch.groupValues[1])
                val entity = nextJson.optJSONObject("props")
                    ?.optJSONObject("pageProps")
                    ?.optJSONObject("state")
                    ?.optJSONObject("data")
                    ?.optJSONObject("entity")
                entity?.optString("name")?.takeIf { it.isNotBlank() }?.let { collectionTitle = it }
                val trackList = entity?.optJSONArray("trackList") ?: JSONArray()
                val limit = trackList.length().coerceAtMost(20)
                for (i in 0 until limit) {
                    val item = trackList.optJSONObject(i) ?: continue
                    val title = item.optString("title", "Track")
                    val artist = item.optString("subtitle", "Spotify Artist")
                    val resolved = resolveFullStreamByQuery("$artist $title")
                    val audioUri = resolved?.first ?: item.optString("audioPreview")
                    val durMs = resolved?.second ?: item.optLong("duration", 180000L)
                    if (audioUri.isBlank()) continue
                    songs.add(
                        Song(
                            id = -Math.abs(("spotify_" + id + "_" + i).hashCode().toLong()) - 30_000L,
                            title = title,
                            artist = artist,
                            album = collectionTitle,
                            durationMs = durMs,
                            contentUri = Uri.parse(audioUri),
                            genre = "Spotify Public",
                            folder = "Streaming/Spotify",
                            streamingProvider = "spotify"
                        )
                    )
                }
            }
            return (customLabel.ifBlank { "Spotify: $collectionTitle" }) to songs
        } else {
            val token = credential.removePrefix("Bearer ").trim()
            if (token.isBlank()) throw Exception("Введите публичную ссылку или OAuth-токен Spotify")
            val req = Request.Builder()
                .url("https://api.spotify.com/v1/me/tracks?limit=25")
                .header("Authorization", "Bearer $token")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("Spotify API вернул HTTP ${resp.code}")
                val items = JSONObject(resp.body?.string() ?: "{}").optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val tr = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                    val title = tr.optString("name", "Spotify Track")
                    val artist = tr.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: "Artist"
                    val resolved = resolveFullStreamByQuery("$artist $title")
                    val uriStr = resolved?.first ?: tr.optString("preview_url")
                    if (uriStr.isBlank()) continue
                    val cover = tr.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                    songs.add(
                        Song(
                            id = -Math.abs(("sp_tk_" + tr.optString("id")).hashCode().toLong()) - 35_000L,
                            title = title,
                            artist = artist,
                            album = tr.optJSONObject("album")?.optString("name") ?: "Spotify Liked",
                            durationMs = resolved?.second ?: tr.optLong("duration_ms", 180000L),
                            contentUri = Uri.parse(uriStr),
                            coverUrl = cover,
                            genre = "Spotify",
                            folder = "Streaming/Spotify",
                            streamingProvider = "spotify"
                        )
                    )
                }
            }
            return (customLabel.ifBlank { "Spotify Аккаунт" }) to songs
        }
    }

    private suspend fun syncYandex(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val cleanToken = tokenInput.substringAfter("access_token=", tokenInput).substringBefore("&").trim()
        if (cleanToken.isBlank()) throw Exception("Введите OAuth-токен Яндекс Музыки")
        val yandexRepo = YandexMusicRepository()
        val resolvedUid = yandexRepo.fetchAccountUid(cleanToken)
        val ids = yandexRepo.getLikedTrackIds(cleanToken, resolvedUid).getOrElse { throw it }
        val metaList = yandexRepo.getTracksMetadata(cleanToken, ids.take(30)).getOrElse { emptyList() }
        val songs = metaList.map { yt ->
            val streamUrl = yandexRepo.getDownloadUrl(cleanToken, yt.id).getOrNull() ?: "https://api.music.yandex.net"
            Song(
                id = -Math.abs(("ym_" + yt.id).hashCode().toLong()) - 40_000L,
                title = yt.title,
                artist = yt.artist,
                album = yt.album,
                durationMs = yt.durationMs,
                contentUri = Uri.parse(streamUrl),
                coverUrl = yt.hdCoverUrl,
                genre = "Яндекс Музыка",
                folder = "YandexMusic",
                streamingProvider = "yandex"
            )
        }
        return (customLabel.ifBlank { "Яндекс Музыка" }) to songs
    }

    private fun syncSoundCloud(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val token = tokenInput.removePrefix("OAuth ").removePrefix("Bearer ").trim()
        if (token.isBlank()) throw Exception("Введите OAuth-токен SoundCloud")
        val req = Request.Builder()
            .url("https://api.soundcloud.com/me/likes/tracks?limit=25")
            .header("Authorization", "OAuth $token")
            .build()
        val songs = mutableListOf<Song>()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("SoundCloud API вернул HTTP ${resp.code}")
            val arr = JSONArray(resp.body?.string() ?: "[]")
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val title = item.optString("title", "SoundCloud Track")
                val artist = item.optJSONObject("user")?.optString("username") ?: "SoundCloud"
                val streamUrl = item.optString("stream_url").takeIf { it.isNotBlank() }
                    ?: resolveFullStreamByQuery("$artist $title")?.first
                    ?: continue
                songs.add(
                    Song(
                        id = -Math.abs(("sc_" + item.optLong("id")).hashCode().toLong()) - 50_000L,
                        title = title,
                        artist = artist,
                        album = "SoundCloud Likes",
                        durationMs = item.optLong("duration", 180000L),
                        contentUri = Uri.parse(streamUrl),
                        coverUrl = item.optString("artwork_url").takeIf { it.isNotBlank() },
                        genre = item.optString("genre", "SoundCloud"),
                        folder = "Streaming/SoundCloud",
                        streamingProvider = "soundcloud"
                    )
                )
            }
        }
        return (customLabel.ifBlank { "SoundCloud" }) to songs
    }

    private fun syncVk(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val token = tokenInput.substringAfter("access_token=", tokenInput).substringBefore("&").trim()
        if (token.isBlank()) throw Exception("Введите токен VK Audio")
        val req = Request.Builder()
            .url("https://api.vk.com/method/audio.get?access_token=" + URLEncoder.encode(token, "UTF-8") + "&v=5.131&count=25")
            .header("User-Agent", "KateMobileAndroid/56 lite-460 (Android 14; SDK 34)")
            .build()
        val songs = mutableListOf<Song>()
        httpClient.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw Exception("VK API вернул HTTP ${resp.code}")
            val items = JSONObject(resp.body?.string() ?: "{}").optJSONObject("response")?.optJSONArray("items") ?: JSONArray()
            for (i in 0 until items.length()) {
                val item = items.optJSONObject(i) ?: continue
                val url = item.optString("url")
                if (url.isBlank()) continue
                songs.add(
                    Song(
                        id = -Math.abs(("vk_" + item.optLong("id")).hashCode().toLong()) - 60_000L,
                        title = item.optString("title", "VK Track"),
                        artist = item.optString("artist", "VK Artist"),
                        album = "VK Музыка",
                        durationMs = item.optLong("duration", 180L) * 1000L,
                        contentUri = Uri.parse(url),
                        genre = "VK Audio",
                        folder = "Streaming/VK",
                        streamingProvider = "vk"
                    )
                )
            }
        }
        return (customLabel.ifBlank { "VK Музыка" }) to songs
    }
}
