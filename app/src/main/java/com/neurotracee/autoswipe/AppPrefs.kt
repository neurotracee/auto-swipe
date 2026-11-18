package com.neurotracee.autoswipe

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 全部本地设置：主题、悬浮球位置、更新检查方式与更新源。
 * 用 Compose 的 MutableState 承载，改完界面立刻重组，不需要重启。
 * 所有设置都只存在本机的 SharedPreferences 里，不往外传。
 */
object AppPrefs {

    private const val FILE = "autoswipe"

    /** 主题：system / light / dark */
    var themeMode by mutableStateOf("system")
        private set

    /** 上次退出时悬浮球是不是开着的：重新开启无障碍后自动恢复 */
    var ballVisible by mutableStateOf(false)
        private set

    /** 悬浮球上次停的位置（像素，左上角）；-1 表示还没拖过，用默认位置 */
    var ballX by mutableStateOf(-1)
        private set
    var ballY by mutableStateOf(-1)
        private set

    /** 启动时自动检查更新；关掉就是纯手动 */
    var autoCheck by mutableStateOf(true)
        private set

    /** 更新源：auto（跟随环境）/ gitee / github */
    var updateSource by mutableStateOf("auto")
        private set

    /**
     * 「这台机器默认该走 Gitee 吗」——启动时算一次就固定，之后不再变。
     * 只看系统环境：中文系统直接 Gitee；非中文但人在东八区也按国内网络走；
     * 任何一项取不到一律按 Gitee 兜底。
     */
    var envPrefersGitee by mutableStateOf(true)
        private set

    private var sp: SharedPreferences? = null

    fun init(ctx: Context) {
        if (sp != null) return
        val p = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        sp = p
        envPrefersGitee = detectGiteePreference(ctx)
        themeMode = when (p.getString("themeMode", "system")) {
            "light", "dark" -> p.getString("themeMode", "system")!!
            else -> "system"
        }
        ballVisible = p.getBoolean("ballVisible", false)
        ballX = p.getInt("ballX", -1)
        ballY = p.getInt("ballY", -1)
        autoCheck = p.getBoolean("autoCheck", true)
        updateSource = p.getString("updateSource", "auto")
            ?.takeIf { it == "gitee" || it == "github" || it == "auto" } ?: "auto"
    }

    fun updateTheme(v: String) {
        themeMode = if (v == "light" || v == "dark") v else "system"
        sp?.edit()?.putString("themeMode", themeMode)?.apply()
    }

    fun markBallVisible(v: Boolean) {
        if (ballVisible == v) return
        ballVisible = v
        sp?.edit()?.putBoolean("ballVisible", v)?.apply()
    }

    fun saveBallPos(x: Int, y: Int) {
        ballX = x
        ballY = y
        sp?.edit()?.putInt("ballX", x)?.putInt("ballY", y)?.apply()
    }

    fun updateAutoCheck(v: Boolean) {
        autoCheck = v
        sp?.edit()?.putBoolean("autoCheck", v)?.apply()
    }

    /** "auto" 表示跟随环境，界面高亮时用 [resolvedUpdateSource] */
    fun updateUpdateSource(v: String) {
        updateSource = if (v == "gitee" || v == "github") v else "auto"
        sp?.edit()?.putString("updateSource", updateSource)?.apply()
    }

    /** 实际会走的源：显式选择优先；auto 时问环境，问不出来按 Gitee 兜底 */
    val resolvedUpdateSource: String
        get() = when (updateSource) {
            "gitee", "github" -> updateSource
            else -> if (Updater.preferGitee()) "gitee" else "github"
        }

    /** 只读系统资源里的语言配置（不是进程的 Locale.getDefault），再结合时区 */
    private fun detectGiteePreference(ctx: Context): Boolean {
        val language = try {
            ctx.resources.configuration.locales.get(0)?.language?.lowercase().orEmpty()
        } catch (e: Exception) {
            ""
        }
        if (language.isBlank()) return true
        if (language.startsWith("zh")) return true

        val offset = try {
            java.util.TimeZone.getDefault()?.getOffset(System.currentTimeMillis())
        } catch (e: Exception) {
            null
        } ?: return true
        return offset == 8 * 60 * 60 * 1000
    }
}
