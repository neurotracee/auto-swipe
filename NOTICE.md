# 第三方组件声明 / Third-Party Notices

本项目（自动翻页，安卓单端）整体以 **GNU AGPL-3.0-or-later** 授权发布，
并附带依据 **AGPL-3.0 第 7 条 (b) 项** 制定的附加条款 —— 见 **[ADDITIONAL-TERMS.md](ADDITIONAL-TERMS.md)**。

简而言之：二次开发或商用都可以，但**必须保留作者署名，并在程序里标注原项目地址**
`https://github.com/neurotracee/auto-swipe`（国内镜像：`https://gitee.com/bio234/auto-swipe`）。

---

## 一、打包进 apk 的组件

| 组件 | 版本 | 用途 | 授权 |
| --- | --- | --- | --- |
| AndroidX（core-ktx / lifecycle-runtime-ktx / activity-compose） | 1.13.1 / 2.8.4 / 1.9.1 | 基础框架 | Apache License 2.0 |
| Jetpack Compose（ui / ui-graphics / foundation / material3 / material-icons-core） | BOM 2024.06.00 | 界面 | Apache License 2.0 |
| Kotlin 标准库 | 1.9.24 | 语言运行时 | Apache License 2.0 |

-apk 中**没有**任何第三方 OCR、广告、统计、崩溃上报 SDK，也没有原生库（无 arm64 之外的 ABI 差异，装在哪台安卓上都一样）。

## 二、只在编译期使用、不进入 apk

| 组件 | 用途 | 授权 |
| --- | --- | --- |
| Android Gradle Plugin 8.4.2 | 构建 | Apache License 2.0 |
| JUnit 4 / kotlin-test | 单元测试 | EPL-1.0 / Apache-2.0 |

## 三、代码来源

- 应用本体（无障碍手势、悬浮球、随机调度、界面）为本项目原创。
- 检查更新相关实现（`Updater.kt`、`UpdateApk.kt`、`ReleaseDigest.kt`）改编自同一作者的
  [PDF 隐私打码工具](https://github.com/neurotracee/pdf-privacy-masker)（AGPL-3.0-or-later）。
  因为是同一著作权人、同一协议，这里不构成额外的第三方义务，但如实注明出处。
- 许可原文见 [LICENSE](LICENSE)；本项目未引入任何 AGPL 之外的强传染性代码。

## 四、必须保留的署名（AGPL-3.0 第 5 条，请勿删除）

```
指导作者：项目创作时为初中生 —— 需求、交互设计与验收测试全部由本人完成
开发    ：豆包工作任务 × DeepSeek-V4.1-Flash
原项目  ：https://github.com/neurotracee/auto-swipe
镜像    ：https://gitee.com/bio234/auto-swipe
```

二次开发或商用必须保留以上署名，并在程序中标注原项目地址；改动过的版本须注明
「本程序基于该地址修改，非原作者发布」。详见 [ADDITIONAL-TERMS.md](ADDITIONAL-TERMS.md)。

## 五、关于「打包后的程序必须开源」

AGPL-3.0 第 6 条要求：以目标代码形式分发（发 apk）时，必须向接收者提供**对应源代码**。

本仓库即为该对应源代码。任何人拿到本项目的 apk，都可以重新构建出功能等价的程序：

```bash
gradle assembleRelease
```

构建细节见 [docs/发布指南.md](docs/发布指南.md)。

## 六、名称与图标

程序名称「自动翻页」与图标不在授权范围内（AGPL-3.0 第 7 条 (e) 项）。
二次发布请改名、换图标。
