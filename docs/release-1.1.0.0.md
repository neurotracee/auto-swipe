更新级别：建议

> **一句话**：加了一套只在本机的日志与反馈页——打开就记、崩了也不丢，能按分钟挑范围复制或存成 TXT，还直接给好 issue 模板。

## 1. 打开就记，崩了也留

- 每次启动写一行：版本号、厂商型号、Android 版本与 API。之后开始 / 暂停自动翻页、显示 / 隐藏小球各留一条。
- 未捕获异常会把堆栈写进当天日志，再照常交给系统；下次启动时首页多一行「上次异常退出，日志已保存」，点开直接看。

## 2. 设置里的日志页

- 入口在「设置 → 关于与开源信息 → 查看日志」。
- 两个视角：「本次启动」只显示这次打开之后的记录；「最近一年」可查 365 天内全部日志，更早的在启动时自动清掉。
- 范围精确到分钟：点起始 / 结束的时间块，先选日期再选时分；选反了会自动交换。
- 复制日志、保存 TXT（走系统「保存到…」选择器，不申请存储权限）、清除。清除要过两道确认，第一道先报范围与行数，第二道红字提醒不可恢复。
- 日志区等宽字体、可上下左右滚动；末尾附版本号。

## 3. 反馈模板与入口

- 模板自动填好「版本与设备」，并给出复现步骤 1. 打开… / 2. 点开… / 3. 结果…、实际看到、预期是，最后一句说明日志粘贴或作为 TXT 附件上传（含行数）。
- 一键复制模板，旁边直达 GitHub Issues 与 Gitee Issues。

## English · Release Notes

Level: recommended

> **In one line**: a fully local logbook and feedback page — records from launch, survives crashes, lets you pick a range down to the minute, and hands you a ready-made issue template.

### 1. Recorded from launch, kept through crashes

- Each launch writes version, device model and Android version/API; start, pause and show/hide ball each add a line.
- Uncaught exceptions append the stack trace to that day's file and are still handed to the system handler. On the next launch the home screen shows “The app crashed last time — the log was saved”, tapping through to the log page.

### 2. The log page in Settings

- Entry: Settings → About & open source → View logs.
- Two views: “This session” shows only lines since this launch; “Last 12 months” reaches everything within 365 days, older files are removed on launch.
- Ranges are picked to the minute: tap the start/end chip, choose a date, then a time; reversed ranges are swapped for you.
- Copy, save as TXT (system “Save as…” picker, no storage permission), and clear. Clearing asks twice: first the range and line count, then a red confirmation that it cannot be undone.
- The log pane is monospaced and scrolls both ways; the version sits at the bottom.

### 3. Template and where to file

- The template pre-fills version & device, then steps 1. Open… / 2. Tap… / 3. Result…, what you saw, what you expected, and a closing line about pasting the log or attaching the TXT (with line count).
- One tap copies the template; buttons next to it open GitHub Issues and Gitee Issues.
