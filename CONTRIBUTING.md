# 参与改进

先说结论：**不用问、不用等批准，直接 fork 就行。** 这个项目是个人在业余时间维护的，
与其让你在门口等回复，不如你先把东西做出来。

这份文件只讲三件事：代码在哪、怎么跑起来、提交前必须过哪些检查。

- 许可是 **GNU AGPL-3.0-or-later**，另有 [ADDITIONAL-TERMS.md](ADDITIONAL-TERMS.md) 里的三条附加要求
  （保留署名、程序里标注原项目地址、改过的版本要说明改过）。改动前请先看一眼。
- 报缺陷、提建议、问用法 → 用 [Issues](https://github.com/neurotracee/auto-swipe/issues/new)，
  Gitee 那边也开着：[Gitee Issues](https://gitee.com/bio234/auto-swipe/issues)。
- 想改代码 → 本文往下看。

---

## 1. 仓库长什么样

单模块安卓工程（Kotlin + Jetpack Compose），没有服务端、没有原生库。

| 路径 | 是什么 |
| --- | --- |
| `app/src/main/java/com/neurotracee/autoswipe/` | 全部代码（见 README 的目录表） |
| `app/src/main/res/xml/accessibility_service_config.xml` | 无障碍服务声明（`canPerformGestures` 在这里） |
| `app/build.gradle.kts` | 版本号与签名（`appVersionName` 是唯一版本来源） |
| `docs/` | 发布流程与说明 |
| `LICENSE` · `NOTICE.md` · `ADDITIONAL-TERMS.md` | 许可与第三方声明 |

## 2. 本地跑起来

需要 **JDK 17 + Android SDK 34 + Gradle 8.x**。

```bash
gradle assembleDebug       # 调试包
gradle assembleRelease     # 发布包：app/build/outputs/apk/release/app-release.apk
```

签名用的是根目录 `keystore.properties`（**不在仓库里**，自己打包时自建一份：
`storeFile` / `storePassword` / `keyAlias` / `keyPassword` 四项）。没有它也能构建，
只是产物未签名，装不上；自己签一份日常调试足够。

## 3. 提交前必须过的检查

本工程没有自动化测试套件，改动之后**至少手动过一遍这几项**：

| 你改了什么 | 必须验证 |
| --- | --- |
| 手势路径 / 间隔（`SwipeAccessibilityService`） | 真机上开无障碍，点球 ▶，确认第一次约 1.5 秒后上滑、之后间隔落在 2–30 秒，状态卡片的倒计时与日志一致 |
| 悬浮球（`BallService` / `BallController` / `BallView`） | 只给悬浮窗权限、不开无障碍，点「显示小球」球必须出现；拖到屏幕边缘不越界；点球提示去开无障碍；运行态图标变 ⏸ |
| 权限引导（`MainActivity` / `HomeScreen`） | 从系统设置返回时状态立刻更新；无障碍关着时「开始翻页」不可点 |
| 更新相关（`Updater` / `UpdateApk` / `ReleaseDigest`） | 用一个真实的 Release 验证能读到版本号；把 `Updater.RELEASES_PAGE` 指向不存在的仓库时要静默失败、不卡住；安装包校验失败时只提示、不装 |
| 界面文案 | 与 README、`使用说明.txt` 里的同一处说法保持一致 |
| 新增依赖 | 更新 `NOTICE.md`，并确认许可兼容（AGPL-3.0 能用的库才能进） |

构建至少跑一次 `gradle assembleRelease`，确保 `BUILD SUCCESSFUL`（编译器告警要清掉，
现在的基线是零 error、零明显 warning）。

## 4. 版本号规则

版本号统一四段：**`主.次.修订.补丁`**，发布标签前面加小写 `v`，例如 `v1.0.0.1`。

| 段 | 什么时候动 | 例子 |
| --- | --- | --- |
| 主 | 核心用法或数据结构变了 | `2.0.0.0` |
| 次 | 加了用户能看见的新功能 | `1.1.0.0` |
| 修订 | 修缺陷、改文案、调性能，不加新功能 | `1.0.1.0` |
| 补丁 | 同一修订版内的紧急修补，只为这一件事再发一次 | `1.0.0.2` |

- 平时发版**补丁段写 `0`**；只有「刚发出去就发现必须马上修」才动第四位。
- 老的三段写法（`1.0.1`）一律视同 `1.0.1.0`。
- 版本号比较是「补齐四段后逐段比、缺段当 0」。
- 一次发版只改**一处**：`app/build.gradle.kts` 的 `appVersionName`。
  `versionCode` 由它自动算出（主×1000000 + 次×10000 + 修订×100 + 补丁），**不要手填**。

## 5. 提交与 PR

- **一个 PR 只做一件事。** 顺手重排整个文件、改一堆引号风格这类改动会让 review 变成体力活，请单独提。
- 提交信息用「范围: 做了什么」的形式，例如
  `fix(ball): 隐藏小球时同步停止循环`、`docs: 补上 Android 13 受限设置的说明`。
- 描述里写清楚「改了什么、怎么验证的」，有真机截图更好。
- 不要提交：`build/`、`app/build/`、`keystore.properties`、`*.jks`、`local.properties`、
  `release/`、任何 apk。

## 6. 行为准则（很重要）

这是一个替代手指滑动的辅助工具，不是自动化平台。**不接受**以下方向的 PR：

- 刷量、刷互动、批量养号、绕过目标应用风控的功能；
- 读取屏幕内容、抓取界面文字、模拟点击广告或按钮的功能；
- 往应用里塞统计、广告、崩溃上报 SDK；
- 把「随机间隔」改成固定或可控到秒以下的「高频」节奏。

改这些方向的 PR 会直接关闭，不做讨论。
