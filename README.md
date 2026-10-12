# TV

独立 Android TV 应用，以 TVBoxOS 为功能基线，继承 AVBox 的视觉风格。使用 Java / XML，复用原源引擎、播放器和遥控器控制链。设计采用顶部导航 C；Android 9+，支持 ARMv7 / ARM64。

## 当前版本

0.1.0 Debug 是首个原生评审包：C 首页、真实推荐与继续观看、详情与选集、播放控制层已接入；搜索键盘保留在左侧。正式安装为空壳，不预置订阅、站点或测试影片。

- 本地交付包：`artifacts/TV-0.1.0-armv7-debug.apk`（约 65 MiB），`artifacts/TV-0.1.0-arm64-debug.apk`（约 75 MiB）。产物目录不入 Git。
- ARM64 Android 11 模拟器已验证配置导入、推荐/详情、本地 H.264 预览与全屏播放、控制层显示和视频运动。
- 用户订阅已验证加载和部分源目录/详情；外部网页嗅探线路播放尚未确认。没有连接 ARMv7 实体电视，不能用模拟器代替真机结论。
- 直播与辅助页面沿用上游功能，完整视觉统一、各页面遥控器验收仍需后续完成。此包不表示所有 HTML 页面均已完整移植。

本地已配置 Android TV 13 / 1080p 模拟器，启动与补测结果见 [TV_EMULATOR.md](docs/TV_EMULATOR.md)。

具体结果见 [验证记录](docs/VALIDATION.md)，任务见 [.scratch/android-tv](.scratch/android-tv/spec.md)。

## 构建

先检查已有工具，环境要求与留存记录见 [ENVIRONMENT.md](docs/ENVIRONMENT.md)。默认使用 `${HOME}/.local/share/tv-android` 中的专用工具；可通过环境变量覆盖。

```sh
sh scripts/android-build.sh :app:assemblePython32Debug :app:assemblePython64Debug
python3 scripts/verify-apk.py app/build/outputs/apk/python32/debug/TV_debug-python32.apk --abi armeabi-v7a
```

保留 Gradle 7.5、AGP 7.2.2、CatVod / QuickJS / Python。构建脚本不安装工具、不设置 shell 启动项，使用 `--no-daemon`。APK 使用本机 Debug 签名，仅供个人测试；尚未建立发布签名与发行流程。

## 设计与出处

HTML 设计及评审截图见 [.scratch/design](.scratch/design/README.md)。源码基线及参考见 [UPSTREAM.md](docs/UPSTREAM.md)，上游 README 原文保留在 [TVBOXOS_README.md](docs/TVBOXOS_README.md)。保留 LICENSE 与第三方声明。

任务使用本地 Markdown；工作规范见 [AGENTS.md](AGENTS.md)。本项目未推送、发布，也未修改参考仓库。
