package com.doapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
internal fun rememberWorkspaceToday(): LocalDate {
    var todayEpochDay by remember { mutableLongStateOf(LocalDate.now().toEpochDay()) }

    LaunchedEffect(todayEpochDay) {
        val now = LocalDateTime.now()
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        delay(Duration.between(now, nextMidnight).toMillis().coerceAtLeast(1_000L))
        todayEpochDay = LocalDate.now().toEpochDay()
    }

    return LocalDate.ofEpochDay(todayEpochDay)
}
