package com.neurotracee.autoswipe

import android.content.Context
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * 本地日志：只写本机文件，不上传、不申请存储权限。
 *
 * 结构上参考了 Apache-2.0 的 Timber 那套「tag + 级别 + 一次性挂上」的用法，
 * 但落盘部分是自己的：按天一个文件，最多留 365 天。
 *
 * 目录：`filesDir/logs/log-2026-11-23.txt`
 * 行格式：`2026-11-23 22:41:07.318 I/Home    点了开始`
 */
object LogBook {

    /** 保留天数：超过一年的日志在启动时自动删除 */
    const val RETAIN_DAYS = 365

    private const val DIR_NAME = "logs"
    private const val CRASH_FLAG = "crash-pending.txt"

    /** 崩溃提示只对最近 7 天有效，避免旧记录一直挂着 */
    private const val CRASH_FRESH_MS = 7L * 24 * 60 * 60 * 1000

    private val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val io = Executors.newSingleThreadExecutor { r ->
        Thread(r, "logbook").apply { isDaemon = true }
    }

    /** 本次进程启动的时刻：默认页「本次启动」就按它过滤 */
    @Volatile
    var sessionStart: Long = System.currentTimeMillis()
        private set

    @Volatile
    private var app: Context? = null

    @Volatile
    var lastCrashAt: Long = 0L
        private set

    @Volatile
    var lastCrashTitle: String = ""
        private set

    fun init(context: Context) {
        app = context.applicationContext
        sessionStart = System.currentTimeMillis()
        readCrashFlag()
        val ctx = app!!
        io.execute {
            prune(ctx)
            write(ctx, "I", "App", "启动 " + summary(ctx))
            val crash = lastCrashAt
            if (crash > 0) {
                write(ctx, "W", "App", "上次崩溃于 " + stamp.format(Date(crash)) + "：" + lastCrashTitle + "（堆栈见当日日志 E/Crash）")
            }
        }
    }

    fun log(tag: String, msg: String) = write(app, "I", tag, msg)

    fun warn(tag: String, msg: String) = write(app, "W", tag, msg)

    fun error(tag: String, msg: String) = write(app, "E", tag, msg)

    // ------------------------------------------------------------ 崩溃

    /**
     * 挂上全局崩溃捕获：把堆栈写进日志、留一个标记，下次打开时首页和关于页都会提示去看日志，
     * 然后照旧交给系统处理，不吞掉崩溃。
     */
    fun installCrashHandler(context: Context) {
        app = context.applicationContext
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val ctx = app
                if (ctx != null) {
                    val sb = StringBuilder()
                    sb.append("线程 ").append(thread.name).append(" 崩了：")
                        .append(throwable.javaClass.name).append(": ").append(throwable.message ?: "")
                    sb.append('\n').append(throwable.stackTraceToString())
                    writeNow(ctx, "E", "Crash", sb.toString())
                    File(dir(ctx), CRASH_FLAG).writeText(
                        System.currentTimeMillis().toString() + "\n" +
                            (throwable.javaClass.simpleName + ": " + (throwable.message ?: ""))
                    )
                }
            } catch (_: Throwable) {
                // 记录失败也不能再抛，否则就无限循环了
            }
            prev?.uncaughtException(thread, throwable)
        }
    }

    /**
     * 只在「上一次确实是未捕获异常」时才留标记：进程被系统回收、用户切到别的应用、
     * 正常退出，都不会在这里凭空生成记录。标记读一次就删掉（一次性），
     * 所以同一次崩溃不会在之后每次开屏都重复提醒。
     */
    private fun readCrashFlag() {
        val ctx = app ?: return
        val f = File(dir(ctx), CRASH_FLAG)
        if (!f.exists()) return
        runCatching {
            val lines = f.readLines()
            val at = lines.getOrNull(0)?.toLongOrNull() ?: 0L
            // 太旧的记录就别再打扰了
            if (at > 0 && System.currentTimeMillis() - at <= CRASH_FRESH_MS) {
                lastCrashAt = at
                lastCrashTitle = lines.getOrNull(1).orEmpty()
            }
        }
        runCatching { f.delete() }
    }

    /** 用户在首页点掉「上次崩了」的提示后调用 */
    fun dismissCrash(ctx: Context) {
        runCatching { File(dir(ctx), CRASH_FLAG).delete() }
        lastCrashAt = 0L
        lastCrashTitle = ""
    }

    // ------------------------------------------------------------ 读

    fun readSession(ctx: Context): String = readRange(ctx, sessionStart, Long.MAX_VALUE)

    fun readAll(ctx: Context): String = readRange(ctx, 0L, Long.MAX_VALUE)

    /** 读 [from, to] 之间的日志；没有内容返回空串 */
    fun readRange(ctx: Context, from: Long, to: Long): String {
        val sb = StringBuilder()
        days(ctx).forEach { file ->
            runCatching {
                file.forEachLine { line ->
                    val ts = lineToTs(line)
                    if (ts > 0 && ts in from..to) sb.append(line).append('\n')
                }
            }
        }
        return sb.toString()
    }

    fun countRange(ctx: Context, from: Long, to: Long): Int {
        var n = 0
        days(ctx).forEach { file ->
            runCatching { file.forEachLine { if (lineToTs(it) in from..to) n++ } }
        }
        return n
    }

    /** 日志覆盖到的最早一天，用于界面提示 */
    fun oldestDay(ctx: Context): String = days(ctx).firstOrNull()?.name?.removePrefix("log-")?.removeSuffix(".txt").orEmpty()

    fun newestDay(ctx: Context): String = days(ctx).lastOrNull()?.name?.removePrefix("log-")?.removeSuffix(".txt").orEmpty()

    // ------------------------------------------------------------ 清

    /** 删除 [from, to] 之间的行；整天的文件直接删。返回删掉的行数 */
    fun clearRange(ctx: Context, from: Long, to: Long): Int {
        var removed = 0
        days(ctx).forEach { file ->
            runCatching {
                val keep = ArrayList<String>()
                file.forEachLine { line ->
                    if (lineToTs(line) in from..to) removed++ else keep.add(line)
                }
                if (keep.isEmpty()) file.delete() else file.writeText(keep.joinToString("\n"))
            }
        }
        return removed
    }

    // ------------------------------------------------------------ 内部

    fun summary(ctx: Context): String {
        val model = (Build.MANUFACTURER + " " + Build.MODEL).trim()
        return "v" + BuildConfig.VERSION_NAME + " · " + model + " · Android " + Build.VERSION.RELEASE +
            "(API " + Build.VERSION.SDK_INT + ")"
    }

    private fun dir(ctx: Context): File =
        File(ctx.filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    private fun dayFile(ctx: Context, ms: Long): File = File(dir(ctx), "log-" + dayFmt.format(Date(ms)) + ".txt")

    private fun write(ctx: Context?, level: String, tag: String, msg: String) {
        if (ctx == null) return
        io.execute { writeNow(ctx, level, tag, msg) }
    }

    private fun writeNow(ctx: Context, level: String, tag: String, msg: String) {
        runCatching {
            val now = System.currentTimeMillis()
            val line = stamp.format(Date(now)) + " " + level + "/" + tag.padEnd(7) + " " +
                msg.replace("\n", "\n    ")
            dayFile(ctx, now).appendText(line + "\n")
        }
    }

    /** 超过 365 天的日志文件直接删掉 */
    private fun prune(ctx: Context) {
        val cutoff = System.currentTimeMillis() - RETAIN_DAYS.toLong() * 24 * 3600 * 1000
        days(ctx).forEach { f ->
            val ms = fileDayStart(f)
            if (ms in 1 until cutoff) f.delete()
        }
    }

    private fun days(ctx: Context): List<File> =
        dir(ctx).listFiles { f -> f.name.startsWith("log-") && f.name.endsWith(".txt") }
            ?.sortedBy { it.name } ?: emptyList()

    private fun fileDayStart(f: File): Long {
        val name = f.name.removePrefix("log-").removeSuffix(".txt")
        return runCatching { dayFmt.parse(name)?.time ?: 0L }.getOrDefault(0L)
    }

    private fun lineToTs(line: String): Long =
        runCatching { stamp.parse(line.substring(0, 23))?.time ?: 0L }.getOrDefault(0L)

    /** 日期选择用的当天 00:00 */
    fun startOfDay(y: Int, m: Int, d: Int): Long = Calendar.getInstance().apply {
        clear(); set(y, m, d, 0, 0, 0)
    }.timeInMillis

    /** 某天某一刻，秒和毫秒按 0 / 999 收口，避免漏掉边界那一行 */
    fun atMinute(y: Int, m: Int, d: Int, hour: Int, minute: Int, end: Boolean): Long =
        Calendar.getInstance().apply {
            clear(); set(y, m, d, hour, minute, if (end) 59 else 0)
            if (end) set(Calendar.MILLISECOND, 999)
        }.timeInMillis
}
