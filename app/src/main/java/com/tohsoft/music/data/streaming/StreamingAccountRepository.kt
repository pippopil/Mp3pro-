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
    AUDIUS("audius", "Audius (Бесплатно)", true, "Войдите через браузер или введите @handle (пусто = Топ-треки, без API-ключей)"),
    SPOTIFY("spotify", "Spotify", true, "Войдите в Spotify для авто-перехвата API токена или откройте/вставьте ссылку на плейлист"),
    YANDEX("yandex", "Яндекс Музыка", false, "Войдите через Яндекс ID — токен перехватится автоматически, все треки пойдут потоково"),
    SOUNDCLOUD("soundcloud", "SoundCloud", false, "Войдите в SoundCloud — API-токен перехватится автоматически из сессии"),
    VK("vk", "VK Музыка", false, "Войдите через VK ID — токен VK Audio перехватится автоматически")
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
                for (i in 0 until trackList.length()) {
                    val item = trackList.optJSONObject(i) ?: continue
                    val title = item.optString("title", "Track")
                    val artist = item.optString("subtitle", "Spotify Artist")
                    val preview = item.optString("audioPreview", "")
                    val durMs = item.optLong("duration", 180000L)
                    val lazyUri = Uri.Builder()
                        .scheme("streamquery")
                        .authority("spotify")
                        .appendQueryParameter("q", "$artist $title")
                        .apply { if (preview.isNotBlank()) appendQueryParameter("fallback", preview) }
                        .build()
                    songs.add(
                        Song(
                            id = -Math.abs(("spotify_" + id + "_" + i).hashCode().toLong()) - 30_000L,
                            title = title,
                            artist = artist,
                            album = collectionTitle,
                            durationMs = durMs,
                            contentUri = lazyUri,
                            genre = "Spotify Public",
                            bitrate = "320 kbps Stream",
                            folder = "Streaming/Spotify",
                            streamingProvider = "spotify"
                        )
                    )
                }
            }
            return (customLabel.ifBlank { "Spotify: $collectionTitle" }) to songs
        } else {
            val token = credential.removePrefix("Bearer ").trim()
            if (token.isBlank()) throw Exception("Войдите в Spotify или вставьте публичную ссылку / OAuth-токен")
            com.tohsoft.music.data.security.SecureTokenStorage
                .getEncryptedPrefs(context, "mp3pro_spotify_secure_token")
                .edit()
                .putString("spotify_oauth_token", token)
                .apply()

            var offset = 0
            val pageSize = 50
            val maxTracks = 500
            while (songs.size < maxTracks) {
                val req = Request.Builder()
                    .url("https://api.spotify.com/v1/me/tracks?limit=$pageSize&offset=$offset")
                    .header("Authorization", "Bearer $token")
                    .build()
                val fetched = httpClient.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        if (offset == 0) throw Exception("Spotify API вернул HTTP ${resp.code}")
                        return@use 0
                    }
                    val items = JSONObject(resp.body?.string() ?: "{}").optJSONArray("items") ?: JSONArray()
                    for (i in 0 until items.length()) {
                        val tr = items.optJSONObject(i)?.optJSONObject("track") ?: continue
                        val title = tr.optString("name", "Spotify Track")
                        val artist = tr.optJSONArray("artists")?.optJSONObject(0)?.optString("name") ?: "Artist"
                        val preview = tr.optString("preview_url", "").takeUnless { it == "null" }.orEmpty()
                        val cover = tr.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url")
                        val lazyUri = Uri.Builder()
                            .scheme("streamquery")
                            .authority("spotify")
                            .appendQueryParameter("q", "$artist $title")
                            .apply { if (preview.isNotBlank()) appendQueryParameter("fallback", preview) }
                            .build()
                        songs.add(
                            Song(
                                id = -Math.abs(("sp_tk_" + tr.optString("id")).hashCode().toLong()) - 35_000L,
                                title = title,
                                artist = artist,
                                album = tr.optJSONObject("album")?.optString("name") ?: "Spotify Liked",
                                durationMs = tr.optLong("duration_ms", 180000L),
                                contentUri = lazyUri,
                                coverUrl = cover,
                                genre = "Spotify",
                                bitrate = "320 kbps Stream",
                                folder = "Streaming/Spotify",
                                streamingProvider = "spotify"
                            )
                        )
                    }
                    items.length()
                }
                if (fetched < pageSize) break
                offset += pageSize
            }
            return (customLabel.ifBlank { "Spotify Аккаунт" }) to songs
        }
    }

    private suspend fun syncYandex(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val yandexRepo = YandexMusicRepository()
        val cleanToken = yandexRepo.sanitizeToken(tokenInput)
        if (cleanToken.isBlank()) throw Exception("Введите OAuth-токен Яндекс Музыки")
        com.tohsoft.music.data.security.SecureTokenStorage
            .getEncryptedPrefs(context, "mp3pro_yandex_secure_token")
            .edit()
            .putString("yandex_oauth_token", cleanToken)
            .apply()
        val resolvedUid = yandexRepo.fetchAccountUidResult(cleanToken).getOrElse { throw it }
        val ids = yandexRepo.getLikedTrackIds(cleanToken, resolvedUid).getOrElse { throw it }
        val allMeta = mutableListOf<com.tohsoft.music.data.yandex.YandexTrack>()
        for (chunk in ids.chunked(100)) {
            val batch = yandexRepo.getTracksMetadata(cleanToken, chunk).getOrNull().orEmpty()
            allMeta.addAll(batch)
        }
        val songs = allMeta.map { yt ->
            Song(
                id = -Math.abs(("ym_" + yt.id).hashCode().toLong()) - 40_000L,
                title = yt.title,
                artist = yt.artist,
                album = yt.album,
                durationMs = yt.durationMs,
                contentUri = Uri.parse("yandex://track/${yt.id}"),
                coverUrl = yt.hdCoverUrl,
                genre = "Яндекс Музыка",
                year = yt.year ?: 2024,
                bitrate = "320 kbps Stream",
                folder = "YandexMusic",
                streamingProvider = "yandex"
            )
        }
        return (customLabel.ifBlank { "Яндекс Музыка" }) to songs
    }

    private fun syncSoundCloud(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val token = tokenInput.removePrefix("OAuth ").removePrefix("Bearer ").trim()
        if (token.isBlank()) throw Exception("Войдите в SoundCloud или введите OAuth-токен")
        com.tohsoft.music.data.security.SecureTokenStorage
            .getEncryptedPrefs(context, "mp3pro_sc_secure_token")
            .edit()
            .putString("soundcloud_oauth_token", token)
            .apply()

        val songs = mutableListOf<Song>()
        var accountName = customLabel.ifBlank { "SoundCloud" }

        // 1) Пробуем современный SoundCloud API v2 (работает с перехваченным oauth_token из WebView)
        try {
            val meReq = Request.Builder()
                .url("https://api-v2.soundcloud.com/me")
                .header("Authorization", "OAuth $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            val userId = httpClient.newCall(meReq).execute().use { resp ->
                if (resp.isSuccessful) {
                    val obj = JSONObject(resp.body?.string() ?: "{}")
                    if (customLabel.isBlank()) {
                        obj.optString("username").takeIf { it.isNotBlank() }?.let { accountName = "SoundCloud: $it" }
                    }
                    obj.optLong("id", 0L)
                } else 0L
            }
            val likesUrl = if (userId > 0L) {
                "https://api-v2.soundcloud.com/users/$userId/track_likes?limit=200"
            } else {
                "https://api-v2.soundcloud.com/me/track_likes?limit=200"
            }
            val v2Req = Request.Builder()
                .url(likesUrl)
                .header("Authorization", "OAuth $token")
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                .build()
            httpClient.newCall(v2Req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val collection = JSONObject(resp.body?.string() ?: "{}").optJSONArray("collection") ?: JSONArray()
                    for (i in 0 until collection.length()) {
                        val tr = collection.optJSONObject(i)?.optJSONObject("track") ?: continue
                        val tid = tr.optLong("id", 0L)
                        if (tid <= 0L) continue
                        val title = tr.optString("title", "SoundCloud Track")
                        val artist = tr.optJSONObject("user")?.optString("username") ?: "SoundCloud"
                        val cover = tr.optString("artwork_url", "")
                            .takeIf { it.isNotBlank() && it != "null" }
                            ?.replace("-large", "-t500x500")
                        val lazyUri = Uri.Builder()
                            .scheme("soundcloud")
                            .authority("track")
                            .appendPath(tid.toString())
                            .appendQueryParameter("q", "$artist $title")
                            .build()
                        songs.add(
                            Song(
                                id = -Math.abs(("sc_" + tid).hashCode().toLong()) - 50_000L,
                                title = title,
                                artist = artist,
                                album = "SoundCloud Likes",
                                durationMs = tr.optLong("duration", 180000L),
                                contentUri = lazyUri,
                                coverUrl = cover,
                                genre = tr.optString("genre", "SoundCloud").ifBlank { "SoundCloud" },
                                bitrate = "Stream HQ",
                                folder = "Streaming/SoundCloud",
                                streamingProvider = "soundcloud"
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2) Fallback на классический API v1, если v2 ничего не вернул
        if (songs.isEmpty()) {
            val req = Request.Builder()
                .url("https://api.soundcloud.com/me/likes/tracks?limit=200")
                .header("Authorization", "OAuth $token")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw Exception("SoundCloud API вернул HTTP ${resp.code}")
                val arr = JSONArray(resp.body?.string() ?: "[]")
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val tid = item.optLong("id", 0L)
                    val title = item.optString("title", "SoundCloud Track")
                    val artist = item.optJSONObject("user")?.optString("username") ?: "SoundCloud"
                    val lazyUri = Uri.Builder()
                        .scheme("soundcloud")
                        .authority("track")
                        .appendPath(tid.toString())
                        .appendQueryParameter("q", "$artist $title")
                        .build()
                    songs.add(
                        Song(
                            id = -Math.abs(("sc_" + tid).hashCode().toLong()) - 50_000L,
                            title = title,
                            artist = artist,
                            album = "SoundCloud Likes",
                            durationMs = item.optLong("duration", 180000L),
                            contentUri = lazyUri,
                            coverUrl = item.optString("artwork_url").takeIf { it.isNotBlank() && it != "null" },
                            genre = item.optString("genre", "SoundCloud"),
                            bitrate = "Stream HQ",
                            folder = "Streaming/SoundCloud",
                            streamingProvider = "soundcloud"
                        )
                    )
                }
            }
        }
        return accountName to songs
    }

    private fun syncVk(tokenInput: String, customLabel: String): Pair<String, List<Song>> {
        val token = tokenInput.substringAfter("access_token=", tokenInput).substringBefore("&").trim()
        if (token.isBlank()) throw Exception("Войдите через VK ID или введите токен VK Audio")
        com.tohsoft.music.data.security.SecureTokenStorage
            .getEncryptedPrefs(context, "mp3pro_vk_secure_token")
            .edit()
            .putString("vk_oauth_token", token)
            .apply()

        val songs = mutableListOf<Song>()
        var offset = 0
        val batchSize = 200
        val maxTracks = 600
        while (songs.size < maxTracks) {
            val req = Request.Builder()
                .url("https://api.vk.com/method/audio.get?access_token=" + URLEncoder.encode(token, "UTF-8") + "&v=5.131&count=$batchSize&offset=$offset")
                .header("User-Agent", "KateMobileAndroid/56 lite-460 (Android 14; SDK 34)")
                .build()
            val fetched = httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    if (offset == 0) throw Exception("VK API вернул HTTP ${resp.code}")
                    return@use 0
                }
                val root = JSONObject(resp.body?.string() ?: "{}")
                val errObj = root.optJSONObject("error")
                if (errObj != null) {
                    val msg = errObj.optString("error_msg", "Ошибка доступа VK Audio")
                    if (offset == 0) throw Exception("VK API: $msg")
                    return@use 0
                }
                val items = root.optJSONObject("response")?.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    val ownerId = item.optLong("owner_id", 0L)
                    val audioId = item.optLong("id", 0L)
                    val directUrl = item.optString("url", "")
                    if (directUrl.isBlank() && (ownerId == 0L || audioId == 0L)) continue
                    val title = item.optString("title", "VK Track")
                    val artist = item.optString("artist", "VK Artist")
                    val cover = item.optJSONObject("album")
                        ?.optJSONObject("thumb")
                        ?.let { th ->
                            th.optString("photo_600").takeIf { it.isNotBlank() }
                                ?: th.optString("photo_300").takeIf { it.isNotBlank() }
                        }
                    // Храним виртуальный vk:// URI, чтобы ссылка обновлялась при каждом запуске и не протухала
                    val trackUri = if (ownerId != 0L && audioId != 0L) {
                        Uri.Builder()
                            .scheme("vk")
                            .authority("track")
                            .appendPath("${ownerId}_${audioId}")
                            .apply { if (directUrl.isNotBlank()) appendQueryParameter("fallback", directUrl) }
                            .build()
                    } else {
                        Uri.parse(directUrl)
                    }
                    songs.add(
                        Song(
                            id = -Math.abs(("vk_" + ownerId + "_" + audioId).hashCode().toLong()) - 60_000L,
                            title = title,
                            artist = artist,
                            album = "VK Музыка",
                            durationMs = item.optLong("duration", 180L) * 1000L,
                            contentUri = trackUri,
                            coverUrl = cover,
                            genre = "VK Audio",
                            bitrate = "320 kbps Stream",
                            folder = "Streaming/VK",
                            streamingProvider = "vk"
                        )
                    )
                }
                items.length()
            }
            if (fetched < batchSize) break
            offset += batchSize
        }
        return (customLabel.ifBlank { "VK Музыка" }) to songs
    }

    /**
     * Ленивое разрешение потоковой ссылки в момент нажатия Play (чтобы ссылки VK / SoundCloud / Spotify не протухали).
     */
    suspend fun resolveLazyStreamUrl(song: Song): Result<String> = withContext(Dispatchers.IO) {
        try {
            val uri = song.contentUri
            when (uri.scheme?.lowercase()) {
                "vk" -> {
                    val ownerAndId = uri.lastPathSegment.orEmpty()
                    val fallbackUrl = uri.getQueryParameter("fallback").orEmpty()
                    val vkToken = com.tohsoft.music.data.security.SecureTokenStorage
                        .getEncryptedPrefs(context, "mp3pro_vk_secure_token")
                        .getString("vk_oauth_token", "")
                        .orEmpty()
                        .ifBlank {
                            loadSavedAccounts().firstOrNull { it.providerId == "vk" }?.credential.orEmpty()
                        }
                    if (vkToken.isNotBlank() && ownerAndId.isNotBlank()) {
                        val req = Request.Builder()
                            .url("https://api.vk.com/method/audio.getById?audios=" + URLEncoder.encode(ownerAndId, "UTF-8") + "&access_token=" + URLEncoder.encode(vkToken, "UTF-8") + "&v=5.131")
                            .header("User-Agent", "KateMobileAndroid/56 lite-460 (Android 14; SDK 34)")
                            .build()
                        val fresh = httpClient.newCall(req).execute().use { resp ->
                            if (resp.isSuccessful) {
                                JSONObject(resp.body?.string() ?: "{}")
                                    .optJSONArray("response")
                                    ?.optJSONObject(0)
                                    ?.optString("url")
                                    ?.takeIf { it.isNotBlank() }
                            } else null
                        }
                        if (!fresh.isNullOrBlank()) return@withContext Result.success(fresh)
                    }
                    if (fallbackUrl.isNotBlank()) return@withContext Result.success(fallbackUrl)
                    val alt = resolveFullStreamByQuery("${song.artist} ${song.title}")?.first
                    if (!alt.isNullOrBlank()) Result.success(alt)
                    else Result.failure(Exception("Не удалось получить свежую ссылку VK Audio"))
                }
                "soundcloud" -> {
                    val trackId = uri.lastPathSegment.orEmpty()
                    val query = uri.getQueryParameter("q").orEmpty().ifBlank { "${song.artist} ${song.title}" }
                    val scToken = com.tohsoft.music.data.security.SecureTokenStorage
                        .getEncryptedPrefs(context, "mp3pro_sc_secure_token")
                        .getString("soundcloud_oauth_token", "")
                        .orEmpty()
                        .ifBlank {
                            loadSavedAccounts().firstOrNull { it.providerId == "soundcloud" }?.credential.orEmpty()
                        }
                    if (scToken.isNotBlank() && trackId.isNotBlank() && trackId != "0") {
                        val infoReq = Request.Builder()
                            .url("https://api-v2.soundcloud.com/tracks/$trackId")
                            .header("Authorization", "OAuth $scToken")
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                            .build()
                        val transcodingEndpoint = httpClient.newCall(infoReq).execute().use { resp ->
                            if (!resp.isSuccessful) return@use null
                            val transcodings = JSONObject(resp.body?.string() ?: "{}")
                                .optJSONObject("media")
                                ?.optJSONArray("transcodings")
                                ?: return@use null
                            var progressive: String? = null
                            var hls: String? = null
                            for (i in 0 until transcodings.length()) {
                                val tc = transcodings.optJSONObject(i) ?: continue
                                val proto = tc.optJSONObject("format")?.optString("protocol").orEmpty()
                                val u = tc.optString("url").takeIf { it.isNotBlank() } ?: continue
                                if (proto == "progressive") progressive = u
                                else if (hls == null) hls = u
                            }
                            progressive ?: hls
                        }
                        if (!transcodingEndpoint.isNullOrBlank()) {
                            val streamReq = Request.Builder()
                                .url(transcodingEndpoint)
                                .header("Authorization", "OAuth $scToken")
                                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                                .build()
                            val signedUrl = httpClient.newCall(streamReq).execute().use { resp ->
                                if (resp.isSuccessful) {
                                    JSONObject(resp.body?.string() ?: "{}").optString("url").takeIf { it.isNotBlank() }
                                } else null
                            }
                            if (!signedUrl.isNullOrBlank()) return@withContext Result.success(signedUrl)
                        }
                    }
                    val alt = resolveFullStreamByQuery(query)?.first
                    if (!alt.isNullOrBlank()) Result.success(alt)
                    else Result.failure(Exception("Поток SoundCloud недоступен"))
                }
                "streamquery" -> {
                    val query = uri.getQueryParameter("q").orEmpty().ifBlank { "${song.artist} ${song.title}" }
                    val fallback = uri.getQueryParameter("fallback").orEmpty()
                    val full = resolveFullStreamByQuery(query)?.first
                    if (!full.isNullOrBlank()) Result.success(full)
                    else if (fallback.isNotBlank()) Result.success(fallback)
                    else Result.failure(Exception("Не удалось найти аудиопоток для ${song.title}"))
                }
                else -> Result.success(uri.toString())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
