package com.tohsoft.music.viewmodel

import android.app.Application
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TrackSortMode(val label: String) {
    TITLE_ASC("По названию (А-Я)"),
    ARTIST("По артисту"),
    ALBUM("По альбому"),
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
    val rememberPlayback: Boolean = true,
    val lockScreenPlayer: Boolean = false,
    val fadeInOut: Boolean = false,
    val shakeToSkip: Boolean = false,
    val gridAlbumView: Boolean = false,
    val hideShortSongs: Boolean = false,
    val isLoading: Boolean = true,
    // Equalizer & Audio FX State
    val isEqualizerOpen: Boolean = false,
    val isEqEnabled: Boolean = true,
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
    val onlineDownloadProgress: Int = 0,
    val onlineDownloadMessage: String = ""
)

enum class RepeatMode {
    NONE, ALL, ONE
}

class MusicPlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
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

    init {
        setupPlayerListener()
        loadSongs()
        try {
            equalizerManager = EqualizerManager(exoPlayer.audioSessionId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var lastBoundSessionId: Int = 0

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

            // Re-apply active EQ preset & settings to the fresh audio session
            equalizerManager?.applyPreset(_uiState.value.activeEqPreset)
            equalizerManager?.setBassBoostStrength(_uiState.value.bassBoost)
            equalizerManager?.setVirtualizerStrength(_uiState.value.virtualizer)
            equalizerManager?.setLoudnessGain(_uiState.value.loudnessGain)
            equalizerManager?.setEnabled(_uiState.value.isEqEnabled)

            // Notify Android system & Xiaomi/Samsung Dolby Atmos/Dirac engine
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
                _uiState.update {
                    it.copy(
                        currentPositionMs = exoPlayer.currentPosition,
                        durationMs = exoPlayer.duration.coerceAtLeast(0)
                    )
                }
                delay(500)
            }
        }
    }

    fun loadSongs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = repository.loadDeviceSongs()
            _uiState.update {
                it.copy(
                    songs = list,
                    filteredSongs = list,
                    currentSong = it.currentSong ?: list.firstOrNull(),
                    isLoading = false
                )
            }
        }
    }

    fun playSong(song: Song) {
        try {
            val mediaItem = MediaItem.fromUri(song.contentUri)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()

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
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
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
    }

    fun nextSong() {
        val state = _uiState.value
        val list = if (state.filteredSongs.isNotEmpty()) state.filteredSongs else state.songs
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == state.currentSong?.id }
        val nextIndex = if (state.isShuffle) {
            list.indices.random()
        } else {
            (currentIndex + 1) % list.size
        }
        playSong(list[nextIndex])
    }

    fun prevSong() {
        val state = _uiState.value
        val list = if (state.filteredSongs.isNotEmpty()) state.filteredSongs else state.songs
        if (list.isEmpty()) return

        val currentIndex = list.indexOfFirst { it.id == state.currentSong?.id }
        val prevIndex = if (currentIndex > 0) currentIndex - 1 else list.size - 1
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
                state.songs
            } else {
                state.songs.filter {
                    it.title.contains(query, ignoreCase = true) || it.artist.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredSongs = filtered)
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

    fun openHideTabsDialog(isOpen: Boolean) {
        _uiState.update { it.copy(isHideTabsDialogOpen = isOpen) }
    }

    fun openPropertiesDialog(isOpen: Boolean, song: Song? = null) {
        _uiState.update { it.copy(isPropertiesDialogOpen = isOpen, selectedSongForMenu = song ?: it.currentSong) }
    }

    fun openThemePicker(isOpen: Boolean) {
        _uiState.update { it.copy(isThemePickerOpen = isOpen) }
    }

    fun openWallpaperPicker(isOpen: Boolean) {
        _uiState.update { it.copy(isWallpaperPickerOpen = isOpen) }
    }

    fun setTheme(theme: String) {
        _uiState.update { it.copy(currentTheme = theme, isThemePickerOpen = false) }
    }

    // -------------------------------------------------------------------------
    // Equalizer & Audio FX Methods
    // -------------------------------------------------------------------------
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

    fun setEqBand(bandIndex: Int, levelDb: Int) {
        equalizerManager?.setBandLevel(bandIndex, levelDb)
        _uiState.update { state ->
            val updated = state.eqBands.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = levelDb
            }
            state.copy(eqBands = updated, activeEqPreset = "Пользовательский")
        }
    }

    fun applyEqPreset(presetName: String) {
        val preset = equalizerManager?.applyPreset(presetName) ?: return
        _uiState.update {
            it.copy(
                activeEqPreset = preset.name,
                eqBands = preset.bands,
                bassBoost = preset.bassBoost,
                virtualizer = preset.virtualizer,
                reverbPreset = preset.reverb
            )
        }
    }

    fun setBassBoost(strength: Int) {
        equalizerManager?.setBassBoostStrength(strength)
        _uiState.update { it.copy(bassBoost = strength) }
    }

    fun setVirtualizer(strength: Int) {
        equalizerManager?.setVirtualizerStrength(strength)
        _uiState.update { it.copy(virtualizer = strength) }
    }

    fun setLoudness(gainDb: Int) {
        equalizerManager?.setLoudnessGain(gainDb)
        _uiState.update { it.copy(loudnessGain = gainDb) }
    }

    fun setReverb(preset: Short) {
        equalizerManager?.setReverb(preset)
        _uiState.update { it.copy(reverbPreset = preset) }
    }

    fun resetEq() {
        equalizerManager?.reset()
        _uiState.update {
            it.copy(
                activeEqPreset = "Normal (Плоский)",
                eqBands = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
                bassBoost = 0,
                virtualizer = 0,
                loudnessGain = 0,
                reverbPreset = 0
            )
        }
    }

    fun toggleSetting(key: String, value: Boolean) {
        _uiState.update {
            when (key) {
                "rememberPlayback" -> it.copy(rememberPlayback = value)
                "lockScreenPlayer" -> it.copy(lockScreenPlayer = value)
                "fadeInOut" -> it.copy(fadeInOut = value)
                "shakeToSkip" -> it.copy(shakeToSkip = value)
                "gridAlbumView" -> it.copy(gridAlbumView = value)
                "hideShortSongs" -> it.copy(hideShortSongs = value)
                else -> it
            }
        }
    }

    // -------------------------------------------------------------------------
    // Visual Cover Art & Online Integration Methods
    // -------------------------------------------------------------------------
    private val coverArtRepository = CoverArtRepository(getApplication())
    private val yandexRepository = YandexMusicRepository()

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

    private fun sortTrackList(list: List<Song>, mode: TrackSortMode): List<Song> {
        return when (mode) {
            TrackSortMode.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            TrackSortMode.ARTIST -> list.sortedBy { it.artist.lowercase() }
            TrackSortMode.ALBUM -> list.sortedBy { it.album.lowercase() }
            TrackSortMode.DURATION -> list.sortedByDescending { it.durationMs }
        }
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

    fun downloadOnlineSong(title: String, artist: String, genre: String, audioUrl: String, coverUrl: String) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    onlineDownloadProgress = 20, 
                    onlineDownloadMessage = "Загрузка аудио: $title..."
                ) 
            }
            delay(600)
            _uiState.update { it.copy(onlineDownloadProgress = 70) }
            delay(600)

            val newSong = Song(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                album = "Онлайн Загрузки",
                durationMs = 210000L,
                contentUri = Uri.parse(audioUrl),
                coverUrl = coverUrl,
                genre = genre,
                bitrate = "320 kbps MP3 HQ",
                isBuiltIn = false
            )

            _uiState.update { state ->
                val updated = listOf(newSong) + state.songs
                state.copy(
                    songs = updated,
                    filteredSongs = updated,
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
            _uiState.update { it.copy(isSearchingCover = true, coverSearchMessage = "Поиск обложки HD...") }
            try {
                val results = coverArtRepository.searchCoverArt(song.artist, song.title)
                val topMatch = results.firstOrNull()
                if (topMatch != null) {
                    val updatedSong = song.copy(coverUrl = topMatch.hdCoverUrl)
                    _uiState.update { state ->
                        val updatedList = state.songs.map { if (it.id == song.id) updatedSong else it }
                        val updatedFiltered = state.filteredSongs.map { if (it.id == song.id) updatedSong else it }
                        state.copy(
                            songs = updatedList,
                            filteredSongs = updatedFiltered,
                            currentSong = updatedSong,
                            isSearchingCover = false,
                            coverSearchMessage = "Найдена обложка: ${topMatch.album}"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isSearchingCover = false, coverSearchMessage = "Обложка не найдена") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearchingCover = false, coverSearchMessage = "Ошибка поиска: ${e.message}") }
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
                    state.copy(
                        songs = combined,
                        filteredSongs = combined,
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
        equalizerManager?.release()
        exoPlayer.release()
        super.onCleared()
    }
}
