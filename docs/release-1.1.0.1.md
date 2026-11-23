更新级别：建议

> **一句话**：崩溃提示不再每次都弹——标记改成一次性、只认最近 7 天，并且正常切应用不会被误报。

## 1. 崩溃提示只提醒一次

- 启动时读到崩溃标记就把文件删掉，下一次启动不会再出现同一条提示。
- 只对最近 7 天内的崩溃有效，更旧的记录直接作废。
- 提示卡直接写明异常类名与消息（如 `IllegalStateException: ...`），比「异常退出」更具体。
- 卡片右下新增「知道了」，点一下收起，不用先打开日志页。

## 2. 判定没有放宽，只是不再粘着

- 标记只在未捕获异常时写入；按 Home 键切走、进程被系统回收、正常返回退出，都不会生成记录。
- 日志里那条「上次崩溃于 …」仍然照写，方便回溯，但它只是历史记录，不影响提示是否出现。

## English · Release Notes

Level: recommended

> **In one line**: the crash notice fires once instead of every launch — the flag is consumed on read, only recent crashes count, and switching apps is never mistaken for a crash.

### 1. The notice appears once

- The flag file is deleted as soon as it is read, so the same crash cannot prompt again on later launches.
- Only crashes from the last 7 days are surfaced; older records are dropped.
- The card now shows the exception class and message (e.g. `IllegalStateException: ...`) instead of a vague "abnormal exit".
- A "Dismiss" action closes the card without opening the log page.

### 2. Detection is unchanged — only the stickiness

- The flag is written solely by the uncaught-exception handler. Pressing Home, the system reclaiming the process, or a normal back-out writes nothing.
- The "crashed at …" line in the log is kept for traceability; it is history only and does not decide whether the card shows.
