package com.doapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials

/** Which top-level page the dock is showing. */
enum class AppPage { DO, SETTINGS }

/**
 * The bottom dock — one frosted bar for the whole app. DO is the list, SETTINGS is everything
 * else. It floats above the wallpaper like the rest of the chrome.
 */
@Composable
fun DockBar(
    current: AppPage,
    onSelect: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(28.dp)

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 10.dp)
            .styleShadow(
                shape = shape,
                elevation = if (m.isNeoBrutalist) 8.dp else 18.dp,
                spotColor = if (m.isNeoBrutalist) Color.Black else Color.Black.copy(alpha = 0.45f),
            )
            .clip(shape)
            .background(m.chrome)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockItem(
            label = "Do",
            icon = Icons.Rounded.CheckCircle,
            selected = current == AppPage.DO,
            onClick = { onSelect(AppPage.DO) },
        )
        DockItem(
            label = "设置",
            icon = Icons.Rounded.Settings,
            selected = current == AppPage.SETTINGS,
            onClick = { onSelect(AppPage.SETTINGS) },
        )
    }
}

@Composable
private fun DockItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    // In neo-brutalist the dock's chrome IS the wallpaper accent, so the selected item must
    // invert — a solid on-accent pill with the accent on top — or it vanishes into the bar.
    val onAccent = if (m.accent.luminance() > 0.5f) Color.Black else Color.White
    val background by animateColorAsState(
        targetValue = when {
            selected && m.isNeoBrutalist -> onAccent
            selected -> m.accent.copy(alpha = 0.16f)
            else -> Color.Transparent
        },
        animationSpec = tween(200),
        label = "dockItemBg",
    )
    val foreground = when {
        selected -> m.accent
        m.isNeoBrutalist -> onAccent
        else -> m.secondaryLabel
    }

    Column(
        Modifier
            .clip(appShape(16.dp))
            .background(background)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 26.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = foreground,
            modifier = Modifier.size(22.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = foreground,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
