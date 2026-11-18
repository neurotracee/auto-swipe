package com.neurotracee.autoswipe

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.view.View

/**
 * 悬浮球本身，纯 Canvas 绘制（不用图片资源，任何分辨率都清晰）。
 *
 * 两种状态一眼能分：
 *   · 暂停 / 待开始 → 白色右三角（▶，意思是「点我开始」）
 *   · 正在自动翻页 → 两道白色竖杠（⏸，意思是「点我停下」）
 *
 * 配色只表达状态，不闪、不呼吸、不弹广告，尽量不打扰正在看的视频。
 */
class BallView(ctx: Context) : View(ctx) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val icon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private var running = false
    private var pressed = false

    fun setRunning(v: Boolean) {
        if (running == v) return
        running = v
        invalidate()
    }

    fun setPressedState(v: Boolean) {
        if (pressed == v) return
        pressed = v
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        // 按下时整体缩一点点，给一点触感反馈
        val scale = if (pressed) 0.94f else 1f
        val r = (minOf(width, height) / 2f - 2f * d) * scale

        // ---- 圆底：运行中偏蓝紫，暂停偏石板灰 ----
        val colors = if (running) {
            intArrayOf(0xFF4F46E5.toInt(), 0xFF3B82F6.toInt())
        } else {
            intArrayOf(0xFF5B6B80.toInt(), 0xFF39424F.toInt())
        }
        paint.style = Paint.Style.FILL
        paint.shader = LinearGradient(
            cx - r, cy - r, cx + r, cy + r,
            colors, null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, paint)
        paint.shader = null

        // ---- 内圈高光边，浅色背景上也看得清边界 ----
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.2f * d
        paint.color = 0x33FFFFFF
        canvas.drawCircle(cx, cy, r - 0.7f * d, paint)

        // ---- 图标 ----
        icon.style = Paint.Style.FILL
        icon.color = Color.WHITE
        if (running) {
            val barW = 0.15f * r
            val barH = 0.62f * r
            val gap = 0.10f * r
            val left = cx - gap / 2f - barW
            val top = cy - barH / 2f
            val rad = 0.06f * r
            canvas.drawRoundRect(RectF(left, top, left + barW, top + barH), rad, rad, icon)
            canvas.drawRoundRect(
                RectF(cx + gap / 2f, top, cx + gap / 2f + barW, top + barH),
                rad, rad, icon
            )
        } else {
            // 右三角，水平方向做了一点视觉居中（三角形重心偏左）
            val triW = 0.46f * r
            val triH = 0.60f * r
            val left = cx - triW / 2f + 0.06f * r
            val path = Path().apply {
                moveTo(left, cy - triH / 2f)
                lineTo(left, cy + triH / 2f)
                lineTo(left + triW, cy)
                close()
            }
            canvas.drawPath(path, icon)
        }
    }
}
