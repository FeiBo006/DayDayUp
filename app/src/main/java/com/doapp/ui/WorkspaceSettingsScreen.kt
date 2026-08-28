package com.doapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doapp.data.BackupFile
import com.doapp.notify.BackgroundAccess
import kotlinx.coroutines.launch

@Composable
fun WorkspaceSettingsScreen(
    trashedCount: Int,
    onOpenWallpaper: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onExport: suspend (Uri) -> Boolean,
    onImport: suspend (Uri) -> Int?,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resumes = rememberResumeCount()
    val reminderIssues = remember(resumes) {
        listOf(
            BackgroundAccess.notificationsAllowed(context),
            BackgroundAccess.exactAlarmsAllowed(context),
            BackgroundAccess.batteryUnrestricted(context),
        ).count { !it }
    }

    WorkspacePage(modifier) { layout ->
        WorkspaceHeader(
            eyebrow = "DayDayUp / Settings",
            title = "设置",
            subtitle = "只保留真正会用到的选项",
        )

        if (layout.usesWideColumns) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.weight(1f)) {
                    ApplicationSettingsPanel(reminderIssues, trashedCount, onOpenReminderSettings, onOpenWallpaper, onOpenTrash)
                }
                Box(Modifier.weight(1f)) { WorkspaceBackupPanel(onExport, onImport) }
            }
        } else {
            ApplicationSettingsPanel(reminderIssues, trashedCount, onOpenReminderSettings, onOpenWallpaper, onOpenTrash)
            WorkspaceBackupPanel(onExport, onImport)
        }
    }
}

@Composable
private fun ApplicationSettingsPanel(
    reminderIssues: Int,
    trashedCount: Int,
    onOpenReminderSettings: () -> Unit,
    onOpenWallpaper: () -> Unit,
    onOpenTrash: () -> Unit,
) {
    WorkspacePanel {
        Text("应用", color = WorkspaceColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        WorkspaceRow(
            title = "提醒与后台",
            subtitle = if (reminderIssues == 0) "权限状态正常" else "$reminderIssues 项需要处理",
            icon = Icons.Rounded.NotificationsNone,
            onClick = onOpenReminderSettings,
            trailing = { Chevron() },
        )
        WorkspaceDivider()
        WorkspaceRow(
            title = "壁纸",
            subtitle = "背景、照片和显示强度",
            icon = Icons.Rounded.Image,
            onClick = onOpenWallpaper,
            trailing = { Chevron() },
        )
        WorkspaceDivider()
        WorkspaceRow(
            title = "回收站",
            subtitle = if (trashedCount == 0) "没有已删除任务" else "$trashedCount 条已删除任务",
            icon = Icons.Rounded.DeleteOutline,
            onClick = onOpenTrash,
            trailing = { Chevron() },
        )
    }
}

@Composable
private fun WorkspaceBackupPanel(
    onExport: suspend (Uri) -> Boolean,
    onImport: suspend (Uri) -> Int?,
) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFile.MIME_TYPE),
    ) { uri ->
        if (uri != null) scope.launch {
            message = "正在导出…"
            message = if (onExport(uri)) "导出完成" else "导出失败"
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            message = "正在导入…"
            message = onImport(uri)?.let { "已导入 $it 条记录" } ?: "导入失败"
        }
    }

    WorkspacePanel {
        Text("备份", color = WorkspaceColors.Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "数据由你保存，不绑定账号。",
            color = WorkspaceColors.Secondary,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 2.dp, bottom = 12.dp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WorkspaceActionButton(
                label = "导出",
                icon = Icons.Rounded.ArrowUpward,
                onClick = { exporter.launch("daydayup-backup.json") },
                modifier = Modifier.weight(1f),
            )
            WorkspaceActionButton(
                label = "导入",
                icon = Icons.Rounded.ArrowDownward,
                onClick = { importer.launch(arrayOf(BackupFile.MIME_TYPE, "application/json", "text/plain")) },
                modifier = Modifier.weight(1f),
                color = WorkspaceColors.Settings,
            )
        }
        message?.let {
            Text(it, color = WorkspaceColors.Secondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 10.dp))
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = WorkspaceColors.Tertiary,
        modifier = Modifier.size(20.dp),
    )
}
