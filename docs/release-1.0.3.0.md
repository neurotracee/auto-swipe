更新级别：建议

> **一句话**：更新弹窗现在能左右翻看最近 6 个版本各自的说明，正文超长也能滚；下载始终是最新版。

## 1. 更新说明：多版本翻阅（安卓）

- 更新器把发布列表里最近几版一起解析回来：每版带版本号、发布日期、自己的安装包地址、自己的发布页，以及拆好的正文。最多 6 条，至少覆盖最新 4 版——好几版没更新的人能一次看完这中间都改了什么。
- 弹窗改成横向分页：左右滑动，或点顶部的 `‹` `›`，或点下面的页码点。当前页顶部显示 `v1.0.3.0 · 2026-11-19 · 你当前是 vX`，最新那页带「最新」标签。
- 每一页的正文单独竖向滚动（高度封顶 250dp）：发布正文再长也能在这里读完，不会把整个弹窗撑出屏幕。
- 每页底部「这一版的发布页 →」打开的是那一版自己的页面，不再都跳最新版。

## 2. 下载行为

- 翻阅只影响你读到哪一页的说明。点「立即更新」用的永远是第 1 页那版，不会因为翻到旧版就下旧版。
- 弹窗底部有一行小字写明这件事，避免误解。

## 3. 顺带修正

- 更新摘要原来固定取中文，现在按当前界面语言取（英文界面看英文摘要，取不到才退回中文）。

## 4. 版本

- 1.0.2.0 → 1.0.3.0（功能级改动，动第三段）；versionCode `1000300`。
- 覆盖安装即可；设置、悬浮球位置、语言选择都保留。

## English · Release Notes

Level: recommended

> **In one line**: the update dialog now lets you flip through the last six releases with their own notes, long notes scroll inside the page, and the download is always the newest build.

### 1. Release notes: browse several versions (Android)

- The updater now returns the recent releases together: each with its version, publish date, its own APK URL, its own release page and its parsed notes. Up to 6 entries, never fewer than the newest 4 — so someone who skipped a few versions can see everything that changed in between.
- The dialog is a horizontal pager: swipe, tap `‹` `›` at the top, or tap the page dots. The current page header reads `v1.0.3.0 · 2026-11-19 · you are on vX`, and the newest page carries a "Latest" chip.
- Each page's notes scroll vertically on their own (capped at 250dp), so even a long release body is readable without pushing the dialog off screen.
- "This version's release page →" at the bottom of each page opens that version's own page, not always the newest one.

### 2. Download behaviour

- Browsing only changes which notes you read. "Update now" always uses page 1's build; flipping back never downloads an old APK.
- A line of small print at the bottom of the dialog states this, so there is no ambiguity.

### 3. Also fixed

- The summary used to be hard-wired to Chinese; it now follows the current UI language (English UI reads the English summary and falls back to Chinese only when missing).

### 4. Version

- 1.0.2.0 → 1.0.3.0 (feature-level, third segment); versionCode `1000300`.
- Installs over the old version; settings, ball position and language choice are kept.
