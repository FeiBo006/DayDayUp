package com.doapp.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex

@Composable
fun AppOpenTransition(
    current: AppPage,
    modifier: Modifier = Modifier,
    content: @Composable (AppPage) -> Unit,
) {
    val reduceMotion = rememberReduceMotion()
    var displayed by remember {
        mutableStateOf(current.takeUnless { it == AppPage.LANDING } ?: AppPage.DO)
    }
    var hasOpenedPage by remember { mutableStateOf(current != AppPage.LANDING) }
    val progress = remember {
        Animatable(if (current == AppPage.LANDING) 0f else 1f)
    }
    val pageProgress = remember { Animatable(1f) }

    LaunchedEffect(current, reduceMotion) {
        if (current == AppPage.LANDING) {
            pageProgress.snapTo(1f)
            progress.animateTo(
                0f,
                if (reduceMotion) tween(90) else spring(dampingRatio = 1f, stiffness = 650f),
            )
        } else {
            val firstOpen = !hasOpenedPage
            if (displayed != current && progress.value > 0.98f) {
                // Switching Dock apps is peer navigation, not another launch. Fade through the
                // shared white canvas so the GPU never has to blend two full-screen pages.
                pageProgress.animateTo(
                    0f,
                    tween(if (reduceMotion) 35 else 55, easing = Motion.EaseOut),
                )
                displayed = current
                hasOpenedPage = true
                pageProgress.animateTo(
                    1f,
                    tween(if (reduceMotion) 55 else 90, easing = Motion.EaseOut),
                )
            } else {
                pageProgress.snapTo(if (firstOpen) 0f else 1f)
                displayed = current
                hasOpenedPage = true
                progress.animateTo(
                    1f,
                    if (reduceMotion) tween(100) else spring(dampingRatio = 1f, stiffness = 520f),
                )
                if (firstOpen) {
                    // Compose on the now-stationary white surface, then reveal. Any unavoidable
                    // first-use class loading cannot tear an in-flight transform this way.
                    pageProgress.animateTo(
                        1f,
                        tween(if (reduceMotion) 55 else 90, easing = Motion.EaseOut),
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (hasOpenedPage && (current != AppPage.LANDING || progress.value > 0.001f)) {
            val dockWidth = (maxWidth - DockMetrics.HorizontalPadding - DockMetrics.HorizontalPadding)
                .coerceAtMost(DockMetrics.MaxWidth)
            val dockLeft = (maxWidth - dockWidth) / 2
            val iconFraction = when (displayed) {
                AppPage.DO -> 1f / 6f
                AppPage.FOCUS -> 3f / 6f
                AppPage.SETTINGS -> 5f / 6f
                AppPage.LANDING -> 3f / 6f
            }
            val originX = ((dockLeft + dockWidth * iconFraction) / maxWidth).coerceIn(0f, 1f)
            // A small origin-aware grow preserves the app-launch relationship without asking the
            // GPU to magnify a full-screen texture from almost zero on every navigation.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val value = progress.value
                        val scale = if (reduceMotion) 1f else 0.84f + 0.16f * value
                        scaleX = scale
                        scaleY = scale
                        alpha = value
                        transformOrigin = TransformOrigin(originX, 0.96f)
                    }
                    .background(Color.White),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .zIndex(2f)
                        .graphicsLayer { alpha = pageProgress.value },
                ) {
                    content(displayed)
                }
            }
        }
    }
}
