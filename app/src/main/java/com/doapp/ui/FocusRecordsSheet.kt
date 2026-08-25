package com.doapp.ui

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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.FocusMode
import com.doapp.data.FocusSession
import com.doapp.data.formatDurationShort
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The log behind the chart. A statistic nobody can audit is a statistic nobody trusts — this is
 * where a wrong-looking wedge gets explained, and where a run you forgot to stop gets deleted.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusRecordsSheet(
    sessions: List<FocusSession>,
    onDelete: (FocusSession) -> Unit,
    onDismiss: () -> Unit,
) {
    val m = materials
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = remember { ZoneId.systemDefault() }
    val newestFirst = remember(sessions) { sessions.sortedByDescending { it.startedAt } }
    val byDay = remember(newestFirst) {
        newestFirst.groupBy { Instant.ofEpochMilli(it.startedAt).atZone(zone).toLocalDate() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        containerColor = m.card,
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
            Text("专注记录", style = MaterialTheme.typography.headlineSmall, color = m.label)

            if (newestFirst.isEmpty()) {
                Text(
                    "还没有记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.secondaryLabel,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            byDay.forEach { (day, ofDay) ->
                Text(
                    text = "$day · ${formatDurationShort(ofDay.sumOf { it.durationMillis })}",
                    style = MaterialTheme.typography.labelLarge,
                    color = m.secondaryLabel,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
                ofDay.forEach { session ->
                    RecordRow(session = session, zone = zone, onDelete = { onDelete(session) })
                }
            }
        }
    }
}

@Composable
private fun RecordRow(session: FocusSession, zone: ZoneId, onDelete: () -> Unit) {
    val m = materials
    val shape = appShape(14.dp)
    val timeFormat = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val start = Instant.ofEpochMilli(session.startedAt).atZone(zone).format(timeFormat)
    val end = Instant.ofEpochMilli(session.endedAt).atZone(zone).format(timeFormat)
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.hairline)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                session.label,
                style = MaterialTheme.typography.bodyLarge,
                color = m.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$start – $end · ${if (session.mode == FocusMode.COUNTDOWN) "倒计时" else "正计时"}",
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            formatDurationShort(session.durationMillis),
            style = MaterialTheme.typography.titleMedium,
            color = m.label,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .size(34.dp)
                .clip(appShape(11.dp))
                .background(m.destructive.copy(alpha = 0.12f))
                .pressableNoRipple(interactionSource, onDelete),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = "删除这条记录",
                tint = m.destructive,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
