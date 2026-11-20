"""发布前自检：releases 页写的版本号，必须和 APK 里实际的版本号一致。

用法：
    python tools/check_release.py                     # 默认查 app/build/outputs/apk/release/app-release.apk
    python tools/check_release.py path/to/x.apk       # 查指定包
    python tools/check_release.py x.apk 1.0.4.2       # 同时核对「这次要发的版本号」

检查三件事：
1. 包里的 versionName 与 app/build.gradle.kts 的 appVersionName 完全一致；
2. 包里的 versionCode 等于四段版本号算出来的值（主*1000000+次*10000+修订*100+补丁）；
3. 若命令行给了「这次要发的版本号」，它也必须与包里的版本号一致。

任何一项不符就返回非 0，提醒先重新构建再发版，避免「发布页版本号最新、下载到的包其实还是旧的」。
"""
import os
import re
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE = os.path.join(ROOT, "app", "build.gradle.kts")
DEFAULT_APK = os.path.join(ROOT, "app", "build", "outputs", "apk", "release", "app-release.apk")

AAPT_CANDIDATES = [
    os.path.join(os.environ.get("ANDROID_HOME", ""), "build-tools", "34.0.0", "aapt2.exe"),
    os.path.join(os.environ.get("ANDROID_HOME", ""), "build-tools", "34.0.0", "aapt2"),
]


def gradle_version():
    txt = open(GRADLE, encoding="utf-8").read()
    m = re.search(r'appVersionName\s*=\s*"([0-9.]+)"', txt)
    if not m:
        raise SystemExit("读不到 app/build.gradle.kts 里的 appVersionName")
    return m.group(1)


def code_of(v):
    a, b, c, d = [int(x) for x in v.split(".")]
    return a * 1000000 + b * 10000 + c * 100 + d


def aapt2():
    for p in AAPT_CANDIDATES:
        if p and os.path.exists(p):
            return p
    raise SystemExit("找不到 aapt2，请设置 ANDROID_HOME 或把 build-tools 路径写进脚本")


def badging(apk):
    out = subprocess.run([aapt2(), "dump", "badging", apk],
                         capture_output=True, text=True, errors="replace")
    flat = out.stdout + out.stderr
    name = re.search(r"versionName='([^']+)'", flat)
    code = re.search(r"versionCode='([^']+)'", flat)
    if not name or not code:
        raise SystemExit("aapt2 没读出包信息，确认这是不是 APK：" + apk)
    return name.group(1), code.group(1)


def main():
    apk = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_APK
    want = sys.argv[2] if len(sys.argv) > 2 else None

    if not os.path.exists(apk):
        raise SystemExit("找不到安装包：" + apk)

    src = gradle_version()
    pkg_name, pkg_code = badging(apk)

    print("构建脚本版本 :", src)
    print("包内版本     :", pkg_name, "(versionCode", pkg_code + ")")
    if want:
        print("本次要发版本 :", want)

    ok = True
    if pkg_name != src:
        print("✗ 包内版本与构建脚本不一致：改了版本号之后没有重新构建")
        ok = False
    if str(code_of(pkg_name)) != pkg_code:
        print("✗ versionCode 与四段版本号算出来的值不一致")
        ok = False
    if want and want != pkg_name:
        print("✗ 本次要发的版本号与包内版本不一致，别把旧包挂到新版本下面")
        ok = False

    print("✓ 可以发版" if ok else "× 先重新构建再发版")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
