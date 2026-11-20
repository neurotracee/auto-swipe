更新级别：建议

> **一句话**：首页整个重做——一张渐变主卡讲清「现在在不在翻」，权限只剩两行，说明文字全部收掉。

## 1. 首页重做

- 顶部原来是一堆卡片和五条要点，现在只留三块：**渐变状态卡**、两行权限、一行小字说明。
- 状态卡：深蓝到浅蓝的斜向渐变，`自动翻页` 小标 + 右侧「待机 / 运行中」标签；大字是当前状态（还没开始 / 已暂停 / 正在翻页），下面一行只写「已翻几页、下次几秒」。
- 卡里带一条向上弯的装饰轨迹和箭头，纯图形，不占一行字。
- 主按钮整宽、白底深字，一眼能找到；「显示 / 隐藏小球」降到文字按钮。
- 两行权限合成一条可点的行：左边图形图标（上滑手势 / 悬浮小球），中间名称与一句五字说明，右边「去开 / 已开」标签；点整行直接跳系统设置。
- 删掉原来那一大段「它到底会做什么」的要点列表，收成一行：`2–30 秒随机 · 拟人轨迹 · 全程离线`。
- 顶部标题栏去掉副标题，只留应用名、间隔标签和设置齿轮。

## 2. 关于页的赞赏说明

- 赞赏码原来是整张图里烤着「如果有帮助，可以请我喝杯奶茶」的中文书法字，英文界面里也会露出中文。现在裁成纯二维码图，那句话改为界面文字，中英各自一套文案。

## 3. README

- 顶部加了一张主视觉图（左侧标题与一句说明、右侧两台真机、左下角两行灰色小字：效果图说明与借鉴来源），中英各一张，英文版点进去不会看到中文界面。
- 界面章节按 DSH Go 的方式排：居中小标题 + 一句话 + 100% 宽合成图，图片来自本版真机截图。
- 新增 `tools/make_hero.py`、`tools/make_showcase.py` 两个出图脚本，图都可由脚本重生成。

## 4. 版本

- 1.0.3.2 → 1.0.4.0（界面级改动，动第三段）；versionCode `1000400`。
- 覆盖安装即可；设置、悬浮球位置、语言选择保留。注意：覆盖安装会关掉系统的无障碍开关，装完记得回去重新打开一次。

## English · Release Notes

Level: recommended

> **In one line**: the home screen was rebuilt — one gradient card says whether it is running, permissions dropped to two rows, and the wall of text is gone.

### 1. Home screen rebuilt

- What used to be several stacked cards and five bullet points is now three blocks: a **gradient status card**, two permission rows, and one line of small print.
- The card uses a diagonal deep-blue to light-blue gradient with an `AUTO SWIPE` label and an "Idle / Running" chip; the large text is the state itself (not running / paused / swiping), with a single line under it for swipes done and seconds to the next one.
- A curved upward trail and arrow are drawn inside the card — pure decoration, taking no line of text.
- The primary button spans the card in white on blue and is easy to find; show/hide ball became a text button.
- The two permissions are now single tappable rows: a drawn glyph (a swipe path, a floating dot), the name plus a five-word hint, and an "On / Turn on" chip; tapping the row opens the system settings.
- The old "what it actually does" bullet list is replaced by one line: `2–30 s random · human-like path · fully offline`.
- The top bar lost its subtitle and keeps the name, the interval chip and the gear.

### 2. Tip section on the About page

- The tip QR used to carry the Chinese caption "如果有帮助，可以请我喝杯奶茶" baked into the image, so it showed up in English too. The image is now the QR alone and the caption is UI text, with its own wording per language.

### 3. README

- A hero image now sits at the top (title and one line on the left, two real devices on the right, two small grey lines at the bottom left for the mockup note and the inspiration credit). There is a Chinese and an English version, so the English page never shows a Chinese UI.
- The screenshots section follows the DSH Go layout: centred heading, one line, then a full-width composite, all from this build's real device captures.
- Two new scripts, `tools/make_hero.py` and `tools/make_showcase.py`, regenerate every image.

### 4. Version

- 1.0.3.2 → 1.0.4.0 (interface-level, third segment); versionCode `1000400`.
- Installs over the previous build; settings, ball position and language choice are preserved. Note: reinstalling turns the system accessibility switch off, so flip it back on once after installing.
