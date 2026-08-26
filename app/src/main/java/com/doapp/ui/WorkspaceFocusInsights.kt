package com.doapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.DailyFocusSummary
import com.doapp.data.FocusSession
import com.doapp.data.FocusSlice
import com.doapp.data.aggregateFocusLabels
import com.doapp.data.dailyFocusSummaries
import com.doapp.data.focusIntensity
import com.doapp.data.formatDurationLong
import com.doapp.data.formatDurationShort
import java.time.DayOfWeek
import java.time.LocalDate

private val HeatLevels = listOf(
    Color(0xFFE8ECEA),
    Color(0xFFC9E6D9),
    Color(0xFF8CCDAE),
    Color(0xFF48AD7D),
    WorkspaceColors.Focus,
)

@Composable
internal fun WorkspaceHeatmapPanel(
    sessions: List<FocusSession>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val start = remember(today) { today.minusWeeks(11).with(DayOfWeek.MONDAY) }
    val end = remember(start) { start.plusDays(83) }
    val days = remember(sessions, start, end) { dailyFocusSummaries(sessions, start, end) }
    val activeDays = remember(days, today) { days.count { !it.date.isAfter(today) && it.totalMillis > 0L } }
    val totalMillis = remember(days, today) {
        days.filterNot { it.date.isAfter(today) }.sumOf { it.totalMillis }
    }

    WorkspacePanel(modifier) {
        InsightHeader(
            title = "活跃热力图",
            subtitle = "近 12 周 · $activeDays 天活跃",
            trailing = formatDurationShort(totalMillis),
        )

        Row(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "${start.monthValue}月",
                style = MaterialTheme.typography.labelSmall,
                color = WorkspaceColors.Tertiary,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "${today.monthValue}月",
                style = MaterialTheme.typography.labelSmall,
                color = WorkspaceColors.Tertiary,
            )
        }

        Heatmap(days = days, today = today, modifier = Modifier.padding(top = 6.dp))

        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("较少", style = MaterialTheme.typography.labelSmall, color = WorkspaceColors.Tertiary)
            HeatLevels.forEach { color ->
                Box(
                    Modifier
                        .padding(start = 4.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(color)
                )
            }
            Text(
                "较多",
                style = MaterialTheme.typography.labelSmall,
                color = WorkspaceColors.Tertiary,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun Heatmap(
    days: List<DailyFocusSummary>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val weeks = remember(days) { days.chunked(7) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        weeks.forEach { week ->
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                week.forEach { day ->
                    val future = day.date.isAfter(today)
                    val level = if (future) 0 else focusIntensity(day.totalMillis)
                    val description = if (future) {
                        "${day.date}，未来日期"
                    } else if (day.totalMillis == 0L) {
                        "${day.date}，没有专注记录"
                    } else {
                        "${day.date}，${formatDurationLong(day.totalMillis)}"
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .semantics { contentDescription = description }
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (future) WorkspaceColors.Panel
                                else HeatLevels[level.coerceIn(0, HeatLevels.lastIndex)]
                            )
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceDailyTrendPanel(
    sessions: List<FocusSession>,
    today: LocalDate,
    modifier: Modifier = Modifier,
) {
    val start = remember(today) { today.minusDays(13) }
    val days = remember(sessions, start, today) { dailyFocusSummaries(sessions, start, today) }
    val allSessions = remember(days) { days.flatMap { it.sessions } }
    val rawSeries = remember(allSessions) { aggregateFocusLabels(allSessions) }
    val series = remember(rawSeries) { foldSlices(rawSeries, max = 5) }
    val maxMillis = remember(days) { days.maxOfOrNull { it.totalMillis }?.coerceAtLeast(60_000L) ?: 60_000L }

    WorkspacePanel(modifier) {
        InsightHeader(
            title = "按天专注趋势",
            subtitle = "近 14 天",
            trailing = formatDurationShort(allSessions.sumOf { it.durationMillis }),
        )

        if (allSessions.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(150.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "完成专注后，这里会出现趋势。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WorkspaceColors.Tertiary,
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(154.dp).padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEachIndexed { index, day ->
                    TrendDay(
                        day = day,
                        series = series,
                        maxMillis = maxMillis,
                        showDate = index % 2 == 0 || index == days.lastIndex,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Column(
                Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                series.forEachIndexed { index, slice ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(SeriesPalette.color(index, isDark = false, isOther = slice.isOther))
                        )
                        Text(
                            slice.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = WorkspaceColors.Secondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 8.dp).weight(1f),
                        )
                        Text(
                            formatDurationShort(slice.millis),
                            style = MaterialTheme.typography.labelMedium,
                            color = WorkspaceColors.Ink,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrendDay(
    day: DailyFocusSummary,
    series: List<FocusSlice>,
    maxMillis: Long,
    showDate: Boolean,
    modifier: Modifier = Modifier,
) {
    val values = remember(day, series) { valuesForSeries(day, series) }
    val height = if (day.totalMillis == 0L) 3.dp else {
        (10f + 102f * (day.totalMillis.toFloat() / maxMillis).coerceIn(0f, 1f)).dp
    }

    Column(
        modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (day.totalMillis == 0L) {
            Box(
                Modifier
                    .width(10.dp)
                    .height(height)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WorkspaceColors.Line)
            )
        } else {
            Column(
                Modifier
                    .width(12.dp)
                    .height(height)
                    .clip(RoundedCornerShape(3.dp)),
            ) {
                values.forEachIndexed { index, value ->
                    if (value > 0L) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(value.toFloat())
                                .background(
                                    SeriesPalette.color(
                                        index,
                                        isDark = false,
                                        isOther = series[index].isOther,
                                    )
                                )
                        )
                    }
                }
            }
        }
        Text(
            text = if (showDate) day.date.dayOfMonth.toString() else "",
            style = MaterialTheme.typography.labelSmall,
            color = WorkspaceColors.Tertiary,
            maxLines = 1,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

private fun valuesForSeries(day: DailyFocusSummary, series: List<FocusSlice>): List<Long> {
    val daily = day.slices.associate { it.label to it.millis }
    val named = series.filterNot { it.isOther }.mapTo(hashSetOf()) { it.label }
    return series.map { slice ->
        if (slice.isOther) daily.filterKeys { it !in named }.values.sum()
        else daily[slice.label] ?: 0L
    }
}

@Composable
private fun InsightHeader(title: String, subtitle: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = WorkspaceColors.Ink,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = WorkspaceColors.Secondary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            trailing,
            style = MaterialTheme.typography.labelLarge,
            color = WorkspaceColors.Secondary,
        )
    }
}
