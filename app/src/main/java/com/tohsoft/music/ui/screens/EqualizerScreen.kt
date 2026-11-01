package com.tohsoft.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohsoft.music.audio.EqualizerManager

private val ColorTurquoise = Color(0xFF00C996)
private val ColorDarkBgTop = Color(0xFF090D1E)
private val ColorDarkBgMid = Color(0xFF0F1A3B)
private val ColorDarkBgBottom = Color(0xFF070B18)
private val ColorTextMuted = Color(0xFF8EA0BE)

@Composable
fun EqualizerScreen(
    isEqEnabled: Boolean,
    activePreset: String,
    bands: List<Int>,
    bassBoost: Int,
    virtualizer: Int,
    loudness: Int,
    reverbPreset: Short,
    onClose: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (String) -> Unit,
    onBandChange: (Int, Int) -> Unit,
    onBassBoostChange: (Int) -> Unit,
    onVirtualizerChange: (Int) -> Unit,
    onLoudnessChange: (Int) -> Unit,
    onReverbChange: (Short) -> Unit,
    onResetEq: () -> Unit
) {
    val bgBrush = Brush.verticalGradient(
        listOf(ColorDarkBgTop, ColorDarkBgMid, ColorDarkBgBottom)
    )

    val categories = listOf("Все", "Жанры", "Бас и драйв", "Акустика", "Устройства")
    var selectedCategory by remember { mutableStateOf("Все") }

    val filteredPresets = remember(selectedCategory) {
        if (selectedCategory == "Все") {
            EqualizerManager.PRESETS
        } else {
            EqualizerManager.PRESETS.filter { it.category == selectedCategory }
        }
    }

    val frequencies = listOf("31", "63", "125", "250", "500", "1k", "2k", "4k", "8k", "16k")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Text("←", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "ЭКВАЛАЙЗЕР",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isEqEnabled) "ВКЛ" else "ВЫКЛ",
                        color = if (isEqEnabled) ColorTurquoise else ColorTextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Switch(
                        checked = isEqEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = ColorTurquoise,
                            uncheckedTrackColor = Color(0x33FFFFFF)
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Category Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isCatActive = selectedCategory == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isCatActive) ColorTurquoise else Color(0x22FFFFFF))
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            color = if (isCatActive) Color(0xFF090D1E) else Color.White,
                            fontWeight = if (isCatActive) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Presets Horizontal Carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filteredPresets.forEach { preset ->
                    val isSelected = activePreset == preset.name
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) ColorTurquoise else Color(0xFF141E38))
                            .clickable(enabled = isEqEnabled) { onSelectPreset(preset.name) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (isSelected) Color(0xFF090D1E) else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // 10-Band Sliders Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Широкополосная настройка (10 полос)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        TextButton(onClick = onResetEq) {
                            Text("Сброс (0 dB)", color = ColorTurquoise, fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        frequencies.forEachIndexed { index, freqLabel ->
                            val bandVal = bands.getOrElse(index) { 0 }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (bandVal > 0) "+$bandVal" else "$bandVal",
                                    color = if (bandVal != 0 && isEqEnabled) ColorTurquoise else ColorTextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(28.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Slider(
                                        value = bandVal.toFloat(),
                                        onValueChange = { onBandChange(index, it.toInt()) },
                                        valueRange = -15f..15f,
                                        enabled = isEqEnabled,
                                        colors = SliderDefaults.colors(
                                            thumbColor = ColorTurquoise,
                                            activeTrackColor = ColorTurquoise,
                                            inactiveTrackColor = Color(0x33FFFFFF)
                                        ),
                                        modifier = Modifier.fillMaxHeight()
                                    )
                                }

                                Text(
                                    text = freqLabel,
                                    color = ColorTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Sound Effects Controls: BassBoost & Virtualizer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Бас (Bass Boost)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$bassBoost%", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { onBassBoostChange(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ColorTurquoise,
                                activeTrackColor = ColorTurquoise,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                // 3D Virtualizer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("3D Звук", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("$virtualizer%", color = Color(0xFF818CF8), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = virtualizer.toFloat(),
                            onValueChange = { onVirtualizerChange(it.toInt()) },
                            valueRange = 0f..100f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF818CF8),
                                activeTrackColor = Color(0xFF818CF8),
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Loudness & Reverb Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Loudness Enhancer Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Громкость", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(if (loudness > 0) "+$loudness dB" else "$loudness dB", color = ColorTurquoise, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = loudness.toFloat(),
                            onValueChange = { onLoudnessChange(it.toInt()) },
                            valueRange = -15f..15f,
                            enabled = isEqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = ColorTurquoise,
                                activeTrackColor = ColorTurquoise,
                                inactiveTrackColor = Color(0x33FFFFFF)
                            )
                        )
                    }
                }

                // Reverb Presets Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF101935)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    val reverbOptions = listOf("Выкл" to 0.toShort(), "Малая" to 1.toShort(), "Средняя" to 2.toShort(), "Зал" to 3.toShort(), "Сцена" to 6.toShort())
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Реверберация", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            reverbOptions.forEach { (label, revCode) ->
                                val isRev = reverbPreset == revCode
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isRev) ColorTurquoise else Color(0x22FFFFFF))
                                        .clickable(enabled = isEqEnabled) { onReverbChange(revCode) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isRev) Color(0xFF090D1E) else Color.White,
                                        fontWeight = if (isRev) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ColorTurquoise)
            ) {
                Text("ПРИМЕНИТЬ И СОХРАНИТЬ", color = Color(0xFF090D1E), fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
