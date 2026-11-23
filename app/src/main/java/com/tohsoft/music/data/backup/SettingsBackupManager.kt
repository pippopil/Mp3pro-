package com.tohsoft.music.data.backup

import android.content.Context
import android.net.Uri
import com.tohsoft.music.audio.AudiophileDSPProcessor
import org.json.JSONArray
import org.json.JSONObject

class SettingsBackupManager(private val context: Context) {

    companion object {
        const val SCHEMA_VERSION = 1
    }

    data class BackupPayload(
        val eqPreset: String,
        val eqBands: List<Int>,
        val preampDb: Float,
        val bassBoost: Int,
        val virtualizer: Int,
        val loudnessGain: Int,
        val isLimiterEnabled: Boolean,
        val isEqEnabled: Boolean,
        val blacklistedFolders: Set<String>,
        val visibleTabs: Set<String>,
        val theme: String,
        val rememberPlayback: Boolean,
        val fadeInOut: Boolean,
        val shakeToSkip: Boolean,
        val hideShortSongs: Boolean
    )

    fun serialize(payload: BackupPayload): String {
        val bandsArr = JSONArray()
        for (i in 0 until 10) {
            val gain = payload.eqBands.getOrElse(i) { 0 }.coerceIn(-15, 15)
            bandsArr.put(
                JSONObject().apply {
                    put("freq", AudiophileDSPProcessor.CENTER_FREQUENCIES_HZ[i].toInt())
                    put("gain", gain)
                }
            )
        }
        val blacklistArr = JSONArray().apply { payload.blacklistedFolders.forEach { put(it) } }
        val tabsArr = JSONArray().apply { payload.visibleTabs.forEach { put(it) } }

        return JSONObject().apply {
            put("version", SCHEMA_VERSION)
            put("app", "MP3 Player Pro")
            put("exportedAt", System.currentTimeMillis())
            put("name", payload.eqPreset)
            put("preamp", payload.preampDb.toDouble())
            put("bands", bandsArr)
            put("bassBoost", payload.bassBoost)
            put("virtualizer", payload.virtualizer)
            put("loudnessGain", payload.loudnessGain)
            put("isLimiterEnabled", payload.isLimiterEnabled)
            put("isEqEnabled", payload.isEqEnabled)
            put("blacklistedFolders", blacklistArr)
            put("visibleTabs", tabsArr)
            put("theme", payload.theme)
            put("rememberPlayback", payload.rememberPlayback)
            put("fadeInOut", payload.fadeInOut)
            put("shakeToSkip", payload.shakeToSkip)
            put("hideShortSongs", payload.hideShortSongs)
        }.toString(2)
    }

    fun parseAndValidate(rawJson: String): Result<BackupPayload> {
        return try {
            val obj = JSONObject(rawJson)
            val version = obj.optInt("version", -1)
            if (version < 1 || version > SCHEMA_VERSION) {
                return Result.failure(IllegalArgumentException("Неподдерживаемая версия формата: $version"))
            }
            val bandsArr = obj.optJSONArray("bands")
                ?: return Result.failure(IllegalArgumentException("Отсутствует массив полос bands"))
            val parsedBands = MutableList(10) { 0 }
            for (i in 0 until minOf(10, bandsArr.length())) {
                val item = bandsArr.opt(i)
                val gainVal = when (item) {
                    is JSONObject -> item.optInt("gain", 0)
                    is Number -> item.toInt()
                    else -> 0
                }
                parsedBands[i] = gainVal.coerceIn(-15, 15)
            }
            val blacklist = mutableSetOf<String>()
            obj.optJSONArray("blacklistedFolders")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank()) blacklist.add(s)
                }
            }
            val tabs = mutableSetOf<String>()
            obj.optJSONArray("visibleTabs")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank()) tabs.add(s)
                }
            }
            if (tabs.isEmpty()) {
                tabs.add("Песни")
                tabs.add("Настройки")
            }

            Result.success(
                BackupPayload(
                    eqPreset = obj.optString("name", "Пользовательский").ifBlank { "Пользовательский" },
                    eqBands = parsedBands,
                    preampDb = obj.optDouble("preamp", 0.0).toFloat().coerceIn(-15f, 15f),
                    bassBoost = obj.optInt("bassBoost", 25).coerceIn(0, 100),
                    virtualizer = obj.optInt("virtualizer", 15).coerceIn(0, 100),
                    loudnessGain = obj.optInt("loudnessGain", 0).coerceIn(-15, 15),
                    isLimiterEnabled = obj.optBoolean("isLimiterEnabled", true),
                    isEqEnabled = obj.optBoolean("isEqEnabled", true),
                    blacklistedFolders = blacklist,
                    visibleTabs = tabs,
                    theme = obj.optString("theme", "cosmic"),
                    rememberPlayback = obj.optBoolean("rememberPlayback", true),
                    fadeInOut = obj.optBoolean("fadeInOut", false),
                    shakeToSkip = obj.optBoolean("shakeToSkip", false),
                    hideShortSongs = obj.optBoolean("hideShortSongs", false)
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToUri(uri: Uri, payload: BackupPayload): Result<Unit> {
        return try {
            val json = serialize(payload)
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: return Result.failure(IllegalStateException("Не удалось открыть поток записи"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun importFromUri(uri: Uri): Result<BackupPayload> {
        return try {
            val raw = context.contentResolver.openInputStream(uri)?.use { input ->
                input.bufferedReader(Charsets.UTF_8).readText()
            } ?: return Result.failure(IllegalStateException("Не удалось прочитать файл"))
            parseAndValidate(raw)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
