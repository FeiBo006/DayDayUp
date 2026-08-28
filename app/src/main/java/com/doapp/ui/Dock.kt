package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
    val HorizontalPadding = 28.dp
    val MaxWidth = 520.dp
    val Height = 72.dp
    val BottomGap = 10.dp
    val ContentGap = 40.dp
    val ReservedBottomPadding = Height + BottomGap + ContentGap
}

@Composable
fun DockBar(
    current: AppPage,
    onSelect: (AppPage) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier
            .padding(horizontal = DockMetrics.HorizontalPadding)
            .widthIn(max = DockMetrics.MaxWidth)
            .fillMaxWidth()
            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + DockMetrics.BottomGap)
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.05f),
                spotColor = Color.Black.copy(alpha = 0.10f),
            )
            .clip(shape)
            .background(Color.White)
            .border(1.dp, Color(0xFFE4E6E9), shape)
            .height(DockMetrics.Height)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DockApp(
            page = AppPage.DO,
            label = "Do",
            icon = Icons.Rounded.CheckCircle,
            iconColor = Color(0xFF17191C),
            selected = current == AppPage.DO,
            onClick = { onSelect(AppPage.DO) },
            modifier = Modifier.weight(1f),
        )
        DockApp(
            page = AppPage.FOCUS,
            label = "专注",
            icon = Icons.Rounded.Timer,
            iconColor = Color(0xFF22A06B),
            selected = current == AppPage.FOCUS,
            onClick = { onSelect(AppPage.FOCUS) },
            modifier = Modifier.weight(1f),
        )
        DockApp(
            page = AppPage.SETTINGS,
            label = "设置",
            icon = Icons.Rounded.Settings,
            iconColor = Color(0xFF667085),
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
    iconColor: Color,
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

    Column(
        modifier
            .semantics { role = Role.Button }
            .clip(RoundedCornerShape(18.dp))
            .pressableNoRipple(interactionSource, onClick)
            .padding(vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconColor),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) Color(0xFF17191C) else Color(0xFF7A8088),
            modifier = Modifier.padding(top = 2.dp),
        )
        Box(
            Modifier
                .padding(top = 2.dp)
                .size(3.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0xFF17191C) else Color.Transparent),
        )
    }
}
