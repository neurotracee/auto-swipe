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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import com.neurotracee.autoswipe.AppState
import com.neurotracee.autoswipe.BuildConfig

/**
 * 首页：一张渐变主卡说明「现在在不在翻」，下面两行权限，最后一行三枚小标签。
 * 图形用 Canvas 现画，不塞文字。
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
    onOpenRepo: () -> Unit,
    onOpenGitee: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(10.dp))

        HeroCard(
            running = AppState.running,
            accessibilityOn = accessibilityOn,
            count = AppState.swipeCount,
            left = AppState.leftSeconds,
            ballVisible = AppState.ballVisible,
            onStart = onStart,
            onStop = onStop,
            onToggleBall = onToggleBall
        )

        Spacer(Modifier.height(24.dp))
        PermissionRow(
            glyph = Glyph.Swipe,
            title = tr("无障碍服务", "Accessibility"),
            hint = tr("代替手指上滑", "Swipes for your finger"),
            done = accessibilityOn,
            onOpen = onOpenAccessibility
        )
        Spacer(Modifier.height(10.dp))
        PermissionRow(
            glyph = Glyph.Ball,
            title = tr("悬浮窗权限", "Overlay"),
            hint = tr("让小球浮在最上层", "Puts the ball on top"),
            done = overlayOn,
            onOpen = onOpenOverlay
        )

        Spacer(Modifier.height(24.dp))
        Text(
            tr(
                "2–30 秒随机 · 拟人轨迹 · 全程离线",
                "2–30 s random · human-like path · fully offline"
            ),
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(22.dp))
        Footer(onOpenRepo = onOpenRepo, onOpenGitee = onOpenGitee)
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun HeroCard(
    running: Boolean,
    accessibilityOn: Boolean,
    count: Int,
    left: Int,
    ballVisible: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onToggleBall: () -> Unit
) {
    val gradient = if (running) {
        Brush.linearGradient(
            listOf(Color(0xFF4338CA), Color(0xFF7C5CF5), Color(0xFFA07BF7)),
            start = Offset(0f, 0f),
            end = Offset(900f, 760f)
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFF1D4ED8), Color(0xFF4C8DF6), Color(0xFF7FB2FB)),
            start = Offset(0f, 0f),
            end = Offset(900f, 760f)
        )
    }
    val title = when {
        running -> tr("正在翻页", "Swiping")
        accessibilityOn -> tr("已暂停", "Paused")
        else -> tr("还没开始", "Not running")
    }
    val sub = when {
        running -> tr("已翻 $count 页 · 下次约 $left 秒", "$count swipes · next ~$left s")
        accessibilityOn -> tr("点小球或下面的按钮开始", "Tap the ball or the button below")
        else -> tr("先把无障碍打开", "Turn accessibility on first")
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(gradient)
    ) {
        // 右上角的装饰：一条向上的轨迹 + 两只箭头 + 一圈柔光，纯装饰，不承载信息
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(size.width * 0.92f, size.height * 0.02f)
            val r = size.width * 0.78f
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(0f to Color.White.copy(alpha = 0.22f), 1f to Color.Transparent),
                    center = c,
                    radius = r
                ),
                radius = r,
                center = c
            )
            drawSwipeTrail(size.width * 0.82f, size.height * 0.34f, size.height * 0.92f)
        }

        Column(Modifier.padding(horizontal = 22.dp, vertical = 22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tr("自动翻页", "AUTO SWIPE"),
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Text(
                        if (running) tr("运行中", "Running") else tr("待机", "Idle"),
                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 10.5.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Text(sub, fontSize = 12.5.sp, color = Color.White.copy(alpha = 0.8f))

            Spacer(Modifier.height(26.dp))
            val btnBg = if (accessibilityOn) Color.White else Color.White.copy(alpha = 0.35f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(btnBg)
                    .clickable(enabled = accessibilityOn) { if (running) onStop() else onStart() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (running) tr("暂停", "Pause") else tr("开始翻页", "Start"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (accessibilityOn) Color(0xFF33406B) else Color.White.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (ballVisible) tr("隐藏小球", "Hide ball") else tr("显示小球", "Show ball"),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleBall() }
                    .padding(vertical = 9.dp)
            )
        }
    }
}

/** 在画布右侧画一条向上弯的轨迹 + 箭头，纯装饰。 */
private fun DrawScope.drawSwipeTrail(x: Float, top: Float, bottom: Float) {
    val p0 = Offset(x, bottom)
    val p1 = Offset(x + 30f, top + (bottom - top) * 0.35f)
    val p2 = Offset(x - 30f, top + (bottom - top) * 0.72f)
    val p3 = Offset(x, top)

    val path = Path()
    path.moveTo(p0.x, p0.y)
    path.cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
    drawPath(
        path,
        color = Color.White.copy(alpha = 0.36f),
        style = Stroke(width = 2.6f, cap = StrokeCap.Round)
    )

    // 轨迹上的几个点，暗示一次次上滑，点都落在曲线上
    for (i in 0..3) {
        val t = 0.22f + i * 0.22f
        drawCircle(Color.White.copy(alpha = 0.32f), radius = 3.6f, center = cubicAt(p0, p1, p2, p3, t))
    }

    // 顶部箭头
    val head = Path()
    head.moveTo(x - 12f, top + 16f)
    head.lineTo(x, top)
    head.lineTo(x + 12f, top + 16f)
    drawPath(head, Color.White.copy(alpha = 0.42f), style = Stroke(width = 2.8f, cap = StrokeCap.Round))
}

private fun cubicAt(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val x = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x
    val y = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y
    return Offset(x, y)
}

private enum class Glyph { Swipe, Ball }

@Composable
private fun PermissionRow(
    glyph: Glyph,
    title: String,
    hint: String,
    done: Boolean,
    onOpen: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().clickable { onOpen() }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(
                        if (done) SuccessGreen.copy(alpha = 0.12f) else BrandBlue.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(20.dp)) {
                    val c = if (done) SuccessGreen else BrandBlue
                    when (glyph) {
                        Glyph.Swipe -> {
                            // 一条上滑轨迹 + 箭头
                            val p = Path()
                            p.moveTo(size.width * 0.5f, size.height * 0.95f)
                            p.cubicTo(
                                size.width * 0.8f, size.height * 0.7f,
                                size.width * 0.2f, size.height * 0.4f,
                                size.width * 0.5f, size.height * 0.05f
                            )
                            drawPath(p, c, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                            val h = Path()
                            h.moveTo(size.width * 0.3f, size.height * 0.24f)
                            h.lineTo(size.width * 0.5f, size.height * 0.05f)
                            h.lineTo(size.width * 0.7f, size.height * 0.24f)
                            drawPath(h, c, style = Stroke(width = 2.6f, cap = StrokeCap.Round))
                        }
                        Glyph.Ball -> {
                            drawCircle(c, radius = size.minDimension * 0.26f, center = center)
                            drawCircle(
                                c.copy(alpha = 0.35f),
                                radius = size.minDimension * 0.42f,
                                center = center,
                                style = Stroke(width = 1.6f)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(hint, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = if (done) SuccessGreen.copy(alpha = 0.12f) else BrandBlue.copy(alpha = 0.10f)
            ) {
                Text(
                    if (done) tr("已开", "On") else tr("去开", "Turn on"),
                    Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (done) SuccessGreen else BrandBlue
                )
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Footer(onOpenRepo: () -> Unit, onOpenGitee: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("v" + BuildConfig.VERSION_NAME, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("  ·  ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "★ GitHub",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onOpenRepo() }
        )
        Text("  ·  ", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            "★ Gitee",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable { onOpenGitee() }
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
