package com.neurotracee.autoswipe

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import kotlin.math.abs

/**
 * 悬浮球的挂载与拖动。
 *
 * 窗口类型用 TYPE_APPLICATION_OVERLAY（Android 8 起）——所以必须先拿到
 * SYSTEM_ALERT_WINDOW 权限，否则 addView 会抛 BadTokenException，这里统一吞掉
 * 并返回 false，由界面引导用户去授权。
 *
 * 小球可以拖到任意位置，松手后记住坐标，下次开起来还在原处；
 * 轻点（位移小于系统滑动阈值）才算点击，否则算拖动——避免想拖的时候误触发。
 */
class BallController(
    private val ctx: Context,
    private val onTap: () -> Unit
) {

    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = ctx.resources.displayMetrics.density
    private val sizePx = (58 * density).toInt()
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val view = BallView(ctx)

    private var params: WindowManager.LayoutParams? = null
    private var downRawX = 0f
    private var downRawY = 0f
    private var startX = 0
    private var startY = 0
    private var dragging = false

    fun show(): Boolean {
        if (params != null) return true
        val p = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        val (sw, sh) = screenSize()
        val defX = sw - sizePx - (10 * density).toInt()
        val defY = (sh * 0.66f).toInt()
        p.x = clamp(if (AppPrefs.ballX >= 0) AppPrefs.ballX else defX, 0, sw - sizePx)
        p.y = clamp(if (AppPrefs.ballY >= 0) AppPrefs.ballY else defY, 0, sh - sizePx)

        view.setOnTouchListener { _, event -> handleTouch(event) }
        return try {
            wm.addView(view, p)
            params = p
            true
        } catch (e: Exception) {
            // 没有悬浮窗权限 / 系统不允许：交给界面提示，不在后台反复重试
            false
        }
    }

    fun hide() {
        if (params == null) return
        runCatching { wm.removeView(view) }
        params = null
    }

    fun setRunning(v: Boolean) = view.setRunning(v)

    private fun handleTouch(event: MotionEvent): Boolean {
        val p = params ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = event.rawX
                downRawY = event.rawY
                startX = p.x
                startY = p.y
                dragging = false
                view.setPressedState(true)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - downRawX
                val dy = event.rawY - downRawY
                if (!dragging && (abs(dx) > slop || abs(dy) > slop)) dragging = true
                if (dragging) {
                    val (sw, sh) = screenSize()
                    p.x = clamp(startX + dx.toInt(), 0, sw - sizePx)
                    p.y = clamp(startY + dy.toInt(), 0, sh - sizePx)
                    runCatching { wm.updateViewLayout(view, p) }
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.setPressedState(false)
                if (dragging) {
                    AppPrefs.saveBallPos(p.x, p.y)
                } else if (event.actionMasked == MotionEvent.ACTION_UP) {
                    onTap()
                }
                dragging = false
                return true
            }
        }
        return false
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

    private fun screenSize(): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width() to b.height()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels to dm.heightPixels
        }
    }

    private fun clamp(v: Int, lo: Int, hi: Int): Int =
        if (hi <= lo) lo else v.coerceIn(lo, hi)
}
