更新级别：建议

> **一句话**：把「崩溃」的判定边界写死，只有未捕获异常才算崩溃；日志选时间的表盘换成「时 / 分」步进 + 键盘输入。

## 1. 崩溃判定：只认真崩溃

之前的问题是标记留得太久又读得太宽，正常切应用回来也会看到提示。这一版把口径写清楚，也写进代码注释和 README：

- **算崩溃**：某线程抛出未捕获异常，被全局未捕获异常处理器接住，才写堆栈、才留标记、下次启动才提示。
- **不算崩溃**：按 Home 或多任务切走、系统低内存把后台进程回收、强制停止、正常按返回退出、系统重启或更新后再打开。这些只会留一条普通启动行，不写标记、不弹提示。
- **本版不推断**：ANR（应用无响应）和原生层崩溃走不到 Java 未捕获异常处理器，不据此判成崩溃，宁可少报也不误报。
- 标记仍然是读到即删，同一次崩溃只提醒一次，7 天外自动作废。

## 2. 时间选择器：去掉表盘

选时间那块从 Material 的表盘换成两组数字：

- 时、分各一组，**上下步进键**、**上下滑动**、**键盘输入**三种方式都支持，键盘是数字键盘。
- **先时后分**：分一开始是锁着的，摸过（点键、滑动、点输入框）时才解锁，防手滑。
- **退回去不丢**：从分点「上一步」回去改小时，再点「下一步」回来，之前填好的分原样还在。
- **输入不打架**：点进输入框会把旧值清掉、用灰字占位显示，打进去的数字直接顶掉它，不会出现「21」后面接「7」变成「17」这种。
- 「取消」「上一步」在左，「确定」在右下角。

## 3. 验证

真机上跑过这些：

- 正常启动 → 按 Home → 强制停止 → 重开：首页没有崩溃提示。
- 人为制造未捕获异常 → 重开：提示出现并写明异常类名；点「知道了」后再重开：不再出现。
- 时/分的步进键、上滑下滑、键盘输入各测一遍；从分退回时再回来，分保留；确定后范围正确落在所选那一分钟。

## English · Release Notes

Update level: recommended

> **In one line**: the crash rule is now written down and strict — only an uncaught exception counts; the dial-style time picker is replaced by hour/minute steppers plus keyboard input.

- **Crash means a crash.** Going to background, being reclaimed by low memory, force-stop, a normal back exit, or a reboot/update never write a crash flag and never raise the home-screen notice. Only an uncaught exception caught by the global handler does.
- **No guessing.** ANRs and native crashes never reach the Java handler, so this release does not infer crashes from them — fewer notices, no false alarms.
- **One-shot flag.** The flag is still deleted as it is read, and expires after 7 days.
- **No more dial.** Hour and minute each get a stepper, vertical swipe and numeric keyboard input. The minute stays locked until the hour is touched, so a slip cannot change it first.
- **Nothing lost on the way back.** Return from the minute step to change the hour, then come back — the minute you already set is still there.
- **Typing replaces.** Tapping the field clears it and shows the old value as a grey placeholder, so typed digits replace instead of appending.
