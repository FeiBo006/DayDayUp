package com.doapp.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.ActiveFocus
import com.doapp.data.Appearance
import com.doapp.data.FocusSession
import com.doapp.data.FocusSlice
import com.doapp.data.StatRange
import com.doapp.data.formatClock
import com.doapp.data.formatDurationLong
import com.doapp.data.formatDurationShort
import com.doapp.data.rangeBounds
import com.doapp.data.stepAnchor
import com.doapp.data.summarize
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import java.time.LocalDate

private val DockClearanceFocus = 88.dp

/**
 * Where the time went.
 *
 * Two cards, mirroring the two questions: *how much today* (the headline), and *split how* (the
 * breakdown). The headline is a stat tile rather than a chart, because one number with a count
 * beside it is already the clearest form it can take.
 */
@Composable
fun FocusScreen(
    sessions: List<FocusSession>,
    active: ActiveFocus?,
    appearance: Appearance,
    onOpenTimer: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    var range by remember { mutableStateOf(StatRange.DAY) }
    var anchor by remember { mutableStateOf(LocalDate.now()) }
    var dayAnchor by remember { mutableStateOf(LocalDate.now()) }

    val (from, to) = rangeBounds(range, anchor)
    val summary = remember(sessions, from, to) { summarize(sessions, from, to) }
    val daySummary = remember(sessions, dayAnchor) { summarize(sessions, dayAnchor, dayAnchor) }
    val wedges = remember(summary) { foldSlices(summary.slices) }

    Box(modifier.fillMaxSize()) {
        WallpaperBackground(appearance)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues()
                    .calculateBottomPadding() + DockClearanceFocus + 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text("专注", style = MaterialTheme.typography.displaySmall, color = m.label)
                    Text(
                        "看看时间都花在了哪里",
                        style = MaterialTheme.typography.bodyMedium,
                        color = m.secondaryLabel,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            item(key = "running") {
                RunningBanner(active = active, onClick = onOpenTimer)
            }

            item(key = "today") {
                DayCard(
                    date = dayAnchor,
                    count = daySummary.sessions,
                    totalMillis = daySummary.totalMillis,
                    onStep = { forward -> dayAnchor = dayAnchor.plusDays(if (forward) 1 else -1) },
                )
            }

            item(key = "distribution") {
                DistributionCard(
                    range = range,
                    from = from,
                    to = to,
                    wedges = wedges,
                    totalMillis = summary.totalMillis,
                    onRangeChange = { range = it },
                    onStep = { forward -> anchor = stepAnchor(range, anchor, forward) },
                    onOpenRecords = onOpenRecords,
                )
            }
        }
    }
}

/**
 * The run in progress, promoted to the top of the page. A timer you can't find is a timer you
 * forget to stop, and an unstopped timer poisons the statistics it feeds.
 */
@Composable
private fun RunningBanner(active: ActiveFocus?, onClick: () -> Unit) {
    val m = materials
    val shape = appShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = Motion.Snappy,
        label = "runningPress",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .styleShadow(shape, elevation = if (m.isNeoBrutalist) 8.dp else 7.dp, spotColor = Color.Black.copy(alpha = 0.3f))
            .clip(shape)
            .background(if (active != null) m.accent else m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (active != null) onColor(m.accent) else m.label
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (active != null) ink.copy(alpha = 0.18f) else m.accent.copy(alpha = 0.12f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = null,
                tint = if (active != null) ink else m.accent,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = active?.label ?: "开始专注",
                style = MaterialTheme.typography.bodyLarge,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = when {
                    active == null -> "挑一件事，计时开始"
                    active.isPaused -> "已暂停 · ${formatClock(active.elapsed(System.currentTimeMillis()))}"
                    else -> "正在进行"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (active != null) ink.copy(alpha = 0.78f) else m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
    }
}

/** The headline: a stat tile, not a one-bar chart. */
@Composable
private fun DayCard(
    date: LocalDate,
    count: Int,
    totalMillis: Long,
    onStep: (Boolean) -> Unit,
) {
    val m = materials
    val shape = appShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CardHeader(title = "当日专注", subtitle = date.toString(), onStep = onStep)
        Row(Modifier.fillMaxWidth()) {
            StatColumn(
                caption = "次数",
                value = count.toString(),
                modifier = Modifier.weight(1f),
            )
            StatColumn(
                caption = "时长",
                value = if (totalMillis == 0L) "0 分钟" else formatDurationLong(totalMillis),
                modifier = Modifier.weight(1f),
                alignEnd = true,
            )
        }
    }
}

@Composable
private fun StatColumn(
    caption: String,
    value: String,
    modifier: Modifier = Modifier,
    alignEnd: Boolean = false,
) {
    val m = materials
    Column(
        modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
    ) {
        Text(caption, style = MaterialTheme.typography.labelLarge, color = m.secondaryLabel)
        Text(
            value,
            style = MaterialTheme.typography.displaySmall,
            color = m.label,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun DistributionCard(
    range: StatRange,
    from: LocalDate,
    to: LocalDate,
    wedges: List<FocusSlice>,
    totalMillis: Long,
    onRangeChange: (StatRange) -> Unit,
    onStep: (Boolean) -> Unit,
    onOpenRecords: () -> Unit,
) {
    val m = materials
    val shape = appShape(18.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CardHeader(
            title = "专注时长分布",
            subtitle = if (from == to) from.toString() else "$from — $to",
            onStep = if (range == StatRange.CUSTOM) null else onStep,
        )

        RangeSegments(selected = range, onSelect = onRangeChange)

        if (wedges.isEmpty()) {
            Text(
                "这段时间还没有专注记录。",
                style = MaterialTheme.typography.bodyMedium,
                color = m.secondaryLabel,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FocusDonut(
                    slices = wedges,
                    modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1f),
                ) {
                    // The hero number lives in the hole — the headline and the split read together.
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "总计",
                            style = MaterialTheme.typography.labelLarge,
                            color = m.secondaryLabel,
                        )
                        Text(
                            formatDurationLong(totalMillis),
                            style = MaterialTheme.typography.headlineSmall,
                            color = m.label,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }

            // Not decoration: the palette's light-mode contrast and CVD margins are only legal
            // with visible labels beside the colour, so this list is part of the chart.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                wedges.forEachIndexed { index, slice ->
                    LegendRow(index = index, slice = slice)
                }
            }

            TextAction(label = "查看专注记录", onClick = onOpenRecords)
        }
    }
}

@Composable
private fun LegendRow(index: Int, slice: FocusSlice) {
    val m = materials
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(SeriesPalette.color(index, m.isDark, slice.isOther))
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                slice.label,
                // Text wears text tokens, never the series colour — the dot carries identity.
                style = MaterialTheme.typography.bodyLarge,
                color = m.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDurationShort(slice.millis),
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            "%.1f%%".format(slice.fraction * 100),
            style = MaterialTheme.typography.titleMedium,
            color = m.secondaryLabel,
        )
    }
}

@Composable
private fun CardHeader(title: String, subtitle: String, onStep: ((Boolean) -> Unit)?) {
    val m = materials
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = m.label)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        if (onStep != null) {
            StepButton(forward = false, onClick = { onStep(false) })
            Spacer(Modifier.width(4.dp))
            StepButton(forward = true, onClick = { onStep(true) })
        }
    }
}

@Composable
private fun StepButton(forward: Boolean, onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = Motion.Snappy,
        label = "stepPress",
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(34.dp)
            .clip(CircleShape)
            .background(m.chrome.copy(alpha = if (m.isNeoBrutalist) 1f else 0.42f))
            .pressableNoRipple(interactionSource, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (forward) Icons.Rounded.ChevronRight else Icons.Rounded.ChevronLeft,
            contentDescription = if (forward) "后一段" else "前一段",
            tint = m.secondaryLabel,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Day / week / month, with one indicator that slides between them. */
@Composable
private fun RangeSegments(selected: StatRange, onSelect: (StatRange) -> Unit) {
    val m = materials
    val options = listOf(
        StatRange.DAY to "日",
        StatRange.WEEK to "周",
        StatRange.MONTH to "月",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(appShape(11.dp))
            .background(if (m.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            val interactionSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .weight(1f)
                    .clip(appShape(9.dp))
                    .background(
                        when {
                            !active -> Color.Transparent
                            m.isDark -> Color(0xFF48484A)
                            else -> Color.White
                        }
                    )
                    .pressableNoRipple(interactionSource) { onSelect(value) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) m.label else m.secondaryLabel,
                )
            }
        }
    }
}

@Composable
private fun TextAction(label: String, onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(appShape(12.dp))
            .background(m.accent.copy(alpha = 0.12f))
            .pressableNoRipple(interactionSource, onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = m.accent)
    }
}
