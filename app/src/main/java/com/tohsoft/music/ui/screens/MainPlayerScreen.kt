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
import coil.compose.AsyncImage
import com.tohsoft.music.data.model.Song
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
    onOpenFullPlayer: () -> Unit,
    onCloseFullPlayer: () -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onCloseSongMenu: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onCloseSleepTimer: () -> Unit,
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
    onDownloadOnlineTrack: (String, String, String, String, String) -> Unit = { _, _, _, _, _ -> }
) {
    val bgBrush = Brush.verticalGradient(
        listOf(ColorDarkBgTop, ColorDarkBgMid, ColorDarkBgBottom)
    )

    Box(modifier = Modifier.fillMaxSize().background(bgBrush)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopTabsBar(
                    activeTab = uiState.activeTab,
                    onTabSelected = onTabSelected,
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
                if (uiState.activeTab == 0) {
                    SongsTabContent(
                        songs = uiState.filteredSongs,
                        currentSong = uiState.currentSong,
                        isPlaying = uiState.isPlaying,
                        searchQuery = uiState.searchQuery,
                        activeGenreFilter = uiState.activeGenreFilter,
                        sortMode = uiState.sortMode,
                        onSearchChange = onSearchQueryChanged,
                        onSelectGenreFilter = onSelectGenreFilter,
                        onSortChange = onSortChange,
                        onOpenOnlineSearch = onOpenOnlineSearch,
                        onSongClick = onSongClick,
                        onSongMenuClick = onSongMenuClick,
                        onOpenYandexModal = onOpenYandexModal,
                        onShuffleAll = {
                            onToggleShuffle()
                            if (uiState.filteredSongs.isNotEmpty()) {
                                onSongClick(uiState.filteredSongs.random())
                            }
                        }
                    )
                } else {
                    SettingsTabContent(
                        uiState = uiState,
                        onHideTabsClick = onHideTabsClick,
                        onSleepTimerClick = onSleepTimerClick,
                        onThemePickerClick = onThemePickerClick,
                        onWallpaperPickerClick = onWallpaperPickerClick,
                        onOpenEqualizer = onOpenEqualizer,
                        onToggleSetting = onToggleSetting,
                        onOpenYandexModal = onOpenYandexModal,
                        onSetCoverStyle = onSetCoverStyle
                    )
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
                    onMenuClick = { onSongMenuClick(song) }
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
                onClose = onCloseEqualizer,
                onToggleEnabled = onToggleEq,
                onSelectPreset = onSelectEqPreset,
                onBandChange = onEqBandChange,
                onBassBoostChange = onBassBoostChange,
                onVirtualizerChange = onVirtualizerChange,
                onLoudnessChange = onLoudnessChange,
                onReverbChange = onReverbChange,
                onResetEq = onResetEq
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
                onProperties = { onPropertiesClick(uiState.selectedSongForMenu); onCloseSongMenu() }
            )
        }

        // Sleep Timer Dialog
        if (uiState.isSleepTimerDialogOpen) {
            SleepTimerModal(
                onDismiss = onCloseSleepTimer,
                onSetTimer = { onCloseSleepTimer() }
            )
        }

        // Hide Tabs Dialog
        if (uiState.isHideTabsDialogOpen) {
            HideTabsModal(onDismiss = onCloseHideTabs)
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

        // Song Properties Dialog
        if (uiState.isPropertiesDialogOpen && uiState.selectedSongForMenu != null) {
            SongPropertiesModal(
                song = uiState.selectedSongForMenu,
                onDismiss = onCloseProperties
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
    }
}

// ------------------------------------------------------------------------------------------------
// TOP TABS BAR WITH YANDEX & EQ QUICK ACCESS
// ------------------------------------------------------------------------------------------------
@Composable
fun TopTabsBar(
    activeTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenYandexModal: () -> Unit,
    onOpenEqualizer: () -> Unit
) {
    Surface(
        color = Color(0xFF090D1E),
        modifier = Modifier.fillMaxWidth().statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: Песни
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(0) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ПЕСНИ",
                    color = if (activeTab == 0) ColorTurquoise else ColorTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (activeTab == 0) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(60.dp)
                            .height(3.dp)
                            .background(ColorTurquoise, RoundedCornerShape(2.dp))
                    )
                }
            }

            // Tab 2: Настройки
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelected(1) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "НАСТРОЙКИ",
                    color = if (activeTab == 1) ColorTurquoise else ColorTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                if (activeTab == 1) {
                    Box(
                        Modifier
                            .padding(top = 4.dp)
                            .width(80.dp)
                            .height(3.dp)
                            .background(ColorTurquoise, RoundedCornerShape(2.dp))
                    )
                }
            }

            // Quick Shortcut: Yandex Music
            IconButton(
                onClick = onOpenYandexModal,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x22FFCC00), CircleShape)
            ) {
                Text("🟡", fontSize = 16.sp)
            }

            Spacer(Modifier.width(6.dp))

            // Quick Shortcut: 10-Band EQ
            IconButton(
                onClick = onOpenEqualizer,
                modifier = Modifier
                    .size(38.dp)
                    .background(Color(0x2200C996), CircleShape)
            ) {
                Text("🎚", fontSize = 16.sp)
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------
// SONGS TAB CONTENT: SEARCH, GENRE FILTER CHIPS & SONG LIST
// ------------------------------------------------------------------------------------------------
@Composable
fun SongsTabContent(
    songs: List<Song>,
    currentSong: Song?,
    isPlaying: Boolean,
    searchQuery: String,
    activeGenreFilter: String,
    sortMode: com.tohsoft.music.viewmodel.TrackSortMode = com.tohsoft.music.viewmodel.TrackSortMode.TITLE_ASC,
    onSearchChange: (String) -> Unit,
    onSelectGenreFilter: (String) -> Unit,
    onSortChange: (com.tohsoft.music.viewmodel.TrackSortMode) -> Unit = {},
    onOpenOnlineSearch: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onSongMenuClick: (Song) -> Unit,
    onOpenYandexModal: () -> Unit,
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    com.tohsoft.music.viewmodel.TrackSortMode.TITLE_ASC to "🔤 А-Я",
                    com.tohsoft.music.viewmodel.TrackSortMode.ARTIST to "🎤 Артист",
                    com.tohsoft.music.viewmodel.TrackSortMode.ALBUM to "💿 Альбом"
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
                            color = if (isModeActive) Color(0xFF090D1E) else Color.White
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
    val infiniteTransition = rememberInfiniteTransition(label = "DancingFrog")
    
    // Прыжки в такт бита
    val bounceY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) -5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "FrogBounce"
    )

    // Наклон корпуса / покачивание
    val rotation by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = if (isPlaying) 12f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 260, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "FrogRotate"
    )

    // Мерцание музыкальных нот
    val noteAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPlaying) 1f else 0.3f,
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
                Text(
                    text = "${song.artist} • ${song.genre}",
                    color = ColorTextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
    onMenuClick: () -> Unit
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
            .padding(horizontal = 20.dp)
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onClose) {
                Text("←", color = Color.White, fontSize = 24.sp)
            }

            // Page indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(ColorTextMuted))
                Box(Modifier.size(8.dp).clip(CircleShape).background(ColorTurquoise))
            }

            Row {
                IconButton(onClick = onOpenEqualizer) {
                    Text("🎚", color = ColorTurquoise, fontSize = 20.sp)
                }
                IconButton(onClick = onCycleCoverStyle) {
                    Text("🎨", color = ColorTurquoise, fontSize = 20.sp)
                }
                IconButton(onClick = onSleepTimerClick) {
                    Text("⏱", color = ColorTextMuted, fontSize = 20.sp)
                }
                IconButton(onClick = onMenuClick) {
                    Text("⋮", color = Color.White, fontSize = 22.sp)
                }
            }
        }

        // Cover Style Switcher Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            CoverStyle.values().forEach { style ->
                val isSelected = (style == coverStyle)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) ColorTurquoise else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) ColorTurquoise else Color(0x33FFFFFF)),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clickable { onSetCoverStyle(style) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(style.icon, fontSize = 13.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = style.title,
                            color = if (isSelected) Color(0xFF040711) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(0.2f))

        // CENTER: INTERACTIVE COVER VIEW (Vinyl spin / 3D Glass / Neon / Poster) + Subwoofer Bass Boost
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            PlayerCoverView(
                coverModel = song.coverUrl ?: song.albumArtUri,
                isPlaying = isPlaying,
                style = coverStyle,
                bassBoostGain = bassBoostGain,
                audioSessionId = audioSessionId,
                onBassBoostChange = onBassBoostChange,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.weight(0.15f))

        // SAMSUNG DOLBY ATMOS & SOUND ENHANCEMENT ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dolby Atmos Badge
            Surface(
                color = Color(0xFF101B38),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, ColorTurquoise.copy(alpha = 0.5f)),
                modifier = Modifier.clickable { onOpenEqualizer() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("DOLBY ATMOS", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("•", color = ColorTextMuted, fontSize = 10.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("Hi-Res 24-bit 96kHz", color = Color.White, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Online HD Cover Search Button
            Surface(
                color = Color(0xFF132247),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color(0x444A689B)),
                modifier = Modifier.clickable { onSearchCover() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(if (isSearchingCover) "⏳" else "🔍", fontSize = 11.sp)
                    Spacer(Modifier.width(4.dp))
                    Text("HD Обложка", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Cover Search Notification Banner if present
        if (coverSearchMessage.isNotBlank()) {
            Text(
                text = coverSearchMessage,
                color = ColorTurquoise,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
            )
        }

        Spacer(Modifier.height(8.dp))

        // SONG TITLE & ARTIST
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = song.title,
                color = ColorTurquoise,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = song.artist,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${song.album} • ${song.bitrate}",
                color = ColorTextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(14.dp))

        // PROGRESS SEEKBAR
        val posFormatted = formatMs(currentPositionMs)
        val durFormatted = formatMs(durationMs)
        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

        Slider(
            value = progress,
            onValueChange = { frac ->
                val newPos = (frac * durationMs).toLong()
                onSeekTo(newPos)
            },
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
            Text(posFormatted, color = ColorTextMuted, fontSize = 12.sp)
            Text(durFormatted, color = ColorTextMuted, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        // PLAYBACK CONTROLS ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { /* Favorite */ }) {
                Text("♡", color = ColorTextMuted, fontSize = 22.sp)
            }
            IconButton(onClick = onToggleShuffle) {
                Text("🔀", color = if (isShuffle) ColorTurquoise else ColorTextMuted, fontSize = 20.sp)
            }
            IconButton(onClick = onPrev) {
                Text("⏮", color = Color.White, fontSize = 24.sp)
            }
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ColorTurquoise)
                    .clickable(onClick = onTogglePlayPause),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    color = Color(0xFF070B18),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            IconButton(onClick = onNext) {
                Text("⏭", color = Color.White, fontSize = 24.sp)
            }
            IconButton(onClick = onToggleRepeat) {
                val repIcon = when (repeatMode) {
                    RepeatMode.ONE -> "🔂"
                    RepeatMode.ALL -> "🔁"
                    RepeatMode.NONE -> "↩"
                }
                Text(repIcon, color = if (repeatMode != RepeatMode.NONE) ColorTurquoise else ColorTextMuted, fontSize = 20.sp)
            }
            IconButton(onClick = onCycleCoverStyle) {
                Text(coverStyle.icon, fontSize = 20.sp)
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
    onSetCoverStyle: (CoverStyle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
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

        // SAMSUNG DOLBY ATMOS & SOUNDALIVE CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .clickable(onClick = onOpenEqualizer),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF101B38)),
            border = BorderStroke(1.dp, ColorTurquoise.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x3300C996), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎚", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Samsung Dolby Atmos & SoundAlive", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("10-полосный эквалайзер, Bass Boost, Виртуализатор 3D", color = ColorTextMuted, fontSize = 12.sp)
                }
                Text("›", color = ColorTurquoise, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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

        SettingActionItem(icon = "🔍", title = "Сканирование музыки в памяти", onClick = {})
        SettingActionItem(icon = "🎚", title = "Эквалайзер и звуковые эффекты", onClick = onOpenEqualizer)
        SettingActionItem(icon = "👁", title = "Скрыть вкладки", onClick = onHideTabsClick)
        SettingActionItem(icon = "📁", title = "Черные папки", onClick = {})
        SettingActionItem(icon = "🎨", title = "Менять тему", onClick = onThemePickerClick)
        SettingActionItem(icon = "🌐", title = "язык – русский (Россия)", onClick = {})

        SettingSwitchItem(
            icon = "✂",
            title = "Скрыть короткие песни",
            checked = uiState.hideShortSongs,
            onCheckedChange = { onToggleSetting("hideShortSongs", it) }
        )
        SettingSwitchItem(
            icon = "📌",
            title = "Запомнить игру",
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
        SettingActionItem(icon = "📲", title = "SmartShare (Приемник)", onClick = {})

        SettingSwitchItem(
            icon = "🔊",
            title = "Объем исчезает и исчезает",
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

        SettingActionItem(icon = "💬", title = "Обратная связь", onClick = {})
        SettingActionItem(icon = "⭐", title = "Оценить 5 звезд", onClick = {})
        SettingActionItem(icon = "🔗", title = "Поделиться", onClick = {})

        Spacer(Modifier.height(24.dp))
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 90.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Мп3-плеер", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Версия приложения 6.8 Modern", color = ColorTextMuted, fontSize = 13.sp)
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
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = tokenInput,
                    onValueChange = { tokenInput = it },
                    label = { Text("OAuth Токен Яндекс Музыки", color = ColorTextMuted, fontSize = 12.sp) },
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
    onProperties: () -> Unit
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
            MenuItem(icon = "⏭", title = "Играть дальше", onClick = onDismiss)
            MenuItem(icon = "➕", title = "Добавить в очередь", onClick = onDismiss)
            MenuItem(icon = "♡", title = "Добавить в избранное", onClick = onDismiss)
            MenuItem(icon = "ℹ", title = "Свойства трека", onClick = onProperties)
        }
    }
}

@Composable
fun MenuItem(icon: String, title: String, onClick: () -> Unit) {
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
    onSetTimer: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("10 секунд") }
    val options = listOf("3 секунды", "5 секунд", "10 секунд", "15 секунд", "30 секунд", "Другое (ы):")

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Таймер сна", color = Color.Black, fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                options.forEach { opt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedOption = opt }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selectedOption == opt),
                            onClick = { selectedOption = opt },
                            colors = RadioButtonDefaults.colors(selectedColor = ColorTurquoise)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(opt, color = Color.Black, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSetTimer) {
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
fun HideTabsModal(onDismiss: () -> Unit) {
    val items = listOf("песни", "Альбомы", "Художники", "Playlist", "Папки", "настройки")
    val checkedState = remember { mutableStateMapOf("песни" to true, "настройки" to true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Выберите, чтобы показать", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column {
                items.forEach { tabName ->
                    val checked = checkedState[tabName] ?: false
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { checkedState[tabName] = !checked }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { checkedState[tabName] = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1E3A8A))
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(tabName, color = Color.Black, fontSize = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
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
// SONG PROPERTIES MODAL
// ------------------------------------------------------------------------------------------------
@Composable
fun SongPropertiesModal(song: Song, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF10162A),
        title = {
            Text("Свойства", color = Color.White, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PropertyRow("Название", song.title)
                PropertyRow("Исполнитель", song.artist)
                PropertyRow("Альбом", song.album)
                PropertyRow("Длительность", song.durationFormatted)
                PropertyRow("Битрейт", song.bitrate)
                PropertyRow("Жанр", song.genre)
                PropertyRow("Путь", song.dataPath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = ColorTurquoise, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column {
        Text(label, color = ColorTextMuted, fontSize = 12.sp)
        Text(value, color = Color.White, fontSize = 14.sp)
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
    onDownload: (title: String, artist: String, url: String, genre: String, coverUrl: String) -> Unit,
    downloadProgress: Int,
    downloadMessage: String
) {
    var searchQuery by remember { mutableStateOf("") }

    val presetOnlineTracks = remember {
        listOf(
            Triple("Cyber City Overdrive", "Synthwave Syndicate", "Phonk / Synth") to ("https://actions.google.com/sounds/v1/science_fiction/alien_spaceship_atmosphere.ogg" to "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80"),
            Triple("Bass Pressure VIP", "Neurofunk Sub HQ", "Drum & Bass") to ("https://actions.google.com/sounds/v1/science_fiction/alien_pulse.ogg" to "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80"),
            Triple("Midnight Groove Lounge", "Tokyo Lo-Fi Beat", "Lo-Fi Beats") to ("https://actions.google.com/sounds/v1/ambiences/subway_ambient_murmur.ogg" to "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80"),
            Triple("Heavy Metal Resonance", "Ironclad Distortion", "Rock / Metal") to ("https://actions.google.com/sounds/v1/transportation/freight_train_horn_loud.ogg" to "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"),
            Triple("Deep Space Sub Harmonic", "Cosmic Bass Lab", "Deep House") to ("https://actions.google.com/sounds/v1/science_fiction/spaceship_door_open.ogg" to "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?w=500&q=80")
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
                                        text = track.genre + " • 320 kbps HQ",
                                        color = ColorTurquoise,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDownload(track.title, track.artist, track.audioUrl, track.genre, track.coverUrl)
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
                                    onDownload(title, artist, streamUrl, genre, coverUrl)
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
