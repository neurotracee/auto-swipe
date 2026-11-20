"""README 主视觉图（hero）生成器。

宽幅首图：左侧大标题 + 三行副标题 + 三枚胶囊，右侧两台带壳手机（约占画面一半面积），
左下角两行灰色小字（免责说明 + 借鉴来源）。中文版与英文版各出一张。

用法：
    python tools/make_hero.py docs/img/hero-zh.png zh
    python tools/make_hero.py docs/img/hero-en.png en
"""
import sys
from PIL import Image, ImageDraw, ImageFont

W, H = 2400, 1500
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
    d.ellipse((W - 620, -320, W + 200, 500), fill=(75, 103, 180, 26))
    d.ellipse((W - 360, 900, W + 260, 1560), fill=(75, 103, 180, 18))
    d.ellipse((-220, -200, 380, 400), fill=(255, 255, 255, 120))
    img.alpha_composite(layer)


def pill(d, xy, text, font, fill=(255, 255, 255, 235), fg=ACCENT, pad=(26, 12)):
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

    f_title = ImageFont.truetype(FONT_BD, 132 if lang == "zh" else 108)
    f_sub = ImageFont.truetype(FONT_RG, 37)
    f_pill = ImageFont.truetype(FONT_RG, 27)
    f_small = ImageFont.truetype(FONT_RG, 23)

    if lang == "zh":
        title = "自动翻页"
        sub = [
            "AutoSwipe · 2–30 秒随机间隔",
            "拟人轨迹上滑 · 悬浮球一键启停",
            "全程离线 · 不读屏幕 · 不需要 root",
        ]
        pills = ["无需 root", "全程离线", "不读屏幕"]
        note1 = "效果图仅供参考，具体以实际界面为准 · 本图由 AI 生成"
        note2 = "思路借鉴 DSH Go 项目，详见 GitHub：ccch1mneyyy/dsh-go"
        shots = ["docs/img/framed/home-zh.png", "docs/img/framed/settings-zh.png"]
    else:
        title = "AutoSwipe"
        sub = [
            "Swipe-up for Android, no screen taps",
            "Random 2–30 s · human-like swipe path",
            "One tap on the ball to start or stop",
        ]
        pills = ["No root", "Fully offline", "No screen reading"]
        note1 = "Visual for reference only; the real UI prevails — AI-generated"
        note2 = "Layout inspired by the DSH Go project — see GitHub: ccch1mneyyy/dsh-go"
        shots = ["docs/img/framed/home-en.png", "docs/img/framed/settings-en.png"]

    # 标题
    title_y = 168
    title_h = int(f_title.size * 1.34)
    d.text((132, title_y), title, font=f_title, fill=INK)

    # 副标题：与标题空出一大截，再按 1.95 倍行距分三行排
    sub_y = title_y + title_h + 66
    sub_lh = int(f_sub.size * 1.95)
    for i, line in enumerate(sub):
        d.text((140, sub_y + i * sub_lh), line, font=f_sub, fill=INK_SOFT)
    sub_bottom = sub_y + (len(sub) - 1) * sub_lh + f_sub.size

    x = 140
    for p in pills:
        x = pill(d, (x, sub_bottom + 46), p, f_pill) + 20

    # 右侧两台手机：高度 1350（约为此前 900 的 1.5 倍）
    ph_h = 1350
    gap = 44
    ph_w = int(769 * ph_h / 1532)
    ph_x0 = W - 110 - (ph_w * 2 + gap)
    ph_y = (H - ph_h) // 2 - 10
    for i, path in enumerate(shots):
        im = Image.open(path).convert("RGBA")
        im = im.resize((ph_w, ph_h), Image.LANCZOS)
        img.alpha_composite(im, (ph_x0 + i * (ph_w + gap), ph_y))

    # 左下角两行灰字：离底边留出约 150px 的呼吸
    d.text((132, H - 150), note1, font=f_small, fill=GRAY)
    d.text((132, H - 100), note2, font=f_small, fill=GRAY)

    img.convert("RGB").save(out, optimize=True)
    print(out, img.size)
    return 0


if __name__ == "__main__":
    sys.exit(main())
