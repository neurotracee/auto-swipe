更新级别：建议

> **一句话**：日志页整体改成卡片式，入口从「关于与开源信息」挪到设置页最后一栏。

## 1. 日志页：卡片式重排

- **顶部**：「应用日志」+ 一句「最近 365 天 · 只在本机」，右上角「完成」。
- **两个页签**：「本次启动」「历史日志」，做成一条整体胶囊，选中项白底加粗。
- **本次启动卡**：右上写行数，正文是浅灰等宽块，底部「复制 / 存为 TXT / 刷新」。
- **历史日志·时间范围卡**：日期、时间都是点一下弹选择器；时间留空就是「整天」，点 × 清回整天；下面横向列出「有日志的日期」胶囊，点一下直接查那一天；底部整行蓝色「查询」。
- **历史日志·查询结果卡**：右上写「N 行 · X KB」，正文同款等宽块，下方「复制 / 存为 TXT」；再往下单独一行「清除这段日期的日志」，仍然是两步确认。
- **时间滚轮**：五格窗口、白底圆角，中间一格加粗并加蓝框；分没解锁时是灰的、推不动，轻点滚轮仍可打字输入。
- **反馈卡**：只留一行标题和一句「版本号与机型已自动填好」，模板折叠在「看模板」里，需要再展开。

## 2. 入口位置

- 设置页最后一栏新增「日志 → 查看日志」，点一次直接进日志页。
- 「关于与开源信息」里不再有日志板块，那一页只留：这个应用是什么、署名、开源协议、仓库地址。

## 3. 配色

- 全部取主题色：亮色是浅灰底 + 白卡 + 蓝主色；暗色自动跟随同一套语义，背景、卡片、字段、描边都会变。
- 没有沿用参考设计里那块绿色底，起始时间和结束时间在亮色下也是普通白卡。

## English · Release Notes

Update level: recommended

> **In one line**: the log screen is rebuilt as cards, and the entry moved from "About & open source" to the last block of the settings page.

- **Log screen.** Header with a one-line note and "Done". Two tabs — this launch, history — as a single pill. This-launch card shows the line count, a monospace block, and "Copy / Save TXT / Refresh". History has a range card (date and time both open pickers, empty time means the whole day, chips list days that actually have logs, full-width "Search") and a result card showing "N lines · X KB" with "Copy / Save TXT", plus a separate "Delete logs in this range" line that still asks twice.
- **Time wheels.** Five-row window on white rounded panels; the centre row is bold with a blue frame; minutes stay grey until the hour is moved; tapping a wheel still lets you type.
- **Entry point.** A new "Logs → View logs" block at the end of the settings page; the About page no longer carries the log section.
- **Colours.** All taken from the theme — light grey page with white cards in light mode, the dark scheme follows automatically. No hard-coded green.

---

**标签**（tags）：`android`、`jetpack-compose`、`accessibility-service`、`auto-scroll`、`logs`、`offline` · 最低 Android 8.0（minSdk 26）· 版本 `1.1.1.4`

**下载与仓库 · Downloads & repos**

- GitHub 仓库：https://github.com/neurotracee/auto-swipe
- GitHub 本次 Release：https://github.com/neurotracee/auto-swipe/releases/tag/v1.1.1.4
- Gitee 镜像仓库：https://gitee.com/bio234/auto-swipe
- Gitee 本次 Release（国内下载更快）：https://gitee.com/bio234/auto-swipe/releases/tag/v1.1.1.4

> Gitee 用户：主仓库在 GitHub，Issue 与 PR 请优先提到 GitHub；GitHub 访问不畅时再用 Gitee 镜像。
> GitHub users: the mirror lives on Gitee for mainland China; file issues on GitHub first.
