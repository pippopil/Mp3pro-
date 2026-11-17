package com.tohsoft.music.data.db

import android.content.Context
import com.tohsoft.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class TrackDatabase(private val context: Context) {

    private val prefs = context.getSharedPreferences("mp3pro_track_database", Context.MODE_PRIVATE)
    private val backupFile: File by lazy { File(context.filesDir, "track_database_backup.json") }

    private fun readRootObject(): JSONObject {
        val raw = prefs.getString("records_json", null)
        if (!raw.isNullOrBlank()) {
            try {
                return JSONObject(raw)
            } catch (_: Exception) {}
        }
        if (backupFile.exists()) {
            try {
                val diskRaw = backupFile.readText()
                if (diskRaw.isNotBlank()) return JSONObject(diskRaw)
            } catch (_: Exception) {}
        }
        return JSONObject()
    }

    private fun writeRootObject(root: JSONObject) {
        val serialized = root.toString()
        prefs.edit().putString("records_json", serialized).apply()
        try {
            backupFile.writeText(serialized)
        } catch (_: Exception) {}
    }

    fun incrementPlayCount(song: Song): Int {
        val root = readRootObject()
        val key = song.id.toString()
        val existing = root.optJSONObject(key) ?: JSONObject().apply {
            put("trackId", key)
            put("title", song.title)
            put("artist", song.artist)
            put("album", song.album)
            put("genre", song.genre)
            put("folder", song.folder)
            put("filePath", song.dataPath)
        }
        val nextCount = existing.optInt("playCount", song.playCount) + 1
        existing.put("playCount", nextCount)
        existing.put("lastPlayedAt", System.currentTimeMillis())
        root.put(key, existing)
        writeRootObject(root)
        return nextCount
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
        val root = readRootObject()
        val key = trackId.toString()
        val rec = root.optJSONObject(key) ?: JSONObject().apply {
            put("trackId", key)
            put("playCount", 0)
        }
        if (!title.isNullOrBlank()) rec.put("customTitle", title.trim())
        if (!artist.isNullOrBlank()) rec.put("customArtist", artist.trim())
        if (!album.isNullOrBlank()) rec.put("customAlbum", album.trim())
        if (!genre.isNullOrBlank()) rec.put("customGenre", genre.trim())
        if (notes != null) rec.put("notes", notes.trim())

        if (tags != null) {
            val cleanTags = tags.map { it.trim().removePrefix("#").lowercase() }
                .filter { it.isNotBlank() }
                .distinct()
            val arr = JSONArray()
            cleanTags.forEach { arr.put(it) }
            rec.put("tags", arr)
        }
        rec.put("updatedAt", System.currentTimeMillis())
        root.put(key, rec)
        writeRootObject(root)
    }

    fun getAllUniqueTags(): List<String> {
        val root = readRootObject()
        val set = linkedSetOf<String>()
        val keys = root.keys()
        while (keys.hasNext()) {
            val rec = root.optJSONObject(keys.next()) ?: continue
            val arr = rec.optJSONArray("tags") ?: continue
            for (i in 0 until arr.length()) {
                val tag = arr.optString(i).trim().lowercase()
                if (tag.isNotBlank()) set.add(tag)
            }
        }
        return set.toList().sorted()
    }

    fun applyRecordsToSongs(songs: List<Song>): List<Song> {
        val root = readRootObject()
        if (root.length() == 0) return songs

        return songs.map { song ->
            val rec = root.optJSONObject(song.id.toString()) ?: return@map song
            val customTitle = rec.optString("customTitle").takeIf { it.isNotBlank() }
            val customArtist = rec.optString("customArtist").takeIf { it.isNotBlank() }
            val customAlbum = rec.optString("customAlbum").takeIf { it.isNotBlank() }
            val customGenre = rec.optString("customGenre").takeIf { it.isNotBlank() }
            val notes = rec.optString("notes", song.customNotes)
            val playCount = rec.optInt("playCount", song.playCount)
            val lastPlayed = if (rec.has("lastPlayedAt")) rec.optLong("lastPlayedAt") else song.lastPlayedAt

            val tagList = mutableListOf<String>()
            val arr = rec.optJSONArray("tags")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val t = arr.optString(i).trim()
                    if (t.isNotBlank()) tagList.add(t)
                }
            }

            song.copy(
                title = customTitle ?: song.title,
                artist = customArtist ?: song.artist,
                album = customAlbum ?: song.album,
                genre = customGenre ?: song.genre,
                customNotes = notes,
                playCount = playCount,
                lastPlayedAt = lastPlayed,
                tags = if (tagList.isNotEmpty()) tagList else song.tags
            )
        }
    }
}
