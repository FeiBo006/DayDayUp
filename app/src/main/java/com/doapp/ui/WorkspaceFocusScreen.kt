package com.doapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doapp.data.ActiveFocus
import com.doapp.data.FocusMode
import com.doapp.data.FocusSession
import com.doapp.data.FocusSlice
import com.doapp.data.FocusSummary
import com.doapp.data.formatClock
import com.doapp.data.formatDurationLong
import com.doapp.data.formatDurationShort
import com.doapp.data.summarize
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun WorkspaceFocusScreen(
    sessions: List<FocusSession>,
    active: ActiveFocus?,
    onOpenTimer: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val summary = remember(sessions, today) { summarize(sessions, today, today) }
    val recent = remember(sessions, today) {
        summarize(sessions, today.minusDays(29), today).slices.take(4)
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        while (active != null) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    WorkspacePage(modifier) { wide ->
        WorkspaceHeader(
                eyebrow = "DayDayUp / Focus",
                title = "专注",
                subtitle = "把注意力留给正在做的事",
                action = {
                    WorkspaceActionButton(
                        label = if (active == null) "开始" else "查看",
                        icon = if (active == null) Icons.Rounded.PlayArrow else Icons.Rounded.Timer,
                        onClick = onOpenTimer,
                        color = WorkspaceColors.Focus,
                    )
                },
        )

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    active?.let { ActiveFocusPanel(it, now, onOpenTimer) }
                    TodayFocusPanel(summary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    RecordsButton(onOpenRecords)
                    RecentFocusPanel(recent)
                }
            }
        } else {
            active?.let { running -> ActiveFocusPanel(running, now, onOpenTimer) }
            TodayFocusPanel(summary)
            RecordsButton(onOpenRecords)
            RecentFocusPanel(recent)
        }

        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                WorkspaceHeatmapPanel(
                    sessions = sessions,
                    today = today,
                    modifier = Modifier.weight(1f),
                )
                WorkspaceDailyTrendPanel(
                    sessions = sessions,
                    today = today,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            WorkspaceHeatmapPanel(sessions = sessions, today = today)
            WorkspaceDailyTrendPanel(sessions = sessions, today = today)
        }
    }
}

@Composable
private fun TodayFocusPanel(summary: FocusSummary) {
    WorkspacePanel {
        Text("今天", color = WorkspaceColors.Secondary, style = MaterialTheme.typography.labelLarge)
        Text(
            text = if (summary.isEmpty) "0 分钟" else formatDurationLong(summary.totalMillis),
            color = WorkspaceColors.Ink,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            text = "${summary.sessions} 次完成的专注",
            color = WorkspaceColors.Secondary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (summary.slices.isNotEmpty()) {
            WorkspaceDivider()
            summary.slices.take(3).forEachIndexed { index, slice ->
                Row(
                    Modifier.fillMaxWidth().padding(top = if (index == 0) 12.dp else 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(slice.label, color = WorkspaceColors.Ink, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(formatDurationShort(slice.millis), color = WorkspaceColors.Secondary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun RecordsButton(onOpenRecords: () -> Unit) {
    WorkspaceActionButton(
        label = "全部记录",
        icon = Icons.Rounded.History,
        onClick = onOpenRecords,
        modifier = Modifier.fillMaxWidth(),
        color = WorkspaceColors.Ink,
    )
}

@Composable
private fun RecentFocusPanel(recent: List<FocusSlice>) {
    WorkspacePanel {
        Text("最近 30 天", color = WorkspaceColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (recent.isEmpty()) {
            Text(
                "完成一次专注后，记录会出现在这里。",
                color = WorkspaceColors.Tertiary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 14.dp),
            )
        } else {
            recent.forEachIndexed { index, slice ->
                if (index > 0) WorkspaceDivider()
                WorkspaceRow(
                    title = slice.label,
                    subtitle = "同名专注已合并",
                    trailing = {
                        Text(formatDurationShort(slice.millis), color = WorkspaceColors.Secondary, style = MaterialTheme.typography.labelLarge)
                    },
                )
            }
        }
    }
}

@Composable
private fun ActiveFocusPanel(active: ActiveFocus, now: Long, onClick: () -> Unit) {
    val shownMillis = if (active.mode == FocusMode.COUNTDOWN) active.remaining(now) else active.elapsed(now)
    Box(Modifier.fillMaxWidth()) {
        WorkspacePanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (active.isPaused) "已暂停" else "进行中",
                        color = WorkspaceColors.Focus,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        active.label,
                        color = WorkspaceColors.Ink,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    formatClock(shownMillis),
                    color = WorkspaceColors.Ink,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            WorkspaceActionButton(
                label = "打开计时器",
                icon = Icons.Rounded.Timer,
                onClick = onClick,
                color = WorkspaceColors.Focus,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            )
        }
    }
}
