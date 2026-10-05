package com.example.data.audio

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

object AudioSynthesizer {

    /**
     * Genera un archivo de audio WAV estéreo a 44.1 kHz, 16 bits PCM válido y
     * 100% reproducible por el MediaPlayer nativo de Android.
     * Incluye una suave armonía musical Hi-Fi para reproducción offline o si no hay stream.
     */
    fun generateHiFiWav(durationSeconds: Int = 20): ByteArray {
        val sampleRate = 44100
        val numChannels = 2
        val bitsPerSample = 16
        val numSamples = sampleRate * durationSeconds
        val bytesPerSample = (bitsPerSample / 8) * numChannels
        val dataSize = numSamples * bytesPerSample
        val totalSize = 36 + dataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            // RIFF chunk descriptor
            put('R'.code.toByte())
            put('I'.code.toByte())
            put('F'.code.toByte())
            put('F'.code.toByte())
            putInt(totalSize)
            put('W'.code.toByte())
            put('A'.code.toByte())
            put('V'.code.toByte())
            put('E'.code.toByte())

            // "fmt " sub-chunk
            put('f'.code.toByte())
            put('m'.code.toByte())
            put('t'.code.toByte())
            put(' '.code.toByte())
            putInt(16) // Subchunk1Size for PCM
            putShort(1) // AudioFormat (1 = PCM)
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * bytesPerSample) // ByteRate
            putShort(bytesPerSample.toShort()) // BlockAlign
            putShort(bitsPerSample.toShort())

            // "data" sub-chunk
            put('d'.code.toByte())
            put('a'.code.toByte())
            put('t'.code.toByte())
            put('a'.code.toByte())
            putInt(dataSize)
        }.array()

        val output = ByteArrayOutputStream(44 + dataSize)
        output.write(header)

        // Generar acorde musical cálido (A4 440Hz + C#5 554.37Hz + E5 659.25Hz) con modulación suave
        val baseFreqs = doubleArrayOf(220.0, 440.0, 554.37, 659.25)
        val buffer = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            // Envolvente de entrada y salida (fade-in / fade-out)
            val envelope = when {
                t < 1.0 -> t
                t > durationSeconds - 2.0 -> (durationSeconds - t) / 2.0
                else -> 1.0
            }.coerceIn(0.0, 1.0)

            var sampleL = 0.0
            var sampleR = 0.0

            for (f in baseFreqs) {
                val wave = sin(2.0 * PI * f * t)
                sampleL += wave * 0.22
                sampleR += sin(2.0 * PI * (f * 1.002) * t) * 0.22
            }

            // Aplicar envolvente y escala a 16-bit signed integer (-32768..32767)
            val shortL = (sampleL * envelope * 24000.0).toInt().coerceIn(-32767, 32767).toShort()
            val shortR = (sampleR * envelope * 24000.0).toInt().coerceIn(-32767, 32767).toShort()

            buffer.putShort(shortL)
            buffer.putShort(shortR)
        }

        output.write(buffer.array())
        return output.toByteArray()
    }
}
