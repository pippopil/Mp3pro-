package com.tohsoft.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Профессиональная DSP-цепочка для Jetpack Media3 (ExoPlayer).
 * Строгий порядок обработки PCM (16-bit или 32-bit Float):
 *   1. Preamp (-15..+15 dB)
 *   2. 10-полосный Peaking Biquad EQ (независимая история z1/z2 для каждого канала!)
 *   3. Bass Shelf (100 Hz)
 *   4. Опциональный Crossfeed (BS2B, ~0.3 мс задержка между L/R каналами)
 *   5. Soft Limiter (tanh) — СТРОГО последним для полного исключения жёсткого клиппинга.
 */
class AudiophileDSPProcessor : BaseAudioProcessor() {

    companion object {
        val CENTER_FREQUENCIES_HZ = doubleArrayOf(
            31.0, 63.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0
        )

        data class BiquadCoeffs(
            val b0: Double,
            val b1: Double,
            val b2: Double,
            val a1: Double,
            val a2: Double
        )

        /**
         * Расчёт коэффициентов Audio EQ Cookbook (Robert Bristow-Johnson) для peaking-фильтра.
         * При gainDb == 0 возвращает единичный pass-through фильтр.
         */
        fun computePeakingBiquad(
            sampleRate: Double,
            centerFreqHz: Double,
            gainDb: Double,
            qFactor: Double = 1.41
        ): BiquadCoeffs {
            if (kotlin.math.abs(gainDb) < 0.01 || sampleRate <= 0.0 || centerFreqHz >= sampleRate * 0.45) {
                return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0)
            }
            val a = 10.0.pow(gainDb / 40.0)
            val omega = 2.0 * PI * centerFreqHz / sampleRate
            val sn = sin(omega)
            val cs = cos(omega)
            val alpha = sn / (2.0 * qFactor.coerceIn(0.3, 10.0))

            val b0 = 1.0 + alpha * a
            val b1 = -2.0 * cs
            val b2 = 1.0 - alpha * a
            val a0 = 1.0 + alpha / a
            val a1 = -2.0 * cs
            val a2 = 1.0 - alpha / a

            return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }

        /**
         * Расчёт коэффициентов Low-Shelf (полка 100 Гц для BassBoost).
         */
        fun computeLowShelfBiquad(
            sampleRate: Double,
            cutoffHz: Double,
            gainDb: Double,
            slope: Double = 0.8
        ): BiquadCoeffs {
            if (kotlin.math.abs(gainDb) < 0.01 || sampleRate <= 0.0 || cutoffHz >= sampleRate * 0.45) {
                return BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0)
            }
            val a = 10.0.pow(gainDb / 40.0)
            val omega = 2.0 * PI * cutoffHz / sampleRate
            val sn = sin(omega)
            val cs = cos(omega)
            val alpha = (sn / 2.0) * sqrt((a + 1.0 / a) * (1.0 / slope - 1.0) + 2.0)
            val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

            val b0 = a * ((a + 1.0) - (a - 1.0) * cs + twoSqrtAAlpha)
            val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cs)
            val b2 = a * ((a + 1.0) - (a - 1.0) * cs - twoSqrtAAlpha)
            val a0 = (a + 1.0) + (a - 1.0) * cs + twoSqrtAAlpha
            val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cs)
            val a2 = (a + 1.0) + (a - 1.0) * cs - twoSqrtAAlpha

            return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
        }

        /**
         * Мягкий лимитер: линейный до 0.95, плавное насыщение tanh выше 0.95 без hard clipping.
         */
        fun softLimit(sample: Float): Float {
            val threshold = 0.95f
            val absVal = kotlin.math.abs(sample)
            if (absVal <= threshold) return sample
            val excess = (absVal - threshold) / (1.0f - threshold)
            val limited = threshold + (1.0f - threshold) * tanh(excess.toDouble()).toFloat()
            return if (sample >= 0f) limited else -limited
        }
    }

    @Volatile private var dspEnabled: Boolean = true
    @Volatile private var limiterEnabled: Boolean = true
    @Volatile private var crossfeedEnabled: Boolean = false
    @Volatile private var preampLinear: Float = 1.0f
    @Volatile private var bassShelfGainDb: Double = 0.0
    private val bandGainsDb = DoubleArray(10) { 0.0 }

    @Volatile private var sampleRateHz: Double = 44100.0
    @Volatile private var channels: Int = 2
    @Volatile private var coeffsDirty: Boolean = true

    // Коэффициенты 10 полос + 1 bass shelf
    private val eqCoeffs = Array(10) { BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0) }
    private var bassCoeffs = BiquadCoeffs(1.0, 0.0, 0.0, 0.0, 0.0)

    // Независимое состояние фильтров Direct Form II Transposed для каждого канала (до 8 каналов)
    private val z1 = Array(11) { DoubleArray(8) }
    private val z2 = Array(11) { DoubleArray(8) }

    // Кольцевой буфер задержки ~0.3 мс для BS2B Crossfeed
    private var crossfeedDelaySamples = 14
    private var delayBufL = FloatArray(64)
    private var delayBufR = FloatArray(64)
    private var delayPos = 0

    fun setEnabled(enabled: Boolean) {
        dspEnabled = enabled
    }

    fun setLimiterEnabled(enabled: Boolean) {
        limiterEnabled = enabled
    }

    fun setCrossfeedEnabled(enabled: Boolean) {
        crossfeedEnabled = enabled
    }

    fun setPreampDb(db: Float) {
        val clamped = db.coerceIn(-15f, 15f)
        preampLinear = 10.0.pow(clamped / 20.0).toFloat()
    }

    fun setBandGainDb(bandIndex: Int, gainDb: Float) {
        if (bandIndex in 0..9) {
            bandGainsDb[bandIndex] = gainDb.coerceIn(-15f, 15f).toDouble()
            coeffsDirty = true
        }
    }

    fun setAllBandsDb(levels: List<Int>) {
        for (i in 0 until minOf(10, levels.size)) {
            bandGainsDb[i] = levels[i].coerceIn(-15, 15).toDouble()
        }
        coeffsDirty = true
    }

    fun setBassShelfFromPercent(percent: Int) {
        val clamped = percent.coerceIn(0, 100)
        // Плавная полка до +6 dB на 100 Гц
        bassShelfGainDb = (clamped / 100.0) * 6.0
        coeffsDirty = true
    }

    private fun updateFilterCoefficientsIfNeeded() {
        if (!coeffsDirty) return
        val sr = sampleRateHz
        for (i in 0..9) {
            eqCoeffs[i] = computePeakingBiquad(sr, CENTER_FREQUENCIES_HZ[i], bandGainsDb[i], 1.41)
        }
        bassCoeffs = computeLowShelfBiquad(sr, 100.0, bassShelfGainDb, 0.85)
        coeffsDirty = false
    }

    private fun resetFilterStates() {
        for (b in z1.indices) {
            z1[b].fill(0.0)
            z2[b].fill(0.0)
        }
        delayBufL.fill(0f)
        delayBufR.fill(0f)
        delayPos = 0
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val enc = inputAudioFormat.encoding
        if (enc != C.ENCODING_PCM_16BIT && enc != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        sampleRateHz = inputAudioFormat.sampleRate.toDouble().coerceAtLeast(8000.0)
        channels = inputAudioFormat.channelCount.coerceIn(1, 8)
        crossfeedDelaySamples = ((sampleRateHz * 0.0003).toInt()).coerceIn(4, 48)
        if (delayBufL.size <= crossfeedDelaySamples) {
            delayBufL = FloatArray(crossfeedDelaySamples + 16)
            delayBufR = FloatArray(crossfeedDelaySamples + 16)
        }
        coeffsDirty = true
        resetFilterStates()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        if (!dspEnabled) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        updateFilterCoefficientsIfNeeded()

        val chCount = channels
        val enc = inputAudioFormat.encoding
        val gain = preampLinear
        val doLimiter = limiterEnabled
        val doCrossfeed = crossfeedEnabled && chCount == 2

        if (enc == C.ENCODING_PCM_FLOAT) {
            val frameBytes = chCount * 4
            val frames = remaining / frameBytes
            for (f in 0 until frames) {
                if (doCrossfeed) {
                    val lIn = processSingleSample(inputBuffer.float * gain, 0)
                    val rIn = processSingleSample(inputBuffer.float * gain, 1)
                    val readIdx = (delayPos - crossfeedDelaySamples + delayBufL.size) % delayBufL.size
                    val delayedR = delayBufR[readIdx]
                    val delayedL = delayBufL[readIdx]
                    delayBufL[delayPos] = lIn
                    delayBufR[delayPos] = rIn
                    delayPos = (delayPos + 1) % delayBufL.size
                    val lOut = lIn * 0.85f + delayedR * 0.15f
                    val rOut = rIn * 0.85f + delayedL * 0.15f
                    outputBuffer.putFloat(if (doLimiter) softLimit(lOut) else lOut.coerceIn(-1f, 1f))
                    outputBuffer.putFloat(if (doLimiter) softLimit(rOut) else rOut.coerceIn(-1f, 1f))
                } else {
                    for (ch in 0 until chCount) {
                        val processed = processSingleSample(inputBuffer.float * gain, ch)
                        val out = if (doLimiter) softLimit(processed) else processed.coerceIn(-1f, 1f)
                        outputBuffer.putFloat(out)
                    }
                }
            }
        } else {
            val frameBytes = chCount * 2
            val frames = remaining / frameBytes
            for (f in 0 until frames) {
                if (doCrossfeed) {
                    val lIn = processSingleSample((inputBuffer.short / 32768.0f) * gain, 0)
                    val rIn = processSingleSample((inputBuffer.short / 32768.0f) * gain, 1)
                    val readIdx = (delayPos - crossfeedDelaySamples + delayBufL.size) % delayBufL.size
                    val delayedR = delayBufR[readIdx]
                    val delayedL = delayBufL[readIdx]
                    delayBufL[delayPos] = lIn
                    delayBufR[delayPos] = rIn
                    delayPos = (delayPos + 1) % delayBufL.size
                    val lOut = lIn * 0.85f + delayedR * 0.15f
                    val rOut = rIn * 0.85f + delayedL * 0.15f
                    val lFinal = if (doLimiter) softLimit(lOut) else lOut.coerceIn(-1f, 1f)
                    val rFinal = if (doLimiter) softLimit(rOut) else rOut.coerceIn(-1f, 1f)
                    outputBuffer.putShort((lFinal * 32767.0f).toInt().coerceIn(-32768, 32767).toShort())
                    outputBuffer.putShort((rFinal * 32767.0f).toInt().coerceIn(-32768, 32767).toShort())
                } else {
                    for (ch in 0 until chCount) {
                        val sampleF = (inputBuffer.short / 32768.0f) * gain
                        val processed = processSingleSample(sampleF, ch)
                        val outF = if (doLimiter) softLimit(processed) else processed.coerceIn(-1f, 1f)
                        outputBuffer.putShort((outF * 32767.0f).toInt().coerceIn(-32768, 32767).toShort())
                    }
                }
            }
        }

        // Дописываем остаточные байты (если есть) и фиксируем буфер
        while (inputBuffer.hasRemaining()) {
            outputBuffer.put(inputBuffer.get())
        }
        outputBuffer.flip()
    }

    private fun processSingleSample(input: Float, ch: Int): Float {
        var x = input.toDouble()
        // 10 полос Graphic EQ (Direct Form II Transposed — независимое состояние для канала ch)
        for (band in 0..9) {
            val c = eqCoeffs[band]
            if (c.b0 == 1.0 && c.b1 == 0.0 && c.b2 == 0.0 && c.a1 == 0.0 && c.a2 == 0.0) continue
            val y = c.b0 * x + z1[band][ch]
            z1[band][ch] = c.b1 * x - c.a1 * y + z2[band][ch]
            z2[band][ch] = c.b2 * x - c.a2 * y
            x = y
        }
        // Bass Shelf (100 Hz)
        val bc = bassCoeffs
        if (!(bc.b0 == 1.0 && bc.b1 == 0.0 && bc.b2 == 0.0 && bc.a1 == 0.0 && bc.a2 == 0.0)) {
            val y = bc.b0 * x + z1[10][ch]
            z1[10][ch] = bc.b1 * x - bc.a1 * y + z2[10][ch]
            z2[10][ch] = bc.b2 * x - bc.a2 * y
            x = y
        }
        return x.toFloat()
    }

    override fun onFlush() {
        resetFilterStates()
    }

    override fun onReset() {
        resetFilterStates()
    }
}
