package com.doapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doapp.data.DailyFocusSummary
import com.doapp.data.FocusSession
import com.doapp.data.focusIntensity
import com.doapp.data.formatDurationLong
import com.doapp.data.formatDurationShort
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** The visual hierarchy for Focus: one hero, then evidence, then detail. */
@Composable
internal fun StudyOverviewCard(
    todayMillis: Long,
    weekMillis: Long,
    activeDays: Int,
    sessionCount: Int,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(26.dp)
    val ink = onColor(m.accent)

    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.accent)
            .border(1.dp, m.accent, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(11.dp))
            Column {
                Text(
                    "今日学习",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.copy(alpha = 0.76f),
                )
                Text(
                    formatHeroDuration(todayMillis),
                    style = MaterialTheme.typography.displaySmall,
                    color = ink,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroMetric(
                icon = Icons.Rounded.AutoGraph,
                label = "本周",
                value = formatCompactDuration(weekMillis),
                ink = ink,
                modifier = Modifier.weight(1f),
            )
            HeroMetric(
                icon = Icons.Rounded.CalendarMonth,
                label = "活跃",
                value = "$activeDays 天",
                ink = ink,
                modifier = Modifier.weight(1f),
            )
            HeroMetric(
                icon = Icons.Rounded.CheckCircle,
                label = "完成",
                value = "$sessionCount 次",
                ink = ink,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HeroMetric(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    ink: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.11f))
            .padding(horizontal = 11.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(icon, contentDescription = null, tint = ink.copy(alpha = 0.78f), modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = ink.copy(alpha = 0.68f))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun StudyHeatmapCard(
    days: List<DailyFocusSummary>,
    today: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    onOpenRecords: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(24.dp)
    val selected = days.firstOrNull { it.date == selectedDate }
        ?: DailyFocusSummary(selectedDate, emptyList())

    Column(
        modifier
            .fillMaxWidth()
            .premiumSurface(shape = shape, elevation = 9.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardHeader(
            icon = Icons.Rounded.CalendarMonth,
            title = "学习热力图",
            subtitle = "点按日期，回看那天做了什么",
        )

        if (days.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatMonth(days.first().date),
                    style = MaterialTheme.typography.labelSmall,
                    color = m.tertiaryLabel,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatMonth(minOf(today, days.last().date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = m.tertiaryLabel,
                )
            }
        }

        HeatmapGrid(
            days = days,
            today = today,
            selectedDate = selectedDate,
            onSelectDate = onSelectDate,
        )

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("少", style = MaterialTheme.typography.labelSmall, color = m.tertiaryLabel)
            Spacer(Modifier.width(6.dp))
            repeat(5) { level ->
                Box(
                    Modifier
                        .padding(horizontal = 2.dp)
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(heatColor(level, m.accent, m.isDark))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("多", style = MaterialTheme.typography.labelSmall, color = m.tertiaryLabel)
        }

        HorizontalDivider(color = m.hairline)
        SelectedDayDetail(day = selected, onOpenRecords = onOpenRecords)
    }
}

@Composable
private fun HeatmapGrid(
    days: List<DailyFocusSummary>,
    today: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    val m = materials
    val weeks = remember(days) { days.chunked(7) }
    val gap = 4.dp

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val labelWidth = 18.dp
        val cell = ((maxWidth - labelWidth - gap * 12) / 12).coerceIn(12.dp, 22.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
            Column(
                Modifier.width(labelWidth),
                verticalArrangement = Arrangement.spacedBy(gap),
            ) {
                listOf("一", "", "三", "", "五", "", "日").forEach { label ->
                    Box(Modifier.size(cell), contentAlignment = Alignment.CenterStart) {
                        Text(label, style = MaterialTheme.typography.labelSmall, color = m.tertiaryLabel)
                    }
                }
            }

            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    week.forEach { day ->
                        val future = day.date.isAfter(today)
                        val selected = day.date == selectedDate
                        val description = if (day.totalMillis == 0L) {
                            "${day.date}，没有学习记录"
                        } else {
                            "${day.date}，${formatDurationLong(day.totalMillis)}，${day.sessionCount} 次"
                        }
                        Box(
                            Modifier
                                .size(cell)
                                .semantics { contentDescription = description }
                                .clip(RoundedCornerShape(cell * 0.28f))
                                .background(
                                    if (future) m.hairline.copy(alpha = 0.28f)
                                    else heatColor(focusIntensity(day.totalMillis), m.accent, m.isDark)
                                )
                                .then(
                                    if (selected) Modifier.border(
                                        width = 2.dp,
                                        color = m.label.copy(alpha = 0.72f),
                                        shape = RoundedCornerShape(cell * 0.28f),
                                    ) else Modifier
                                )
                                .clickable(enabled = !future) { onSelectDate(day.date) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetail(day: DailyFocusSummary, onOpenRecords: () -> Unit) {
    val m = materials
    val zone = remember { ZoneId.systemDefault() }
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Column(Modifier.weight(1f)) {
            Text(
                formatSelectedDate(day.date),
                style = MaterialTheme.typography.titleMedium,
                color = m.label,
            )
            Text(
                if (day.sessionCount == 0) "这天还没有学习记录"
                else "${day.sessionCount} 次专注 · ${formatDurationLong(day.totalMillis)}",
                style = MaterialTheme.typography.bodyMedium,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (day.sessionCount > 0) {
            Text(
                "全部记录",
                style = MaterialTheme.typography.labelLarge,
                color = m.accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(9.dp))
                    .clickable(onClick = onOpenRecords)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            )
        }
    }

    if (day.sessions.isNotEmpty()) {
        Column(
            Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            day.sessions.forEachIndexed { index, session ->
                StudyActivityRow(
                    session = session,
                    zone = zone,
                    formatter = formatter,
                    color = SeriesPalette.color(index % SeriesPalette.MAX_SERIES, m.isDark),
                )
            }
        }
    }
}

@Composable
private fun StudyActivityRow(
    session: FocusSession,
    zone: ZoneId,
    formatter: DateTimeFormatter,
    color: Color,
) {
    val m = materials
    val start = Instant.ofEpochMilli(session.startedAt).atZone(zone).format(formatter)
    val end = Instant.ofEpochMilli(session.endedAt).atZone(zone).format(formatter)

    Row(
        Modifier
            .fillMaxWidth()
            .clip(appShape(14.dp))
            .background(m.chrome)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                session.label,
                style = MaterialTheme.typography.bodyLarge,
                color = m.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$start – $end",
                style = MaterialTheme.typography.labelSmall,
                color = m.tertiaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Text(
            formatDurationShort(session.durationMillis),
            style = MaterialTheme.typography.titleMedium,
            color = m.label,
        )
    }
}

@Composable
internal fun SevenDayTrendCard(
    days: List<DailyFocusSummary>,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(24.dp)
    val maxMillis = days.maxOfOrNull { it.totalMillis }?.coerceAtLeast(30L * 60_000L)
        ?: 30L * 60_000L
    val selected = days.firstOrNull { it.date == selectedDate } ?: days.lastOrNull()
    val average = if (days.isEmpty()) 0L else days.sumOf { it.totalMillis } / days.size

    Column(
        modifier
            .fillMaxWidth()
            .premiumSurface(shape = shape, elevation = 9.dp)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DashboardHeader(
            icon = Icons.Rounded.AutoGraph,
            title = "最近 7 天",
            subtitle = "日均 ${formatCompactDuration(average)}",
            trailing = selected?.let { formatCompactDuration(it.totalMillis) },
        )

        Row(
            Modifier.fillMaxWidth().height(126.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                val selectedBar = day.date == selectedDate
                val fraction = (day.totalMillis.toFloat() / maxMillis).coerceIn(0f, 1f)
                TrendBar(
                    day = day,
                    fraction = fraction,
                    selected = selectedBar,
                    onClick = { onSelectDate(day.date) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TrendBar(
    day: DailyFocusSummary,
    fraction: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                Modifier
                    .width(if (selected) 22.dp else 18.dp)
                    .height(barHeight(fraction))
                    .clip(RoundedCornerShape(topStart = 7.dp, topEnd = 7.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(
                        if (day.totalMillis == 0L) m.hairline.copy(alpha = 0.52f)
                        else if (selected) m.accent
                        else m.accent.copy(alpha = 0.46f)
                    )
            )
        }
        Spacer(Modifier.height(7.dp))
        Text(
            weekdayLabel(day.date.dayOfWeek),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) m.label else m.tertiaryLabel,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun DashboardHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: String? = null,
) {
    val m = materials
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        AppIconBadge(icon, contentDescription = null)
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = m.label)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelLarge,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        trailing?.let {
            Text(it, style = MaterialTheme.typography.titleMedium, color = m.label)
        }
    }
}

private fun heatColor(level: Int, accent: Color, dark: Boolean): Color = when (level) {
    1 -> accent.copy(alpha = if (dark) 0.24f else 0.18f)
    2 -> accent.copy(alpha = if (dark) 0.43f else 0.36f)
    3 -> accent.copy(alpha = if (dark) 0.68f else 0.62f)
    4 -> accent.copy(alpha = 0.96f)
    else -> if (dark) Color.White.copy(alpha = 0.065f) else Color.Black.copy(alpha = 0.045f)
}

private fun formatHeroDuration(millis: Long): String {
    val minutes = millis / 60_000L
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> "$hours 小时 $remainder 分"
        hours > 0 -> "$hours 小时"
        else -> "$minutes 分钟"
    }
}

private fun formatCompactDuration(millis: Long): String {
    val minutes = millis / 60_000L
    val hours = minutes / 60
    val remainder = minutes % 60
    return when {
        hours > 0 && remainder > 0 -> "${hours}时${remainder}分"
        hours > 0 -> "${hours}小时"
        else -> "${minutes}分钟"
    }
}

private fun formatMonth(date: LocalDate): String = "${date.monthValue}月"

private fun formatSelectedDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
    return date.format(formatter)
}

private fun weekdayLabel(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "一"
    DayOfWeek.TUESDAY -> "二"
    DayOfWeek.WEDNESDAY -> "三"
    DayOfWeek.THURSDAY -> "四"
    DayOfWeek.FRIDAY -> "五"
    DayOfWeek.SATURDAY -> "六"
    DayOfWeek.SUNDAY -> "日"
}

private fun barHeight(fraction: Float): Dp =
    if (fraction <= 0f) 4.dp else (12f + 76f * fraction).dp
