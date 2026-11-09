package com.tohsoft.music.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

enum class CoverStyle(val title: String, val icon: String) {
    VINYL_RECORD("Винил", "💿"),
    GLASS_3D("3D Стекло", "🪞"),
    NEON_AURA("Неон", "✨"),
    POSTER("Постер", "🖼️")
}

/**
 * Интерактивный компонент отображения обложки в плеере.
 * Поддерживает 4 визуальных стиля, Coil загрузку онлайн/локальных обложек
 * и плавное вращение виниловой пластинки при воспроизведении.
 */
@Composable
fun PlayerCoverView(
    coverModel: Any?,
    isPlaying: Boolean,
    style: CoverStyle = CoverStyle.VINYL_RECORD,
    bassBoostGain: Int = 25,
    audioSessionId: Int = 0,
    onBassBoostChange: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "VinylCover")

    // Реалистичное вращение виниловой пластинки 33⅓ RPM (2800 мс на полный оборот)
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SpinAngle"
    )

    // Subwoofer Bass Pulse: деликатная сабвуферная пульсация
    val bassMultiplier = (bassBoostGain.coerceIn(0, 100) / 100f)
    val bassScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isPlaying && bassBoostGain > 0) 1.0f + (bassMultiplier * 0.045f) else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 460, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "BassPulse"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(190.dp)
                .graphicsLayer(scaleX = bassScale, scaleY = bassScale),
            contentAlignment = Alignment.Center
        ) {
            when (style) {
                CoverStyle.VINYL_RECORD -> {
                    // Виниловая пластинка с аналоговыми канавками, радиальными бликами света и вращением
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isPlaying) angle else 0f)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF0A0F1D),
                                        Color(0xFF1E293B),
                                        Color(0xFF38BDF8).copy(alpha = 0.28f),
                                        Color(0xFF0A0F1D),
                                        Color(0xFF1E293B),
                                        Color(0xFF38BDF8).copy(alpha = 0.28f),
                                        Color(0xFF0A0F1D)
                                    )
                                )
                            )
                            .border(
                                width = if (bassBoostGain > 40) 4.dp else 3.dp,
                                color = if (bassBoostGain > 40) Color(0xFF06B6D4) else Color(0xFF334155),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Концентрические канавки винила
                        Box(modifier = Modifier.size(168.dp).border(1.dp, Color(0x3364748B), CircleShape))
                        Box(modifier = Modifier.size(148.dp).border(1.dp, Color(0x2264748B), CircleShape))
                        Box(modifier = Modifier.size(128.dp).border(1.dp, Color(0x3364748B), CircleShape))
                        Box(modifier = Modifier.size(108.dp).border(1.dp, Color(0x2264748B), CircleShape))

                        // Центральный вращающийся лейбл с обложкой
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF06B6D4), Color(0xFF4F46E5))
                                    )
                                )
                                .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverModel != null && coverModel.toString().isNotBlank()) {
                                AsyncImage(
                                    model = coverModel,
                                    contentDescription = "Cover Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "♫",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Центральное отверстие шпинделя
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF020617))
                                .border(1.5.dp, Color(0xFFE2E8F0), CircleShape)
                        )
                    }

                    // Тонарм проигрывателя (стилус/игла)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-2).dp, y = 4.dp)
                            .size(width = 6.dp, height = 36.dp)
                            .rotate(if (isPlaying) 26f else 10f)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFE2E8F0), Color(0xFF64748B))
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    )
                }

                CoverStyle.GLASS_3D -> {
                    // 3D Карточка со стеклянной фаской и тенями
                    Card(
                        modifier = Modifier
                            .size(180.dp)
                            .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF06B6D4)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                Brush.linearGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B)))
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (coverModel != null && coverModel.toString().isNotBlank()) {
                                AsyncImage(
                                    model = coverModel,
                                    contentDescription = "Cover Art",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text("♫", color = Color(0xFF06B6D4), fontSize = 48.sp)
                            }

                            // Стеклянный блик
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                Color.White.copy(alpha = 0.18f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            )
                        }
                    }
                }

                CoverStyle.NEON_AURA -> {
                    // Неоновое фоновое свечение (Аура)
                    if (coverModel != null && coverModel.toString().isNotBlank()) {
                        AsyncImage(
                            model = coverModel,
                            contentDescription = null,
                            modifier = Modifier
                                .size(190.dp)
                                .blur(24.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .blur(22.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF06B6D4).copy(alpha = 0.45f))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF0C142B))
                            .border(2.dp, Color(0xFF22D3EE), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverModel != null && coverModel.toString().isNotBlank()) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = "Cover Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("♫", color = Color(0xFF22D3EE), fontSize = 48.sp)
                        }
                    }
                }

                CoverStyle.POSTER -> {
                    // Полноэкранный стиль
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A)))
                            )
                            .shadow(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (coverModel != null && coverModel.toString().isNotBlank()) {
                            AsyncImage(
                                model = coverModel,
                                contentDescription = "Cover Art",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text("♫", color = Color.White, fontSize = 54.sp)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Интерактивный регулятор Bass Boost прямо под обложкой с трансляцией в AudioSession
        if (onBassBoostChange != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2206B6D4))
                    .border(1.dp, Color(0x4406B6D4), RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // Consume touches so they NEVER fall through to the cover beneath
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "🔊 БАС: $bassBoostGain%",
                    color = Color(0xFF22D3EE),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )

                if (audioSessionId > 0) {
                    Text(
                        text = "• #$audioSessionId",
                        color = Color(0xFF8EA0BE),
                        fontSize = 9.sp
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, radius = 12.dp)
                            ) {
                                val nextGain = (bassBoostGain - 10).coerceAtLeast(0)
                                onBassBoostChange(nextGain)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("-", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00C996))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = rememberRipple(bounded = true, radius = 12.dp)
                            ) {
                                val nextGain = (bassBoostGain + 10).coerceAtMost(100)
                                onBassBoostChange(nextGain)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color(0xFF090D1E), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
