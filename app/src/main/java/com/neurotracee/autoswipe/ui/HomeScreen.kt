package com.neurotracee.autoswipe.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
        SectionLabel("开两个开关就能用")

        Spacer(Modifier.height(8.dp))
        StepCard(
            index = "1",
            title = "无障碍服务",
            desc = "唯一能代替手指上滑的开关。只画一条向上的滑动路径，不读屏幕内容、不点按钮。\n" +
                "如果这个开关是灰的、点不动：先到「应用信息 → 右上角菜单 → 允许受限设置」，再回来开启。",
            done = accessibilityOn,
            actionLabel = if (accessibilityOn) "去系统设置查看" else "去开启",
            onAction = onOpenAccessibility
        )

        Spacer(Modifier.height(10.dp))
        StepCard(
            index = "2",
            title = "悬浮窗权限",
            desc = "小球要浮在视频上层才点得到。它只显示一个圆点，不截屏、不录屏。",
            done = overlayOn,
            actionLabel = if (overlayOn) "去系统设置查看" else "去授权",
            onAction = onOpenOverlay
        )

        Spacer(Modifier.height(16.dp))
        SectionLabel("它到底会做什么")

        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(14.dp)) {
                FactRow("每次间隔都在 2–30 秒之间随机取值，不是固定节奏")
                FactRow("从屏幕约 74% 高度滑到 30%，一次 0.3–0.5 秒，像手指而不是甩动")
                FactRow("点小球 ▶ 开始，图标变成 ⏸；再点一下立刻停")
                FactRow("息屏自动暂停；系统里关掉无障碍开关就彻底停下")
                FactRow("全程本地运行，不联网、不记录你看了什么")
                Spacer(Modifier.height(2.dp))
                Text(
                    "一次上滑的成功率取决于目标应用：部分应用对无障碍手势有限制，遇到滑不动的界面说明它对这类操作做了限制。",
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
                "开源仓库",
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
        running -> "正在自动翻页"
        accessibilityOn -> "已暂停，等你点一下"
        else -> "还没开启无障碍服务"
    }
    val sub = when {
        running -> buildString {
            append("已翻 $count 页")
            append(" · 下次约 $left 秒")
            if (lastGap > 0) append(" · 上次间隔 $lastGap 秒")
        }
        accessibilityOn -> if (ballVisible) "点小球上的 ▶ 开始，图标变 ⏸ 后随时能停" else "先点下面的「显示小球」，或在应用内直接开始"
        ballVisible -> "小球已经显示出来了；还要开启无障碍服务，点它才会真的翻页"
        else -> "小球只依赖悬浮窗权限，可以先显示；无障碍开关开了才会翻页"
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
                    Text(if (running) "暂停" else "开始翻页", fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onToggleBall,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (ballVisible) "隐藏小球" else "显示小球",
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
                        if (done) "已开启" else "未开启",
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
