package com.neurotracee.autoswipe.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.LogBook
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 日志页：本次启动 / 最近一年，两个视角；范围可选到分钟；
 * 支持复制、存成 TXT、按范围清除（两次确认），末尾给出 issue 反馈模板。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDialog(onClose: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    // 0 = 本次启动，1 = 最近一年
    var tab by remember { mutableStateOf(0) }
    var from by remember { mutableStateOf(LogBook.sessionStart) }
    var to by remember { mutableStateOf(System.currentTimeMillis()) }
    var editing by remember { mutableStateOf(0) }          // 0 关；1 改起始；2 改结束
    var askClear by remember { mutableStateOf(0) }         // 0 关；1 第一次确认；2 第二次确认
    var tick by remember { mutableStateOf(0) }             // 内容刷新标记

    val fmt = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }
    val body = remember(tab, from, to, tick) {
        if (tab == 0) LogBook.readSession(ctx) else LogBook.readRange(ctx, from, to)
    }
    val count = remember(tab, from, to, tick) {
        if (tab == 0) LogBook.countRange(ctx, LogBook.sessionStart, Long.MAX_VALUE)
        else LogBook.countRange(ctx, from, to)
    }

    fun tip(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    // 存成 TXT：系统「保存到…」选择器，零权限
    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
        }.isSuccess && body.isNotEmpty()
        tip(if (ok) tr("已保存日志", "Log saved") else tr("保存失败", "Couldn't save the log"))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp)
            ) {
                // 标题
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "‹",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClose() }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        tr("日志与问题反馈", "Logs & feedback"),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onClose) {
                        Text(tr("完成", "Done"), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Column(
                    Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 24.dp)
                ) {
                    // 两个视角
                    Segment(
                        options = listOf(tr("本次启动", "This session"), tr("最近一年", "Last 12 months")),
                        selected = tab,
                        onSelect = { tab = it }
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (tab == 0)
                            tr("只显示这次打开应用之后的记录，共 $count 行。", "$count lines since this launch.")
                        else
                            tr(
                                "日志只保留 365 天，更早的会在启动时自动清掉。当前范围共 $count 行。",
                                "Logs are kept for 365 days; older ones are removed on launch. $count lines in range."
                            ),
                        fontSize = 11.5.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (tab == 1) {
                        Spacer(Modifier.height(14.dp))
                        Text(tr("时间范围（精确到分钟）", "Time range (to the minute)"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TimeChip(fmt.format(Date(from))) { editing = 1 }
                            Text("  →  ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TimeChip(fmt.format(Date(to))) { editing = 2 }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr(
                                "挑到某天的某一分钟，就能对准当时那条报错。",
                                "Pick the exact day and minute to land on the error you saw."
                            ),
                            fontSize = 10.5.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LogAction(tr("复制日志", "Copy"), primary = true) {
                            if (body.isEmpty()) {
                                tip(tr("这个范围里没有日志", "No log lines in this range"))
                            } else {
                                clipboard.setText(AnnotatedString(body))
                                tip(tr("已复制 $count 行", "Copied $count lines"))
                            }
                        }
                        LogAction(tr("保存 TXT", "Save TXT"), primary = false) {
                            if (body.isEmpty()) tip(tr("这个范围里没有日志", "No log lines in this range"))
                            else saver.launch("AutoSwipe-log-" + fmt.format(Date()).replace(" ", "-").replace(":", "") + ".txt")
                        }
                        LogAction(tr("清除…", "Clear…"), primary = false, danger = true) {
                            askClear = 1
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            if (body.isEmpty()) tr("（还没有日志）", "(no log lines yet)")
                            else body,
                            fontSize = 10.sp,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 280.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(12.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    FeedbackBlock(
                        count = count,
                        onCopy = { t ->
                            clipboard.setText(AnnotatedString(t))
                            tip(tr("反馈模板已复制", "Template copied"))
                        }
                    )

                    Spacer(Modifier.height(10.dp))
                    Text(
                        tr("版本 v", "Version v") + BuildConfig.VERSION_NAME,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 时间选择：先选日期，再选时分。editing 1 = 起始，2 = 结束
    if (editing != 0) {
        val isStart = editing == 1
        val anchor = Calendar.getInstance().apply { timeInMillis = if (isStart) from else to }
        var pickingTime by remember(editing) { mutableStateOf(false) }
        var y by remember(editing) { mutableStateOf(anchor.get(Calendar.YEAR)) }
        var m by remember(editing) { mutableStateOf(anchor.get(Calendar.MONTH)) }
        var d by remember(editing) { mutableStateOf(anchor.get(Calendar.DAY_OF_MONTH)) }
        val dateState = rememberDatePickerState(initialSelectedDateMillis = utcMidnight(y, m, d))

        if (!pickingTime) {
            DatePickerDialog(
                onDismissRequest = { editing = 0 },
                confirmButton = {
                    TextButton(onClick = {
                        dateState.selectedDateMillis?.let { ms ->
                            val c = Calendar.getInstance().apply {
                                timeZone = java.util.TimeZone.getTimeZone("UTC")
                                timeInMillis = ms
                            }
                            y = c.get(Calendar.YEAR); m = c.get(Calendar.MONTH); d = c.get(Calendar.DAY_OF_MONTH)
                        }
                        pickingTime = true
                    }) { Text(tr("下一步", "Next")) }
                },
                dismissButton = {
                    TextButton(onClick = { editing = 0 }) { Text(tr("取消", "Cancel")) }
                }
            ) {
                DatePicker(state = dateState, showModeToggle = false)
            }
        } else {
            val timeState = rememberTimePickerState(
                initialHour = anchor.get(Calendar.HOUR_OF_DAY),
                initialMinute = anchor.get(Calendar.MINUTE),
                is24Hour = true
            )
            AlertDialog(
                onDismissRequest = { pickingTime = false },
                title = { Text(if (isStart) tr("起始时刻", "Start time") else tr("结束时刻", "End time")) },
                text = { TimePicker(state = timeState) },
                confirmButton = {
                    TextButton(onClick = {
                        val ms = LogBook.atMinute(y, m, d, timeState.hour, timeState.minute, end = !isStart)
                        if (isStart) from = ms else to = ms
                        if (from > to) { val t = from; from = to; to = t }
                        tick++
                        pickingTime = false
                        editing = 0
                    }) { Text(tr("确定", "OK")) }
                },
                dismissButton = {
                    TextButton(onClick = { pickingTime = false }) { Text(tr("取消", "Cancel")) }
                }
            )
        }
    }

    // 清除：第一次问范围与条数，第二次再确认一次
    if (askClear == 1) {
        val f = if (tab == 0) LogBook.sessionStart else from
        val t = if (tab == 0) System.currentTimeMillis() else to
        val n = LogBook.countRange(ctx, f, t)
        AlertDialog(
            onDismissRequest = { askClear = 0 },
            title = { Text(tr("清除这段日志？", "Clear these logs?")) },
            text = {
                Text(
                    tr(
                        "范围：${fmt.format(Date(f))} → ${fmt.format(Date(t))}\n共 $n 行，删除后无法恢复，也不会备份。",
                        "Range: ${fmt.format(Date(f))} → ${fmt.format(Date(t))}\n$n lines. Deleted logs cannot be restored and are not backed up."
                    ),
                    fontSize = 13.sp, lineHeight = 20.sp
                )
            },
            confirmButton = { TextButton(onClick = { askClear = 2 }) { Text(tr("继续", "Continue")) } },
            dismissButton = { TextButton(onClick = { askClear = 0 }) { Text(tr("取消", "Cancel")) } }
        )
    }

    if (askClear == 2) {
        val f = if (tab == 0) LogBook.sessionStart else from
        val t = if (tab == 0) System.currentTimeMillis() else to
        AlertDialog(
            onDismissRequest = { askClear = 0 },
            title = { Text(tr("再确认一次", "One more confirmation")) },
            text = {
                Text(
                    tr(
                        "这一步之后日志就真的没了。若不是为了清空间，建议先复制或存成 TXT 再删。",
                        "After this tap the logs are gone. Unless you need the space, copy or save a TXT first."
                    ),
                    fontSize = 13.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val removed = LogBook.clearRange(ctx, f, t)
                    LogBook.log("Logs", "手动清除 $removed 行（" + fmt.format(Date(f)) + " → " + fmt.format(Date(t)) + "）")
                    askClear = 0
                    tick++
                    tip(tr("已清除 $removed 行", "Cleared $removed lines"))
                }) { Text(tr("确认删除", "Delete"), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { askClear = 0 }) { Text(tr("取消", "Cancel")) } }
        )
    }

    BackHandler(enabled = true) { onClose() }
}

private fun utcMidnight(y: Int, m: Int, d: Int): Long =
    Calendar.getInstance().apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
        clear(); set(y, m, d, 0, 0, 0)
    }.timeInMillis

@Composable
private fun Segment(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .background(if (on) MaterialTheme.colorScheme.surface else Color.Transparent, RoundedCornerShape(9.dp))
                        .border(if (on) 1.dp else 0.dp, if (on) BrandBlue.copy(alpha = 0.55f) else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeChip(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LogAction(text: String, primary: Boolean, danger: Boolean = false, onClick: () -> Unit) {
    val bg = when {
        danger -> MaterialTheme.colorScheme.error.copy(alpha = 0.10f)
        primary -> BrandBlue.copy(alpha = 0.12f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val fg = when {
        danger -> MaterialTheme.colorScheme.error
        primary -> BrandBlue
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = bg,
        border = if (!primary && !danger) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(text, Modifier.padding(horizontal = 14.dp, vertical = 9.dp), fontSize = 12.5.sp, color = fg)
    }
}

@Composable
private fun FeedbackBlock(count: Int, onCopy: (String) -> Unit) {
    val ctx = LocalContext.current
    val summary = remember { LogBook.summary(ctx) }
    val template = buildString {
        appendLine(tr("版本与设备：$summary", "Version & device: $summary"))
        appendLine()
        appendLine(tr("复现步骤：", "Steps to reproduce:"))
        appendLine(tr("1. 打开……", "1. Open …"))
        appendLine(tr("2. 点开……", "2. Tap …"))
        appendLine(tr("3. 结果……", "3. Result …"))
        appendLine()
        appendLine(tr("实际看到：", "What I saw:"))
        appendLine(tr("预期是：", "What I expected:"))
        appendLine()
        appendLine(tr("日志：粘贴在下面，或作为 TXT 附件上传（共 $count 行）。", "Logs: pasted below, or attach the TXT ($count lines)."))
    }

    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.padding(14.dp)) {
            Text(tr("要反馈问题？照这个写就够", "Filing a bug? This is all it takes"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                tr(
                    "版本号、机型与系统、复现步骤、实际看到与预期，一句话说清发生了什么；日志直接粘贴，或存成 TXT 传上去。",
                    "Version, device and OS, steps, what you saw and expected — one sentence each. Paste the log, or attach the TXT."
                ),
                fontSize = 11.5.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                template,
                fontSize = 10.5.sp, lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            LogAction(tr("复制这份模板", "Copy this template"), primary = true) { onCopy(template) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LogAction("GitHub Issues", primary = false) {
                    if (!openUrlInBrowser(ctx, "$REPO_GITHUB/issues")) Toast.makeText(ctx, REPO_GITHUB, Toast.LENGTH_LONG).show()
                }
                LogAction("Gitee Issues", primary = false) {
                    if (!openUrlInBrowser(ctx, "$REPO_GITEE/issues")) Toast.makeText(ctx, REPO_GITEE, Toast.LENGTH_LONG).show()
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                tr(
                    "两边的 issue 都能提；建议同一个问题只提一处，附一条链接过去就行。",
                    "Either tracker works; please file once and link across rather than duplicating."
                ),
                fontSize = 10.sp, lineHeight = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
