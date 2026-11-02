package com.tohsoft.music.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.PresetReverb
import android.media.audiofx.Virtualizer
import android.os.Build

data class EqParametricBand(
    val id: Int,
    val freqHz: Int,
    val gainDb: Float,
    val qFactor: Float,
    val hexColor: String
)

data class EqPreset(
    val name: String,
    val category: String,
    val bands: List<Int>, // 10 bands in dB: [-15..15]
    val bassBoost: Int = 20, // 0..100%
    val virtualizer: Int = 15, // 0..100%
    val reverb: Short = PresetReverb.PRESET_NONE,
    val treble: Int = 30,
    val preampDb: Float = 0.0f,
    val parametricBands: List<EqParametricBand> = emptyList()
)

class EqualizerManager(private var audioSessionId: Int) {

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var presetReverb: PresetReverb? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    var isEnabled: Boolean = true
        private set

    var preampDb: Float = 0.0f
        private set

    var isLimiterEnabled: Boolean = true
        private set

    var currentTreblePercent: Int = 30
        private set

    private var currentBassBoostPercent: Int = 0

    // 10 Center Frequencies in Hz for wide-band EQ
    val centerFrequencies = listOf(31, 63, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

    // Current 10-band levels in dB [-15..15]
    val currentBands = IntArray(10) { 0 }

    companion object {
        val PRESETS = listOf(
            // Студия и Аудиофилы (профили Poweramp)
            EqPreset(
                "1MORE Quad Driver", "Студия",
                listOf(4, 6, 2, -1, 1, 3, 2, 4, 6, 5),
                35, 25, PresetReverb.PRESET_PLATE, 40, 0.0f,
                listOf(
                    EqParametricBand(1, 105, 9.8f, 0.70f, "#EF4444"),
                    EqParametricBand(2, 275, 2.5f, 1.38f, "#DC2626"),
                    EqParametricBand(3, 807, 1.8f, 0.87f, "#84CC16"),
                    EqParametricBand(4, 4460, -5.3f, 2.21f, "#06B6D4"),
                    EqParametricBand(5, 10200, 2.5f, 0.75f, "#A855F7")
                )
            ),
            EqPreset(
                "Harman Target 2024", "Студия",
                listOf(6, 5, 3, 0, 0, 1, 2, 4, 3, 2),
                40, 20, PresetReverb.PRESET_SMALLROOM, 30, -1.0f,
                listOf(
                    EqParametricBand(1, 80, 5.5f, 0.85f, "#EF4444"),
                    EqParametricBand(2, 300, -1.0f, 1.20f, "#EAB308"),
                    EqParametricBand(3, 1500, 1.5f, 1.10f, "#10B981"),
                    EqParametricBand(4, 3200, 3.5f, 1.60f, "#6366F1"),
                    EqParametricBand(5, 8500, 2.0f, 0.90f, "#A855F7")
                )
            ),
            EqPreset("Studio Reference (Студийный плоский)", "Студия", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 0, 0, PresetReverb.PRESET_NONE, 0, 0.0f),
            EqPreset("Open-Back Audiophile (Sennheiser HD)", "Студия", listOf(4, 3, 1, 0, 0, 0, 1, 2, 3, 2), 25, 30, PresetReverb.PRESET_MEDIUMROOM, 25, -0.5f),
            EqPreset("Warm Vinyl LP (Виниловая теплота)", "Студия", listOf(5, 4, 3, 2, 1, 0, -1, -2, -3, -4), 40, 15, PresetReverb.PRESET_SMALLROOM, 10, 0.5f),
            EqPreset("Clear Vocal / De-Esser", "Студия", listOf(-3, -2, 0, 2, 4, 5, 4, -2, -1, 1), 10, 15, PresetReverb.PRESET_SMALLROOM, 20, 0.0f),

            // Жанры
            EqPreset("Normal (Плоский)", "Музыка", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 0, 0, PresetReverb.PRESET_NONE, 0, 0.0f),
            EqPreset("Rock (Рок)", "Жанры", listOf(5, 4, 3, -1, -2, 1, 3, 5, 6, 6), 35, 20, PresetReverb.PRESET_PLATE, 45, -1.0f),
            EqPreset("Pop (Поп)", "Жанры", listOf(-1, 2, 4, 5, 4, 2, -1, 2, 4, 3), 25, 15, PresetReverb.PRESET_SMALLROOM, 30, 0.0f),
            EqPreset("Jazz (Джаз)", "Жанры", listOf(4, 3, 1, 2, -1, -1, 0, 2, 4, 4), 20, 30, PresetReverb.PRESET_MEDIUMROOM, 35, 0.0f),
            EqPreset("Classical (Классика)", "Жанры", listOf(5, 4, 3, 2, -1, -1, 0, 2, 4, 5), 10, 40, PresetReverb.PRESET_LARGEHALL, 35, 0.0f),
            EqPreset("Hip-Hop (Хип-хоп / Рэп)", "Жанры", listOf(8, 7, 5, 1, -1, -1, 2, 2, 4, 4), 60, 25, PresetReverb.PRESET_SMALLROOM, 30, -2.0f),
            EqPreset("Electronic / EDM (Клубная)", "Жанры", listOf(7, 6, 4, 1, 0, 2, 3, 5, 6, 6), 50, 35, PresetReverb.PRESET_PLATE, 50, -1.5f),
            EqPreset("Heavy Metal (Тяжёлый метал)", "Жанры", listOf(6, 5, 1, -2, -3, 2, 5, 7, 8, 8), 40, 25, PresetReverb.PRESET_PLATE, 60, -2.0f),
            EqPreset("Acoustic (Акустика)", "Жанры", listOf(3, 3, 2, 1, 2, 3, 3, 4, 4, 3), 15, 20, PresetReverb.PRESET_MEDIUMROOM, 35, 0.0f),
            EqPreset("R&B / Soul (Ритм-н-блюз)", "Жанры", listOf(6, 6, 4, 1, -1, 1, 3, 4, 5, 5), 45, 25, PresetReverb.PRESET_SMALLROOM, 35, -0.5f),
            EqPreset("Blues (Блюз)", "Жанры", listOf(3, 4, 2, 0, -1, 1, 2, 3, 4, 4), 25, 20, PresetReverb.PRESET_PLATE, 30, 0.0f),
            EqPreset("Reggae (Регги)", "Жанры", listOf(7, 7, 4, 0, -2, 1, 3, 4, 4, 3), 55, 15, PresetReverb.PRESET_PLATE, 25, -1.0f),
            EqPreset("Latin (Латино)", "Жанры", listOf(4, 3, 1, 0, -1, 0, 2, 4, 5, 5), 30, 20, PresetReverb.PRESET_SMALLROOM, 40, 0.0f),
            EqPreset("Country (Кантри)", "Жанры", listOf(3, 2, 1, 0, 2, 2, 1, 3, 3, 2), 20, 15, PresetReverb.PRESET_SMALLROOM, 25, 0.0f),
            EqPreset("Dance / House (Танцевальный)", "Жанры", listOf(6, 6, 4, 1, 0, 2, 4, 5, 6, 5), 50, 30, PresetReverb.PRESET_PLATE, 45, -1.0f),
            EqPreset("Techno / Trance (Транс)", "Жанры", listOf(7, 6, 3, 0, -1, 2, 4, 6, 7, 8), 55, 40, PresetReverb.PRESET_PLATE, 55, -1.5f),
            EqPreset("Drum & Bass (DnB 174 BPM)", "Жанры", listOf(10, 8, 3, 4, 1, 2, 4, 5, 7, 6), 75, 30, PresetReverb.PRESET_PLATE, 50, -2.5f),
            EqPreset("Peak-Time Techno (Берлинский рейв)", "Жанры", listOf(9, 7, 2, -1, 0, 3, 5, 6, 8, 7), 65, 45, PresetReverb.PRESET_STAGE, 60, -2.0f),
            EqPreset("Phonk & 808 Drift (Фонк 808)", "Жанры", listOf(11, 9, 4, 0, -2, 1, 4, 6, 7, 8), 80, 35, PresetReverb.PRESET_STAGE, 55, -2.5f),

            // Бас и драйв
            EqPreset("Bass Boost Ultra (Ультра Бас)", "Бас и драйв", listOf(12, 10, 8, 4, 1, 0, 0, 0, 0, 0), 85, 10, PresetReverb.PRESET_NONE, 20, -3.0f),
            EqPreset("Deep Sub-Bass (Глубокий саб)", "Бас и драйв", listOf(14, 11, 7, 2, 0, -1, -1, 0, 0, 0), 95, 15, PresetReverb.PRESET_NONE, 15, -3.5f),
            EqPreset("Punchy Kick (Панч бочки)", "Бас и драйв", listOf(6, 9, 7, 3, 0, 1, 2, 3, 2, 1), 70, 15, PresetReverb.PRESET_NONE, 25, -1.5f),
            EqPreset("Car Audio Punch (Для авто)", "Бас и драйв", listOf(9, 8, 5, 2, -1, 1, 3, 5, 6, 7), 75, 30, PresetReverb.PRESET_PLATE, 45, -2.0f),

            // Акустика и атмосфера
            EqPreset("Treble Boost (Кристальный верх)", "Акустика", listOf(0, 0, 0, 0, 1, 2, 4, 7, 9, 12), 10, 20, PresetReverb.PRESET_SMALLROOM, 90, -1.0f),
            EqPreset("Vocal Booster (Чистый вокал)", "Акустика", listOf(-3, -2, -1, 3, 6, 6, 5, 2, 0, -1), 15, 20, PresetReverb.PRESET_SMALLROOM, 30, 0.0f),
            EqPreset("Podcast / Speech (Речь и подкаст)", "Акустика", listOf(-5, -4, -1, 4, 7, 7, 5, 2, 0, -2), 0, 0, PresetReverb.PRESET_NONE, 10, 1.0f),
            EqPreset("Live Concert (Концертная арена)", "Акустика", listOf(4, 3, 1, 0, 2, 3, 4, 5, 5, 6), 35, 60, PresetReverb.PRESET_LARGEHALL, 40, -0.5f),
            EqPreset("Cathedral / Space (Собор и космос)", "Акустика", listOf(3, 2, 1, 0, 1, 2, 3, 4, 6, 7), 20, 75, PresetReverb.PRESET_LARGEROOM, 50, -1.0f),
            EqPreset("Warm Analog (Тёплый ламповый звук)", "Акустика", listOf(5, 4, 4, 3, 2, 1, 0, -1, -2, -3), 40, 15, PresetReverb.PRESET_MEDIUMROOM, 15, 0.5f),
            EqPreset("Lounge & Chillout (Лаунж отдых)", "Акустика", listOf(4, 3, 2, 2, 1, 1, 2, 3, 4, 4), 30, 35, PresetReverb.PRESET_MEDIUMROOM, 30, 0.0f),
            EqPreset("Movie / Cinematic (Кино и фильмы)", "Акустика", listOf(8, 6, 3, 1, 0, 2, 4, 6, 7, 8), 65, 50, PresetReverb.PRESET_LARGEHALL, 50, -1.5f),
            EqPreset("Gaming Dynamic (Игры и экшн)", "Акустика", listOf(8, 7, 3, -1, -2, 2, 5, 7, 8, 9), 70, 55, PresetReverb.PRESET_STAGE, 65, -2.0f),

            // Устройства
            EqPreset("In-Ear Earphones (Вакуумные затычки)", "Устройства", listOf(3, 2, -1, -1, 1, 2, 4, 5, 5, 6), 30, 35, PresetReverb.PRESET_SMALLROOM, 35, 0.0f),
            EqPreset("Over-Ear Studio (Полноразмерные)", "Устройства", listOf(1, 2, 1, 0, 0, 1, 2, 3, 3, 4), 25, 25, PresetReverb.PRESET_SMALLROOM, 25, 0.0f),
            EqPreset("Small Bluetooth Speaker (Портативная колонка)", "Устройства", listOf(6, 5, 3, 1, 2, 3, 4, 5, 4, 3), 40, 20, PresetReverb.PRESET_NONE, 30, 1.0f),
            EqPreset("TWS Wireless Pods (TWS наушники)", "Устройства", listOf(4, 3, 0, 0, 2, 3, 4, 5, 5, 4), 35, 30, PresetReverb.PRESET_SMALLROOM, 40, 0.0f)
        )
    }

    init {
        initEffects()
    }

    fun attachAudioSession(newSessionId: Int) {
        if (newSessionId > 0 && newSessionId != audioSessionId) {
            audioSessionId = newSessionId
            release()
            initEffects()
        }
    }

    private fun initEffects() {
        try {
            if (audioSessionId > 0) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = isEnabled
                }
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = isEnabled
                }
                virtualizer = Virtualizer(0, audioSessionId).apply {
                    enabled = isEnabled
                }
                try {
                    presetReverb = PresetReverb(0, audioSessionId).apply {
                        preset = PresetReverb.PRESET_NONE
                        enabled = isEnabled
                    }
                } catch (e: Exception) {
                    // Reverb fallback
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    try {
                        loudnessEnhancer = LoudnessEnhancer(audioSessionId).apply {
                            enabled = isEnabled
                        }
                    } catch (e: Exception) {
                        // Loudness fallback
                    }
                }
                applyBandsToHardware()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            presetReverb?.enabled = enabled
            loudnessEnhancer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandLevel(bandIndex: Int, levelDb: Int) {
        if (bandIndex in 0..9) {
            currentBands[bandIndex] = levelDb.coerceIn(-15, 15)
            applyBandsToHardware()
        }
    }

    fun applyPreset(presetName: String): EqPreset? {
        val preset = PRESETS.firstOrNull { it.name == presetName } ?: return null
        preset.bands.forEachIndexed { index, level ->
            if (index in 0..9) {
                currentBands[index] = level
            }
        }
        currentBassBoostPercent = preset.bassBoost
        applyBandsToHardware()
        setBassBoostStrength(preset.bassBoost)
        setVirtualizerStrength(preset.virtualizer)
        setReverb(preset.reverb)
        return preset
    }

    fun setBassBoostStrength(percent: Int) {
        currentBassBoostPercent = percent.coerceIn(0, 100)
        // Scaled strength: avoids exceeding digital ceiling
        val strength = (currentBassBoostPercent * 10).toShort()
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // Update hardware bands with dynamic anti-pumping headroom
        applyBandsToHardware()
    }

    fun setVirtualizerStrength(percent: Int) {
        val strength = (percent.coerceIn(0, 100) * 10).toShort()
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(strength)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setLoudnessGain(gainDb: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val mB = (gainDb.coerceIn(-15, 15) * 100)
                loudnessEnhancer?.setTargetGain(mB)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setReverb(preset: Short) {
        try {
            presetReverb?.preset = preset
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun applyBandsToHardware() {
        val eq = equalizer ?: return
        try {
            val hwBands = eq.numberOfBands.toInt()
            val bandRange = eq.bandLevelRange ?: return
            if (bandRange.size < 2) return
            val minLevel = bandRange[0]
            val maxLevel = bandRange[1]

            // Dynamic Anti-Pumping Headroom:
            // When user boosts bass or EQ frequencies high, Android's DRC Limiter ducks overall volume.
            // We calculate peak boost and apply smooth pre-cut headroom to prevent limiter ducking.
            val maxBandGain = currentBands.maxOrNull() ?: 0
            val bassImpact = (currentBassBoostPercent * 0.08f).toInt()
            val peakBoost = kotlin.math.max(maxBandGain, bassImpact)
            val headroomOffsetDb = if (peakBoost > 0) -(peakBoost * 0.45f).toInt() else 0

            for (i in 0 until hwBands) {
                val centerFreqHz = eq.getCenterFreq(i.toShort()) / 1000

                // Logarithmic octave-weighted band interpolation across 10 virtual frequencies
                var totalWeight = 0f
                var weightedGainSum = 0f
                for (vIdx in centerFrequencies.indices) {
                    val vFreq = centerFrequencies[vIdx]
                    val octDistance = kotlin.math.abs(kotlin.math.ln(vFreq.toDouble() / centerFreqHz.toDouble()) / kotlin.math.ln(2.0))
                    if (octDistance < 1.4) {
                        val weight = (1.4 - octDistance).toFloat()
                        weightedGainSum += currentBands[vIdx] * weight
                        totalWeight += weight
                    }
                }

                val interpolatedGain = if (totalWeight > 0f) weightedGainSum / totalWeight else 0f
                val finalLevelDb = (interpolatedGain + headroomOffsetDb).coerceIn(-15f, 15f)
                val hwLevel = ((finalLevelDb / 15f) * maxLevel).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt())

                try {
                    eq.setBandLevel(i.toShort(), hwLevel.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Soft-clipping RMS makeup gain via LoudnessEnhancer
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                try {
                    val makeupGainMb = if (peakBoost > 0) (peakBoost * 22).coerceIn(0, 350) else 0
                    loudnessEnhancer?.setTargetGain(makeupGainMb)
                } catch (e: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reset() {
        for (i in currentBands.indices) {
            currentBands[i] = 0
        }
        applyBandsToHardware()
        setBassBoostStrength(0)
        setVirtualizerStrength(0)
        setLoudnessGain(0)
        setReverb(PresetReverb.PRESET_NONE)
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            presetReverb?.release()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer?.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            equalizer = null
            bassBoost = null
            virtualizer = null
            presetReverb = null
            loudnessEnhancer = null
        }
    }
}
