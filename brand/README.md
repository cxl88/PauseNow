# PauseNow 品牌资产

## 核心含义

图形由两根圆角暂停竖线和一条未闭合的保护环组成：停一下不是禁止使用，而是在打开应用前留出一次主动选择的空间。

## 文件与用途

| 文件 | 使用场景 |
| --- | --- |
| `pausenow-app-icon.svg` | 官网 favicon、应用商店宣传图、社媒头像 |
| `pausenow-lockup-light.svg` | 白色或浅色官网背景的页眉和页脚 |
| `pausenow-lockup-dark.svg` | 深绿背景的官网首屏和页脚 |
| `pausenow-mark-mono-deep.svg` | 单色印刷、浅色 UI、文档 |
| `pausenow-mark-mono-light.svg` | 深色 UI、深色图片水印 |

## 色彩

- 深绿：`#176B5B`
- 薄荷绿：`#8FE6D7`
- 暖白：`#F7F7F2`
- 炭黑：`#1F2A28`

## 官网接入

浅色页眉使用：

```html
<img src="/brand/pausenow-lockup-light.svg" alt="PauseNow 停一下" width="234" height="54">
```

深色背景使用 `pausenow-lockup-dark.svg`。横版字标使用系统或官网已加载的 Inter 字体；若未加载 Inter，会降级为 Arial。

## Android 接入

`android/app/src/main/res/mipmap-anydpi-v26/` 提供 Adaptive Icon，Manifest 已引用 `@mipmap/ic_launcher`。系统浅色模式采用深绿底、薄荷保护环与暖白暂停符；系统深色模式自动切换为暖白底、深绿单色符号。
