package com.doapp.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.ActiveFocus
import com.doapp.data.FocusSession
import com.doapp.data.FocusSlice
import com.doapp.data.StatRange
import com.doapp.data.dailyFocusSummaries
import com.doapp.data.formatClock
import com.doapp.data.formatDurationLong
import com.doapp.data.formatDurationShort
import com.doapp.data.rangeBounds
import com.doapp.data.stepAnchor
import com.doapp.data.summarize
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

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
    onOpenTimer: () -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    var todayEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    val today = LocalDate.ofEpochDay(todayEpochDay)
    var rangeName by rememberSaveable { mutableStateOf(StatRange.WEEK.name) }
    val range = runCatching { StatRange.valueOf(rangeName) }.getOrDefault(StatRange.WEEK)
    var anchorEpochDay by rememberSaveable { mutableLongStateOf(todayEpochDay) }
    val anchor = LocalDate.ofEpochDay(anchorEpochDay)
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(todayEpochDay) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    // Keep the dashboard honest when it remains open across midnight.
    LaunchedEffect(todayEpochDay) {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
        todayEpochDay = LocalDate.now().toEpochDay()
    }

    val (from, to) = rangeBounds(range, anchor)
    val summary = remember(sessions, from, to) { summarize(sessions, from, to) }
    val wedges = remember(summary) { foldSlices(summary.slices) }

    val heatmapFrom = remember(today) { today.with(DayOfWeek.MONDAY).minusWeeks(11) }
    val heatmapTo = remember(heatmapFrom) { heatmapFrom.plusDays(83) }
    val heatmapDays = remember(sessions, heatmapFrom, heatmapTo) {
        dailyFocusSummaries(sessions, heatmapFrom, heatmapTo)
    }
    val elapsedDays = remember(heatmapDays, today) { heatmapDays.filterNot { it.date.isAfter(today) } }
    val todayMillis = elapsedDays.firstOrNull { it.date == today }?.totalMillis ?: 0L
    val weekStart = remember(today) { today.with(DayOfWeek.MONDAY) }
    val weekMillis = elapsedDays.filterNot { it.date.isBefore(weekStart) }.sumOf { it.totalMillis }
    val activeDays = elapsedDays.count { it.totalMillis > 0L }
    val dashboardSessions = elapsedDays.sumOf { it.sessionCount }
    val trendDays = remember(elapsedDays, today) {
        val start = today.minusDays(6)
        elapsedDays.filter { !it.date.isBefore(start) && !it.date.isAfter(today) }
    }

    Box(modifier.fillMaxSize()) {
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
                        "把每天的投入，变成看得见的进步",
                        style = MaterialTheme.typography.bodyMedium,
                        color = m.secondaryLabel,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            item(key = "running") {
                RunningBanner(active = active, onClick = onOpenTimer)
            }

            item(key = "overview") {
                StudyOverviewCard(
                    todayMillis = todayMillis,
                    weekMillis = weekMillis,
                    activeDays = activeDays,
                    sessionCount = dashboardSessions,
                )
            }

            item(key = "heatmap") {
                StudyHeatmapCard(
                    days = heatmapDays,
                    today = today,
                    selectedDate = selectedDate,
                    onSelectDate = { selectedEpochDay = it.toEpochDay() },
                    onOpenRecords = onOpenRecords,
                )
            }

            item(key = "trend") {
                SevenDayTrendCard(
                    days = trendDays,
                    selectedDate = selectedDate,
                    onSelectDate = { selectedEpochDay = it.toEpochDay() },
                )
            }

            item(key = "distribution") {
                DistributionCard(
                    range = range,
                    from = from,
                    to = to,
                    wedges = wedges,
                    totalMillis = summary.totalMillis,
                    onRangeChange = {
                        rangeName = it.name
                        anchorEpochDay = todayEpochDay
                    },
                    onStep = { forward ->
                        anchorEpochDay = stepAnchor(range, anchor, forward).toEpochDay()
                    },
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
    val shape = appShape(24.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = Motion.Press,
        label = "runningPress",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .styleShadow(shape, elevation = 3.dp, spotColor = Color.Black.copy(alpha = 0.16f))
            .clip(shape)
            .background(if (active != null) m.accent else m.card)
            .styleBorder(shape, if (active != null) m.accent else m.hairline)
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
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = if (active != null) ink.copy(alpha = 0.72f) else m.tertiaryLabel,
            modifier = Modifier.size(20.dp),
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
    val shape = appShape(24.dp)

    Column(
        Modifier
            .fillMaxWidth()
            .premiumSurface(shape = shape, elevation = 8.dp)
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
        animationSpec = Motion.Press,
        label = "stepPress",
    )
    Box(
        Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .size(34.dp)
            .clip(CircleShape)
            .background(m.chrome)
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
    val reduceMotion = rememberReduceMotion()
    val options = listOf(
        StatRange.DAY to "日",
        StatRange.WEEK to "周",
        StatRange.MONTH to "月",
    )
    val position = animateFloatAsState(
        targetValue = options.indexOfFirst { it.first == selected }.coerceAtLeast(0).toFloat(),
        animationSpec = if (reduceMotion) Motion.Instant else Motion.Select,
        label = "rangeIndicator",
    )
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .clip(appShape(11.dp))
            .background(if (m.isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
            .padding(2.dp),
    ) {
        val gap = 2.dp
        val segmentWidth = (maxWidth - gap * 2) / options.size
        val stepPx = with(LocalDensity.current) { (segmentWidth + gap).toPx() }
        Box(
            Modifier
                .width(segmentWidth)
                .height(34.dp)
                .graphicsLayer { translationX = stepPx * position.value }
                .clip(appShape(9.dp))
                .background(if (m.isDark) Color(0xFF48484A) else Color.White)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            options.forEach { (value, label) ->
                val active = value == selected
                val interactionSource = remember { MutableInteractionSource() }
                val foreground by animateColorAsState(
                    targetValue = if (active) m.label else m.secondaryLabel,
                    animationSpec = tween(130, easing = Motion.EaseOut),
                    label = "rangeLabel",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(appShape(9.dp))
                        .pressableNoRipple(interactionSource) {
                            if (!active) onSelect(value)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = foreground,
                    )
                }
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
