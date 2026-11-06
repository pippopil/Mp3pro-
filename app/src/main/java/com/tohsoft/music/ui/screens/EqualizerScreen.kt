package com.tohsoft.music.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohsoft.music.audio.EqualizerManager
import com.tohsoft.music.audio.EqPreset
import kotlin.math.*

private val ColorPowerampBg = Color(0xFF090D14)
private val ColorPowerampCard = Color(0xFF131824)
private val ColorTurquoise = Color(0xFF00C996)
private val ColorNeonGreen = Color(0xFF10B981)
private val ColorNeonLime = Color(0xFF84CC16)
private val ColorNeonCyan = Color(0xFF06B6D4)
private val ColorNeonAmber = Color(0xFFF59E0B)
private val ColorTextMuted = Color(0xFF8E9EB5)

private val BAND_COLORS = listOf(
    Color(0xFF8A1818), // 31 Hz - Crimson
    Color(0xFF282C37), // 62 Hz - Slate
    Color(0xFF6B1D82), // 125 Hz - Purple
    Color(0xFF155E38), // 250 Hz - Emerald
    Color(0xFF6B5812), // 500 Hz - Gold Olive
    Color(0xFF0F5A6B), // 1 kHz - Dark Teal
    Color(0xFF1E3A75), // 2 kHz - Cobalt
    Color(0xFF751A4D), // 4 kHz - Magenta
    Color(0xFF6E3912), // 8 kHz - Amber
    Color(0xFF222B38)  // 16 kHz - Charcoal
)

private val BAND_FREQS = listOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)
private val BAND_FREQS_LABEL = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

@Composable
fun PowerampRotaryDial(
    value: Float,
    label: String,
    displayValue: String,
    activeColor: Color = ColorNeonLime,
    sizeDp: Int = 60,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 200f
                        val newVal = (value + delta).coerceIn(0f, 1f)
                        onValueChange(newVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawArc(
                    color = Color(0x33FFFFFF),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val sweep = 270f * value.coerceIn(0f, 1f)
                drawArc(
                    color = activeColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                drawCircle(
                    color = Color(0xFF1E2433),
                    radius = radius * 0.75f,
                    center = center
                )

                val angleRad = Math.toRadians((135f + sweep).toDouble())
                val dotRadius = radius * 0.52f
                val dotX = center.x + (dotRadius * cos(angleRad)).toFloat()
                val dotY = center.y + (dotRadius * sin(angleRad)).toFloat()
                drawCircle(
                    color = Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(dotX, dotY)
                )
            }
        }
        Text(
            text = displayValue,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp
        )
    }
}

@Composable
fun FrequencyResponseSpline(
    bands: List<Int>,
    preampDb: Float,
    activeColor: Color = ColorNeonGreen,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF0B0F19))
            .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
            val w = size.width
            val h = size.height
            val midY = h / 2f
            val dbScale = (h / 32f)

            drawLine(
                color = Color(0x22FFFFFF),
                start = Offset(0f, midY),
                end = Offset(w, midY),
                strokeWidth = 1.dp.toPx()
            )

            val bandCount = 10
            for (i in 0 until bandCount) {
                val gain = bands.getOrElse(i) { 0 }
                if (gain != 0) {
                    val centerX = (i + 0.5f) / bandCount.toFloat() * w
                    val peakY = midY - ((gain + preampDb) * dbScale)
                    val hillPath = Path().apply {
                        moveTo(centerX - (w / 7f), midY)
                        cubicTo(
                            centerX - (w / 14f), peakY,
                            centerX + (w / 14f), peakY,
                            centerX + (w / 7f), midY
                        )
                        close()
                    }
                    val hillColor = BAND_COLORS.getOrElse(i) { ColorNeonGreen }.copy(alpha = 0.35f)
                    drawPath(hillPath, color = hillColor)
                }
            }

            val points = mutableListOf<Offset>()
            val steps = 60
            for (s in 0..steps) {
                val frac = s / steps.toFloat()
                val x = frac * w
                var totalGain = preampDb
                for (i in 0 until bandCount) {
                    val gain = bands.getOrElse(i) { 0 }
                    val bandCenterFrac = (i + 0.5f) / bandCount.toFloat()
                    val dist = (frac - bandCenterFrac) / 0.14f
                    val weight = exp(-(dist * dist) / 2f)
                    totalGain += gain * weight
                }
                val y = (midY - totalGain * dbScale).coerceIn(4f, h - 4f)
                points.add(Offset(x, y))
            }

            val splinePath = Path()
            if (points.isNotEmpty()) {
                splinePath.moveTo(points[0].x, points[0].y)
                for (p in 1 until points.size) {
                    splinePath.lineTo(points[p].x, points[p].y)
                }
            }

            drawPath(
                path = splinePath,
                color = activeColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun EqualizerScreen(
    isEqEnabled: Boolean,
    activePreset: String,
    bands: List<Int>,
    bassBoost: Int,
    virtualizer: Int,
    loudness: Int,
    reverbPreset: Short,
    isParametricMode: Boolean = false,
    isAutoEqEnabled: Boolean = true,
    preampDb: Float = 0.0f,
    isLimiterEnabled: Boolean = true,
    trebleLevel: Int = 30,
    currentSongTitle: String = "MP3 Player Pro",
    currentSongGenre: String = "Все",
    onClose: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onReverbChange: (Short) -> Unit,
    onResetEq: () -> Unit,
    onToggleParametricMode: (Boolean) -> Unit = {},
    onToggleAutoEq: (Boolean) -> Unit = {},
    onPreampChange: (Float) -> Unit = {},
    onToggleLimiter: (Boolean) -> Unit = {},
    onTrebleChange: (Int) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(if (isParametricMode) 1 else 0) }
    var isPresetModalOpen by remember { mutableStateOf(false) }
    val bandQValues = remember { mutableStateListOf(0.70f, 0.85f, 1.20f, 0.87f, 1.10f, 1.41f, 1.60f, 2.21f, 1.0f, 0.75f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPowerampBg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onClose) {
                    Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "ЭКВАЛАЙЗЕР PRO",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "32-Bit Floating Point DSP",
                        color = ColorTurquoise,
                        fontSize = 10.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isAutoEqEnabled) Color(0x3310B981) else Color(0x22FFFFFF),
                    border = BorderStroke(1.dp, if (isAutoEqEnabled) ColorNeonGreen else Color(0x44FFFFFF)),
                    modifier = Modifier.clickable { onToggleAutoEq(!isAutoEqEnabled) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚡", fontSize = 12.sp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = if (isAutoEqEnabled) "АВТО-EQ" else "РУЧНОЙ",
                            color = if (isAutoEqEnabled) ColorNeonGreen else ColorTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEqEnabled) "ВКЛ" else "ВЫКЛ",
                        color = if (isEqEnabled) ColorNeonGreen else ColorTextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(4.dp))
                    Switch(
                        checked = isEqEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorNeonGreen,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // MAIN EQ VIEW
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> {
                        // TAB 0: 10-BAND GRAPHIC FADERS
                        Column(modifier = Modifier.fillMaxSize()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Preamp
                                Column(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0.0", color = ColorTextMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier.weight(1f).width(28.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Slider(
                                            value = preampDb,
                                            onValueChange = { onPreampChange(it) },
                                            valueRange = -12f..12f,
                                            enabled = isEqEnabled,
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = ColorNeonAmber,
                                                inactiveTrackColor = Color(0x33FFFFFF)
                                            )
                                        )
                                    }
                                    Text("Преамп", color = ColorNeonAmber, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }

                                // 10 Frequency Faders
                                BAND_FREQS_LABEL.forEachIndexed { index, freqLabel ->
                                    val bandVal = bands.getOrElse(index) { 0 }
                                    Column(
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (bandVal > 0) "+$bandVal" else "$bandVal",
                                            color = if (bandVal != 0 && isEqEnabled) ColorNeonGreen else ColorTextMuted,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Box(
                                            modifier = Modifier.weight(1f).width(28.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Slider(
                                                value = bandVal.toFloat(),
                                                onValueChange = { onBandChange(index, it.toInt()) },
                                                valueRange = -15f..15f,
                                                steps = 29,
                                                enabled = isEqEnabled,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = ColorNeonGreen,
                                                    inactiveTrackColor = Color(0x33FFFFFF)
                                                )
                                            )
                                        }

                                        Text(
                                            text = freqLabel,
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // TAB 1: PARAMETRIC STUDIO CARDS
                        LazyRow(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.width(135.dp).fillMaxHeight(),
                                    colors = CardDefaults.cardColors(containerColor = ColorPowerampCard),
                                    shape = RoundedCornerShape(18.dp),
                                    border = BorderStroke(1.dp, Color(0x33FFFFFF))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Преамп", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Box(
                                            modifier = Modifier.weight(1f).width(36.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Slider(
                                                value = preampDb,
                                                onValueChange = { onPreampChange(it) },
                                                valueRange = -12f..12f,
                                                enabled = isEqEnabled,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color.White,
                                                    activeTrackColor = ColorNeonAmber,
                                                    inactiveTrackColor = Color(0x33FFFFFF)
                                                )
                                            )
                                        }
                                        Text(String.format("%.1f dB", preampDb), color = ColorNeonAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            itemsIndexed(BAND_FREQS) { index, freqHz ->
                                val gain = bands.getOrElse(index) { 0 }
                                val qVal = bandQValues.getOrElse(index) { 1.41f }
                                val cardBg = BAND_COLORS.getOrElse(index) { ColorPowerampCard }

                                Card(
                                    modifier = Modifier.width(190.dp).fillMaxHeight(),
                                    colors = CardDefaults.cardColors(containerColor = cardBg),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0x44FFFFFF))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(10.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0x33000000)
                                            ) {
                                                Text(
                                                    text = "L+R",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text("∿", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.width(42.dp).fillMaxHeight(),
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Slider(
                                                    value = gain.toFloat(),
                                                    onValueChange = { onBandChange(index, it.toInt()) },
                                                    valueRange = -15f..15f,
                                                    steps = 29,
                                                    enabled = isEqEnabled,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color.White,
                                                        activeTrackColor = ColorNeonGreen,
                                                        inactiveTrackColor = Color(0x33FFFFFF)
                                                    )
                                                )
                                            }

                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.SpaceEvenly,
                                                modifier = Modifier.weight(1f).fillMaxHeight()
                                            ) {
                                                PowerampRotaryDial(
                                                    value = (qVal / 5f).coerceIn(0f, 1f),
                                                    label = "Q",
                                                    displayValue = String.format("%.2f", qVal),
                                                    activeColor = ColorNeonCyan,
                                                    sizeDp = 52,
                                                    onValueChange = { frac ->
                                                        bandQValues[index] = (frac * 5f).coerceIn(0.2f, 5.0f)
                                                    }
                                                )

                                                PowerampRotaryDial(
                                                    value = (index / 9f).coerceIn(0f, 1f),
                                                    label = "Част.",
                                                    displayValue = BAND_FREQS_LABEL[index],
                                                    activeColor = ColorNeonLime,
                                                    sizeDp = 52,
                                                    onValueChange = {}
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Усил. " + (if (gain > 0) "+$gain" else "$gain"),
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Част. " + BAND_FREQS_LABEL[index],
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // TAB 2: SOUND EFFECTS & EXTERNAL DSP
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF14203D)),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, ColorTurquoise.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🎚", fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Системный DSP для сторонних плееров", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        text = "Эквалайзер перехватывает сессии Яндекс Музыки, Spotify и VK (Session 0). Все звуковые эффекты применяются ко всей системе Android.",
                                        color = ColorTextMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0x3300C996)
                                    ) {
                                        Text(
                                            text = "🟢 DSP-микшер активен: все эффекты включены",
                                            color = ColorTurquoise,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            Text("3D Звук (Virtualizer): " + virtualizer + "%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Slider(
                                value = virtualizer.toFloat(),
                                onValueChange = { onVirtualizerChange(it.toInt()) },
                                valueRange = 0f..100f,
                                enabled = isEqEnabled,
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ColorNeonCyan)
                            )

                            Spacer(Modifier.height(8.dp))

                            Text("Реверберация (Акустическое пространство)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            val reverbNames = listOf("Откл.", "Малая комната", "Средняя комната", "Большая комната", "Большой зал", "Пластина (Plate)")
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                reverbNames.forEachIndexed { revIdx, name ->
                                    val isRevActive = reverbPreset.toInt() == revIdx
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isRevActive) ColorTurquoise else Color(0xFF1E283E),
                                        modifier = Modifier.clickable(enabled = isEqEnabled) { onReverbChange(revIdx.toShort()) }
                                    ) {
                                        Text(
                                            text = name,
                                            color = if (isRevActive) Color(0xFF090D14) else Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text("Усиление громкости (Loudness Enhancer): " + loudness + " mB", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Slider(
                                value = loudness.toFloat(),
                                onValueChange = { onLoudnessChange(it.toInt()) },
                                valueRange = 0f..1000f,
                                enabled = isEqEnabled,
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = ColorNeonLime)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // MIDDLE SECTION: LIVE SPLINE FREQUENCY RESPONSE
            FrequencyResponseSpline(
                bands = bands,
                preampDb = preampDb,
                activeColor = ColorNeonGreen
            )

            Spacer(Modifier.height(6.dp))

            // PRESET SELECTOR CAPSULE & DVC STATUS BADGE
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (selectedTab == 1) "ⓘ DVC EQ-PARAM LMT" else "ⓘ DVC EQ-10 LMT",
                    color = ColorTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ColorPowerampCard,
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    modifier = Modifier.clickable { isPresetModalOpen = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = activePreset,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("▾", color = ColorTurquoise, fontSize = 12.sp)
                    }
                }

                IconButton(onClick = onResetEq) {
                    Text("↺", color = ColorTextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(6.dp))

            // BOTTOM CONTROLS: [ Экв. ], [ Лимит ], Rotary Bass, Rotary Treble
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isEqEnabled) Color(0x3300C996) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isEqEnabled) ColorTurquoise else Color(0x33FFFFFF)),
                        modifier = Modifier.clickable { onToggleEnabled(!isEqEnabled) }
                    ) {
                        Text(
                            text = "Экв.",
                            color = if (isEqEnabled) ColorTurquoise else ColorTextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isLimiterEnabled) Color(0x3310B981) else Color(0x22FFFFFF),
                        border = BorderStroke(1.dp, if (isLimiterEnabled) ColorNeonGreen else Color(0x33FFFFFF)),
                        modifier = Modifier.clickable { onToggleLimiter(!isLimiterEnabled) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("|||", fontSize = 10.sp, color = ColorNeonGreen, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Лимит",
                                color = if (isLimiterEnabled) ColorNeonGreen else ColorTextMuted,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                PowerampRotaryDial(
                    value = (bassBoost / 100f).coerceIn(0f, 1f),
                    label = "Низк.",
                    displayValue = "$bassBoost%",
                    activeColor = ColorNeonGreen,
                    sizeDp = 50,
                    onValueChange = { frac ->
                        onBassBoostChange((frac * 100).toInt())
                    }
                )

                PowerampRotaryDial(
                    value = (trebleLevel / 100f).coerceIn(0f, 1f),
                    label = "Выс.",
                    displayValue = "$trebleLevel%",
                    activeColor = ColorNeonCyan,
                    sizeDp = 50,
                    onValueChange = { frac ->
                        onTrebleChange((frac * 100).toInt())
                    }
                )
            }

            Spacer(Modifier.height(6.dp))

            // BOTTOM 3-TAB SWITCHER BAR
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = ColorPowerampCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedTab = 0
                        onToggleParametricMode(false)
                    }) {
                        Text(
                            text = "📊",
                            fontSize = 20.sp,
                            modifier = if (selectedTab == 0) Modifier.background(Color(0x3300C996), CircleShape).padding(4.dp) else Modifier
                        )
                    }

                    IconButton(onClick = {
                        selectedTab = 1
                        onToggleParametricMode(true)
                    }) {
                        Text(
                            text = "🎛️",
                            fontSize = 20.sp,
                            modifier = if (selectedTab == 1) Modifier.background(Color(0x3300C996), CircleShape).padding(4.dp) else Modifier
                        )
                    }

                    IconButton(onClick = {
                        selectedTab = 2
                    }) {
                        Text(
                            text = "🎚️",
                            fontSize = 20.sp,
                            modifier = if (selectedTab == 2) Modifier.background(Color(0x3300C996), CircleShape).padding(4.dp) else Modifier
                        )
                    }
                }
            }
        }

        // PRESET MODAL BOTTOM SHEET
        if (isPresetModalOpen) {
            AlertDialog(
                onDismissRequest = { isPresetModalOpen = false },
                containerColor = Color(0xFF101626),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Библиотека пресетов (36)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        TextButton(onClick = { isPresetModalOpen = false }) {
                            Text("Закрыть", color = ColorTurquoise)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val categories = listOf("Студия", "Жанры", "Бас и драйв", "Акустика", "Устройства")
                        categories.forEach { cat ->
                            Text(
                                text = cat.uppercase(),
                                color = ColorTurquoise,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                            EqualizerManager.PRESETS.filter { it.category == cat }.forEach { preset ->
                                val isSelected = activePreset == preset.name
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) Color(0x3300C996) else Color(0x11FFFFFF),
                                    border = BorderStroke(1.dp, if (isSelected) ColorTurquoise else Color(0x22FFFFFF)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp)
                                        .clickable {
                                            onSelectPreset(preset.name)
                                            isPresetModalOpen = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = preset.name,
                                            color = if (isSelected) ColorTurquoise else Color.White,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (isSelected) {
                                            Text("✓", color = ColorTurquoise, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}
