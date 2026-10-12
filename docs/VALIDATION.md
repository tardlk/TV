# TV 0.1.0 Debug 验证记录

日期：2026-10-12。代码基线：TVBoxOS ab11d289e09963a9daf65ca7f6b7a9a8cbe184e1。

## 包验证

Python32 / Python64 Debug 构建成功。`scripts/verify-apk.py` 检查独立包名 com.coke.tv、名称 TV、最低 API 28、精确单 ABI、QuickJS 与 Python 3.8 库、无非空预置源配置、无用户测试订阅字节。两个 APK 的 apksigner v2 验证通过。

| APK | 字节数 | SHA-256 |
| --- | ---: | --- |
| ARMv7 | 68241746 | d80ad8b4465ea9f5e4ca7d43266623879a260e1e3b729a7570a3a6f08863a5b5 |
| ARM64 | 78856100 | 948a5b516935ea7636bd70956017f54cc8069165c1218d6908259073a8b6fdd3 |

产物与 JSON 报告在 ignored `artifacts/`。测试订阅未写入源码或 APK。

## 实际运行

Android 11 / API 30 ARM64 Google APIs 模拟器，1280×720：

- 全新安装为空壳；通过原配置对话框导入用户订阅成功。
- 用户订阅包含 56 个站点。JAR 首页加载分类、推荐与图片；采集源详情可打开。
- 使用宿主协议脚本抽查三个接口，目录 / 列表 / 详情成功；第四个接口返回非 JSON。这是接口抽查，不代表全部站点可用。
- 自制 60 秒 H.264 色条及移动方块视频，经过原源 API → 原详情 → 原播放器，预览与全屏出画面；不同时间截图确认方块移动。方向键可调出改造后的原播放器控制层。
- 分类向上可以返回顶部 Home；原返回链可用。未重写按键或焦点框架。
- 固定点播配置 lives: [] 暴露上游数组越界，已增加非空判断，复验通过。
- 最后一轮 AndroidRuntime 错误日志没有崩溃记录。

`uiautomator dump` 在视频界面偶尔无法获得 idle 状态，已改为直接截图观察；不将自动化超时作为播放器失败结论。原生截图保留在 `artifacts/native-previews/`。

## 验证范围之外

尚未确认用户订阅的外部线路播通：选到的网页线路停留在嗅探。固定视频成功仅验证本地媒体接线和出画面；没有验证真实音频、所有解码器、所有格式与源、直播连续播放、全部遥控器动作。ARMv7 APK 已构建和包内容校验，当前没有 ARMv7 实体电视；不能用 ARM64 模拟器替代真机结果。

首包主要完成 C 首页、详情、选集与播放器层。辅助页面完整视觉统一与设备验收见本地任务 04。

## 固定样本复现

复用 macOS 已有 Swift / AVFoundation，无额外多媒体工具安装：

```sh
mkdir -p .local-test/fixed-site
swift tests/fixtures/make-clip.swift .local-test/fixed-site/clip.mp4
python3 tests/fixtures/site.py --directory .local-test/fixed-site
```

在 Android 模拟器的原配置对话框输入 `http://10.0.2.2:48979/config.json`。只绑定宿主 127.0.0.1:48979；测试结束 Ctrl-C 停止服务。样本无外部站点与第三方媒体，不属于 APK 资源。

## Android TV 13 专用镜像补测

在官方 API 33 ARM64 Android TV / 1080p 设备上安装同一首包，验证空壳、顶部方向导航、搜索与设置、固定配置、详情预览 / 全屏视频和媒体键暂停恢复、历史与用户订阅首页。采样得到视频停止 / 运动交替，AndroidRuntime 无崩溃；详见 [TV_EMULATOR.md](TV_EMULATOR.md)。没有改变 ARMv7 真机、外部线路与直播仍待验证的结论。
