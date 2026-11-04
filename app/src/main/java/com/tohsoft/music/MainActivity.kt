package com.tohsoft.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.compose.material3.MaterialTheme
import com.tohsoft.music.service.DynamicIslandOverlayService
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.tohsoft.music.ui.screens.MainPlayerScreen
import com.tohsoft.music.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MusicPlayerViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = permissions[Manifest.permission.READ_MEDIA_AUDIO] ?: false
        val storageGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: false

        if (audioGranted || storageGranted) {
            viewModel.loadSongs()
        } else {
            Toast.makeText(this, "Требуется доступ к аудиофайлам для воспроизведения", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this)[MusicPlayerViewModel::class.java]

        checkAndRequestPermissions()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val uiState by viewModel.uiState.collectAsState()

                MainPlayerScreen(
                    uiState = uiState,
                    onSongClick = { song -> viewModel.playSong(song) },
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onNext = { viewModel.nextSong() },
                    onPrev = { viewModel.prevSong() },
                    onSeekTo = { pos -> viewModel.seekTo(pos) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onSearchQueryChanged = { q -> viewModel.onSearchQueryChanged(q) },
                    onTabSelected = { tab -> viewModel.setTab(tab) },
                    onOpenFullPlayer = { viewModel.openFullPlayer() },
                    onCloseFullPlayer = { viewModel.closeFullPlayer() },
                    onSongMenuClick = { song -> viewModel.openSongMenu(song) },
                    onCloseSongMenu = { viewModel.closeSongMenu() },
                    onSleepTimerClick = { viewModel.openSleepTimerDialog(true) },
                    onCloseSleepTimer = { viewModel.openSleepTimerDialog(false) },
                    onStartSleepTimer = { seconds -> viewModel.startSleepTimer(seconds) },
                    onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                    onHideTabsClick = { viewModel.openHideTabsDialog(true) },
                    onCloseHideTabs = { viewModel.openHideTabsDialog(false) },
                    onThemePickerClick = { viewModel.openThemePicker(true) },
                    onCloseThemePicker = { viewModel.openThemePicker(false) },
                    onSelectTheme = { theme -> viewModel.setTheme(theme) },
                    onWallpaperPickerClick = { viewModel.openWallpaperPicker(true) },
                    onCloseWallpaperPicker = { viewModel.openWallpaperPicker(false) },
                    onPropertiesClick = { song -> viewModel.openPropertiesDialog(true, song) },
                    onCloseProperties = { viewModel.openPropertiesDialog(false) },
                    onToggleSetting = { key, value -> viewModel.toggleSetting(key, value) },
                    onOpenEqualizer = { viewModel.openEqualizer() },
                    onCloseEqualizer = { viewModel.closeEqualizer() },
                    onToggleEq = { viewModel.toggleEqEnabled(it) },
                    onSelectEqPreset = { viewModel.applyEqPreset(it) },
                    onEqBandChange = { band, level -> viewModel.setEqBand(band, level) },
                    onBassBoostChange = { viewModel.setBassBoost(it) },
                    onVirtualizerChange = { viewModel.setVirtualizer(it) },
                    onLoudnessChange = { viewModel.setLoudness(it) },
                    onReverbChange = { viewModel.setReverb(it) },
                    onResetEq = { viewModel.resetEq() },
                    onSetCoverStyle = { style -> viewModel.setCoverStyle(style) },
                    onCycleCoverStyle = { viewModel.cycleCoverStyle() },
                    onOpenYandexModal = { viewModel.openYandexModal(true) },
                    onCloseYandexModal = { viewModel.openYandexModal(false) },
                    onSyncYandex = { token -> viewModel.syncYandexMusic(token) },
                    onSearchCover = { viewModel.searchCoverForCurrentSong() },
                    onSelectGenreFilter = { genre -> viewModel.setGenreFilter(genre) },
                    onSortChange = { mode -> viewModel.setSortMode(mode) },
                    onOpenOnlineSearch = { viewModel.openOnlineSearch(true) },
                    onCloseOnlineSearch = { viewModel.openOnlineSearch(false) },
                    onSearchOnline = { query -> viewModel.searchOnlineTracks(query) },
                    onDownloadOnlineTrack = { title, artist, genre, url, cover ->
                        viewModel.downloadOnlineSong(title, artist, genre, url, cover)
                    }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onStop() {
        super.onStop()
        // При сворачивании в фон: если играет музыка, запускаем островок фронтальной камеры
        val state = viewModel.uiState.value
        if (state.isPlaying && state.currentSong != null) {
            val intent = Intent(this, DynamicIslandOverlayService::class.java).apply {
                action = DynamicIslandOverlayService.ACTION_SHOW
                putExtra(DynamicIslandOverlayService.EXTRA_TITLE, state.currentSong.title)
                putExtra(DynamicIslandOverlayService.EXTRA_ARTIST, state.currentSong.artist)
                putExtra(DynamicIslandOverlayService.EXTRA_IS_PLAYING, true)
            }
            try {
                startService(intent)
            } catch (ignored: Exception) {}
        }
    }

    override fun onStart() {
        super.onStart()
        // При возвращении в приложение скрываем плавающий островок камеры
        val intent = Intent(this, DynamicIslandOverlayService::class.java).apply {
            action = DynamicIslandOverlayService.ACTION_HIDE
        }
        try {
            startService(intent)
        } catch (ignored: Exception) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == "android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL") {
            viewModel.openEqualizer()
        }
    }
}
