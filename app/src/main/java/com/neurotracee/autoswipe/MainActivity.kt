package com.neurotracee.autoswipe

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.neurotracee.autoswipe.ui.AutoUpdateGate
import com.neurotracee.autoswipe.ui.AutoSwipeTheme
import com.neurotracee.autoswipe.ui.BrandBlue
import com.neurotracee.autoswipe.ui.HomeScreen
import com.neurotracee.autoswipe.ui.ManualUpdateDialog
import com.neurotracee.autoswipe.ui.SettingsDialog
import com.neurotracee.autoswipe.ui.tr

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AutoSwipeTheme {
                AppRoot()
            }
        }
    }
}

/**
 * 首页总装：顶栏、状态卡片、两个权限入口、设置与更新对话框。
 *
 * 从系统设置回来（ON_RESUME）就重新读一次权限状态——用户去开了无障碍 / 悬浮窗，
 * 回来时界面必须立刻反映，而不是让他反复进出应用。
 */
@Composable
private fun AppRoot() {
    val ctx = LocalContext.current
    var showSettings by remember { mutableStateOf(false) }
    var showUpdate by remember { mutableStateOf(false) }
    var accessibilityOn by remember { mutableStateOf(SwipeAccessibilityService.isEnabled(ctx)) }
    var overlayOn by remember { mutableStateOf(SwipeAccessibilityService.canOverlay(ctx)) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityOn = SwipeAccessibilityService.isEnabled(ctx)
                overlayOn = SwipeAccessibilityService.canOverlay(ctx)
                AppState.serviceEnabled = accessibilityOn
                if (!accessibilityOn) {
                    // 开关被关掉了：服务已经断开，界面上的小球状态一并归零
                    AppState.ballVisible = false
                    AppState.running = false
                    AppState.leftSeconds = 0
                }
                if (accessibilityOn && SwipeAccessibilityService.instance != null) {
                    AppState.serviceEnabled = true
                }
                // 进程被系统回收过：小球按上次的开关状态恢复
                if (AppPrefs.ballVisible && !AppState.ballVisible && overlayOn) {
                    BallService.start(ctx)
                }
            }
        }
        lifecycle.addObserver(obs)
        onDispose { lifecycle.removeObserver(obs) }
    }

    fun toast(text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()

    fun openAccessibility() {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast(tr("请到系统设置 → 无障碍 中手动开启「自动翻页」", "Please enable “AutoSwipe” in Settings → Accessibility"))
        }
    }

    fun openOverlay() {
        try {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + ctx.packageName)
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast(tr("请到系统设置 → 应用 → 特殊权限 → 悬浮窗 中允许本应用", "Please allow this app under Settings → Apps → Special access → Display over other apps"))
        }
    }

    fun toggleBall() {
        if (AppState.ballVisible) {
            BallService.stop(ctx)
            AppPrefs.markBallVisible(false)
            toast(tr("小球已隐藏", "Floating ball hidden"))
            return
        }
        if (!SwipeAccessibilityService.canOverlay(ctx)) {
            toast(tr("请先授予悬浮窗权限", "Overlay permission is required"))
            openOverlay()
            return
        }
        if (BallService.start(ctx)) {
            toast(tr("小球已显示：点 ▶ 开始，再点 ⏸ 暂停", "Ball shown: tap ▶ to start, ❚❚ to stop"))
        } else {
            toast(tr("没能显示小球，请检查悬浮窗权限", "Could not show the ball; check the overlay permission"))
            openOverlay()
        }
    }

    fun startAuto() {
        val svc = SwipeAccessibilityService.instance
        if (!SwipeAccessibilityService.isEnabled(ctx) || svc == null) {
            toast(tr("请先开启无障碍服务", "Enable the accessibility service first"))
            openAccessibility()
            return
        }
        svc.start()
        toast(tr("已开始：第一次约 1.5 秒后上滑，之后每 2–30 秒随机一次", "Started: first swipe in ~1.5 s, then every 2–30 s at random"))
    }

    fun stopAuto() {
        SwipeAccessibilityService.instance?.stop()
        toast(tr("已暂停", "Paused"))
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(onSettings = { showSettings = true })
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            HomeScreen(
                accessibilityOn = accessibilityOn,
                overlayOn = overlayOn,
                onOpenAccessibility = { openAccessibility() },
                onOpenOverlay = { openOverlay() },
                onToggleBall = { toggleBall() },
                onStart = { startAuto() },
                onStop = { stopAuto() },
                onOpenRepo = {
                    if (!com.neurotracee.autoswipe.ui.openUrlInBrowser(
                            ctx, "https://github.com/neurotracee/auto-swipe"
                        )
                    ) {
                        toast(tr("没能打开浏览器，请手动访问仓库地址", "Could not open a browser; visit the repository manually"))
                    }
                },
                onOpenGitee = {
                    if (!com.neurotracee.autoswipe.ui.openUrlInBrowser(
                            ctx, "https://gitee.com/bio234/auto-swipe"
                        )
                    ) {
                        toast(tr("没能打开浏览器，请手动访问仓库地址", "Could not open a browser; visit the repository manually"))
                    }
                }
            )
        }
    }

    AutoUpdateGate()

    if (showSettings) {
        SettingsDialog(
            onClose = { showSettings = false },
            onCheckUpdate = {
                showSettings = false
                showUpdate = true
            },
            onToggleBall = { toggleBall() }
        )
    }

    if (showUpdate) {
        ManualUpdateDialog(onClose = { showUpdate = false })
    }
}

@Composable
private fun AppTopBar(onSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                tr("自动翻页", "AutoSwipe"),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(7.dp))
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = BrandBlue.copy(alpha = 0.12f)
            ) {
                Text(
                    tr("2–30 秒随机", "2–30 s random"),
                    Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 10.sp,
                    color = BrandBlue
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.size(46.dp)) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = tr("设置", "Settings"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
