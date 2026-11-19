更新级别：建议

> **一句话**：关于页现在真的能点 Star、也能看到赞赏码；不花钱那条路写得很清楚。

## 1. 关于页：支持作者（安卓）

- 新增「如果这个应用帮到了你」：先讲清楚这是顺手做的小工具，然后给两条路。
- **点 Star 卡片**：「不想花钱也没关系，点一颗 Star 就是最好的支持——别人搜得到，我才更有动力维护。」右侧按钮直达 GitHub；打不开浏览器时链接自动复制并提示。
- **赞赏码**：用的是本人 PDF 项目那张同款码，白底放进圆角白卡里，深色模式下也不刺眼。写明「完全自愿，不打赏也一样免费、功能不缺」。
- 长按图片或点「保存赞赏码」都能存：API 29+ 直接写相册 `Pictures/AutoSwipe`，API 26~28 走系统「保存到…」选择器，**一个存储权限都不申请**。
- 首页底部版本行右侧加了「★ 点个 Star」，一处即达。

## 2. 文档

- README 中英各加一章「如果这个应用帮到了你 / If this app helped you」，含赞赏码图片（`docs/img/thanks_qr.png`）。
- CHANGELOG 补 1.0.2.0 条目。

## 3. 版本

- 1.0.1.1 → 1.0.2.0（功能级改动，动第三段）；versionCode `1000200`。
- 覆盖安装即可；设置、悬浮球位置都保留。

## English · Release Notes

Level: recommended

> **In one line**: the About page can now actually take you to the repo for a star, and shows the tip QR — and the "costs nothing" path is spelled out.

### 1. About page: supporting the author (Android)

- New block "If this app helped you": first the honest framing (it began as a small side project), then two ways to support it.
- **Star card**: "No money needed — a star is the best support: it helps others find the project, and it keeps me motivated." The button opens GitHub directly; if no browser is available the link is copied to the clipboard with a toast.
- **Tip QR code**: the same code used by the author's PDF project, white artwork on a rounded white card so it stays readable in dark mode. The copy states plainly that it is optional and nothing is locked behind it.
- Long-press the image or tap "Save the QR": API 29+ writes straight to the gallery album `Pictures/AutoSwipe`; API 26–28 fall back to the system "Save as…" picker. **No storage permission is requested at all.**
- The home footer now has "★ Star it" next to the version, so it is one tap away.

### 2. Documentation

- README gained a matching chapter in both languages ("如果这个应用帮到了你" / "If this app helped you") with the QR image at `docs/img/thanks_qr.png`.
- CHANGELOG has the 1.0.2.0 entry.

### 3. Version

- 1.0.1.1 → 1.0.2.0 (feature-level, third segment); versionCode `1000200`.
- Installs over the old version; settings and ball position are kept.
