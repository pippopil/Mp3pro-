package com.tohsoft.music.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class AudiophileDspBiquadTest {

    @Test
    fun allTenBandsProduceFiniteStableBiquadCoefficients() {
        val sampleRates = doubleArrayOf(44100.0, 48000.0, 96000.0)
        val testGains = doubleArrayOf(-12.0, -6.0, 0.0, 6.0, 12.0)

        for (sr in sampleRates) {
            for (freq in AudiophileDSPProcessor.CENTER_FREQUENCIES_HZ) {
                for (gain in testGains) {
                    val c = AudiophileDSPProcessor.computePeakingBiquad(sr, freq, gain, 1.41)
                    assertTrue("b0 finite", c.b0.isFinite())
                    assertTrue("b1 finite", c.b1.isFinite())
                    assertTrue("b2 finite", c.b2.isFinite())
                    assertTrue("a1 finite", c.a1.isFinite())
                    assertTrue("a2 finite", c.a2.isFinite())
                    // Условие устойчивости биквада (полюса внутри единичной окружности): |a2| < 1 и |a1| < 1 + a2
                    assertTrue("stable a2 for freq=$freq gain=$gain", abs(c.a2) < 1.0)
                    assertTrue("stable a1 for freq=$freq gain=$gain", abs(c.a1) < 1.0 + c.a2 + 1e-6)
                }
            }
        }
    }

    @Test
    fun zeroGainReturnsPassThroughIdentity() {
        val c = AudiophileDSPProcessor.computePeakingBiquad(48000.0, 1000.0, 0.0, 1.41)
        assertEquals(1.0, c.b0, 1e-9)
        assertEquals(0.0, c.b1, 1e-9)
        assertEquals(0.0, c.b2, 1e-9)
        assertEquals(0.0, c.a1, 1e-9)
        assertEquals(0.0, c.a2, 1e-9)
    }

    @Test
    fun bassShelf100HzIsStable() {
        val c = AudiophileDSPProcessor.computeLowShelfBiquad(48000.0, 100.0, 6.0, 0.85)
        assertTrue(c.b0.isFinite())
        assertTrue(abs(c.a2) < 1.0)
        assertTrue(abs(c.a1) < 1.0 + c.a2 + 1e-6)
    }

    @Test
    fun softLimiterPreventsClippingAboveThreshold() {
        assertEquals(0.5f, AudiophileDSPProcessor.softLimit(0.5f), 1e-6f)
        val highPos = AudiophileDSPProcessor.softLimit(3.5f)
        val highNeg = AudiophileDSPProcessor.softLimit(-3.5f)
        assertTrue("positive peak smoothly limited <= 1.0", highPos in 0.95f..1.0f)
        assertTrue("negative peak smoothly limited >= -1.0", highNeg in -1.0f..-0.95f)
    }
}
