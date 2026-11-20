更新级别：建议

> **一句话**：把首页的字距、行距和块间距重排了一遍——状态归状态、说明归说明，不再挤成一团。

## 1. 排版节奏

- 主状态那行（还没开始 / 已暂停 / 正在翻页）：行高从默认拉到 42sp，上距 26dp、下距 10dp，标题与副行之间真正空出一截。
- 副行（已翻几页 / 下次几秒）行高 19sp、字距 0.3sp、白色透明度调到 0.82，比标题退一层，但看得清。
- 卡片内经手、按钮、文字链：内边距 24dp，按钮高度 54dp，「开始翻页」加 1.2sp 字距；「显示 / 隐藏小球」上距 8dp、上下各留 10dp 点击区，不再贴着按钮。
- 「自动翻页」小标与「待机 / 运行中」标签：字距 2.4sp / 0.6sp，标签内边距加宽，读起来不再是一个色块。
- 两行权限：标题 15.5sp / 行高 22sp，提示 11.5sp / 行高 17sp，两者间距 5dp，圆角方块与文字间距 16dp，整行上下留 17dp。
- 底部一行小字：字号 12sp、行高 20sp、字距 0.4sp，与权限区之间空 26dp；页脚版本号与两个 Star 链接也加了 0.4sp 字距和 18sp 行高。
- 英文那句一长串小字（`2–30 s random · human-like path · fully offline`）在真机上会被悬浮球压住，缩为 `2–30 s · human-like · offline`。

## 2. README

- 「下载与安装」整节提到「这个项目是谁做的」和「关于署名」前面，开头就直给 GitHub / Gitee 的 releases/latest 链接，先让人下载再谈别的。
- 英文段同样把 `### Download & install` 提到 `### Who made this` 之前。
- 首页截图换成这一版，主视觉图和合成图一并重出。

## English · Release Notes

Level: recommended

> **In one line**: the home screen's letter spacing, line heights and block gaps were retuned — status reads as status, notes read as notes, nothing is crammed together.

### 1. Typographic rhythm

- The main state line (not running / paused / swiping) now has a 42sp line height, 26dp above and 10dp below, so the headline and the line under it actually breathe.
- The sub-line (swipes done / seconds to the next one) uses a 19sp line height, 0.3sp letter spacing and 82% white — one step back from the headline, still legible.
- Inside the card: 24dp padding, a 54dp button, 1.2sp letter spacing on "Start"; show/hide ball sits 8dp under the button with 10dp vertical touch padding.
- The "AUTO SWIPE" label and the Idle/Running chip got wider letter spacing and roomier padding, so they no longer read as one solid block.
- Permission rows: 15.5sp title with a 22sp line height, 11.5sp hint with 17sp, 5dp between them, 16dp after the glyph, 17dp row padding.
- The single line of small print is now 12sp with a 20sp line height and 0.4sp spacing, sitting 26dp under the permission rows; the footer line got the same treatment.
- The long English line (`2–30 s random · human-like path · fully offline`) ran under the floating ball on real devices, so it is now `2–30 s · human-like · offline`.

### 2. README

- The "Download & install" section moved above "Who made this" and "Attribution", opening with the direct GitHub / Gitee `releases/latest` links.
- The English section does the same: `### Download & install` now precedes `### Who made this`.
- Home screenshots were recaptured from this build; the hero and composites were regenerated.
