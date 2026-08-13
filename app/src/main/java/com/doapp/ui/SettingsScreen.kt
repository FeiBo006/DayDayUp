package com.doapp.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.doapp.data.Appearance
import com.doapp.data.AppearanceOptions
import com.doapp.data.BackupFile
import com.doapp.notify.BackgroundAccess
import com.doapp.ui.theme.appShape
import com.doapp.ui.theme.materials
import kotlinx.coroutines.delay
import java.time.LocalDate

/** The settings page — wallpaper lives here now, alongside the trash. */
@Composable
fun SettingsScreen(
    appearance: Appearance,
    trashedCount: Int,
    onOpenWallpaper: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenReminderSettings: () -> Unit,
    onExport: (Uri) -> Boolean,
    onImport: (Uri) -> Int?,
    onSetSelfReminder: (String) -> Unit,
    onSelectStyle: (String) -> Unit,
    onSelectFont: (String) -> Unit,
    onSelectTextColor: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val context = LocalContext.current
    val resumes = rememberResumeCount()
    val reminderIssues = remember(resumes) {
        listOf(
            BackgroundAccess.notificationsAllowed(context),
            BackgroundAccess.exactAlarmsAllowed(context),
            BackgroundAccess.batteryUnrestricted(context),
        ).count { !it }
    }

    Box(modifier.fillMaxSize()) {
        WallpaperBackground(appearance)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 18.dp,
                bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp + 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "title") {
                Column(Modifier.padding(bottom = 10.dp)) {
                    Text("设置", style = MaterialTheme.typography.displaySmall, color = m.label)
                    Text(
                        "外观与回收站",
                        style = MaterialTheme.typography.bodyMedium,
                        color = m.secondaryLabel,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            item(key = "self-reminder") {
                SelfReminderCard(
                    value = appearance.selfReminder,
                    onValueChange = onSetSelfReminder,
                )
            }

            item(key = "reminders") {
                SettingRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "提醒与后台",
                    subtitle = if (reminderIssues > 0) "有 $reminderIssues 项未开启，提醒可能延迟"
                    else "通知、精确闹钟与后台限制",
                    onClick = onOpenReminderSettings,
                )
            }

            item(key = "backup") {
                BackupCard(onExport = onExport, onImport = onImport)
            }

            item(key = "wallpaper") {
                SettingRow(
                    icon = Icons.Rounded.Image,
                    title = "壁纸",
                    subtitle = "更换背景、模糊与压暗",
                    onClick = onOpenWallpaper,
                )
            }

            item(key = "trash") {
                SettingRow(
                    icon = Icons.Rounded.DeleteSweep,
                    title = "回收站",
                    subtitle = if (trashedCount > 0) "$trashedCount 条已删除" else "已删除的任务",
                    onClick = onOpenTrash,
                    )
            }

            item(key = "style") {
                SettingChoiceRow(
                    icon = Icons.Rounded.Palette,
                    title = "风格",
                    subtitle = when (appearance.styleId) {
                        AppearanceOptions.STYLE_NEO_BRUTALIST -> "新野兽派"
                        AppearanceOptions.STYLE_DOODLE -> "手绘涂鸦"
                        else -> "柔和材质"
                    },
                    options = listOf(
                        AppearanceOptions.STYLE_SOFT to "柔和",
                        AppearanceOptions.STYLE_NEO_BRUTALIST to "新野兽派",
                        AppearanceOptions.STYLE_DOODLE to "手绘涂鸦",
                    ),
                    selected = appearance.styleId,
                    onSelect = onSelectStyle,
                )
            }

            item(key = "font") {
                SettingChoiceRow(
                    icon = Icons.Rounded.TextFields,
                    title = "字体",
                    subtitle = "应用于标题、按钮、Today、Plan 和 Dock",
                    options = listOf(
                        AppearanceOptions.FONT_SYSTEM to "系统",
                        AppearanceOptions.FONT_SERIF to "衬线",
                        AppearanceOptions.FONT_MONO to "等宽",
                    ),
                    selected = appearance.fontId,
                    onSelect = onSelectFont,
                )
            }

            item(key = "text-color") {
                SettingChoiceRow(
                    icon = Icons.Rounded.Palette,
                    title = "文字颜色",
                    subtitle = "选择高对比文字主色",
                    options = listOf(
                        AppearanceOptions.COLOR_DEFAULT to "默认",
                        AppearanceOptions.COLOR_INK to "墨黑",
                        AppearanceOptions.COLOR_NAVY to "藏蓝",
                        AppearanceOptions.COLOR_FOREST to "森林",
                        AppearanceOptions.COLOR_BURGUNDY to "酒红",
                    ),
                    selected = appearance.textColorId,
                    onSelect = onSelectTextColor,
                )
            }
        }
    }
}

/**
 * Export and import, side by side. The file goes wherever the user picks, which is the whole
 * point — anything inside the app's own storage disappears with the app, and an update that
 * can't install over the old one has to uninstall it first.
 */
@Composable
private fun BackupCard(
    onExport: (Uri) -> Boolean,
    onImport: (Uri) -> Int?,
) {
    val m = materials
    val shape = appShape(18.dp)
    var message by remember { mutableStateOf<String?>(null) }

    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BackupFile.MIME_TYPE)
    ) { uri ->
        message = when {
            uri == null -> null
            onExport(uri) -> "已导出"
            else -> "导出失败，换个位置再试"
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        message = when (val added = onImport(uri)) {
            null -> "读不出来，这个文件可能不是 DayDayUp 的备份"
            0 -> "备份里的任务都已经在了"
            else -> "导入了 $added 条"
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Rounded.SaveAlt,
                contentDescription = null,
                tint = m.accent,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text("备份", style = MaterialTheme.typography.bodyLarge, color = m.label)
                Text(
                    "任务只存在这台手机上，卸载就没了。换机或重装前先导出一份。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.secondaryLabel,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChoiceChip(
                label = "导出",
                selected = false,
                onClick = {
                    message = null
                    exporter.launch("daydayup-${LocalDate.now()}.json")
                },
                modifier = Modifier.weight(1f),
            )
            ChoiceChip(
                label = "导入",
                selected = false,
                onClick = {
                    message = null
                    importer.launch(arrayOf(BackupFile.MIME_TYPE, "*/*"))
                },
                modifier = Modifier.weight(1f),
            )
        }
        message?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = m.secondaryLabel)
        }
    }
}

@Composable
private fun SelfReminderCard(
    value: String,
    onValueChange: (String) -> Unit,
) {
    val m = materials
    val shape = appShape(18.dp)

    // The field owns its text while you type. Routing every keystroke through the store meant the
    // store's trim() ate the space you had just typed, right out from under the cursor — and it
    // rewrote SharedPreferences once per key. Commit once the typing settles instead.
    var draft by remember { mutableStateOf(value) }
    LaunchedEffect(draft) {
        if (draft == value) return@LaunchedEffect
        delay(400)
        onValueChange(draft)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(m.card)
            .styleBorder(shape, m.topEdge, width = if (m.isNeoBrutalist) 3.dp else 1.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.EditNote, contentDescription = null, tint = m.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text("监督语", style = MaterialTheme.typography.bodyLarge, color = m.label)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .clip(appShape(14.dp))
                .background(if (m.isDoodle) m.cardPressed else m.chrome.copy(alpha = 0.42f))
                .styleBorder(appShape(14.dp), m.topEdge)
                .heightIn(min = 94.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            if (draft.isBlank()) {
                Text(
                    "写一句提醒自己坚持下去的话…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = m.tertiaryLabel,
                )
            }
            BasicTextField(
                value = draft,
                onValueChange = { draft = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = m.label,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(m.accent),
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Done,
                ),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SettingChoiceRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = m.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, color = m.label)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = m.secondaryLabel)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (id, label) ->
                ChoiceChip(
                    label = label,
                    selected = selected == id,
                    onClick = { onSelect(id) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(10.dp)
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier
            .clip(shape)
            .background(if (selected) m.accent else m.chrome.copy(alpha = if (m.isNeoBrutalist) 1f else 0.40f))
            .styleBorder(shape, if (m.isNeoBrutalist) Color.Black else m.topEdge, width = if (m.isNeoBrutalist) 2.dp else 1.dp)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = when {
                selected -> onColor(m.accent)
                m.isNeoBrutalist -> Color.Black
                else -> m.label
            },
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val m = materials
    val shape = appShape(18.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = Motion.Snappy,
        label = "settingPress",
    )

    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (m.isNeoBrutalist) Modifier
                    .shadow(8.dp, shape, spotColor = Color.Black)
                else Modifier.styleShadow(
                    shape = shape,
                    elevation = 7.dp,
                    ambientColor = Color.Black.copy(alpha = 0.3f),
                    spotColor = Color.Black.copy(alpha = 0.3f),
                )
            )
            .clip(shape)
            .background(if (pressed) m.cardPressed else m.card)
            .styleBorder(shape, m.topEdge)
            .pressableNoRipple(interactionSource, onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(m.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = m.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = m.label)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = m.secondaryLabel,
                modifier = Modifier.padding(top = 1.dp),
            )
        }
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = m.tertiaryLabel,
            modifier = Modifier.size(18.dp),
        )
    }
}
