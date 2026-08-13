package com.doapp.ui

import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import kotlin.math.sign

/**
 * Apple describes springs with damping ratio + response (seconds), not mass/stiffness.
 * Compose wants stiffness, and with mass = 1 the two relate as stiffness = (2π / response)².
 * These three specs cover everything in the app.
 */
object Motion {
    /** Reposition, settle-back, list movement. Critically damped: no overshoot. */
    val Move: AnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 247f)   // response 0.40s

    /** Immediate feedback: press states, small toggles. */
    val Snappy: AnimationSpec<Float> = spring(dampingRatio = 1f, stiffness = 439f) // response 0.30s

    /** Only after a gesture carried momentum — a flick deserves the overshoot, a fade does not. */
    val Momentum: AnimationSpec<Float> = spring(dampingRatio = 0.8f, stiffness = 247f)
}

/**
 * Apple's momentum projection from *Designing Fluid Interfaces* — where a flick would come to
 * rest, given its release velocity. Not the textbook v²/2a; this is the exponential-decay form
 * the platform actually ships.
 */
fun projectMomentum(velocity: Float, decelerationRate: Float = 0.998f): Float =
    (velocity / 1000f) * decelerationRate / (1f - decelerationRate)

/**
 * Progressive resistance past a boundary. A hard stop reads as frozen; this reads as
 * "still responding, but there's nothing more this way".
 */
fun rubberBand(offset: Float, limit: Float, dimension: Float, constant: Float = 0.55f): Float {
    val magnitude = abs(offset)
    if (magnitude <= limit) return offset
    val overshoot = magnitude - limit
    val damped = (overshoot * dimension * constant) / (dimension + constant * overshoot)
    return sign(offset) * (limit + damped)
}

/**
 * Honors the system "remove animations" setting. Reduced motion is not *no* feedback — callers
 * swap springs for a plain cross-fade and drop the overshoot, but keep the state change visible.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
}
