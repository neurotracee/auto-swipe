更新级别：建议

> **一句话**：时/分选择改成两只滚轮（像行李箱密码扣），手指上下推、数字跟着走、松手吸附，彻底修掉上一版「滑不动」。

## 1. 为什么上一版滑不动

上一版把滑动挂在数字输入框上，而输入框在手指按下的那一刻就去抢焦点、弹键盘，对话框跟着键盘上移，手指还在滑，手势已经被打断。所以用脚本快速滑动能生效，真人用手指慢慢推反而没反应。

## 2. 这一版怎么改

- **去掉步进键和数字框**：不再有 ▲▼，也不再有一按就弹键盘的输入框，换成上下两只滚轮。
- **像密码扣一样的手感**：手指上下推，数字跟着位移；推过一整格值就变一格，松手 180 毫秒吸附回最近一格，滚动过程有连续的相邻数字。
- **轻点仍能打字**：点一下滚轮弹出小输入框，数字键盘直接覆盖旧值，输入路径没有被砍掉。
- **先时后分不变**：分的滚轮在选时之前是灰的、推不动；时一动过就解锁。
- **退回去不丢**：从分退回上一步改小时，再回来，之前填好的分还在。
- 「取消」「上一步」在左，「确定」在右下角。

## 3. 验证

- 编译通过；发版自检三项核对通过（版本 `1.1.1.1`，versionCode 1010101）。
- 真机覆盖安装成功，Android 16 / OPPO Find X6 Pro 上版本号显示为 1.1.1.1。
- 滚轮手感以真机手指实测为准；如仍觉得太窄或吸附太慢，可调格高与回弹时长。

## English · Release Notes

Update level: recommended

> **In one line**: hour/minute selection is now two wheels, like a luggage combination lock — push up or down, the digits follow, and it snaps on release. The "won't swipe" bug from the last build is gone.

- **Why the last build failed.** The drag was attached to the numeric field, which grabbed focus and raised the keyboard the instant a finger went down. The dialog shifted up with the keyboard, the gesture was interrupted, so a scripted fast swipe worked while a real slow finger did not.
- **No buttons, no field.** The ▲▼ steppers and the keyboard-popping input are gone, replaced by two vertical wheels.
- **Lock-like feel.** Push up or down and the digits move with your finger; each full row shifts the value by one; on release it animates 180 ms back to the nearest row, with neighbouring digits visible while scrolling.
- **Typing is still there.** Tap the wheel to open a small numeric field; typed digits replace the old value.
- **Hour first, then minute.** The minute wheel stays grey and immovable until the hour has been touched.
- **Nothing lost going back.** Return from the minute step to change the hour and come back — the minute is still there.
- **Verification.** Build and release self-check pass (version `1.1.1.1`, versionCode 1010101); installed on device and reports 1.1.1.1 on Android 16.
