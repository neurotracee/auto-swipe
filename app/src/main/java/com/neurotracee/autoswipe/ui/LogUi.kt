package com.neurotracee.autoswipe.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.LogBook
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * 调色板：把主题色收在一处，卡片、字段、按钮都从这儿取。
 * 亮色是浅灰底 + 白卡，暗色自动走同一套语义，不做额外的绿色底。
 */
private data class Palette(
    val bg: Color,
    val card: Color,
    val paper: Color,
    val track: Color,
    val line: Color,
    val ink: Color,
    val sub: Color,
    val fair: Color,
    val down: Color
)

@Composable
private fun palette(): Palette {
    val c = MaterialTheme.colorScheme
    return Palette(
        bg = c.background,
        card = c.surface,
        paper = c.surfaceVariant.copy(alpha = 0.55f),
        track = c.surfaceVariant,
        line = c.outline.copy(alpha = 0.55f),
        ink = c.onSurface,
        sub = c.onSurfaceVariant,
        fair = c.primary,
        down = c.error
    )
}

private val DAY_FMT: String = "yyyy-MM-dd"

/**
 * 日志页：本次启动 / 按日期查历史，复制、存 TXT、按区间清除，末尾是反馈模板。
 *
 * 排版走卡片：范围一张卡、结果一张卡、反馈一张卡，正文最多三行说明，
 * 其余信息都收进小字或胶囊，避免堆成一版工程味的长文字。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogDialog(onClose: () -> Unit) {
    val p = palette()
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val fmt = remember { SimpleDateFormat(DAY_FMT, Locale.US) }
    val fmtFull = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US) }

    var tab by remember { mutableStateOf(0) }                       // 0 本次启动 · 1 历史日志
    var session by remember { mutableStateOf(LogBook.readSession(ctx)) }
    var fromText by remember { mutableStateOf(fmt.format(Date())) }
    var toText by remember { mutableStateOf(fmt.format(Date())) }
    var fromTime by remember { mutableStateOf("") }
    var toTime by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf("") }
    var previewCount by remember { mutableStateOf(0) }
    var days by remember { mutableStateOf(LogBook.recentDays(ctx, 30)) }
    var confirmStage by remember { mutableStateOf(0) }
    var confirmText by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf(0 to 0L) }
    var timeTarget by remember { mutableStateOf(-1) }   // -1 关 · 0 起始时间 · 1 结束时间
    var dateTarget by remember { mutableStateOf(-1) }   // -1 关 · 0 起始日期 · 1 结束日期

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    fun validTime(s: String): Boolean =
        s.isBlank() || Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(s.trim())

    /** 日期 + 时间（空 = 整天）折算成毫秒区间 */
    fun rangeMs(): Pair<Long, Long>? {
        val f = runCatching { fmt.parse(fromText.trim()) }.getOrNull()
        val t = runCatching { fmt.parse(toText.trim()) }.getOrNull()
        if (f == null || t == null) {
            toast(tr("日期格式要写成 2026-11-20", "Dates must look like 2026-11-20"))
            return null
        }
        if (t.before(f)) {
            toast(tr("结束日期不能早于起始日期", "The end date is before the start date"))
            return null
        }
        if (!validTime(fromTime) || !validTime(toTime)) {
            toast(tr("时间要写成 09:30 这种 24 小时制", "Times must use 24-hour HH:mm"))
            return null
        }
        fun edge(base: Long, s: String, end: Boolean): Long {
            if (s.isBlank()) return if (end) base + 86_399_999L else base
            val parts = s.trim().split(":")
            val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
            return base + (h * 60L + m) * 60_000L + if (end) 59_999L else 0L
        }
        return edge(f.time, fromTime, false) to edge(t.time, toTime, true)
    }

    fun reload() {
        val r = rangeMs() ?: return
        scope.launch {
            preview = LogBook.readRange(ctx, r.first, r.second)
            previewCount = LogBook.countRange(ctx, r.first, r.second)
            if (preview.isBlank()) toast(tr("这段时间没有日志", "No logs in that range"))
        }
    }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val body = if (tab == 0) session else preview
        val ok = runCatching {
            ctx.contentResolver.openOutputStream(uri)?.use { it.write(body.toByteArray()) }
        }.isSuccess && body.isNotBlank()
        toast(if (ok) tr("已保存为 TXT", "Saved as TXT") else tr("保存失败，换个目录试试", "Save failed — try another folder"))
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        androidx.compose.material3.Surface(
            color = p.bg,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(top = 14.dp, bottom = 28.dp)
                ) {
                    // 顶部：标题 + 一句说明 + 完成
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                tr("应用日志", "App logs"),
                                fontSize = 21.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = p.ink
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                tr("最近 365 天 · 只在本机", "Last 365 days · stays on this device"),
                                fontSize = 11.sp,
                                color = p.sub
                            )
                        }
                        Text(
                            tr("完成", "Done"),
                            fontSize = 14.sp,
                            color = p.fair,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable { onClose() }.padding(8.dp)
                        )
                    }

                    Spacer(Modifier.height(14.dp))
                    Segmented(
                        p,
                        listOf(tr("本次启动", "This launch"), tr("历史日志", "History")),
                        tab
                    ) { i ->
                        tab = i
                        if (i == 1 && preview.isBlank()) reload()
                    }

                    Spacer(Modifier.height(12.dp))
                    if (tab == 0) {
                        SheetCard(p) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("本次启动", "This launch"), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
                                Spacer(Modifier.weight(1f))
                                MetaText(p, tr("${lineCount(session)} 行", "${lineCount(session)} lines"))
                            }
                            Spacer(Modifier.height(10.dp))
                            LogBox(
                                p,
                                session.ifBlank { tr("这次启动还没有日志", "No log lines yet") },
                                Modifier.fillMaxWidth().heightIn(min = 200.dp, max = 380.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillButton(p, tr("复制", "Copy"), primary = true) {
                                    if (session.isBlank()) toast(tr("没有可复制的内容", "Nothing to copy"))
                                    else {
                                        clipboard.setText(AnnotatedString(session))
                                        toast(tr("日志已复制", "Log copied"))
                                    }
                                }
                                PillButton(p, tr("存为 TXT", "Save TXT")) {
                                    if (session.isBlank()) toast(tr("没有可保存的内容", "Nothing to save"))
                                    else saver.launch("AutoSwipe-log-" + fmt.format(Date()) + ".txt")
                                }
                                PillButton(p, tr("刷新", "Refresh")) { session = LogBook.readSession(ctx) }
                            }
                        }
                    } else {
                        // 时间范围：日期、时间都是点一下弹选择器
                        SheetCard(p) {
                            SectionHead(p, tr("时间范围", "Time range"), tr("点一下就能选", "Tap to pick"))
                            Spacer(Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EdgeLabel(p, tr("起", "From"))
                                Column(Modifier.weight(1f)) {
                                    Text(tr("日期", "Date"), fontSize = 11.sp, color = p.sub)
                                    Spacer(Modifier.height(4.dp))
                                    DateChip(p, fromText, Modifier.fillMaxWidth()) { dateTarget = 0 }
                                }
                                TimeField(
                                    p, tr("时间", "Time"), fromTime, Modifier.weight(0.85f),
                                    onPick = { timeTarget = 0 }, onClear = { fromTime = "" }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                EdgeLabel(p, tr("止", "To"))
                                Column(Modifier.weight(1f)) {
                                    Text(tr("日期", "Date"), fontSize = 11.sp, color = p.sub)
                                    Spacer(Modifier.height(4.dp))
                                    DateChip(p, toText, Modifier.fillMaxWidth()) { dateTarget = 1 }
                                }
                                TimeField(
                                    p, tr("时间", "Time"), toTime, Modifier.weight(0.85f),
                                    onPick = { timeTarget = 1 }, onClear = { toTime = "" }
                                )
                            }

                            if (days.isNotEmpty()) {
                                Spacer(Modifier.height(14.dp))
                                Text(tr("有日志的日期", "Days with logs"), fontSize = 10.5.sp, color = p.sub)
                                Spacer(Modifier.height(7.dp))
                                Row(
                                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    days.take(30).forEach { d ->
                                        val s = fmt.format(Date(d))
                                        MiniChip(p, s.substring(5), selected = s == fromText && s == toText) {
                                            fromText = s
                                            toText = s
                                            fromTime = ""
                                            toTime = ""
                                            reload()
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(14.dp))
                            PillButton(p, tr("查询", "Search"), primary = true, fill = true) { reload() }
                        }

                        Spacer(Modifier.height(10.dp))
                        SheetCard(p) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(tr("查询结果", "Result"), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
                                Spacer(Modifier.weight(1f))
                                val kb = preview.toByteArray().size / 1024
                                MetaText(
                                    p,
                                    if (preview.isBlank()) tr("未查询", "Not queried")
                                    else tr("$previewCount 行 · $kb KB", "$previewCount lines · $kb KB")
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                            LogBox(
                                p,
                                preview.ifBlank { tr("选好范围后点「查询」", "Pick a range, then tap Search") },
                                Modifier.fillMaxWidth().heightIn(min = 170.dp, max = 320.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PillButton(p, tr("复制", "Copy"), primary = true) {
                                    if (preview.isBlank()) toast(tr("先查一下再复制", "Search first"))
                                    else {
                                        clipboard.setText(AnnotatedString(preview))
                                        toast(tr("已复制 $previewCount 行", "Copied $previewCount lines"))
                                    }
                                }
                                PillButton(p, tr("存为 TXT", "Save TXT")) {
                                    if (preview.isBlank()) toast(tr("先查一下再保存", "Search first"))
                                    else saver.launch("AutoSwipe-log-" + fmt.format(Date()) + ".txt")
                                }
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            tr("清除这段日期的日志", "Delete logs in this range"),
                            fontSize = 12.5.sp,
                            color = p.down,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val r = rangeMs() ?: return@clickable
                                    stats = LogBook.countRange(ctx, r.first, r.second) to
                                        LogBook.readRange(ctx, r.first, r.second).toByteArray().size.toLong()
                                    confirmText = fmtFull.format(Date(r.first)) + " → " + fmtFull.format(Date(r.second))
                                    confirmStage = 1
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                    FeedbackCard(p, ctx, clipboard, { toast(it) })
                }
            }
        }
    }

    // 日期选择器
    if (dateTarget >= 0) {
        val seedMs = runCatching { fmt.parse(if (dateTarget == 0) fromText.trim() else toText.trim()) }
            .getOrNull()?.let { utcMidnightOf(it) }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = seedMs)
        DatePickerDialog(
            onDismissRequest = { dateTarget = -1 },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        val c = Calendar.getInstance().apply {
                            timeZone = java.util.TimeZone.getTimeZone("UTC")
                            timeInMillis = ms
                        }
                        val s = fmt.format(c.time)
                        if (dateTarget == 0) fromText = s else toText = s
                    }
                    dateTarget = -1
                }) { Text(tr("确定", "OK"), color = p.fair) }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = -1 }) { Text(tr("取消", "Cancel"), color = p.sub) }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }

    // 时间滚轮
    if (timeTarget >= 0) {
        TimePickerDialog(
            title = if (timeTarget == 0) tr("起始时间", "From time") else tr("结束时间", "To time"),
            initial = if (timeTarget == 0) fromTime else toTime,
            p = p,
            onDismiss = { timeTarget = -1 },
            onConfirm = { v ->
                if (timeTarget == 0) fromTime = v else toTime = v
                timeTarget = -1
            }
        )
    }

    // 删除两步确认
    if (confirmStage == 1) {
        AlertDialog(
            onDismissRequest = { confirmStage = 0 },
            title = { Text(tr("删除前的确认", "Before deleting")) },
            text = {
                Text(
                    tr(
                        "将要删除 $confirmText 的日志，共 ${stats.first} 行、约 ${stats.second / 1024} KB。\n删除后无法恢复。",
                        "This will delete logs for $confirmText — ${stats.first} lines, about ${stats.second / 1024} KB.\nDeleted logs cannot be recovered."
                    ),
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmStage = 2 }) { Text(tr("继续", "Continue"), color = p.down) }
            },
            dismissButton = { TextButton(onClick = { confirmStage = 0 }) { Text(tr("取消", "Cancel")) } }
        )
    }
    if (confirmStage == 2) {
        AlertDialog(
            onDismissRequest = { confirmStage = 0 },
            title = { Text(tr("再确认一次", "Confirm once more")) },
            text = {
                Text(
                    tr("确定永久删除 $confirmText 这些日志吗？这一步不能撤销。",
                        "Permanently delete the logs for $confirmText? This cannot be undone."),
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val r = rangeMs()
                    if (r != null) {
                        val n = LogBook.clearRange(ctx, r.first, r.second)
                        LogBook.log("Logs", "手动清除 $n 行（$confirmText）")
                        toast(tr("已删除 $n 行", "Deleted $n lines"))
                    }
                    confirmStage = 0
                    preview = ""
                    previewCount = 0
                    days = LogBook.recentDays(ctx, 30)
                    session = LogBook.readSession(ctx)
                    reload()
                }) { Text(tr("永久删除", "Delete"), color = p.down) }
            },
            dismissButton = { TextButton(onClick = { confirmStage = 0 }) { Text(tr("取消", "Cancel")) } }
        )
    }

    BackHandler(enabled = true) { onClose() }
}

/** 把某天 0 点（本地时区）折算成 DatePicker 需要的 UTC 0 点 */
private fun utcMidnightOf(localDay: Date): Long {
    val c = Calendar.getInstance().apply { time = localDay }
    return Calendar.getInstance().apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
        clear()
        set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
    }.timeInMillis
}

// ------------------------------------------------------------ 卡片与小组件

@Composable
private fun SheetCard(
    p: Palette,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(p.card)
            .border(1.dp, p.line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun SectionHead(p: Palette, title: String, hint: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
        Spacer(Modifier.weight(1f))
        Text(hint, fontSize = 10.5.sp, color = p.sub)
    }
}

@Composable
private fun MetaText(p: Palette, text: String) {
    Text(text, fontSize = 10.5.sp, color = p.sub)
}

@Composable
private fun Segmented(p: Palette, items: List<String>, index: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(p.track)
            .padding(4.dp)
    ) {
        items.forEachIndexed { i, s ->
            val active = i == index
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (active) p.card else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    s,
                    fontSize = 12.5.sp,
                    color = if (active) p.ink else p.sub,
                    fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun PillButton(
    p: Palette,
    text: String,
    primary: Boolean = false,
    fill: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(11.dp)
    val base = if (fill) Modifier.fillMaxWidth() else Modifier
    Box(
        base
            .clip(shape)
            .background(if (primary) p.fair else p.card)
            .border(1.dp, if (primary) p.fair else p.line, shape)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.5.sp, color = if (primary) Color.White else p.ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EdgeLabel(p: Palette, text: String) {
    Text(text, fontSize = 10.5.sp, color = p.sub, modifier = Modifier.width(14.dp))
}

@Composable
private fun DateChip(p: Palette, value: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(11.dp)
    Box(
        modifier
            .clip(shape)
            .background(p.paper)
            .border(1.dp, p.line, shape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 11.dp)
    ) {
        Text(value, fontSize = 13.sp, color = p.ink)
    }
}

@Composable
private fun MiniChip(p: Palette, text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        Modifier
            .clip(shape)
            .background(if (selected) p.fair else p.card)
            .border(1.dp, if (selected) p.fair else p.line, shape)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 7.dp)
    ) {
        Text(text, fontSize = 11.5.sp, color = if (selected) Color.White else p.ink)
    }
}

@Composable
private fun LogBox(p: Palette, text: String, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(p.paper)
            .border(1.dp, p.line, RoundedCornerShape(12.dp))
    ) {
        Column(Modifier.padding(12.dp).verticalScroll(rememberScrollState())) {
            Text(
                text,
                fontSize = 10.5.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.Monospace,
                color = p.ink
            )
        }
    }
}

private fun lineCount(text: String): Int = text.lineSequence().count { it.isNotBlank() }

/** 反馈卡：默认只露一行说明，模板折叠，想看再展开。 */
@Composable
private fun FeedbackCard(
    p: Palette,
    ctx: android.content.Context,
    clipboard: androidx.compose.ui.platform.ClipboardManager,
    toast: (String) -> Unit
) {
    val summary = remember { LogBook.summary(ctx) }
    val template = tr(
        """版本号：v${BuildConfig.VERSION_NAME}
机型系统：$summary
问题一句话：

复现步骤：
1. 打开 App……
2. 点开……
3. 结果……

实际看到：
预期结果：

日志：已随本 Issue 附上 TXT（日志页「存为 TXT」导出）""",
        """Version: v${BuildConfig.VERSION_NAME}
Device: $summary
In one sentence:

Steps to reproduce:
1. Open the app …
2. Tap …
3. Result …

What I saw:
What I expected:

Log: attach the TXT exported from the log screen"""
    )
    var expanded by remember { mutableStateOf(false) }

    SheetCard(p) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tr("有问题要反馈？", "Reporting a bug?"), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = p.ink)
                Spacer(Modifier.height(2.dp))
                Text(tr("版本号与机型已自动填好", "Version and device are pre-filled"), fontSize = 11.sp, color = p.sub)
            }
            Text(
                if (expanded) tr("收起", "Hide") else tr("看模板", "Template"),
                fontSize = 11.5.sp, color = p.fair, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { expanded = !expanded }.padding(6.dp)
            )
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(p.paper)
                    .border(1.dp, p.line, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(template, fontSize = 10.5.sp, lineHeight = 15.sp, fontFamily = FontFamily.Monospace, color = p.ink)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton(p, tr("复制模板", "Copy template"), primary = true) {
                clipboard.setText(AnnotatedString(template))
                toast(tr("模板已复制，记得把日志一起附上", "Template copied — attach the log as well"))
            }
            PillButton(p, "GitHub") {
                if (!openUrlInBrowser(ctx, "$REPO_GITHUB/issues")) toast(REPO_GITHUB)
            }
            PillButton(p, "Gitee") {
                if (!openUrlInBrowser(ctx, "$REPO_GITEE/issues")) toast(REPO_GITEE)
            }
        }
    }
}

// ------------------------------------------------------------ 时间选择：双滚轮

private val WHEEL_HOURS = (0..23).toList()
private val WHEEL_MINUTES = (0..59).toList()

/** 时间字段：空白显示「整天」，点开滚轮；右侧 × 清空回整天。 */
@Composable
private fun TimeField(
    p: Palette,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onPick: () -> Unit,
    onClear: () -> Unit
) {
    Column(modifier) {
        Text(label, fontSize = 11.sp, color = p.sub)
        Spacer(Modifier.height(4.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(p.paper)
                .border(1.dp, p.line, RoundedCornerShape(10.dp))
                .clickable { onPick() }
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (value.isBlank()) tr("整天", "Whole day") else value,
                fontSize = 13.sp,
                color = if (value.isBlank()) p.sub else p.ink,
                modifier = Modifier.weight(1f)
            )
            if (value.isNotBlank()) {
                Text("×", fontSize = 15.sp, color = p.sub,
                    modifier = Modifier.clickable { onClear() }.padding(4.dp))
            } else {
                Text("▾", fontSize = 12.sp, color = p.sub)
            }
        }
    }
}

/** 时分选择窗：左轮是时、右轮是分，右下角「确定」。 */
@Composable
private fun TimePickerDialog(
    title: String,
    initial: String?,
    p: Palette,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val seed = remember(initial) {
        val parts = initial?.trim()?.split(":")
        if (parts != null && parts.size == 2) {
            val h = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            if (h != null && m != null && h in 0..23 && m in 0..59) h to m else null
        } else null
    }
    var hour by remember(initial) { mutableStateOf(seed?.first ?: 9) }
    var minute by remember(initial) { mutableStateOf(seed?.second ?: 0) }
    // 分默认锁着：时没被动过之前是灰的、推不动；已经带着旧值进来时直接可用
    var minuteEnabled by remember(initial) { mutableStateOf(initial != null && initial.isNotBlank() && seed != null) }
    var inputTarget by remember { mutableStateOf(-1) }
    var inputText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = p.ink) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TimeWheel(
                        p = p,
                        values = WHEEL_HOURS,
                        selected = hour,
                        enabled = true,
                        onSelect = {
                            hour = it
                            if (!minuteEnabled) minuteEnabled = true
                        },
                        onClickInput = { inputTarget = 0; inputText = two(hour) },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = p.ink)
                    TimeWheel(
                        p = p,
                        values = WHEEL_MINUTES,
                        selected = minute,
                        enabled = minuteEnabled,
                        onSelect = { minute = it },
                        onClickInput = { inputTarget = 1; inputText = two(minute) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (minuteEnabled) tr("上下推动滚轮；轻点滚轮可以打字输入", "Drag the wheels up or down; tap one to type a value")
                    else tr("先推一下「时」，分的滚轮就会解锁", "Push the hour wheel first to unlock minutes"),
                    fontSize = 11.sp, color = p.sub, lineHeight = 16.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(two(hour) + ":" + two(minute)) }) {
                Text(tr("确定", "OK"), color = p.fair, fontWeight = FontWeight.Medium)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("取消", "Cancel"), color = p.sub) }
        }
    )

    // 轻点滚轮后的小输入框
    if (inputTarget >= 0) {
        val max = if (inputTarget == 0) 23 else 59
        val ok = inputText.toIntOrNull()?.let { it in 0..max } == true
        AlertDialog(
            onDismissRequest = { inputTarget = -1 },
            title = { Text(if (inputTarget == 0) tr("输入小时", "Type the hour") else tr("输入分钟", "Type the minute"), fontSize = 15.sp) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { s -> inputText = s.filter { it.isDigit() }.take(2) },
                    singleLine = true,
                    label = { Text(if (inputTarget == 0) "0–23" else "0–59", fontSize = 11.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = inputText.isNotEmpty() && !ok,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(enabled = ok, onClick = {
                    val v = inputText.toInt()
                    if (inputTarget == 0) {
                        hour = v
                        if (!minuteEnabled) minuteEnabled = true
                    } else {
                        minute = v
                    }
                    inputTarget = -1
                }) { Text(tr("确定", "OK"), color = if (ok) p.fair else p.sub) }
            },
            dismissButton = { TextButton(onClick = { inputTarget = -1 }) { Text(tr("取消", "Cancel")) } }
        )
    }
}

private fun two(v: Int): String = if (v < 10) "0$v" else v.toString()

/**
 * 滚轮：手指上下推、每过一格就更新值；松手后 180ms 吸附到最近一格。
 * 中间一格同字号、加粗，外加一圈高亮框；上下相邻的字透明度递减。
 */
@Composable
private fun TimeWheel(
    p: Palette,
    values: List<Int>,
    selected: Int,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
    onClickInput: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemH = 44.dp
    val boxH = itemH * 5
    val itemPx = with(LocalDensity.current) { itemH.toPx() }
    val scope = rememberCoroutineScope()
    var pos by remember { mutableStateOf(selected.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    var settling by remember { mutableStateOf(false) }

    LaunchedEffect(selected) {
        if (!dragging && !settling) pos = selected.toFloat()
    }

    fun settle() {
        dragging = false
        scope.launch {
            settling = true
            val from = pos
            val target = from.roundToInt().coerceIn(0, values.lastIndex)
            animate(0f, 1f, animationSpec = tween(180, easing = FastOutSlowInEasing)) { t, _ ->
                pos = from + (target - from) * t
            }
            pos = target.toFloat()
            if (target != selected) onSelect(target)
            settling = false
        }
    }

    Box(
        modifier
            .height(boxH)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) p.paper else p.track)
            .border(1.dp, p.line, RoundedCornerShape(14.dp))
            .clipToBounds()
            .pointerInput(enabled, values.size) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures(
                    onDragStart = { dragging = true },
                    onDragEnd = { settle() },
                    onDragCancel = { settle() },
                    onVerticalDrag = { change, delta ->
                        change.consume()
                        val next = (pos - delta / itemPx)
                            .coerceIn(-0.55f, values.lastIndex.toFloat() + 0.55f)
                        pos = next
                        val idx = next.roundToInt().coerceIn(0, values.lastIndex)
                        if (idx != selected) onSelect(idx)
                    }
                )
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { onClickInput() }
            }
    ) {
        // 中间选中格
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemH)
                .padding(horizontal = 8.dp)
                .clip(RoundedCornerShape(10.dp))
                .border(1.5.dp, if (enabled) p.fair else p.line, RoundedCornerShape(10.dp))
        )

        values.forEach { v ->
            val dist = abs(v - pos)
            if (dist <= 2.6f) {
                Text(
                    two(v),
                    fontSize = 20.sp,
                    fontWeight = if (dist < 0.5f) FontWeight.Bold else FontWeight.Normal,
                    color = if (enabled) {
                        p.ink.copy(alpha = (1f - dist * 0.26f).coerceIn(0.25f, 1f))
                    } else {
                        p.sub.copy(alpha = (0.45f - dist * 0.12f).coerceAtLeast(0.08f))
                    },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(0, ((v - pos) * itemPx).roundToInt()) }
                )
            }
        }
    }
}
