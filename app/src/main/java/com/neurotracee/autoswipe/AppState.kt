package com.neurotracee.autoswipe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 无障碍服务 → 界面 的单向状态栏。
 *
 * 服务在系统进程里被绑定，界面随时可能被关掉再打开，所以状态放在这个单例里，
 * 而不是各自持有引用。字段只在主线程写（服务的所有回调本来就在主线程），
 * Compose 读它的 MutableState 会自动重组。
 */
object AppState {

    /** 无障碍服务是否已经连上（等于系统里那个开关真的打开了） */
    var serviceEnabled by mutableStateOf(false)

    /** 悬浮球现在是否显示 */
    var ballVisible by mutableStateOf(false)

    /** 是否正在按随机间隔自动上滑 */
    var running by mutableStateOf(false)

    /** 本次运行已经翻过多少页 */
    var swipeCount by mutableStateOf(0)

    /** 距离下一次上滑还有几秒（未运行时为 0） */
    var leftSeconds by mutableStateOf(0)

    /** 最近一次实际使用的间隔（秒），用来让用户确认「确实是随机的」 */
    var lastGapSeconds by mutableStateOf(0)

    /** 上次是不是异常退出：首页会因此多一行「去看日志」的提示 */
    var crashNotice by mutableStateOf(false)
}
