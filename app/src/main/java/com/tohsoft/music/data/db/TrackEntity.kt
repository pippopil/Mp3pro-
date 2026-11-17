package com.tohsoft.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import org.json.JSONArray

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val folder: String = "",
    val filePath: String = "",
    val customTitle: String? = null,
    val customArtist: String? = null,
    val customAlbum: String? = null,
    val customGenre: String? = null,
    val notes: String = "",
    val playCount: Int = 0,
    val lastPlayedAt: Long? = null,
    val tags: List<String> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

class TrackTypeConverters {
    @TypeConverter
    fun fromTagsList(tags: List<String>?): String {
        val arr = JSONArray()
        tags?.forEach { t ->
            val clean = t.trim().lowercase()
            if (clean.isNotBlank()) arr.put(clean)
        }
        return arr.toString()
    }

    @TypeConverter
    fun toTagsList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            val result = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val item = arr.optString(i).trim().lowercase()
                if (item.isNotBlank()) result.add(item)
            }
            result.distinct()
        } catch (_: Exception) {
            raw.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }.distinct()
        }
    }
}
