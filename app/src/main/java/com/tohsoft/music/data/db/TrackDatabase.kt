package com.tohsoft.music.data.db

import android.content.Context
import com.tohsoft.music.data.model.Song
import org.json.JSONObject
import java.io.File

class TrackDatabase(private val context: Context) {

    private val dao: TrackDao by lazy { RoomTrackDatabase.getInstance(context).trackDao() }
    private val legacyPrefs = context.getSharedPreferences("mp3pro_track_database", Context.MODE_PRIVATE)
    private val backupFile: File by lazy { File(context.filesDir, "track_database_backup.json") }

    init {
        migrateLegacyJsonIfNeeded()
    }

    private fun migrateLegacyJsonIfNeeded() {
        if (legacyPrefs.getBoolean("migrated_to_room_v1", false)) return
        val raw = legacyPrefs.getString("records_json", null)
            ?: if (backupFile.exists()) try { backupFile.readText() } catch (_: Exception) { null } else null

        if (!raw.isNullOrBlank()) {
            try {
                val root = JSONObject(raw)
                val entities = mutableListOf<TrackEntity>()
                val keys = root.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val rec = root.optJSONObject(key) ?: continue
                    val id = key.toLongOrNull() ?: rec.optString("trackId").toLongOrNull() ?: continue
                    val tagList = mutableListOf<String>()
                    val arr = rec.optJSONArray("tags")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val t = arr.optString(i).trim().lowercase()
                            if (t.isNotBlank()) tagList.add(t)
                        }
                    }
                    entities.add(
                        TrackEntity(
                            trackId = id,
                            title = rec.optString("title", "Трек"),
                            artist = rec.optString("artist", "Неизвестный исполнитель"),
                            album = rec.optString("album", "Music"),
                            genre = rec.optString("genre", "Музыка"),
                            folder = rec.optString("folder", ""),
                            filePath = rec.optString("filePath", ""),
                            customTitle = rec.optString("customTitle").takeIf { it.isNotBlank() },
                            customArtist = rec.optString("customArtist").takeIf { it.isNotBlank() },
                            customAlbum = rec.optString("customAlbum").takeIf { it.isNotBlank() },
                            customGenre = rec.optString("customGenre").takeIf { it.isNotBlank() },
                            notes = rec.optString("notes", ""),
                            playCount = rec.optInt("playCount", 0),
                            lastPlayedAt = if (rec.has("lastPlayedAt")) rec.optLong("lastPlayedAt") else null,
                            tags = tagList.distinct(),
                            updatedAt = rec.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
                if (entities.isNotEmpty()) {
                    dao.insertOrReplaceAllSync(entities)
                }
            } catch (_: Exception) {}
        }
        legacyPrefs.edit().putBoolean("migrated_to_room_v1", true).apply()
    }

    fun incrementPlayCount(song: Song): Int {
        val now = System.currentTimeMillis()
        val existing = dao.getTrackByIdSync(song.id)
        if (existing == null) {
            val nextCount = song.playCount + 1
            dao.insertOrReplaceSync(
                TrackEntity(
                    trackId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    genre = song.genre,
                    folder = song.folder,
                    filePath = song.dataPath,
                    notes = song.customNotes,
                    playCount = nextCount,
                    lastPlayedAt = now,
                    tags = song.tags,
                    updatedAt = now
                )
            )
            return nextCount
        }
        dao.incrementPlayCountSync(song.id, now)
        return existing.playCount + 1
    }

    fun saveCustomMetadata(
        trackId: Long,
        title: String?,
        artist: String?,
        album: String?,
        genre: String?,
        notes: String?,
        tags: List<String>?
    ) {
        val now = System.currentTimeMillis()
        val existing = dao.getTrackByIdSync(trackId) ?: TrackEntity(
            trackId = trackId,
            title = title?.trim().orEmpty().ifBlank { "Трек" },
            artist = artist?.trim().orEmpty().ifBlank { "Исполнитель" },
            album = album?.trim().orEmpty().ifBlank { "Альбом" },
            genre = genre?.trim().orEmpty().ifBlank { "Музыка" }
        )
        val cleanTags = tags?.map { it.trim().removePrefix("#").lowercase() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?: existing.tags

        dao.insertOrReplaceSync(
            existing.copy(
                customTitle = title?.trim()?.takeIf { it.isNotBlank() } ?: existing.customTitle,
                customArtist = artist?.trim()?.takeIf { it.isNotBlank() } ?: existing.customArtist,
                customAlbum = album?.trim()?.takeIf { it.isNotBlank() } ?: existing.customAlbum,
                customGenre = genre?.trim()?.takeIf { it.isNotBlank() } ?: existing.customGenre,
                notes = notes?.trim() ?: existing.notes,
                tags = cleanTags,
                updatedAt = now
            )
        )
    }

    fun syncScannedSongs(songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val entities = songs.map { song ->
            val prev = dao.getTrackByIdSync(song.id)
            val tagSet = linkedSetOf<String>()
            prev?.tags?.forEach { if (it.isNotBlank()) tagSet.add(it.trim().lowercase()) }
            tagSet.add("scanned")
            tagSet.add("local-music")
            if (song.folder.equals("YandexMusic", ignoreCase = true)) tagSet.add("yandex-likes")
            if (song.folder.equals("Downloaded", ignoreCase = true)) tagSet.add("downloaded")

            TrackEntity(
                trackId = song.id,
                title = prev?.title?.takeIf { it.isNotBlank() } ?: song.title,
                artist = prev?.artist?.takeIf { it.isNotBlank() } ?: song.artist,
                album = prev?.album?.takeIf { it.isNotBlank() } ?: song.album,
                genre = prev?.genre?.takeIf { it.isNotBlank() } ?: song.genre,
                folder = song.folder,
                filePath = song.dataPath,
                customTitle = prev?.customTitle,
                customArtist = prev?.customArtist,
                customAlbum = prev?.customAlbum,
                customGenre = prev?.customGenre,
                notes = prev?.notes ?: song.customNotes,
                playCount = prev?.playCount ?: song.playCount,
                lastPlayedAt = prev?.lastPlayedAt ?: song.lastPlayedAt,
                tags = tagSet.toList(),
                updatedAt = now
            )
        }
        dao.insertOrReplaceAllSync(entities)
        return entities.size
    }

    fun getAllUniqueTags(): List<String> {
        val set = linkedSetOf<String>()
        dao.getAllTracksSync().forEach { entity ->
            entity.tags.forEach { t ->
                val clean = t.trim().lowercase()
                if (clean.isNotBlank()) set.add(clean)
            }
        }
        return set.toList().sorted()
    }

    fun applyRecordsToSongs(songs: List<Song>): List<Song> {
        val map = dao.getAllTracksSync().associateBy { it.trackId }
        if (map.isEmpty()) return songs

        return songs.map { song ->
            val rec = map[song.id] ?: return@map song
            song.copy(
                title = rec.customTitle ?: song.title,
                artist = rec.customArtist ?: song.artist,
                album = rec.customAlbum ?: song.album,
                genre = rec.customGenre ?: song.genre,
                customNotes = rec.notes,
                playCount = rec.playCount,
                lastPlayedAt = rec.lastPlayedAt ?: song.lastPlayedAt,
                tags = if (rec.tags.isNotEmpty()) rec.tags else song.tags
            )
        }
    }
}
