package com.doapp.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.doapp.data.Appearance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class WallpaperPreset(
    val id: String,
    val name: String,
    val colors: List<Color>,
    val prefersDarkText: Boolean = true,
    /** The app's accent takes its cue from the wallpaper, so chrome never fights the background. */
    val accent: Color = Color(0xFF3D7BD6),
    val accentDark: Color = Color(0xFF7FA8FF),
) {
    fun accent(isDark: Boolean): Color = if (isDark) accentDark else accent
}

val WallpaperPresets = listOf(
    WallpaperPreset("sierra", "晨蓝", listOf(Color(0xFFDCE7F7)),
        accent = Color(0xFF3D7BD6), accentDark = Color(0xFF7FA8FF)),
    WallpaperPreset("blush", "霞粉", listOf(Color(0xFFF6E2E8)),
        accent = Color(0xFFE5487C), accentDark = Color(0xFFFF7FA8)),
    WallpaperPreset("mint", "薄荷", listOf(Color(0xFFDDEEE7)),
        accent = Color(0xFF1FA97E), accentDark = Color(0xFF5EDCB0)),
    WallpaperPreset("sand", "暖沙", listOf(Color(0xFFF1E7DC)),
        accent = Color(0xFFC47A3C), accentDark = Color(0xFFE8A96E)),
    WallpaperPreset("dusk", "暮色", listOf(Color(0xFF1F2937)),
        prefersDarkText = false, accent = Color(0xFF6C8CFF), accentDark = Color(0xFF8FA5FF)),
    WallpaperPreset("graphite", "石墨", listOf(Color(0xFF2C2C2E)),
        prefersDarkText = false, accent = Color(0xFF8E8E93), accentDark = Color(0xFFAEAEB2)),
)

fun presetOf(id: String): WallpaperPreset =
    WallpaperPresets.firstOrNull { it.id == id } ?: WallpaperPresets.first()

/**
 * The wallpaper layer. Blur and dim are what let a photo of anything sit behind text without
 * fighting it — the picture becomes a material rather than an image you have to read through.
 */
@Composable
fun WallpaperBackground(appearance: Appearance, modifier: Modifier = Modifier) {
    val preset = presetOf(appearance.presetId)
    // Sliders are direct manipulation: the background follows the finger instead of chasing it
    // through a 260 ms animation after every pointer update.
    val blurRadius = appearance.blur * 40f
    val scrim = Color.Black.copy(alpha = appearance.dim * 0.7f)

    Box(modifier.fillMaxSize()) {
        // Keep a solid color underneath the photo so decoding never causes a flash.
        Box(Modifier.fillMaxSize().background(preset.colors.first()))

        if (appearance.showsPhoto) {
            val file = appearance.photoFile
            var bitmap by remember(file?.absolutePath) {
                mutableStateOf<ImageBitmap?>(null)
            }
            LaunchedEffect(file?.absolutePath) {
                bitmap = file?.let { decodeScaled(it) }
            }
            bitmap?.let {
                Image(
                    bitmap = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = 1.06f; scaleY = 1.06f }
                        .blur(blurRadius.dp),
                )
            }
        }

        Box(Modifier.fillMaxSize().background(scrim))
    }
}

@Composable
private fun NotebookPaperBackground(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val paper = Color(0xFFFFFEF5)
        val blueLine = Color(0xFF78B9D4).copy(alpha = 0.26f)
        val redMargin = Color(0xFFFF7B7B).copy(alpha = 0.34f)
        val doodleInk = Color(0xFF6D6A62).copy(alpha = 0.24f)
        val markerYellow = Color(0xFFFFD93D).copy(alpha = 0.62f)
        val markerTeal = Color(0xFF4ECDC4).copy(alpha = 0.48f)
        val markerCoral = Color(0xFFFF6B6B).copy(alpha = 0.52f)
        val lineGap = 38.dp.toPx()
        val topInset = 24.dp.toPx()
        val marginX = 74.dp.toPx()

        drawRect(paper)

        var y = topInset
        while (y < size.height) {
            drawLine(
                color = blueLine,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx(),
            )
            y += lineGap
        }

        drawLine(
            color = redMargin,
            start = Offset(marginX, 0f),
            end = Offset(marginX, size.height),
            strokeWidth = 1.2.dp.toPx(),
        )

        drawCircle(
            color = markerYellow,
            radius = 15.dp.toPx(),
            center = Offset(size.width - 46.dp.toPx(), 92.dp.toPx()),
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = markerTeal,
            radius = 22.dp.toPx(),
            center = Offset(size.width - 48.dp.toPx(), size.height * 0.74f),
            style = Stroke(width = 3.dp.toPx()),
        )
        drawCircle(
            color = doodleInk,
            radius = 16.dp.toPx(),
            center = Offset(42.dp.toPx(), size.height * 0.58f),
            style = Stroke(width = 2.dp.toPx()),
        )

        drawStar(
            center = Offset(size.width - 68.dp.toPx(), size.height * 0.18f),
            outerRadius = 13.dp.toPx(),
            innerRadius = 5.dp.toPx(),
            color = markerYellow,
        )
        drawStar(
            center = Offset(34.dp.toPx(), size.height * 0.82f),
            outerRadius = 9.dp.toPx(),
            innerRadius = 3.dp.toPx(),
            color = markerCoral,
        )
        drawArrow(
            start = Offset(size.width * 0.22f, size.height * 0.36f),
            end = Offset(size.width * 0.28f, size.height * 0.36f),
            color = markerCoral,
        )
        drawScribble(
            start = Offset(size.width * 0.77f, size.height * 0.88f),
            color = markerTeal,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    color: Color,
) {
    val path = Path()
    repeat(10) { index ->
        val angle = (-Math.PI / 2.0) + index * Math.PI / 5.0
        val radius = if (index % 2 == 0) outerRadius else innerRadius
        val point = Offset(
            x = center.x + kotlin.math.cos(angle).toFloat() * radius,
            y = center.y + kotlin.math.sin(angle).toFloat() * radius,
        )
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
    }
    path.close()
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawArrow(
    start: Offset,
    end: Offset,
    color: Color,
) {
    drawLine(color, start, end, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    val direction = (end - start).let { vector ->
        val length = vector.getDistance().coerceAtLeast(1f)
        Offset(vector.x / length, vector.y / length)
    }
    val side = Offset(-direction.y, direction.x)
    val tip = end
    val left = tip - direction * 9.dp.toPx() + side * 4.dp.toPx()
    val right = tip - direction * 9.dp.toPx() - side * 4.dp.toPx()
    drawLine(color, tip, left, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color, tip, right, strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawScribble(
    start: Offset,
    color: Color,
) {
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x + 14.dp.toPx(), start.y - 9.dp.toPx(),
            start.x + 24.dp.toPx(), start.y + 9.dp.toPx(),
            start.x + 38.dp.toPx(), start.y,
        )
        cubicTo(
            start.x + 52.dp.toPx(), start.y - 9.dp.toPx(),
            start.x + 62.dp.toPx(), start.y + 9.dp.toPx(),
            start.x + 76.dp.toPx(), start.y,
        )
    }
    drawPath(
        path = path,
        color = color,
        style = androidx.compose.ui.graphics.drawscope.Stroke(
            width = 2.5.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        ),
    )
}

/** Decodes at roughly display resolution — a 12MP photo as a background is pure waste. */
private suspend fun decodeScaled(file: File, maxDimension: Int = 1440): ImageBitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val longest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (longest / sample > maxDimension) sample *= 2

            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(file.absolutePath, options)?.asImageBitmap()
        }.getOrNull()
    }
