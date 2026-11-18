package com.neurotracee.autoswipe.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.AppPrefs
import com.neurotracee.autoswipe.ReleaseDigest
import com.neurotracee.autoswipe.UpdateApk
import com.neurotracee.autoswipe.Updater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

/** 网络慢到什么程度才值得弹一个「正在检查」——快的时候连提示都不闪 */
private const val SLOW_HINT_MS = 1_200L

/** 自动检查的总预算：超了就当成网络不通，静默放行 */
private const val AUTO_BUDGET_MS = 20_000L

/**
 * 启动时的自动检查更新。与「设置 → 检查更新」共用 [Updater]，
 * 断网 / 超时 / 已是最新都不打扰，只有真发现新版本才弹窗。
 */
@Composable
fun AutoUpdateGate() {
    var checking by remember { mutableStateOf(false) }
    var skipped by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<Updater.Result?>(null) }

    LaunchedEffect(Unit) {
        if (!AppPrefs.autoCheck) return@LaunchedEffect
        val job = async(Dispatchers.IO) {
            Updater.check(BuildConfig.VERSION_NAME, AppPrefs.updateSource)
        }

        var r = withTimeoutOrNull(SLOW_HINT_MS) { job.await() }
        if (r == null) {
            checking = true
            r = withTimeoutOrNull(AUTO_BUDGET_MS - SLOW_HINT_MS) { job.await() }
            checking = false
        }

        val res = r ?: return@LaunchedEffect
        if (!res.ok || !res.hasUpdate) return@LaunchedEffect
        if (skipped) return@LaunchedEffect
        result = res
    }

    if (checking && !skipped && result == null) {
        UpdateShell(
            title = "正在检查更新",
            accent = BrandBlue,
            busy = true,
            onClose = { skipped = true }
        ) {
            Text(
                "正在向更新源读一次版本号，不传任何文件。\n网络慢的话可以直接关掉，不影响使用。",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    result?.let { r ->
        UpdateResultDialog(r, onClose = { result = null })
    }
}

/** 设置里手动点「检查更新」时使用：先显示进度，再按结果展示 */
@Composable
fun ManualUpdateDialog(onClose: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var result by remember { mutableStateOf<Updater.Result?>(null) }

    LaunchedEffect(Unit) {
        result = withContext(Dispatchers.IO) {
            Updater.check(BuildConfig.VERSION_NAME, AppPrefs.updateSource)
        }
        loading = false
    }

    if (loading) {
        UpdateShell(title = "正在检查更新", accent = BrandBlue, busy = true, onClose = onClose) {
            Text(
                "正在读取发布列表，只读版本号，不发送任何本机内容。",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val r = result
    when {
        r == null -> UpdateShell(
            title = "检查更新没成功",
            accent = DangerRed,
            onClose = onClose
        ) {
            Text(
                "没能拿到结果，请检查网络后重试。",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        !r.ok -> UpdateShell(
            title = "连不上下游",
            accent = DangerRed,
            onClose = onClose
        ) {
            Text(
                "更新源这次没有回应，可能是网络、代理或安全软件拦住了。\n${r.detail}",
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        !r.hasUpdate -> UpdateShell(
            title = "已是最新版本 v${r.latest}",
            accent = SuccessGreen,
            onClose = onClose
        ) {
            Text(
                "当前版本 v${r.current}，不需要更新。",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            SourceNote(r)
        }
        else -> UpdateResultDialog(r, onClose = onClose)
    }
}

/** 发现新版本时的窗口。两档都只是「建议」：永远留着关闭，只在真关之前劝一次 */
@Composable
private fun UpdateResultDialog(r: Updater.Result, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val strong = r.strong
    val accent = if (strong) DangerRed else BrandBlue
    val dig = r.digest?.pick(false)
    val hasDigest = dig != null && (dig.brief.isNotBlank() || dig.items.isNotEmpty())
    var pleading by remember(r) { mutableStateOf(false) }
    val tryClose: () -> Unit = { if (pleading) onClose() else pleading = true }

    Dialog(
        onDismissRequest = tryClose,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(
                Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.84f).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when {
                            pleading && strong -> "再看一眼，好吗？"
                            pleading -> "确定先不更新吗？"
                            strong -> "强烈建议更新到 v" + r.latest
                            else -> "发现新版本 v" + r.latest
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pleading) MaterialTheme.colorScheme.onSurface else accent,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = tryClose) {
                        Text(if (pleading) "关闭" else "以后再说", fontSize = 12.5.sp)
                    }
                }

                if (!pleading) {
                    Spacer(Modifier.height(6.dp))
                    Surface(shape = RoundedCornerShape(999.dp), color = accent.copy(alpha = 0.12f)) {
                        Text(
                            if (strong) "强烈建议更新" else "建议更新",
                            Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            fontSize = 11.sp,
                            color = accent
                        )
                    }
                }

                Spacer(Modifier.height(9.dp))
                SourceNote(r)
                Spacer(Modifier.height(4.dp))
                Text(
                    when {
                        pleading && strong ->
                            "这个版本修的是一上手就会碰到的问题——旧版里那些反复出错、只能一遍遍重来的地方，" +
                                "新版已经处理好了。花一分钟换上，往后能省下不少时间。\n\n" +
                                "当然，换不换还是你说了算：点「我还是要用旧版」就能继续用，下次打开只会再提醒你一次。"
                        pleading ->
                            "不更新也能照常用，只是新版顺手修了几个小毛病，用起来更稳。\n\n" +
                                "下次打开还会再提醒一次，到时候想换也来得及。"
                        hasDigest ->
                            "当前是 v" + r.current + "。这一版的关键改动就是下面这几条，完整说明在发布页。"
                        strong ->
                            "当前是 v" + r.current + "。这一版修了一直在影响使用的问题，强烈建议尽快换上——" +
                                "更新只要一分钟，原来的设置和用法都不变。"
                        else ->
                            "当前是 v" + r.current + "。新版修了几个已知问题，用起来更稳——" +
                                "不更新也能继续用，但新版更省心。"
                    },
                    fontSize = 12.5.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!pleading) {
                    Spacer(Modifier.height(10.dp))
                    UpdateNotesBlock(r = r, accent = accent, onOpenPage = { openUrlInBrowser(ctx, r.page) })

                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            UpdateActionBlock(
                                r = r,
                                accent = accent,
                                onOpenBrowser = { openUrlInBrowser(ctx, r.url) },
                                onCopyLink = { copyLink(ctx, r.url) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = if (pleading) onClose else tryClose) {
                        Text(
                            when {
                                pleading && strong -> "我还是要用旧版"
                                pleading -> "先这样，下次再说"
                                else -> "暂不更新"
                            },
                            fontSize = 13.sp
                        )
                    }
                    if (pleading) {
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = { pleading = false },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text("好，现在更新", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

/** 通用外壳：一条标题 + 一段内容 + 一个关闭按钮 */
@Composable
private fun UpdateShell(
    title: String,
    accent: Color,
    busy: Boolean = false,
    onClose: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                        modifier = Modifier.weight(1f)
                    )
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                content()
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onClose,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(if (busy) "跳过，先进入" else "知道了", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/**
 * 这次结果是从哪个通道读到的。发生自动切换时据实说明一句，
 * 免得用户以为「我明明默认 Gitee，怎么跑去 GitHub 了」。
 */
@Composable
private fun SourceNote(r: Updater.Result) {
    if (r.channel.isBlank()) return
    val text = if (r.fallbackFrom.isNotBlank()) {
        r.fallbackFrom + " 这次没连上，已经自动改用 " + r.channel + " 了。"
    } else {
        "本次从 " + r.channel + " 读取。"
    }
    Text(
        text,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    )
}

/**
 * 「立即更新」这一块：应用内下载 + 交给系统安装器。
 * 启动自动检查与设置里的手动检查共用，保证两处行为一字不差。
 */
@Composable
private fun UpdateActionBlock(
    r: Updater.Result,
    accent: Color,
    onOpenBrowser: () -> Unit,
    onCopyLink: () -> Unit,
    onDone: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var downloading by remember(r) { mutableStateOf(false) }
    var percent by remember(r) { mutableStateOf(0) }
    var needPerm by remember(r) { mutableStateOf(false) }
    var started by remember(r) { mutableStateOf(false) }
    var error by remember(r) { mutableStateOf("") }
    var apk by remember(r) { mutableStateOf<File?>(null) }

    fun begin() {
        downloading = true
        percent = 0
        error = ""
        needPerm = false
        scope.launch {
            val f = UpdateApk.fetchAndInstall(
                ctx = ctx,
                url = r.url,
                version = r.latest
            ) { done, total ->
                percent = if (total > 0) ((done * 100) / total).toInt().coerceIn(0, 100) else 0
            }
            apk = f.apk
            downloading = false
            when (val o = f.outcome) {
                is UpdateApk.Outcome.Started -> {
                    started = true
                    onDone()
                }
                is UpdateApk.Outcome.NeedPermission -> needPerm = true
                is UpdateApk.Outcome.Failed -> error = o.reason
            }
        }
    }

    fun resume() {
        val f = apk ?: run { begin(); return }
        when (val o = UpdateApk.retryInstall(ctx, f)) {
            is UpdateApk.Outcome.Started -> {
                started = true
                needPerm = false
                onDone()
            }
            is UpdateApk.Outcome.NeedPermission -> needPerm = true
            is UpdateApk.Outcome.Failed -> error = o.reason
        }
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            // 用户去系统设置开了「安装应用」再回来：不用再点一次
            if (event == Lifecycle.Event.ON_RESUME && needPerm && UpdateApk.canInstall(ctx)) {
                needPerm = false
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            when {
                downloading -> "正在下载 v" + r.latest + "… $percent%（不用离开这个页面，下完直接弹出安装）"
                needPerm -> "安装包已经下好了，但系统还没允许本应用「安装应用」。点「继续安装」会带你去开那个开关，开完回来再点一次就好。"
                started -> "已经打开系统安装界面。装完覆盖旧版即可，不用先卸载。"
                error.isNotBlank() -> error
                else -> "点「立即更新」直接在这里下载并安装，不用经过浏览器，也不用自己去文件夹里找文件。"
            },
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = if (error.isNotBlank() && !downloading) DangerRed
            else MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (downloading) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { percent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = accent
            )
        }

        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            Button(
                enabled = !downloading,
                onClick = {
                    when {
                        needPerm && UpdateApk.canInstall(ctx) -> resume()
                        needPerm -> {
                            UpdateApk.openInstallPermission(ctx)
                            Toast.makeText(
                                ctx,
                                "打开开关后回到这里点「继续安装」",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        apk != null -> resume()
                        else -> begin()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) {
                Text(
                    when {
                        downloading -> "下载中 $percent%"
                        needPerm -> "继续安装"
                        apk != null -> "继续安装"
                        error.isNotBlank() -> "重试"
                        else -> "立即更新"
                    },
                    fontSize = 12.5.sp
                )
            }
            TextButton(enabled = !downloading, onClick = onOpenBrowser) {
                Text("用浏览器下载", fontSize = 12.sp)
            }
            TextButton(onClick = onCopyLink) {
                Text("复制链接", fontSize = 12.sp)
            }
        }

        if (error.isNotBlank() || started) {
            Text(
                "如果改用浏览器下载：Gitee 有时会把文件存成「…apk.zip」，把结尾的 .zip 删掉就能正常安装。",
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * 更新弹窗里那两段文字：顶上一行 100 字以内的摘要，
 * 下面按平台列出详细条目，末尾一条「具体详见发布页」。
 */
@Composable
private fun UpdateNotesBlock(
    r: Updater.Result,
    accent: Color,
    onOpenPage: () -> Unit
) {
    val notes = r.digest?.pick(false) ?: return
    if (notes.brief.isBlank() && notes.items.isEmpty()) return

    if (notes.brief.isNotBlank()) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = accent.copy(alpha = 0.07f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 11.dp, vertical = 9.dp)) {
                Text(
                    notes.brief,
                    fontSize = 12.5.sp,
                    lineHeight = 19.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (notes.items.isNotEmpty()) {
        Spacer(Modifier.height(4.dp))
        notes.items.forEachIndexed { i, n ->
            if (i > 0) {
                Surface(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                ) {}
            }
            Column(Modifier.padding(vertical = 7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlatformTag(n.platform)
                    Text(
                        n.title,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                if (n.lines.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        n.lines.joinToString("\n"),
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalArrangement = Arrangement.End
    ) {
        TextButton(onClick = onOpenPage) {
            Text("具体详见发布页 →", fontSize = 11.5.sp)
        }
    }
}

/** 平台小标签，颜色只用来区分平台，不做好坏暗示 */
@Composable
private fun PlatformTag(platform: String) {
    val (label, fg, bg) = when (platform) {
        "win" -> Triple("Windows", Color(0xFF2B5BD0), Color(0xFFEEF4FF))
        "android" -> Triple("安卓", Color(0xFF1A7F45), Color(0xFFEAF7EE))
        else -> Triple("两端", Color(0xFF6B3FC4), Color(0xFFF3EEFE))
    }
    Surface(shape = RoundedCornerShape(999.dp), color = bg) {
        Text(
            label,
            Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = fg
        )
    }
}

internal fun openUrlInBrowser(ctx: Context, url: String): Boolean = try {
    ctx.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (e: Exception) {
    copyLink(ctx, url)
    false
}

private fun copyLink(ctx: Context, url: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText("releases", url))
    Toast.makeText(ctx, "链接已复制，粘到浏览器里打开", Toast.LENGTH_SHORT).show()
}

/** 首页/关于页复用的仓库地址 */
internal const val REPO_URL = "https://github.com/neurotracee/auto-swipe"

/** 预览/示例数据 */
internal fun sampleResultForNotes(): Updater.Result = Updater.Result(
    ok = true,
    current = "1.0.0.0",
    latest = "1.0.0.1",
    hasUpdate = true,
    strong = false,
    digest = ReleaseDigest.of(
        "更新级别：建议\n\n" +
            "> **一句话**：翻页节奏更接近手指，悬浮球位置不再重置。\n\n" +
            "## 1. 修复：个别机型上悬浮球会回到默认位置（安卓）\n\n" +
            "拖动后保存的坐标现在会正确恢复。\n"
    )
)
