package com.neurotracee.autoswipe

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * 自动上滑的核心。
 *
 * 只做一件事：每隔 2–30 秒随机取一个间隔，在屏幕上模拟一次「从下往上滑」。
 * 手势是标准 AccessibilityService.dispatchGesture，不点击、不读取屏幕内容、
 * 不碰输入框，也不联网。
 *
 * 轨迹按真人的样子生成：起点与终点每次都小幅漂移，路径带轻微横向弯曲与抖动，
 * 并且拆成「起步慢 → 中间快 → 收尾缓」三段不同速度（用 willContinue 连续笔画实现，
 * 手指全程不抬起）。漂移幅度被压得很小，纵向始终单调向上，保证一定滑得上去。
 *
 * 生命周期完全由系统管：用户在系统设置里打开「自动翻页」后，系统绑定这个服务；
 * 关掉开关或服务被系统回收时，循环、悬浮球、状态一并收干净。
 *
 * 循环本身的开关来自悬浮球：点一下开始，再点一下停止（见 [toggleLoop]）。
 */
class SwipeAccessibilityService : AccessibilityService() {

    companion object {
        /** 界面上要「现在有没有连上」时读这个；null 表示还没连上 */
        @Volatile
        var instance: SwipeAccessibilityService? = null
            private set

        /** 点小球后到第一次上滑之间的固定等待：留出切回视频 App 的时间 */
        const val FIRST_DELAY_MS = 1_500L

        /** 随机间隔的下限 / 上限（题目要求 2–30 秒） */
        const val MIN_GAP_MS = 2_000L
        const val MAX_GAP_MS = 30_000L

        /** 一次上滑的总时长范围：慢一点更像手指，太快会被识别成甩动 */
        private const val SWIPE_MS_MIN = 380L
        private const val SWIPE_MS_MAX = 640L

        /** 三段速度的时间分配：起步占 42%，中间 36%，收尾 22% */
        private const val SEG1_SHARE = 0.42f
        private const val SEG2_SHARE = 0.36f

        /**
         * 系统里「自动翻页」的开关是否真的打开。
         * 不依赖服务实例：应用冷启动、服务还没回调时也能正确显示状态。
         */
        fun isEnabled(ctx: Context): Boolean {
            val expected = ctx.packageName + "/" + SwipeAccessibilityService::class.java.name
            val flat = try {
                Settings.Secure.getString(
                    ctx.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ).orEmpty()
            } catch (e: Exception) {
                return false
            }
            return flat.split(':').any { it.equals(expected, ignoreCase = true) }
        }

        /** 悬浮窗权限是否已授予 */
        fun canOverlay(ctx: Context): Boolean = try {
            Settings.canDrawOverlays(ctx)
        } catch (e: Exception) {
            false
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var loop = false
    private var count = 0
    private var nextAt = 0L
    private var screenReceiver: BroadcastReceiver? = null

    // ------------------------------------------------------------ 生命周期

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        AppState.serviceEnabled = true
        registerScreenOff()
        // 小球由独立的 BallService 托管：上次开着就恢复，这里只负责把它拉起来
        if (AppPrefs.ballVisible) BallService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要任何屏幕内容：这里刻意什么都不做
    }

    override fun onInterrupt() {
        stop()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        stop()
        AppState.running = false
        AppState.leftSeconds = 0
        unregisterScreenOff()
        instance = null
        AppState.serviceEnabled = false
    }

    /** 息屏时先停下：屏幕黑着滑也没意义，还会白耗电 */
    private fun registerScreenOff() {
        if (screenReceiver != null) return
        val r = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_SCREEN_OFF) stop()
            }
        }
        screenReceiver = r
        try {
            ContextCompat.registerReceiver(
                this, r, IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } catch (e: Exception) {
            screenReceiver = null
        }
    }

    private fun unregisterScreenOff() {
        screenReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        screenReceiver = null
    }

    // ------------------------------------------------------------ 循环

    /** 点悬浮球的唯一分支：跑着就停，停着就跑 */
    fun toggleLoop() {
        if (loop) stop() else start()
    }

    fun start(): Boolean {
        if (loop) return true
        loop = true
        count = 0
        AppState.running = true
        AppState.swipeCount = 0
        AppState.lastGapSeconds = 0
        BallService.instance?.setRunning(true)

        nextAt = SystemClock.uptimeMillis() + FIRST_DELAY_MS
        handler.post(ticker)
        handler.postDelayed(swiper, FIRST_DELAY_MS)
        return true
    }

    fun stop() {
        if (!loop) {
            AppState.running = false
            AppState.leftSeconds = 0
            BallService.instance?.setRunning(false)
            return
        }
        loop = false
        handler.removeCallbacks(swiper)
        handler.removeCallbacks(ticker)
        AppState.running = false
        AppState.leftSeconds = 0
        BallService.instance?.setRunning(false)
    }

    /** 每 0.5 秒刷新一次「下次还有几秒」，只为界面好看，不参与调度 */
    private val ticker = object : Runnable {
        override fun run() {
            if (!loop) return
            val left = nextAt - SystemClock.uptimeMillis()
            AppState.leftSeconds = ((left + 999) / 1000).toInt().coerceAtLeast(0)
            handler.postDelayed(this, 500)
        }
    }

    /** 真正的节拍：滑一次 → 取一个新的随机间隔 → 再排下一次 */
    private val swiper = object : Runnable {
        override fun run() {
            if (!loop) return
            val ok = swipeOnce()
            if (ok) {
                count++
                AppState.swipeCount = count
            }
            val gap = randomGap()
            AppState.lastGapSeconds = (gap / 1000).toInt()
            nextAt = SystemClock.uptimeMillis() + gap
            handler.postDelayed(this, gap)
        }
    }

    /** 2–30 秒内的随机间隔（含两端，毫秒级随机） */
    private fun randomGap(): Long =
        Random.nextLong(MIN_GAP_MS, MAX_GAP_MS + 1)

    /**
     * 一次上滑手势。
     *
     * 轨迹不是固定直线：起点在横向 40%–60%、纵向 73.5%–80% 的带状区域里随机落点，
     * 终点在 27.5%–34.5% 之间，起止横向差最多 5% 屏宽；路径带一点 S 形弯曲和
     * 0.2%–0.5% 屏宽的细波动。幅度都压在很小的范围里，纵向始终单调向上——
     * 宁可少漂一点，也不能漂到侧边栏或导航条上导致滑不上去。
     *
     * 速度也不是匀速：整条轨迹切成「起步（30% 距离 / 42% 时间）、主体（50% / 36%）、
     * 收尾（20% / 22%）」三段，用 willContinue 连续笔画串起来，手指全程不抬起，
     * 所以看起来是先慢、中间快、末尾缓，而不是机器那样一口气匀速推上去。
     *
     * 返回 false 表示这一拍没滑成（系统忙 / 上一次还没走完），循环会照常排下一次。
     */
    private fun swipeOnce(): Boolean {
        if (!isEnabled(this)) {
            // 用户在系统里把开关关掉了，循环没有意义
            stop()
            return false
        }
        val (w, h) = screenSize()
        if (w <= 0f || h <= 0f) return false
        // 上一次手势还没走完：这一拍安静跳过，避免两条轨迹叠在一起
        if (plan != null) return false

        val p = buildPlan(w, h)
        plan = p
        segIndex = 0
        if (!dispatchSegment(0)) {
            plan = null
            return false
        }
        // 兜底：万一某一拍的回调没回来，别让手指一直按在屏幕上
        handler.postDelayed(watchdog, p.totalMs + 900L)
        return true
    }

    // ------------------------------------------------------------ 手势轨迹

    private data class Seg(val path: Path, val ms: Long, val endX: Float, val endY: Float)
    private data class Plan(val segs: List<Seg>, val totalMs: Long)

    private var plan: Plan? = null
    private var segIndex = 0

    /** 上一段的笔画对象：继续笔画必须由它派生（continueStroke 是实例方法） */
    private var lastStroke: GestureDescription.StrokeDescription? = null

    /** 回调超时兜底：抬手，别把手指留在屏幕上 */
    private val watchdog = Runnable { abortGesture() }

    /** 生成一条「像手指」的轨迹，并按速度切成三段 */
    private fun buildPlan(w: Float, h: Float): Plan {
        val startX = w * (0.40f + Random.nextFloat() * 0.20f)          // 40%–60%
        val endX = startX + w * (Random.nextFloat() - 0.5f) * 0.05f    // 起止横向差 ≤5% 宽
        val startY = h * (0.735f + Random.nextFloat() * 0.065f)        // 73.5%–80%
        val endY = h * (0.275f + Random.nextFloat() * 0.070f)          // 27.5%–34.5%

        val amp = w * (0.006f + Random.nextFloat() * 0.010f)           // 弯曲 0.6%–1.6% 宽
        val micro = w * (0.002f + Random.nextFloat() * 0.005f)         // 细波动 0.2%–0.7% 宽
        val bend = (Random.nextFloat() - 0.5f) * 1.2f                  // 弯曲方向、强弱
        val total = Random.nextLong(SWIPE_MS_MIN, SWIPE_MS_MAX + 1)

        val n = 30
        val xs = FloatArray(n + 1)
        val ys = FloatArray(n + 1)
        for (i in 0..n) {
            val t = i.toFloat() / n
            // 纵向严格单调：方向上永远是一次完整的上滑，不会滑到一半折回来
            ys[i] = startY + (endY - startY) * t
            val sway = amp * sin(PI * t.toDouble()).toFloat() * bend
            val wiggle = micro * sin(PI * (2.1 * t + 0.35)).toFloat()
            xs[i] = startX + (endX - startX) * t + sway + wiggle
        }

        // 距离切分：起步 30%、主体 50%、收尾 20%（对应时间 42% / 36% / 22%）
        val cut1 = (n * 0.30f).toInt()
        val cut2 = (n * 0.80f).toInt()

        fun subPath(from: Int, to: Int): Path = Path().apply {
            moveTo(xs[from], ys[from])
            for (i in from + 1..to) lineTo(xs[i], ys[i])
        }

        val ms1 = (total * SEG1_SHARE).toLong().coerceAtLeast(90L)
        val ms2 = (total * SEG2_SHARE).toLong().coerceAtLeast(70L)
        val ms3 = (total - ms1 - ms2).coerceAtLeast(60L)
        return Plan(
            listOf(
                Seg(subPath(0, cut1), ms1, xs[cut1], ys[cut1]),
                Seg(subPath(cut1, cut2), ms2, xs[cut2], ys[cut2]),
                Seg(subPath(cut2, n), ms3, xs[n], ys[n])
            ),
            ms1 + ms2 + ms3
        )
    }

    /**
     * 派发第 [i] 段。
     * 第一段是新的按下（StrokeDescription），后面两段用 continueStroke 接着上一段走，
     * 手指不抬起，所以是一整次滑动而不是三次。
     */
    private fun dispatchSegment(i: Int): Boolean {
        val p = plan ?: return false
        if (i !in p.segs.indices) return false
        val seg = p.segs[i]
        val more = i < p.segs.size - 1
        val stroke = if (i == 0) {
            GestureDescription.StrokeDescription(seg.path, 0, seg.ms, more)
        } else {
            val prev = lastStroke ?: return false
            prev.continueStroke(seg.path, 0, seg.ms, more)
        }
        lastStroke = stroke
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val cb = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                // 只认当前这一段的结果，旧回调直接忽略
                if (i != segIndex || plan == null) return
                val size = plan?.segs?.size ?: return
                if (i + 1 < size) {
                    segIndex = i + 1
                    if (!dispatchSegment(i + 1)) abortGesture()
                } else {
                    finishGesture()
                }
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                if (i == segIndex) abortGesture()
            }
        }

        return runCatching { dispatchGesture(gesture, cb, handler) }.getOrDefault(false)
    }

    private fun finishGesture() {
        plan = null
        segIndex = 0
        lastStroke = null
        handler.removeCallbacks(watchdog)
    }

    /** 中断时把手指抬起来：从上一段派生一个 1px 的收尾笔画，不会留下按住的指针，也不会变成一次点击 */
    private fun abortGesture() {
        val p = plan ?: return
        val idx = (segIndex - 1).coerceAtLeast(0)
        val seg = p.segs.getOrNull(idx)
        val prev = lastStroke
        finishGesture()
        if (seg == null || prev == null) return
        val liftPath = Path().apply {
            moveTo(seg.endX, seg.endY)
            lineTo(seg.endX, seg.endY - 1f)
        }
        val lift = GestureDescription.Builder()
            .addStroke(prev.continueStroke(liftPath, 0, 50L, false))
            .build()
        runCatching { dispatchGesture(lift, null, null) }
    }

    /** 真实屏幕尺寸（含状态栏与导航栏），不同机型上都能滑到该到的高度 */
    private fun screenSize(): Pair<Float, Float> {
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val b = wm.maximumWindowMetrics.bounds
            b.width().toFloat() to b.height().toFloat()
        } else {
            val dm = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(dm)
            dm.widthPixels.toFloat() to dm.heightPixels.toFloat()
        }
    }

    private fun toast(text: String) {
        runCatching { Toast.makeText(this, text, Toast.LENGTH_SHORT).show() }
    }
}
