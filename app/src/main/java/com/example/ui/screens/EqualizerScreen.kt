package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CosmicMeshBackground
import com.example.ui.theme.Accent
import com.example.ui.theme.AccentDim
import com.example.ui.theme.BgCard
import com.example.ui.theme.BgSecondary
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.HoverBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SearchAndDownloadViewModel

@Composable
fun EqualizerScreen(
    viewModel: SearchAndDownloadViewModel,
    modifier: Modifier = Modifier
) {
    val eqState by viewModel.equalizerState.collectAsState()

    val presets = listOf("Master Hi-Fi", "Bass Boost", "Vocal Clarity", "Rock Dinámico", "Plano / Flat")

    Box(modifier = modifier.fillMaxSize()) {
        CosmicMeshBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Cabecera con botón reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentDim)
                            .border(1.dp, HoverBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Ecualizador Hi-Fi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Motor Nativo Android AudioFx & DSP",
                            style = MaterialTheme.typography.bodySmall,
                            color = Accent
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AccentDim,
                    border = BorderStroke(1.dp, HoverBorder),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.resetEqualizer() }
                        .testTag("reset_equalizer_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "RESET",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Accent
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Presets chips
            Text(
                text = "PRESETS AUDIÓFILOS EN VIVO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.take(3).forEach { preset ->
                    val isSelected = eqState.activePreset == preset
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Accent else BgCard,
                        border = BorderStroke(1.dp, if (isSelected) Accent else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.applyEqualizerPreset(preset) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.drop(3).forEach { preset ->
                    val isSelected = eqState.activePreset == preset
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Accent else BgCard,
                        border = BorderStroke(1.dp, if (isSelected) Accent else GlassBorder),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.applyEqualizerPreset(preset) }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Sliders de Neón en tiempo real
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calibración de Frecuencias (Tiempo Real)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Bajos (Bass Boost)
                    BandSlider(
                        label = "Bajos (Bass Boost Sub-woofer)",
                        valueText = "${eqState.bassBoost.toInt()}%",
                        currentValue = eqState.bassBoost,
                        range = 0f..100f,
                        activeColor = Accent,
                        onValueChange = { viewModel.setEqualizerBass(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Medios / Voces
                    BandSlider(
                        label = "Medios / Voces (Mid-range Clarity)",
                        valueText = "${if (eqState.vocalsLevel > 0) "+" else ""}${eqState.vocalsLevel.toInt()} dB",
                        currentValue = eqState.vocalsLevel,
                        range = -10f..10f,
                        activeColor = CyanAccent,
                        onValueChange = { viewModel.setEqualizerVocals(it) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Agudos / Brillo
                    BandSlider(
                        label = "Agudos (Treble & Air Hi-Res)",
                        valueText = "${if (eqState.trebleLevel > 0) "+" else ""}${eqState.trebleLevel.toInt()} dB",
                        currentValue = eqState.trebleLevel,
                        range = -10f..10f,
                        activeColor = Accent,
                        onValueChange = { viewModel.setEqualizerTreble(it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Volumen Master
            Card(
                colors = CardDefaults.cardColors(containerColor = BgSecondary),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Volumen Master Hi-Fi",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "${eqState.masterVolume.toInt()}%",
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Slider(
                        value = eqState.masterVolume,
                        onValueChange = { viewModel.setMasterVolume(it) },
                        valueRange = 0f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = Accent,
                            activeTrackColor = Accent,
                            inactiveTrackColor = BgCard
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun BandSlider(
    label: String,
    valueText: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    activeColor: Color,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = activeColor,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = activeColor,
                activeTrackColor = activeColor,
                inactiveTrackColor = BgCard
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
