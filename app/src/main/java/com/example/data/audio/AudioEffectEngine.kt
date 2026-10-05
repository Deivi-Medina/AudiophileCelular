package com.example.data.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerState(
    val isEnabled: Boolean = true,
    val bassBoost: Float = 60f, // 0 to 100%
    val vocalsLevel: Float = 2f, // -10 to +10 dB
    val trebleLevel: Float = 3f, // -10 to +10 dB
    val masterVolume: Float = 85f, // 0 to 100%
    val activePreset: String = "Master Hi-Fi"
)

class AudioEffectEngine {
    private val TAG = "AudioEffectEngine"

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var currentSessionId: Int = 0

    private val _state = MutableStateFlow(EqualizerState())
    val state: StateFlow<EqualizerState> = _state.asStateFlow()

    fun attachSession(audioSessionId: Int) {
        if (audioSessionId == 0 || audioSessionId == currentSessionId) return
        currentSessionId = audioSessionId

        release()

        try {
            equalizer = Equalizer(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
            }
            bassBoost = BassBoost(0, audioSessionId).apply {
                enabled = _state.value.isEnabled
                if (strengthSupported) {
                    setStrength(((_state.value.bassBoost / 100f) * 1000).toInt().toShort())
                }
            }
            applyBands()
            Log.d(TAG, "Attached native audio effects to session: $audioSessionId")
        } catch (e: Exception) {
            Log.w(TAG, "AudioFx initialization warning (device simulated): ${e.message}")
        }
    }

    fun setBassBoost(level: Float) {
        _state.value = _state.value.copy(bassBoost = level.coerceIn(0f, 100f))
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(((level / 100f) * 1000).toInt().toShort())
                }
            }
        } catch (_: Exception) {}
    }

    fun setVocals(level: Float) {
        _state.value = _state.value.copy(vocalsLevel = level.coerceIn(-10f, 10f))
        applyBands()
    }

    fun setTreble(level: Float) {
        _state.value = _state.value.copy(trebleLevel = level.coerceIn(-10f, 10f))
        applyBands()
    }

    fun setMasterVolume(volume: Float) {
        _state.value = _state.value.copy(masterVolume = volume.coerceIn(0f, 100f))
    }

    fun applyPreset(presetName: String) {
        when (presetName) {
            "Master Hi-Fi" -> {
                _state.value = _state.value.copy(
                    bassBoost = 60f,
                    vocalsLevel = 2f,
                    trebleLevel = 4f,
                    activePreset = presetName
                )
            }
            "Bass Boost" -> {
                _state.value = _state.value.copy(
                    bassBoost = 95f,
                    vocalsLevel = 0f,
                    trebleLevel = 2f,
                    activePreset = presetName
                )
            }
            "Vocal Clarity" -> {
                _state.value = _state.value.copy(
                    bassBoost = 20f,
                    vocalsLevel = 6f,
                    trebleLevel = 3f,
                    activePreset = presetName
                )
            }
            "Rock Dinámico" -> {
                _state.value = _state.value.copy(
                    bassBoost = 70f,
                    vocalsLevel = -1f,
                    trebleLevel = 6f,
                    activePreset = presetName
                )
            }
            "Plano / Flat" -> {
                _state.value = _state.value.copy(
                    bassBoost = 0f,
                    vocalsLevel = 0f,
                    trebleLevel = 0f,
                    activePreset = presetName
                )
            }
        }
        setBassBoost(_state.value.bassBoost)
        applyBands()
    }

    private fun applyBands() {
        try {
            val eq = equalizer ?: return
            val numBands = eq.numberOfBands.toInt()
            if (numBands > 0) {
                val minLevel = eq.bandLevelRange[0]
                val maxLevel = eq.bandLevelRange[1]
                val range = maxLevel - minLevel

                for (band in 0 until numBands) {
                    val progress = band.toFloat() / (numBands - 1).coerceAtLeast(1)
                    val dbValue = when {
                        progress < 0.33f -> _state.value.bassBoost / 10f - 5f // bass
                        progress < 0.66f -> _state.value.vocalsLevel // mid
                        else -> _state.value.trebleLevel // treble
                    }
                    val targetLevel = (minLevel + ((dbValue + 10f) / 20f * range)).toInt()
                        .coerceIn(minLevel.toInt(), maxLevel.toInt())
                    eq.setBandLevel(band.toShort(), targetLevel.toShort())
                }
            }
        } catch (_: Exception) {}
    }

    fun reset() {
        applyPreset("Plano / Flat")
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
    }
}
