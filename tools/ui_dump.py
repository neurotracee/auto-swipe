"""把 uiautomator dump 的 XML 变成「能点哪里」的清单。

用法：
    python tools/ui_dump.py <ui.xml> [关键字]

不带关键字：列出所有带文字或 content-desc 的节点，格式 `文字/描述  [x1,y1][x2,y2]`。
带关键字：只列匹配项，并额外打印可直接用于 adb shell input tap 的中心点。
"""
import re
import sys

NODE = re.compile(r"<node\b[^>]*>")
ATTR = re.compile(r'(\w[\w-]*)="([^"]*)"')
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")


def center(b):
    m = BOUNDS.search(b or "")
    if not m:
        return None
    x1, y1, x2, y2 = map(int, m.groups())
    return (x1 + x2) // 2, (y1 + y2) // 2


def main():
    path = sys.argv[1]
    kw = sys.argv[2] if len(sys.argv) > 2 else None
    xml = open(path, encoding="utf-8").read()
    hits = []
    for tag in NODE.findall(xml):
        attrs = dict(ATTR.findall(tag))
        label = (attrs.get("text") or "").strip() or (attrs.get("content-desc") or "").strip()
        if not label:
            continue
        b = attrs.get("bounds", "")
        if kw and kw not in label:
            continue
        c = center(b)
        hits.append((label, b, c, attrs.get("clickable")))
    for label, b, c, click in hits:
        if kw:
            print(f"{label}  {b}  center={c[0]},{c[1]}  clickable={click}" if c else f"{label}  {b}")
        else:
            print(f"{label}  {b}")
    if kw and not hits:
        print("(没有匹配)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
