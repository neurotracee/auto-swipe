package com.neurotracee.autoswipe.ui

import com.neurotracee.autoswipe.AppPrefs

/**
 * 界面文案：中文 / English 二选一，立即生效。
 *
 * 用法就是 `tr("设置", "Settings")`——读 [AppPrefs.lang] 这个 Compose 状态，
 * 所以在界面上切换语言会让正在显示的页面直接重组，不需要重启。
 * 非界面的地方（Toast）调用同一个函数，按当下选择的语言取字符串。
 */
fun tr(zh: String, en: String): String = if (AppPrefs.lang == "en") en else zh

/** 仓库地址：首页、设置、关于页共用一处 */
const val REPO_GITHUB = "https://github.com/neurotracee/auto-swipe"
const val REPO_GITEE = "https://gitee.com/bio234/auto-swipe"
