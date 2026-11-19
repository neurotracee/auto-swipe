"""带壳截屏生成器（AutoSwipe README 用）。

把手机截图套进一个深色机身外壳：圆角、细边框、右侧音量键与电源键、
下面一点柔和阴影。输出透明底 PNG，放进 README 的表格里不会出现白块。

用法：
    python tools/frame_shots.py out_dir shot1.png shot2.png ...

每张输出 <name>.png，宽 640 的设备图；也可以给 --row 生成并排长图。
"""
import os
import sys
from PIL import Image, ImageDraw, ImageFilter

# 目标屏幕宽度（导出图里手机的宽度）
SCREEN_W = 640
BEZEL = 16           # 边框厚度（导出坐标）
SIDE_BTN_W = 5
SHADOW = 46


def rounded_mask(size, radius):
    m = Image.new("L", size, 0)
    ImageDraw.Draw(m).rounded_rectangle((0, 0, size[0] - 1, size[1] - 1), radius, fill=255)
    return m


def frame_one(src_path):
    shot = Image.open(src_path).convert("RGB")
    w, h = shot.size
    sh = int(h * SCREEN_W / w)
    shot = shot.resize((SCREEN_W, sh), Image.LANCZOS)

    screen_r = int(SCREEN_W * 0.115)          # 屏幕圆角
    body_w = SCREEN_W + BEZEL * 2
    body_h = sh + BEZEL * 2
    body_r = screen_r + BEZEL

    canvas = Image.new("RGBA", (body_w + SHADOW * 2 + SIDE_BTN_W, body_h + SHADOW * 2), (0, 0, 0, 0))

    # 阴影：机身形状模糊后压低透明度
    shadow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).rounded_rectangle(
        (SHADOW + SIDE_BTN_W, SHADOW + 10, SHADOW + SIDE_BTN_W + body_w, SHADOW + 10 + body_h),
        body_r, fill=(0, 0, 0, 120),
    )
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas.alpha_composite(shadow)

    ox, oy = SHADOW + SIDE_BTN_W, SHADOW

    d = ImageDraw.Draw(canvas)
    # 机身
    d.rounded_rectangle((ox, oy, ox + body_w, oy + body_h), body_r, fill=(24, 24, 27, 255))
    # 边框高光（内侧一圈细亮线）
    d.rounded_rectangle((ox + 1, oy + 1, ox + body_w - 1, oy + body_h - 1), body_r,
                        outline=(72, 72, 78, 255), width=1)
    # 侧键
    btn_x = ox + body_w + 1
    d.rounded_rectangle((btn_x, oy + int(body_h * 0.20), btn_x + SIDE_BTN_W, oy + int(body_h * 0.31)),
                        3, fill=(38, 38, 42, 255))
    d.rounded_rectangle((btn_x, oy + int(body_h * 0.34), btn_x + SIDE_BTN_W, oy + int(body_h * 0.45)),
                        3, fill=(38, 38, 42, 255))
    d.rounded_rectangle((btn_x, oy + int(body_h * 0.52), btn_x + SIDE_BTN_W, oy + int(body_h * 0.62)),
                        3, fill=(38, 38, 42, 255))

    # 屏幕（圆角裁切）
    mask = rounded_mask((SCREEN_W, sh), screen_r)
    canvas.paste(shot, (ox + BEZEL, oy + BEZEL), mask)
    return canvas


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    for p in sys.argv[2:]:
        img = frame_one(p)
        name = os.path.splitext(os.path.basename(p))[0] + "-framed.png"
        out = os.path.join(out_dir, name)
        img.save(out)
        print(out, img.size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
