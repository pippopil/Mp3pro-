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
                recommendedPreset = "Drum & Bass (DnB 174 BPM)"
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
                recommendedPreset = "Techno (Техно рамбл)"
            ),
            Song(
                id = -103L,
                title = "Neon Horizon",
                artist = "Vector Prime",
                album = "Cyberpunk 2088 OST",
                durationMs = 184000L,
                contentUri = Uri.parse("https://cdn.pixabay.com/download/audio/2021/08/04/audio_12b0c7443c.mp3?filename=electronic-future-beats-117997.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=800&auto=format&fit=crop&q=85",
                genre = "Synthwave / Cyberpunk",
                year = 2024,
                bitrate = "FLAC 24-bit 96kHz",
                isBuiltIn = true,
                recommendedPreset = "Electronic / EDM (Клубная)"
            ),
            Song(
                id = -104L,
                title = "Shadow Overdrive",
                artist = "Iron Resonance",
                album = "High Octane Distortion",
                durationMs = 215000L,
                contentUri = Uri.parse("https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=chill-abstract-intention-12099.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1498038432885-c6f3f1b912ee?w=800&auto=format&fit=crop&q=85",
                genre = "Heavy Rock / Metal",
                year = 2024,
                bitrate = "320 kbps MP3",
                isBuiltIn = true,
                recommendedPreset = "Rock (Рок)"
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
                recommendedPreset = "Deep Sub-Bass (Глубокий саб)"
            ),
            Song(
                id = -106L,
                title = "Acoustic Dawn",
                artist = "Clara Vance & Strings",
                album = "Unplugged Session",
                durationMs = 168000L,
                contentUri = Uri.parse("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3"),
                coverUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=800&auto=format&fit=crop&q=85",
                genre = "Acoustic & Vocal",
                year = 2025,
                bitrate = "FLAC 24-bit 48kHz",
                isBuiltIn = true,
                recommendedPreset = "Acoustic (Акустика)"
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
                recommendedPreset = "Lounge & Chillout (Лаунж отдых)"
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
}
