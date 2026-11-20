"""README 主视觉图（hero）生成器。

一张宽幅首图：左侧标题与一句说明，右侧两台带壳手机，左下角两行灰色小字
（免责说明 + 借鉴来源）。中文版与英文版各出一张。

用法：
    python tools/make_hero.py docs/img/hero-zh.png zh
    python tools/make_hero.py docs/img/hero-en.png en
"""
import sys
from PIL import Image, ImageDraw, ImageFont

W, H = 1800, 780
FONT_BD = r"C:\Windows\Fonts\msyhbd.ttc"
FONT_RG = r"C:\Windows\Fonts\msyh.ttc"

BG_TOP = (246, 249, 255)
BG_BOT = (226, 235, 248)
INK = (27, 36, 48)
INK_SOFT = (90, 100, 114)
GRAY = (138, 143, 152)
ACCENT = (75, 103, 180)


def gradient():
    img = Image.new("RGB", (W, H), BG_TOP)
    d = ImageDraw.Draw(img)
    for y in range(H):
        t = y / (H - 1)
        c = tuple(int(BG_TOP[i] + (BG_BOT[i] - BG_TOP[i]) * t) for i in range(3))
        d.line([(0, y), (W, y)], fill=c)
    return img.convert("RGBA")


def soft_circles(img):
    layer = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((W - 520, -260, W + 160, 420), fill=(75, 103, 180, 26))
    d.ellipse((W - 300, 520, W + 220, 1040), fill=(75, 103, 180, 18))
    d.ellipse((-180, -160, 320, 340), fill=(255, 255, 255, 120))
    img.alpha_composite(layer)


def pill(d, xy, text, font, fill=(255, 255, 255, 235), fg=ACCENT, pad=(22, 10)):
    tw = d.textlength(text, font=font)
    th = font.size
    box = (xy[0], xy[1], xy[0] + int(tw) + pad[0] * 2, xy[1] + th + pad[1] * 2)
    d.rounded_rectangle(box, (box[3] - box[1]) // 2, fill=fill, outline=(200, 212, 232, 255), width=1)
    d.text((xy[0] + pad[0], xy[1] + pad[1] - 2), text, font=font, fill=fg)
    return box[2]


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        return 1
    out, lang = sys.argv[1], sys.argv[2]
    img = gradient()
    soft_circles(img)
    d = ImageDraw.Draw(img)

    f_title = ImageFont.truetype(FONT_BD, 116 if lang == "zh" else 96)
    f_sub = ImageFont.truetype(FONT_RG, 33)
    f_pill = ImageFont.truetype(FONT_RG, 24)
    f_small = ImageFont.truetype(FONT_RG, 21)

    if lang == "zh":
        title = "自动翻页"
        sub = "AutoSwipe · 2–30 秒随机间隔 · 拟人轨迹 · 悬浮球一键启停"
        pills = ["无需 root", "全程离线", "不读屏幕"]
        note1 = "效果图仅供参考，具体以实际界面为准 · 本图由 AI 生成"
        note2 = "思路借鉴 DSH Go 项目，详见 GitHub：ccch1mneyyy/dsh-go"
        shots = ["docs/img/framed/home-zh.png", "docs/img/framed/settings-zh.png"]
    else:
        title = "AutoSwipe"
        sub = "Random 2–30 s gaps · human-like path · one tap on the floating ball"
        pills = ["No root", "Fully offline", "No screen reading"]
        note1 = "Visual for reference only; the real UI prevails — AI-generated"
        note2 = "Layout inspired by the DSH Go project — see GitHub: ccch1mneyyy/dsh-go"
        shots = ["docs/img/framed/home-en.png", "docs/img/framed/settings-en.png"]

    title_y = 120
    title_h = int(f_title.size * 1.42)
    d.text((120, title_y), title, font=f_title, fill=INK)
    sub_y = title_y + title_h
    d.text((124, sub_y), sub, font=f_sub, fill=INK_SOFT)

    x = 124
    for p in pills:
        x = pill(d, (x, sub_y + f_sub.size + 22), p, f_pill) + 16

    ph_h = 600
    gap = 34
    ph_x = W - 96 - (int(769 * ph_h / 1532) * 2 + gap)
    for i, path in enumerate(shots):
        im = Image.open(path).convert("RGBA")
        im = im.resize((int(im.size[0] * ph_h / im.size[1]), ph_h), Image.LANCZOS)
        img.alpha_composite(im, (ph_x, 92))
        ph_x += im.size[0] + gap

    d.text((122, H - 92), note1, font=f_small, fill=GRAY)
    d.text((122, H - 58), note2, font=f_small, fill=GRAY)
    img.convert("RGB").save(out, optimize=True)
    print(out, img.size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
