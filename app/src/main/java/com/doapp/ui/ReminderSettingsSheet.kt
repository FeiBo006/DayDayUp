package com.doapp.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.doapp.notify.BackgroundAccess
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials

/**
 * One page for every switch that stands between a reminder and the user seeing it. They live in
 * four different corners of system settings, and a reminder can be set perfectly and still never
 * arrive because of any one of them — so they belong on a single screen that says which is off.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsSheet(
    keepAlive: Boolean,
    onKeepAliveChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val m = materials
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = rememberBackgroundAccess()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = if (m.isNeoBrutalist || m.isDoodle) m.card
        else if (m.isDark) Color(0xFF1C1C1E) else Color(0xFFF7F7F9),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 20.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("提醒与后台", style = MaterialTheme.typography.headlineSmall, color = m.label)
            Text(
                "提醒要准时送到，下面几项都得放行。系统把它们分散在不同的设置页里，这里是一次性检查的地方。",
                style = MaterialTheme.typography.bodyMedium,
                color = m.secondaryLabel,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            AccessRow(
                title = "通知权限",
                detail = "关掉就完全收不到提醒",
                granted = state.notifications,
                onFix = { BackgroundAccess.openNotificationSettings(context) },
            )
            AccessRow(
                title = "精确闹钟",
                detail = "关掉后提醒会被系统攒批，晚几分钟到",
                granted = state.exactAlarms,
                onFix = { BackgroundAccess.openExactAlarmSettings(context) },
            )
            AccessRow(
                title = "电池优化",
                detail = "受限时，息屏后的提醒会被推迟",
                granted = state.battery,
                onFix = { BackgroundAccess.openBatterySettings(context) },
            )
            if (state.hasAutoStart) {
                AccessRow(
                    title = "自启动",
                    detail = "厂商系统会在划掉应用后清空它的闹钟，开启自启动才能保住",
                    // No API reports this one, so it can't be checked — only opened.
                    granted = null,
                    onFix = { BackgroundAccess.openAutoStartSettings(context) },
                )
            }

            ToggleRow(
                title = "后台保活",
                detail = "常驻一条通知，换取被划掉后闹钟不被清空。想让提醒最可靠再开。",
                checked = keepAlive,
                onCheckedChange = onKeepAliveChange,
                modifier = Modifier.padding(top = 6.dp),
            )

            Text(
                "即使某一项没开，错过的提醒也会在下次打开应用或开机时补发一条，不会彻底丢失。",
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
private fun AccessRow(
    title: String,
    detail: String,
    granted: Boolean?,
    onFix: () -> Unit,
) {
    val m = materials
    val shape = appShape(16.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val ok = granted == true

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .pressableNoRipple(interactionSource, onFix)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (granted != null) {
            Icon(
                imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = if (ok) m.success else m.destructive,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = m.label)
            Text(
                detail,
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (ok) "已开启" else "去设置",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) m.tertiaryLabel else m.accent,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    detail: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = m.label)
            Text(
                detail,
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = m.success),
        )
    }
}

private data class BackgroundAccessState(
    val notifications: Boolean,
    val exactAlarms: Boolean,
    val battery: Boolean,
    val hasAutoStart: Boolean,
)

/** Re-reads the four system switches on every return to the foreground. */
@Composable
private fun rememberBackgroundAccess(): BackgroundAccessState {
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    return remember(resumes) {
        BackgroundAccessState(
            notifications = BackgroundAccess.notificationsAllowed(context),
            exactAlarms = BackgroundAccess.exactAlarmsAllowed(context),
            battery = BackgroundAccess.batteryUnrestricted(context),
            hasAutoStart = BackgroundAccess.hasAutoStartSettings(context),
        )
    }
}

/**
 * Recomposition trigger for state that lives in system settings. Counts foreground returns, so
 * `remember(resumes) { ... }` re-reads a permission after the user has been off changing it.
 */
@Composable
fun rememberResumeCount(): Int {
    // Not just `as? LifecycleOwner`: inside a sheet the local context can be a themed wrapper
    // around the activity rather than the activity itself.
    val owner = LocalContext.current.findLifecycleOwner()
    var resumes by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val lifecycle = owner?.lifecycle ?: return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumes++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return resumes
}

private tailrec fun Context.findLifecycleOwner(): LifecycleOwner? = when (this) {
    is LifecycleOwner -> this
    is ContextWrapper -> baseContext.findLifecycleOwner()
    else -> null
}
