package com.tohsoft.music.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.tohsoft.music.audio.EqualizerManager
import com.tohsoft.music.data.CoverArtRepository
import com.tohsoft.music.data.model.Song
import com.tohsoft.music.data.repository.MusicRepository
import com.tohsoft.music.data.yandex.YandexMusicRepository
import com.tohsoft.music.ui.components.CoverStyle
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request

enum class TrackSortMode(val label: String) {
    TITLE_ASC("По названию (А-Я)"),
    ARTIST("По артисту"),
    ALBUM("По альбому"),
    FOLDER("По папкам"),
    DURATION("По длительности")
}

data class PlayerUiState(
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0,
    val durationMs: Long = 0,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val sleepTimerMinutesLeft: Int? = null,
    val searchQuery: String = "",
    val activeTab: Int = 0, // 0 = ПЕСНИ, 1 = НАСТРОЙКИ
    val isFullPlayerOpen: Boolean = false,
    val currentTheme: String = "cosmic",
    val isSongMenuOpen: Boolean = false,
    val selectedSongForMenu: Song? = null,
    val isSleepTimerDialogOpen: Boolean = false,
    val isHideTabsDialogOpen: Boolean = false,
    val isPropertiesDialogOpen: Boolean = false,
    val isThemePickerOpen: Boolean = false,
    val isWallpaperPickerOpen: Boolean = false,
    val isBlacklistFoldersDialogOpen: Boolean = false,
    val isLanguageDialogOpen: Boolean = false,
    val isSmartShareDialogOpen: Boolean = false,
    val blacklistedFolders: Set<String> = setOf("WhatsApp Audio", "Voice Notes", "Notifications"),
    val visibleTabs: Set<String> = setOf("Песни", "Настройки"),
    val currentLanguage: String = "Русский (Россия)",
    val isScanningStorage: Boolean = false,
    val statusToastMessage: String? = null,
    val rememberPlayback: Boolean = true,
    val lockScreenPlayer: Boolean = false,
    val fadeInOut: Boolean = false,
    val shakeToSkip: Boolean = false,
    val gridAlbumView: Boolean = false,
    val hideShortSongs: Boolean = false,
    val isLoading: Boolean = true,
    // Equalizer & Audio FX State (Poweramp Pro Dual-Mode)
    val isEqualizerOpen: Boolean = false,
    val isEqEnabled: Boolean = true,
    val isParametricMode: Boolean = false,
    val isAutoEqEnabled: Boolean = true,
    val eqPreampDb: Float = 0.0f,
    val isLimiterEnabled: Boolean = true,
    val trebleLevel: Int = 35,
    val activeEqPreset: String = "Normal (Плоский)",
    val eqBands: List<Int> = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
    val bassBoost: Int = 25,
    val virtualizer: Int = 15,
    val loudnessGain: Int = 0,
    val reverbPreset: Short = 0,
    val audioSessionId: Int = 0,
    // Visual Cover Art & Online Integration State
    val coverStyle: CoverStyle = CoverStyle.VINYL_RECORD,
    val isYandexModalOpen: Boolean = false,
    val isYandexSyncing: Boolean = false,
    val yandexSyncProgress: Int = 0,
    val yandexSyncStatus: String = "",
    val isSearchingCover: Boolean = false,
    val coverSearchMessage: String = "",
    val activeGenreFilter: String = "Все",
    val sortMode: TrackSortMode = TrackSortMode.TITLE_ASC,
    val isOnlineSearchOpen: Boolean = false,
    val onlineSearchQuery: String = "",
    val isSearchingOnline: Boolean = false,
    val onlineSearchResults: List<CoverArtRepository.OnlineMusicTrack> = emptyList(),
    val onlineSearchError: String? = null,
    val onlineDownloadProgress: Int = 0,
    val onlineDownloadMessage: String = ""
)

enum class RepeatMode {
    NONE, ALL, ONE
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs: SharedPreferences = application.getSharedPreferences("mp3_player_pro_prefs", Context.MODE_PRIVATE)
    private val repository = MusicRepository(application)
    private val coverArtRepository = CoverArtRepository(application)
    private val yandexRepository = YandexMusicRepository()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(application)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var equalizerManager: EqualizerManager? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var originalExoVolume: Float = 1.0f
    private var lastBoundSessionId: Int = 0

    init {
        loadSavedPreferences()
        setupPlayerListener()
        loadSongs()
        try {
            equalizerManager = EqualizerManager(exoPlayer.audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSavedPreferences() {
        val savedRemember = prefs.getBoolean("rememberPlayback", true)
        val savedLockScreen = prefs.getBoolean("lockScreenPlayer", false)
        val savedFade = prefs.getBoolean("fadeInOut", false)
        val savedShake = prefs.getBoolean("shakeToSkip", false)
        val savedGrid = prefs.getBoolean("gridAlbumView", false)
        val savedHideShort = prefs.getBoolean("hideShortSongs", false)
        val savedTheme = prefs.getString("currentTheme", "cosmic") ?: "cosmic"
        val savedLang = prefs.getString("currentLanguage", "Русский (Россия)") ?: "Русский (Россия)"
        val defaultBlacklist = setOf("WhatsApp Audio", "Voice Notes", "Notifications")
        val savedBlacklist = prefs.getStringSet("blacklistedFolders", defaultBlacklist) ?: defaultBlacklist
        val defaultTabs = setOf("Песни", "Настройки")
        val savedTabs = prefs.getStringSet("visibleTabs", defaultTabs) ?: defaultTabs

        _uiState.update {
            it.copy(
                rememberPlayback = savedRemember,
                lockScreenPlayer = savedLockScreen,
                fadeInOut = savedFade,
                shakeToSkip = savedShake,
                gridAlbumView = savedGrid,
                hideShortSongs = savedHideShort,
                currentTheme = savedTheme,
                currentLanguage = savedLang,
                blacklistedFolders = savedBlacklist,
                visibleTabs = savedTabs
            )
        }
    }

    private fun setupPlayerListener() {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) {
                    startProgressTracker()
                    val sessionId = exoPlayer.audioSessionId
                    if (sessionId > 0) {
                        bindAudioSession(sessionId)
                    }
                } else {
                    progressJob?.cancel()
                    if (_uiState.value.rememberPlayback && _uiState.value.currentSong != null) {
                        prefs.edit().putLong("lastPositionMs", exoPlayer.currentPosition).apply()
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    val sessionId = exoPlayer.audioSessionId
                    if (sessionId > 0) {
                        bindAudioSession(sessionId)
                    }
                }
                if (playbackState == Player.STATE_ENDED) {
                    if (_uiState.value.repeatMode == RepeatMode.ONE) {
                        exoPlayer.seekTo(0)
                        exoPlayer.play()
                    } else {
                        nextSong()
                    }
                }
            }
        })
    }

    private fun bindAudioSession(sessionId: Int) {
        if (sessionId > 0 && sessionId != lastBoundSessionId) {
            lastBoundSessionId = sessionId
            equalizerManager?.attachAudioSession(sessionId)

            equalizerManager?.applyPreset(_uiState.value.activeEqPreset)
            equalizerManager?.setBassBoostStrength(_uiState.value.bassBoost)
            equalizerManager?.setVirtualizerStrength(_uiState.value.virtualizer)
            equalizerManager?.setLoudnessGain(_uiState.value.loudnessGain)
            equalizerManager?.setEnabled(_uiState.value.isEqEnabled)

            try {
                val intent = Intent(AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, sessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getApplication<Application>().packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }
                getApplication<Application>().sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun unbindAudioSession() {
        if (lastBoundSessionId > 0) {
            try {
                val intent = Intent(AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, lastBoundSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getApplication<Application>().packageName)
                }
                getApplication<Application>().sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            lastBoundSessionId = 0
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                val pos = exoPlayer.currentPosition
                _uiState.update {
                    it.copy(
                        currentPositionMs = pos,
                        durationMs = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                if (_uiState.value.rememberPlayback && _uiState.value.currentSong != null) {
                    prefs.edit().putLong("lastPositionMs", pos).apply()
                }
                delay(500)
            }
        }
    }

    private fun filterAndProcessSongs(rawSongs: List<Song>, hideShort: Boolean, blacklisted: Set<String>): List<Song> {
        return rawSongs.filter { song ->
            if (hideShort && song.durationMs in 1..29999) {
                return@filter false
            }
            val fName = song.folderName.lowercase()
            for (bl in blacklisted) {
                if (bl.isNotBlank() && fName.contains(bl.lowercase())) {
                    return@filter false
                }
            }
            true
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val rawList = repository.loadDeviceSongs()
            val listWithCovers = rawList.map { song ->
                if (song.coverUrl == null && (song.albumArtUri == null || song.albumArtUri.toString().isBlank())) {
                    val gen = coverArtRepository.generateProceduralCover(song.id, song.title, song.artist)
                    song.copy(coverUrl = gen.toString())
                } else {
                    song
                }
            }

            val filtered = filterAndProcessSongs(listWithCovers, _uiState.value.hideShortSongs, _uiState.value.blacklistedFolders)
            val sortedFiltered = sortTrackList(filtered, _uiState.value.sortMode)

            val lastSongId = prefs.getLong("lastSongId", -1L)
            val lastPos = prefs.getLong("lastPositionMs", 0L)
            val restoredSong = if (_uiState.value.rememberPlayback && lastSongId > 0) {
                sortedFiltered.firstOrNull { it.id == lastSongId }
            } else null

            _uiState.update {
                it.copy(
                    songs = listWithCovers,
                    filteredSongs = sortedFiltered,
                    currentSong = restoredSong ?: it.currentSong ?: sortedFiltered.firstOrNull(),
                    currentPositionMs = if (restoredSong != null) lastPos else 0L,
                    durationMs = restoredSong?.durationMs ?: (sortedFiltered.firstOrNull()?.durationMs ?: 0L),
                    isLoading = false
                )
            }

            if (restoredSong != null && lastPos > 0) {
                try {
                    exoPlayer.setMediaItem(MediaItem.fromUri(restoredSong.contentUri))
                    exoPlayer.prepare()
                    exoPlayer.seekTo(lastPos)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun rescanStorage() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningStorage = true) }
            val rawList = repository.loadDeviceSongs()
            val listWithCovers = rawList.map { song ->
                if (song.coverUrl == null && (song.albumArtUri == null || song.albumArtUri.toString().isBlank())) {
                    val gen = coverArtRepository.generateProceduralCover(song.id, song.title, song.artist)
                    song.copy(coverUrl = gen.toString())
                } else {
                    song
                }
            }
            val filtered = filterAndProcessSongs(listWithCovers, _uiState.value.hideShortSongs, _uiState.value.blacklistedFolders)
            val sorted = sortTrackList(filtered, _uiState.value.sortMode)
            _uiState.update {
                it.copy(
                    songs = listWithCovers,
                    filteredSongs = sorted,
                    isScanningStorage = false,
                    statusToastMessage = "Сканирование завершено: найдено ${sorted.size} треков"
                )
            }
        }
    }

    fun playSong(song: Song) {
        try {
            val mediaItem = MediaItem.fromUri(song.contentUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()

            if (_uiState.value.fadeInOut) {
                exoPlayer.volume = 0.0f
                exoPlayer.play()
                viewModelScope.launch {
                    val steps = 10
                    for (i in 1..steps) {
                        delay(120)
                        exoPlayer.volume = (i / steps.toFloat()).coerceIn(0f, 1f)
                    }
                }
            } else {
                exoPlayer.volume = 1.0f
                exoPlayer.play()
            }

            val sessionId = exoPlayer.audioSessionId
            if (sessionId > 0) {
                bindAudioSession(sessionId)
            }

            _uiState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    currentPositionMs = 0,
                    durationMs = song.durationMs
                )
            }

            if (_uiState.value.rememberPlayback) {
                prefs.edit().putLong("lastSongId", song.id).putLong("lastPositionMs", 0L).apply()
            }

            if (_uiState.value.isAutoEqEnabled) {
                autoApplyEqPresetForSong(song)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            if (_uiState.value.rememberPlayback) {
                prefs.edit().putLong("lastPositionMs", exoPlayer.currentPosition).apply()
            }
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _uiState.value.currentSong != null) {
                playSong(_uiState.value.currentSong!!)
            } else {
                exoPlayer.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _uiState.update { it.copy(currentPositionMs = positionMs) }
        if (_uiState.value.rememberPlayback) {
            prefs.edit().putLong("lastPositionMs", positionMs).apply()
        }
    }

    fun nextSong() {
        val state = _uiState.value
        val list = state.filteredSongs
        if (list.isEmpty()) return

        val nextIndex = if (state.isShuffle) {
            (list.indices).random()
        } else {
            val currentIdx = list.indexOfFirst { it.id == state.currentSong?.id }
            if (currentIdx != -1 && currentIdx + 1 < list.size) currentIdx + 1 else 0
        }
        playSong(list[nextIndex])
    }

    fun prevSong() {
        val state = _uiState.value
        val list = state.filteredSongs
        if (list.isEmpty()) return

        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
            return
        }

        val currentIdx = list.indexOfFirst { it.id == state.currentSong?.id }
        val prevIndex = if (currentIdx > 0) currentIdx - 1 else list.size - 1
        playSong(list[prevIndex])
    }

    fun toggleShuffle() {
        _uiState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        val nextMode = when (_uiState.value.repeatMode) {
            RepeatMode.NONE -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.NONE
        }
        _uiState.update { it.copy(repeatMode = nextMode) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                filterAndProcessSongs(state.songs, state.hideShortSongs, state.blacklistedFolders)
            } else {
                filterAndProcessSongs(state.songs, state.hideShortSongs, state.blacklistedFolders).filter {
                    it.title.contains(query, ignoreCase = true) ||
                    it.artist.contains(query, ignoreCase = true) ||
                    it.album.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredSongs = sortTrackList(filtered, state.sortMode))
        }
    }

    fun setTab(index: Int) {
        _uiState.update { it.copy(activeTab = index) }
    }

    fun openFullPlayer() {
        _uiState.update { it.copy(isFullPlayerOpen = true) }
    }

    fun closeFullPlayer() {
        _uiState.update { it.copy(isFullPlayerOpen = false) }
    }

    fun openSongMenu(song: Song) {
        _uiState.update { it.copy(isSongMenuOpen = true, selectedSongForMenu = song) }
    }

    fun closeSongMenu() {
        _uiState.update { it.copy(isSongMenuOpen = false, selectedSongForMenu = null) }
    }

    fun openSleepTimerDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isSleepTimerDialogOpen = isOpen) }
    }

    fun startSleepTimer(seconds: Int) {
        sleepTimerJob?.cancel()
        originalExoVolume = exoPlayer.volume
        val initialMins = Math.max(1, Math.ceil(seconds / 60.0).toInt())
        _uiState.update { it.copy(sleepTimerMinutesLeft = initialMins, isSleepTimerDialogOpen = false) }

        sleepTimerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                val minsLeft = Math.max(0, Math.ceil(remaining / 60.0).toInt())
                _uiState.update { it.copy(sleepTimerMinutesLeft = minsLeft) }

                // Плавное снижение громкости трека за последние 5 секунд (Crossfade 5 сек)
                if (remaining in 1..5) {
                    val fadeRatio = remaining / 5.0f
                    exoPlayer.volume = (originalExoVolume * fadeRatio).coerceIn(0f, 1f)
                }
            }

            exoPlayer.pause()
            delay(150)
            exoPlayer.volume = originalExoVolume
            _uiState.update { it.copy(sleepTimerMinutesLeft = null) }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        exoPlayer.volume = originalExoVolume
        _uiState.update { it.copy(sleepTimerMinutesLeft = null, isSleepTimerDialogOpen = false) }
    }

    fun openHideTabsDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isHideTabsDialogOpen = isOpen) }
    }

    fun toggleVisibleTab(tabName: String) {
        val current = _uiState.value.visibleTabs.toMutableSet()
        if (current.contains(tabName)) {
            if (current.size > 1) current.remove(tabName)
        } else {
            current.add(tabName)
        }
        prefs.edit().putStringSet("visibleTabs", current).apply()
        _uiState.update { it.copy(visibleTabs = current) }
    }

    fun openPropertiesDialog(isOpen: Boolean, song: Song? = null) {
        _uiState.update { it.copy(isPropertiesDialogOpen = isOpen, selectedSongForMenu = song ?: it.currentSong) }
    }

    fun openThemePicker(isOpen: Boolean) {
        _uiState.update { it.copy(isThemePickerOpen = isOpen) }
    }

    fun setTheme(theme: String) {
        prefs.edit().putString("currentTheme", theme).apply()
        _uiState.update { it.copy(currentTheme = theme, isThemePickerOpen = false) }
    }

    fun openWallpaperPicker(isOpen: Boolean) {
        _uiState.update { it.copy(isWallpaperPickerOpen = isOpen) }
    }

    fun openBlacklistFoldersDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isBlacklistFoldersDialogOpen = isOpen) }
    }

    fun toggleBlacklistFolder(folderName: String) {
        val current = _uiState.value.blacklistedFolders.toMutableSet()
        if (current.contains(folderName)) {
            current.remove(folderName)
        } else {
            current.add(folderName)
        }
        prefs.edit().putStringSet("blacklistedFolders", current).apply()
        _uiState.update { it.copy(blacklistedFolders = current) }
        val filtered = filterAndProcessSongs(_uiState.value.songs, _uiState.value.hideShortSongs, current)
        _uiState.update { it.copy(filteredSongs = sortTrackList(filtered, it.sortMode)) }
    }

    fun openLanguageDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isLanguageDialogOpen = isOpen) }
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("currentLanguage", language).apply()
        _uiState.update { it.copy(currentLanguage = language, isLanguageDialogOpen = false) }
    }

    fun openSmartShareDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isSmartShareDialogOpen = isOpen) }
    }

    fun clearStatusToast() {
        _uiState.update { it.copy(statusToastMessage = null) }
    }

    fun toggleSetting(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        _uiState.update {
            when (key) {
                "rememberPlayback" -> it.copy(rememberPlayback = value)
                "lockScreenPlayer" -> it.copy(lockScreenPlayer = value)
                "fadeInOut" -> it.copy(fadeInOut = value)
                "shakeToSkip" -> it.copy(shakeToSkip = value)
                "gridAlbumView" -> it.copy(gridAlbumView = value)
                "hideShortSongs" -> {
                    val updatedFiltered = filterAndProcessSongs(it.songs, value, it.blacklistedFolders)
                    it.copy(hideShortSongs = value, filteredSongs = sortTrackList(updatedFiltered, it.sortMode))
                }
                else -> it
            }
        }
    }

    fun openEqualizer() {
        _uiState.update { it.copy(isEqualizerOpen = true) }
    }

    fun closeEqualizer() {
        _uiState.update { it.copy(isEqualizerOpen = false) }
    }

    fun toggleEqEnabled(enabled: Boolean) {
        equalizerManager?.setEnabled(enabled)
        _uiState.update { it.copy(isEqEnabled = enabled) }
    }

    fun applyEqPreset(presetName: String) {
        equalizerManager?.applyPreset(presetName)
        val bands = equalizerManager?.getAllBandLevels() ?: _uiState.value.eqBands
        _uiState.update { it.copy(activeEqPreset = presetName, eqBands = bands) }
    }

    fun setEqBand(band: Int, level: Int) {
        equalizerManager?.setBandLevel(band, level)
        val updated = _uiState.value.eqBands.toMutableList()
        if (band in updated.indices) {
            updated[band] = level
        }
        _uiState.update { it.copy(eqBands = updated, activeEqPreset = "Пользовательский") }
    }

    fun setBassBoost(strength: Int) {
        equalizerManager?.setBassBoostStrength(strength)
        _uiState.update { it.copy(bassBoost = strength) }
    }

    fun setVirtualizer(strength: Int) {
        equalizerManager?.setVirtualizerStrength(strength)
        _uiState.update { it.copy(virtualizer = strength) }
    }

    fun setLoudness(gain: Int) {
        equalizerManager?.setLoudnessGain(gain)
        _uiState.update { it.copy(loudnessGain = gain) }
    }

    fun setReverb(preset: Short) {
        equalizerManager?.setReverbPreset(preset)
        _uiState.update { it.copy(reverbPreset = preset) }
    }

    fun resetEq() {
        applyEqPreset("Normal (Плоский)")
        setBassBoost(0)
        setVirtualizer(0)
        setLoudness(0)
        setReverb(0)
    }

    private fun sortTrackList(list: List<Song>, mode: TrackSortMode): List<Song> {
        return when (mode) {
            TrackSortMode.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            TrackSortMode.ARTIST -> list.sortedBy { it.artist.lowercase() }
            TrackSortMode.ALBUM -> list.sortedBy { it.album.lowercase() }
            TrackSortMode.FOLDER -> list.sortedWith(compareBy({ it.folderName.lowercase() }, { it.title.lowercase() }))
            TrackSortMode.DURATION -> list.sortedByDescending { it.durationMs }
        }
    }

    private fun autoApplyEqPresetForSong(song: Song) {
        val allPresets = EqualizerManager.PRESETS
        val query = (song.genre + " " + song.folder + " " + song.title + " " + song.recommendedPreset).lowercase()
        val matched = when {
            query.contains("synthwave") || query.contains("cyberpunk") || query.contains("edm") || query.contains("club") ->
                allPresets.firstOrNull { it.name.contains("Electronic") }
            query.contains("metal") || query.contains("heavy") ->
                allPresets.firstOrNull { it.name.contains("Heavy Metal") }
            query.contains("rock") ->
                allPresets.firstOrNull { it.name.contains("Rock") }
            query.contains("phonk") || query.contains("drift") ->
                allPresets.firstOrNull { it.name.contains("Phonk") }
            query.contains("drum & bass") || query.contains("dnb") ->
                allPresets.firstOrNull { it.name.contains("Drum & Bass") }
            query.contains("techno") || query.contains("acid") ->
                allPresets.firstOrNull { it.name.contains("Peak-Time Techno") }
            query.contains("trap") || query.contains("808") || query.contains("sub") ->
                allPresets.firstOrNull { it.name.contains("Deep Sub-Bass") }
            query.contains("hip-hop") || query.contains("rap") || query.contains("рэп") ->
                allPresets.firstOrNull { it.name.contains("Hip-Hop") }
            query.contains("lofi") || query.contains("chill") || query.contains("lounge") ->
                allPresets.firstOrNull { it.name.contains("Lounge") }
            query.contains("acoustic") || query.contains("vocal") ->
                allPresets.firstOrNull { it.name.contains("Acoustic") }
            query.contains("jazz") || query.contains("blues") ->
                allPresets.firstOrNull { it.name.contains("Jazz") }
            query.contains("classic") || query.contains("symphony") ->
                allPresets.firstOrNull { it.name.contains("Classical") }
            query.contains("pop") ->
                allPresets.firstOrNull { it.name.contains("Pop") }
            else -> allPresets.firstOrNull { it.name.contains("Harman") } ?: allPresets[0]
        }
        matched?.let {
            applyEqPreset(it.name)
            _uiState.update { state ->
                state.copy(
                    eqPreampDb = it.preampDb,
                    trebleLevel = it.treble
                )
            }
        }
    }

    fun toggleAutoEq(enabled: Boolean) {
        _uiState.update { it.copy(isAutoEqEnabled = enabled) }
        if (enabled && _uiState.value.currentSong != null) {
            autoApplyEqPresetForSong(_uiState.value.currentSong!!)
        }
    }

    fun setParametricMode(enabled: Boolean) {
        _uiState.update { it.copy(isParametricMode = enabled) }
    }

    fun setPreamp(db: Float) {
        _uiState.update { it.copy(eqPreampDb = db) }
    }

    fun toggleLimiter(enabled: Boolean) {
        _uiState.update { it.copy(isLimiterEnabled = enabled) }
    }

    fun setTreble(level: Int) {
        _uiState.update { it.copy(trebleLevel = level) }
    }

    fun setGenreFilter(genre: String) {
        _uiState.update { state ->
            val filtered = if (genre == "Все") {
                state.songs
            } else if (genre == "Локальные") {
                state.songs.filter { !it.isBuiltIn }
            } else {
                state.songs.filter { it.genre.contains(genre, ignoreCase = true) }
            }
            state.copy(activeGenreFilter = genre, filteredSongs = sortTrackList(filtered, state.sortMode))
        }
    }

    fun setCoverStyle(style: CoverStyle) {
        _uiState.update { it.copy(coverStyle = style) }
    }

    fun cycleCoverStyle() {
        val styles = CoverStyle.values()
        val nextIdx = (styles.indexOf(_uiState.value.coverStyle) + 1) % styles.size
        setCoverStyle(styles[nextIdx])
    }

    fun openYandexModal(isOpen: Boolean) {
        _uiState.update { it.copy(isYandexModalOpen = isOpen) }
    }

    fun openOnlineSearch(isOpen: Boolean) {
        _uiState.update { it.copy(isOnlineSearchOpen = isOpen) }
    }

    fun setSortMode(mode: TrackSortMode) {
        _uiState.update { state ->
            val sortedSongs = sortTrackList(state.songs, mode)
            val sortedFiltered = sortTrackList(state.filteredSongs, mode)
            state.copy(sortMode = mode, songs = sortedSongs, filteredSongs = sortedFiltered)
        }
    }

    fun searchOnlineTracks(query: String) {
        _uiState.update { it.copy(onlineSearchQuery = query) }
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(onlineSearchResults = emptyList(), isSearchingOnline = false, onlineSearchError = null) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingOnline = true, onlineSearchError = null) }
            try {
                val results = coverArtRepository.searchOnlineMusic(trimmed)
                _uiState.update {
                    it.copy(
                        isSearchingOnline = false,
                        onlineSearchResults = results,
                        onlineSearchError = if (results.isEmpty()) "По запросу "$trimmed" ничего не найдено. Попробуйте другой запрос." else null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSearchingOnline = false,
                        onlineSearchError = "Ошибка поиска: " + (e.localizedMessage ?: "не удалось связаться с сервером")
                    )
                }
            }
        }
    }

    fun downloadOnlineSong(title: String, artist: String, genre: String, audioUrl: String, coverUrl: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    onlineDownloadProgress = 15,
                    onlineDownloadMessage = "Подключение к источнику: $title..."
                )
            }

            val context = getApplication<Application>()
            val downloadedUri = withContext(Dispatchers.IO) {
                try {
                    val musicDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir
                    val safeName = (artist + "_" + title).replace(Regex("[^a-zA-Z0-9а-яА-ЯёЁ_ -]"), "_").take(50) + ".m4a"
                    val destFile = File(musicDir, safeName)

                    val req = Request.Builder()
                        .url(audioUrl)
                        .header("User-Agent", "MP3PlayerPro-Android/1.0.0")
                        .build()
                    coverArtRepository.httpClient.newCall(req).execute().use { response ->
                        if (!response.isSuccessful) return@withContext null
                        val body = response.body ?: return@withContext null
                        val contentLength = body.contentLength()
                        var totalRead = 0L
                        body.byteStream().use { input ->
                            FileOutputStream(destFile).use { output ->
                                val buffer = ByteArray(8 * 1024)
                                var read: Int
                                while (input.read(buffer).also { read = it } != -1) {
                                    output.write(buffer, 0, read)
                                    totalRead += read
                                    if (contentLength > 0) {
                                        val progress = ((totalRead * 80) / contentLength).toInt() + 15
                                        _uiState.update { it.copy(onlineDownloadProgress = progress, onlineDownloadMessage = "Скачивание: " + progress + "%") }
                                    }
                                }
                            }
                        }
                        Uri.fromFile(destFile)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            val finalUri = downloadedUri ?: Uri.parse(audioUrl)
            val newSong = Song(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                album = "Онлайн Загрузки",
                durationMs = 210000L,
                contentUri = finalUri,
                coverUrl = coverUrl,
                genre = genre,
                bitrate = "320 kbps MP3 HQ",
                isBuiltIn = false
            )

            _uiState.update { state ->
                val updated = listOf(newSong) + state.songs
                val sorted = sortTrackList(filterAndProcessSongs(updated, state.hideShortSongs, state.blacklistedFolders), state.sortMode)
                state.copy(
                    songs = updated,
                    filteredSongs = sorted,
                    onlineDownloadProgress = 100,
                    onlineDownloadMessage = "Успешно скачан трек: $title!",
                    isOnlineSearchOpen = false
                )
            }
        }
    }

    fun searchCoverForCurrentSong() {
        val song = _uiState.value.currentSong ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingCover = true, coverSearchMessage = "Поиск официальной HD-обложки...") }
            try {
                val results = coverArtRepository.searchCoverArt(song.artist, song.title)
                val topMatch = results.firstOrNull()
                val finalUrl = if (topMatch != null) {
                    val cachedUri = coverArtRepository.downloadAndCacheCover(song.id, topMatch.hdCoverUrl)
                    cachedUri?.toString() ?: topMatch.hdCoverUrl
                } else {
                    _uiState.update { it.copy(coverSearchMessage = "Генерация художественной обложки...") }
                    val genUri = coverArtRepository.generateProceduralCover(song.id, song.title, song.artist)
                    genUri.toString()
                }

                val updatedSong = song.copy(coverUrl = finalUrl)
                _uiState.update { state ->
                    val updatedList = state.songs.map { if (it.id == song.id) updatedSong else it }
                    val updatedFiltered = state.filteredSongs.map { if (it.id == song.id) updatedSong else it }
                    state.copy(
                        songs = updatedList,
                        filteredSongs = updatedFiltered,
                        currentSong = updatedSong,
                        isSearchingCover = false,
                        coverSearchMessage = if (topMatch != null) "Найдена обложка: " + topMatch.album else "Сгенерирована авторская обложка"
                    )
                }
            } catch (e: Exception) {
                val genUri = coverArtRepository.generateProceduralCover(song.id, song.title, song.artist)
                val updatedSong = song.copy(coverUrl = genUri.toString())
                _uiState.update { state ->
                    val updatedList = state.songs.map { if (it.id == song.id) updatedSong else it }
                    val updatedFiltered = state.filteredSongs.map { if (it.id == song.id) updatedSong else it }
                    state.copy(
                        songs = updatedList,
                        filteredSongs = updatedFiltered,
                        currentSong = updatedSong,
                        isSearchingCover = false,
                        coverSearchMessage = "Сгенерирована авторская обложка"
                    )
                }
            }
        }
    }

    fun syncYandexMusic(token: String, userId: String = "me") {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isYandexSyncing = true,
                    yandexSyncProgress = 5,
                    yandexSyncStatus = "Подключение к Яндекс ID..."
                )
            }

            try {
                val idsResult = yandexRepository.getLikedTrackIds(token, userId)
                val ids = idsResult.getOrNull()
                if (ids.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isYandexSyncing = false,
                            yandexSyncStatus = "Ошибка авторизации или список лайков пуст"
                        )
                    }
                    return@launch
                }

                _uiState.update {
                    it.copy(
                        yandexSyncProgress = 25,
                        yandexSyncStatus = "Найдено ${ids.size} треков. Загрузка данных..."
                    )
                }

                val tracksResult = yandexRepository.getTracksMetadata(token, ids.take(50))
                val tracks = tracksResult.getOrNull() ?: emptyList()

                val yandexSongs = tracks.map { yt ->
                    Song(
                        id = yt.id.hashCode().toLong(),
                        title = yt.title,
                        artist = yt.artist,
                        album = yt.album,
                        durationMs = yt.durationMs,
                        contentUri = Uri.parse("https://api.music.yandex.net"),
                        coverUrl = yt.hdCoverUrl,
                        genre = "Яндекс Музыка",
                        year = yt.year ?: 2024,
                        bitrate = "320 kbps MP3",
                        isBuiltIn = false
                    )
                }

                _uiState.update { state ->
                    val combined = (yandexSongs + state.songs).distinctBy { it.id }
                    val sorted = sortTrackList(filterAndProcessSongs(combined, state.hideShortSongs, state.blacklistedFolders), state.sortMode)
                    state.copy(
                        songs = combined,
                        filteredSongs = sorted,
                        isYandexSyncing = false,
                        yandexSyncProgress = 100,
                        yandexSyncStatus = "Успешно добавлено ${yandexSongs.size} треков!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isYandexSyncing = false,
                        yandexSyncStatus = "Ошибка: ${e.message}"
                    )
                }
            }
        }
    }

    override fun onCleared() {
        unbindAudioSession()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        equalizerManager?.release()
        exoPlayer.release()
        super.onCleared()
    }
}
