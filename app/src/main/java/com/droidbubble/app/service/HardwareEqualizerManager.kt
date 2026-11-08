package com.droidbubble.app.service

import android.media.audiofx.Equalizer
import android.util.Log
import com.droidbubble.app.model.IslandSettings
import kotlin.math.roundToInt

object HardwareEqualizerManager {
    private const val TAG = "HardwareEqualizerMgr"
    private var equalizer: Equalizer? = null

    fun applyFromSettings(settings: IslandSettings) {
        if (!settings.eqShowEqualizer) {
            release()
            return
        }
        runCatching {
            if (equalizer == null) {
                equalizer = Equalizer(0, 0).apply { enabled = true }
            }
            val eq = equalizer ?: return
            if (!eq.enabled) eq.enabled = true
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange // millibels, e.g. [-1500, 1500]
            val minMb = range.getOrElse(0) { -1500 }.toInt()
            val maxMb = range.getOrElse(1) { 1500 }.toInt()

            val bandsDb = listOf(
                settings.eqBand60Hz,
                settings.eqBand230Hz,
                settings.eqBand910Hz,
                settings.eqBand3kHz,
                settings.eqBand14kHz
            )
            for (i in 0 until minOf(numBands, bandsDb.size)) {
                val targetMb = (bandsDb[i] * 100f).roundToInt().coerceIn(minMb, maxMb).toShort()
                eq.setBandLevel(i.toShort(), targetMb)
            }
        }.onFailure { e ->
            Log.w(TAG, "Hardware Equalizer update skipped: ${e.message}")
        }
    }

    fun getPresetBands(preset: String): List<Float> {
        return when (preset) {
            "BASS_BOOST" -> listOf(6.0f, 4.5f, 0.0f, 1.0f, 2.0f)
            "VOCAL" -> listOf(-2.0f, 1.0f, 4.5f, 3.5f, 1.0f)
            "POP" -> listOf(-1.0f, 2.5f, 4.0f, 2.0f, -1.0f)
            "ROCK" -> listOf(4.5f, 2.5f, -1.0f, 3.0f, 4.5f)
            "FLAT" -> listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
            else -> listOf(0.0f, 0.0f, 0.0f, 0.0f, 0.0f)
        }
    }

    fun release() {
        runCatching {
            equalizer?.release()
        }
        equalizer = null
    }
}
