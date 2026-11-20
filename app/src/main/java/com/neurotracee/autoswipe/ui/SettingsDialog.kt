package com.neurotracee.autoswipe.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neurotracee.autoswipe.AppPrefs
import com.neurotracee.autoswipe.AppState
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.R
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 设置：两级页面。
 *
 * 一级是分组列表（语言 / 外观 / 悬浮球 / 息屏 / 更新 / 版本），
 * 二级是「关于与开源信息」。
 *
 * 这里每一项都真有作用，不是摆设：
 * - 语言：改 [AppPrefs.lang]，整页连同首页、更新弹窗立刻换文字；
 * - 外观：改 [AppPrefs.themeMode]，跟随系统 / 浅色 / 深色即时生效；
 * - 悬浮球：真的把球挂上或收掉；
 * - 息屏：改 [AppPrefs.pauseOnScreenOff]，服务按它决定黑屏时停不停；
 * - 检查方式与更新源：改 [AppPrefs.autoCheck] / [AppPrefs.updateSource]，更新器读这两个值；
 * - 版本行：当场发起一次检查。
 */
@Composable
fun SettingsDialog(
    onClose: () -> Unit,
    onCheckUpdate: () -> Unit,
    onToggleBall: () -> Unit
) {
    var page by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                if (page == 0) {
                    SettingsHeader(
                        title = tr("设置", "Settings"),
                        done = tr("完成", "Done"),
                        onBack = null,
                        onDone = onClose
                    )
                    RootPage(
                        onCheckUpdate = onCheckUpdate,
                        onToggleBall = onToggleBall,
                        onOpenAbout = { page = 1 }
                    )
                } else {
                    SettingsHeader(
                        title = tr("关于与开源信息", "About & open source"),
                        done = tr("完成", "Done"),
                        onBack = { page = 0 },
                        onDone = onClose
                    )
                    AboutPage()
                }
            }
        }
    }

    BackHandler(enabled = page == 1) { page = 0 }
}

@Composable
private fun SettingsHeader(
    title: String,
    done: String,
    onBack: (() -> Unit)?,
    onDone: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Text(
                "‹",
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable { onBack() }
                    .padding(end = 8.dp, top = 0.dp, bottom = 4.dp)
            )
        }
        Text(
            title,
            fontSize = 21.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onDone) {
            Text(done, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------- 一级页

@Composable
private fun RootPage(
    onCheckUpdate: () -> Unit,
    onToggleBall: () -> Unit,
    onOpenAbout: () -> Unit
) {
    var lang by remember { mutableStateOf(AppPrefs.lang) }
    var theme by remember { mutableStateOf(AppPrefs.themeMode) }
    var autoCheck by remember { mutableStateOf(AppPrefs.autoCheck) }
    var source by remember { mutableStateOf(AppPrefs.updateSource) }
    var pauseOff by remember { mutableStateOf(AppPrefs.pauseOnScreenOff) }
    val ballVisible = AppState.ballVisible

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        SettingLabel(tr("界面语言", "Language"))
        Segmented(
            options = listOf(
                tr("中文", "中文"),
                "English"
            ),
            selected = if (lang == "en") 1 else 0,
            onSelect = {
                lang = if (it == 1) "en" else "zh"
                AppPrefs.updateLang(lang)
            }
        )
        Hint(
            tr(
                "切换后立即生效，不用重启；更新弹窗、首页文案也会一起换。",
                "Applies immediately, no restart; the home screen and update dialogs switch too."
            )
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("外观", "Appearance"))
        Segmented(
            options = listOf(
                tr("跟随系统", "System"),
                tr("浅色", "Light"),
                tr("深色", "Dark")
            ),
            selected = when (theme) {
                "light" -> 1
                "dark" -> 2
                else -> 0
            },
            onSelect = {
                theme = when (it) {
                    1 -> "light"
                    2 -> "dark"
                    else -> "system"
                }
                AppPrefs.updateTheme(theme)
            }
        )
        Hint(
            tr(
                "选「跟随系统」时，会随手机的深色模式自动切换。",
                "With “System”, the app follows the phone's dark mode automatically."
            )
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("悬浮球", "Floating ball"))
        ToggleRow(
            title = tr("显示悬浮球", "Show floating ball"),
            desc = tr(
                "只依赖悬浮窗权限，不需要先开无障碍；关掉时正在跑的循环会一起停。",
                "Needs only the overlay permission, not accessibility. Turning it off also stops the loop."
            ),
            checked = ballVisible,
            onChecked = { onToggleBall() }
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("运行状态", "While running"))
        ToggleRow(
            title = tr("息屏时自动暂停", "Pause when the screen turns off"),
            desc = tr(
                "黑屏时滑也没意义，还会白耗电。关掉后息屏也继续按节奏滑。",
                "Swiping with the screen off is pointless and drains battery. Turn off to keep going."
            ),
            checked = pauseOff,
            onChecked = {
                pauseOff = it
                AppPrefs.updatePauseOnScreenOff(it)
            }
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("更新检查方式", "Update check"))
        Segmented(
            options = listOf(
                tr("自动检查（启动时）", "Auto (on launch)"),
                tr("手动检查", "Manual")
            ),
            selected = if (autoCheck) 0 else 1,
            onSelect = {
                autoCheck = it == 0
                AppPrefs.updateAutoCheck(autoCheck)
            }
        )
        Hint(
            tr(
                "自动：每次启动在后台静默查一次，没网就不响。手动：只有点下面的「检查更新」才联网。",
                "Auto: one silent check per launch, skipped when offline. Manual: network only when you tap “Check for updates”."
            )
        )

        Spacer(Modifier.height(18.dp))
        SettingLabel(tr("更新源", "Update source"))
        Spacer(Modifier.height(8.dp))
        SourceRow(
            title = tr("Gitee（建议中国大陆用户）", "Gitee (recommended in mainland China)"),
            selected = (source == "auto" && AppPrefs.resolvedUpdateSource == "gitee") || source == "gitee",
            onClick = {
                source = "gitee"
                AppPrefs.updateUpdateSource("gitee")
            }
        )
        Spacer(Modifier.height(8.dp))
        SourceRow(
            title = tr("GitHub（海外用户）", "GitHub (outside China)"),
            selected = (source == "auto" && AppPrefs.resolvedUpdateSource == "github") || source == "github",
            onClick = {
                source = "github"
                AppPrefs.updateUpdateSource("github")
            }
        )
        Hint(
            tr(
                "没得选的时候默认就是 Gitee。只有系统语言不是中文、时区也不在东八区（明显在海外）才默认改用 GitHub；" +
                    "环境判断不出来一律按 Gitee。选中的源连不上会自动换另一个再试，换的时候界面上会说明。",
                "Default is Gitee. GitHub is only picked when the system language is not Chinese and the time zone is " +
                    "outside UTC+8; anything unclear falls back to Gitee. If the chosen source fails, the other one is " +
                    "tried and the dialog says so."
            )
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("版本与更新", "Version"))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    tr("当前版本", "Current version") + " v" + BuildConfig.VERSION_NAME,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    tr(
                        "查到的更新来自上面的更新源；装新版直接覆盖，不用卸载。",
                        "Updates come from the source above; new builds install over the old one."
                    ),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onCheckUpdate) {
                Text(tr("检查更新", "Check"), fontSize = 13.5.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                .clickable { onOpenAbout() }
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                tr("关于与开源信息", "About & open source"),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text("›", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---------------------------------------------------------------- 二级页

@Composable
private fun AboutPage() {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current

    fun fail(url: String) {
        clipboard.setText(AnnotatedString(url))
        Toast.makeText(
            ctx,
            tr("没能打开浏览器，链接已复制：$url", "Couldn't open a browser; link copied: $url"),
            Toast.LENGTH_LONG
        ).show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        SettingLabel(tr("这个应用是什么", "What this app is"))
        Text(
            tr(
                "一个只做一件事的小工具：按 2–30 秒的随机间隔，用无障碍手势替你从下往上滑一次；" +
                    "悬浮球点一下开始、再点一下停。它不读屏幕内容、不模拟点击按钮、不上传任何东西，" +
                    "唯一的联网是检查更新。",
                "One small tool that does one thing: every 2–30 seconds at random it performs a swipe-up " +
                    "gesture for you. Tap the floating ball to start, tap again to stop. It does not read the " +
                    "screen, does not tap buttons, and uploads nothing; the only network use is the update check."
            ),
            fontSize = 12.5.sp,
            lineHeight = 20.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("署名", "Credits"))
        CreditLine(tr("指导作者", "Advisor"), tr("项目创作时为初中生", "Was a junior high school student when this project was created"))
        CreditLine(tr("开发", "Developed by"), "豆包工作任务 × DeepSeek-V4.1-Flash")
        CreditLine(tr("模型", "Main model"), "DeepSeek-V4.1-Flash")

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("开源协议", "License"))
        Text(
            "GNU AGPL-3.0-or-later",
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Hint(
            tr(
                "允许商用与二次开发，但必须保留上面的署名、在程序里标注原项目地址；改过的版本要注明「非原作者发布」。" +
                    "另附 AGPL-3.0 第 7 条 (b) 项附加条款，详见仓库里的 ADDITIONAL-TERMS.md。",
                "Commercial use and forks are allowed, but the credits above must stay, the original project URL " +
                    "must be visible in the app, and modified builds must say they are modified. Additional terms " +
                    "under AGPL-3.0 §7(b) are in ADDITIONAL-TERMS.md."
            )
        )

        Spacer(Modifier.height(12.dp))
        Text(
            tr("开源仓库", "Repositories"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinkRow("GitHub", REPO_GITHUB) { if (!openUrlInBrowser(ctx, REPO_GITHUB)) fail(REPO_GITHUB) }
        LinkRow(tr("Gitee（国内镜像）", "Gitee (China mirror)"), REPO_GITEE) {
            if (!openUrlInBrowser(ctx, REPO_GITEE)) fail(REPO_GITEE)
        }

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("权限", "Permissions"))
        Hint(
            tr(
                "无障碍服务：只用来画一条向上的滑动路径，不读屏幕、不点按钮。\n" +
                    "悬浮窗：只为把小球画在别的应用上面，不截屏、不录屏。\n" +
                    "网络：只在检查更新时读公开版本号；下载安装包只从 Gitee / GitHub 的官方发布地址。",
                "Accessibility: only draws an upward swipe path; it never reads the screen or taps buttons.\n" +
                    "Overlay: only draws the ball above other apps; no screenshots, no screen recording.\n" +
                    "Network: reads a public version number during update checks only; downloads come only from " +
                    "official Gitee / GitHub release URLs."
            )
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))

        SettingLabel(tr("第三方组件", "Third-party components"))
        Hint(
            tr(
                "AndroidX、Jetpack Compose、Kotlin 标准库，均为 Apache License 2.0。" +
                    "没有广告、统计、崩溃上报 SDK，也没有原生库。完整清单见 NOTICE.md。",
                "AndroidX, Jetpack Compose and the Kotlin standard library, all Apache License 2.0. " +
                    "No ads, analytics or crash-reporting SDKs, and no native libraries. Full list in NOTICE.md."
            )
        )

        Spacer(Modifier.height(18.dp))
        DividerLine()
        Spacer(Modifier.height(14.dp))
        ThanksBlock()

        Spacer(Modifier.height(16.dp))
        Text(
            tr("版本 v", "Version v") + BuildConfig.VERSION_NAME,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 赞赏码：白底本身是一张图，深浅色都放进同一张圆角白卡里，像一张贴纸 */
@Composable
private fun ThanksBlock() {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    fun tip(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    // 8.0 / 9.0 没有相册写入权限，改用系统「保存到…」选择器，全程零权限
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        tip(
            if (ThanksQr.writeTo(ctx, uri)) tr("已保存", "Saved")
            else tr("保存失败，可以截屏后用微信提取文字", "Couldn't save — try a screenshot instead")
        )
    }

    fun saveQr() {
        if (saving) return
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (!ThanksQr.canSaveToGallery()) {
            picker.launch(ThanksQr.FILE_NAME)
            return
        }
        saving = true
        scope.launch {
            val msg = try {
                val path = withContext(Dispatchers.IO) { ThanksQr.saveToGallery(ctx) }
                tr("已保存到相册：$path", "Saved to gallery: $path")
            } catch (e: Exception) {
                tr("保存失败，可以截屏后用微信提取文字", "Couldn't save — try a screenshot instead")
            }
            saving = false
            tip(msg)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        SettingLabel(tr("如果这个应用帮到了你", "If this app helped you"))
        Text(
            tr(
                "本来就是顺手做的小工具，能派上用场就挺好。",
                "It began as a small side project, and I'm glad it's useful."
            ),
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // ---- 点 Star：不打赏也能帮上忙的那条路 ----
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    tr("给仓库点个 Star", "Star the repository"),
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    tr(
                        "不想花钱也没关系，点一颗 Star 就是最好的支持——别人搜得到，我才更有动力维护。" +
                            "两个仓库任选一个就行：GitHub 是主仓，Gitee 是国内镜像，访问快一些，代码完全一样。",
                        "No money needed — a star is the best support: it helps others find the project, " +
                            "and it keeps me motivated. Either repository works: GitHub is the main one, " +
                            "Gitee is the China mirror and loads faster; the code is identical."
                    ),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tr(
                        "要是两个都用得上，两边各点一颗最省事也最管用：GitHub 和 Gitee 的 Star 各算各的，" +
                            "谁也替不了谁。国内的人多半只在 Gitee 上搜，海外的人只翻 GitHub——" +
                            "你在两个平台都留一颗，这工具才更容易被下一个需要它的人找到。",
                        "If you use both, one star on each is the best help you can give: GitHub and Gitee " +
                            "count their stars separately, so neither stands in for the other. People in China " +
                            "mostly search Gitee; people elsewhere only look at GitHub — a star on both is what " +
                            "makes this easy for the next person who needs it to find."
                    ),
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        if (!openUrlInBrowser(ctx, REPO_GITHUB)) {
                            clipboard.setText(AnnotatedString(REPO_GITHUB))
                            Toast.makeText(
                                ctx,
                                tr("没能打开浏览器，链接已复制：$REPO_GITHUB", "No browser; link copied: $REPO_GITHUB"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Text(tr("去 GitHub 点 Star", "Star on GitHub"), fontSize = 12.5.sp)
                    }
                    TextButton(onClick = {
                        if (!openUrlInBrowser(ctx, REPO_GITEE)) {
                            clipboard.setText(AnnotatedString(REPO_GITEE))
                            Toast.makeText(
                                ctx,
                                tr("没能打开浏览器，链接已复制：$REPO_GITEE", "No browser; link copied: $REPO_GITEE"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Text(tr("去 Gitee 点 Star", "Star on Gitee"), fontSize = 12.5.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            tr(
                "国内用户：愿意的话可以请我喝杯奶茶，完全自愿，不打赏也一样免费、功能不缺。",
                "In China? You can buy me a milk tea if you feel like it — entirely optional; the app stays " +
                    "free and fully functional either way."
            ),
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .padding(vertical = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { saveQr() })
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.thanks_qr_only),
                contentDescription = tr("赞赏码", "Tip QR code"),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .width(302.dp)
                    .aspectRatio(950f / 505f)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            tr(
                "“如果有帮助，可以请我喝杯奶茶”",
                "“If it helped, a cup of milk tea is welcome.”"
            ),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            tr(
                "微信扫码 · 金额随意 · 长按图片可保存",
                "Scan in WeChat · any amount · long-press to save"
            ),
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            TextButton(enabled = !saving, onClick = { saveQr() }) {
                Text(
                    if (saving) tr("保存中…", "Saving…") else tr("保存赞赏码", "Save the QR"),
                    fontSize = 12.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------- 组件

@Composable
private fun SettingLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun Hint(text: String) {
    Spacer(Modifier.height(8.dp))
    Text(
        text,
        fontSize = 10.5.sp,
        lineHeight = 16.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
    )
}

@Composable
private fun Segmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(4.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Box(
                    Modifier
                        .weight(1f)
                        .background(
                            if (on) MaterialTheme.colorScheme.surface else Color.Transparent,
                            RoundedCornerShape(9.dp)
                        )
                        .border(
                            width = if (on) 1.dp else 0.dp,
                            color = if (on) BrandBlue.copy(alpha = 0.55f) else Color.Transparent,
                            shape = RoundedCornerShape(9.dp)
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (on) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
            Text(
                desc,
                fontSize = 10.5.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedTrackColor = BrandBlue)
        )
    }
}

@Composable
private fun SourceRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                if (selected) BrandBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                shape
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) BrandBlue else Color.Transparent,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(18.dp)
                .border(
                    1.5.dp,
                    if (selected) BrandBlue else MaterialTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    Modifier
                        .size(8.dp)
                        .background(BrandBlue, CircleShape)
                )
            }
        }
        Spacer(Modifier.width(11.dp))
        Text(
            title,
            fontSize = 13.5.sp,
            color = if (selected) BrandBlue else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CreditLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        Text(
            value,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LinkRow(label: String, url: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() }
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            url,
            fontSize = 10.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DividerLine() {
    Surface(
        Modifier
            .fillMaxWidth()
            .height(1.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    ) {}
}


