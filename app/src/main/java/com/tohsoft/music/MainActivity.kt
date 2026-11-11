package com.tohsoft.music

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import com.tohsoft.music.service.MusicPlaybackService
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import com.tohsoft.music.service.DynamicIslandOverlayService
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.tohsoft.music.ui.screens.MainPlayerScreen
import com.tohsoft.music.viewmodel.MusicPlayerViewModel
import kotlin.math.sqrt

class MainActivity : ComponentActivity(), SensorEventListener {

    private lateinit var viewModel: MusicPlayerViewModel
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var lastShakeTimestamp: Long = 0

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

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        checkAndRequestPermissions()

        val filter = IntentFilter().apply {
            addAction(MusicPlaybackService.ACTION_APP_SHUTDOWN)
            addAction(MusicPlaybackService.ACTION_MEDIA_COMMAND)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(shutdownReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(shutdownReceiver, filter)
        }

        // Прямой слушатель кнопок из шторки уведомлений и экрана блокировки
        MusicPlaybackService.mediaControlListener = object : MusicPlaybackService.MediaControlListener {
            override fun onTogglePlayPause() {
                runOnUiThread { viewModel.togglePlayPause() }
            }
            override fun onNext() {
                runOnUiThread { viewModel.nextSong() }
            }
            override fun onPrev() {
                runOnUiThread { viewModel.prevSong() }
            }
            override fun onCloseApp() {
                runOnUiThread {
                    finishAffinity()
                    System.exit(0)
                }
            }
        }

        // Непрерывная синхронизация состояния воспроизведения со шторкой уведомлений
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                val song = state.currentSong
                if (song != null) {
                    val notifIntent = Intent(this@MainActivity, MusicPlaybackService::class.java).apply {
                        action = if (state.isPlaying) MusicPlaybackService.ACTION_UPDATE_NOTIFICATION else MusicPlaybackService.ACTION_PAUSE_NOTIFICATION
                        putExtra("extra_title", song.title)
                        putExtra("extra_artist", song.artist)
                        putExtra("extra_is_playing", state.isPlaying)
                    }
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(notifIntent)
                        } else {
                            startService(notifIntent)
                        }
                    } catch (ignored: Exception) {}
                }
            }
        }

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
                    onSelectFolder = { folder -> viewModel.selectFolder(folder) },
                    onPlayFolder = { folder -> viewModel.playFolder(folder) },
                    onOpenCreatePlaylist = { viewModel.openCreatePlaylistDialog(true) },
                    onCloseCreatePlaylist = { viewModel.openCreatePlaylistDialog(false) },
                    onCreatePlaylist = { name, icon, color -> viewModel.createPlaylist(name, icon, color) },
                    onOpenAddToPlaylist = { song -> viewModel.openAddToPlaylistDialog(song) },
                    onCloseAddToPlaylist = { viewModel.closeAddToPlaylistDialog() },
                    onAddSongToPlaylist = { playlistId, songId -> viewModel.addSongToPlaylist(playlistId, songId) },
                    onRemoveSongFromPlaylist = { playlistId, songId -> viewModel.removeSongFromPlaylist(playlistId, songId) },
                    onDeletePlaylist = { playlistId -> viewModel.deletePlaylist(playlistId) },
                    onSelectPlaylist = { playlistId -> viewModel.selectPlaylist(playlistId) },
                    onPlayPlaylist = { playlistId -> viewModel.playPlaylist(playlistId) },
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
                    onToggleParametricMode = { viewModel.setParametricMode(it) },
                    onToggleAutoEq = { viewModel.toggleAutoEq(it) },
                    onPreampChange = { viewModel.setPreamp(it) },
                    onToggleLimiter = { viewModel.toggleLimiter(it) },
                    onTrebleChange = { viewModel.setTreble(it) },
                    onRequestOverlayPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            if (!Settings.canDrawOverlays(this)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:" + packageName)
                                )
                                startActivity(intent)
                                Toast.makeText(this, "Разрешите показ поверх других приложений для островка", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this, "Островок активен! При сворачивании появится у камеры 🐸", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
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
                    },
                    onRescanStorage = { viewModel.rescanStorage() },
                    onOpenBlacklistFolders = { viewModel.openBlacklistFoldersDialog(true) },
                    onCloseBlacklistFolders = { viewModel.openBlacklistFoldersDialog(false) },
                    onToggleBlacklistFolder = { folder -> viewModel.toggleBlacklistFolder(folder) },
                    onOpenLanguage = { viewModel.openLanguageDialog(true) },
                    onCloseLanguage = { viewModel.openLanguageDialog(false) },
                    onSelectLanguage = { lang -> viewModel.setLanguage(lang) },
                    onOpenSmartShare = { viewModel.openSmartShareDialog(true) },
                    onCloseSmartShare = { viewModel.openSmartShareDialog(false) },
                    onToggleVisibleTab = { tab -> viewModel.toggleVisibleTab(tab) },
                    onClearStatusToast = { viewModel.clearStatusToast() },
                    onPromptDeleteSong = { song -> viewModel.promptDeleteSong(song) },
                    onConfirmDeleteSong = { viewModel.confirmDeleteSong() },
                    onDismissDeleteSong = { viewModel.dismissDeleteDialog() },
                    onFeedback = {
                        try {
                            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@mp3playerpro.app")
                                putExtra(Intent.EXTRA_SUBJECT, "MP3 Player Pro - Отзыв")
                                val deviceInfo = "Устройство: " + Build.MANUFACTURER + " " + Build.MODEL + " (Android " + Build.VERSION.RELEASE + ")"
                                putExtra(Intent.EXTRA_TEXT, deviceInfo)
                            }
                            startActivity(Intent.createChooser(emailIntent, "Отправить отзыв"))
                        } catch (e: Exception) {
                            Toast.makeText(this, "Почтовый клиент не найден", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRateApp = {
                        try {
                            val rateIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
                            startActivity(rateIntent)
                        } catch (e: Exception) {
                            val webIntent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                            )
                            startActivity(webIntent)
                        }
                    },
                    onShareApp = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "MP3 Player Pro")
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Рекомендую MP3 Player Pro — мощный плеер с 10-полосным эквалайзером и системным звуковым процессором для Яндекс Музыки и Spotify!"
                            )
                        }
                        startActivity(Intent.createChooser(shareIntent, "Поделиться MP3 Player Pro"))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            if (!viewModel.uiState.value.shakeToSkip) return
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat() / SensorManager.GRAVITY_EARTH
            if (gForce > 2.7f) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTimestamp > 1200L) {
                    lastShakeTimestamp = now
                    viewModel.nextSong()
                    Toast.makeText(this, "🔀 Встряхивание: следующий трек", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

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

    private val shutdownReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicPlaybackService.ACTION_APP_SHUTDOWN -> {
                    finishAffinity()
                }
                MusicPlaybackService.ACTION_MEDIA_COMMAND -> {
                    when (intent.getStringExtra("command")) {
                        MusicPlaybackService.ACTION_TOGGLE_PLAY -> viewModel.togglePlayPause()
                        MusicPlaybackService.ACTION_NEXT -> viewModel.nextSong()
                        MusicPlaybackService.ACTION_PREV -> viewModel.prevSong()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val state = viewModel.uiState.value
        if (state.currentSong != null) {
            // Размещаем уведомление управления в выдвижной шторке Android
            val notifIntent = Intent(this, MusicPlaybackService::class.java).apply {
                action = if (state.isPlaying) MusicPlaybackService.ACTION_UPDATE_NOTIFICATION else MusicPlaybackService.ACTION_PAUSE_NOTIFICATION
                putExtra("extra_title", state.currentSong.title)
                putExtra("extra_artist", state.currentSong.artist)
                putExtra("extra_is_playing", state.isPlaying)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(notifIntent)
                } else {
                    startService(notifIntent)
                }
            } catch (ignored: Exception) {}

            // Показываем плавающий Dynamic Island у фронтальной камеры
            if (state.isPlaying && (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this))) {
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
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, DynamicIslandOverlayService::class.java).apply {
            action = DynamicIslandOverlayService.ACTION_HIDE
        }
        try {
            startService(intent)
        } catch (ignored: Exception) {}
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(shutdownReceiver)
        } catch (ignored: Exception) {}
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == "android.media.action.DISPLAY_AUDIO_EFFECT_CONTROL_PANEL") {
            viewModel.openEqualizer()
        }
    }
}
