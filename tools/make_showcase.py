"""README 展示图合成器（AutoSwipe）。

把带壳截图（tools/frame_shots.py 的产物）并排拼成一张宽图，做法参照
DSH Go 的 README：一张图讲一个功能，透明底，浅色/深色主题下都不出白块。

用法：
    python tools/make_showcase.py out_dir name1 p1 p2 [name2 p3 p4 ...]

每组形如 `名字 图1 图2 图3`：以名字开头，后面跟 1~3 张图。
输出 <out_dir>/<name>.png。
"""
import os
import sys
from PIL import Image

GAP = 64          # 手机之间的空隙
MARGIN = 56       # 画布内边距


def compose(paths):
    imgs = [Image.open(p).convert("RGBA") for p in paths]
    # 统一高度：按最高的一张等比缩放其余
    h = max(i.size[1] for i in imgs)
    scaled = []
    for im in imgs:
        if im.size[1] != h:
            w = int(im.size[0] * h / im.size[1])
            im = im.resize((w, h), Image.LANCZOS)
        scaled.append(im)
    total_w = sum(i.size[0] for i in scaled) + GAP * (len(scaled) - 1) + MARGIN * 2
    canvas = Image.new("RGBA", (total_w, h + MARGIN * 2), (0, 0, 0, 0))
    x = MARGIN
    for im in scaled:
        canvas.alpha_composite(im, (x, MARGIN))
        x += im.size[0] + GAP
    return canvas


def main():
    if len(sys.argv) < 4:
        print(__doc__)
        return 1
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    groups, cur = [], None
    for a in sys.argv[2:]:
        if not a.lower().endswith((".png", ".jpg", ".jpeg", ".webp")):
            cur = [a]
            groups.append(cur)
        else:
            if cur is None:
                print("每组要以名字开头：", a)
                return 1
            cur.append(a)
    for g in groups:
        if len(g) < 2:
            continue
        img = compose(g[1:])
        out = os.path.join(out_dir, g[0] + ".png")
        img.save(out, optimize=True)
        print(out, img.size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
