package com.tohsoft.music.data.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.tohsoft.music.data.model.Song
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class StorageScanOutput(
    val songs: List<Song>,
    val scannedPaths: List<String>,
    val syncedDbCount: Int,
    val permissionMode: String
)

class StorageScanner(private val context: Context) {

    private val trackDbPrefs = context.getSharedPreferences("mp3pro_track_database", Context.MODE_PRIVATE)

    companion object {
        private val AUDIO_EXTS = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "opus")

        fun hasManageExternalStorage(): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true
            }
        }

        fun hasAudioScanPermission(context: Context): Boolean {
            if (hasManageExternalStorage()) return true
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        fun buildManageAllFilesIntent(context: Context): Intent? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
            if (Environment.isExternalStorageManager()) return null
            return Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.packageName)
            )
        }
    }

    fun collectPublicMusicRoots(): List<File> {
        val roots = linkedSetOf<File>()
        val extRoot = Environment.getExternalStorageDirectory()
        val publicMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        roots.add(File(publicMusic, "Downloaded"))
        roots.add(File(publicMusic, "YandexMusic/Мне нравится"))
        roots.add(File(publicMusic, "YandexMusic"))
        roots.add(publicMusic)
        context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { roots.add(it) }
        roots.add(publicDownloads)
        roots.add(File(publicDownloads, "YandexMusic"))
        roots.add(File(extRoot, "YandexMusic"))
        listOf("/storage/usbotg/Music", "/mnt/usb/Music", "/mnt/usb_storage").forEach { usb ->
            val dir = File(usb)
            if (dir.exists() && dir.isDirectory && dir.canRead()) roots.add(dir)
        }
        return roots.filter { it.exists() && it.isDirectory && it.canRead() }
    }

    fun scanPublicMusicDirectories(alreadyIndexedPaths: Set<String> = emptySet()): StorageScanOutput {
        val discovered = mutableListOf<Song>()
        val scannedPaths = mutableListOf<String>()
        val permissionMode = when {
            hasManageExternalStorage() -> "MANAGE_EXTERNAL_STORAGE"
            hasAudioScanPermission(context) -> "READ_MEDIA_AUDIO"
            else -> "NONE"
        }

        if (permissionMode == "NONE") {
            return StorageScanOutput(discovered, scannedPaths, 0, permissionMode)
        }

        collectPublicMusicRoots().forEach { rootDir ->
            try {
                rootDir.walkTopDown().maxDepth(6).forEach { file ->
                    if (!file.isFile || !file.canRead() || file.length() < 8192L) return@forEach
                    val ext = file.extension.lowercase()
                    if (ext !in AUDIO_EXTS) return@forEach

                    val absPath = file.absolutePath
                    if (absPath.lowercase() in alreadyIndexedPaths) return@forEach

                    var title = file.nameWithoutExtension
                    var artist = "Неизвестный исполнитель"
                    var album = file.parentFile?.name ?: "Music"
                    var durationMs = 180_000L

                    if (title.contains(" - ")) {
                        val parts = title.split(" - ", limit = 2)
                        artist = parts[0].trim().ifBlank { artist }
                        title = parts[1].trim().ifBlank { title }
                    }

                    val retriever = MediaMetadataRetriever()
                    try {
                        retriever.setDataSource(absPath)
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?.takeIf { it.isNotBlank() }?.let { title = it }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                            ?.takeIf { it.isNotBlank() }?.let { artist = it }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                            ?.takeIf { it.isNotBlank() }?.let { album = it }
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()?.let { dur -> if (dur >= 5000L) durationMs = dur }
                    } catch (_: Exception) {
                    } finally {
                        try { retriever.release() } catch (_: Exception) {}
                    }

                    val isYandex = absPath.contains("Yandex", ignoreCase = true) ||
                        absPath.contains("Мне нравится", ignoreCase = true)
                    val isUsb = absPath.contains("usbotg", ignoreCase = true) ||
                        absPath.contains("/mnt/usb", ignoreCase = true)
                    val folderLabel = when {
                        isYandex -> "YandexMusic"
                        isUsb -> "ChanganUSB"
                        else -> file.parentFile?.name ?: "Music"
                    }

                    discovered.add(
                        Song(
                            id = -Math.abs(absPath.hashCode().toLong()) - 1000L,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            contentUri = Uri.fromFile(file),
                            dataPath = absPath,
                            genre = if (isYandex) "Яндекс Музыка" else "Music",
                            folder = folderLabel,
                            isBuiltIn = false
                        )
                    )
                    scannedPaths.add(absPath)
                }
            } catch (_: Exception) {
            }
        }

        val synced = syncSongsToTrackDatabase(discovered)
        return StorageScanOutput(discovered, scannedPaths, synced, permissionMode)
    }

    private fun syncSongsToTrackDatabase(songs: List<Song>): Int {
        if (songs.isEmpty()) return 0
        val existingRaw = trackDbPrefs.getString("records_json", null)
        val rootObj = try {
            if (!existingRaw.isNullOrBlank()) JSONObject(existingRaw) else JSONObject()
        } catch (_: Exception) {
            JSONObject()
        }

        val now = System.currentTimeMillis()
        songs.forEach { song ->
            val key = song.id.toString()
            val prev = rootObj.optJSONObject(key)
            val tagSet = linkedSetOf<String>()
            val prevTags = prev?.optJSONArray("tags")
            if (prevTags != null) {
                for (i in 0 until prevTags.length()) {
                    val t = prevTags.optString(i).trim().lowercase()
                    if (t.isNotBlank()) tagSet.add(t)
                }
            }
            tagSet.add("scanned")
            tagSet.add("local-music")
            if (song.folder.equals("YandexMusic", ignoreCase = true)) tagSet.add("yandex-likes")
            if (song.folder.equals("Downloaded", ignoreCase = true)) tagSet.add("downloaded")
            val tagsJson = JSONArray()
            tagSet.forEach { tagsJson.put(it) }

            val rec = JSONObject().apply {
                put("trackId", key)
                put("title", prev?.optString("title")?.takeIf { it.isNotBlank() } ?: song.title)
                put("artist", prev?.optString("artist")?.takeIf { it.isNotBlank() } ?: song.artist)
                put("album", prev?.optString("album")?.takeIf { it.isNotBlank() } ?: song.album)
                put("genre", prev?.optString("genre")?.takeIf { it.isNotBlank() } ?: song.genre)
                put("notes", prev?.optString("notes") ?: song.customNotes)
                put("folder", song.folder)
                put("filePath", song.dataPath)
                put("playCount", prev?.optInt("playCount", 0) ?: 0)
                if (prev != null && prev.has("lastPlayedAt")) {
                    put("lastPlayedAt", prev.optLong("lastPlayedAt"))
                }
                put("tags", tagsJson)
                put("syncedAt", now)
            }
            rootObj.put(key, rec)
        }

        trackDbPrefs.edit().putString("records_json", rootObj.toString()).apply()
        return songs.size
    }
}
