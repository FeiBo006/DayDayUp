package com.doapp.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doapp.data.Appearance
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials

/**
 * Wallpaper picker. Personalization is the point — the list is the same, but it should be able
 * to sit on something that belongs to you.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperSheet(
    appearance: Appearance,
    onSelectPreset: (String) -> Unit,
    onPickPhoto: (android.net.Uri) -> Unit,
    onBlurChange: (Float) -> Unit,
    onDimChange: (Float) -> Unit,
    onDismiss: () -> Unit,
) {
    val m = materials
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let(onPickPhoto) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (m.isDoodle) m.card
        else if (m.isDark) Color(0xFF1C1C1E) else Color(0xFFF7F7F9),
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("壁纸", style = MaterialTheme.typography.headlineSmall, color = m.label)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PhotoTile(
                    selected = appearance.showsPhoto,
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                WallpaperPresets.take(2).forEach { preset ->
                    PresetTile(
                        preset = preset,
                        selected = !appearance.showsPhoto && appearance.presetId == preset.id,
                        onClick = { onSelectPreset(preset.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WallpaperPresets.drop(2).take(3).forEach { preset ->
                    PresetTile(
                        preset = preset,
                        selected = !appearance.showsPhoto && appearance.presetId == preset.id,
                        onClick = { onSelectPreset(preset.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                WallpaperPresets.drop(5).forEach { preset ->
                    PresetTile(
                        preset = preset,
                        selected = !appearance.showsPhoto && appearance.presetId == preset.id,
                        onClick = { onSelectPreset(preset.id) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Keep the last row's tiles the same width as the rows above.
                repeat(3 - WallpaperPresets.drop(5).size) {
                    Spacer(Modifier.weight(1f))
                }
            }

            LabelledSlider("模糊", appearance.blur, onBlurChange)
            LabelledSlider("变暗", appearance.dim, onDimChange)

            Text(
                "模糊和变暗决定文字有多好读 —— 照片越花，越该往右调。",
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
            )
        }
    }
}

@Composable
private fun PresetTile(
    preset: WallpaperPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TileFrame(selected = selected, onClick = onClick, modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .then(
                    if (materials.isNeoBrutalist) Modifier.background(preset.colors.first())
                    else Modifier.background(Brush.linearGradient(preset.colors))
                )
        )
        Text(
            text = preset.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (preset.prefersDarkText) Color.Black.copy(alpha = 0.55f) else Color.White,
            modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
        )
    }
}

@Composable
private fun PhotoTile(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val m = materials
    TileFrame(selected = selected, onClick = onClick, modifier = modifier) {
        Box(
            Modifier
                .matchParentSize()
                .background(if (m.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.PhotoLibrary,
                    contentDescription = null,
                    tint = m.accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "相册",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = m.accent,
                )
            }
        }
    }
}

@Composable
private fun TileFrame(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    val ring by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = Motion.Snappy,
        label = "tileSelected",
    )
    val shape = appShape(14.dp)

    Box(
        modifier
            .aspectRatio(0.78f)
            .clip(shape)
            .border(
                width = (2.5f * ring).dp,
                color = m.accent.copy(alpha = ring),
                shape = shape,
            )
            .pressableNoRipple(interactionSource, onClick)
    ) {
        content()
        if (ring > 0f) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .graphicsLayer { alpha = ring; scaleX = ring; scaleY = ring }
                    .clip(CircleShape)
                    .background(m.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun LabelledSlider(label: String, value: Float, onChange: (Float) -> Unit) {
    val m = materials
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = m.label)
            Spacer(Modifier.weight(1f))
            Text(
                "${(value * 100).toInt()}",
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
            )
        }
        Slider(
            value = value,
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = m.accent,
                inactiveTrackColor = if (m.isDark) Color.White.copy(alpha = 0.16f)
                else Color.Black.copy(alpha = 0.10f),
            ),
            modifier = Modifier.height(28.dp),
        )
    }
}
