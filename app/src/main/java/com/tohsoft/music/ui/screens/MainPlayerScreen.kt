package com.tohsoft.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.tohsoft.music.data.model.Song
import com.tohsoft.music.data.streaming.StreamingAccount
import com.tohsoft.music.data.streaming.StreamingProvider
import com.tohsoft.music.ui.components.CoverStyle
import com.tohsoft.music.ui.components.PlayerCoverView
import com.tohsoft.music.viewmodel.PlayerUiState
import com.tohsoft.music.viewmodel.RepeatMode

private val ColorTurquoise = Color(0xFF00C996)
private val ColorTurquoiseDark = Color(0xFF008966)
private val ColorDarkBgTop = Color(0xFF090D1E)
private val ColorDarkBgMid = Color(0xFF0F1A3B)
private val ColorDarkBgBottom = Color(0xFF070B18)
private val ColorTextMuted = Color(0xFF8EA0BE)
private val ColorYandexGold = Color(0xFFFFCC00)

@Composable
fun MainPlayerScreen(
    uiState: PlayerUiState,
    onSongClick: (Song) -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTabSelected: (Int) -> Unit,
    onSelectFolder: (String?) -> Unit = {},
    onPlayFolder: (String) -> Unit = {},
    onOpenCreatePlaylist: () -> Unit = {},
    onCloseCreatePlaylist: () -> Unit = {},
    onCreatePlaylist: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenAddToPlaylist: (Song) -> Unit = {},
    onCloseAddToPlaylist: () -> Unit = {},
    onAddSongToPlaylist: (String, Long) -> Unit = { _, _ -> },
    onRemoveSongFromPlaylist: (String, Long) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onSelectPlaylist: (String?) -> Unit = {},
    onPlayPlaylist: (String) -> Unit = {},
    onOpenFullPlayer: () -> Unit,
    onCloseFullPlayer: () -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onCloseSongMenu: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onCloseSleepTimer: () -> Unit,
    onStartSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onHideTabsClick: () -> Unit,
    onCloseHideTabs: () -> Unit,
    onThemePickerClick: () -> Unit,
    onCloseThemePicker: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onWallpaperPickerClick: () -> Unit,
    onCloseWallpaperPicker: () -> Unit,
    onPropertiesClick: (Song?) -> Unit,
    onCloseProperties: () -> Unit,
    onToggleSetting: (String, Boolean) -> Unit,
    onOpenEqualizer: () -> Unit,
    onCloseEqualizer: () -> Unit,
    onToggleEq: (Boolean) -> Unit,
    onSelectEqPreset: (String) -> Unit,
    onEqBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onReverbChange: (Short) -> Unit,
    onResetEq: () -> Unit,
    onSetCoverStyle: (CoverStyle) -> Unit,
    onCycleCoverStyle: () -> Unit,
    onOpenYandexModal: () -> Unit,
    onCloseYandexModal: () -> Unit = {},
    onSyncYandex: (String) -> Unit,
    onSearchCover: () -> Unit,
    onSelectGenreFilter: (String) -> Unit,
    onSortChange: (com.tohsoft.music.viewmodel.TrackSortMode) -> Unit = {},
    onOpenOnlineSearch: () -> Unit = {},
    onCloseOnlineSearch: () -> Unit = {},
    onSearchOnline: (String) -> Unit = {},
    onDownloadOnlineTrack: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> },
    onRescanStorage: () -> Unit = {},
    onOpenBlacklistFolders: () -> Unit = {},
    onCloseBlacklistFolders: () -> Unit = {},
    onToggleBlacklistFolder: (String) -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onCloseLanguage: () -> Unit = {},
    onSelectLanguage: (String) -> Unit = {},
    onOpenSmartShare: () -> Unit = {},
    onCloseSmartShare: () -> Unit = {},
    onToggleVisibleTab: (String) -> Unit = {},
    onClearStatusToast: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onToggleParametricMode: (Boolean) -> Unit = {},
    onToggleAutoEq: (Boolean) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onToggleLimiter: (Boolean) -> Unit = {},
    onTrebleChange: (Int) -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    onPromptDeleteSong: (Song) -> Unit = {},
    onConfirmDeleteSong: () -> Unit = {},
    onDismissDeleteSong: () -> Unit = {},
    onOpenStorageScanner: () -> Unit = {},
    onCloseStorageScanner: () -> Unit = {},
    onRequestAudioPermission: () -> Unit = {},
    onRequestAllFilesPermission: () -> Unit = {},
    onSaveTrackMetadata: (Long, String, String, String, String, String, List<String>) -> Unit = { _, _, _, _, _, _, _ -> },
    onSelectTagFilter: (String) -> Unit = {},
    onOpenAccountsModal: () -> Unit = {},
    onCloseAccountsModal: () -> Unit = {},
    onConnectAccount: (String, String, String) -> Unit = { _, _, _ -> },
    onSelectActiveAccount: (String?) -> Unit = {},
    onRemoveAccount: (String) -> Unit = {},
    onExportSettingsBackup: () -> Unit = {},
    onImportSettingsBackup: () -> Unit = {}
) {
    val bgBrush = Brush.verticalGradient(
        listOf(ColorDarkBgTop, ColorDarkBgMid, ColorDarkBgBottom)
    )

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(uiState.statusToastMessage) {
        uiState.statusToastMessage?.let { msg ->
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
            onClearStatusToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(bgBrush).systemBarsPadding()) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopTabsBar(
                    activeTab = uiState.activeTab,
                    accountsCount = uiState.streamingAccounts.size,
                    hasActiveAccount = uiState.activeAccountId != null,
                    onTabSelected = onTabSelected,
                    onOpenAccountsModal = onOpenAccountsModal,
                    onOpenYandexModal = onOpenYandexModal,
                    onOpenEqualizer = onOpenEqualizer
                )
            },
            bottomBar = {
                uiState.currentSong?.let { song ->
                    BottomMiniPlayerBar(
                        song = song,
                        isPlaying = uiState.isPlaying,
                        onBarClick = onOpenFullPlayer,
                        onTogglePlayPause = onTogglePlayPause,
                        onNext = onNext,
                        onPrev = onPrev
                    )
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (uiState.activeTab) {
                    0 -> {
                        SongsTabContent(
                            songs = uiState.filteredSongs,
                            currentSong = uiState.currentSong,
                            isPlaying = uiState.isPlaying,
                            searchQuery = uiState.searchQuery,
                            activeGenreFilter = uiState.activeGenreFilter,
                            availableTags = uiState.availableTags,
                            activeTagFilter = uiState.activeTagFilter,
                            sortMode = uiState.sortMode,
                            onSearchChange = onSearchQueryChanged,
                            onSelectGenreFilter = onSelectGenreFilter,
                            onSelectTagFilter = onSelectTagFilter,
                            onSortChange = onSortChange,
                            onOpenOnlineSearch = onOpenOnlineSearch,
                            onOpenAccountsModal = onOpenAccountsModal,
                            onSongClick = onSongClick,
                            onSongMenuClick = onSongMenuClick,
                            onOpenYandexModal = onOpenYandexModal,
                            onAddToPlaylist = onOpenAddToPlaylist,
                            onShuffleAll = {
                                onToggleShuffle()
                                if (uiState.filteredSongs.isNotEmpty()) {
                                    onSongClick(uiState.filteredSongs.random())
                                }
                            }
                        )
                    }
                    1 -> {
                        FoldersTabContent(
                            songs = uiState.songs,
                            selectedFolder = uiState.selectedFolder,
                            isScanning = uiState.isScanningStorage,
                            hasAudioPermission = uiState.storageHasAudioPermission,
                            hasAllFilesPermission = uiState.storageHasAllFilesPermission,
                            lastScanSyncedCount = uiState.lastScanSyncedCount,
                            onOpenStorageScanner = onOpenStorageScanner,
                            onRescanFolders = onRescanStorage,
                            onSelectFolder = onSelectFolder,
                            onPlayFolder = onPlayFolder,
                            onSongClick = onSongClick,
                            onAddToPlaylist = onOpenAddToPlaylist
                        )
                    }
                    2 -> {
                        PlaylistsTabContent(
                            playlists = uiState.userPlaylists,
                            allSongs = uiState.songs,
                            selectedPlaylistId = uiState.selectedPlaylistId,
                            onSelectPlaylist = onSelectPlaylist,
                            onOpenCreatePlaylist = onOpenCreatePlaylist,
                            onPlayPlaylist = onPlayPlaylist,
                            onDeletePlaylist = onDeletePlaylist,
                            onRemoveSongFromPlaylist = onRemoveSongFromPlaylist,
                            onSongClick = onSongClick
                        )
                    }
                    else -> {
                        SettingsTabContent(
                            uiState = uiState,
                            onHideTabsClick = onHideTabsClick,
                            onSleepTimerClick = onSleepTimerClick,
                            onThemePickerClick = onThemePickerClick,
                            onWallpaperPickerClick = onWallpaperPickerClick,
                            onOpenEqualizer = onOpenEqualizer,
                            onToggleSetting = onToggleSetting,
                            onOpenYandexModal = onOpenYandexModal,
                            onOpenAccountsModal = onOpenAccountsModal,
                            onOpenStorageScanner = onOpenStorageScanner,
                            onSetCoverStyle = onSetCoverStyle,
                            onRescanStorage = onRescanStorage,
                            onOpenBlacklistFolders = onOpenBlacklistFolders,
                            onOpenLanguage = onOpenLanguage,
                            onOpenSmartShare = onOpenSmartShare,
                            onFeedback = onFeedback,
                            onRateApp = onRateApp,
                            onShareApp = onShareApp,
                            onRequestOverlayPermission = onRequestOverlayPermission,
                            onExportSettingsBackup = onExportSettingsBackup,
                            onImportSettingsBackup = onImportSettingsBackup
                        )
                    }
                }
            }
        }

        // Full Screen Player Overlay (matching modern styled design)
        AnimatedVisibility(
            visible = uiState.isFullPlayerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            uiState.currentSong?.let { song ->
                FullPlayerScreen(
                    song = song,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = uiState.currentPositionMs,
                    durationMs = if (uiState.durationMs > 0) uiState.durationMs else song.durationMs,
                    isShuffle = uiState.isShuffle,
                    repeatMode = uiState.repeatMode,
                    coverStyle = uiState.coverStyle,
                    bassBoostGain = uiState.bassBoost,
                    audioSessionId = uiState.audioSessionId,
                    isSearchingCover = uiState.isSearchingCover,
                    coverSearchMessage = uiState.coverSearchMessage,
                    onClose = onCloseFullPlayer,
                    onTogglePlayPause = onTogglePlayPause,
                    onNext = onNext,
                    onPrev = onPrev,
                    onSeekTo = onSeekTo,
                    onToggleShuffle = onToggleShuffle,
                    onToggleRepeat = onToggleRepeat,
                    onSetCoverStyle = onSetCoverStyle,
                    onCycleCoverStyle = onCycleCoverStyle,
                    onBassBoostChange = onBassBoostChange,
                    onSearchCover = onSearchCover,
                    onSleepTimerClick = onSleepTimerClick,
                    onOpenEqualizer = onOpenEqualizer,
                    onMenuClick = { onSongMenuClick(song) },
                    onRequestOverlayPermission = onRequestOverlayPermission,
                    onPromptDeleteSong = onPromptDeleteSong
                )
            }
        }

        // Wide-Band Equalizer Screen Overlay
        AnimatedVisibility(
            visible = uiState.isEqualizerOpen,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            EqualizerScreen(
                isEqEnabled = uiState.isEqEnabled,
                activePreset = uiState.activeEqPreset,
                bands = uiState.eqBands,
                bassBoost = uiState.bassBoost,
                virtualizer = uiState.virtualizer,
                loudness = uiState.loudnessGain,
                reverbPreset = uiState.reverbPreset,
                isParametricMode = uiState.isParametricMode,
                isAutoEqEnabled = uiState.isAutoEqEnabled,
                preampDb = uiState.eqPreampDb,
                isLimiterEnabled = uiState.isLimiterEnabled,
                trebleLevel = uiState.trebleLevel,
                currentSongTitle = uiState.currentSong?.title ?: "MP3 Player Pro",
                currentSongGenre = uiState.currentSong?.genre ?: "Все",
                onClose = onCloseEqualizer,
                onToggleEnabled = onToggleEq,
                onSelectPreset = onSelectEqPreset,
                onBandChange = onEqBandChange,
                onBassBoostChange = onBassBoostChange,
                onVirtualizerChange = onVirtualizerChange,
                onLoudnessChange = onLoudnessChange,
                onReverbChange = onReverbChange,
                onResetEq = onResetEq,
                onToggleParametricMode = onToggleParametricMode,
                onToggleAutoEq = onToggleAutoEq,
                onPreampChange = onPreampChange,
                onToggleLimiter = onToggleLimiter,
                onTrebleChange = onTrebleChange
            )
        }

        // Create Playlist Dialog
        if (uiState.isCreatePlaylistOpen) {
            CreatePlaylistDialog(
                onDismiss = onCloseCreatePlaylist,
                onCreate = onCreatePlaylist
            )
        }

        // Add to Playlist Dialog
        if (uiState.isAddToPlaylistOpen && uiState.songToAddToPlaylist != null) {
            AddToPlaylistDialog(
                song = uiState.songToAddToPlaylist,
                playlists = uiState.userPlaylists,
                onDismiss = onCloseAddToPlaylist,
                onAddToPlaylist = onAddSongToPlaylist,
                onOpenCreatePlaylist = {
                    onCloseAddToPlaylist()
                    onOpenCreatePlaylist()
                }
            )
        }

        // Yandex Music Sync Modal Dialog
        if (uiState.isYandexModalOpen) {
            YandexSyncModal(
                isSyncing = uiState.isYandexSyncing,
                progress = uiState.yandexSyncProgress,
                statusText = uiState.yandexSyncStatus,
                onDismiss = onCloseYandexModal,
                onSync = onSyncYandex
            )
        }

        // Song Context Menu Bottom Sheet
        if (uiState.isSongMenuOpen && uiState.selectedSongForMenu != null) {
            SongContextMenuModal(
                song = uiState.selectedSongForMenu,
                onDismiss = onCloseSongMenu,
                onPlay = { onSongClick(uiState.selectedSongForMenu); onCloseSongMenu() },
                onOpenEqualizer = { onOpenEqualizer(); onCloseSongMenu() },
                onSearchCover = { onSearchCover(); onCloseSongMenu() },
                onProperties = { onPropertiesClick(uiState.selectedSongForMenu); onCloseSongMenu() },
                onDelete = onPromptDeleteSong
            )
        }

        // Sleep Timer Dialog
        if (uiState.isSleepTimerDialogOpen) {
            SleepTimerModal(
                onDismiss = onCloseSleepTimer,
                onSetTimer = { seconds ->
                    onStartSleepTimer(seconds)
                    onCloseSleepTimer()
                }
            )
        }

        // Hide Tabs Dialog
        if (uiState.isHideTabsDialogOpen) {
            HideTabsModal(
                visibleTabs = uiState.visibleTabs,
                onToggleTab = onToggleVisibleTab,
                onDismiss = onCloseHideTabs
            )
        }

        // Blacklist Folders Dialog
        if (uiState.isBlacklistFoldersDialogOpen) {
            BlacklistFoldersModal(
                blacklistedFolders = uiState.blacklistedFolders,
                onToggleFolder = onToggleBlacklistFolder,
                onDismiss = onCloseBlacklistFolders
            )
        }

        // Language Selection Dialog
        if (uiState.isLanguageDialogOpen) {
            LanguageModal(
                currentLanguage = uiState.currentLanguage,
                onSelectLanguage = onSelectLanguage,
                onDismiss = onCloseLanguage
            )
        }

        // SmartShare Dialog
        if (uiState.isSmartShareDialogOpen) {
            SmartShareModal(
                onDismiss = onCloseSmartShare
            )
        }

        // Theme Selector Modal
        if (uiState.isThemePickerOpen) {
            ThemePickerModal(
                currentTheme = uiState.currentTheme,
                onSelectTheme = onSelectTheme,
                onDismiss = onCloseThemePicker
            )
        }

        // Wallpaper Screen Modal
        if (uiState.isWallpaperPickerOpen) {
            WallpaperModal(onDismiss = onCloseWallpaperPicker)
        }

        // Song Properties & TrackDatabase Editor Dialog
        if (uiState.isPropertiesDialogOpen && uiState.selectedSongForMenu != null) {
            val targetSong = uiState.selectedSongForMenu
            SongPropertiesModal(
                song = targetSong,
                onDismiss = onCloseProperties,
                onSaveMetadata = { title, artist, album, genre, notes, tags ->
                    onSaveTrackMetadata(targetSong.id, title, artist, album, genre, notes, tags)
                    onCloseProperties()
                }
            )
        }

        // Track Delete Confirmation Dialog
        if (uiState.isDeleteDialogOpen && uiState.songToDelete != null) {
            val songToDelete = uiState.songToDelete
            AlertDialog(
                onDismissRequest = onDismissDeleteSong,
                containerColor = Color(0xFF131D33),
                title = {
                    Text("Удалить трек?", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                },
                text = {
                    Text(
                        "Вы действительно хотите удалить «" + songToDelete.title + "» (" + songToDelete.artist + ") из медиатеки и памяти устройства?",
                        color = ColorTextMuted,
                        fontSize = 13.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onConfirmDeleteSong,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Удалить", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteSong) {
                        Text("Отмена", color = Color.White)
                    }
                }
            )
        }

        // Online Music Search & Download Modal Dialog
        if (uiState.isOnlineSearchOpen) {
            OnlineMusicSearchModal(
                onDismiss = onCloseOnlineSearch,
                onSearch = onSearchOnline,
                isSearching = uiState.isSearchingOnline,
                searchResults = uiState.onlineSearchResults,
                searchError = uiState.onlineSearchError,
                onDownload = onDownloadOnlineTrack,
                downloadProgress = uiState.onlineDownloadProgress,
                downloadMessage = uiState.onlineDownloadMessage
            )
        }

        // StorageScanner Interactive Bridge & Permissions Modal
        if (uiState.isStorageScannerOpen) {
            StorageScannerModal(
                hasAudioPermission = uiState.storageHasAudioPermission,
                hasAllFilesPermission = uiState.storageHasAllFilesPermission,
                isScanning = uiState.isScanningStorage,
                lastScanSyncedCount = uiState.lastScanSyncedCount,
                scannedDirectories = uiState.lastScanDirectories,
                totalSongsCount = uiState.songs.size,
                onRequestAudioPermission = onRequestAudioPermission,
                onRequestAllFilesPermission = onRequestAllFilesPermission,
                onScanAndSync = onRescanStorage,
                onDismiss = onCloseStorageScanner
            )
        }

        // Free Multi-Provider Streaming Accounts Modal
        if (uiState.isAccountsModalOpen) {
            StreamingAccountsModal(
                accounts = uiState.streamingAccounts,
                activeAccountId = uiState.activeAccountId,
                isConnecting = uiState.isConnectingAccount,
                statusText = uiState.accountConnectStatus,
                onConnectAccount = onConnectAccount,
                onSelectActiveAccount = onSelectActiveAccount,
                onRemoveAccount = onRemoveAccount,
                onDismiss = onCloseAccountsModal
            )
        }
    }
}

// ------------------------------------------------------------------------------------------------
// TOP TABS BAR WITH STREAMING ACCOUNTS, YANDEX & EQ QUICK ACCESS
// ------------------------------------------------------------------------------------------------
@Composable
fun TopTabsBar(
    activeTab: Int,
    accountsCount: Int = 0,
    hasActiveAccount: Boolean = false,
    onTabSelected: (Int) -> Unit,
    onOpenAccountsModal: () -> Unit = {},
    onOpenYandexModal: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Surface(
        color = Color(0xFF090D1E),
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Верхняя полоса: брендинг плеера и быстрые ярлыки (отделены от вкладок)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "MP3 PLAYER",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "PRO",
                        color = ColorTurquoise,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Ярлык бесплатных стриминг-аккаунтов (Audius / Spotify / VK / SC)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (hasActiveAccount) Color(0x3300C996) else Color(0x2238BDF8),
                        border = BorderStroke(1.dp, if (hasActiveAccount) ColorTurquoise else Color(0x4438BDF8)),
                        modifier = Modifier.clickable { onOpenAccountsModal() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👤", fontSize = 12.sp)
                            if (accountsCount > 0) {
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    text = "${accountsCount}",
                                    color = if (hasActiveAccount) ColorTurquoise else Color(0xFF7DD3FC),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    // Ярлык Яндекс Музыка
                    IconButton(
                        onClick = onOpenYandexModal,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x22FFCC00), CircleShape)
                    ) {
                        Text("🟡", fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(6.dp))
                    // Ярлык 10-полосный эквалайзер
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0x2200C996), CircleShape)
                    ) {
                        Text("🎚", fontSize = 13.sp)
                    }
                }
            }
            // Полноширинная строка 4 вкладок без наездов на иконки и перекрытий
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf("ПЕСНИ", "ПАПКИ", "ПЛЕЙЛИСТЫ", "НАСТРОЙКИ")
                tabs.forEachIndexed { index, title ->
                    val isSelected = activeTab == index
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabSelected(index) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) ColorTurquoise else ColorTextMuted,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                        if (isSelected) {
                            Box(
                                Modifier
                                    .padding(top = 4.dp)
                                    .width(42.dp)
                                    .height(3.dp)
                                    .background(ColorTurquoise, RoundedCornerShape(2.dp))
                            )
                        } else {
                            Spacer(Modifier.height(7.dp))
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SONGS TAB CONTENT: SEARCH, SORT, GENRE & TRACKDATABASE TAG FILTER CHIPS & SONG LIST
// ------------------------------------------------------------------------------------------------
@Composable
fun SongsTabContent(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    searchQuery: String,
    activeGenreFilter: String,
    availableTags: List<String> = emptyList(),
    activeTagFilter: String = "",
    sortMode: com.tohsoft.music.viewmodel.TrackSortMode = com.tohsoft.music.viewmodel.TrackSortMode.TITLE_ASC,
    onSearchChange: (String) -> Unit,
    onSelectGenreFilter: (String) -> Unit,
    onSelectTagFilter: (String) -> Unit = {},
    onSortChange: (com.tohsoft.music.viewmodel.TrackSortMode) -> Unit = {},
    onOpenOnlineSearch: () -> Unit = {},
    onOpenAccountsModal: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onOpenYandexModal: () -> Unit,
    onAddToPlaylist: (Song) -> Unit = {},
    onShuffleAll: () -> Unit
) {
    val genreChips = listOf("Все", "Drum & Bass", "Techno", "Rock", "Synthwave", "Trap", "Acoustic", "Lo-Fi", "Локальные")

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Controls Subheader
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔍", fontSize = 18.sp, color = ColorTextMuted)
            Spacer(Modifier.width(6.dp))
            TextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(
                        "Поиск треков (${songs.size})",
                        color = ColorTextMuted,
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                singleLine = true
            )
            IconButton(onClick = onShuffleAll) {
                Text("🔀", fontSize = 18.sp)
            }
            IconButton(onClick = onOpenAccountsModal) {
                Text("👤", fontSize = 16.sp)
            }
            IconButton(onClick = onOpenOnlineSearch) {
                Text("🌐", fontSize = 16.sp)
            }
            IconButton(onClick = onOpenYandexModal) {
                Text("🟡", fontSize = 16.sp)
            }
        }

        // Sorting Row & Online Tracks Shortcut
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    com.tohsoft.music.viewmodel.TrackSortMode.TITLE_ASC to "🔤 А-Я",
                    com.tohsoft.music.viewmodel.TrackSortMode.ARTIST to "🎤 Артист",
                    com.tohsoft.music.viewmodel.TrackSortMode.ALBUM to "💿 Альбом",
                    com.tohsoft.music.viewmodel.TrackSortMode.FOLDER to "📁 Папка",
                    com.tohsoft.music.viewmodel.TrackSortMode.MOST_PLAYED to "▶ Популяр."
                ).forEach { (mode, label) ->
                    val isModeActive = sortMode == mode
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isModeActive) ColorTurquoise else Color(0x1AFFFFFF))
                            .clickable { onSortChange(mode) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.sp,
                            fontWeight = if (isModeActive) FontWeight.Bold else FontWeight.Normal,
                            color = if (isModeActive) Color(0xFF090D1E) else Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            Button(
                onClick = onOpenOnlineSearch,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                modifier = Modifier.height(26.dp)
            ) {
                Text("⬇ Скачать онлайн", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(Modifier.height(4.dp))

        // Horizontal Genre Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(genreChips) { genre ->
                val isSelected = (genre == activeGenreFilter) || (genre == "Drum & Bass" && activeGenreFilter.contains("Drum"))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) ColorTurquoise else Color(0xFF141F3C),
                    border = BorderStroke(1.dp, if (isSelected) ColorTurquoise else Color(0x333A4D6B)),
                    modifier = Modifier.clickable { onSelectGenreFilter(if (isSelected && genre != "Все") "Все" else genre) }
                ) {
                    Text(
                        text = genre,
                        color = if (isSelected) Color(0xFF060919) else Color.White,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // TrackDatabase User #Tag Filter Chips
        if (availableTags.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    val allActive = activeTagFilter.isBlank()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (allActive) Color(0x3338BDF8) else Color(0x11FFFFFF),
                        border = BorderStroke(1.dp, if (allActive) Color(0xFF38BDF8) else Color(0x22FFFFFF)),
                        modifier = Modifier.clickable { onSelectTagFilter("") }
                    ) {
                        Text(
                            text = "#все-теги",
                            color = if (allActive) Color(0xFF7DD3FC) else ColorTextMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                items(availableTags) { tag ->
                    val isTagActive = activeTagFilter.equals(tag, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTagActive) Color(0xFF38BDF8) else Color(0xFF131D38),
                        border = BorderStroke(1.dp, if (isTagActive) Color(0xFF38BDF8) else Color(0x3338BDF8)),
                        modifier = Modifier.clickable {
                            onSelectTagFilter(if (isTagActive) "" else tag)
                        }
                    ) {
                        Text(
                            text = "#${tag}",
                            color = if (isTagActive) Color(0xFF060919) else Color(0xFF93C5FD),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Divider(color = Color(0x22FFFFFF), thickness = 0.5.dp, modifier = Modifier.padding(top = 4.dp))

        if (songs.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Треки не найдены", color = ColorTextMuted, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onSelectGenreFilter("Все"); onSearchChange("") },
                        colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
                    ) {
                        Text("Сбросить фильтры", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                items(songs) { song ->
                    val isCurrent = song.id == currentSong?.id
                    SongListItem(
                        song = song,
                        isCurrent = isCurrent,
                        isPlaying = isCurrent && isPlaying,
                        onClick = { onSongClick(song) },
                        onMenuClick = { onSongMenuClick(song) }
                    )
                }
            }
        }
    }
}

/**
 * Анимированная визуализация танцующего лягушонка (🐸🕺) на плашке играющего трека
 */
@Composable
fun DancingFrogVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Останавливаем бесконечную анимацию, когда экран не в состоянии RESUMED (экономия CPU и батареи)
    @Suppress("DEPRECATION")
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var isScreenResumed by remember { mutableStateOf(true) }
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> isScreenResumed = true
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE,
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> isScreenResumed = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val animateActive = isPlaying && isScreenResumed

    val infiniteTransition = rememberInfiniteTransition(label = "DancingFrog")
    
    // Прыжки в такт бита
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animateActive) -5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "FrogBounce"
    )

    // Наклон корпуса / покачивание
    val rotation by infiniteTransition.animateFloat(
        initialValue = if (animateActive) -12f else 0f,
        targetValue = if (animateActive) 12f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "FrogRotate"
    )

    // Мерцание музыкальных нот
    val noteAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (animateActive) 1f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 520, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "NoteAlpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x2E10B981))
            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(10.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = "♪",
            color = Color(0xFF34D399).copy(alpha = noteAlpha),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (bounceY * 0.6f).dp)
        )

        Spacer(Modifier.width(2.dp))

        Box(
            modifier = Modifier
                .offset(y = bounceY.dp)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🐸",
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.width(2.dp))

        Text(
            text = "♫",
            color = Color(0xFF22D3EE).copy(alpha = noteAlpha),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-bounceY * 0.6f).dp)
        )
    }
}

@Composable
fun SongListItem(
    song: Song,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(if (isCurrent) Color(0x1400C996) else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail: shows actual cover art from coverUrl or albumArtUri with smooth fallback
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )
                .border(
                    1.dp,
                    if (isCurrent) ColorTurquoise else Color(0x33475569),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            val imageSource = song.coverUrl ?: song.albumArtUri
            if (imageSource != null) {
                AsyncImage(
                    model = imageSource,
                    contentDescription = song.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = if (isPlaying) "⏸" else "♫",
                    color = if (isCurrent) ColorTurquoise else Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrent) ColorTurquoise else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.isBuiltIn) {
                    Surface(
                        color = Color(0x3300C996),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = "ТЕСТ HQ",
                            color = ColorTurquoise,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                if (!song.streamingProvider.isNullOrBlank()) {
                    Surface(
                        color = Color(0x3338BDF8),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            text = song.streamingProvider.uppercase(),
                            color = Color(0xFF7DD3FC),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "${song.artist} • ${song.genre}",
                    color = ColorTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            if (song.playCount > 0 || song.tags.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (song.playCount > 0) {
                        Surface(
                            color = Color(0x2200C996),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "▶ ${song.playCount}",
                                color = ColorTurquoise,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    song.tags.take(2).forEach { tag ->
                        Surface(
                            color = Color(0x2238BDF8),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "#${tag}",
                                color = Color(0xFF93C5FD),
                                fontSize = 9.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }
        }

        // Визуализация плашки играющего трека: Танцующий лягушонок
        if (isCurrent) {
            DancingFrogVisualizer(isPlaying = isPlaying)
            Spacer(Modifier.width(6.dp))
        }

        Text(
            text = song.durationFormatted,
            color = ColorTextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 6.dp)
        )

        IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
            Text("⋮", color = ColorTextMuted, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// BOTTOM DOCKED MINI PLAYER BAR
// ------------------------------------------------------------------------------------------------
@Composable
fun BottomMiniPlayerBar(
    song: Song,
    isPlaying: Boolean,
    onBarClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    Surface(
        color = Color(0xFF0C142B),
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBarClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ColorTurquoiseDark),
                contentAlignment = Alignment.Center
            ) {
                val imageSource = song.coverUrl ?: song.albumArtUri
                if (imageSource != null) {
                    AsyncImage(
                        model = imageSource,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("♫", color = Color.White, fontSize = 20.sp)
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = song.artist,
                    color = ColorTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Text("⏮", color = ColorTurquoise, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ColorTurquoise)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = Color(0xFF070B18),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Text("⏭", color = ColorTurquoise, fontSize = 16.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// FULLSCREEN PLAYER: REAL COVER VIEW, STYLE SWITCHER, DOLBY ATMOS & CONTROLS
// ------------------------------------------------------------------------------------------------
@Composable
fun FullPlayerScreen(
    song: Song,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    coverStyle: CoverStyle,
    bassBoostGain: Int = 0,
    audioSessionId: Int = 0,
    isSearchingCover: Boolean,
    coverSearchMessage: String,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onSetCoverStyle: (CoverStyle) -> Unit,
    onCycleCoverStyle: () -> Unit,
    onBassBoostChange: (Int) -> Unit = {},
    onSearchCover: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onMenuClick: () -> Unit,
    onRequestOverlayPermission: () -> Unit = {},
    onPromptDeleteSong: (Song) -> Unit = {}
) {
    val nebulaBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF070B1A),
            Color(0xFF0F1E4A),
            Color(0xFF0A1333),
            Color(0xFF040711)
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(nebulaBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                Text("←", color = Color.White, fontSize = 22.sp)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "СЕЙЧАС ИГРАЕТ",
                    color = ColorTurquoise,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = song.album,
                    color = ColorTextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row {
                IconButton(onClick = onOpenEqualizer, modifier = Modifier.size(36.dp)) {
                    Text("🎚", color = ColorTurquoise, fontSize = 18.sp)
                }
                IconButton(onClick = onCycleCoverStyle, modifier = Modifier.size(36.dp)) {
                    Text("🎨", color = ColorTurquoise, fontSize = 18.sp)
                }
                IconButton(onClick = onMenuClick, modifier = Modifier.size(36.dp)) {
                    Text("⋮", color = Color.White, fontSize = 20.sp)
                }
            }
        }

        // Cover Style Switcher Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CoverStyle.values().forEach { style ->
                val isSelected = (style == coverStyle)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) ColorTurquoise else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) ColorTurquoise else Color(0x33FFFFFF)),
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .clickable { onSetCoverStyle(style) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(style.icon, fontSize = 11.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = style.title,
                            color = if (isSelected) Color(0xFF040711) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // CENTER: INTERACTIVE COVER VIEW (Vinyl spin / 3D Glass / Neon / Poster) + Subwoofer Bass Boost
        Box(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            PlayerCoverView(
                coverModel = song.coverUrl ?: song.albumArtUri,
                isPlaying = isPlaying,
                style = coverStyle,
                bassBoostGain = bassBoostGain,
                audioSessionId = audioSessionId,
                onBassBoostChange = onBassBoostChange
            )
        }

        // Центрированный бейдж DOLBY ATMOS & Hi-Res
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                color = Color(0xFF101B38),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ColorTurquoise.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { onOpenEqualizer() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DOLBY ATMOS", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("•", color = ColorTextMuted, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Hi-Res 24-bit 96kHz", color = Color.White, fontSize = 10.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        // 3 сбалансированные кнопки в один ряд без переносов текста (Островок, Обложка, Удалить)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка Островок
            Surface(
                color = Color(0xFF142B1E),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF22C55E)),
                modifier = Modifier.weight(1f).clickable { onRequestOverlayPermission() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🐸", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("Островок", color = Color(0xFF22C55E), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
            // Кнопка Обложка
            Surface(
                color = Color(0xFF132247),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x444A689B)),
                modifier = Modifier.weight(1f).clickable { onSearchCover() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(if (isSearchingCover) "⏳" else "🔍", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("Обложка", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
            // Кнопка Удалить трек прямо из плеера
            Surface(
                color = Color(0xFF2A1418),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x66EF4444)),
                modifier = Modifier.weight(1f).clickable { onPromptDeleteSong(song) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("🗑", fontSize = 12.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("Удалить", color = Color(0xFFEF4444), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        }
        
        // Cover Search Notification Banner if present
        if (coverSearchMessage.isNotBlank()) {
            Text(
                text = coverSearchMessage,
                color = ColorTurquoise,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
            )
        }

        // SONG TITLE & ARTIST
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                color = ColorTurquoise,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = song.artist,
                color = Color.White,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // PROGRESS SEEKBAR
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            val posFormatted = formatMs(currentPositionMs)
            val durFormatted = formatMs(durationMs)
            val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    val newPos = (frac * durationMs).toLong()
                    onSeekTo(newPos)
                },
                modifier = Modifier.fillMaxWidth().height(24.dp),
                colors = SliderDefaults.colors(
                    thumbColor = ColorTurquoise,
                    activeTrackColor = ColorTurquoise,
                    inactiveTrackColor = Color(0x33FFFFFF)
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(posFormatted, color = ColorTextMuted, fontSize = 11.sp)
                Text(durFormatted, color = ColorTextMuted, fontSize = 11.sp)
            }
        }

        // PLAYBACK CONTROLS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(40.dp)) {
                Text("🔀", color = if (isShuffle) ColorTurquoise else ColorTextMuted, fontSize = 18.sp)
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(44.dp)) {
                Text("⏮", color = Color.White, fontSize = 22.sp)
            }
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF22D3EE), Color(0xFF10B981))
                        )
                    )
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = Color(0xFF070B18),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(44.dp)) {
                Text("⏭", color = Color.White, fontSize = 22.sp)
            }
            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(40.dp)) {
                val repIcon = when (repeatMode) {
                    RepeatMode.ONE -> "🔂"
                    RepeatMode.ALL -> "🔁"
                    RepeatMode.NONE -> "↩"
                }
                Text(repIcon, color = if (repeatMode != RepeatMode.NONE) ColorTurquoise else ColorTextMuted, fontSize = 18.sp)
            }
        }

        // BOTTOM QUICK NAVIGATION (Превью-стиль: Библиотека, Эквалайзер, Таймер, Свернуть)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp, bottom = 2.dp)
                .border(BorderStroke(0.5.dp, Color(0x33334155)), RoundedCornerShape(12.dp))
                .background(Color(0x550B1226), RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onClose() }
            ) {
                Text("🎵", fontSize = 14.sp)
                Text("Библиотека", color = ColorTextMuted, fontSize = 9.sp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenEqualizer() }
            ) {
                Text("🎛️", fontSize = 14.sp)
                Text("EQ", color = ColorTurquoise, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onSleepTimerClick() }
            ) {
                Text("⏱️", fontSize = 14.sp)
                Text("Таймер", color = ColorTextMuted, fontSize = 9.sp)
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onRequestOverlayPermission() }
            ) {
                Text("🐸", fontSize = 14.sp)
                Text("Свернуть", color = Color(0xFF22C55E), fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SETTINGS TAB CONTENT: YANDEX MUSIC CARD, COVER STYLES, AUDIO FX & ADVANCED SETTINGS
// ------------------------------------------------------------------------------------------------
@Composable
fun SettingsTabContent(
    uiState: PlayerUiState,
    onHideTabsClick: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onThemePickerClick: () -> Unit,
    onWallpaperPickerClick: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onToggleSetting: (String, Boolean) -> Unit,
    onOpenYandexModal: () -> Unit,
    onOpenAccountsModal: () -> Unit = {},
    onOpenStorageScanner: () -> Unit = {},
    onSetCoverStyle: (CoverStyle) -> Unit,
    onRescanStorage: () -> Unit = {},
    onOpenBlacklistFolders: () -> Unit = {},
    onOpenLanguage: () -> Unit = {},
    onOpenSmartShare: () -> Unit = {},
    onFeedback: () -> Unit = {},
    onRateApp: () -> Unit = {},
    onShareApp: () -> Unit = {},
    onToggleParametricMode: (Boolean) -> Unit = {},
    onToggleAutoEq: (Boolean) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onToggleLimiter: (Boolean) -> Unit = {},
    onTrebleChange: (Int) -> Unit = {},
    onRequestOverlayPermission: () -> Unit = {},
    onExportSettingsBackup: () -> Unit = {},
    onImportSettingsBackup: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // MULTI-PROVIDER FREE STREAMING ACCOUNTS CARD (Audius, Spotify, Yandex, SC, VK)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clickable(onClick = onOpenAccountsModal),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101F38)),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0x3338BDF8), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌐", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Бесплатные аккаунты и стриминг",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val accStatus = if (uiState.streamingAccounts.isEmpty()) {
                            "Audius (@handle без ключей), Spotify по ссылке, Яндекс, SoundCloud, VK"
                        } else {
                            "Подключено: ${uiState.streamingAccounts.size} • Нажмите для переключения"
                        }
                        Text(
                            text = accStatus,
                            color = Color(0xFF93C5FD),
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onOpenAccountsModal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Управление аккаунтами (${uiState.streamingAccounts.size})",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // YANDEX MUSIC HERO CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onOpenYandexModal),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF191B28)),
            border = BorderStroke(1.dp, ColorYandexGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0x33FFCC00), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🟡", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = "Яндекс Музыка (Интеграция)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Синхронизация треков «Мне нравится» и загрузка в офлайн",
                            color = ColorTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenYandexModal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorYandexGold),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Синхронизировать медиатеку",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // TRACKDATABASE LOCAL STATS BANNER
        val totalPlays = uiState.songs.sumOf { it.playCount }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111C2D)),
            border = BorderStroke(1.dp, Color(0x3300C996))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗄", fontSize = 20.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("TrackDatabase (Локальная база ID3 и тегов)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(
                        "Треков: ${uiState.songs.size} • Прослушиваний: ${totalPlays} • Тегов: ${uiState.availableTags.size}",
                        color = ColorTurquoise,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // COVER VISUALIZATION STYLE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF11172D)),
            border = BorderStroke(1.dp, Color(0x3300C996))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🎨", fontSize = 18.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Стиль обложки в плеере", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CoverStyle.values().forEach { style ->
                        val isSelected = (style == uiState.coverStyle)
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSetCoverStyle(style) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) ColorTurquoise else Color(0xFF1A2342),
                            border = BorderStroke(1.dp, if (isSelected) ColorTurquoise else Color(0x22FFFFFF))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(style.icon, fontSize = 20.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = style.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF070B18) else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // POWERAMP-STYLE PRO STUDIO EQUALIZER CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onOpenEqualizer),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A)),
            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x3322C55E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎛️", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Студийный Эквалайзер PRO (Poweramp Style)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("10-полосный + Параметрический EQ, 36 пресетов, Авто-EQ", color = Color(0xFF86EFAC), fontSize = 11.sp)
                }
                Text("›", color = Color(0xFF22C55E), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // DYNAMIC ISLAND CAMERA FROG OVERLAY CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable { onRequestOverlayPermission() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14241B)),
            border = BorderStroke(1.dp, Color(0xFF22C55E).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0x3322C55E), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🐸", fontSize = 20.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Dynamic Island у камеры (Лягушонок)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Компактный островок с танцующим лягушонком при сворачивании", color = Color(0xFF86EFAC), fontSize = 11.sp)
                }
                Text("›", color = Color(0xFF22C55E), fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = "ОСНОВНЫЕ НАСТРОЙКИ",
            color = ColorTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 6.dp)
        )

        val permLabel = if (uiState.storageHasAllFilesPermission) "Полный доступ ✓" else if (uiState.storageHasAudioPermission) "Доступ медиа ✓" else "Нужны права"
        SettingActionItem(icon = "🔍", title = "StorageScanner и папки музыки (${permLabel})", onClick = onOpenStorageScanner)
        SettingActionItem(icon = "🌐", title = "Стриминг-аккаунты (${uiState.streamingAccounts.size})", onClick = onOpenAccountsModal)
        SettingActionItem(icon = "🎚", title = "Эквалайзер и звуковые эффекты", onClick = onOpenEqualizer)
        SettingActionItem(icon = "👁", title = "Скрыть вкладки", onClick = onHideTabsClick)
        SettingActionItem(icon = "📁", title = "Черные папки", onClick = onOpenBlacklistFolders)
        SettingActionItem(icon = "🎨", title = "Менять тему", onClick = onThemePickerClick)
        SettingActionItem(icon = "🌐", title = "язык – " + uiState.currentLanguage, onClick = onOpenLanguage)

        SettingSwitchItem(
            icon = "✂",
            title = "Скрыть короткие песни",
            checked = uiState.hideShortSongs,
            onCheckedChange = { onToggleSetting("hideShortSongs", it) }
        )
        SettingSwitchItem(
            icon = "📌",
            title = "Запоминать последний трек",
            checked = uiState.rememberPlayback,
            onCheckedChange = { onToggleSetting("rememberPlayback", it) }
        )
        SettingSwitchItem(
            icon = "🔒",
            title = "Играть на экране блокировки",
            checked = uiState.lockScreenPlayer,
            onCheckedChange = { onToggleSetting("lockScreenPlayer", it) }
        )

        SettingActionItem(icon = "⏱", title = "Таймер сна", onClick = onSleepTimerClick)
        SettingActionItem(icon = "📲", title = "SmartShare (Приемник)", onClick = onOpenSmartShare)
        SettingActionItem(icon = "💾", title = "Экспорт настроек и EQ (JSON)", onClick = onExportSettingsBackup)
        SettingActionItem(icon = "📂", title = "Импорт настроек и EQ (JSON)", onClick = onImportSettingsBackup)

        SettingSwitchItem(
            icon = "🔊",
            title = "Плавное затухание громкости (Fade In/Out)",
            checked = uiState.fadeInOut,
            onCheckedChange = { onToggleSetting("fadeInOut", it) }
        )
        SettingSwitchItem(
            icon = "📱",
            title = "Измените песню, встряхивая устройство",
            checked = uiState.shakeToSkip,
            onCheckedChange = { onToggleSetting("shakeToSkip", it) }
        )
        SettingSwitchItem(
            icon = "▦",
            title = "Просмотр сетки альбомов",
            checked = uiState.gridAlbumView,
            onCheckedChange = { onToggleSetting("gridAlbumView", it) }
        )

        Spacer(Modifier.height(16.dp))
        Text(
            text = "РАЗВИТИЕ",
            color = ColorTextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        SettingActionItem(icon = "💬", title = "Обратная связь", onClick = onFeedback)
        SettingActionItem(icon = "⭐", title = "Оценить 5 звезд", onClick = onRateApp)
        SettingActionItem(icon = "🔗", title = "Поделиться", onClick = onShareApp)

        Spacer(Modifier.height(24.dp))
        val context = androidx.compose.ui.platform.LocalContext.current
        val (appVerName, appVerCode) = remember(context) {
            try {
                val pInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                val ver = pInfo.versionName ?: "1.0.0-beta14"
                val code = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    pInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode.toLong()
                }
                Pair(ver, code)
            } catch (e: Exception) {
                Pair("1.0.0-beta20", 20L)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("MP3 Player Pro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Версия приложения: v$appVerName (Сборка $appVerCode)", color = ColorTextMuted, fontSize = 13.sp)
        }
    }
}

// ------------------------------------------------------------------------------------------------
// YANDEX MUSIC MODAL DIALOG
// ------------------------------------------------------------------------------------------------
@Composable
fun YandexSyncModal(
    isSyncing: Boolean,
    progress: Int,
    statusText: String,
    onDismiss: () -> Unit,
    onSync: (String) -> Unit
) {
    var tokenInput by remember { mutableStateOf("") }
    var showYandexWebView by remember { mutableStateOf(false) }

    if (showYandexWebView) {
        com.tohsoft.music.ui.components.YandexOAuthWebViewDialog(
            onTokenCaptured = { capturedToken ->
                showYandexWebView = false
                tokenInput = capturedToken
                onSync(capturedToken)
            },
            onDismiss = { showYandexWebView = false }
        )
    }

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        containerColor = Color(0xFF14192B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🟡", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("Яндекс Музыка", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Синхронизация библиотеки «Мне нравится» и загрузка MP3 высокого качества (320 kbps) в локальное хранилище телефона.",
                    color = ColorTextMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { showYandexWebView = true },
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorYandexGold),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "🟡 Войти через Яндекс ID (авто-токен)",
                        color = Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("Или вставьте OAuth-токен вручную", color = ColorTextMuted, fontSize = 12.sp) },
                    placeholder = { Text("y0_AgAAAA...", color = Color(0x66FFFFFF), fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ColorYandexGold,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    ),
                    enabled = !isSyncing
                )

                if (isSyncing) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = ColorYandexGold,
                        trackColor = Color(0x33FFCC00)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = statusText.ifEmpty { "Синхронизация треков..." },
                        color = ColorYandexGold,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSync(tokenInput) },
                enabled = !isSyncing,
                colors = ButtonDefaults.buttonColors(containerColor = ColorYandexGold)
            ) {
                Text(
                    text = if (isSyncing) "Загрузка..." else "Синхронизировать",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            if (!isSyncing) {
                TextButton(onClick = onDismiss) {
                    Text("Отмена", color = ColorTextMuted)
                }
            }
        }
    )
}

@Composable
fun SettingActionItem(icon: String, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x1A00C996)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp, color = ColorTurquoise)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun SettingSwitchItem(icon: String, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0x1A00C996)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 16.sp, color = ColorTurquoise)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = ColorTurquoise,
                uncheckedTrackColor = Color(0x33FFFFFF)
            )
        )
    }
}

// ------------------------------------------------------------------------------------------------
// SONG CONTEXT MENU (matching Screenshot 10)
// ------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongContextMenuModal(
    song: Song,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onSearchCover: () -> Unit,
    onProperties: () -> Unit,
    onDelete: (Song) -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10162A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            MenuItem(icon = "▶", title = "Играть", onClick = onPlay)
            MenuItem(icon = "🔍", title = "Найти HD обложку онлайн", onClick = onSearchCover)
            MenuItem(icon = "🎚", title = "Звуковые эффекты и эквалайзер", onClick = onOpenEqualizer)
            MenuItem(icon = "➕", title = "Добавить в очередь", onClick = onDismiss)
            MenuItem(icon = "ℹ", title = "Свойства трека", onClick = onProperties)
            MenuItem(icon = "🗑", title = "Удалить трек из плеера", textColor = Color(0xFFEF4444), onClick = { onDelete(song); onDismiss() })
        }
    }
}

@Composable
fun MenuItem(icon: String, title: String, textColor: Color = Color.White, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0x2200C996)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = ColorTurquoise, fontSize = 14.sp)
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = Color.White, fontSize = 15.sp)
    }
}

// ------------------------------------------------------------------------------------------------
// SLEEP TIMER DIALOG
// ------------------------------------------------------------------------------------------------
@Composable
fun SleepTimerModal(
    onDismiss: () -> Unit,
    onSetTimer: (Int) -> Unit = {}
) {
    var selectedOption by remember { mutableStateOf("10 секунд (Тест)") }
    val options = listOf(
        "10 секунд (Тест затухания)" to 10,
        "15 минут" to 15 * 60,
        "30 минут" to 30 * 60,
        "45 минут" to 45 * 60,
        "60 минут" to 60 * 60
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Таймер сна (Sleep Timer)", color = Color.Black, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text(
                    "Автоматическая остановка воспроизведения с плавным линейным снижением громкости (Crossfade 5 сек).",
                    color = Color.DarkGray,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                options.forEach { (label, _) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = label }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == label),
                            onClick = { selectedOption = label },
                            colors = RadioButtonDefaults.colors(selectedColor = ColorTurquoise)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val chosenSecs = options.firstOrNull { it.first == selectedOption }?.second ?: 600
                onSetTimer(chosenSecs)
            }) {
                Text("ОК", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// HIDE TABS MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun HideTabsModal(
    visibleTabs: Set<String>,
    onToggleTab: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val items = listOf("Песни", "Настройки", "Альбомы", "Артисты", "Плейлисты", "Папки")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14192B),
        title = {
            Text("Выберите видимые вкладки", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                items.forEach { tabName ->
                    val checked = visibleTabs.contains(tabName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTab(tabName) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { onToggleTab(tabName) },
                            colors = CheckboxDefaults.colors(checkedColor = ColorTurquoise)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(tabName, color = Color.White, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("OK", color = Color(0xFF070B18), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// BLACKLIST FOLDERS MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun BlacklistFoldersModal(
    blacklistedFolders: Set<String>,
    onToggleFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val commonFolders = listOf(
        "WhatsApp Audio",
        "Telegram Audio",
        "Voice Notes",
        "Notifications",
        "Ringtones",
        "Recordings",
        "Podcasts",
        "Android/media"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14192B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📁", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("Черные папки (Исключения)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                Text(
                    "Отметьте папки, треки из которых не должны появляться в вашей медиатеке:",
                    color = ColorTextMuted,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                commonFolders.forEach { folder ->
                    val isBlacklisted = blacklistedFolders.contains(folder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleFolder(folder) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isBlacklisted,
                            onCheckedChange = { onToggleFolder(folder) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFFEF4444),
                                uncheckedColor = Color(0xFF8EA0BE)
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(folder, color = if (isBlacklisted) Color(0xFFFCA5A5) else Color.White, fontSize = 14.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("Готово", color = Color(0xFF070B18), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// LANGUAGE SELECTION MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun LanguageModal(
    currentLanguage: String,
    onSelectLanguage: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val languages = listOf(
        "Русский (Россия)",
        "English (United States)",
        "Español",
        "Deutsch",
        "Français",
        "Українська",
        "Қазақша"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14192B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("Выбор языка", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column {
                languages.forEach { lang ->
                    val isSelected = (lang == currentLanguage)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(0x3300C996) else Color.Transparent)
                            .clickable { onSelectLanguage(lang); onDismiss() }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = lang,
                            color = if (isSelected) ColorTurquoise else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 15.sp
                        )
                        if (isSelected) {
                            Text("✓", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ColorTurquoise)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// SMARTSHARE RECEIVER MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun SmartShareModal(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF14192B),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("📲", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text("SmartShare (Приемник)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Быстрая передача треков без сжатия по Wi-Fi Direct и локальной сети:",
                    color = ColorTextMuted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0B1021))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📡 Статус: Готов к приёму", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Порт: 8080 (HTTP / Direct P2P)", color = ColorTextMuted, fontSize = 12.sp)
                        Text("Устройство: MP3PlayerPro-Direct", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Откройте плеер на другом устройстве и выберите «Отправить трек»", color = ColorTextMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("Понятно", color = Color(0xFF070B18), fontWeight = FontWeight.Bold)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// THEME PICKER MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun ThemePickerModal(
    currentTheme: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val themes = listOf(
        "Космос" to Color(0xFF0F1E4A),
        "Закат" to Color(0xFF8B2B43),
        "Розовый" to Color(0xFFB33B6C),
        "Фиолетовый" to Color(0xFF5A2A82),
        "Синий" to Color(0xFF1D4ED8),
        "Лазурный" to Color(0xFF0284C7),
        "Изумруд" to Color(0xFF0D9488),
        "Зеленый" to Color(0xFF16A34A),
        "Лайм" to Color(0xFF65A30D),
        "Янтарь" to Color(0xFFD97706),
        "Оранжевый" to Color(0xFFEA580C),
        "Монохром" to Color(0xFF475569)
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0B1021)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Text("←", color = Color.White, fontSize = 24.sp)
                }
                Text("Менять тему", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                IconButton(onClick = onDismiss) {
                    Text("✓", color = Color.White, fontSize = 20.sp)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(themes) { (name, color) ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(color)
                            .clickable { onSelectTheme(name) }
                            .padding(8.dp),
                        contentAlignment = Alignment.TopStart
                    ) {
                        if (currentTheme == name || (currentTheme == "cosmic" && name == "Космос")) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(ColorTurquoise),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("✓", color = Color.White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// WALLPAPER MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun WallpaperModal(onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF070B19)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.statusBarsPadding().padding(8.dp)
            ) {
                Text("←", color = Color.White, fontSize = 24.sp)
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {}) { Text("<", color = Color.White, fontSize = 32.sp) }
                IconButton(onClick = {}) { Text(">", color = Color.White, fontSize = 32.sp) }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(0.8f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B52C7)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("УСТАНОВКА ОБОЕВ", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SONG PROPERTIES & TRACKDATABASE METADATA / TAGS EDITOR MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun SongPropertiesModal(
    song: Song,
    onDismiss: () -> Unit,
    onSaveMetadata: (String, String, String, String, String, List<String>) -> Unit = { _, _, _, _, _, _ -> }
) {
    var titleInput by remember(song.id) { mutableStateOf(song.title) }
    var artistInput by remember(song.id) { mutableStateOf(song.artist) }
    var albumInput by remember(song.id) { mutableStateOf(song.album) }
    var genreInput by remember(song.id) { mutableStateOf(song.genre) }
    var notesInput by remember(song.id) { mutableStateOf(song.customNotes) }
    var tagsState by remember(song.id) { mutableStateOf(song.tags) }
    var newTagInput by remember { mutableStateOf("") }
    val quickSuggestedTags = listOf("favorite", "changan", "bass", "rock", "chill", "workout")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10162A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Свойства и TrackDatabase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Surface(
                    color = Color(0x2200C996),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "▶ ${song.playCount} просл.",
                        color = ColorTurquoise,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 430.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Название трека", color = ColorTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )
                OutlinedTextField(
                    value = artistInput,
                    onValueChange = { artistInput = it },
                    label = { Text("Исполнитель", color = ColorTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = albumInput,
                        onValueChange = { albumInput = it },
                        label = { Text("Альбом", color = ColorTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorTurquoise,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        )
                    )
                    OutlinedTextField(
                        value = genreInput,
                        onValueChange = { genreInput = it },
                        label = { Text("Жанр", color = ColorTextMuted, fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ColorTurquoise,
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        )
                    )
                }
                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Личная заметка", color = ColorTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )

                Text("Пользовательские #теги:", color = Color(0xFF93C5FD), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (tagsState.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tagsState) { tag ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0x3338BDF8),
                                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                                modifier = Modifier.clickable {
                                    tagsState = tagsState.filterNot { it.equals(tag, ignoreCase = true) }
                                }
                            ) {
                                Text(
                                    text = "#${tag} ✕",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Новый #тег...", color = Color(0x66FFFFFF), fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0x44FFFFFF)
                        )
                    )
                    Button(
                        onClick = {
                            val clean = newTagInput.trim().removePrefix("#").lowercase()
                            if (clean.isNotBlank() && tagsState.none { it.equals(clean, ignoreCase = true) }) {
                                tagsState = tagsState + clean
                                newTagInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                    ) {
                        Text("+ Тег", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickSuggestedTags) { sTag ->
                        val exists = tagsState.any { it.equals(sTag, ignoreCase = true) }
                        if (!exists) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0x1AFFFFFF),
                                modifier = Modifier.clickable {
                                    tagsState = tagsState + sTag
                                }
                            ) {
                                Text(
                                    text = "+ #${sTag}",
                                    color = ColorTextMuted,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Divider(color = Color(0x22FFFFFF), modifier = Modifier.padding(vertical = 2.dp))
                PropertyRow("Длительность / Битрейт", "${song.durationFormatted} • ${song.bitrate}")
                PropertyRow("Путь файла", song.dataPath.ifBlank { song.contentUri.toString() })
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pendingTag = newTagInput.trim().removePrefix("#").lowercase()
                    val finalTags = if (pendingTag.isNotBlank() && tagsState.none { it.equals(pendingTag, ignoreCase = true) }) {
                        tagsState + pendingTag
                    } else {
                        tagsState
                    }
                    onSaveMetadata(titleInput, artistInput, albumInput, genreInput, notesInput, finalTags)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("Сохранить", color = Color(0xFF070B18), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ColorTextMuted)
            }
        }
    )
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column {
        Text(label, color = ColorTextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 13.sp)
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val secStr = if (sec < 10) "0$sec" else "$sec"
    val minStr = if (min < 10) "0$min" else "$min"
    return "$minStr:$secStr"
}

// ------------------------------------------------------------------------------------------------
// FOLDERS TAB CONTENT (ПАПКИ: YANDEX MUSIC, DOWNLOAD, CHANGAN USB, MUSIC)
// ------------------------------------------------------------------------------------------------
@Composable
fun FoldersTabContent(
    songs: List<Song>,
    selectedFolder: String?,
    isScanning: Boolean = false,
    hasAudioPermission: Boolean = false,
    hasAllFilesPermission: Boolean = false,
    lastScanSyncedCount: Int = 0,
    onOpenStorageScanner: () -> Unit = {},
    onRescanFolders: () -> Unit = {},
    onSelectFolder: (String?) -> Unit,
    onPlayFolder: (String) -> Unit,
    onSongClick: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit
) {
    // Group songs into folders (Yandex Music "Мне нравится", Download, Changan USB, Music)
    val folderMap = remember(songs) {
        val map = mutableMapOf<String, MutableList<Song>>()
        map["YandexMusic"] = mutableListOf()
        map["Download"] = mutableListOf()
        map["Music"] = mutableListOf()
        map["ChanganUSB"] = mutableListOf()

        songs.forEach { s ->
            val f = when {
                s.folder.contains("Yandex", ignoreCase = true) ||
                s.folder.contains("Мне нравится", ignoreCase = true) ||
                s.dataPath.contains("YandexMusic", ignoreCase = true) ||
                s.album.contains("Мне нравится", ignoreCase = true) ||
                s.title.contains("Yandex", ignoreCase = true) -> "YandexMusic"
                s.folder.contains("Download", ignoreCase = true) -> "Download"
                s.folder.contains("Changan", ignoreCase = true) || s.folder.contains("USB", ignoreCase = true) -> "ChanganUSB"
                s.folder.isNotBlank() -> s.folderName
                else -> "Music"
            }
            map.getOrPut(f) { mutableListOf() }.add(s)
        }
        map
    }

    if (selectedFolder != null) {
        // Detailed folder view
        val folderTracks = folderMap[selectedFolder] ?: emptyList()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSelectFolder(null) }
                ) {
                    Text("← ", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Все папки", color = ColorTurquoise, fontSize = 13.sp)
                }

                Button(
                    onClick = { onPlayFolder(selectedFolder) },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("▶ Воспроизвести папку", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            Surface(
                color = Color(0xFF131D38),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x3300C996)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val icon = when {
                        selectedFolder.contains("Yandex", ignoreCase = true) -> "🟡"
                        selectedFolder.contains("Download", ignoreCase = true) -> "📥"
                        selectedFolder.contains("Changan", ignoreCase = true) -> "🚗"
                        else -> "📁"
                    }
                    Text(icon, fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (selectedFolder == "YandexMusic") "Яндекс Музыка — Мне нравится (Скачанные)" else selectedFolder,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        val folderPath = when (selectedFolder) {
                            "YandexMusic" -> "/storage/emulated/0/Music/YandexMusic/Мне нравится/"
                            "Download" -> "/storage/emulated/0/Download/"
                            "ChanganUSB" -> "/storage/usbotg/Music/ (ГУ Авто)"
                            else -> "/storage/emulated/0/Music/$selectedFolder/"
                        }
                        Text(folderPath, color = ColorTextMuted, fontSize = 10.sp)
                        Text(
                            folderTracks.size.toString() + " аудиофайлов • Готовы к воспроизведению",
                            color = ColorTurquoise,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folderTracks) { song ->
                    Surface(
                        color = Color(0xFF0F1A3B),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.coverUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                Text(song.artist, color = ColorTextMuted, fontSize = 11.sp, maxLines = 1)
                            }
                            IconButton(
                                onClick = { onAddToPlaylist(song) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("➕", fontSize = 12.sp)
                            }
                            Text(song.durationFormatted, color = ColorTextMuted, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    } else {
        // List of all folders
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Surface(
                    color = Color(0xFF131D38),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x3300C996)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Text("📁", fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Воспроизведение по папкам", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val permText = if (hasAllFilesPermission) "Полный доступ к /Music ✓" else if (hasAudioPermission) "Доступ READ_MEDIA_AUDIO ✓" else "⚠ Нет доступа к файлам"
                                    Text(
                                        text = "${permText} • TrackDB: ${lastScanSyncedCount}",
                                        color = if (hasAllFilesPermission || hasAudioPermission) ColorTurquoise else Color(0xFFFBBF24),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onOpenStorageScanner,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, ColorTurquoise),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("⚙ Права", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                                Button(
                                    onClick = onRescanFolders,
                                    enabled = !isScanning,
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (isScanning) "Скан..." else "🔍 Скан",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "StorageScanner сканирует /Music/YandexMusic/Мне нравится, Download и USB-флешку авто, синхронизируя файлы в TrackDatabase.",
                            color = ColorTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            val folderKeys = folderMap.keys.toList().sortedBy { if (it == "YandexMusic") 0 else if (it == "ChanganUSB") 1 else 2 }

            items(folderKeys) { fKey ->
                val tracks = folderMap[fKey] ?: emptyList()
                val icon = when {
                    fKey.contains("Yandex", ignoreCase = true) -> "🟡"
                    fKey.contains("Download", ignoreCase = true) -> "📥"
                    fKey.contains("Changan", ignoreCase = true) -> "🚗"
                    else -> "📁"
                }
                val displayName = when (fKey) {
                    "YandexMusic" -> "Яндекс Музыка — Мне нравится"
                    "Download" -> "Загрузки (Download)"
                    "ChanganUSB" -> "Флешка Changan UNI-S (USB)"
                    else -> "Папка: $fKey"
                }
                val pathHint = when (fKey) {
                    "YandexMusic" -> "/storage/emulated/0/Music/YandexMusic/Мне нравится/"
                    "Download" -> "/storage/emulated/0/Download/"
                    "ChanganUSB" -> "/storage/usbotg/Music/ (Авто)"
                    else -> "/storage/emulated/0/Music/$fKey/"
                }

                Surface(
                    color = Color(0xFF0F1A3B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (fKey == "YandexMusic") Color(0x55FFCC00) else Color(0x22FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectFolder(fKey) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(if (fKey == "YandexMusic") Color(0x22FFCC00) else Color(0x2200C996), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(icon, fontSize = 24.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(pathHint, color = ColorTextMuted, fontSize = 9.sp, maxLines = 1)
                            Text(
                                tracks.size.toString() + " " + if (tracks.size == 1) "трек" else if (tracks.size in 2..4) "трека" else "треков",
                                color = ColorTurquoise,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onPlayFolder(fKey) },
                            colors = ButtonDefaults.buttonColors(containerColor = if (fKey == "YandexMusic") Color(0xFFFFCC00) else ColorTurquoise),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("▶ Играть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// PLAYLISTS TAB CONTENT (ПЛЕЙЛИСТЫ: СОЗДАНИЕ И УПРАВЛЕНИЕ)
// ------------------------------------------------------------------------------------------------
@Composable
fun PlaylistsTabContent(
    playlists: List<com.tohsoft.music.data.model.UserPlaylist>,
    allSongs: List<Song>,
    selectedPlaylistId: String?,
    onSelectPlaylist: (String?) -> Unit,
    onOpenCreatePlaylist: () -> Unit,
    onPlayPlaylist: (String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    onRemoveSongFromPlaylist: (String, Long) -> Unit,
    onSongClick: (Song) -> Unit
) {
    if (selectedPlaylistId != null) {
        val pl = playlists.find { it.id == selectedPlaylistId }
        val plSongs = pl?.songIds?.mapNotNull { id -> allSongs.find { it.id == id } } ?: emptyList()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onSelectPlaylist(null) }
                ) {
                    Text("← ", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Все плейлисты", color = ColorTurquoise, fontSize = 13.sp)
                }

                Button(
                    onClick = { pl?.let { onPlayPlaylist(it.id) } },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("▶ Слушать плейлист", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                color = Color(0xFF131D38),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x3300C996)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(pl?.icon ?: "🎵", fontSize = 28.sp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(pl?.name ?: "Плейлист", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(plSongs.size.toString() + " треков в списке", color = ColorTurquoise, fontSize = 11.sp)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (plSongs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text("В этом плейлисте пока нет песен. Добавляйте треки кнопкой «+» во вкладках «ПЕСНИ» или «ПАПКИ»!", color = ColorTextMuted, textAlign = TextAlign.Center, fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(plSongs) { song ->
                        Surface(
                            color = Color(0xFF0F1A3B),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongClick(song) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                                    Text(song.artist, color = ColorTextMuted, fontSize = 11.sp, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { pl?.let { onRemoveSongFromPlaylist(it.id, song.id) } },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text("✕", color = Color(0xFFEF4444), fontSize = 13.sp)
                                }
                                Text(song.durationFormatted, color = ColorTextMuted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    } else {
        // List of all user playlists
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                // Большая заметная кнопка "Создать новый плейлист"
                Button(
                    onClick = onOpenCreatePlaylist,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("➕", fontSize = 16.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("СОЗДАТЬ НОВЫЙ ПЛЕЙЛИСТ", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                    }
                }
            }

            item {
                Surface(
                    color = Color(0xFF131D38),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x2200C996)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💡", fontSize = 18.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Как создать плейлист: нажмите бирюзовую кнопку сверху, введите название или выберите готовый шаблон. Для добавления песен нажимайте «+» на любом треке.",
                            color = ColorTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            items(playlists) { pl ->
                val trackCount = pl.songIds.size
                Surface(
                    color = Color(0xFF0F1A3B),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPlaylist(pl.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color(0x2200C996), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(pl.icon, fontSize = 24.sp)
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(pl.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "$trackCount " + if (trackCount == 1) "трек" else if (trackCount in 2..4) "трека" else "треков",
                                color = ColorTurquoise,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Button(
                            onClick = { onPlayPlaylist(pl.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("▶ Играть", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Spacer(Modifier.width(4.dp))

                        IconButton(
                            onClick = { onDeletePlaylist(pl.id) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Text("✕", color = Color(0xFF64748B), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// DIALOG: CREATE NEW PLAYLIST
// ------------------------------------------------------------------------------------------------
@Composable
fun CreatePlaylistDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, icon: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("🎵") }
    val icons = listOf("🎵", "❤️", "🚗", "🟡", "🔥", "⚡", "🎧", "🌟")
    val presets = listOf("В авто Changan", "Яндекс Любимое", "Мой микс", "Спорт & Драйв")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1A3B),
        title = {
            Text("➕ Создать новый плейлист", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Название плейлиста...", color = Color(0xFF64748B), fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Text("Быстрые шаблоны:", color = ColorTextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0x2200C996),
                            border = BorderStroke(1.dp, Color(0x4400C996)),
                            modifier = Modifier.clickable { name = preset }
                        ) {
                            Text(preset, color = ColorTurquoise, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Выберите иконку:", color = ColorTextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    icons.forEach { icon ->
                        val isSelected = selectedIcon == icon
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (isSelected) Color(0x4400C996) else Color(0x11FFFFFF), CircleShape)
                                .border(1.dp, if (isSelected) ColorTurquoise else Color.Transparent, CircleShape)
                                .clickable { selectedIcon = icon },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(icon, fontSize = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedIcon, "#00C996")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("Создать", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = ColorTextMuted)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// DIALOG: ADD SONG TO PLAYLIST
// ------------------------------------------------------------------------------------------------
@Composable
fun AddToPlaylistDialog(
    song: Song,
    playlists: List<com.tohsoft.music.data.model.UserPlaylist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (playlistId: String, songId: Long) -> Unit,
    onOpenCreatePlaylist: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F1A3B),
        title = {
            Text("➕ Добавить в плейлист", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = Color(0xFF131D38),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.coverUrl,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(6.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(song.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                            Text(song.artist, color = ColorTextMuted, fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text("Выберите плейлист:", color = ColorTextMuted, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(playlists) { pl ->
                        val isAlreadyIn = pl.songIds.contains(song.id)
                        Surface(
                            color = Color(0x22FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isAlreadyIn) ColorTurquoise else Color(0x11FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddToPlaylist(pl.id, song.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(pl.icon, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(pl.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                                if (isAlreadyIn) {
                                    Text("✓ В списке", color = ColorTurquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = onOpenCreatePlaylist,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x3300C996))
                ) {
                    Text("➕ Создать новый плейлист", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ColorTextMuted)
            }
        }
    )
}
// ------------------------------------------------------------------------------------------------
// ONLINE MUSIC SEARCH & DOWNLOAD MODAL (Scoped Storage Downloader)
// ------------------------------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineMusicSearchModal(
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit = {},
    isSearching: Boolean = false,
    searchResults: List<com.tohsoft.music.data.CoverArtRepository.OnlineMusicTrack> = emptyList(),
    searchError: String? = null,
    onDownload: (title: String, artist: String, genre: String, audioUrl: String, coverUrl: String) -> Unit,
    downloadProgress: Int,
    downloadMessage: String
) {
    var searchQuery by remember { mutableStateOf("") }

    val presetOnlineTracks = remember {
        listOf(
            Triple("Кино — Группа крови", "Виктор Цой", "Русский Рок (Zaycev HQ)") to ("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3" to "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"),
            Triple("Linkin Park — In The End", "Linkin Park", "Alternative Rock") to ("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"),
            Triple("Miyagi & Andy Panda — Minor", "Miyagi & Andy Panda", "Hip-Hop / Reggae") to ("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80"),
            Triple("Ghostrifter — Hot Coffee (Lo-Fi)", "Ghostrifter Official", "Lo-Fi 320k") to ("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3" to "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500&q=80"),
            Triple("Karl Casey — Cyberpunk 2080", "White Bat Audio", "Synthwave 320k") to ("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80")
        )
    }

    val popularChips = remember {
        listOf("Eminem", "Кино", "Linkin Park", "Miyagi", "Phonk", "Bass Boost", "Queen")
    }

    // Автоматический запуск поиска при вводе с задержкой 450мс
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.length >= 2) {
            delay(450)
            onSearch(trimmed)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "🌐 Онлайн поиск и загрузка",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "320 kbps",
                        color = ColorTurquoise,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(Color(0xFF064E3B), RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Text("✕", color = ColorTextMuted, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Поле поиска с кнопкой Найти
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Название трека, артист (Кино, Eminem...)", color = Color(0xFF64748B), fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Text("✕", color = Color(0xFF94A3B8), fontSize = 13.sp)
                            }
                        }
                    }
                )

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = { onSearch(searchQuery.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    Text("🔍", fontSize = 15.sp)
                }
            }

            // Быстрые теги артистов
            Spacer(Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(popularChips) { chip ->
                    val isSelected = searchQuery.equals(chip, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) ColorTurquoise else Color(0xFF1E293B))
                            .clickable {
                                searchQuery = chip
                                onSearch(chip)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = chip,
                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            if (downloadProgress > 0) {
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = downloadProgress.toFloat() / 100f,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = ColorTurquoise,
                    trackColor = Color(0xFF1E293B)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = downloadMessage,
                    color = ColorTurquoise,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            // Состояние загрузки
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = ColorTurquoise,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Поиск треков и аудиопотоков...",
                            color = ColorTextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else if (searchResults.isNotEmpty()) {
                // Реальные результаты онлайн поиска
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(searchResults) { track ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = track.coverUrl,
                                contentDescription = track.title,
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = track.artist + " • " + track.album,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (track.isPreview) "Превью 30с • " + track.genre else "Полный трек • " + track.genre + " • 320 kbps",
                                        color = if (track.isPreview) Color(0xFFFBBF24) else ColorTurquoise,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDownload(track.title, track.artist, track.genre, track.audioUrl, track.coverUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⬇ Скачать", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            } else if (searchQuery.isNotBlank()) {
                // Если искали, но ничего не найдено
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = searchError ?: ("По запросу «" + searchQuery + "» ничего не найдено."),
                            color = Color(0xFFF87171),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Попробуйте ввести имя артиста (например: Eminem, Кино, Miyagi, Linkin Park)",
                            color = ColorTextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Если поле поиска пустое, показываем стартовые треки
                Text(
                    text = "Популярные рекомендации:",
                    color = ColorTextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(presetOnlineTracks) { (info, urls) ->
                        val (title, artist, genre) = info
                        val (streamUrl, coverUrl) = urls
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = coverUrl,
                                contentDescription = title,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$artist • $genre",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "320 kbps Master Audio",
                                    color = ColorTurquoise,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDownload(title, artist, genre, streamUrl, coverUrl)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("⬇ Скачать", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// STORAGESCANNER INTERACTIVE UI BRIDGE & PERMISSIONS MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun StorageScannerModal(
    hasAudioPermission: Boolean,
    hasAllFilesPermission: Boolean,
    isScanning: Boolean,
    lastScanSyncedCount: Int,
    scannedDirectories: List<String>,
    totalSongsCount: Int,
    onRequestAudioPermission: () -> Unit,
    onRequestAllFilesPermission: () -> Unit,
    onScanAndSync: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF11182C),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔍", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("StorageScanner и TrackDatabase", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Рекурсивное сканирование /Music, /Music/YandexMusic/Мне нравится, Download и синхронизация найденных треков в локальную TrackDatabase.",
                    color = ColorTextMuted,
                    fontSize = 12.sp
                )

                // Permission row 1: READ_MEDIA_AUDIO / READ_EXTERNAL_STORAGE
                Surface(
                    color = Color(0xFF19233D),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (hasAudioPermission) ColorTurquoise.copy(alpha = 0.5f) else Color(0x55FBBF24))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("READ_MEDIA_AUDIO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = if (hasAudioPermission) "✓ Разрешение выдано" else "Требуется доступ к аудио",
                                color = if (hasAudioPermission) ColorTurquoise else Color(0xFFFBBF24),
                                fontSize = 11.sp
                            )
                        }
                        if (!hasAudioPermission) {
                            Button(
                                onClick = onRequestAudioPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Выдать", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Permission row 2: MANAGE_EXTERNAL_STORAGE (All files access)
                Surface(
                    color = Color(0xFF19233D),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (hasAllFilesPermission) ColorTurquoise.copy(alpha = 0.5f) else Color(0x5538BDF8))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("MANAGE_EXTERNAL_STORAGE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = if (hasAllFilesPermission) "✓ Полный доступ ко всем папкам" else "Нужен для /Music/YandexMusic и USB",
                                color = if (hasAllFilesPermission) ColorTurquoise else Color(0xFF93C5FD),
                                fontSize = 11.sp
                            )
                        }
                        if (!hasAllFilesPermission) {
                            Button(
                                onClick = onRequestAllFilesPermission,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0EA5E9)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Настроить", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }

                Surface(
                    color = Color(0xFF0C1528),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Синхронизировано в TrackDatabase: ${lastScanSyncedCount} • Всего в плеере: ${totalSongsCount}",
                            color = ColorTurquoise,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        val dirsToDisplay = if (scannedDirectories.isNotEmpty()) scannedDirectories else listOf(
                            "/storage/emulated/0/Music",
                            "/storage/emulated/0/Music/YandexMusic/Мне нравится",
                            "/storage/emulated/0/Music/Downloaded",
                            "/storage/emulated/0/Download",
                            "/storage/usbotg/Music"
                        )
                        dirsToDisplay.forEach { dir ->
                            Text("• ${dir}", color = ColorTextMuted, fontSize = 10.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onScanAndSync,
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text(
                    text = if (isScanning) "Сканирование..." else "🔍 Сканировать и синхронизировать",
                    color = Color(0xFF070B18),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = ColorTextMuted)
            }
        }
    )
}

// ------------------------------------------------------------------------------------------------
// FREE MULTI-PROVIDER STREAMING ACCOUNTS MODAL (Audius, Spotify, Yandex, SoundCloud, VK)
// ------------------------------------------------------------------------------------------------
@Composable
fun StreamingAccountsModal(
    accounts: List<StreamingAccount>,
    activeAccountId: String?,
    isConnecting: Boolean,
    statusText: String,
    onConnectAccount: (String, String, String) -> Unit,
    onSelectActiveAccount: (String?) -> Unit,
    onRemoveAccount: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(StreamingProvider.AUDIUS) }
    var credentialInput by remember { mutableStateOf("") }
    var customLabelInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!isConnecting) onDismiss() },
        containerColor = Color(0xFF10182C),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌐", fontSize = 20.sp)
                Spacer(Modifier.width(8.dp))
                Text("Аккаунты и Стриминг (Бесплатно)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(StreamingProvider.values().toList()) { prov ->
                        val selected = selectedProvider == prov
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Color(0xFF0EA5E9) else Color(0xFF19243D),
                            border = BorderStroke(1.dp, if (selected) Color(0xFF38BDF8) else Color(0x22FFFFFF)),
                            modifier = Modifier.clickable {
                                selectedProvider = prov
                                credentialInput = ""
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = prov.displayName,
                                    color = if (selected) Color.White else ColorTextMuted,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                Surface(
                    color = if (selectedProvider.isFreeNoKey) Color(0x2200C996) else Color(0x22FFCC00),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selectedProvider.isFreeNoKey) ColorTurquoise.copy(alpha = 0.4f) else ColorYandexGold.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = selectedProvider.hint,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                var showStreamingYandexWebView by remember { mutableStateOf(false) }

                if (showStreamingYandexWebView) {
                    com.tohsoft.music.ui.components.YandexOAuthWebViewDialog(
                        onTokenCaptured = { capturedToken ->
                            showStreamingYandexWebView = false
                            credentialInput = capturedToken
                            onConnectAccount(StreamingProvider.YANDEX.id, capturedToken, customLabelInput)
                        },
                        onDismiss = { showStreamingYandexWebView = false }
                    )
                }

                if (selectedProvider == StreamingProvider.YANDEX) {
                    Button(
                        onClick = { showStreamingYandexWebView = true },
                        enabled = !isConnecting,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = ColorYandexGold),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "🟡 Войти через Яндекс ID (авто-токен)",
                            color = Color.Black,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = credentialInput,
                    onValueChange = { credentialInput = it },
                    label = {
                        val lbl = when (selectedProvider) {
                            StreamingProvider.AUDIUS -> "@handle или пусто для Топ-треков"
                            StreamingProvider.SPOTIFY -> "Ссылка open.spotify.com/... или токен"
                            else -> "OAuth-токен сервиса"
                        }
                        Text(lbl, color = ColorTextMuted, fontSize = 11.sp)
                    },
                    singleLine = true,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )

                OutlinedTextField(
                    value = customLabelInput,
                    onValueChange = { customLabelInput = it },
                    label = { Text("Название профиля (необязательно)", color = ColorTextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = ColorTurquoise,
                        unfocusedBorderColor = Color(0x44FFFFFF)
                    )
                )

                Button(
                    onClick = {
                        onConnectAccount(selectedProvider.id, credentialInput, customLabelInput)
                    },
                    enabled = !isConnecting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (isConnecting) "Подключение и синхронизация..." else "➕ Подключить бесплатно",
                        color = Color(0xFF070B18),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                if (statusText.isNotBlank()) {
                    Text(
                        text = statusText,
                        color = if (statusText.contains("Ошибка", true)) Color(0xFFF87171) else ColorTurquoise,
                        fontSize = 11.sp
                    )
                }

                Divider(color = Color(0x22FFFFFF))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Сохранённые аккаунты (${accounts.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    if (activeAccountId != null) {
                        Text(
                            text = "Только локальные",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable { onSelectActiveAccount(null) }
                        )
                    }
                }

                if (accounts.isEmpty()) {
                    Text(
                        text = "Пока нет подключённых аккаунтов. Выберите Audius или вставьте публичную ссылку Spotify — API-ключи не требуются!",
                        color = ColorTextMuted,
                        fontSize = 11.sp
                    )
                } else {
                    accounts.forEach { acc ->
                        val isActive = acc.id == activeAccountId
                        Surface(
                            color = if (isActive) Color(0x2200C996) else Color(0xFF19243D),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isActive) ColorTurquoise else Color(0x22FFFFFF)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectActiveAccount(if (isActive) null else acc.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(acc.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        text = "${acc.providerId.uppercase()} • ${acc.tracks.size} треков" + if (isActive) " • Активен ✓" else "",
                                        color = if (isActive) ColorTurquoise else ColorTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                TextButton(onClick = { onSelectActiveAccount(if (isActive) null else acc.id) }) {
                                    Text(if (isActive) "Скрыть" else "Выбрать", color = ColorTurquoise, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                TextButton(onClick = { onRemoveAccount(acc.id) }) {
                                    Text("✕", color = Color(0xFFEF4444), fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isConnecting) {
                TextButton(onClick = onDismiss) {
                    Text("Готово", color = ColorTurquoise, fontWeight = FontWeight.Bold)
                }
            }
        }
    )
}
