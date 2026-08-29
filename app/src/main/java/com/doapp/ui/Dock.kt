package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppPage { LANDING, DO, FOCUS, SETTINGS }

internal object DockMetrics {
    val HorizontalPadding = 44.dp
    val MaxWidth = 420.dp
    val Height = 58.dp
    val BottomGap = 12.dp
    val ContentGap = 28.dp
    val ReservedBottomPadding = Height + BottomGap + ContentGap
}

@Composable
fun DockBar(
    current: AppPage,
    onSelect: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier
            .padding(horizontal = DockMetrics.HorizontalPadding)
            .widthIn(max = DockMetrics.MaxWidth)
            .fillMaxWidth()
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DockMetrics.BottomGap)
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f),
            )
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE7E9EC), shape)
            .height(DockMetrics.Height)
            .padding(horizontal = 5.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockApp(
            page = AppPage.DO,
            label = "Do",
            icon = Icons.Rounded.CheckCircle,
            selected = current == AppPage.DO,
            onClick = { onSelect(AppPage.DO) },
            modifier = Modifier.weight(1f),
        )
        DockApp(
            page = AppPage.FOCUS,
            label = "专注",
            icon = Icons.Rounded.Timer,
            selected = current == AppPage.FOCUS,
            onClick = { onSelect(AppPage.FOCUS) },
            modifier = Modifier.weight(1f),
        )
        DockApp(
            page = AppPage.SETTINGS,
            label = "设置",
            icon = Icons.Rounded.Settings,
            selected = current == AppPage.SETTINGS,
            onClick = { onSelect(AppPage.SETTINGS) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DockApp(
    page: AppPage,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = Motion.Press,
        label = "dockAppPress-${page.name}",
    )

    Row(
        modifier
            .semantics { role = Role.Button }
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(15.dp))
            .background(if (selected) Color(0xFFF1F2F4) else Color.Transparent)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) Color(0xFF17191C) else Color(0xFF858B93),
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFF17191C) else Color(0xFF858B93),
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}
