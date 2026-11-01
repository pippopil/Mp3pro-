package com.tohsoft.music.data.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val albumArtUri: Uri? = null,
    val dataPath: String = "",
    val coverUrl: String? = null,
    val genre: String = "Музыка",
    val year: Int = 0,
    val bitrate: String = "320 kbps",
    val isBuiltIn: Boolean = false,
    val recommendedPreset: String = "Normal (Плоский)"
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            val sec = if (seconds < 10) "0$seconds" else "$seconds"
            return "$minutes:$sec"
        }
}
