更新级别：建议

> **一句话**：界面、安装器提示、系统无障碍说明、README 和这个发布页，现在都是中英双语。

## 1. 双语补齐（安卓）

- 安装器的失败原因全部走 `tr()`：下载地址不在白名单、文件太小、下载不完整、不是安装包、包名不对、版本对不上、没法交给系统——英文界面下不会再冒中文。
- 新增 `values-en/strings.xml`：系统「无障碍」设置里这一项的服务名与说明、桌面上的应用名，在英文系统下显示英文。
  这一处跟随系统语言，不受应用内切换影响（系统页面没法被单个应用改写）。

## 2. 文档与发布页（仓库）

- README 改成中英对等两版（467 行）：来源与署名三条、权限、滑动细节、两级设置、隐私、更新机制、安装、构建、目录、FAQ、维护节奏都有英文版，顶部加语言跳转。
- `使用说明.txt` 补了英文附录，装完直接看的那份也是双语。
- 以后每个发布页正文都是「中文段 + `## English · Release Notes` 段」，应用按当前语言取对应那段，取不到就退回另一段。

## 3. 版本

- 1.0.1.0 → 1.0.1.1，只动最后一段；versionCode `1000101`。
- 覆盖安装即可，不用卸载；设置和悬浮球位置都保留。

## English · Release Notes

Level: recommended

> **In one line**: the UI, installer messages, the system accessibility description, the README and these release notes now ship in both Chinese and English.

### 1. Localisation completed (Android)

- Every installer failure reason now goes through `tr()` — disallowed download URL, file too small, incomplete download, not an APK, wrong package name, version mismatch, handing it to the system failed. No Chinese leaks into an English UI.
- New `values-en/strings.xml`: the accessibility service name and description shown in the system's Accessibility settings, plus the launcher label, render in English on an English system. Those follow the system locale; an app cannot rewrite the system page.

### 2. Documentation and release pages

- The README is now two peer versions rather than a summary: provenance and the three credit requirements, permissions, gesture details, two-level settings, privacy, update mechanism, install, build, layout, FAQ, maintenance cadence — all in English, with a language link at the top.
- The plain-text user guide gained an English appendix.
- From now on every release body is "Chinese section + `## English · Release Notes`", and the app picks the section matching your language, falling back to the other one.

### 3. Version

- 1.0.1.0 → 1.0.1.1, last segment only; versionCode `1000101`.
- Installs over the old version; settings and ball position are kept.
