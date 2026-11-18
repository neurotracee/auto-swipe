package com.neurotracee.autoswipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neurotracee.autoswipe.AppState
import com.neurotracee.autoswipe.BuildConfig
import com.neurotracee.autoswipe.SwipeAccessibilityService

/**
 * 首页：一眼看到「现在在不在翻」，下面两步把权限开齐，再往下是它到底会做什么。
 * 状态全部来自 [AppState]，服务那边一改，这里立刻重组。
 *
 * 文案都走 [tr]，所以设置里切成 English 后这一页会立刻跟着换，不用重启。
 */
@Composable
fun HomeScreen(
    accessibilityOn: Boolean,
    overlayOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onOpenOverlay: () -> Unit,
    onToggleBall: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenRepo: () -> Unit
) {
    val running = AppState.running
    val count = AppState.swipeCount
    val left = AppState.leftSeconds
    val lastGap = AppState.lastGapSeconds

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        StatusCard(
            running = running,
            accessibilityOn = accessibilityOn,
            count = count,
            left = left,
            lastGap = lastGap,
            ballVisible = AppState.ballVisible,
            onStart = onStart,
            onStop = onStop,
            onToggleBall = onToggleBall
        )

        Spacer(Modifier.height(14.dp))
        SectionLabel(tr("开两个开关就能用", "Two switches and you're set"))

        Spacer(Modifier.height(8.dp))
        StepCard(
            index = "1",
            title = tr("无障碍服务", "Accessibility service"),
            desc = tr(
                "唯一能代替手指上滑的开关。只画一条向上的滑动路径，不读屏幕内容、不点按钮。\n" +
                    "如果这个开关是灰的、点不动：先到「应用信息 → 右上角菜单 → 允许受限设置」，再回来开启。",
                "The only switch that can swipe for your finger. It draws one upward path — no screen reading, " +
                    "no button taps.\nIf the switch is grey and won't move: go to App info → menu → “Allow restricted " +
                    "settings”, then come back."
            ),
            done = accessibilityOn,
            actionLabel = if (accessibilityOn) tr("去系统设置查看", "Open settings") else tr("去开启", "Enable"),
            onAction = onOpenAccessibility
        )

        Spacer(Modifier.height(10.dp))
        StepCard(
            index = "2",
            title = tr("悬浮窗权限", "Overlay permission"),
            desc = tr(
                "小球要浮在视频上层才点得到。它只显示一个圆点，不截屏、不录屏。",
                "The ball has to float above the video app to be tappable. It draws one dot — no screenshots, " +
                    "no recording."
            ),
            done = overlayOn,
            actionLabel = if (overlayOn) tr("去系统设置查看", "Open settings") else tr("去授权", "Grant"),
            onAction = onOpenOverlay
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel(tr("它到底会做什么", "What it actually does"))

        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                FactRow(tr("每次间隔都在 2–30 秒之间随机取值，不是固定节奏", "Every gap is a random 2–30 seconds, not a fixed beat"))
                FactRow(
                    tr(
                        "起手位置每拍都会小幅漂移，轨迹带一点弯曲，先慢、中间快、收尾缓，约 0.38–0.64 秒",
                        "Start point drifts a little each time; the path curves and eases in and out, about 0.38–0.64 s"
                    )
                )
                FactRow(tr("点小球 ▶ 开始，图标变成 ❚❚；再点一下立刻停", "Tap ▶ on the ball to start, it turns ❚❚; tap again to stop"))
                FactRow(tr("息屏自动暂停；系统里关掉无障碍开关就彻底停下", "Auto-pauses when the screen turns off; turning accessibility off stops everything"))
                FactRow(tr("全程本地运行，不联网、不记录你看了什么", "Runs entirely on device; no network, no record of what you watched"))
                Spacer(Modifier.height(2.dp))
                Text(
                    tr(
                        "一次上滑的成功率取决于目标应用：部分应用对无障碍手势有限制，遇到滑不动的界面说明它对这类操作做了限制。",
                        "Whether a swipe works depends on the target app: some limit accessibility gestures, and a " +
                            "screen that won't move is that app's restriction."
                    ),
                    fontSize = 10.5.sp,
                    lineHeight = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "v" + BuildConfig.VERSION_NAME,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("  ·  ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                tr("开源仓库", "Repository"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenRepo() }
            )
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun StatusCard(
    running: Boolean,
    accessibilityOn: Boolean,
    count: Int,
    left: Int,
    lastGap: Int,
    ballVisible: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggleBall: () -> Unit
) {
    val accent = when {
        running -> BrandIndigo
        accessibilityOn -> BrandBlue
        else -> Color(0xFF8A94A3)
    }
    val title = when {
        running -> tr("正在自动翻页", "Auto-swiping")
        accessibilityOn -> tr("已暂停，等你点一下", "Paused — one tap to go")
        else -> tr("还没开启无障碍服务", "Accessibility is off")
    }
    val sub = when {
        running -> buildString {
            append(tr("已翻 $count 页", "$count swipes done"))
            append(" · ")
            append(tr("下次约 $left 秒", "next in ~$left s"))
            if (lastGap > 0) {
                append(" · ")
                append(tr("上次间隔 $lastGap 秒", "last gap $lastGap s"))
            }
        }
        accessibilityOn -> if (ballVisible) {
            tr("点小球上的 ▶ 开始，图标变 ❚❚ 后随时能停", "Tap ▶ on the ball; it turns ❚❚ and you can stop anytime")
        } else {
            tr("先点下面的「显示小球」，或在应用内直接开始", "Tap “Show ball” below, or start right here")
        }
        ballVisible -> tr("小球已经显示出来了；还要开启无障碍服务，点它才会真的翻页", "The ball is showing; enable accessibility so it actually swipes")
        else -> tr("小球只依赖悬浮窗权限，可以先显示；无障碍开关开了才会翻页", "The ball only needs the overlay permission; swiping needs accessibility")
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(accent, CircleShape)
                )
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        sub,
                        fontSize = 11.5.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = if (running) onStop else onStart,
                    enabled = accessibilityOn,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (running) tr("暂停", "Pause") else tr("开始翻页", "Start"), fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onToggleBall,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (ballVisible) tr("隐藏小球", "Hide ball") else tr("显示小球", "Show ball"),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    index: String,
    title: String,
    desc: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(22.dp)
                        .background(
                            if (done) SuccessGreen.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.surfaceVariant,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (done) "✓" else index,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (done) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = if (done) SuccessGreen.copy(alpha = 0.12f)
                    else WarnAmber.copy(alpha = 0.12f)
                ) {
                    Text(
                        if (done) tr("已开启", "On") else tr("未开启", "Off"),
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.5.sp,
                        color = if (done) SuccessGreen else WarnAmber
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                desc,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onAction,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(actionLabel, fontSize = 12.5.sp)
            }
        }
    }
}

@Composable
private fun FactRow(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Box(
            Modifier
                .padding(top = 5.dp)
                .size(5.dp)
                .background(BrandBlue.copy(alpha = 0.55f), CircleShape)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 11.5.sp,
            lineHeight = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
