package com.neurotracee.autoswipe

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import android.widget.Toast

/**
 * 悬浮球宿主（独立于无障碍服务）。
 *
 * 为什么单独一个服务：小球属于「悬浮窗」这件事，只依赖 SYSTEM_ALERT_WINDOW；
 * 无障碍服务负责「滑」，但它没被系统绑定时不存在。把球托管在无障碍里，
 * 用户给了悬浮窗权限却看不到球——这正是 v1.0.0.0 的问题。
 *
 * 现在：给了悬浮窗权限 → 球就能出来；点球时如果无障碍还没开，
 * 明确提示去开（并跳到系统设置），而不是毫无反应。
 * 球的图标仍然跟着真实的运行状态走：▶ 待开始 / ⏸ 正在翻。
 */
class BallService : Service() {

    companion object {
        @Volatile
        var instance: BallService? = null
            private set

        /** 从别的组件（Activity / 无障碍服务）把球拉起来 */
        fun start(ctx: Context): Boolean {
            if (instance != null) return true
            return try {
                ctx.startService(Intent(ctx, BallService::class.java))
                true
            } catch (e: Exception) {
                false
            }
        }

        /** 收起球（用户在应用里点了「隐藏小球」） */
        fun stop(ctx: Context) {
            try {
                ctx.stopService(Intent(ctx, BallService::class.java))
            } catch (e: Exception) {
                // 服务本来就没起来，忽略
            }
        }
    }

    private var ball: BallController? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showBall()
        // 被系统回收后尽量自己回来（用户是明确打开过小球的）
        return START_STICKY
    }

    private fun showBall() {
        if (ball != null) {
            AppState.ballVisible = true
            return
        }
        if (!SwipeAccessibilityService.canOverlay(this)) {
            // 没有悬浮窗权限：直接收掉自己，界面那边会引导去授权
            stopSelf()
            return
        }
        val ctl = BallController(this) { onBallTap() }
        if (!ctl.show()) {
            stopSelf()
            return
        }
        ctl.setRunning(AppState.running)
        ball = ctl
        AppState.ballVisible = true
        AppPrefs.markBallVisible(true)
    }

    fun setRunning(v: Boolean) {
        ball?.setRunning(v)
    }

    /** 点球：无障碍在就切换播放/暂停；不在就明确告诉用户缺哪一步 */
    private fun onBallTap() {
        val svc = SwipeAccessibilityService.instance
        if (svc == null || !SwipeAccessibilityService.isEnabled(this)) {
            toast("还差一步：请先在系统设置里开启「自动翻页」的无障碍服务")
            openAccessibilitySettings()
            return
        }
        svc.toggleLoop()
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            toast("请手动到 系统设置 → 无障碍 中开启「自动翻页」")
        }
    }

    override fun onDestroy() {
        // 球没了就没有「一键停」的入口了，顺手把正在跑的循环停掉，免得它背地里一直滑
        SwipeAccessibilityService.instance?.stop()
        ball?.hide()
        ball = null
        if (instance === this) instance = null
        AppState.ballVisible = false
        super.onDestroy()
    }

    private fun toast(text: String) {
        runCatching { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    }
}
