package com.tohsoft.music.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.tohsoft.music.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MusicRepository(private val context: Context) {

    companion object {
        // Встроенные эталонные треки для тестирования 10-полосного эквалайзера и стилей обложки
        val BUILT_IN_TEST_TRACKS = listOf(
            Song(
                id = -101L,
                title = "Neuro Pulse",
                artist = "Cybernetic Breakcore",
                album = "Sub-Bass Velocity 174",
                durationMs = 195000L,
                contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=800&auto=format&fit=crop&q=85",
                genre = "Drum & Bass (174 BPM)",
                year = 2025,
                bitrate = "320 kbps MP3 HQ",
                isBuiltIn = true,
                recommendedPreset = "Drum & Bass (DnB 174 BPM)",
                folder = "Music/DrumAndBass"
            ),
            Song(
                id = -102L,
                title = "Dark Matter Berlin",
                artist = "Untergrund 909",
                album = "Berghain Peak Time Vol. 4",
                durationMs = 228000L,
                contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?w=800&auto=format&fit=crop&q=85",
                genre = "Peak-Time Techno (138 BPM)",
                year = 2025,
                bitrate = "320 kbps MP3 HQ",
                isBuiltIn = true,
                recommendedPreset = "Techno (Техно рамбл)",
                folder = "Music/Techno"
            ),
            Song(
                id = -103L,
                title = "Neon Horizon (Yandex Music HQ)",
                artist = "Vector Prime",
                album = "Cyberpunk 2088 OST",
                durationMs = 184000L,
                contentUri = Uri.parse("https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3?filename=electronic-future-beats-117997.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=85",
                genre = "Synthwave / Cyberpunk",
                year = 2024,
                bitrate = "FLAC 24-bit 96kHz",
                isBuiltIn = true,
                recommendedPreset = "Electronic / EDM (Клубная)",
                folder = "YandexMusic"
            ),
            Song(
                id = -104L,
                title = "Shadow Overdrive (Yandex Music HQ)",
                artist = "Iron Resonance",
                album = "High Octane Distortion",
                durationMs = 215000L,
                contentUri = Uri.parse("https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=chill-abstract-intention-12099.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800&auto=format&fit=crop&q=85",
                genre = "Heavy Rock / Metal",
                year = 2024,
                bitrate = "320 kbps MP3",
                isBuiltIn = true,
                recommendedPreset = "Rock (Рок)",
                folder = "YandexMusic"
            ),
            Song(
                id = -105L,
                title = "Subzero 808",
                artist = "808 Phantom",
                album = "Low End Theory 2.0",
                durationMs = 176000L,
                contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=800&auto=format&fit=crop&q=85",
                genre = "Trap / Heavy Bass",
                year = 2024,
                bitrate = "320 kbps MP3",
                isBuiltIn = true,
                recommendedPreset = "Deep Sub-Bass (Глубокий саб)",
                folder = "Download"
            ),
            Song(
                id = -106L,
                title = "Acoustic Dawn (Yandex Music HQ)",
                artist = "Clara Vance & Strings",
                album = "Unplugged Session",
                durationMs = 168000L,
                contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800&auto=format&fit=crop&q=85",
                genre = "Acoustic & Vocal",
                year = 2025,
                bitrate = "FLAC 24-bit 48kHz",
                isBuiltIn = true,
                recommendedPreset = "Acoustic (Акустика)",
                folder = "YandexMusic"
            ),
            Song(
                id = -107L,
                title = "Midnight Coffee & Vinyl",
                artist = "Solitude Lounge",
                album = "Rainy City Nights",
                durationMs = 198000L,
                contentUri = Uri.parse("https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=800&auto=format&fit=crop&q=85",
                genre = "Lo-Fi Chillhop / Jazz",
                year = 2023,
                bitrate = "320 kbps MP3",
                isBuiltIn = true,
                recommendedPreset = "Lounge & Chillout (Лаунж отдых)",
                folder = "Download"
            )
        )
    }

    suspend fun loadDeviceSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()

        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
            val collectionUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

            context.contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Неизвестный трек"
                    val artist = cursor.getString(artistCol) ?: "Неизвестный исполнитель"
                    val album = cursor.getString(albumCol) ?: "Неизвестный альбом"
                    val duration = cursor.getLong(durationCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val path = cursor.getString(dataCol) ?: ""

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val resolvedFolder = if (path.isNotBlank()) {
                        val parent = path.substringBeforeLast('/', "")
                        if (parent.contains("ru.yandex.music", ignoreCase = true) || parent.contains("YandexMusic", ignoreCase = true)) {
                            "YandexMusic"
                        } else if (parent.contains("Download", ignoreCase = true)) {
                            "Download"
                        } else {
                            parent.substringAfterLast('/').ifBlank { "Music" }
                        }
                    } else {
                        "Music"
                    }

                    songList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            dataPath = path,
                            genre = "Локальный файл",
                            folder = resolvedFolder,
                            isBuiltIn = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Добавляем встроенные тестовые треки в самое начало списка
        songList.addAll(0, BUILT_IN_TEST_TRACKS)
        songList
    }

    private val prefs = context.getSharedPreferences("mp3pro_playlists_store", Context.MODE_PRIVATE)

    fun loadUserPlaylists(): List<com.tohsoft.music.data.model.UserPlaylist> {
        val json = prefs.getString("user_playlists_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val array = org.json.JSONArray(json)
                val list = mutableListOf<com.tohsoft.music.data.model.UserPlaylist>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val songIdsArray = obj.optJSONArray("songIds")
                    val songIds = mutableListOf<Long>()
                    if (songIdsArray != null) {
                        for (j in 0 until songIdsArray.length()) {
                            songIds.add(songIdsArray.getLong(j))
                        }
                    }
                    list.add(
                        com.tohsoft.music.data.model.UserPlaylist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            icon = obj.optString("icon", "🎵"),
                            colorHex = obj.optString("colorHex", "#00C996"),
                            songIds = songIds,
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // Дефолтные готовые плейлисты для Changan, Яндекс Музыки и Избранного
        val defaultList = listOf(
            com.tohsoft.music.data.model.UserPlaylist(
                id = "pl-yandex",
                name = "🟡 Яндекс Музыка (Офлайн)",
                icon = "🟡",
                colorHex = "#FFCC00",
                songIds = listOf(-103L, -104L, -106L)
            ),
            com.tohsoft.music.data.model.UserPlaylist(
                id = "pl-changan",
                name = "🚗 В авто Changan UNI-S",
                icon = "🚗",
                colorHex = "#00C996",
                songIds = listOf(-101L, -102L, -105L)
            ),
            com.tohsoft.music.data.model.UserPlaylist(
                id = "pl-favorites",
                name = "❤️ Избранное (Любимые)",
                icon = "❤️",
                colorHex = "#E11D48",
                songIds = listOf(-101L, -103L)
            )
        )
        saveUserPlaylists(defaultList)
        return defaultList
    }

    fun saveUserPlaylists(playlists: List<com.tohsoft.music.data.model.UserPlaylist>) {
        try {
            val array = org.json.JSONArray()
            for (pl in playlists) {
                val obj = org.json.JSONObject()
                obj.put("id", pl.id)
                obj.put("name", pl.name)
                obj.put("icon", pl.icon)
                obj.put("colorHex", pl.colorHex)
                obj.put("createdAt", pl.createdAt)
                val songsArr = org.json.JSONArray()
                pl.songIds.forEach { songsArr.put(it) }
                obj.put("songIds", songsArr)
                array.put(obj)
            }
            prefs.edit().putString("user_playlists_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
