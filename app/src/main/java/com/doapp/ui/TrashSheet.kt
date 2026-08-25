package com.doapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doapp.data.Task
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay

/** The trash: deleted tasks wait here until restored or purged for good. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashSheet(
    trashed: List<Task>,
    onRestore: (Task) -> Unit,
    onPurge: (Task) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val m = materials
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sorted = trashed.sortedByDescending { it.deletedAt ?: 0L }
    var confirmClear by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            now = System.currentTimeMillis()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        sheetMaxWidth = 640.dp,
        containerColor = m.card,
        dragHandle = { TrashHandle() },
    ) {
        Column(
            Modifier
                // Nothing bounds how many tasks land in here, and a plain Column would just clip
                // the ones past the bottom edge with no way to reach them.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(
                    bottom = WindowInsets.navigationBars.asPaddingValues()
                        .calculateBottomPadding() + 20.dp
                ),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "回收站",
                    style = MaterialTheme.typography.headlineSmall,
                    color = m.label,
                    modifier = Modifier.weight(1f),
                )
                if (sorted.isNotEmpty()) {
                    ClearTrashButton(onClick = { confirmClear = true })
                }
            }

            if (sorted.isEmpty()) {
                Text(
                    "回收站是空的",
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.secondaryLabel,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sorted.forEach { task ->
                            TrashRow(
                                task = task,
                                now = now,
                                onRestore = { onRestore(task) },
                                onPurge = { onPurge(task) },
                            )
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空回收站？") },
            text = { Text("回收站里的任务将被永久删除，无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearAll()
                }) { Text("清空", color = m.destructive) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TrashRow(
    task: Task,
    now: Long,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
) {
    val m = materials
    val shape = appShape(16.dp)
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
                task.title,
                style = MaterialTheme.typography.bodyLarge,
                color = m.secondaryLabel,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (task.note.isNotBlank()) {
                Text(
                    task.note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.tertiaryLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
            task.deletedAt?.let { deletedAt ->
                val days = trashDaysRemaining(deletedAt, now)
                Text(
                    text = "剩余 $days 天",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (days <= 1) m.destructive else m.tertiaryLabel,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        TrashActionButton(
            icon = Icons.Rounded.Restore,
            tint = m.accent,
            bg = m.accent.copy(alpha = 0.12f),
            onClick = onRestore,
        )
        Spacer(Modifier.width(6.dp))
        TrashActionButton(
            icon = Icons.Rounded.DeleteForever,
            tint = m.destructive,
            bg = m.destructive.copy(alpha = 0.12f),
            onClick = onPurge,
        )
    }
}

@Composable
private fun TrashActionButton(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(38.dp)
            .clip(appShape(12.dp))
            .background(bg)
            .pressableNoRipple(interactionSource, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ClearTrashButton(onClick: () -> Unit) {
    val m = materials
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        Modifier
            .clip(appShape(12.dp))
            .background(m.destructive.copy(alpha = 0.12f))
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text("清空", style = MaterialTheme.typography.titleMedium, color = m.destructive)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrashHandle() {
    val m = materials
    Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
        Box(
                Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(appShape(3.dp))
                    .background(m.tertiaryLabel)
        )
    }
}
